/**
 *  Copyright (C) 2013  Dominik Schürmann <dominik@dominikschuermann.de>
 *  Copyright (C) 2010-2011  Lukas Aichbauer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.ical;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

@SuppressLint("NewApi")
public class AndroidCalendar {
    public int mId;
    public String mIdStr;
    public String mName;
    public String mDisplayName;
    public String mAccountName;
    public String mAccountType;
    public String mOwner;
    public boolean mIsActive;
    public String mTimezone;
    public int mNumEntries;

    private static final String[] CAL_COLS = new String[] {
        Calendars._ID, Calendars.DELETED, Calendars.NAME, Calendars.CALENDAR_DISPLAY_NAME,
        Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE, Calendars.OWNER_ACCOUNT,
        Calendars.VISIBLE, Calendars.CALENDAR_TIME_ZONE };

    // Load all available calendars.
    // If an empty list is returned the caller probably needs to enable calendar
    // read permissions in App Ops/XPrivacy etc.
    public static List<AndroidCalendar> loadAll(ContentResolver resolver) {

        if (!haveProvider(resolver, Calendars.CONTENT_URI)
            || !haveProvider(resolver, Events.CONTENT_URI)) {
            return new ArrayList<AndroidCalendar>();
        }

        Cursor src = resolver.query(Calendars.CONTENT_URI, CAL_COLS, null, null, null);
        List<AndroidCalendar> calendars = new ArrayList<AndroidCalendar>(src.getCount());

        while (src.moveToNext()) {
            if (getInt(src, Calendars.DELETED) != 0) {
                continue;
            }
            AndroidCalendar calendar = new AndroidCalendar();
            calendar.mId = getInt(src, Calendars._ID);
            calendar.mIdStr = getString(src, Calendars._ID);
            calendar.mName = getString(src, Calendars.NAME);
            calendar.mDisplayName = getString(src, Calendars.CALENDAR_DISPLAY_NAME);
            calendar.mAccountName = getString(src, Calendars.ACCOUNT_NAME);
            calendar.mAccountType = getString(src, Calendars.ACCOUNT_TYPE);
            calendar.mOwner = getString(src, Calendars.OWNER_ACCOUNT);
            calendar.mIsActive = getInt(src, Calendars.VISIBLE) == 1;
            calendar.mTimezone = getString(src, Calendars.CALENDAR_TIME_ZONE);

            final String[] cols = new String[] { Events._ID };
            final String where = Events.CALENDAR_ID + "=?";
            final String[] args = new String[] { calendar.mIdStr };
            Cursor sizer = resolver.query(Events.CONTENT_URI, cols, where, args, null);
            calendar.mNumEntries = sizer.getCount();
            sizer.close();
            calendars.add(calendar);
        }
        src.close();

        return calendars;
    }

    private static int getInt(Cursor src, String columnName) {
        return src.getInt(src.getColumnIndex(columnName));
    }

    private static String getString(Cursor src, String columnName) {
        return src.getString(src.getColumnIndex(columnName));
    }

    private static boolean haveProvider(ContentResolver resolver, Uri uri) {
        // Check an individual provider is installed
        ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
        if (provider != null) {
            provider.release();
        }
        return provider != null;
    }

    @Override
    public String toString() {
        return mDisplayName + " (" + mIdStr + ")";
    }
}
