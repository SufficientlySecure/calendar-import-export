package at.aichbauer.ical;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

@SuppressLint("NewApi")
public class GoogleCalendar {
	public static final String ID = CalendarContract.Calendars._ID;
	public static final String SYNC_ACCOUNT = CalendarContract.Calendars.ACCOUNT_NAME;
	public static final String SYNC_ACCOUNT_TYPE = CalendarContract.Calendars.ACCOUNT_TYPE;
	public static final String SYNC_ID = CalendarContract.Calendars._SYNC_ID;
	public static final String SYNC_DIRTY = CalendarContract.Calendars.DIRTY;
	public static final String NAME = CalendarContract.Calendars.NAME;
	public static final String DISPLAY_NAME = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME;
	public static final String COLOR = CalendarContract.Calendars.CALENDAR_COLOR;
	public static final String ACCESS_LEVEL = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL;
	public static final String SELECTED = CalendarContract.Calendars.VISIBLE;
	public static final String SYNC_EVENTS = CalendarContract.Calendars.SYNC_EVENTS;
	public static final String LOCATION = CalendarContract.Calendars.CALENDAR_LOCATION;
	public static final String TIMEZONE = CalendarContract.Calendars.CALENDAR_TIME_ZONE;
	public static final String OWNERACCOUNT = CalendarContract.Calendars.OWNER_ACCOUNT;
	public static final Uri CONTENT_URI = CalendarContract.Calendars.CONTENT_URI;
	
	private int id;
	private String name;
	private String displayName;
	private String ownerAccount;
	private boolean isActive;

	private int entries;

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getOwnerAccount() {
		return ownerAccount;
	}

	public boolean isActive() {
		return isActive;
	}

	public int getId() {
		return id;
	}

	public static Uri getContentURI() {
			return CONTENT_URI;
	}

	public static GoogleCalendar retrieve(Cursor c) {
		GoogleCalendar calendar = new GoogleCalendar();
		calendar.id = c.getInt(c.getColumnIndex(ID));
		calendar.name = c.getString(c.getColumnIndex(NAME));
		calendar.displayName = c.getString(c.getColumnIndex(DISPLAY_NAME));
		calendar.ownerAccount = c.getString(c.getColumnIndex(OWNERACCOUNT));
		calendar.isActive = c.getInt(c.getColumnIndex(SELECTED)) == 1;
		return calendar;
	}

	public void setEntryCount(int entries) {
		this.entries = entries;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CalendarId: " + id);
		builder.append("\nEvents: " + entries);
		builder.append("\nDisplayName:\n" + displayName);
		builder.append("\nName:\n" + name);
		builder.append("\nOwner:\n" + ownerAccount);
		builder.append("\nIsActive:\n" + isActive + "\n");
		return builder.toString();
	}

	public String toHtml() {
		StringBuilder builder = new StringBuilder();
		builder.append("<b>CalendarId:</b><br>" + id);
		builder.append("<br><b>Events:</b><br>" + entries);
		builder.append("<br><b>DisplayName:</b><br>" + displayName);
		builder.append("<br><b>Name:</b><br>" + name);
		builder.append("<br><b>Owner:</b><br>" + ownerAccount);
		builder.append("<br><b>IsActive:</b><br>" + isActive + "<br>");
		return builder.toString();
	}
}
