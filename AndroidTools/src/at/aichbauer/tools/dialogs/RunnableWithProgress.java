package at.aichbauer.tools.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

public abstract class RunnableWithProgress {
	private Activity activity;
	private ProgressDialog dialog;

	public RunnableWithProgress(Activity activity) {
		this.activity = activity;
	}

	public Activity getActivity() {
		return this.activity;
	}

	public void setProgressMessage(final String message) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Log.d(DialogTools.class.getSimpleName(), Thread.currentThread().toString());
				dialog.setMessage(message);
			}
		});
	}

	protected void setProgressDialog(ProgressDialog dialog) {
		this.dialog = dialog;
	}
	
	public void setProgressTitle(final int titleResource) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dialog.setTitle(activity.getString(titleResource));
			}
		});
	}
	
	public void setProgressTitle(final String title) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dialog.setTitle(title);
			}
		});
	}

	public void setProgressMessage(final int resource) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dialog.setMessage(activity.getString(resource));
			}
		});
	}

	public void setProgress(final int progress) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dialog.setProgress(progress);
			}
		});
	}

	public void incrementProgress(final int progress) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dialog.incrementProgressBy(progress);
			}
		});
	}

	public abstract void run(ProgressDialog dialog);
	
	public int getProgress() {
		return -1;
	}
}
