/**
 *  Copyright (C) 2015  Jon Griffiths (jon_p_griffiths@yahoo.com)
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

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class SaveCalendar extends RunnableWithProgress {
    private static final String TAG = SaveCalendar.class.getSimpleName();
    private static final String PREF_FILE = "lastExportFile";

    private AndroidCalendar androidCalendar;
    private PropertyFactoryImpl factory = PropertyFactoryImpl.getInstance();
    TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    Set<TimeZone> insertedTimeZones = new HashSet<TimeZone>();

    public SaveCalendar(Activity activity) {
        super(activity);
        androidCalendar = ((MainActivity)activity).getSelectedCalendar();
    }

    @Override
    public void run(ProgressDialog dialog) {
        MainActivity activity = (MainActivity)getActivity();

        insertedTimeZones.clear();

        String file = DialogTools.ask(activity, R.string.dialog_choosefilename_title,
                                      R.string.dialog_choosefilename_message,
                                      activity.preferences.getString(PREF_FILE, ""), true, false);
        if (TextUtils.isEmpty(file)) {
            return;
        }
        activity.preferences.edit().putString(PREF_FILE, file).commit();
        if (!file.endsWith(".ics")) {
            file += ".ics";
        }

        String output = Environment.getExternalStorageDirectory() + File.separator + file;
        int i = 0;
        setMessage(R.string.progress_loading_calendarentries);

        // query events
        ContentResolver resolver = activity.getContentResolver();
        String where = Events.CALENDAR_ID + "=?";
        String[] args = new String[] { Integer.toString(androidCalendar.id) };
        Cursor cur = resolver.query(Events.CONTENT_URI, null, where, args, null);
        dialog.setMax(cur.getCount());

        Calendar cal = new Calendar();
        String name = activity.getPackageName();
        String ver;
        try {
            ver = activity.getPackageManager().getPackageInfo(name, 0).versionName;
        } catch (NameNotFoundException e) {
            ver = "Unknown Build";
        }
        String prodId = "-//" + androidCalendar.owner + "//iCal Import/Export " + ver + "//EN";
        cal.getProperties().add(new ProdId(prodId));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);
        if (androidCalendar.timezone != null) {
            // FIXME: set X-WR-TIMEZONE
        }

        DtStamp timestamp = new DtStamp(); // Same timestamp for all events

        // Collect up events and add them after any timezones
        List<VEvent> events = new ArrayList<VEvent>();

        while (cur.moveToNext()) {
            try {
                incrementProgressBy(1);
                VEvent e = convertFromDb(cur, activity, cal, timestamp);
                events.add(e);
                Log.d(TAG, "Adding event: " + e.toString());
                i++;
            } catch (IOException e) {
            }
        }
        cur.close();

        for (VEvent v : events) {
            cal.getComponents().add(v);
        }

        String key = "ical4j.validation.relaxed";
        CompatibilityHints.setHintEnabled(key, activity.preferences.getBoolean(key, true));

        CalendarOutputter outputter = new CalendarOutputter();
        Resources res = activity.getResources();
        try {
            setMessage(R.string.progress_writing_calendar_to_file);
            outputter.output(cal, new FileOutputStream(output));

            String msg = res.getQuantityString(R.plurals.dialog_sucessfully_written_calendar,
                                               i, i, file);
            activity.showToast(msg);

        } catch (Exception e) {
            Log.e(TAG, "SaveCalendar", e);

            DialogTools.info(activity, R.string.dialog_bug_title, "Error:\n" + e.getMessage());
        }

    }

    private VEvent convertFromDb(Cursor cur, MainActivity activity, Calendar cal, DtStamp timestamp)
            throws IOException {
        PropertyList l = new PropertyList();

        l.add(timestamp);
        if (!copyProperty(l, Property.UID, cur, Events.UID_2445)) {
            // Generate a UID. Not ideal, since its not reproducable
            l.add(new Uid(activity.generateUid()));
        }

        copyProperty(l, Property.SUMMARY, cur, Events.TITLE);
        copyProperty(l, Property.DESCRIPTION, cur, Events.DESCRIPTION);
        copyProperty(l, Property.ORGANIZER, cur, Events.ORGANIZER);
        copyProperty(l, Property.LOCATION, cur, Events.EVENT_LOCATION);
        // FIXME: copyProperty(l, Property.STATUS, cur, Events.STATUS);

        boolean allDay = TextUtils.equals(getString(cur, Events.ALL_DAY), "1");
        if (allDay) {
            // All day event, no need to provide an end date or duration
            l.add(new DtStart(getDateTime(cur, Events.DTSTART, null, null)));
        } else {
            // Regular or zero-time event. Start date must be a date-time
            Date startDate = getDateTime(cur, Events.DTSTART, Events.EVENT_TIMEZONE, cal);
            l.add(new DtStart(startDate));

            // Use duration if we have one, otherwise end date
            if (!TextUtils.isEmpty(getString(cur, Events.DURATION))) {
                copyProperty(l, Property.DURATION, cur, Events.DURATION);
            } else {
                String endTz = Events.EVENT_END_TIMEZONE;
                if (endTz == null) {
                    endTz = Events.EVENT_TIMEZONE;
                }
                l.add(new DtEnd(getDateTime(cur, Events.DTEND, endTz, cal)));
            }
        }

        // FIXME: copyProperty(l, Property.CLASS, cur, Events.ACCESS_LEVEL);
        // FIXME: TRANSP
        // FIXME: copyProperty(l, Property.AVAILABILITY , cur, Events.AVAILABILITY );
        copyProperty(l, Property.RRULE, cur, Events.RRULE);
        copyProperty(l, Property.RDATE, cur, Events.RDATE);
        copyProperty(l, Property.EXRULE, cur, Events.EXRULE);
        copyProperty(l, Property.EXDATE, cur, Events.EXDATE);
        //copyProperty(l, Property.URL, cur, Events.CUSTOM_APP_URI);

        // FIXME: Alarms
        return new VEvent(l);
    }

    private int getColumnIndex(Cursor cur, String dbName) {
        return dbName == null ? -1 : cur.getColumnIndex(dbName);
    }

    private String getString(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? null : cur.getString(i);
    }

    private Date dateFromMs(long ms) {
        Date d = new Date();
        d.setTime(ms);
        return d;
    }

    private Date getDateTime(Cursor cur, String dbName, String dbTzName, Calendar cal) {
        int i = getColumnIndex(cur, dbName);
        if (i != -1 && !cur.isNull(i)) {
            if (cal == null) {
                return dateFromMs(cur.getLong(i)); // Ignore timezone for date-only dates
            }

            String tz = getString(cur, dbTzName);
            DateTime dt = new DateTime(true); // UTC
            dt.setTime(cur.getLong(i));
            if (!TextUtils.isEmpty(tz) && !TextUtils.equals(tz, Time.TIMEZONE_UTC)) {
                TimeZone t = tzRegistry.getTimeZone(tz);
                dt.setTimeZone(t);
                // Calendar doesn't prevent multiple additions of the same TZ, so check here.
                if (!insertedTimeZones.contains(t)) {
                    cal.getComponents().add(t.getVTimeZone());
                    insertedTimeZones.add(t);
                }
            }
            return dt;
        }
        return null;
    }

    private boolean copyProperty(PropertyList l, String evName, Cursor cur, String dbName) {
        // None of the exceptions caught below should be able to be thrown AFAICS.
        try {
            String value = getString(cur, dbName);
            if (value != null) {
                Property p = factory.createProperty(evName);
                p.setValue(value);
                l.add(p);
                return true;
            }
        } catch (IOException e) {
        } catch (URISyntaxException e) {
        } catch (ParseException e) {
        }
        return false;
    }
}
