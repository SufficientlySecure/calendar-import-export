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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryImpl;
import net.fortuna.ical4j.model.parameter.TzId;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.provider.CalendarContract.Events;
import android.util.Log;

@SuppressLint("NewApi")
public final class AndroidVEventWrapper {
    private Map<String, IAndroidWrapper> androidToVEvent;
    private static AndroidVEventWrapper wrapper;
    private static final String TAG = AndroidVEventWrapper.class.getSimpleName();

    private AndroidVEventWrapper() {
        androidToVEvent = new HashMap<String, IAndroidWrapper>();

        androidToVEvent.put("description", new AndroidWrapper("description"));
        androidToVEvent.put("rrule", new AndroidWrapper("rrule"));
        androidToVEvent.put("summary", new AndroidWrapper("summary", "title"));
        androidToVEvent.put("location", new AndroidWrapper("location", "eventLocation"));
        androidToVEvent.put("dtstart", new AndroidDateWrapper("dtstart"));
        androidToVEvent.put("dtend", new AndroidDateWrapper("dtend"));
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

    interface IAndroidWrapper {
        void wrap(PropertyList properties, Cursor c);
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
                        String dbTz = c.getString(c.getColumnIndex(Events.EVENT_TIMEZONE));
                        Log.d(TAG, "timezone from Android: " + dbTz);
                        TimeZone tz = new TimeZoneRegistryImpl().getTimeZone(dbTz);

                        // TODO: Do we need to convert this timezone??? To account for GMT
                        // conversion to string?
                        Log.d(TAG, "timezone display name: " + tz.getDisplayName());
                        Log.d(TAG, "vtimezone id: " + tz.getVTimeZone().getTimeZoneId());

                        // Time needs to be the local time (i.e. the time in the timezone of the
                        // event)
                        // see FORM#3, http://www.kanzaki.com/docs/ical/dateTime.html
                        DateTime dt = new DateTime(Long.valueOf(value));
                        dt.setTimeZone(tz);
                        Log.d(TAG, "dateTime: " + dt.toString());

                        // Add timezone to event
                        // create tzid parameter..
                        TzId tzParam = new TzId(tz.getID());
                        // create value parameter..
                        // Value type = Value.TIME;
                        ParameterList params = new ParameterList();
                        params.add(tzParam);
                        // params.add(type);

                        PropertyFactoryImpl factory = PropertyFactoryImpl.getInstance();
                        properties.add(factory.createProperty(keyVEvent, params, dt.toString()));
                    } catch (Exception exc) {
                        Log.d(TAG, "Error", exc);
                    }
                }
            }
        }
    }

    private Property createProperty(String key, String value)
            throws IOException, URISyntaxException, ParseException {
        PropertyFactoryImpl factory = PropertyFactoryImpl.getInstance();
        Property property = factory.createProperty(key);
        property.setValue(value);
        return property;
    }
}
