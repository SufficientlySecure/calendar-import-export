package at.aichbauer.ical;

import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import at.aichbauer.tools.dialogs.RunnableWithProgress;

public abstract class ProcessVEvent extends RunnableWithProgress {
	private Calendar calendar;
	private int calendarId;
	private final String LOG_ID = ProcessVEvent.class.getSimpleName();

	public ProcessVEvent(Activity activity, Calendar calendar, int calendarId) {
		super(activity);
		this.calendar = calendar;
		this.calendarId = calendarId;
	}

	public Calendar getCalendar() {
		return calendar;
	}

	public int getCalendarId() {
		return calendarId;
	}

	public List<Integer> getIds(ContentValues cValues) {
		Cursor c = getFromContentValues(cValues);
		List<Integer> ids = new ArrayList<Integer>(c.getCount());
		while (c.moveToNext()) {
			ids.add(c.getInt(0));
		}
		c.close();
		return ids;
	}

	public boolean contains(ContentValues cValues) {
		Cursor c = getFromContentValues(cValues);
		int count = c.getCount();
		c.close();
		return count > 0;
	}

	private Cursor getFromContentValues(ContentValues cValues) {
		String where = "title = ? AND dtstart = ?";
		Log.d(LOG_ID, "title = " + cValues.getAsString("title") + " AND dtstart = " + cValues.getAsString("dtstart"));
		String[] values = new String[] { cValues.getAsString("title"), cValues.getAsString("dtstart") };
		Cursor c = getActivity().getContentResolver().query(VEventWrapper.getContentURI(), new String[] { "_id" },
				where, values, null);
		return c;

	}
}
