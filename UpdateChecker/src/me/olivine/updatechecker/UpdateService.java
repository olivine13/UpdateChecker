package me.olivine.updatechecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class UpdateService extends Service {

	public final static String tag = UpdateService.class.getSimpleName();

	final static int MSG_DOWNLOAD_DONE = 0x00;
	final static int MSG_DOWNLOAD_CANCEL = 0x01;
	final static int MSG_UPDATE_PROGRESS = 0x02;
	private String downloadUrl;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(tag, "onCreate");
		super.onCreate();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					URL url;
					try {
						url = new URL(downloadUrl);
					} catch (MalformedURLException e) {
						Log.d(tag, "url解析失败");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL, UpdateChecker.CODE_URL);
						msg.sendToTarget();
						return;
					}
					// 创建连接
					HttpURLConnection conn;
					InputStream is;
					int length;
					try {
						conn = (HttpURLConnection) url.openConnection();
						conn.connect();
						// 获取文件大小
						length = conn.getContentLength();
						// 创建输入流
						is = conn.getInputStream();
					} catch (IOException e) {
						Log.d(tag, "网络连接失败");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL, UpdateChecker.CODE_HTTP);
						msg.sendToTarget();
						return;
					}

					File file = new File(UpdateChecker.save_path);
					// 判断文件目录是否存在
					if (!file.exists()) {
						Log.d(tag, "file not found");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_FILE_NOTFOUND);
						msg.sendToTarget();
						return;
					}

					if( UpdateChecker.apkName==null ) {
						Log.d(tag, "文件名不合法");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_IO_ERROR);
						msg.sendToTarget();
						return;
					}
					File apkFile = new File(UpdateChecker.save_path, UpdateChecker.apkName);
					FileOutputStream fos;
					try {
						fos = new FileOutputStream(apkFile);

						int count = 0;
						// 缓存
						byte buf[] = new byte[1024];
						// 写入到文件中
						do {
							if (UpdateChecker.isCancel) {
								// 关闭文件流,删除下载文件
								is.close();
								fos.close();
								apkFile.delete();
								Message msg = handler.obtainMessage(
										MSG_DOWNLOAD_CANCEL,
										UpdateChecker.CODE_DEFAULT);
								msg.sendToTarget();
								return;
							}
							int numread = is.read(buf);
							count += numread;
							// 计算进度条位置
							int progress = (int) (((float) count / length) * 100);
							// 更新进度
							handler.removeMessages(MSG_UPDATE_PROGRESS);
							Message msg = handler.obtainMessage(
									MSG_UPDATE_PROGRESS, progress);
							msg.sendToTarget();
							if (numread <= 0) {
								// 下载完成
								handler.sendEmptyMessage(MSG_DOWNLOAD_DONE);
								break;
							}
							// 写入文件
							fos.write(buf, 0, numread);
						} while (true);
					} catch (FileNotFoundException e) {
						Log.d(tag, "文件不存在");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_FILE_NOTFOUND);
						msg.sendToTarget();
					} catch (IOException e) {
						Log.d(tag, "文件读写错误");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_IO_ERROR);
						msg.sendToTarget();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							Log.d(tag, "文件关闭失败");
							Message msg = handler.obtainMessage(
									MSG_DOWNLOAD_CANCEL,
									UpdateChecker.CODE_IO_ERROR);
							msg.sendToTarget();
						}
					}
				}
				// 更新结束,更新通知栏并且结束更新服务
				handler.sendEmptyMessage(MSG_DOWNLOAD_DONE);
			}

		}).start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		downloadUrl = intent.getStringExtra("url");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.d(tag, "onDestroy");
		super.onDestroy();
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_DOWNLOAD_DONE) {
				UpdateChecker.getInstance().updateFinish(
						getApplicationContext());
				stopSelf();
			} else if (msg.what == MSG_DOWNLOAD_CANCEL) {
				UpdateChecker.getInstance().updateCancel((Integer) msg.obj);
				stopSelf();
			} else if (msg.what == MSG_UPDATE_PROGRESS) {
				UpdateChecker.getInstance().updateNotificationPorgress(
						(Integer) msg.obj);
			}
		}
	};

}
