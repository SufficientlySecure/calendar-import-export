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
