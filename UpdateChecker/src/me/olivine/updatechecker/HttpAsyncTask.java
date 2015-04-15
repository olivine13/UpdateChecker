package me.olivine.updatechecker;

import android.os.AsyncTask;

public class HttpAsyncTask extends AsyncTask<IAsyncCallBack, Integer, Boolean> {

	public final static String tag = "HttpAsyncTask";

	private IAsyncCallBack[] params;
	private boolean[] result;

	@Override
	protected Boolean doInBackground(IAsyncCallBack... params) {

		this.params = params;
		this.result = new boolean[this.params.length];

		for (int i = 0; i < this.params.length; i++) {
			this.result[i] = this.params[i].workToDo();
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {

		for (int i = 0; i < this.params.length; i++) {
			if (this.result[i])
				this.params[i].onComplete();
			else {
				this.params[i].onError();
			}
		}
	}
}
