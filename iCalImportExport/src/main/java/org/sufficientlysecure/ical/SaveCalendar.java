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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.FreeBusy;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.model.Period;
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
import android.provider.CalendarContract.Reminders;
import android.text.format.Time;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class SaveCalendar extends RunnableWithProgress {
    private static final String TAG = SaveCalendar.class.getSimpleName();
    private static final String PREF_EXPORT_FILE = "lastExportFile";

    private AndroidCalendar mAndroidCalendar;
    private PropertyFactoryImpl mPropertyFactory = PropertyFactoryImpl.getInstance();
    private TimeZoneRegistry mTzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    private Set<TimeZone> mInsertedTimeZones = new HashSet<TimeZone>();

    private static final List<String> STATUS_ENUM = Arrays.asList("TENTATIVE", "CONFIRMED", "CANCELED");
    private static final List<String> CLASS_ENUM = Arrays.asList(null, "CONFIDENTIAL", "PRIVATE", "PUBLIC");
    private static final List<String> AVAIL_ENUM = Arrays.asList(null, "FREE", "BUSY-TENTATIVE");

    private static final String[] EVENT_COLS = new String[] {
        Events._ID, Events.ORIGINAL_ID, Events.UID_2445, Events.TITLE, Events.DESCRIPTION,
        Events.ORGANIZER, Events.EVENT_LOCATION, Events.STATUS, Events.ALL_DAY, Events.RDATE,
        Events.RRULE, Events.DTSTART, Events.EVENT_TIMEZONE, Events.DURATION, Events.DTEND,
        Events.EVENT_END_TIMEZONE, Events.ACCESS_LEVEL, Events.AVAILABILITY, Events.EXDATE,
        Events.EXRULE, Events.CUSTOM_APP_PACKAGE, Events.CUSTOM_APP_URI, Events.HAS_ALARM };

    private static final String[] REMINDER_COLS = new String[] {
        Reminders.MINUTES, Reminders.METHOD
    };

    public SaveCalendar(Activity activity) {
        super(activity);
        mAndroidCalendar = ((MainActivity) activity).getSelectedCalendar();
    }

    @Override
    public void run(ProgressDialog dialog) {
        MainActivity activity = (MainActivity) getActivity();

        mInsertedTimeZones.clear();

        String file = activity.preferences.getString(PREF_EXPORT_FILE, "");
        file = DialogTools.ask(activity, R.string.dialog_choosefilename_title,
                               R.string.dialog_choosefilename_message, file, true, false);
        if (TextUtils.isEmpty(file)) {
            return;
        }
        activity.preferences.edit().putString(PREF_EXPORT_FILE, file).commit();
        if (!file.endsWith(".ics")) {
            file += ".ics";
        }

        String output = Environment.getExternalStorageDirectory() + File.separator + file;
        int i = 0;
        setMessage(R.string.progress_loading_calendarentries);

        // query events
        ContentResolver resolver = activity.getContentResolver();
        String where = Events.CALENDAR_ID + "=?";
        String[] args = new String[] { mAndroidCalendar.mIdStr };
        Cursor cur = resolver.query(Events.CONTENT_URI, EVENT_COLS, where, args, null);
        dialog.setMax(cur.getCount());

        String key = "ical4j.validation.relaxed";
        CompatibilityHints.setHintEnabled(key, activity.preferences.getBoolean(key, true));

        Calendar cal = new Calendar();
        String name = activity.getPackageName();
        String ver;
        try {
            ver = activity.getPackageManager().getPackageInfo(name, 0).versionName;
        } catch (NameNotFoundException e) {
            ver = "Unknown Build";
        }
        String prodId = "-//" + mAndroidCalendar.mOwner + "//iCal Import/Export " + ver + "//EN";
        cal.getProperties().add(new ProdId(prodId));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);
        if (mAndroidCalendar.mTimezone != null) {
            // We don't write any events with floating times, but export this
            // anyway so the default timezone for new events is correct when
            // the file is imported into a system that supports it.
            cal.getProperties().add(new XProperty("X-WR-TIMEZONE", mAndroidCalendar.mTimezone));
        }

        DtStamp timestamp = new DtStamp(); // Same timestamp for all events

        // Collect up events and add them after any timezones
        List<VEvent> events = new ArrayList<VEvent>();

        while (cur.moveToNext()) {
            try {
                incrementProgressBy(1);
                VEvent e = convertFromDb(cur, activity, cal, timestamp);
                if (e != null) {
                    events.add(e);
                    Log.d(TAG, "Adding event: " + e.toString());
                }
                i++;
            } catch (IOException e) {
            }
        }
        cur.close();

        for (VEvent v: events) {
            cal.getComponents().add(v);
        }

        try {
            setMessage(R.string.progress_writing_calendar_to_file);
            new CalendarOutputter().output(cal, new FileOutputStream(output));

            Resources res = activity.getResources();
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
        //String cursorContents = DatabaseUtils.dumpCurrentRowToString(cur);
        //Log.d(TAG, "cursor: " + cursorContents);

        if (hasStringValue(cur, Events.ORIGINAL_ID)) {
            // FIXME: Support these edited instances
            Log.d(TAG, "Ignoring edited instance of a recurring event");
            return null;
        }

        PropertyList l = new PropertyList();
        l.add(timestamp);
        if (copyProperty(l, Property.UID, cur, Events.UID_2445) == null) {
            // Generate a UID. Not ideal, since its not reproducible
            l.add(new Uid(activity.generateUid()));
        }

        String summary = copyProperty(l, Property.SUMMARY, cur, Events.TITLE);
        String description = copyProperty(l, Property.DESCRIPTION, cur, Events.DESCRIPTION);
        copyProperty(l, Property.ORGANIZER, cur, Events.ORGANIZER);
        copyProperty(l, Property.LOCATION, cur, Events.EVENT_LOCATION);
        copyEnumProperty(l, Property.STATUS, cur, Events.STATUS, STATUS_ENUM);

        boolean allDay = TextUtils.equals(getString(cur, Events.ALL_DAY), "1");
        boolean isRecurring = hasStringValue(cur, Events.RRULE)
                              || hasStringValue(cur, Events.RDATE);
        boolean isTransparent;
        DtEnd dtEnd = null;

        if (allDay) {
            // All day event
            isTransparent = true;
            Date start = getDateTime(cur, Events.DTSTART, null, null);
            l.add(new DtStart(start));
            dtEnd = new DtEnd(utcDateFromMs(start.getTime() + 86400000));
            l.add(dtEnd);
        } else {
            // Regular or zero-time event. Start date must be a date-time
            Date startDate = getDateTime(cur, Events.DTSTART, Events.EVENT_TIMEZONE, cal);
            l.add(new DtStart(startDate));

            // Use duration if we have one, otherwise end date
            if (hasStringValue(cur, Events.DURATION)) {
                // FIXME: Are any other values used for 0 durations?
                isTransparent = getString(cur, Events.DURATION).equals("PT0S");
                if (!isTransparent) {
                    copyProperty(l, Property.DURATION, cur, Events.DURATION);
                }
            } else {
                String endTz = Events.EVENT_END_TIMEZONE;
                if (endTz == null) {
                    endTz = Events.EVENT_TIMEZONE;
                }
                Date end = getDateTime(cur, Events.DTEND, endTz, cal);
                dtEnd = new DtEnd(end);
                isTransparent = startDate.getTime() == end.getTime();
                if (!isTransparent) {
                    l.add(dtEnd);
                }
            }
        }

        copyEnumProperty(l, Property.CLASS, cur, Events.ACCESS_LEVEL, CLASS_ENUM);

        int availability = getInt(cur, Events.AVAILABILITY);
        if (availability > Events.AVAILABILITY_TENTATIVE) {
            availability = -1; // Unknown/Invalid
        }

        if (isTransparent) {
            // This event is ordinarily transparent. If availability shows that its
            // not free, then mark it opaque.
            if (availability >= 0 && availability != Events.AVAILABILITY_FREE) {
                l.add(Transp.OPAQUE);
            }
        } else if (availability >= 0 && availability != Events.AVAILABILITY_BUSY) {
            // This event is ordinarily busy but differs, so output a FREEBUSY
            // period covering the time of the event
            FreeBusy fb = new FreeBusy();
            fb.getParameters().add(new FbType(AVAIL_ENUM.get(availability)));
            DateTime start = dateTimeFromProperty((DtStart) l.getProperty(Property.DTSTART));

            if (dtEnd != null) {
                DateTime end = dateTimeFromProperty(dtEnd);
                fb.getPeriods().add(new Period(start, end));
            } else {
                Duration d = (Duration) l.getProperty(Property.DURATION);
                fb.getPeriods().add(new Period(start, d.getDuration()));
            }
        }

        copyProperty(l, Property.RRULE, cur, Events.RRULE);
        copyProperty(l, Property.RDATE, cur, Events.RDATE);
        copyProperty(l, Property.EXRULE, cur, Events.EXRULE);
        copyProperty(l, Property.EXDATE, cur, Events.EXDATE);
        if (TextUtils.isEmpty(getString(cur, Events.CUSTOM_APP_PACKAGE))) {
            // Only copy URL if there is no app i.e. we probably imported it.
            copyProperty(l, Property.URL, cur, Events.CUSTOM_APP_URI);
        }

        VEvent e = new VEvent(l);

        if (getInt(cur, Events.HAS_ALARM) == 1) {
            // Add alarms

            String s = summary == null ? (description == null ? "" : description) : summary;
            Description desc = new Description(s);

            ContentResolver resolver = activity.getContentResolver();
            int eventId = getInt(cur, Events._ID);
            Cursor alarmCur = Reminders.query(resolver, eventId, REMINDER_COLS);
            while (alarmCur.moveToNext()) {
                int mins = getInt(alarmCur, Reminders.MINUTES);
                if (mins == -1) {
                    mins = 60; // FIXME: Get the real default
                }
                // FIXME: We should support other types if possible
                int method = getInt(alarmCur, Reminders.METHOD);
                if (method == Reminders.METHOD_DEFAULT || method == Reminders.METHOD_ALERT) {
                    VAlarm alarm = new VAlarm(new Dur(0, 0, -mins, 0));
                    alarm.getProperties().add(Action.DISPLAY);
                    alarm.getProperties().add(desc);
                    e.getAlarms().add(alarm);
                }
            }
        }

        return e;
    }

    private int getColumnIndex(Cursor cur, String dbName) {
        return dbName == null ? -1 : cur.getColumnIndex(dbName);
    }

    private String getString(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? null : cur.getString(i);
    }

    private int getInt(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getInt(i);
    }

    private boolean hasValue(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i != -1 && !cur.isNull(i);
    }

    private boolean hasStringValue(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i != -1 && !TextUtils.isEmpty(cur.getString(i));
    }

    private Date utcDateFromMs(long ms) {
        Date d = new Date(ms);
        // FIXME: Does not being able to change the timezone here affect this?
        return d;
    }

    private DateTime dateTimeFromProperty(DateProperty d) {
        if (d.getDate() instanceof DateTime) {
            return (DateTime) (d.getDate());
        }
        return new DateTime(d.getDate());
    }

    private Date getDateTime(Cursor cur, String dbName, String dbTzName, Calendar cal) {
        int i = getColumnIndex(cur, dbName);
        if (i != -1 && !cur.isNull(i)) {
            if (cal == null) {
                return utcDateFromMs(cur.getLong(i)); // Ignore timezone for date-only dates
            }

            String tz = getString(cur, dbTzName);
            DateTime dt = new DateTime(true); // UTC
            dt.setTime(cur.getLong(i));
            if (!TextUtils.isEmpty(tz) && !TextUtils.equals(tz, Time.TIMEZONE_UTC)) {
                TimeZone t = mTzRegistry.getTimeZone(tz);
                dt.setTimeZone(t);
                // Calendar doesn't prevent multiple additions of the same TZ, so check here.
                if (!mInsertedTimeZones.contains(t)) {
                    cal.getComponents().add(t.getVTimeZone());
                    mInsertedTimeZones.add(t);
                }
            }
            return dt;
        }
        return null;
    }

    private String copyProperty(PropertyList l, String evName, Cursor cur, String dbName) {
        // None of the exceptions caught below should be able to be thrown AFAICS.
        try {
            String value = getString(cur, dbName);
            if (value != null) {
                Property p = mPropertyFactory.createProperty(evName);
                p.setValue(value);
                l.add(p);
                return value;
            }
        } catch (IOException e) {
        } catch (URISyntaxException e) {
        } catch (ParseException e) {
        }
        return null;
    }

    private boolean copyEnumProperty(PropertyList l, String evName, Cursor cur, String dbName,
                                     List<String> vals) {
        // None of the exceptions caught below should be able to be thrown AFAICS.
        try {
            int i = getColumnIndex(cur, dbName);
            if (i != -1 && !cur.isNull(i)) {
                int value = (int) cur.getLong(i);
                if (value >= 0 && value < vals.size() && vals.get(value) != null) {
                    Property p = mPropertyFactory.createProperty(evName);
                    p.setValue(vals.get(value));
                    l.add(p);
                    return true;
                }
            }
        } catch (IOException e) {
        } catch (URISyntaxException e) {
        } catch (ParseException e) {
        }
        return false;
    }
}
