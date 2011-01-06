package at.aichbauer.ical;

import java.io.File;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import at.aichbauer.tools.dialogs.DialogTools;
import at.aichbauer.tools.providers.ProviderTools;

public class InsertVEvents extends ProcessVEvent {
	private static String LOG_ID = InsertVEvents.class.getSimpleName();

	public InsertVEvents(Activity activity, Calendar calendar, int calendarId) {
		super(activity, calendar, calendarId);
	}

	@Override
	public void run(ProgressDialog dialog) {
		try {
			if (!DialogTools.decisionDialog(getActivity(), R.string.dialog_information_title,
					R.string.dialog_insert_entries, R.string.dialog_yes, R.string.dialog_no, R.drawable.calendar)) {
				return;
			}
			boolean checkForDuplicates = DialogTools.decisionDialog(getActivity(), R.string.dialog_information_title,
					R.string.dialog_insert_search_for_duplicates, R.string.dialog_yes, R.string.dialog_no,
					R.drawable.calendar);

			setProgressMessage(R.string.progress_insert_entries);
			ComponentList vevents = getCalendar().getComponents(VEvent.VEVENT);

			dialog.setMax(vevents.size());
			int i = 0;
			int j = 0;
			for (Object event : vevents) {
				ContentValues values = VEventWrapper.resolve((VEvent) event, getCalendarId());
				if (!checkForDuplicates || !contains(values)) {
					Uri uri = getActivity().getContentResolver().insert(VEventWrapper.getContentURI(), values);
					Log.d(LOG_ID, uri != null ? "Inserted calendar event: " + uri.toString()
							: "Could not insert calendar event.");
					i += uri != null ? 1 : 0;

				} else {
					j++;
				}
				incrementProgress(1);
			}

			String message = getActivity().getString(R.string.dialog_entries_inserted, i);
			if (checkForDuplicates) {
				message += "\n" + getActivity().getString(R.string.dialog_found_duplicates, j);
			}
			DialogTools.showInformationDialog(getActivity(),
					getActivity().getString(R.string.dialog_information_title), message, R.drawable.calendar);
		} catch (Exception exc) {
			try {
				ProviderTools.writeException(Environment.getExternalStorageDirectory() + File.separator + "ical_error.log",
						exc);
			} catch (Exception e) {
				
			}
			DialogTools
					.showInformationDialog(
							getActivity(),
							"Error",
							"I am sorry for you inconvinience. Please send file ical_error.log located on your sd card to lukas.aichbauer@gmail.com.",
							R.drawable.calendar);
		}
	}
}
