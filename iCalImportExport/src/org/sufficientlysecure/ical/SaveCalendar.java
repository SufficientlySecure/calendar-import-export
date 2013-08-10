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
import java.io.FileOutputStream;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Log;

@SuppressLint("NewApi")
public class SaveCalendar extends RunnableWithProgress {
    private final String TAG = SaveCalendar.class.getSimpleName();

    private AndroidCalendar androidCalendar;

    public SaveCalendar(Activity activity, AndroidCalendar calendar) {
        super(activity);
        this.androidCalendar = calendar;
    }

    @Override
    public void run(ProgressDialog dialog) {
        String input = DialogTools.questionDialog(getActivity(),
                R.string.dialog_choosefilename_title, R.string.dialog_choosefilename_message,
                R.string.dialog_proceed, null, true, R.drawable.calendar, false);
        if (input == null || input.equals("")) {
            return;
        }
        if (!input.endsWith(".ics")) {
            input += ".ics";
        }
        String output = Environment.getExternalStorageDirectory() + File.separator + input;
        int i = 0;
        setProgressMessage(R.string.progress_loading_calendarentries);

        // query events
        Cursor c = getActivity().getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                null, CalendarContract.Events.CALENDAR_ID + " = ?",
                new String[] { Integer.toString(androidCalendar.getId()) }, null);
        dialog.setMax(c.getCount());

        // don't save empty calendars
        if (c.getCount() == 0) {
            DialogTools.showInformationDialog(getActivity(), R.string.dialog_information_title,
                    R.string.dialog_empty_calendar, R.drawable.calendar);
            return;
        }

        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId(androidCalendar.getOwnerAccount()));
        calendar.getProperties().add(Version.VERSION_2_0);
        // get timezone
        // TODO: Test
        if (androidCalendar.getTimezone() != null) {
            TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
            VTimeZone tz = registry.getTimeZone(androidCalendar.getTimezone()).getVTimeZone();

            calendar.getComponents().add(tz);
        }

        while (c.moveToNext()) {
            VEvent vevent = VEventWrapper.resolve(c);
            vevent.getProperties().add(new Uid((i + 1) + "+" + androidCalendar.getOwnerAccount()));
            calendar.getComponents().add(vevent);
            Log.d(TAG, "Adding event to calendar");
            incrementProgress(1);
            i++;
        }
        c.close();
        CalendarOutputter outputter = new CalendarOutputter();
        try {
            setProgressMessage(R.string.progress_writing_calendar_to_file);
            outputter.output(calendar, new FileOutputStream(output));

            DialogTools.showInformationDialog(getActivity(),
                    getActivity().getString(R.string.dialog_success_title), getActivity()
                            .getString(R.string.dialog_sucessfully_written_calendar, i, output),
                    R.drawable.calendar);
        } catch (Exception e) {
            Log.e(TAG, "SaveCalendar", e);

            DialogTools.showInformationDialog(getActivity(),
                    getActivity().getString(R.string.dialog_bug_title),
                    "Error:\n" + e.getMessage(), R.drawable.calendar);
        }

    }
}
