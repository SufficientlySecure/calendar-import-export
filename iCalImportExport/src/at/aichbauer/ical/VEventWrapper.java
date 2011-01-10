package at.aichbauer.ical;

import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStamp;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import at.aichbauer.ical.GoogleVEventWrapper.IGoogleWrapper;
import at.aichbauer.ical.GoogleVEventWrapper.IVEventWrapper;

public class VEventWrapper {
	private static String TAG = VEventWrapper.class.getSimpleName();
	
	private static String[] keys = new String[] { "organizer", "rrule", "summary", "description", "location",
			"dtstart", "dtend" };

	public static VEvent resolve(Cursor c) {
		PropertyList properties = new PropertyList();
		GoogleVEventWrapper wrapperInstance = GoogleVEventWrapper.getInstance();
		for (String key : keys) {
			IGoogleWrapper wrapper = wrapperInstance.getGoogleWrapper(key);
			wrapper.wrap(properties, c);
		}

		VEvent vevent = new VEvent(properties);
		vevent.getProperties().add(new DtStamp());
		Log.d(TAG, "VEvent resolved from cursor");
		return vevent;
	}

	public static ContentValues resolve(VEvent vevent, int calendar_id) {
		ContentValues values = new ContentValues();
		GoogleVEventWrapper wrapperInstance = GoogleVEventWrapper.getInstance();
		for (String key : keys) {
			IVEventWrapper wrapper = wrapperInstance.getVEventWrapper(key);
			wrapper.wrap(values, vevent);
		}
		values.put("calendar_id", calendar_id);
		Log.d(TAG, "VEvent ready to insert into db");
		return values;
	}

	public static Uri getContentURI() {
		if (Build.VERSION.SDK_INT <= 7) {
			return Uri.parse("content://calendar/events");
		} else {
			return Uri.parse("content://com.android.calendar/events");
		}
	}
}
