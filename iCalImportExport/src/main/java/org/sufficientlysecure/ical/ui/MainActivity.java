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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.commons.codec.binary.Base64;

import org.sufficientlysecure.ical.AndroidCalendar;
import org.sufficientlysecure.ical.Controller;
import org.sufficientlysecure.ical.R;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

public class MainActivity extends Activity {
    public static final String LOAD_CALENDAR = "org.sufficientlysecure.ical.LOAD_CALENDAR";
    public static final String EXTRA_CALENDAR_ID = "calendarId";

    // FIXME: These should be abstracted into a preferences class
    public static SharedPreferences preferences;

    // UID generation
    private long mUidMs = 0;
    private String mUidTail;

    // Views
    private Spinner mCalendarSpinner;
    private Spinner mFileSpinner;
    private Button mLoadButton;
    private Button mInsertButton;
    private Button mDeleteButton;
    private Button mExportButton;
    private Controller mController;

    private TextView mTextCalName;
    private TextView mTextCalAccountName;
    private TextView mTextCalAccountType;
    private TextView mTextCalOwner;
    private TextView mTextCalState;
    private TextView mTextCalId;
    private TextView mTextCalTimezone;
    private TextView mTextCalSize;

    // Values
    private List<CalendarSource> mSources;
    private List<AndroidCalendar> mCalendars;
    private LinearLayout mInsertDeleteLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mController = new Controller(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieve views
        mCalendarSpinner = (Spinner) findViewById(R.id.SpinnerChooseCalendar);
        AdapterView.OnItemSelectedListener calListener;
        calListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AndroidCalendar calendar = mCalendars.get(pos);
                mTextCalName.setText(calendar.mName);
                mTextCalAccountName.setText(calendar.mAccountName);
                mTextCalAccountType.setText(calendar.mAccountType);
                mTextCalOwner.setText(calendar.mOwner);
                mTextCalState.setText(calendar.mIsActive ? R.string.active : R.string.inactive);
                mTextCalId.setText(calendar.mIdStr);
                if (calendar.mTimezone == null) {
                    mTextCalTimezone.setText(R.string.not_applicable);
                } else {
                    mTextCalTimezone.setText(calendar.mTimezone);
                }
                updateNumEntries(calendar);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        mCalendarSpinner.setOnItemSelectedListener(calListener);

        mFileSpinner = (Spinner) findViewById(R.id.SpinnerFile);
        AdapterView.OnItemSelectedListener fileListener;
        fileListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mInsertDeleteLayout.setVisibility(View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        mFileSpinner.setOnItemSelectedListener(fileListener);

        setupButton(R.id.SearchButton);
        mLoadButton = setupButton(R.id.LoadButton);
        mInsertButton = setupButton(R.id.InsertButton);
        mDeleteButton = setupButton(R.id.DeleteButton);
        mExportButton = setupButton(R.id.SaveButton);
        mInsertDeleteLayout = (LinearLayout) findViewById(R.id.InsertDeleteLayout);
        setupButton(R.id.SetUrlButton);

        mTextCalName = (TextView) findViewById(R.id.TextCalName);
        mTextCalAccountName = (TextView) findViewById(R.id.TextCalAccountName);
        mTextCalAccountType = (TextView) findViewById(R.id.TextCalAccountType);
        mTextCalOwner = (TextView) findViewById(R.id.TextCalOwner);
        mTextCalState = (TextView) findViewById(R.id.TextCalState);
        mTextCalId = (TextView) findViewById(R.id.TextCalId);
        mTextCalTimezone = (TextView) findViewById(R.id.TextCalTimezone);
        mTextCalSize = (TextView) findViewById(R.id.TextCalSize);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        final int id = action.equals(LOAD_CALENDAR) ? intent.getIntExtra(EXTRA_CALENDAR_ID, -1) : -1;

        new Thread(new Runnable() {
                       @Override
                       public void run() {
                           mController.init(id);
                       }
                   }).start();

        if (action.equals(Intent.ACTION_VIEW)) {
            // File intent
            setUrl(intent.getDataString(), null, null);
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
        final int entries = calendar.mNumEntries;
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              mTextCalSize.setText(Integer.toString(entries));
                              mExportButton.setEnabled(entries > 0);
                              mInsertDeleteLayout.setVisibility(View.GONE);
                          }
                      });
    }

    private Button setupButton(int resourceId) {
        Button b = (Button) findViewById(resourceId);
        b.setOnClickListener(mController);
        return b;
    }

    private <E> void setupSpinner(final Spinner spinner, final List<E> list, final Button b) {
        final int id = android.R.layout.simple_spinner_item;
        final int dropId = android.R.layout.simple_spinner_dropdown_item;
        final Context ctx = (Context) this;

        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              ArrayAdapter<E> adaptor = new ArrayAdapter<E>(ctx, id, list);
                              adaptor.setDropDownViewResource(dropId);
                              spinner.setAdapter(adaptor);
                              if (list.size() != 0) {
                                  spinner.setVisibility(View.VISIBLE);
                              }
                              b.setVisibility(View.VISIBLE);
                          }
                      });
    }

    public void setCalendars(List<AndroidCalendar> calendars) {
        mCalendars = calendars;
        setupSpinner(mCalendarSpinner, mCalendars, mExportButton);
    }

    private void setSources(List<CalendarSource> sources) {
        mSources = sources;
        setupSpinner(mFileSpinner, mSources, mLoadButton);
    }

    public void setFiles(List<File> files) {
        List<CalendarSource> sources = new ArrayList<CalendarSource>(files.size());

        for (File file: files) {
            try {
                sources.add(new CalendarSource(file.toURI().toURL(), null, null));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        setSources(sources);
    }

    public boolean setUrl(String url, String username, String password) {
        try {
            CalendarSource source = new CalendarSource(new URL(url), username, password);
            setSources(Collections.singletonList(source));
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public void setCalendar(final Calendar calendar) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (calendar == null) {
                                  mInsertDeleteLayout.setVisibility(View.GONE);
                                  return;
                              }

                              Resources res = getResources();
                              final int n = calendar.getComponents(VEvent.VEVENT).size();
                              mInsertButton.setText(get(res, R.plurals.dialog_entries_insert, n));
                              mDeleteButton.setText(get(res, R.plurals.dialog_entries_delete, n));
                              mInsertDeleteLayout.setVisibility(View.VISIBLE);
                          }
                          private String get(Resources res, int id, int n) {
                              return res.getQuantityString(id, n, n);
                          }
                      });
    }

    public AndroidCalendar getSelectedCalendar() {
        return (AndroidCalendar) mCalendarSpinner.getSelectedItem();
    }

    public void selectCalendar(long id) {
        for (int i = 0; i < mCalendars.size(); i++) {
            if (mCalendars.get(i).mId == id) {
                final int index = i;
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      mCalendarSpinner.setSelection(index);
                                  }
                              });
                return;
            }
        }
    }

    public URLConnection getSelectedURL() throws IOException {
        CalendarSource sel = (CalendarSource) mFileSpinner.getSelectedItem();
        return sel == null ? null : sel.getConnection();
    }

    public String generateUid() {
        // Generated UIDs take the form <ms>-<uuid>@sufficientlysecure.org.
        if (mUidTail == null) {
            String uidPid = preferences.getString("uidPid", null);
            if (uidPid == null) {
                uidPid = UUID.randomUUID().toString().replace("-", "");
                preferences.edit().putString("uidPid", uidPid).commit();
            }
            mUidTail = uidPid + "@sufficientlysecure.org";
        }

        long ms = System.currentTimeMillis();
        if (mUidMs == ms) {
            ms++; // Force ms to be unique within the app
        }
        mUidMs = ms;
        return Long.toString(ms) + mUidTail;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
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
            startActivity(new Intent(this, SettingsActivity.class));
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

    private class CalendarSource {
        private static final String HTTP_SEP = "://";

        private URL mUrl;
        private String mUsername;
        private String mPassword;

        public CalendarSource(URL url, String username, String password) {
            mUrl = url;
            mUsername = username;
            mPassword = password;
        }

        public URLConnection getConnection() throws IOException {
            if (mUsername != null) {
                String protocol = mUrl.getProtocol();
                String userPass = mUsername + ":" + mPassword;

                if (protocol.equalsIgnoreCase("ftp") || protocol.equalsIgnoreCase("ftps")) {
                    String external = mUrl.toExternalForm();
                    String end = external.substring(protocol.length() + HTTP_SEP.length());
                    return new URL(protocol + HTTP_SEP + userPass + "@" + end).openConnection();
                }

                if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
                    String encoded = new String(new Base64().encode(userPass.getBytes()));
                    URLConnection connection = mUrl.openConnection();
                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                    return connection;
                }
            }
            return mUrl.openConnection();
        }

        @Override
        public String toString() {
            return mUrl.toString();
        }
    }
}
