package at.aichbauer.ical.activities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import at.aichbauer.ical.Controller;
import at.aichbauer.ical.GoogleCalendar;
import at.aichbauer.ical.ICalConstants;
import at.aichbauer.ical.R;
import at.aichbauer.ical.inputAdapters.BasicInputAdapter;
import at.aichbauer.tools.dialogs.DialogTools;
import at.aichbauer.tools.dialogs.SpinnerTools;

public class CalendarActivity extends Activity {
	private static final String TAG = CalendarActivity.class.getSimpleName();

	/*
	 * Views
	 */
	private Spinner calendarSpinner;
	private Spinner fileSpinner;
	private Button calendarInformation;
	private Button searchButton;
	private Button loadButton;
	private Button insertButton;
	private Button deleteButton;
	private Button setUrlButton;
	private Button dumpCalendar;
	private TextView icalInformation;
	private Controller controller;

	/*
	 * Values
	 */
	private List<BasicInputAdapter> urls;
	private List<GoogleCalendar> calendars;
	private LinearLayout processGroup;
	private SharedPreferences preferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		this.controller = new Controller(this);

		// Show help menu
		preferences = getSharedPreferences("at.aichbauer.iCal", Context.MODE_PRIVATE);
		boolean helpShown = preferences.getBoolean(ICalConstants.PREFERENCE_HELP_SHOWN, false);
		if (!helpShown) {
			DialogTools.showInformationDialog(this, getString(R.string.menu_help), getString(R.string.changelog),
					R.drawable.calendar_gray);
			Editor editor = preferences.edit();
			editor.putBoolean(ICalConstants.PREFERENCE_HELP_SHOWN, true);
			editor.commit();
		}

		// Retrieve views
		calendarSpinner = (Spinner) findViewById(R.id.Spinner01);
		fileSpinner = (Spinner) findViewById(R.id.Spinner02);
		searchButton = (Button) findViewById(R.id.SearchButton);
		loadButton = (Button) findViewById(R.id.LoadButton);
		insertButton = (Button) findViewById(R.id.InsertButton);
		deleteButton = (Button) findViewById(R.id.DeleteButton);
		calendarInformation = (Button) findViewById(R.id.ShowInformationButton);
		dumpCalendar = (Button) findViewById(R.id.SaveButton);
		icalInformation = (TextView) findViewById(R.id.textView01);
		processGroup = (LinearLayout) findViewById(R.id.linearLayout01);
		setUrlButton = (Button) findViewById(R.id.SetUrlButton);

		new Thread(new Runnable() {
			@Override
			public void run() {
				controller.init();
			}
		}).start();

		searchButton.setOnClickListener(controller);
		loadButton.setOnClickListener(controller);
		calendarInformation.setOnClickListener(controller);
		dumpCalendar.setOnClickListener(controller);
		deleteButton.setOnClickListener(controller);
		insertButton.setOnClickListener(controller);
		setUrlButton.setOnClickListener(controller);

		// if file intent
		Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_VIEW)) {
			try {
				setUrls(Arrays.asList(new BasicInputAdapter(new URL(intent.getDataString()))));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a list of calendars to the user interface for selection.
	 * 
	 * @param calendars
	 */
	public void setCalendars(List<GoogleCalendar> calendars) {
		this.calendars = calendars;
		List<String> calendarStrings = new ArrayList<String>();
		for (GoogleCalendar cal : calendars) {
			calendarStrings.add(cal.getDisplayName() + " (" + cal.getId() + ")");
		}
		SpinnerTools.simpleSpinnerInUI(this, calendarSpinner, calendarStrings);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				calendarInformation.setVisibility(CalendarActivity.this.calendars == null ? View.GONE : View.VISIBLE);
				dumpCalendar.setVisibility(CalendarActivity.this.calendars == null ? View.GONE : View.VISIBLE);

			}
		});
	}

	/**
	 * Set a list of url's for selection.
	 * 
	 * @param urls
	 *            Url's to display in the list
	 */
	public void setUrls(List<BasicInputAdapter> urls) {
		this.urls = urls;
		SpinnerTools.simpleSpinnerInUI(this, fileSpinner, urls);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				loadButton.setVisibility(CalendarActivity.this.urls == null ? View.GONE : View.VISIBLE);
			}
		});
	}

	public void setCalendar(final Calendar calendar) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				icalInformation.setVisibility(calendar == null ? View.GONE : View.VISIBLE);
				processGroup.setVisibility(calendar == null ? View.GONE : View.VISIBLE);
				if (calendar != null) {
					icalInformation.setText(getString(R.string.textview_calendar_short_information, calendar
							.getComponents(VEvent.VEVENT).size()));
				}
			}
		});
	}

	public SharedPreferences getPreferenceStore() {
		return preferences;
	}

	public GoogleCalendar getSelectedCalendar() {
		if (calendarSpinner.getSelectedItem() != null && calendars != null) {
			String calendarName = calendarSpinner.getSelectedItem().toString();
			for (GoogleCalendar cal : calendars) {
				if ((cal.getDisplayName() + " (" + cal.getId() + ")").equals(calendarName)) {
					return cal;
				}
			}
		}
		return null;
	}

	public BasicInputAdapter getSelectedURL() {
		return fileSpinner.getSelectedItem() != null ? (BasicInputAdapter) fileSpinner.getSelectedItem() : null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.quit) {
			onBackPressed();
		} else if (item.getItemId() == R.id.help) {
			DialogTools.showInformationDialog(this, getString(R.string.menu_help), Html.fromHtml(ICalConstants.HELP),
					R.drawable.calendar_gray);
		} else if (item.getItemId() == R.id.changelog) {
			DialogTools.showInformationDialog(this, R.string.menu_changelog, R.string.changelog,
					R.drawable.calendar_gray);
		} else if (item.getItemId() == R.id.license) {
			DialogTools.showInformationDialog(this, R.string.menu_license, R.string.license, R.drawable.calendar_gray);
		} else if (item.getItemId() == R.id.beer) {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ICalConstants.BUY_ME_BEER));
			startActivity(i);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean rated = preferences.getBoolean(ICalConstants.PREFERENCE_RATED, false);
				if (!rated) {
					boolean decision = DialogTools.decisionDialog(CalendarActivity.this,
							R.string.dialog_information_title, R.string.dialog_beer,
							R.string.dialog_button_buy_me_beer, R.string.dialog_no, R.drawable.calendar);
					if (decision) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ICalConstants.BUY_ME_BEER));
						startActivity(i);
					} else {
						decision = DialogTools.decisionDialog(CalendarActivity.this, R.string.dialog_information_title,
								R.string.dialog_rating, R.string.dialog_button_go_to_market,
								R.string.dialog_button_no_i_dont_like, R.drawable.calendar);

						if (decision) {
							Editor editor = preferences.edit();
							editor.putBoolean(ICalConstants.PREFERENCE_RATED, decision);
							editor.commit();

							Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ICalConstants.MARKET_URL));
							startActivity(i);
						}
					}
				}
				finish();
			}
		}).start();
	}
}
