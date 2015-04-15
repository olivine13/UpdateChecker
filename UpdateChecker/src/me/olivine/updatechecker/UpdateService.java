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
				// �ж�SD���Ƿ���ڣ������Ƿ���ж�дȨ��
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					URL url;
					try {
						url = new URL(downloadUrl);
					} catch (MalformedURLException e) {
						Log.d(tag, "url����ʧ��");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL, UpdateChecker.CODE_URL);
						msg.sendToTarget();
						return;
					}
					// ��������
					HttpURLConnection conn;
					InputStream is;
					int length;
					try {
						conn = (HttpURLConnection) url.openConnection();
						conn.connect();
						// ��ȡ�ļ���С
						length = conn.getContentLength();
						// ����������
						is = conn.getInputStream();
					} catch (IOException e) {
						Log.d(tag, "��������ʧ��");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL, UpdateChecker.CODE_HTTP);
						msg.sendToTarget();
						return;
					}

					File file = new File(UpdateChecker.save_path);
					// �ж��ļ�Ŀ¼�Ƿ����
					if (!file.exists()) {
						Log.d(tag, "file not found");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_FILE_NOTFOUND);
						msg.sendToTarget();
						return;
					}

					if( UpdateChecker.apkName==null ) {
						Log.d(tag, "�ļ������Ϸ�");
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
						// ����
						byte buf[] = new byte[1024];
						// д�뵽�ļ���
						do {
							if (UpdateChecker.isCancel) {
								// �ر��ļ���,ɾ�������ļ�
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
							// ���������λ��
							int progress = (int) (((float) count / length) * 100);
							// ���½���
							handler.removeMessages(MSG_UPDATE_PROGRESS);
							Message msg = handler.obtainMessage(
									MSG_UPDATE_PROGRESS, progress);
							msg.sendToTarget();
							if (numread <= 0) {
								// �������
								handler.sendEmptyMessage(MSG_DOWNLOAD_DONE);
								break;
							}
							// д���ļ�
							fos.write(buf, 0, numread);
						} while (true);
					} catch (FileNotFoundException e) {
						Log.d(tag, "�ļ�������");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_FILE_NOTFOUND);
						msg.sendToTarget();
					} catch (IOException e) {
						Log.d(tag, "�ļ���д����");
						Message msg = handler.obtainMessage(
								MSG_DOWNLOAD_CANCEL,
								UpdateChecker.CODE_IO_ERROR);
						msg.sendToTarget();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							Log.d(tag, "�ļ��ر�ʧ��");
							Message msg = handler.obtainMessage(
									MSG_DOWNLOAD_CANCEL,
									UpdateChecker.CODE_IO_ERROR);
							msg.sendToTarget();
						}
					}
				}
				// ���½���,����֪ͨ�����ҽ������·���
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
