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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.util.Log;

@SuppressLint("NewApi")
public class VEventWrapper {
    private static String TAG = VEventWrapper.class.getSimpleName();

    private static String[] keys = { "rrule", "summary", "description", "location",
            "dtstart", "dtend" }; // "organizer",

    public static VEvent resolve(Cursor c) {
        PropertyList properties = new PropertyList();
        AndroidVEventWrapper wrapperInstance = AndroidVEventWrapper.getInstance();
        for (String key : keys) {
            wrapperInstance.getAndroidWrapper(key).wrap(properties, c);
        }

        VEvent vevent = new VEvent(properties);
        vevent.getProperties().add(new DtStamp());
        Log.d(TAG, "VEvent resolved from cursor");
        return vevent;
    }
}
