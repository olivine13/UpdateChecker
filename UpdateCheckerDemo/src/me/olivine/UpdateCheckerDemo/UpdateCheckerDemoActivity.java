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

		// 设置下载文件保存地址
		UpdateChecker.setSavePath("/test");
		// 设置检查更新服务器地址,这里使用我的个人服务器做demo测试
		UpdateChecker
				.setServerAddress("http://192.168.1.1/static/updatecheckerdemo");

		((Button) findViewById(R.id.button))
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// 检测新版本,请自行实现回调处理
						UpdateChecker.getInstance().checkUpdate(
								UpdateCheckerDemoActivity.this,
								new UpdateChecker.OnResponseListener() {

									// 正常下载 响应
									@Override
									public void onResponse(boolean isNewest,
											VersionInfo versionInfo) {
										if (isNewest) {
											Toast.makeText(
													UpdateCheckerDemoActivity.this,
													"已经是最新版本",
													Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(
													UpdateCheckerDemoActivity.this,
													"发现新版本", Toast.LENGTH_SHORT)
													.show();
											getUpdateDialog(versionInfo).show();
										}
									}

									// 错误处理
									@Override
									public void onError() {
										Toast.makeText(
												UpdateCheckerDemoActivity.this,
												"版本信息获取失败", Toast.LENGTH_SHORT)
												.show();
									}

									// 缓存处理
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
				.setTitle("更新")
				.setMessage(versionInfo.getUpdateInfo())
				.setPositiveButton("更新", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						// 开启下载，此方法在通知栏弹出消息题型
						UpdateChecker.getInstance().startUpdate(
								UpdateCheckerDemoActivity.this,
								versionInfo.getDownloadUrl());
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.d(tag, "取消更新");
					}
				}).create();
	}
}
