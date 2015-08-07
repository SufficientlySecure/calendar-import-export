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

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryImpl;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.TzId;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;

@SuppressLint("NewApi")
public class AndroidVEventWrapper {
    private Map<String, IAndroidWrapper> androidToVEvent;
    private Map<String, IVEventWrapper> vEventToAndroid;
    private static AndroidVEventWrapper wrapper;
    private static final String TAG = AndroidVEventWrapper.class.getSimpleName();

    private AndroidVEventWrapper() {
        androidToVEvent = new HashMap<String, IAndroidWrapper>();
        vEventToAndroid = new HashMap<String, IVEventWrapper>();

        androidToVEvent.put("description", new AndroidWrapper("description"));
        // androidToVEvent.put("organizer", new AndroidOrganizerWrapper("organizer"));
        androidToVEvent.put("rrule", new AndroidWrapper("rrule"));
        androidToVEvent.put("summary", new AndroidWrapper("summary", "title"));
        androidToVEvent.put("location", new AndroidWrapper("location", "eventLocation"));
        androidToVEvent.put("dtstart", new AndroidDateWrapper("dtstart"));
        androidToVEvent.put("dtend", new AndroidDateWrapper("dtend"));
        // androidToVEvent.put("dtstamp", new AndroidDateWrapper("dtstamp",
        // "lastDate"));

        vEventToAndroid.put("description", new VEventWrapper("description"));
        // vEventToAndroid.put("organizer", new VEventWrapper("organizer"));
        vEventToAndroid.put("rrule", new VEventWrapper("rrule"));
        vEventToAndroid.put("summary", new VEventWrapper("summary", "title"));
        vEventToAndroid.put("location", new VEventWrapper("location", "eventLocation"));
        vEventToAndroid.put("dtstart", new VEventDateWrapper("dtstart"));
        vEventToAndroid.put("dtend", new VEventDateWrapper("dtend"));
        // vEventToAndroid.put("dtstamp", new VEventDateWrapper("dtstamp",
        // "lastDate"));
    }

    public static synchronized AndroidVEventWrapper getInstance() {
        if (wrapper == null) {
            wrapper = new AndroidVEventWrapper();
        }
        return wrapper;
    }

    public IAndroidWrapper getAndroidWrapper(String key) {
        return androidToVEvent.get(key);
    }

    public IVEventWrapper getVEventWrapper(String key) {
        return vEventToAndroid.get(key);
    }

    interface IAndroidWrapper {
        public void wrap(PropertyList properties, Cursor c);
    }

    interface IVEventWrapper {
        public void wrap(ContentValues values, VEvent vevent);
    }

    private class AndroidWrapper implements IAndroidWrapper {
        private String keyVEvent;
        private String keyAndroidEvent;

        public AndroidWrapper(String keyVEvent, String keyAndroidEvent) {
            this.keyVEvent = keyVEvent.toUpperCase(Locale.US);
            this.keyAndroidEvent = keyAndroidEvent;
        }

        public AndroidWrapper(String key) {
            this.keyVEvent = key.toUpperCase(Locale.US);
            this.keyAndroidEvent = key;
        }

        public void wrap(PropertyList properties, Cursor c) {
            int columnIndex = c.getColumnIndex(keyAndroidEvent);
            if (columnIndex != -1) {
                String value = c.getString(columnIndex);
                if (value != null) {
                    try {
                        Log.d(TAG, "AndroidWrapper, VEvent: " + keyVEvent + " AndroidEvent: "
                                + keyAndroidEvent + " Value: " + value);
                        properties.add(createProperty(keyVEvent, value));
                    } catch (Exception exc) {
                        Log.d(TAG, "Error", exc);
                    }
                }
            }
        }
    }

