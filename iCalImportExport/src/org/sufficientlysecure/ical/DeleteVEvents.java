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

import java.util.List;

import org.sufficientlysecure.ical.tools.dialogs.DialogTools;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeleteVEvents extends ProcessVEvent {

    public DeleteVEvents(Activity activity, Calendar calendar, int calendarId) {
        super(activity, calendar, calendarId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void run(ProgressDialog dialog) {
        if (!DialogTools.decisionDialog(getActivity(), R.string.dialog_information_title,
                R.string.dialog_delete_entries, R.string.dialog_yes, R.string.dialog_no,
                R.drawable.calendar)) {
            return;
        }
        setProgressMessage(R.string.progress_deleting_calendarentries);
        ComponentList vevents = getCalendar().getComponents(VEvent.VEVENT);

        dialog.setMax(vevents.size());

        int i = 0;
        ContentResolver contentResolver = getActivity().getContentResolver();
        for (Object event : vevents) {
            ContentValues values = VEventWrapper.resolve((VEvent) event, getCalendarId());
            List<Integer> ids = getIds(values);

            for (Integer id : ids) {
                i += contentResolver.delete(
                        Uri.withAppendedPath(VEventWrapper.getContentURI(), Integer.toString(id)),
                        null, null);
                // Delete reminder
                contentResolver.delete(Reminder.getContentURI(),
                        CalendarContract.Reminders.EVENT_ID + " = ?",
                        new String[] { Integer.toString(id) });
            }
            incrementProgress(1);
        }

        DialogTools.showInformationDialog(getActivity(),
                getActivity().getString(R.string.dialog_information_title), getActivity()
                        .getString(R.string.dialog_entries_deleted, i), R.drawable.calendar);
    }
}
