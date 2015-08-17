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
import java.util.Collections;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

import org.sufficientlysecure.ical.AndroidCalendar;
import org.sufficientlysecure.ical.Controller;
import org.sufficientlysecure.ical.R;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.SpinnerTools;
import org.sufficientlysecure.ical.util.BasicInputAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

public class MainActivity extends Activity {
    public static final String LOAD_CALENDAR = "org.sufficientlysecure.ical.LOAD_CALENDAR";
    public static final String EXTRA_CALENDAR_ID = "calendarId";

    public static SharedPreferences preferences;

    /*
     * Views
     */
    private Spinner calendarSpinner;
    private Spinner fileSpinner;
    private Button loadButton;
    private Button insertButton;
    private Button deleteButton;
    private Button exportButton;
    private Controller controller;

    private TextView textCalName;
    private TextView textCalAccountName;
    private TextView textCalAccountType;
    private TextView textCalOwner;
    private TextView textCalState;
    private TextView textCalId;
    private TextView textCalTimezone;
    private TextView textCalSize;

    /*
     * Values
     */
    private List<BasicInputAdapter> urls;
    private List<AndroidCalendar> calendars;
    private LinearLayout insertDeleteLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        this.controller = new Controller(this);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieve views
        calendarSpinner = (Spinner) findViewById(R.id.SpinnerChooseCalendar);
        AdapterView.OnItemSelectedListener calListener;
        calListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AndroidCalendar calendar = calendars.get(pos);
                textCalName.setText(calendar.name);
                textCalAccountName.setText(calendar.accountName);
                textCalAccountType.setText(calendar.accountType);
                textCalOwner.setText(calendar.owner);
                textCalState.setText(calendar.isActive ? R.string.active : R.string.inactive);
                textCalId.setText(Integer.toString(calendar.id));
                if (calendar.timezone == null) {
                    textCalTimezone.setText(R.string.not_applicable);
                } else {
                    textCalTimezone.setText(calendar.timezone);
                }
                updateNumEntries(calendar);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        calendarSpinner.setOnItemSelectedListener(calListener);

        fileSpinner = (Spinner) findViewById(R.id.SpinnerFile);
        AdapterView.OnItemSelectedListener fileListener;
        fileListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                insertDeleteLayout.setVisibility(View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        fileSpinner.setOnItemSelectedListener(fileListener);

        setupButton(R.id.SearchButton);
        loadButton = setupButton(R.id.LoadButton);
        insertButton = setupButton(R.id.InsertButton);
        deleteButton = setupButton(R.id.DeleteButton);
        exportButton = setupButton(R.id.SaveButton);
        insertDeleteLayout = (LinearLayout) findViewById(R.id.InsertDeleteLayout);
        setupButton(R.id.SetUrlButton);

        textCalName = (TextView)findViewById(R.id.TextCalName);
        textCalAccountName = (TextView)findViewById(R.id.TextCalAccountName);
        textCalAccountType = (TextView)findViewById(R.id.TextCalAccountType);
        textCalOwner = (TextView)findViewById(R.id.TextCalOwner);
        textCalState = (TextView)findViewById(R.id.TextCalState);
        textCalId = (TextView)findViewById(R.id.TextCalId);
        textCalTimezone = (TextView)findViewById(R.id.TextCalTimezone);
        textCalSize = (TextView)findViewById(R.id.TextCalSize);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        final int id = action.equals(LOAD_CALENDAR) ? intent.getIntExtra(EXTRA_CALENDAR_ID, -1) : -1;

        new Thread(new Runnable() {
                       @Override
                       public void run() {
                           controller.init(id);
                       }
                   }).start();

        if (action.equals(Intent.ACTION_VIEW)) {
            // File intent
            try {
                setUrl(new BasicInputAdapter(new URL(intent.getDataString())));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                          }
                      });
    }

    public void updateNumEntries(AndroidCalendar calendar) {
        final int entries = calendar.numEntries;
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              textCalSize.setText(Integer.toString(entries));
                              exportButton.setEnabled(entries > 0);
                              insertDeleteLayout.setVisibility(View.GONE);
                          }
                      });
    }

    private Button setupButton(int resourceId) {
        Button b = (Button)findViewById(resourceId);
        b.setOnClickListener(controller);
        return b;
    }

    public void setCalendars(List<AndroidCalendar> calendars) {
        this.calendars = calendars;
        List<String> calendarStrings = new ArrayList<String>();
        for (AndroidCalendar cal : calendars) {
            calendarStrings.add(cal.displayName + " (" + cal.id + ")");
        }
        SpinnerTools.simpleSpinnerInUI(this, calendarSpinner, calendarStrings);

        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              List<AndroidCalendar> l = MainActivity.this.calendars;
                              exportButton.setVisibility(l == null ? View.GONE : View.VISIBLE);
                          }
                      });
    }

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

    public void setUrl(BasicInputAdapter urls) {
        setUrls(Collections.singletonList(urls));
    }

    public void setCalendar(final Calendar calendar) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (calendar == null) {
                                  insertDeleteLayout.setVisibility(View.GONE);
                                  return;
                              }

                              Resources res = getResources();
                              final int n = calendar.getComponents(VEvent.VEVENT).size();
                              insertButton.setText(get(res, R.plurals.dialog_entries_insert, n));
                              deleteButton.setText(get(res, R.plurals.dialog_entries_delete, n));
                              insertDeleteLayout.setVisibility(View.VISIBLE);
                          }
                          private String get(Resources res, int id, int n) {
                              return res.getQuantityString(id, n, n);
                          }
                      });
    }

    public AndroidCalendar getSelectedCalendar() {
        if (calendarSpinner.getSelectedItem() != null && calendars != null) {
            String calendarName = calendarSpinner.getSelectedItem().toString();
            for (AndroidCalendar cal : calendars) {
                if ((cal.displayName + " (" + cal.id + ")").equals(calendarName)) {
                    return cal;
                }
            }
        }
        return null;
    }

    public void selectCalendar(long id) {
        for (final AndroidCalendar cal : calendars) {
            if (cal.id == id) {
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
        Object sel = fileSpinner.getSelectedItem();
        return sel == null ? null : (BasicInputAdapter)sel;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.help:
            DialogTools.info(this, R.string.menu_help, Html.fromHtml(getString(R.string.help)));
            break;

        case R.id.settings:
            // Show our Settings view
            this.startActivity(new Intent(this, SettingsActivity.class));
            break;

        case R.id.legal_notices:
            showLegalNotices();
            break;

        default:
            return super.onContextItemSelected(item);
        }

        return true;
    }

    private void showLegalNotices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView text = new TextView(this);
        text.setText(Html.fromHtml(getString(R.string.legal_notices)));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(text);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