    // private class AndroidOrganizerWrapper implements IAndroidWrapper {
    // private String keyVEvent;
    // private String keyAndroidEvent;
    //
    // public AndroidOrganizerWrapper(String keyVEvent, String keyAndroidEvent) {
    // this.keyVEvent = keyVEvent.toUpperCase();
    // this.keyAndroidEvent = keyAndroidEvent;
    // }
    //
    // public AndroidOrganizerWrapper(String key) {
    // this.keyVEvent = key.toUpperCase();
    // this.keyAndroidEvent = key;
    // }
    //
    // public void wrap(PropertyList properties, Cursor c) {
    // int columnIndex = c.getColumnIndex(keyAndroidEvent);
    // if (columnIndex != -1) {
    // String value = c.getString(columnIndex);
    // if (value != null) {
    // // remove whitespaces from organizer string
    // value = value.replaceAll("\\s", "");
    // try {
    // Log.d(TAG, "AndroidWrapper, VEvent: " + keyVEvent + " AndroidEvent: "
    // + keyAndroidEvent + " Value: " + value);
    // properties.add(createProperty(keyVEvent, value));
    // } catch (Exception exc) {
    // Log.d(TAG, "Error", exc);
    // }
    // }
    // }
    // }
    // }

    private class AndroidDateWrapper implements IAndroidWrapper {
        private String keyVEvent;
        private String keyAndroidEvent;

        public AndroidDateWrapper(String keyVEvent, String keyAndroidEvent) {
            this.keyVEvent = keyVEvent.toUpperCase(Locale.US);
            this.keyAndroidEvent = keyAndroidEvent;
        }

        public AndroidDateWrapper(String key) {
            this.keyVEvent = key.toUpperCase(Locale.US);
            this.keyAndroidEvent = key;
        }

