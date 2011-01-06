package at.aichbauer.ical;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.VEvent;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class GoogleVEventWrapper {
	private Map<String, IGoogleWrapper> googleToVEvent;
	private Map<String, IVEventWrapper> vEventToGoogle;
	private static GoogleVEventWrapper wrapper;
	private static final String LOG_ID = GoogleVEventWrapper.class.getSimpleName();

	private GoogleVEventWrapper() {
		googleToVEvent = new HashMap<String, IGoogleWrapper>();
		vEventToGoogle = new HashMap<String, IVEventWrapper>();

		googleToVEvent.put("description", new GoogleWrapper("description"));
		googleToVEvent.put("organizer", new GoogleWrapper("organizer"));
		googleToVEvent.put("rrule", new GoogleWrapper("rrule"));
		googleToVEvent.put("summary", new GoogleWrapper("summary", "title"));
		googleToVEvent.put("location", new GoogleWrapper("location", "eventLocation"));
		googleToVEvent.put("dtstart", new GoogleDateWrapper("dtstart"));
		googleToVEvent.put("dtend", new GoogleDateWrapper("dtend"));
		// googleToVEvent.put("dtstamp", new GoogleDateWrapper("dtstamp",
		// "lastDate"));

		vEventToGoogle.put("description", new VEventWrapper("description"));
		vEventToGoogle.put("organizer", new VEventWrapper("organizer"));
		vEventToGoogle.put("rrule", new VEventWrapper("rrule"));
		vEventToGoogle.put("summary", new VEventWrapper("summary", "title"));
		vEventToGoogle.put("location", new VEventWrapper("location", "eventLocation"));
		vEventToGoogle.put("dtstart", new VEventDateWrapper("dtstart"));
		vEventToGoogle.put("dtend", new VEventDateWrapper("dtend"));
		// vEventToGoogle.put("dtstamp", new VEventDateWrapper("dtstamp",
		// "lastDate"));
	}

	public static synchronized GoogleVEventWrapper getInstance() {
		if (wrapper == null) {
			wrapper = new GoogleVEventWrapper();
		}
		return wrapper;
	}

	public IGoogleWrapper getGoogleWrapper(String key) {
		return googleToVEvent.get(key);
	}

	public IVEventWrapper getVEventWrapper(String key) {
		return vEventToGoogle.get(key);
	}

	interface IGoogleWrapper {
		public void wrap(PropertyList properties, Cursor c);
	}

	interface IVEventWrapper {
		public void wrap(ContentValues values, VEvent vevent);
	}

	private class GoogleWrapper implements IGoogleWrapper {
		private String keyVEvent;
		private String keyGoogleEvent;

		public GoogleWrapper(String keyVEvent, String keyGoogleEVent) {
			this.keyVEvent = keyVEvent.toUpperCase();
			this.keyGoogleEvent = keyGoogleEVent;
		}

		public GoogleWrapper(String key) {
			this.keyVEvent = key.toUpperCase();
			this.keyGoogleEvent = key;
		}

		public void wrap(PropertyList properties, Cursor c) {
			int columnIndex = c.getColumnIndex(keyGoogleEvent);
			if (columnIndex != -1) {
				String value = c.getString(columnIndex);
				if (value != null) {
					try {
						Log.d(LOG_ID, "VEvent: " + keyVEvent + " GoogleEvent: " + keyGoogleEvent + " Value: " + value);
						properties.add(createProperty(keyVEvent, value));
					} catch (Exception exc) {
						Log.d(LOG_ID, "Error", exc);
					}
				}
			}
		}
	}

	private class GoogleDateWrapper implements IGoogleWrapper {
		private String keyVEvent;
		private String keyGoogleEvent;

		public GoogleDateWrapper(String keyVEvent, String keyGoogleEvent) {
			this.keyVEvent = keyVEvent.toUpperCase();
			this.keyGoogleEvent = keyGoogleEvent;
		}

		public GoogleDateWrapper(String key) {
			this.keyVEvent = key.toUpperCase();
			this.keyGoogleEvent = key;
		}

		public void wrap(PropertyList properties, Cursor c) {
			int columnIndex = c.getColumnIndex(keyGoogleEvent);
			if (columnIndex != -1) {
				String value = c.getString(columnIndex);
				if (value != null) {
					try {
						Log.d(LOG_ID, "VEvent: " + keyVEvent + " GoogleEvent: " + keyGoogleEvent + " Value: " + value);
						// Find timezone
						// TimeZone timezone = new
						// TimeZoneRegistryImpl().getTimeZone(c.getString(c
						// .getColumnIndex("timezone")));
						DateTime dateTime = new DateTime(Long.valueOf(value));
						// dateTime.setTimeZone(timezone);

						properties.add(createProperty(keyVEvent, dateTime.toString()));
					} catch (Exception exc) {
						Log.d(LOG_ID, "Error", exc);
					}
				}
			}
		}
	}

	private class VEventWrapper implements IVEventWrapper {
		private String keyGoogleEvent;
		private String keyVEvent;

		public VEventWrapper(String keyVEvent, String keyGoogleEvent) {
			this.keyVEvent = keyVEvent.toUpperCase();
			this.keyGoogleEvent = keyGoogleEvent;
		}

		public VEventWrapper(String key) {
			this.keyVEvent = key.toUpperCase();
			this.keyGoogleEvent = key;
		}

		public void wrap(ContentValues values, VEvent vevent) {
			Property property = vevent.getProperty(keyVEvent);
			if (property != null) {
				Log.d(LOG_ID, "VEvent: " + keyVEvent + " GoogleEvent: " + keyGoogleEvent + " Value: "
						+ property.getValue());
				values.put(keyGoogleEvent, property.getValue());
			}
		}
	}

	private class VEventDateWrapper implements IVEventWrapper {
		private String keyVEvent;
		private String keyGoogleEvent;

		public VEventDateWrapper(String keyVEvent, String keyGoogleEvent) {
			this.keyVEvent = keyVEvent.toUpperCase();
			this.keyGoogleEvent = keyGoogleEvent;
		}

		public VEventDateWrapper(String key) {
			this.keyVEvent = key.toUpperCase();
			this.keyGoogleEvent = key;
		}

		public void wrap(ContentValues values, VEvent vevent) {
			Property property = vevent.getProperty(keyVEvent);
			if (property != null) {
				try {
					Log.d(LOG_ID, "VEvent: " + keyVEvent + " GoogleEvent: " + keyGoogleEvent + " Value: "
							+ property.getValue());

					long timeInMillis = 0;
					Parameter parameter = property.getParameter("VALUE");
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

					if (property.getParameter("TZID") != null) {
						String tzone = property.getParameter("TZID").getValue();
						if (tzone != null) {
							TimeZoneRegistry registry = Controller.calendarBuilder.getRegistry();
							TimeZone z = registry.getTimeZone(tzone);
							if (z != null) {
								Log.d(LOG_ID, "Adjusting timezone by " + z.getRawOffset());
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
								Log.d(LOG_ID, "Could not find timezone: " + tzone);
							}
						}
						values.put("eventTimezone", tzone);
					}

					values.put(keyGoogleEvent, timeInMillis);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Property createProperty(String key, String value) throws IOException, URISyntaxException, ParseException {
		PropertyFactory factory = PropertyFactoryImpl.getInstance();
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
