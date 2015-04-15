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
	 * �������0x00,����ȡ��
	 */
	public final static int CODE_DEFAULT = 0x00;
	/**
	 * �������0x01��url����ʧ��
	 */
	public final static int CODE_URL = 0x01;
	/**
	 * �������0x02,��������ʧ��
	 */
	public final static int CODE_HTTP = 0x02;
	/**
	 * �������0x03���ļ�δ�ҵ�
	 */
	public final static int CODE_FILE_NOTFOUND = 0x03;
	/**
	 * �������0x04,�ļ�����дʧ��
	 */
	public final static int CODE_IO_ERROR = 0x04;

	/**
	 * ��������ַ
	 */
	static String server_address = "http://127.0.0.1";

	/**
	 * ����·��
	 */
	static String save_path = Environment.getExternalStorageDirectory()
			+ "/Download";

	/**
	 * Ӧ������,��.apk��׺
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
	 * ���ø����ļ����棬Ĭ��Ϊ:Environment.getExternalStorageDirectory() + "/Download"
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

	// ������������
	HttpAsyncTask task;

	/**
	 * ��ȡ��ǰ����汾��Ϣ
	 * 
	 * @param context
	 * @return �汾��Ϣ
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
			Log.e(tag, "����������");
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
					Log.e(tag, "xml��������");
					return null;
				}
				he.consumeContent();
			}
		} catch (ClientProtocolException e) {
			Log.e(tag, "Э�����");
			return null;
		} catch (IOException e) {
			Log.e(tag, "��д����");
			return null;
		}
		return versionInfo;
	}

	/**
	 * * ��ȡ������Ϣ
	 * 
	 * @param context
	 *            ������
	 * @param pOnResponseListener
	 *            ��Ӧ����
	 * @return �Ƿ��и��°汾
	 */
	public void checkUpdate(final Context context,
			final OnResponseListener pOnResponseListener) {

		// �ȼ���Ƿ񻺴��������Ϣ
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
			Log.d(tag, "���ڽ��и��¼��");
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
	 * ����Ƿ��л�������ļ�
	 * 
	 * @param context
	 *            ��ǰ������
	 * 
	 * @param outVi
	 *            ���л��������Ϣ��outViΪ��װ��Ϣ
	 * @param outPath
	 *            ���л��氲װ�ļ���outPath��¼��װ·��������Ϊnull
	 * @return ������false��˵��û�л�����Ч�ĸ����ļ�������true˵���л�������ļ�
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
			Log.d(tag, "��ǰ�汾�Ȼ���汾����");
			return false;
		}
		outPath.append(sp.getString("savePath", null));
		if (outPath == null || !new File(outPath.toString()).exists()) {
			Log.d(tag, "�����ļ�������");
			return false;
		}
		return true;
	}

	/**
	 * ���»�����Ϣ
	 * 
	 * @param context
	 *            ��ǰ������
	 * @param vi
	 *            Ҫ����İ汾��Ϣ
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
	 * ���»�����Ϣ
	 * 
	 * @param context
	 *            ��ǰ������
	 * @param path
	 *            Ҫ���µı���·��
	 */
	private void updateCache(Context context, String path) {
		SharedPreferences sp = context.getSharedPreferences("update_info",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("savePath", path);
		editor.apply();
	}

	/**
	 * ��ʼ����
	 * 
	 * @param context
	 *            ��ǰ������
	 * @param url
	 *            ���ص�ַ
	 * @param updatingIconId
	 *            ֪ͨ������ʱͼ�꣬ʹ��Ĭ��Ϊ0
	 * @param updateFinishIconId
	 *            ֪ͨ�����½���ʱͼ�꣬ʹ��Ĭ��Ϊ0
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
	 * ��ʼ����
	 * 
	 * @param context
	 *            ��ǰ������
	 * @param url
	 *            ���ص�ַ
	 */
	public void startUpdate(Context context, String url) {

		isCancel = false;

		apkName = getApkName(url);

		// �½��ļ���
		File fp = new File(save_path);
		if (!fp.exists()) {
			fp.mkdir();
		}

		mNotificationManager = (NotificationManager) context
				.getSystemService(Activity.NOTIFICATION_SERVICE);

		// ��ʼ��֪ͨ��
		mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setTicker("��ʼ���ظ���")
				.setSmallIcon(mUpdatingIconId)
				.setContentIntent(
						getDefalutIntent(context, Notification.FLAG_NO_CLEAR
								| PendingIntent.FLAG_ONE_SHOT))
				.setDefaults(Notification.DEFAULT_SOUND).setContentTitle("����")
				.setPriority(Notification.PRIORITY_DEFAULT).setOngoing(true);// ����֪ͨСICON
		Notification notification = mBuilder.build();

		mNotificationManager.notify(1, notification);

		// �������ط���
		Intent service = new Intent(context, UpdateService.class);
		service.putExtra("url", url);
		context.startService(service);
	}

	/**
	 * ���ø��½���ʱ����
	 * 
	 * @param pOnUpdateFinishListener
	 *            ���½�������
	 */
	public void setOnUpdateFinishListener(
			OnUpdateFinishListener pOnUpdateFinishListener) {
		this.mOnUpdateFinishListener = pOnUpdateFinishListener;
	}

	/**
	 * ���ø���ȡ��ʱ����
	 * 
	 * @param pOnUpdateCancelListener
	 *            ����ȡ������
	 */
	public void setOnUpdateCancelListener(
			OnUpdateCancelListener pOnUpdateCancelListener) {
		this.mOnUpdateCancelListener = pOnUpdateCancelListener;
	}

	/**
	 * ����֪ͨ������
	 * 
	 * @param progress
	 *            ���½���,maxΪ100
	 */
	protected void updateNotificationPorgress(int progress) {
		mBuilder.setProgress(100, progress, false).setDefaults(0);
		Notification notification = mBuilder.build();

		mNotificationManager.notify(1, notification);
	}

	protected void updateCancel(int code) {
		Log.d(tag, "ȡ������");
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
				.setContentTitle("�������")
				.setContentText("�����װ")
				.setOngoing(false)
				.setAutoCancel(true)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setContentIntent(
						getInstallIntent(context, Notification.FLAG_AUTO_CANCEL
								| PendingIntent.FLAG_ONE_SHOT))
				.setSmallIcon(mUpdateFinishIconId);

		Notification notification = mBuilder.build();
		mNotificationManager.notify(1, notification);

		// ���»�����Ϣ
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
		 * ����ȡ����������ļ�ʱ
		 * 
		 * @param versionInfo
		 *            ����İ汾��Ϣ
		 * @param savePath
		 *            ����İ�װ�ļ�·��
		 */
		void onCache(VersionInfo versionInfo, String savePath);

		/**
		 * ����������������Ӧ����
		 * 
		 * @param isNewest
		 *            �Ƿ������°汾,true�����°汾��false���ڸ��°汾
		 * @param versionInfo
		 *            ����������°汾��versionInfo���������°汾����������°汾,���ص�ǰ�汾
		 */
		void onResponse(boolean isNewest, VersionInfo versionInfo);

		/**
		 * ������
		 */
		void onError();
	}

	/**
	 * ���ظ�����ϼ��������������ʱ����onFinish����
	 * 
	 * @author lzq
	 *
	 */
	public interface OnUpdateFinishListener {
		void onFinish();
	}

	/**
	 * ���ظ���ȡ����������ȡ��ʱ����onCancel����
	 * 
	 * @author lzq
	 *
	 */

	public interface OnUpdateCancelListener {
		/**
		 * ����ȡ��������
		 * 
		 * @param code
		 *            ȡ��ʱ������
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
