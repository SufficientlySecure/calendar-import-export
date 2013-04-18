package at.aichbauer.tools.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DialogTools {
	private DialogTools() {

	}

	public static void showInformationDialog(final Activity activity, final int title, final int message,
			final int drawableResource) {
		showInformationDialog(activity, activity.getString(title), activity.getString(message), drawableResource);
	}

	public static void showInformationDialog(final Activity activity, final CharSequence title,
			final CharSequence message, final int drawableResource) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog dialog = new AlertDialog.Builder(activity).setMessage(message).setIcon(drawableResource)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						}).setTitle(title).create();

				dialog.show();
			}
		});
	}

	public static ProgressDialog runWithProgress(Context context, final RunnableWithProgress runnable,
			boolean isCancelable) {
		return runWithProgress(context, runnable, isCancelable, ProgressDialog.STYLE_SPINNER);
	}

	public static ProgressDialog runWithProgress(final Context context, final RunnableWithProgress runnable,
			final boolean isCancelable, final int style) {
		final ProgressDialog dialog = new ProgressDialog(context);
		dialog.setProgressStyle(style);
		dialog.setCancelable(isCancelable);
		dialog.setMessage("");
		dialog.setTitle("");
		dialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				runnable.setProgressDialog(dialog);
				runnable.run(dialog);
				dialog.cancel();
			}
		}).start();
		return dialog;
	}

	public static String questionDialog(Activity activity, int titleResource, int messageResource, int okResource,
			String input, boolean cancelable, int drawableResource, boolean password) {
		return questionDialog(activity, activity.getString(titleResource), activity.getString(messageResource),
				activity.getString(okResource), input, cancelable, drawableResource, password);
	}

	public static String questionDialog(final Activity activity, final CharSequence title, final CharSequence message,
			final CharSequence ok, final String input, final boolean cancelable, final int drawableResource,
			final boolean password) {
		final String[] result = new String[2];
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Dialog dialog = new Dialog(activity);
				dialog.setTitle(title);
				dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);

				LinearLayout layout = new LinearLayout(activity);
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				layout.setMinimumWidth(300);

				TextView view = new TextView(activity);
				view.setPadding(10, 10, 10, 10);
				view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				view.setTextSize(16);
				layout.addView(view);
				view.setText(message);

				final EditText editText = new EditText(activity);
				if (password) {
					editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
				} else {
					editText.setSingleLine();
				}
				editText.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				if (input != null) {
					editText.setText(input);
				}
				layout.addView(editText);

				
				LinearLayout buttons = new LinearLayout(activity);
				buttons.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				buttons.setBackgroundColor(Color.GRAY);
				buttons.setOrientation(LinearLayout.HORIZONTAL);
				
				layout.addView(buttons);
				
				Button button = new Button(activity);
				button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
				button.setGravity(Gravity.CENTER_HORIZONTAL);
				button.setText(ok);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						result[0] = editText.getText().toString();
						result[1] = "";
						dialog.cancel();
					}
				});

				buttons.addView(button);

				if (cancelable) {
					button = new Button(activity);
					button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
					button.setGravity(Gravity.CENTER_HORIZONTAL);
					button.setText("Cancel");
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							result[1] = "";
							dialog.cancel();
						}
					});
				}

				buttons.addView(button);

				dialog.setCancelable(cancelable);
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						result[1] = "";
					}
				});

				dialog.setContentView(layout);
				dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, drawableResource);
				dialog.show();
			}
		});
		while (result[1] == null) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
			}
		}
		return result[0];
	}

	public static boolean decisionDialog(Activity activity, int titleResource, int messageResource, int yesResource,
			int noResource, int drawableResource) {
		return decisionDialog(activity, activity.getString(titleResource), activity.getString(messageResource),
				activity.getString(yesResource), activity.getString(noResource), drawableResource);
	}

	public static boolean decisionDialog(final Activity activity, final CharSequence title, final CharSequence message,
			final CharSequence yes, final CharSequence no, final int drawableResource) {

		final boolean[] result = new boolean[1];
		final Object[] obj = new Object[1];
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				AlertDialog dialog = builder.setMessage(message).setPositiveButton(yes, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						result[0] = true;
						obj[0] = new Object();
						dialog.cancel();
					}
				}).setNegativeButton(no, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						result[0] = false;
						obj[0] = new Object();
						dialog.cancel();
					}
				}).setTitle(title).setIcon(drawableResource).create();

				dialog.show();
			}
		});
		while (obj[0] == null) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
			}
		}
		return result[0];
	}
}
