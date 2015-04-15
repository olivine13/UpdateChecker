package me.olivine.updatechecker;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class UpdateChecker {

	public final static String tag = UpdateChecker.class.getSimpleName();

	/**
	 * 错误代码0x00,正常取消
	 */
	public final static int CODE_DEFAULT = 0x00;
	/**
	 * 错误代码0x01，url解析失败
	 */
	public final static int CODE_URL = 0x01;
	/**
	 * 错误代码0x02,网络连接失败
	 */
	public final static int CODE_HTTP = 0x02;
	/**
	 * 错误代码0x03，文件未找到
	 */
	public final static int CODE_FILE_NOTFOUND = 0x03;
	/**
	 * 错误代码0x04,文件流读写失败
	 */
	public final static int CODE_IO_ERROR = 0x04;

	/**
	 * 服务器地址
	 */
	static String server_address = "http://127.0.0.1";

	/**
	 * 保存路径
	 */
	static String save_path = Environment.getExternalStorageDirectory()
			+ "/Download";

	/**
	 * 应用名称,带.apk后缀
	 */
	static String apkName = "";

	static boolean isCancel = false;

	static class UpdateCheckerHolder {
		static UpdateChecker INSTANCE = new UpdateChecker();
	}

	public static UpdateChecker getInstance() {
		return UpdateCheckerHolder.INSTANCE;
	}

	/**
	 * 设置更新文件保存，默认为:Environment.getExternalStorageDirectory() + "/Download"
	 * 
	 * @param path
	 */
	public static void setSavePath(String path) {
		save_path = path;
	}

	protected static String getSavePath() {
		return save_path;
	}

	public static void setServerAddress(String address) {
		server_address = address;
	}

	public static String getServerAddress() {
		return server_address;
	}

	int mUpdatingIconId = android.R.drawable.stat_sys_download;
	int mUpdateFinishIconId = android.R.drawable.stat_sys_download_done;
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotificationManager;

	OnUpdateFinishListener mOnUpdateFinishListener;
	OnUpdateCancelListener mOnUpdateCancelListener;

	// 网络请求任务
	HttpAsyncTask task;

	/**
	 * 获取当前软件版本信息
	 * 
	 * @param context
	 * @return 版本信息
	 */
	public VersionInfo getCurrentVersionInfo(Context context) {
		VersionInfo versionInfo = new VersionInfo();
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			versionInfo.setVersionCode(pi.versionCode);
			versionInfo.setVersionName(pi.versionName);
			versionInfo.setDownloadUrl(null);
			versionInfo.setUpdateInfo(null);
		} catch (NameNotFoundException e) {
			Log.e(tag, "包名不存在");
			return null;
		}
		return versionInfo;
	}

	public VersionInfo getNewestVersionInfo() {
		VersionInfo versionInfo = new VersionInfo();
		HttpClient hc = new DefaultHttpClient();
		HttpGet get = new HttpGet(server_address + "/VersionInfo.xml");
		try {
			HttpResponse response = hc.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity he = response.getEntity();
				try {
					SAXParserFactory factory = SAXParserFactory.newInstance();
					SAXParser parser = factory.newSAXParser();
					parser.parse(he.getContent(), new VersionSaxHandler(
							versionInfo));
				} catch (Exception e) {
					Log.e(tag, "xml解析错误");
					return null;
				}
				he.consumeContent();
			}
		} catch (ClientProtocolException e) {
			Log.e(tag, "协议错误");
			return null;
		} catch (IOException e) {
			Log.e(tag, "读写错误");
			return null;
		}
		return versionInfo;
	}

	/**
	 * * 获取更新信息
	 * 
	 * @param context
	 *            上下文
	 * @param pOnResponseListener
	 *            响应监听
	 * @return 是否有更新版本
	 */
	public void checkUpdate(final Context context,
			final OnResponseListener pOnResponseListener) {

		// 先检查是否缓存过更新信息
		VersionInfo vi = new VersionInfo();
		StringBuffer savePath = new StringBuffer() ;
		if (checkCache(context, vi, savePath)) {
			pOnResponseListener.onCache(vi, savePath.toString());
			return;
		}

		if (task == null)
			task = new HttpAsyncTask() {
				@Override
				protected void onPostExecute(Boolean result) {
					super.onPostExecute(result);
					task = null;
				}
			};
		else {
			Log.d(tag, "正在进行更新检查");
			return;
		}
		task.execute(new IAsyncCallBack() {

			VersionInfo vi;
			VersionInfo newVi;

			@Override
			public boolean workToDo() {
				vi = getCurrentVersionInfo(context);
				newVi = getNewestVersionInfo();

				if (vi == null || newVi == null)
					return false;
				return true;
			}

			@Override
			public void onComplete() {
				if (newVi.getVersionCode() <= vi.getVersionCode()) {
					updateCache(context, vi);
					if (pOnResponseListener != null)
						pOnResponseListener.onResponse(true, vi);
				} else {
					updateCache(context, newVi);
					if (pOnResponseListener != null)
						pOnResponseListener.onResponse(false, newVi);
				}
			}

			@Override
			public void onError() {
				if (pOnResponseListener != null)
					pOnResponseListener.onError();
			}
		});
	}

	/**
	 * 检查是否有缓存更新文件
	 * 
	 * @param context
	 *            当前上下文
	 * 
	 * @param outVi
	 *            若有缓存更新信息，outVi为安装信息
	 * @param outPath
	 *            若有缓存安装文件，outPath记录安装路径，否则为null
	 * @return 若返回false，说明没有缓存有效的更新文件，返回true说明有缓存更新文件
	 */
	private boolean checkCache(Context context, final VersionInfo outVi,
			final StringBuffer outPath) {
		SharedPreferences sp = context.getSharedPreferences("update_info",
				Context.MODE_PRIVATE);
		outVi.mVersionCode = sp.getInt("versionCode", 0);
		outVi.mVersionName = sp.getString("versionName", null);
		outVi.mUpdateInfo = sp.getString("updateInfo", null);
		if (getCurrentVersionInfo(context).getVersionCode() >= outVi
				.getVersionCode()) {
			Log.d(tag, "当前版本比缓存版本更新");
			return false;
		}
		outPath.append(sp.getString("savePath", null));
		if (outPath == null || !new File(outPath.toString()).exists()) {
			Log.d(tag, "缓存文件不存在");
			return false;
		}
		return true;
	}

	/**
	 * 更新缓存信息
	 * 
	 * @param context
	 *            当前上下文
	 * @param vi
	 *            要缓存的版本信息
	 */
	private void updateCache(Context context, VersionInfo vi) {
		SharedPreferences sp = context.getSharedPreferences("update_info",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt("versionCode", vi.mVersionCode);
		editor.putString("versionName", vi.getVersionName());
		editor.putString("updateInfo", vi.getUpdateInfo());
		editor.apply();
	}

	/**
	 * 更新缓存信息
	 * 
	 * @param context
	 *            当前上下文
	 * @param path
	 *            要更新的保存路径
	 */
	private void updateCache(Context context, String path) {
		SharedPreferences sp = context.getSharedPreferences("update_info",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("savePath", path);
		editor.apply();
	}

	/**
	 * 开始更新
	 * 
	 * @param context
	 *            当前上下文
	 * @param url
	 *            下载地址
	 * @param updatingIconId
	 *            通知栏更新时图标，使用默认为0
	 * @param updateFinishIconId
	 *            通知栏更新结束时图标，使用默认为0
	 */
	public void startUpdate(Context context, String url, int updatingIconId,
			int updateFinishIconId) {
		if (updatingIconId != 0)
			mUpdatingIconId = updatingIconId;
		if (updateFinishIconId != 0)
			mUpdateFinishIconId = updateFinishIconId;
		startUpdate(context, url);
	}

	/**
	 * 开始更新
	 * 
	 * @param context
	 *            当前上下文
	 * @param url
	 *            下载地址
	 */
	public void startUpdate(Context context, String url) {

		isCancel = false;

		apkName = getApkName(url);

		// 新建文件夹
		File fp = new File(save_path);
		if (!fp.exists()) {
			fp.mkdir();
		}

		mNotificationManager = (NotificationManager) context
				.getSystemService(Activity.NOTIFICATION_SERVICE);

		// 初始化通知栏
		mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setTicker("开始下载更新")
				.setSmallIcon(mUpdatingIconId)
				.setContentIntent(
						getDefalutIntent(context, Notification.FLAG_NO_CLEAR
								| PendingIntent.FLAG_ONE_SHOT))
				.setDefaults(Notification.DEFAULT_SOUND).setContentTitle("更新")
				.setPriority(Notification.PRIORITY_DEFAULT).setOngoing(true);// 设置通知小ICON
		Notification notification = mBuilder.build();

		mNotificationManager.notify(1, notification);

		// 启动下载服务
		Intent service = new Intent(context, UpdateService.class);
		service.putExtra("url", url);
		context.startService(service);
	}

	/**
	 * 设置更新结束时监听
	 * 
	 * @param pOnUpdateFinishListener
	 *            更新结束监听
	 */
	public void setOnUpdateFinishListener(
			OnUpdateFinishListener pOnUpdateFinishListener) {
		this.mOnUpdateFinishListener = pOnUpdateFinishListener;
	}

	/**
	 * 设置更新取消时监听
	 * 
	 * @param pOnUpdateCancelListener
	 *            更新取消监听
	 */
	public void setOnUpdateCancelListener(
			OnUpdateCancelListener pOnUpdateCancelListener) {
		this.mOnUpdateCancelListener = pOnUpdateCancelListener;
	}

	/**
	 * 更新通知栏进度
	 * 
	 * @param progress
	 *            更新进度,max为100
	 */
	protected void updateNotificationPorgress(int progress) {
		mBuilder.setProgress(100, progress, false).setDefaults(0);
		Notification notification = mBuilder.build();

		mNotificationManager.notify(1, notification);
	}

	protected void updateCancel(int code) {
		Log.d(tag, "取消下载");
		if (mOnUpdateCancelListener != null) {
			mOnUpdateCancelListener.onCancel(code);
		}
		mNotificationManager.cancelAll();
	}

	protected void updateFinish(Context context) {

		if (mOnUpdateFinishListener != null) {
			mOnUpdateFinishListener.onFinish();
		}

		mBuilder.setProgress(0, 0, false)
				.setContentTitle("更新完成")
				.setContentText("点击安装")
				.setOngoing(false)
				.setAutoCancel(true)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setContentIntent(
						getInstallIntent(context, Notification.FLAG_AUTO_CANCEL
								| PendingIntent.FLAG_ONE_SHOT))
				.setSmallIcon(mUpdateFinishIconId);

		Notification notification = mBuilder.build();
		mNotificationManager.notify(1, notification);

		// 更新缓存信息
		updateCache(context, save_path + "/" + apkName);
	}

	private PendingIntent getDefalutIntent(Context context, int flags) {
		Intent intent = new Intent(context, UpdateReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
				intent, flags);
		return pendingIntent;
	}

	private PendingIntent getInstallIntent(Context context, int flags) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(
				Uri.fromFile(new File(save_path + "/" + apkName)),
				"application/vnd.android.package-archive");
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 1,
				intent, flags);
		return pendingIntent;
	}

	private String getApkName(String downloadUrl) {
		String result = null;
		Pattern pat = Pattern.compile("\\w+.apk$");
		Matcher mat = pat.matcher(downloadUrl);
		if (mat.find())
			result = mat.group();
		return result;
	}

	public interface OnResponseListener {

		/**
		 * 当获取到缓存更新文件时
		 * 
		 * @param versionInfo
		 *            缓存的版本信息
		 * @param savePath
		 *            缓存的安装文件路径
		 */
		void onCache(VersionInfo versionInfo, String savePath);

		/**
		 * 网络请求正常，响应处理
		 * 
		 * @param isNewest
		 *            是否是最新版本,true是最新版本，false存在更新版本
		 * @param versionInfo
		 *            如果不是最新版本，versionInfo将包含最新版本，如果是最新版本,返回当前版本
		 */
		void onResponse(boolean isNewest, VersionInfo versionInfo);

		/**
		 * 错误处理
		 */
		void onError();
	}

	/**
	 * 下载更新完毕监听，当下载完毕时调用onFinish函数
	 * 
	 * @author lzq
	 *
	 */
	public interface OnUpdateFinishListener {
		void onFinish();
	}

	/**
	 * 下载更新取消监听，当取消时调用onCancel函数
	 * 
	 * @author lzq
	 *
	 */

	public interface OnUpdateCancelListener {
		/**
		 * 更新取消返回码
		 * 
		 * @param code
		 *            取消时返回码
		 */
		void onCancel(int code);
	}

	class VersionSaxHandler extends DefaultHandler {

		private String tagName;
		VersionInfo mVersionInfo;

		public VersionSaxHandler(VersionInfo vi) {
			mVersionInfo = vi;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {

			this.tagName = localName;
			if (this.tagName.equalsIgnoreCase("Application")) {
				mVersionInfo.mVersionCode = Integer.parseInt(attributes
						.getValue("versionCode"));
				mVersionInfo.mVersionName = attributes.getValue("versionName");
				mVersionInfo.mDownloadUrl = attributes.getValue("downloadUrl");
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (this.tagName == null)
				return;

			String value = new String(ch, start, length);
			if (this.tagName.equalsIgnoreCase("UpdateInfo")) {
				if (mVersionInfo.mUpdateInfo == null)
					mVersionInfo.mUpdateInfo = "";
				mVersionInfo.mUpdateInfo += value;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			this.tagName = null;
		}
	}
}
