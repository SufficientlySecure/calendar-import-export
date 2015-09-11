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
import java.util.Locale;
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
import org.sufficientlysecure.ical.util.Log;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CalendarContractWrapper.Events;
import android.provider.CalendarContractWrapper.Reminders;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.EditText;
import android.database.DatabaseUtils;

@SuppressLint("NewApi")
public class SaveCalendar extends RunnableWithProgress {
    private static final String TAG = SaveCalendar.class.getSimpleName();

    private AndroidCalendar mAndroidCalendar;
    private PropertyFactoryImpl mPropertyFactory = PropertyFactoryImpl.getInstance();
    private TimeZoneRegistry mTzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    private Set<TimeZone> mInsertedTimeZones = new HashSet<>();

    private static final List<String> STATUS_ENUM = Arrays.asList("TENTATIVE", "CONFIRMED", "CANCELLED");
    private static final List<String> CLASS_ENUM = Arrays.asList(null, "CONFIDENTIAL", "PRIVATE", "PUBLIC");
    private static final List<String> AVAIL_ENUM = Arrays.asList(null, "FREE", "BUSY-TENTATIVE");

    private static final String[] EVENT_COLS = new String[] {
        Events._ID, Events.ORIGINAL_ID, Events.UID_2445, Events.TITLE, Events.DESCRIPTION,
        Events.ORGANIZER, Events.EVENT_LOCATION, Events.STATUS, Events.ALL_DAY, Events.RDATE,
        Events.RRULE, Events.DTSTART, Events.EVENT_TIMEZONE, Events.DURATION, Events.DTEND,
        Events.EVENT_END_TIMEZONE, Events.ACCESS_LEVEL, Events.AVAILABILITY, Events.EXDATE,
        Events.EXRULE, Events.CUSTOM_APP_PACKAGE, Events.CUSTOM_APP_URI, Events.HAS_ALARM
    };

    private static final String[] REMINDER_COLS = new String[] {
        Reminders.MINUTES, Reminders.METHOD
    };

    public SaveCalendar(Activity activity) {
        super(activity, ProgressDialog.STYLE_HORIZONTAL);
        mAndroidCalendar = ((MainActivity) activity).getSelectedCalendar();
    }

    public void run() {
        MainActivity activity = (MainActivity) getActivity();
        Settings settings = activity.getSettings();

        mInsertedTimeZones.clear();

        String file = getFile(settings.getString(Settings.PREF_LASTEXPORTFILE));
        if (TextUtils.isEmpty(file))
            return;

        settings.putString(Settings.PREF_LASTEXPORTFILE, file);
        if (!file.endsWith(".ics"))
            file += ".ics";

        String output = Environment.getExternalStorageDirectory() + File.separator + file;
        int i = 0;
        setMessage(R.string.loading_calendar_entries);

        // query events
        ContentResolver resolver = activity.getContentResolver();
        String where = Events.CALENDAR_ID + "=?";
        String[] args = new String[] { mAndroidCalendar.mIdStr };
        Cursor cur = resolver.query(Events.CONTENT_URI, EVENT_COLS, where, args, null);
        setMax(cur.getCount());

        boolean relaxed = settings.getIcal4jValidationRelaxed();
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, relaxed);

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
        List<VEvent> events = new ArrayList<>();

        while (cur.moveToNext()) {
            incrementProgressBy(1);
            VEvent e = convertFromDb(cur, activity, cal, timestamp);
            if (e != null) {
                events.add(e);
                if (Log.getIsUserEnabled())
                    Log.d(TAG, "Adding event: " + e.toString());
            }
            i++;
        }
        cur.close();

        for (VEvent v: events)
            cal.getComponents().add(v);

