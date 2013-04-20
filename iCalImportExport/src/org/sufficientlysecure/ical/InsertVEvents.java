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

import java.io.File;

import org.sufficientlysecure.ical.tools.dialogs.DialogTools;
import org.sufficientlysecure.ical.tools.providers.ProviderTools;

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
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class InsertVEvents extends ProcessVEvent {
    private static final String TAG = InsertVEvents.class.getSimpleName();

    public InsertVEvents(Activity activity, Calendar calendar, int calendarId) {
        super(activity, calendar, calendarId);
    }

    @Override
    public void run(ProgressDialog dialog) {
        try {
            if (!DialogTools.decisionDialog(getActivity(), R.string.dialog_information_title,
                    R.string.dialog_insert_entries, R.string.dialog_yes, R.string.dialog_no,
                    R.drawable.calendar)) {
                return;
            }
            boolean checkForDuplicates = DialogTools.decisionDialog(getActivity(),
                    R.string.dialog_information_title,
                    R.string.dialog_insert_search_for_duplicates, R.string.dialog_yes,
                    R.string.dialog_no, R.drawable.calendar);

            Reminder reminder = new Reminder();

            while (DialogTools.decisionDialog(getActivity(), "Reminder",
                    "Add a reminder? Will be used for all Events!",
                    getActivity().getString(android.R.string.yes),
                    getActivity().getString(android.R.string.no), R.drawable.calendar)) {
                String time_in_minutes = DialogTools.questionDialog(getActivity(), "Reminder",
                        "Insert minutes for reminding before event",
                        getActivity().getString(android.R.string.ok), "10", true,
                        R.drawable.calendar, false);
                try {
                    if (time_in_minutes != null) {
                        reminder.addReminder(Integer.parseInt(time_in_minutes));
                    }
                } catch (Exception exc) {
                    DialogTools.showInformationDialog(getActivity(),
                            getActivity().getString(R.string.dialog_bug_title),
                            "Minutes could not be parsed", R.drawable.calendar);
                }
            }

            setProgressMessage(R.string.progress_insert_entries);
            ComponentList vevents = getCalendar().getComponents(VEvent.VEVENT);

            dialog.setMax(vevents.size());
            ContentResolver resolver = getActivity().getContentResolver();
            int i = 0;
            int j = 0;
            for (Object event : vevents) {
                ContentValues values = VEventWrapper.resolve((VEvent) event, getCalendarId());
                if (reminder.getReminders().size() > 0) {
                    values.put(CalendarContract.Events.HAS_ALARM, 1);
                }
                if (!checkForDuplicates || !contains(values)) {
                    Uri uri = resolver.insert(VEventWrapper.getContentURI(), values);
                    Log.d(TAG, uri != null ? "Inserted calendar event: " + uri.toString()
                            : "Could not insert calendar event.");
                    if (uri != null) {
                        i += 1;
                        for (int time : reminder.getReminders()) {
                            int id = Integer.parseInt(uri.getLastPathSegment());
                            Log.d(TAG, "Inserting reminder for event with id: " + id);
                            ContentValues reminderValues = new ContentValues();
                            reminderValues.put(Reminder.EVENT_ID, id);
                            reminderValues.put(Reminder.MINUTES, time);
                            reminderValues.put(Reminder.METHOD, 1);
                            uri = resolver.insert(Reminder.getContentURI(), reminderValues);
                            Log.d(TAG, uri != null ? "Inserted reminder: " + uri.toString()
                                    : "Could not insert reminder.");
                        }
                    }
                } else {
                    j++;
                }
                incrementProgress(1);
            }

            String message = getActivity().getString(R.string.dialog_entries_inserted, i);
            if (checkForDuplicates) {
                message += "\n" + getActivity().getString(R.string.dialog_found_duplicates, j);
            }
            DialogTools.showInformationDialog(getActivity(),
                    getActivity().getString(R.string.dialog_information_title), message,
                    R.drawable.calendar);
        } catch (Exception exc) {
            try {
                ProviderTools.writeException(Environment.getExternalStorageDirectory()
                        + File.separator + "ical_error.log", exc);
            } catch (Exception e) {

            }
            DialogTools.showInformationDialog(getActivity(),
                    getActivity().getString(R.string.dialog_bug_title),
                    getActivity().getString(R.string.dialog_bug), R.drawable.calendar);
        }
    }
}
