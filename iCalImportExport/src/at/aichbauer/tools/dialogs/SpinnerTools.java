package at.aichbauer.tools.dialogs;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public abstract class SpinnerTools {
	private SpinnerTools() {

	}

	public static <E> void simpleSpinner(Context context, Spinner spinner, List<E> input) {
		ArrayAdapter<E> adapter = new ArrayAdapter<E>(context, android.R.layout.simple_spinner_item, input);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		if (input != null && input.size() != 0) {
			spinner.setVisibility(View.VISIBLE);
		}
	}

	public static <E> void simpleSpinnerInUI(final Activity activity, final Spinner spinner, final List<E> input) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				simpleSpinner(activity, spinner, input);
			}
		});
	}
}
