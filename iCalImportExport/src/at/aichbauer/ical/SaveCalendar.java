package at.aichbauer.ical;

import java.io.File;
import java.io.FileOutputStream;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import at.aichbauer.tools.dialogs.DialogTools;
import at.aichbauer.tools.dialogs.RunnableWithProgress;

public class SaveCalendar extends RunnableWithProgress {
	private final String LOG_ID = SaveCalendar.class.getSimpleName();

	private GoogleCalendar googleCalendar;

	public SaveCalendar(Activity activity, GoogleCalendar calendar) {
		super(activity);
		this.googleCalendar = calendar;
	}

	@Override
	public void run(ProgressDialog dialog) {
		String input = DialogTools.questionDialog(getActivity(), R.string.dialog_choosefilename_title,
				R.string.dialog_choosefilename_message, R.string.dialog_proceed,null, true, R.drawable.calendar);
		if (input == null || input.equals("")) {
			return;
		}
		if (!input.endsWith(".ics")) {
			input += ".ics";
		}
		String output = Environment.getExternalStorageDirectory() + File.separator + input;
		int i = 0;
		setProgressMessage(R.string.progress_loading_calendarentries);
		Cursor c = getActivity().getContentResolver().query(VEventWrapper.getContentURI(), null, "calendar_id  = ?",
				new String[] { Integer.toString(googleCalendar.getId()) }, null);
		dialog.setMax(c.getCount());
		if (c.getCount() == 0) {
			DialogTools.showInformationDialog(getActivity(), R.string.dialog_information_title,
					R.string.dialog_empty_calendar, R.drawable.calendar);
			return;
		}

		Calendar calendar = new Calendar();
		calendar.getProperties().add(new ProdId(googleCalendar.getOwnerAccount()));
		calendar.getProperties().add(Version.VERSION_2_0);
		while (c.moveToNext()) {
			VEvent vevent = VEventWrapper.resolve(c);
			vevent.getProperties().add(new Uid((i + 1) + "+" + googleCalendar.getOwnerAccount()));
			calendar.getComponents().add(vevent);
			Log.d(LOG_ID, "Adding event to calendar");
			incrementProgress(1);
			i++;
		}
		c.close();
		CalendarOutputter outputter = new CalendarOutputter();
		try {
			setProgressMessage(R.string.progress_writing_calendar_to_file);
			outputter.output(calendar, new FileOutputStream(output));
		} catch (Exception e) {
			setProgressMessage("Error:\n" + e.getMessage());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}
		DialogTools.showInformationDialog(getActivity(), getActivity().getString(R.string.dialog_success_title),
				getActivity().getString(R.string.dialog_sucessfully_written_calendar, i, output), R.drawable.calendar);
	}
}
