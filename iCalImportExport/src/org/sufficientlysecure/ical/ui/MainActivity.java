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

package org.sufficientlysecure.ical.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

import org.sufficientlysecure.ical.AndroidCalendar;
import org.sufficientlysecure.ical.Controller;
import org.sufficientlysecure.ical.ICalConstants;
import org.sufficientlysecure.ical.R;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.SpinnerTools;
import org.sufficientlysecure.ical.util.BasicInputAdapter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String LOAD_CALENDAR = "org.sufficientlysecure.ical.LOAD_CALENDAR";
    public static final String EXTRA_CALENDAR_ID = "calendarId";

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
    private List<AndroidCalendar> calendars;
    private LinearLayout processGroup;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        this.controller = new Controller(this);

        // Retrieve views
        calendarSpinner = (Spinner) findViewById(R.id.SpinnerChooseCalendar);
        fileSpinner = (Spinner) findViewById(R.id.SpinnerFile);
        searchButton = (Button) findViewById(R.id.SearchButton);
        loadButton = (Button) findViewById(R.id.LoadButton);
        insertButton = (Button) findViewById(R.id.InsertButton);
        deleteButton = (Button) findViewById(R.id.DeleteButton);
        calendarInformation = (Button) findViewById(R.id.ShowInformationButton);
        dumpCalendar = (Button) findViewById(R.id.SaveButton);
        icalInformation = (TextView) findViewById(R.id.IcalInfo);
        processGroup = (LinearLayout) findViewById(R.id.linearLayoutProcess);
        setUrlButton = (Button) findViewById(R.id.SetUrlButton);

        searchButton.setOnClickListener(controller);
        loadButton.setOnClickListener(controller);
        calendarInformation.setOnClickListener(controller);
        dumpCalendar.setOnClickListener(controller);
        deleteButton.setOnClickListener(controller);
        insertButton.setOnClickListener(controller);
        setUrlButton.setOnClickListener(controller);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();

            if (LOAD_CALENDAR.equals(action)) {
                final long calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1);

                // load with specific calendar
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        controller.init(calendarId);
                    }
                }).start();
            } else {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        controller.init();
                    }
                }).start();

                // if file intent
                if (Intent.ACTION_VIEW.equals(action)) {
                    try {
                        setUrls(Arrays
                                .asList(new BasicInputAdapter(new URL(intent.getDataString()))));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Add a list of calendars to the user interface for selection.
     * 
     * @param calendars
     */
    public void setCalendars(List<AndroidCalendar> calendars) {
        this.calendars = calendars;
        List<String> calendarStrings = new ArrayList<String>();
        for (AndroidCalendar cal : calendars) {
            calendarStrings.add(cal.getDisplayName() + " (" + cal.getId() + ")");
        }
        SpinnerTools.simpleSpinnerInUI(this, calendarSpinner, calendarStrings);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calendarInformation.setVisibility(MainActivity.this.calendars == null ? View.GONE
                        : View.VISIBLE);
                dumpCalendar.setVisibility(MainActivity.this.calendars == null ? View.GONE
                        : View.VISIBLE);

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
                loadButton.setVisibility(MainActivity.this.urls == null ? View.GONE : View.VISIBLE);
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
                    icalInformation.setText(getString(R.string.textview_calendar_short_information,
                            calendar.getComponents(VEvent.VEVENT).size()));
                }
            }
        });
    }

    public SharedPreferences getPreferenceStore() {
        return preferences;
    }

    public AndroidCalendar getSelectedCalendar() {
        if (calendarSpinner.getSelectedItem() != null && calendars != null) {
            String calendarName = calendarSpinner.getSelectedItem().toString();
            for (AndroidCalendar cal : calendars) {
                if ((cal.getDisplayName() + " (" + cal.getId() + ")").equals(calendarName)) {
                    return cal;
                }
            }
        }
        return null;
    }

    public void selectCalendar(long id) {
        for (final AndroidCalendar cal : calendars) {
            if (cal.getId() == id) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calendarSpinner.setSelection(calendars.indexOf(cal));
                    }
                });
            }
        }
    }

    public BasicInputAdapter getSelectedURL() {
        return fileSpinner.getSelectedItem() != null ? (BasicInputAdapter) fileSpinner
                .getSelectedItem() : null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            DialogTools.showInformationDialog(this, getString(R.string.menu_help),
                    Html.fromHtml(ICalConstants.HELP), R.drawable.icon);
        } else if (item.getItemId() == R.id.changelog) {
            DialogTools.showInformationDialog(this, R.string.menu_changelog, R.string.changelog,
                    R.drawable.icon);
        } else if (item.getItemId() == R.id.license) {
            DialogTools.showInformationDialog(this, R.string.menu_license, R.string.license,
                    R.drawable.icon);
        }
        return super.onContextItemSelected(item);
    }
}
