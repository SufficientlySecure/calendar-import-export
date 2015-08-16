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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.Related;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.Property;

import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;
import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.RemindersDialog;
import org.sufficientlysecure.ical.util.ProviderTools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.format.Time;
import android.util.Log;

@SuppressLint("NewApi")
public class ProcessVEvent extends RunnableWithProgress {
    private static final String TAG = ProcessVEvent.class.getSimpleName();

    private static final Duration oneDay = createDuration("P1D");
    private static final Duration zeroMins = createDuration("P0M");

    private Calendar iCalCalendar;
    private AndroidCalendar androidCalendar;
    private static boolean isInserter;

    public ProcessVEvent(Activity activity, Calendar iCalCalendar, AndroidCalendar androidCalendar,
            boolean isInserter) {
        super(activity);
        this.iCalCalendar = iCalCalendar;
        this.androidCalendar = androidCalendar;
        this.isInserter = isInserter;
    }

    @Override
    public void run(ProgressDialog dialog) {
        try {
            MainActivity activity = (MainActivity)getActivity();
            SharedPreferences prefs = activity.preferences;
            boolean checkForDuplicates = prefs.getBoolean("setting_import_no_dupes", true);
            boolean useReminders = prefs.getBoolean("setting_import_reminders", false);

            List<Integer> defReminders = RemindersDialog.getSavedRemindersInMinutes();
            List<Integer> reminders = new ArrayList<Integer>();

            setProgressMessage(R.string.progress_insert_entries);
            ComponentList vevents = iCalCalendar.getComponents(VEvent.VEVENT);

            dialog.setMax(vevents.size());
            ContentResolver resolver = activity.getContentResolver();
            int numDel = 0;
            int numIns = 0;
            int numDups = 0;

            ContentValues alarm = new ContentValues();
            alarm.put(Reminders.METHOD, Reminders.METHOD_ALERT);

            for (Object ve: vevents) {
                incrementProgress(1);

                VEvent e = (VEvent)ve;
                Log.d(TAG, "source event: " + e.toString());

                ContentValues c = convertToDB(e, defReminders, reminders, androidCalendar.id);

                if (!isInserter) {
                    Cursor cur = getFromContentValues(resolver, c);
                    while (cur.moveToNext()) {
                        String id = cur.getString(0);
                        Uri eventUri = Uri.withAppendedPath(Events.CONTENT_URI, id);
                        numDel += resolver.delete(eventUri, null, null);
                        String where = Reminders.EVENT_ID + "=?";
                        resolver.delete(Reminders.CONTENT_URI, where, new String[] { id });
                    }
                    cur.close();
                    continue;
                }

                if (checkForDuplicates && doesDbContain(resolver, c)) {
                    Log.d(TAG, "Ignoring duplicate event");
                    numDups++;
                    continue;
                }

                Log.d(TAG, "destination values: " + c);

                Uri uri = insertAndLog(resolver, Events.CONTENT_URI, c, "Event");
                if (uri == null) {
                    continue; // FIXME: Note the failure
                }
                final int id = Integer.parseInt(uri.getLastPathSegment());

                numIns++;

                for (int time : (useReminders && reminders.size() > 0 ? reminders : defReminders)) {
                    Log.d(TAG, "Inserting reminder for event with id: " + id);

                    alarm.put(Reminders.EVENT_ID, id);
                    alarm.put(Reminders.MINUTES, time);
                    insertAndLog(resolver, Reminders.CONTENT_URI, alarm, "Reminder");
                }
            }

            androidCalendar.numEntries += numIns;
            androidCalendar.numEntries -= numDel;
            activity.updateNumEntries(androidCalendar);

            Resources res = activity.getResources();
            int n = isInserter ? numIns : numDel;
            String msg = res.getQuantityString(R.plurals.dialog_entries_processed, n, n) + "\n";
            if (isInserter) {
                msg += "\n";
                if (checkForDuplicates) {
                    msg += res.getQuantityString(R.plurals.dialog_found_duplicates, numDups, numDups);
                } else {
                    msg += res.getString(R.string.dialog_did_not_check_dupes);
                }
            }

            DialogTools.showInformationDialog(activity, R.string.dialog_information_title,
                    msg, R.drawable.icon);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "ProcessVEvent", e);
            try {
                ProviderTools.writeException(Environment.getExternalStorageDirectory()
                        + File.separator + "ical_error.log", e);
            } catch (Exception ignored) {

            }
            DialogTools.showInformationDialog(getActivity(), R.string.dialog_bug_title,
                    R.string.dialog_bug, R.drawable.icon);
        }
    }

    private ContentValues convertToDB(VEvent e, List<Integer> defReminders,
                                      List<Integer> reminders, int calendarId) {

        reminders.clear();
        boolean allDay = false;

        // Munge a VEvent so Android doesn't reject it once converted
        if (!hasProperty(e, Property.DTEND) && !hasProperty(e, Property.DURATION)) {
            // From RFC 2445:
            // - If the start date is a date, the event lasts all day, midnight to midnight.
            // - If the start date is a datetime, the event lasts no time (end = start).
            // However according to the Android calendar docs,
            //  - You cannot provide both a DURATION and DTEND.
            //  - If an event is recurring it must have a DURATION, otherwise it must have DTEND.
            //  - If an event is marked as all day it must be in the UTC timezone.
            //
            boolean startIsDate = !(e.getStartDate().getDate() instanceof DateTime);
            boolean isRecurring = hasProperty(e, Property.RRULE) || hasProperty(e, Property.RDATE);

            if (startIsDate) {
                e.getProperties().add(oneDay);
                allDay = true;
                e.getStartDate().setUtc(true);
            } else {
                e.getProperties().add(zeroMins);
                // Zero time events are always free time so override/set TRANSP accordingly
                e.getProperties().remove(e.getProperty(Property.TRANSP));
                e.getProperties().add(Transp.TRANSPARENT);
            }

            if (!isRecurring) {
                // Calculate end date from duration, set it and remove the duration.
                e.getProperties().add(e.getEndDate());
                e.getProperties().remove(e.getProperty(Property.DURATION));
            }
        }

        // Now populate and return the db values for the event
        ContentValues c = new ContentValues();

        c.put(Events.CALENDAR_ID, calendarId);
        copyProperty(c, Events.TITLE, e, Property.SUMMARY);
        copyProperty(c, Events.DESCRIPTION, e, Property.DESCRIPTION);

        if (hasProperty(e, Property.ORGANIZER)) {
            copyProperty(c, Events.ORGANIZER, e, Property.ORGANIZER);
            c.put(Events.GUESTS_CAN_MODIFY, 1); // Ensure we can edit the item if not the organiser
        }

        copyProperty(c, Events.EVENT_LOCATION, e, Property.LOCATION);

        if (hasProperty(e, Property.STATUS))
        {
            String status = e.getProperty(Property.STATUS).getValue();
            if (status.equals("TENTATIVE")) {
                c.put(Events.STATUS, Events.STATUS_TENTATIVE);
            } else if (status.equals("CONFIRMED")) {
                c.put(Events.STATUS, Events.STATUS_CONFIRMED);
            } else if (status.equals("CANCELED")) {
                c.put(Events.STATUS, Events.STATUS_CANCELED);
            }
        }

        copyProperty(c, Events.DURATION, e, Property.DURATION);

        if (allDay) {
            c.put(Events.ALL_DAY, 1);
        }

        copyDateProperty(c, Events.DTSTART, Events.EVENT_TIMEZONE, e.getStartDate());
        if (hasProperty(e, Property.DTEND)) {
            copyDateProperty(c, Events.DTEND, Events.EVENT_END_TIMEZONE, e.getEndDate());
        }

        if (hasProperty(e, Property.CLASS))
        {
            String access = e.getProperty(Property.CLASS).getValue();
            int accessLevel = Events.ACCESS_DEFAULT;
            if (access.equals("PUBLIC")) {
                accessLevel = Events.ACCESS_PUBLIC;
            } else if (access.equals("PRIVATE")) {
                accessLevel = Events.ACCESS_PRIVATE;
            } else if (access.equals("CONFIDENTIAL")) {
                accessLevel = Events.ACCESS_CONFIDENTIAL;
            }
            c.put(Events.ACCESS_LEVEL, accessLevel);
        }

        // Work out availability. This is confusing as FREEBUSY and TRANSP overlap.
        if (Events.AVAILABILITY != null) {
            int availability = Events.AVAILABILITY_BUSY;
            if (hasProperty(e, Property.TRANSP)) {
                if (e.getTransparency() == Transp.TRANSPARENT) {
                    availability = Events.AVAILABILITY_FREE;
                }
            } else if (hasProperty(e, Property.FREEBUSY)) {
                String fbtype = e.getProperty(Property.FREEBUSY).getValue();
                if (fbtype.equals("FREE")) {
                    availability = Events.AVAILABILITY_FREE;
                } else if (fbtype.equals("BUSY-TENTATIVE")) {
                    availability = Events.AVAILABILITY_TENTATIVE;
                }
            }
            c.put(Events.AVAILABILITY, availability);
        }

        copyProperty(c, Events.RRULE, e, Property.RRULE);
        copyProperty(c, Events.RDATE, e, Property.RDATE);
        copyProperty(c, Events.EXRULE, e, Property.EXRULE);
        copyProperty(c, Events.EXDATE, e, Property.EXDATE);
        copyProperty(c, Events.CUSTOM_APP_URI, e, Property.URL);
        copyProperty(c, Events.UID_2445, e, Property.UID);


        for (Object alarm: e.getAlarms()) {
            VAlarm a = (VAlarm)alarm;

            if (a.getAction() != Action.AUDIO && a.getAction() != Action.DISPLAY) {
                continue; // Ignore email and procedure alarms
            }

            Trigger t = a.getTrigger();
            long startMs = e.getStartDate().getDate().getTime();
            long alarmMs;

            if (t.getDateTime() != null) {
                alarmMs = t.getDateTime().getTime(); // Absolute
            } else if (t.getDuration() != null && t.getDuration().isNegative()) {
                Parameter rel = t.getParameter(Parameter.RELATED);
                if (rel != null && ((Related)rel) == Related.END) {
                    startMs = e.getEndDate().getDate().getTime();
                }
                alarmMs = startMs - durationToMs(t.getDuration()); // Relative
            } else {
                continue; // FIXME: Log this unsupported alarm
            }
            int reminder = (int)((startMs - alarmMs) / 1000 / 60);
            if (reminder >=0 && !reminders.contains(reminder)) {
                reminders.add(reminder);
            }
        }

        if (defReminders.size() > 0 || reminders.size() > 0) {
            c.put(Events.HAS_ALARM, 1);
        }

        // FIXME: Attendees, SELF_ATTENDEE_STATUS
        return c;
    }

    private static Duration createDuration(String value) {
        Duration d = new Duration();
        d.setValue(value);
        return d;
    }

    private static long durationToMs(Dur d) {
        long sec = d.getSeconds();
        sec += d.getMinutes() * (60);
        sec += d.getHours() * (60 * 60);
        sec += d.getDays() * (60 * 60 * 24);
        sec += d.getWeeks() * (60 * 60 * 24 * 7);
        return sec * 1000;
    }

    private boolean hasProperty(VEvent e, String name) {
        return e.getProperty(name) != null;
    }

    private void copyProperty(ContentValues c, String dbName, VEvent e, String evName) {
        if (dbName != null) {
            Property p = e.getProperty(evName);
            if (p != null) {
                c.put(dbName, p.getValue());
            }
        }
    }

    private void copyDateProperty(ContentValues c, String dbName, String dbTzName, DateProperty date) {
        if (dbName != null && date.getDate() != null) {
            c.put(dbName, date.getDate().getTime()); // ms since epoc in GMT
            if (dbTzName != null) {
                if (date.isUtc() || date.getTimeZone() == null) {
                    c.put(dbTzName, Time.TIMEZONE_UTC);
                } else {
                    c.put(dbTzName, date.getTimeZone().getID());
                }
            }
        }
    }

    private Uri insertAndLog(ContentResolver resolver, Uri uri, ContentValues vals, String type) {
        Uri result = resolver.insert(uri, vals);
        if (result == null) {
            Log.d(TAG, "Could not insert " + type);
        } else {
            Log.d(TAG,  "Inserted " + type + ": " + result.toString());
        }
        return result;
    }

    private boolean doesDbContain(ContentResolver resolver, ContentValues c) {
        Cursor cur = getFromContentValues(resolver, c);
        int count = cur.getCount();
        cur.close();
        return count > 0;
    }

    private Cursor getFromContentValues(ContentResolver resolver, ContentValues c) {
        // FIXME: This should match UID's, once we correctly preserve them, i.e:
        // (src.UID == search.UID) || (search.UID == null && src.title == search.title)
        // If UID's cannot be re-used between different calendars then we should
        // drop the CALENDAR_ID column from the where clause and make sure we handle
        // importing the same UID into two calendars sanely.
        String where = Events.CALENDAR_ID + "=? AND " + Events.TITLE + "=? AND " +
                Events.DTSTART + "=?";
        String[] args = new String[] {
                c.getAsString(Events.CALENDAR_ID),
                c.getAsString(Events.TITLE),
                c.getAsString(Events.DTSTART)
        };
        return resolver.query(Events.CONTENT_URI, new String[] { Events._ID }, where, args, null);
    }
}
