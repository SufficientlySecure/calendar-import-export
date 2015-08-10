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

import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Log;

@SuppressLint("NewApi")
public class SaveCalendar extends RunnableWithProgress {
    private static final String TAG = SaveCalendar.class.getSimpleName();
    private static final String PREF_FILE = "export_filename";

    private AndroidCalendar androidCalendar;

    public SaveCalendar(Activity activity, AndroidCalendar calendar) {
        super(activity);
        this.androidCalendar = calendar;
    }

    @Override
    public void run(ProgressDialog dialog) {
        MainActivity activity = (MainActivity)getActivity();

        String file = DialogTools.questionDialog(activity,
                R.string.dialog_choosefilename_title, R.string.dialog_choosefilename_message,
                activity.preferences.getString(PREF_FILE, ""), true, false);
        if (file == null || file.equals("")) {
            return;
        }
        activity.preferences.edit().putString(PREF_FILE, file).commit();
        if (!file.endsWith(".ics")) {
            file += ".ics";
        }

        String output = Environment.getExternalStorageDirectory() + File.separator + file;
        int i = 0;
        setProgressMessage(R.string.progress_loading_calendarentries);

        // query events
        Cursor c = activity.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                null, CalendarContract.Events.CALENDAR_ID + " = ?",
                new String[] { Integer.toString(androidCalendar.getId()) }, null);
        dialog.setMax(c.getCount());

        // don't save empty calendars
        if (c.getCount() == 0) {
            DialogTools.showInformationDialog(activity, R.string.dialog_information_title,
                    R.string.dialog_empty_calendar, R.drawable.icon);
            return;
        }

        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId(androidCalendar.getOwnerAccount()));
        calendar.getProperties().add(Version.VERSION_2_0);
        // set calendar timezone, only defined on Google Calendars?
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
        Resources res = activity.getResources();
        try {
            setProgressMessage(R.string.progress_writing_calendar_to_file);
            outputter.output(calendar, new FileOutputStream(output));

            String txt = res.getQuantityString(R.plurals.dialog_sucessfully_written_calendar,
                    i, i, output);
            DialogTools.showInformationDialog(activity, R.string.dialog_success_title,
                    txt, R.drawable.icon);
        } catch (Exception e) {
            Log.e(TAG, "SaveCalendar", e);

            DialogTools.showInformationDialog(activity, R.string.dialog_bug_title,
                    "Error:\n" + e.getMessage(), R.drawable.icon);
        }

    }
}