        public void wrap(PropertyList properties, Cursor c) {
            int columnIndex = c.getColumnIndex(keyAndroidEvent);
            if (columnIndex != -1) {
                String value = c.getString(columnIndex);
                if (value != null) {
                    try {
                        Log.d(TAG, "AndroidDateWrapper, VEvent: " + keyVEvent + " AndroidEvent: "
                                + keyAndroidEvent + " Value: " + value);
                        // Find timezone
                        String dbTimezone = c.getString(c
                                .getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE));
                        Log.d(TAG, "timezone from Android: " + dbTimezone);
                        net.fortuna.ical4j.model.TimeZone timezone = new TimeZoneRegistryImpl()
                                .getTimeZone(dbTimezone);

                        // TODO: Do we need to convert this timezone??? To account for GMT
                        // conversion to string?
                        Log.d(TAG, "timezone display name: " + timezone.getDisplayName());
                        Log.d(TAG, "vtimezone id: " + timezone.getVTimeZone().getTimeZoneId());

                        // Time needs to be the local time (i.e. the time in the timezone of the
                        // event)
                        // see FORM#3, http://www.kanzaki.com/docs/ical/dateTime.html
                        DateTime dateTime = new DateTime(Long.valueOf(value));
                        dateTime.setTimeZone(timezone);
                        Log.d(TAG, "dateTime: " + dateTime.toString());

                        // Add timezone to event
                        // create tzid parameter..
                        TzId tzParam = new TzId(timezone.getID());
                        // create value parameter..
                        // Value type = Value.TIME;
                        ParameterList params = new ParameterList();
                        params.add(tzParam);
                        // params.add(type);

                        PropertyFactoryImpl factory = PropertyFactoryImpl.getInstance();
                        Property property = factory.createProperty(keyVEvent, params,
                                dateTime.toString());

                        properties.add(property);
                    } catch (Exception exc) {
                        Log.d(TAG, "Error", exc);
                    }
                }
            }
        }
    }

    private class VEventWrapper implements IVEventWrapper {
        private String keyAndroidEvent;
        private String keyVEvent;

        public VEventWrapper(String keyVEvent, String keyGoogleEvent) {
            this.keyVEvent = keyVEvent.toUpperCase(Locale.US);
            this.keyAndroidEvent = keyGoogleEvent;
        }

        public VEventWrapper(String key) {
            this.keyVEvent = key.toUpperCase(Locale.US);
            this.keyAndroidEvent = key;
        }

        public void wrap(ContentValues values, VEvent vevent) {
            Property property = vevent.getProperty(keyVEvent);
            if (property != null) {
                Log.d(TAG, "VEventWrapper, VEvent: " + keyVEvent + " AndroidEvent: "
                        + keyAndroidEvent + " Value: " + property.getValue());
                values.put(keyAndroidEvent, property.getValue());
            }
        }
    }

    private class VEventDateWrapper implements IVEventWrapper {
        private String keyVEvent;
        private String keyAndroidEvent;

        public VEventDateWrapper(String keyVEvent, String keyAndroidEvent) {
            this.keyVEvent = keyVEvent.toUpperCase(Locale.US);
            this.keyAndroidEvent = keyAndroidEvent;
        }

        public VEventDateWrapper(String key) {
            this.keyVEvent = key.toUpperCase(Locale.US);
            this.keyAndroidEvent = key;
        }

        public void wrap(ContentValues values, VEvent vevent) {
            Property property = vevent.getProperty(keyVEvent);
            if (property != null) {
                try {
                    Log.d(TAG, "VEventDateWrapper, VEvent: " + keyVEvent + " AndroidEvent: "
                            + keyAndroidEvent + " Value: " + property.getValue());

                    long timeInMillis = 0;
                    Parameter parameter = property.getParameter(Parameter.VALUE);
                    if (parameter != null && parameter.getValue().equals("DATE")) {
                        int year = Integer.parseInt(property.getValue().substring(0, 4));
                        int month = Integer.parseInt(property.getValue().substring(4, 6));
                        int day = Integer.parseInt(property.getValue().substring(6, 8));
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.clear();
                        cal.set(year, month - 1, day, 0, 0, 0);
                        timeInMillis = cal.getTimeInMillis();
                    } else {
                        timeInMillis = dateToMillis(new DateTime(property.getValue()));
                    }

                    // parse timezone (TZID)
                    if (property.getParameter(Parameter.TZID) != null) {
                        String tzone = property.getParameter(Parameter.TZID).getValue();
                        Log.d(TAG, "Found TZID (timezone) property: " + tzone);
                        if (tzone != null) {
                            TimeZoneRegistry registry = Controller.calendarBuilder.getRegistry();
                            TimeZone z = registry.getTimeZone(tzone);
                            if (z != null) {
                                Log.d(TAG, "Adjusting timezone by " + z.getRawOffset());
                                int z1 = z.getRawOffset();

                                // Try to convert gmt GMT-05:00
                                int z2 = 0;
                                TimeZone currentZone = TimeZone.getDefault();
                                String dispName = currentZone.getDisplayName(true, TimeZone.SHORT);
                                if (dispName.startsWith("GMT")) {
                                    int shift = 0;
                                    if (dispName.matches("GMT[+].*")) {
                                        shift++;
                                    }
                                    if (dispName.matches("GMT[+|-][0-9][0-9]:[0-9][0-9].*")) {
                                        z2 = Integer.parseInt(dispName.substring(3 + shift, 6)) * 3600000;
                                        z2 += ((z2 < 0) ? -1 : 1)
                                                * (Integer.parseInt(dispName.substring(7, 9)) * 60000);
                                    } else if (dispName.matches("GMT[+|-][0-9][0-9].*")) {
                                        z2 = Integer.parseInt(dispName.substring(3 + shift, 6)) * 3600000;
                                    } else if (dispName.matches("GMT[+|-][0-9].*")) {
                                        z2 = Integer.parseInt(dispName.substring(3 + shift, 5)) * 3600000;
                                    }
                                } else {
                                    z2 = currentZone.getRawOffset();
                                }

                                timeInMillis -= z1 - z2;

                            } else {
                                Log.d(TAG, "Could not find timezone: " + tzone);
                            }
                        }
                        values.put(CalendarContract.Events.EVENT_TIMEZONE, tzone);
                    } else {
                        // If no TZID property was found for this event, use TimeZone set on device
                        values.put(CalendarContract.Events.EVENT_TIMEZONE,
                                Time.getCurrentTimezone());
                    }

                    values.put(keyAndroidEvent, timeInMillis);
                } catch (ParseException e) {
                    Log.e(TAG, "AndroidVEventWrapper", e);
                }
            }
        }
    }

    private Property createProperty(String key, String value) throws IOException,
            URISyntaxException, ParseException {
        PropertyFactoryImpl factory = PropertyFactoryImpl.getInstance();
        Property property = factory.createProperty(key);
        property.setValue(value);
        return property;
    }

    private long dateToMillis(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.getTimeInMillis();
    }
}
