package at.aichbauer.ical;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Build;

public class Reminder {
	public static final String _ID = "_id";
	public static final String EVENT_ID = "event_id";
	public static final String MINUTES = "minutes";
	// Defaul 1
	public static final String METHOD = "method";

	private List<Integer> reminders;

	public Reminder() {
		this.reminders = new ArrayList<Integer>();
	}

	public void addReminder(int time_in_minutes) {
		if (!this.reminders.contains(time_in_minutes)) {
			this.reminders.add(time_in_minutes);
		}
	}

	public List<Integer> getReminders() {
		return reminders;
	}

	public static Uri getContentURI() {
		if (Build.VERSION.SDK_INT <= 7) {
			return Uri.parse("content://calendar/reminders");
		} else {
			return Uri.parse("content://com.android.calendar/reminders");
		}
	}
}
