package at.aichbauer.ical;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

public class GoogleCalendar {
	public static final String ID = "_id";
	public static final String SYNC_ACCOUNT = "_sync_account";
	public static final String SYNC_ACCOUNT_TYPE = "_sync_account_type";
	public static final String SYNC_ID = "_sync_id";
	public static final String SYNC_VERSION = "_sync_version";
	public static final String SYNC_TIME = "_sync_time";
	public static final String SYNC_LOCAL_ID = "_sync_local_id";
	public static final String SYNC_DIRTY = "_sync_dirty";
	public static final String SYNC_MARK = "_sync_mark";
	public static final String URL = "url";
	public static final String NAME = "name";
	public static final String DISPLAY_NAME = "displayName";
	public static final String HIDDEN = "hidden";
	public static final String COLOR = "color";
	public static final String ACCESS_LEVEL = "access_level";
	public static final String SELECTED = "selected";
	public static final String SYNC_EVENTS = "sync_events";
	public static final String LOCATION = "location";
	public static final String TIMEZONE = "timezone";
	public static final String OWNERACCOUNT = "ownerAccount";
	public static final Uri CONTENT_URI_PRE_8 = Uri.parse("content://calendar/calendars");
	public static final Uri CONTENT_URI = Uri.parse("content://com.android.calendar/calendars");
	
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
		if(Build.VERSION.SDK_INT <= 7) {
			return CONTENT_URI_PRE_8;
		} else {
			return CONTENT_URI;
		}
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
