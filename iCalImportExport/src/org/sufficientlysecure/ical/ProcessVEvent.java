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

import org.sufficientlysecure.ical.tools.dialogs.RunnableWithProgress;

import net.fortuna.ical4j.model.Calendar;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
        String where = CalendarContract.Events.TITLE + " = ? AND "
                + CalendarContract.Events.DTSTART + " = ?";
        Log.d(LOG_ID,
                CalendarContract.Events.TITLE + " = "
                        + cValues.getAsString(CalendarContract.Events.TITLE) + " AND "
                        + CalendarContract.Events.DTSTART + " = "
                        + cValues.getAsString(CalendarContract.Events.DTSTART));
        String[] values = new String[] { cValues.getAsString(CalendarContract.Events.TITLE),
                cValues.getAsString(CalendarContract.Events.DTSTART) };
        Cursor c = getActivity().getContentResolver().query(VEventWrapper.getContentURI(),
                new String[] { CalendarContract.Events._ID }, where, values, null);
        return c;
    }
}
