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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.parameter.Related;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.FreeBusy;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.Property;

import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;
import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.RemindersDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContractWrapper.Events;
import android.provider.CalendarContractWrapper.Reminders;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class ProcessVEvent extends RunnableWithProgress {
    private static final String TAG = ProcessVEvent.class.getSimpleName();

    private static final Duration ONE_DAY = createDuration("P1D");
    private static final Duration ZERO_SECONDS = createDuration("PT0S");

    private Calendar mICalCalendar;
    private AndroidCalendar mAndroidCalendar;
    private boolean mIsInserter;

    private final class Options {
        public Settings.DuplicateHandlingEnum mDuplicateHandling;
        public boolean mKeepUids;
        private boolean mImportReminders;
        private List<Integer> mDefaultReminders;

        public Options(Settings settings) {
            mDuplicateHandling = settings.getDuplicateHandling();
            mKeepUids = settings.getKeepUids();
            mImportReminders = settings.getImportReminders();
            mDefaultReminders = RemindersDialog.getSavedRemindersInMinutes(settings);
        }

        public List<Integer> getReminders(List<Integer> eventReminders) {
            if (mImportReminders && eventReminders.size() > 0)
                return eventReminders;
            return mDefaultReminders;
        }
    }

    public ProcessVEvent(Activity activity, Calendar iCalCalendar, boolean isInserter) {
        super(activity, ProgressDialog.STYLE_HORIZONTAL);
        mICalCalendar = iCalCalendar;
        mAndroidCalendar = ((MainActivity) activity).getSelectedCalendar();
        mIsInserter = isInserter;
    }

    public void run() {
        try {
            MainActivity activity = (MainActivity) getActivity();
            Options options = new Options(activity.getSettings());

            List<Integer> reminders = new ArrayList<Integer>();

            setMessage(R.string.processing_entries);
            ComponentList events = mICalCalendar.getComponents(VEvent.VEVENT);

            setMax(events.size());
            ContentResolver resolver = activity.getContentResolver();
            int numDel = 0;
            int numIns = 0;
            int numDups = 0;
            final boolean ignoreDupes = options.mDuplicateHandling == Settings.DuplicateHandlingEnum.DUP_IGNORE;

            ContentValues cAlarm = new ContentValues();
            cAlarm.put(Reminders.METHOD, Reminders.METHOD_ALERT);

            for (Object ve: events) {
                incrementProgressBy(1);

                VEvent e = (VEvent) ve;
                Log.d(TAG, "source event: " + e.toString());

                if (e.getRecurrenceId() != null) {
                    // FIXME: Support these edited instances
                    Log.d(TAG, "Ignoring edited instance of a recurring event");
                    continue;
                }

                ContentValues c = convertToDB(e, options, reminders, mAndroidCalendar.mId);
                boolean mustDelete = !mIsInserter;
                if (!mustDelete) {
                    mustDelete = dbHasDuplicate(resolver, options, c);
                    if (mustDelete && ignoreDupes) {
                        Log.d(TAG, "Ignoring duplicate event");
                        numDups++;
                        continue;
                    }
                }

                if (mustDelete) {
                    Cursor cur = query(resolver, options, c);
                    if (cur != null) {
                        while (cur.moveToNext()) {
                            String id = cur.getString(0);
                            Uri eventUri = Uri.withAppendedPath(Events.CONTENT_URI, id);
                            numDel += resolver.delete(eventUri, null, null);
                            String where = Reminders.EVENT_ID + "=?";
                            resolver.delete(Reminders.CONTENT_URI, where, new String[] { id });
                        }
                        cur.close();
                    }
                    if (!mIsInserter)
                        continue;
                }

                if (Events.UID_2445 != null && !c.containsKey(Events.UID_2445)) {
                    // Create a UID for this event to use. We create it here so if
                    // exported multiple times it will always have the same id.
                    c.put(Events.UID_2445, activity.generateUid());
                }

                Log.d(TAG, "destination values: " + c);

                Uri uri = insertAndLog(resolver, Events.CONTENT_URI, c, "Event");
                if (uri == null)
                    continue; // FIXME: Note the failure

                final int id = Integer.parseInt(uri.getLastPathSegment());

                numIns++;

                for (int time: options.getReminders(reminders)) {
                    Log.d(TAG, "Inserting reminder for event with id: " + id);

                    cAlarm.put(Reminders.EVENT_ID, id);
                    cAlarm.put(Reminders.MINUTES, time);
                    insertAndLog(resolver, Reminders.CONTENT_URI, cAlarm, "Reminder");
                }
            }

            mAndroidCalendar.mNumEntries += numIns;
            mAndroidCalendar.mNumEntries -= numDel;
            activity.updateNumEntries(mAndroidCalendar);

            Resources res = activity.getResources();
            int n = mIsInserter ? numIns : numDel;
            String msg = res.getQuantityString(R.plurals.processed_n_entries, n, n) + "\n";
            if (mIsInserter) {
                msg += "\n";
                if (options.mDuplicateHandling == Settings.DuplicateHandlingEnum.DUP_DONT_CHECK)
                    msg += res.getString(R.string.did_not_check_for_dupes);
                else
                    msg += res.getQuantityString(R.plurals.found_n_duplicates, numDups, numDups);
            }

            activity.showToast(msg);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "ProcessVEvent", e);
            try {
                String p = Environment.getExternalStorageDirectory() + "/ical_error.log";
                PrintStream out = new PrintStream(new FileOutputStream(p));
                e.printStackTrace(out);
            } catch (Exception ignored) {
            }
            DialogTools.info(getActivity(), R.string.error, R.string.dialog_bug);
        }
    }

    // Munge a VEvent so Android won't reject it, then convert to ContentValues for inserting
    private ContentValues convertToDB(VEvent e, Options options,
                                      List<Integer> reminders, int calendarId) {
        reminders.clear();

        boolean allDay = false;
        boolean startIsDate = !(e.getStartDate().getDate() instanceof DateTime);
        boolean isRecurring = hasProperty(e, Property.RRULE) || hasProperty(e, Property.RDATE);

        if (startIsDate) {
            // If the start date is a DATE, the event is all-day, midnight to midnight (RFC 2445).
            // Add a duration of 1 day and remove the end date. If the event is non-recurring then
            // we will convert the duration to an end date below, which fixes all-day cases where
            // the end date is set to the same day at 23:59:59, rolls over because of a TZ, etc.
            e.getProperties().add(ONE_DAY);
            allDay = true;
            //  If an event is marked as all day it must be in the UTC timezone.
            e.getStartDate().setUtc(true);
            removeProperty(e, Property.DTEND);
        }

        if (!hasProperty(e, Property.DTEND) && !hasProperty(e, Property.DURATION)) {
            // No end date or duration given.
            // Since we added a duration above when the start date is a DATE:
            // - The start date is a DATETIME, the event lasts no time at all (RFC 2445).
            e.getProperties().add(ZERO_SECONDS);
            // Zero time events are always free (RFC 2445), so override/set TRANSP accordingly.
            removeProperty(e, Property.TRANSP);
            e.getProperties().add(Transp.TRANSPARENT);
        }

        if (isRecurring) {
            // Recurring event. Android insists on a duration.
            if (!hasProperty(e, Property.DURATION)) {
                // Calculate duration from start to end date
                Duration d = new Duration(e.getStartDate().getDate(), e.getEndDate().getDate());
                e.getProperties().add(d);
            }
            removeProperty(e, Property.DTEND);
        } else {
            // Non-recurring event. Android insists on an end date.
            if (!hasProperty(e, Property.DTEND)) {
                // Calculate end date from duration, set it and remove the duration.
                e.getProperties().add(e.getEndDate());
            }
            removeProperty(e, Property.DURATION);
        }

        // Now calculate the db values for the event
        ContentValues c = new ContentValues();

        c.put(Events.CALENDAR_ID, calendarId);
        copyProperty(c, Events.TITLE, e, Property.SUMMARY);
        copyProperty(c, Events.DESCRIPTION, e, Property.DESCRIPTION);

        if (hasProperty(e, Property.ORGANIZER)) {
            copyProperty(c, Events.ORGANIZER, e, Property.ORGANIZER);
            c.put(Events.GUESTS_CAN_MODIFY, 1); // Ensure we can edit the item if not the organiser
        }

        copyProperty(c, Events.EVENT_LOCATION, e, Property.LOCATION);

        if (hasProperty(e, Property.STATUS)) {
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

        if (allDay)
            c.put(Events.ALL_DAY, 1);

        copyDateProperty(c, Events.DTSTART, Events.EVENT_TIMEZONE, e.getStartDate());
        if (hasProperty(e, Property.DTEND))
            copyDateProperty(c, Events.DTEND, Events.EVENT_END_TIMEZONE, e.getEndDate());

        if (hasProperty(e, Property.CLASS)) {
            String access = e.getProperty(Property.CLASS).getValue();
            int accessLevel = Events.ACCESS_DEFAULT;
            if (access.equals("CONFIDENTIAL"))
                accessLevel = Events.ACCESS_CONFIDENTIAL;
            else if (access.equals("PRIVATE"))
                accessLevel = Events.ACCESS_PRIVATE;
            else if (access.equals("PUBLIC"))
                accessLevel = Events.ACCESS_PUBLIC;

            c.put(Events.ACCESS_LEVEL, accessLevel);
        }

        // Work out availability. This is confusing as FREEBUSY and TRANSP overlap.
        if (Events.AVAILABILITY != null) {
            int availability = Events.AVAILABILITY_BUSY;
            if (hasProperty(e, Property.TRANSP)) {
                if (e.getTransparency() == Transp.TRANSPARENT)
                    availability = Events.AVAILABILITY_FREE;

            } else if (hasProperty(e, Property.FREEBUSY)) {
                FreeBusy fb = (FreeBusy) e.getProperty(Property.FREEBUSY);
                FbType fbType = (FbType) fb.getParameter(Parameter.FBTYPE);
                if (fbType != null && fbType == FbType.FREE)
                    availability = Events.AVAILABILITY_FREE;
                else if (fbType != null && fbType == FbType.BUSY_TENTATIVE)
                    availability = Events.AVAILABILITY_TENTATIVE;
            }
            c.put(Events.AVAILABILITY, availability);
        }

        copyProperty(c, Events.RRULE, e, Property.RRULE);
        copyProperty(c, Events.RDATE, e, Property.RDATE);
        copyProperty(c, Events.EXRULE, e, Property.EXRULE);
        copyProperty(c, Events.EXDATE, e, Property.EXDATE);
        copyProperty(c, Events.CUSTOM_APP_URI, e, Property.URL);
        copyProperty(c, Events.UID_2445, e, Property.UID);
        if (c.containsKey(Events.UID_2445) && TextUtils.isEmpty(c.getAsString(Events.UID_2445))) {
            // Remove null/empty UIDs
            c.remove(Events.UID_2445);
        }

        for (Object alarm: e.getAlarms()) {
            VAlarm a = (VAlarm) alarm;

            if (a.getAction() != Action.AUDIO && a.getAction() != Action.DISPLAY)
                continue; // Ignore email and procedure alarms

            Trigger t = a.getTrigger();
            long startMs = e.getStartDate().getDate().getTime();
            long alarmMs;

            if (t.getDateTime() != null)
                alarmMs = t.getDateTime().getTime(); // Absolute
            else if (t.getDuration() != null && t.getDuration().isNegative()) {
                Related rel = (Related) t.getParameter(Parameter.RELATED);
                if (rel != null && rel == Related.END) {
                    startMs = e.getEndDate().getDate().getTime();
                }
                alarmMs = startMs - durationToMs(t.getDuration()); // Relative
            } else
                continue; // FIXME: Log this unsupported alarm

            int reminder = (int) ((startMs - alarmMs) / DateUtils.MINUTE_IN_MILLIS);
            if (reminder >= 0 && !reminders.contains(reminder))
                reminders.add(reminder);
        }

        if (options.getReminders(reminders).size() > 0)
            c.put(Events.HAS_ALARM, 1);

        // FIXME: Attendees, SELF_ATTENDEE_STATUS
        return c;
    }

    private static Duration createDuration(String value) {
        Duration d = new Duration();
        d.setValue(value);
        return d;
    }

    private static long durationToMs(Dur d) {
        long ms = 0;
        ms += d.getSeconds() * DateUtils.SECOND_IN_MILLIS;
        ms += d.getMinutes() * DateUtils.MINUTE_IN_MILLIS;
        ms += d.getHours()   * DateUtils.HOUR_IN_MILLIS;
        ms += d.getDays()    * DateUtils.DAY_IN_MILLIS;
        ms += d.getWeeks()   * DateUtils.WEEK_IN_MILLIS;
        return ms;
    }

    private boolean hasProperty(VEvent e, String name) {
        return e.getProperty(name) != null;
    }

    private void removeProperty(VEvent e, String name) {
        Property p = e.getProperty(name);
        if (p != null)
            e.getProperties().remove(p);
    }

    private void copyProperty(ContentValues c, String dbName, VEvent e, String evName) {
        if (dbName != null) {
            Property p = e.getProperty(evName);
            if (p != null)
                c.put(dbName, p.getValue());
        }
    }

    private void copyDateProperty(ContentValues c, String dbName, String dbTzName, DateProperty date) {
        if (dbName != null && date.getDate() != null) {
            c.put(dbName, date.getDate().getTime()); // ms since epoc in GMT
            if (dbTzName != null) {
                if (date.isUtc() || date.getTimeZone() == null)
                    c.put(dbTzName, Time.TIMEZONE_UTC);
                else
                    c.put(dbTzName, date.getTimeZone().getID());
            }
        }
    }

    private Uri insertAndLog(ContentResolver resolver, Uri uri, ContentValues c, String type) {
        Uri result = resolver.insert(uri, c);
        if (result == null)
            Log.d(TAG, "Could not insert " + type);
        else
            Log.d(TAG,  "Inserted " + type + ": " + result.toString());
        return result;
    }

    private boolean dbHasDuplicate(ContentResolver resolver, Options options, ContentValues c) {
        if (options.mDuplicateHandling == Settings.DuplicateHandlingEnum.DUP_DONT_CHECK)
            return false;

        Cursor cur = query(resolver, options, c);
        if (cur == null)
            return false;

        int count = cur.getCount();
        cur.close();
        return count > 0;
    }

    private Cursor query(ContentResolver resolver, Options options, ContentValues c) {

        final String[] cols = new String[] { Events._ID };

        if (options.mKeepUids && Events.UID_2445 != null && c.containsKey(Events.UID_2445)) {
            String where = Events.UID_2445 + "=?";
            String[] args = new String[] { c.getAsString(Events.UID_2445) };
            return resolver.query(Events.CONTENT_URI, cols, where, args, null);
        }

        // Without UIDs, the best we can do is check the start date and title within
        // the current calendar, even though this may return false duplicates.
        if (!c.containsKey(Events.CALENDAR_ID) || !c.containsKey(Events.DTSTART))
            return null;

        StringBuilder b = new StringBuilder();
        b.append(Events.CALENDAR_ID).append("=? AND ")
        .append(Events.DTSTART).append("=? AND ")
        .append(Events.TITLE);

        List<String> argsList = new ArrayList<String>();
        argsList.add(c.getAsString(Events.CALENDAR_ID));
        argsList.add(c.getAsString(Events.DTSTART));

        if (c.containsKey(Events.TITLE)) {
            b.append("=?");
            argsList.add(c.getAsString(Events.TITLE));
        } else
            b.append(" is null");

        String where = b.toString();
        String[] args = argsList.toArray(new String[argsList.size()]);

        return resolver.query(Events.CONTENT_URI, cols, where, args, null);
    }
}
