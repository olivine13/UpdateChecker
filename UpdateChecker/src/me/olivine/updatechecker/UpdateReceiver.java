package me.olivine.updatechecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateReceiver extends BroadcastReceiver {

	public final static String tag = UpdateReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		UpdateChecker.isCancel = true ;
	}

}
