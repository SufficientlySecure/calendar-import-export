package at.aichbauer.ical;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import at.aichbauer.ical.activities.CalendarActivity;
import at.aichbauer.tools.dialogs.DialogTools;
import at.aichbauer.tools.dialogs.RunnableWithProgress;

public class Controller implements OnClickListener {
	private static final String TAG = Controller.class.getName();
	private CalendarActivity activity;
	public static CalendarBuilder calendarBuilder;
	private Calendar calendar;

	public Controller(CalendarActivity activity) {
		this.activity = activity;
	}

	public void init() {
		checkPrequesites();

		Cursor c = activity.getContentResolver().query(GoogleCalendar.getContentURI(), null, null, null, null);
		List<GoogleCalendar> calendars = new ArrayList<GoogleCalendar>(c.getCount());

		while (c.moveToNext()) {
			GoogleCalendar cal = GoogleCalendar.retrieve(c);
			Cursor cursor = activity.getContentResolver().query(VEventWrapper.getContentURI(), new String[] {},
					"calendar_id = ?", new String[] { Integer.toString(cal.getId()) }, null);
			cal.setEntryCount(cursor.getCount());
			cursor.close();
			calendars.add(cal);
		}
		c.close();

		this.activity.setCalendars(calendars);
	}

	public void checkPrequesites() {
		// Check if all necessary providers are installed
		ContentProviderClient calendarClient = activity.getContentResolver().acquireContentProviderClient(
				GoogleCalendar.getContentURI());
		ContentProviderClient eventClient = activity.getContentResolver().acquireContentProviderClient(
				VEventWrapper.getContentURI());

		if (eventClient == null || calendarClient == null) {
			DialogTools.showInformationDialog(activity, R.string.dialog_information_title,
					R.string.dialog_no_calendar_provider, R.drawable.calendar);
			activity.finish();
		}
		calendarClient.release();
		eventClient.release();

		// Check if their is a calendar
		Cursor c = activity.getContentResolver().query(GoogleCalendar.getContentURI(), null, null, null, null);

		if (c.getCount() == 0) {
			if (DialogTools.decisionDialog(activity, "No calendars...",
					"Create new dummy calendar? This can not be undone at the moment.", "Yes", "No",
					R.drawable.calendar_gray)) {
				ContentValues v = new ContentValues();
				v.put(GoogleCalendar.NAME, "dummy");
				v.put(GoogleCalendar.DISPLAY_NAME, "LocalDummyCalendar");
				v.put(GoogleCalendar.HIDDEN, 0);
				v.put(GoogleCalendar.ACCESS_LEVEL, 700);
				v.put(GoogleCalendar.TIMEZONE, java.util.Calendar.getInstance().getTimeZone().getID());
				v.put(GoogleCalendar.SYNC_ACCOUNT, "none");

				Account[] abc = AccountManager.get(activity).getAccounts();
				if (abc == null || abc.length == 0) {
					DialogTools.showInformationDialog(activity, "Problem creating calendar",
							"No account to use for calendar. Their must be at least one account...",
							R.drawable.calendar);
					return;
				}
				v.put(GoogleCalendar.SYNC_ACCOUNT_TYPE, abc[0].type);

				activity.getContentResolver().insert(GoogleCalendar.getContentURI(), v);
			} else {
				DialogTools.showInformationDialog(activity, R.string.dialog_information_title, R.string.dialog_exiting,
						R.drawable.calendar);
				activity.finish();
			}
		}
		c.close();
	}

	@Override
	public void onClick(View v) {
		// Handling search for file event
		if (v.getId() == R.id.SearchButton) {
			RunnableWithProgress run = new RunnableWithProgress(activity) {
				@Override
				public void run(ProgressDialog dialog) {
					setProgressMessage(R.string.progress_searching_ical_files);

					List<File> files = CalendarUtils.searchFiles(Environment.getExternalStorageDirectory(), "ics",
							"ical", "icalendar");
					List<URL> urls = new ArrayList<URL>(files.size());

					for (File file : files) {
						try {
							urls.add(file.toURL());
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
					// Collections.sort(urls, new );
					activity.setUrls(urls);
				}
			};
			DialogTools.runWithProgress(activity, run, false);
		}

		else if (v.getId() == R.id.LoadButton) {
			RunnableWithProgress run = new RunnableWithProgress(activity) {
				@Override
				public void run(ProgressDialog dialog) {
					if (calendarBuilder == null) {
						setProgressMessage(R.string.progress_loading_builder);
						calendarBuilder = new CalendarBuilder();
					}
					try {
						setProgressMessage(R.string.progress_reading_ical);
						InputStream in = activity.getSelectedURL().openStream();
						if (in != null) {
							calendar = calendarBuilder.build(in);
						}
						activity.setCalendar(calendar);
					} catch (Exception exc) {
						DialogTools.showInformationDialog(activity, activity.getString(R.string.dialog_error_title),
								activity.getString(R.string.dialog_error_unparseable) + exc.getMessage(),
								R.drawable.calendar);
						Log.d(TAG, "Error", exc);
					}
				}
			};
			DialogTools.runWithProgress(activity, run, false);
		} else if (v.getId() == R.id.SetUrlButton) {
			RunnableWithProgress run = new RunnableWithProgress(activity) {
				@Override
				public void run(ProgressDialog dialog) {
					String answer = DialogTools.questionDialog(activity, R.string.dialog_enter_url_title,
							R.string.dialog_enter_url, R.string.dialog_proceed, activity.getPreferenceStore()
									.getString(ICalConstants.PREFERENCE_LAST_URL, ""), true, R.drawable.calendar);
					if (answer != null && !answer.equals("")) {
						try {
							setProgressMessage("Parsing url...");
							URL url = new URL(answer);
							Editor editor = activity.getPreferenceStore().edit();
							editor.putString(ICalConstants.PREFERENCE_LAST_URL, answer);
							editor.commit();
							activity.setUrls(Arrays.asList(url));
						} catch (MalformedURLException exc) {
							DialogTools.showInformationDialog(activity,
									activity.getString(R.string.dialog_error_title), "URL was not parseable..."
											+ exc.getMessage(), R.drawable.calendar);
							Log.d(TAG, "Error", exc);
						}
					}
				}
			};
			DialogTools.runWithProgress(activity, run, false);
		} else if (v.getId() == R.id.SaveButton) {
			DialogTools.runWithProgress(activity, new SaveCalendar(activity, activity.getSelectedCalendar()), false,
					ProgressDialog.STYLE_HORIZONTAL);
		} else if (v.getId() == R.id.ShowInformationButton) {
			DialogTools.showInformationDialog(activity, activity.getString(R.string.dialog_information_title), Html
					.fromHtml(activity.getSelectedCalendar().toHtml()), R.drawable.calendar);
		} else if (v.getId() == R.id.InsertButton) {
			DialogTools.runWithProgress(activity, new InsertVEvents(activity, calendar, activity.getSelectedCalendar()
					.getId()), false, ProgressDialog.STYLE_HORIZONTAL);
		} else if (v.getId() == R.id.DeleteButton) {
			DialogTools.runWithProgress(activity, new DeleteVEvents(activity, calendar, activity.getSelectedCalendar()
					.getId()), false, ProgressDialog.STYLE_HORIZONTAL);
		}
	}
}
