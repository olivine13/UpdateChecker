package me.olivine.UpdateCheckerDemo;

import java.io.File;

import me.olivine.updatechecker.UpdateChecker;
import me.olivine.updatechecker.VersionInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class UpdateCheckerDemoActivity extends Activity {

	public final static String tag = UpdateCheckerDemoActivity.class
			.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ���������ļ������ַ
		UpdateChecker.setSavePath("/test");
		// ���ü����·�������ַ,����ʹ���ҵĸ��˷�������demo����
		UpdateChecker
				.setServerAddress("http://192.168.1.1/static/updatecheckerdemo");

		((Button) findViewById(R.id.button))
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// ����°汾,������ʵ�ֻص�����
						UpdateChecker.getInstance().checkUpdate(
								UpdateCheckerDemoActivity.this,
								new UpdateChecker.OnResponseListener() {

									// �������� ��Ӧ
									@Override
									public void onResponse(boolean isNewest,
											VersionInfo versionInfo) {
										if (isNewest) {
											Toast.makeText(
													UpdateCheckerDemoActivity.this,
													"�Ѿ������°汾",
													Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(
													UpdateCheckerDemoActivity.this,
													"�����°汾", Toast.LENGTH_SHORT)
													.show();
											getUpdateDialog(versionInfo).show();
										}
									}

									// ������
									@Override
									public void onError() {
										Toast.makeText(
												UpdateCheckerDemoActivity.this,
												"�汾��Ϣ��ȡʧ��", Toast.LENGTH_SHORT)
												.show();
									}

									// ���洦��
									@Override
									public void onCache(
											VersionInfo versionInfo,
											String savePath) {
										Toast.makeText(
												UpdateCheckerDemoActivity.this,
												savePath, Toast.LENGTH_SHORT)
												.show();
										Intent intent = new Intent(
												Intent.ACTION_VIEW);
										intent.setDataAndType(Uri
												.fromFile(new File(savePath)),
												"application/vnd.android.package-archive");
										startActivity(intent);
									}
								});
					}
				});
	}

	private Dialog getUpdateDialog(final VersionInfo versionInfo) {
		return new AlertDialog.Builder(this)
				.setTitle("����")
				.setMessage(versionInfo.getUpdateInfo())
				.setPositiveButton("����", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						// �������أ��˷�����֪ͨ��������Ϣ����
						UpdateChecker.getInstance().startUpdate(
								UpdateCheckerDemoActivity.this,
								versionInfo.getDownloadUrl());
					}
				})
				.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.d(tag, "ȡ������");
					}
				}).create();
	}
}
