/**
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

import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStamp;

import org.sufficientlysecure.ical.AndroidVEventWrapper.IAndroidWrapper;
import org.sufficientlysecure.ical.AndroidVEventWrapper.IVEventWrapper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;

@SuppressLint("NewApi")
public class VEventWrapper {
    private static String TAG = VEventWrapper.class.getSimpleName();

    private static String[] keys = new String[] { "rrule", "summary", "description", "location",
            "dtstart", "dtend" };// "organizer",

    public static VEvent resolve(Cursor c) {
        PropertyList properties = new PropertyList();
        AndroidVEventWrapper wrapperInstance = AndroidVEventWrapper.getInstance();
        for (String key : keys) {
            IAndroidWrapper wrapper = wrapperInstance.getAndroidWrapper(key);
            wrapper.wrap(properties, c);
        }

        VEvent vevent = new VEvent(properties);
        vevent.getProperties().add(new DtStamp());
        Log.d(TAG, "VEvent resolved from cursor");
        return vevent;
    }

    public static ContentValues resolve(VEvent vevent, int calendar_id) {
        ContentValues values = new ContentValues();
        AndroidVEventWrapper wrapperInstance = AndroidVEventWrapper.getInstance();
        for (String key : keys) {
            IVEventWrapper wrapper = wrapperInstance.getVEventWrapper(key);
            wrapper.wrap(values, vevent);
        }
        values.put(CalendarContract.Events.CALENDAR_ID, calendar_id);
        Log.d(TAG, "VEvent ready to insert into db");
        return values;
    }
}