        try {
            setMessage(R.string.writing_calendar_to_file);
            new CalendarOutputter().output(cal, new FileOutputStream(output));

            Resources res = activity.getResources();
            String msg = res.getQuantityString(R.plurals.wrote_n_events_to, i, i, file);
            activity.showToast(msg);

        } catch (Exception e) {
            Log.e(TAG, "SaveCalendar", e);

            DialogTools.info(activity, R.string.error, "Error:\n" + e.getMessage());
        }
    }

    private void getFileImpl(final String previousFile, final String[] result) {

        final EditText input = new EditText(getActivity());
        input.setText(previousFile);
        input.selectAll();

        final int ok = android.R.string.ok;
        final int cancel = android.R.string.cancel;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog dlg = builder.setTitle(R.string.enter_filename)
                                 .setMessage(R.string.please_enter_filename)
                                 .setView(input)
                                 .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface iface, int id) {
                                                         result[0] = input.getText().toString();
                                                     }
                                                 })
                                 .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface iface, int id) {
                                                         result[0] = "";
                                                     }
                                                 })
                                 .create();
        int state = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
        dlg.getWindow().setSoftInputMode(state);
        dlg.show();
    }

    private String getFile(final String previousFile) {
        final String[] result = new String[1];
        getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            getFileImpl(previousFile, result);
                                        }
                                   });
        while (result[0] == null) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {
            }
        }
        return result[0];
    }

    private VEvent convertFromDb(Cursor cur, MainActivity activity, Calendar cal, DtStamp timestamp) {
        if (Log.getIsUserEnabled())
            Log.d(TAG, "cursor: " + DatabaseUtils.dumpCurrentRowToString(cur));

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
        boolean isTransparent;
        DtEnd dtEnd = null;

        if (allDay) {
            // All day event
            isTransparent = true;
            Date start = getDateTime(cur, Events.DTSTART, null, null);
            l.add(new DtStart(start));
            dtEnd = new DtEnd(utcDateFromMs(start.getTime() + DateUtils.DAY_IN_MILLIS));
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
        if (availability > Events.AVAILABILITY_TENTATIVE)
            availability = -1;     // Unknown/Invalid

        if (isTransparent) {
            // This event is ordinarily transparent. If availability shows that its
            // not free, then mark it opaque.
            if (availability >= 0 && availability != Events.AVAILABILITY_FREE)
                l.add(Transp.OPAQUE);

        } else if (availability >= 0 && availability != Events.AVAILABILITY_BUSY) {
            // This event is ordinarily busy but differs, so output a FREEBUSY
            // period covering the time of the event
            FreeBusy fb = new FreeBusy();
            fb.getParameters().add(new FbType(AVAIL_ENUM.get(availability)));
            DateTime start = dateTimeFromProperty((DtStart) l.getProperty(Property.DTSTART));

            if (dtEnd != null)
                fb.getPeriods().add(new Period(start, dateTimeFromProperty(dtEnd)));
            else {
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
            long eventId = getLong(cur, Events._ID);
            Cursor alarmCur = Reminders.query(resolver, eventId, REMINDER_COLS);
            while (alarmCur.moveToNext()) {
                int mins = getInt(alarmCur, Reminders.MINUTES);
                if (mins == -1)
                    mins = 60;     // FIXME: Get the real default

                // FIXME: We should support other types if possible
                int method = getInt(alarmCur, Reminders.METHOD);
                if (method == Reminders.METHOD_DEFAULT || method == Reminders.METHOD_ALERT) {
                    VAlarm alarm = new VAlarm(new Dur(0, 0, -mins, 0));
                    alarm.getProperties().add(Action.DISPLAY);
                    alarm.getProperties().add(desc);
                    e.getAlarms().add(alarm);
                }
            }
            alarmCur.close();
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

    private long getLong(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getLong(i);
    }

    private int getInt(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getInt(i);
    }

    private boolean hasStringValue(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i != -1 && !TextUtils.isEmpty(cur.getString(i));
    }

    private Date utcDateFromMs(long ms) {
        // This date will be UTC provided the default false value of the iCal4j property
        // "net.fortuna.ical4j.timezone.date.floating" has not been changed.
        return new Date(ms);
    }

    private DateTime dateTimeFromProperty(DateProperty d) {
        if (d.getDate() instanceof DateTime)
            return (DateTime) (d.getDate());
        return new DateTime(d.getDate());
    }

    private boolean isUtcTimeZone(final String tz) {
        if (TextUtils.isEmpty(tz))
            return true;
        final String utz = tz.toUpperCase(Locale.US);
        return utz.equals("UTC") || utz.equals("UTC-0") || utz.equals("UTC+0") || utz.endsWith("/UTC");
    }

    private Date getDateTime(Cursor cur, String dbName, String dbTzName, Calendar cal) {
        int i = getColumnIndex(cur, dbName);
        if (i != -1 && !cur.isNull(i)) {
            if (cal == null)
                return utcDateFromMs(cur.getLong(i));     // Ignore timezone for date-only dates

            String tz = getString(cur, dbTzName);
            final boolean isUtc = isUtcTimeZone(tz);

            DateTime dt = new DateTime(isUtc);
            dt.setTime(cur.getLong(i));

            if (!isUtc) {
                TimeZone t = mTzRegistry.getTimeZone(tz);
                dt.setTimeZone(t);
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
        } catch (IOException | URISyntaxException | ParseException ignored) {
        }
        return null;
    }

    private void copyEnumProperty(PropertyList l, String evName, Cursor cur, String dbName,
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
                }
            }
        } catch (IOException | URISyntaxException | ParseException ignored) {
        }
    }
}
