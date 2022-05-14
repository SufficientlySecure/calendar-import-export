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

package org.sufficientlysecure.ical.ui;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.apache.commons.codec.binary.Base64;

import org.sufficientlysecure.ical.AndroidCalendar;
import org.sufficientlysecure.ical.ProcessVEvent;
import org.sufficientlysecure.ical.SaveCalendar;
import org.sufficientlysecure.ical.Settings;
import org.sufficientlysecure.ical.R;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;
import org.sufficientlysecure.ical.util.Log;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
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
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    private static final String TAG = "ICS_MainActivity";

    public static final String LOAD_CALENDAR = "org.sufficientlysecure.ical.LOAD_CALENDAR";
    public static final String EXTRA_CALENDAR_ID = "calendarId";

    private static final int MY_PERMISSIONS_REQUEST = 1;
    private static final String[] MY_PERMISSIONS = new String[] {
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Settings mSettings;

    private CalendarBuilder mCalendarBuilder;
    private Calendar mCalendar;

    private static final long NO_CALENDAR = -1;
    private long mIntentCalendarId = NO_CALENDAR;
    private boolean mInitialCreated = false;

    private IntentFilter mCalendarUpdateFilter;
    private BroadcastReceiver mCalendarUpdateReciever;

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

    private TextView mTextCalName;
    private TextView mTextCalAccountName;
    private TextView mTextCalAccountType;
    private TextView mTextCalOwner;
    private TextView mTextCalState;
    private TextView mTextCalId;
    private TextView mTextCalTimezone;
    private TextView mTextCalSize;

    // Values
    private List<AndroidCalendar> mCalendars;
    private ScrollView mScrollViewMain;
    private LinearLayout mInsertDeleteLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mIntentCalendarId = NO_CALENDAR;
        mInitialCreated = true;

        // Create a receiver for calendar updates.
        mCalendarUpdateFilter = new IntentFilter("android.intent.action.PROVIDER_CHANGED");
        mCalendarUpdateReciever = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                Log.d(TAG, "Received broadcast: " + intent.getAction());
                if (intent.getAction() == mCalendarUpdateFilter.getAction(0))
                    onExternalCalendarChanged();
            }
        };

        initView();

        if (hasPermissions())
            initIntent();
    }

    private boolean isGranted(final String permission) {
        final int status = ContextCompat.checkSelfPermission(this, permission);
        return status == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        boolean allGranted = true;
        for (final String permission : MY_PERMISSIONS)
            if (!isGranted(permission)) {
                allGranted = false;
                break;
            }

        if (allGranted)
            return true;

        ActivityCompat.requestPermissions(this, MY_PERMISSIONS, MY_PERMISSIONS_REQUEST);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                boolean allGranted = false;

                if (grantResults.length > 0) {
                    allGranted = true;
                    for (int grantResult : grantResults)
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                            break;
                        }
                }

                if (!allGranted) {
                    Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                initIntent();
            }
        }
    }

    private void initView() {
        mSettings = new Settings(PreferenceManager.getDefaultSharedPreferences(this));
        SettingsActivity.processSettings(mSettings);

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
                if (calendar.mTimezone == null)
                    mTextCalTimezone.setText(R.string.not_applicable);
                else
                    mTextCalTimezone.setText(calendar.mTimezone);
                mSettings.putLong(Settings.PREF_LASTCALENDARID, calendar.mId);
                mSettings.putString(Settings.PREF_LASTCALENDARNAME, calendar.mName);
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
        mScrollViewMain = (ScrollView) findViewById(R.id.ScrollViewMain);
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
    }

    private void initIntent() {
        Intent intent = getIntent();
        if (intent == null)
            return;

        String action = intent.getAction();

        if (action.equals(LOAD_CALENDAR))
            mIntentCalendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, NO_CALENDAR);

        onExternalCalendarChanged();

        if (action.equals(Intent.ACTION_VIEW))
            setSource(null, intent.getData(), null, null); // File intent
    }

    public Settings getSettings() {
        return mSettings;
    }

    private boolean isListUpdate(final List<AndroidCalendar> calendars) {
        if (mCalendars == null) {
            Log.d(TAG, "First time init of calendar list");
            return true;
        }
        if (mCalendars.size() != calendars.size()) {
            Log.i(TAG, "A calendar has been added or removed");
            return true;
        }
        for (int i = 0; i < mCalendars.size(); ++i) {
            if (mCalendars.get(i).differsFrom(calendars.get(i))) {
                Log.i(TAG, "A calendar or its events has changed");
                return true;
            }
        }
        Log.d(TAG, "No calendar changes found");
        return false; // No differences
    }

    private void initialiseCalendars() {
        Log.d(TAG, "initialiseCalendars");

        List<AndroidCalendar> calendars = AndroidCalendar.loadAll(getContentResolver());
        if (calendars.isEmpty()) {
            Runnable task;
            task = new Runnable() {
                public void run() {
                    DialogInterface.OnClickListener okTask;
                    okTask = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface iface, int id) {
                            iface.cancel();
                            MainActivity.this.finish();
                        }
                    };
                    new AlertDialog.Builder(MainActivity.this)
                                   .setMessage(R.string.no_calendars_found)
                                   .setIcon(R.mipmap.ic_launcher)
                                   .setTitle(R.string.information)
                                   .setCancelable(false)
                                   .setPositiveButton(android.R.string.ok, okTask).create()
                                   .show();
                }
            };
            runOnUiThread(task);
        }

        if (!isListUpdate(calendars))
            return;

        // FIXME: If we already have initialised then:
        //  a) Preserve our chosen calendar selection if it still exists

        mCalendars = calendars;
        setupSpinner(mCalendarSpinner, mCalendars, mExportButton);

        String calendarName = null;

        if (mIntentCalendarId == NO_CALENDAR) {
            // Not loading from an Intent: use the previously selected calendar
            mIntentCalendarId = mSettings.getLong(mSettings.PREF_LASTCALENDARID, NO_CALENDAR);
            if (mIntentCalendarId != NO_CALENDAR)
                calendarName = mSettings.getString(mSettings.PREF_LASTCALENDARNAME);
        }

        boolean found = false;
        for (int i = 0; i < mCalendars.size(); i++) {
            if (mCalendars.get(i).mId == mIntentCalendarId &&
                mCalendars.get(i).mName != null &&
                (calendarName == null || mCalendars.get(i).mName.contentEquals(calendarName))) {
                found = true;
                final int index = i;
                runOnUiThread(new Runnable() {
                    public void run() {
                        mCalendarSpinner.setSelection(index);
                    }
                });
                break;
            }
        }
        if (!found)
            mIntentCalendarId = NO_CALENDAR; // Don't try to match this id again
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
                          public void run() {
                              Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                          }
                      });
    }

    public void updateNumEntries(AndroidCalendar calendar) {
        final int entries = calendar.mNumEntries;
        runOnUiThread(new Runnable() {
                          public void run() {
                              mTextCalSize.setText(Integer.toString(entries));
                              mExportButton.setEnabled(entries > 0);
                              mInsertDeleteLayout.setVisibility(View.GONE);
                          }
                      });
    }

    private Button setupButton(int id) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(this);
        return button;
    }

    private <E> void setupSpinner(final Spinner spinner, final List<E> list, final Button button) {
        final int id = android.R.layout.simple_spinner_item;
        final int dropId = android.R.layout.simple_spinner_dropdown_item;
        final Context ctx = this;

        runOnUiThread(new Runnable() {
                          public void run() {
                              ArrayAdapter<E> adaptor = new ArrayAdapter<>(ctx, id, list);
                              adaptor.setDropDownViewResource(dropId);
                              spinner.setAdapter(adaptor);
                              if (list.size() != 0)
                                  spinner.setVisibility(View.VISIBLE);
                              button.setVisibility(View.VISIBLE);
                          }
                      });
    }

    private void setSources(List<CalendarSource> sources) {
        setupSpinner(mFileSpinner, sources, mLoadButton);
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mInitialCreated)
            mInitialCreated = false; // Init already done by onCreate()
        else
            onExternalCalendarChanged();

        registerReceiver(mCalendarUpdateReciever, mCalendarUpdateFilter);
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        unregisterReceiver(mCalendarUpdateReciever);
    }

    private void onExternalCalendarChanged() {
        new Thread(new Runnable() {
                           public void run() {
                               // Update view if any source calendar was modified
                               MainActivity.this.initialiseCalendars();
                           }
                       }).start();
    }

    public boolean setSource(String url, Uri uri, String username, String password) {
        try {
            CalendarSource source = new CalendarSource(url, uri, username, password);
            setSources(Collections.singletonList(source));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public AndroidCalendar getSelectedCalendar() {
        return (AndroidCalendar) mCalendarSpinner.getSelectedItem();
    }

    public InputStream getSelectedURI() throws IOException {
        CalendarSource sel = (CalendarSource) mFileSpinner.getSelectedItem();
        return sel == null ? null : sel.getStream();
    }

    public String generateUid() {
        // Generated UIDs take the form <ms>-<uuid>@sufficientlysecure.org.
        if (mUidTail == null) {
            String uidPid = mSettings.getString(Settings.PREF_UIDPID);
            if (uidPid.length() == 0) {
                uidPid = UUID.randomUUID().toString().replace("-", "");
                mSettings.putString(Settings.PREF_UIDPID, uidPid);
            }
            mUidTail = uidPid + "@sufficientlysecure.org";
        }

        mUidMs = Math.max(mUidMs, System.currentTimeMillis());
        String uid = Long.toString(mUidMs) + mUidTail;
        mUidMs++;

        return uid;
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
            DialogTools.info(this, R.string.help, Html.fromHtml(getString(R.string.help_html)));
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
        TextView text = new TextView(this);
        text.setText(Html.fromHtml(getString(R.string.legal_notices_html)));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        new AlertDialog.Builder(this).setView(text).create().show();
    }

    private class CalendarSource {
        private static final String HTTP_SEP = "://";

        private URL mUrl = null;
        private Uri mUri = null;
        private final String mString;
        private final String mUsername;
        private final String mPassword;

        public CalendarSource(String url, Uri uri,
                              String username, String password) throws MalformedURLException {
            if (url != null) {
                mUrl = new URL(url);
                mString = mUrl.toString();
            } else {
                mUri = uri;
                mString = uri.toString();
            }
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
                    String encoded = new String(Base64.encodeBase64(userPass.getBytes("UTF-8")));
                    URLConnection connection = mUrl.openConnection();
                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                    return connection;
                }
            }
            return mUrl.openConnection();
        }

        public InputStream getStream() throws IOException {
            if (mUri != null)
                return getContentResolver().openInputStream(mUri);
            URLConnection c = this.getConnection();
            return c == null ? null : c.getInputStream();
        }

        @Override
        public String toString() {
            return mString;
        }
    }

    private class SearchForFiles extends RunnableWithProgress {
        public SearchForFiles(MainActivity activity) {
            super(activity, R.string.searching_for_files, false);
        }

        private void search(File root, List<CalendarSource> sources, String... extensions) {
            if (!root.isFile()) {
                File[] children = root.listFiles();
                if (children == null)
                    return;
                for (File file: children)
                    search(file, sources, extensions);
            }
            for (String ext: extensions) {
                if (root.toString().endsWith(ext)) {
                    try {
                        final String url = root.toURI().toURL().toString();
                        sources.add(new CalendarSource(url, null, null, null));
                    } catch (MalformedURLException e) {
                        // Can't happen
                    }
                }
            }
        }

        @Override
        protected void run() throws Exception {
            List<CalendarSource> sources = new ArrayList<>();
            search(Environment.getExternalStorageDirectory(), sources, "ics", "ical", "icalendar");
            getActivity().setSources(sources);
        }
    }

    private class LoadFile extends RunnableWithProgress {
        public LoadFile(MainActivity activity) {
            super(activity, R.string.reading_file_please_wait, false);
        }

        private void setHint(String key, boolean value) {
            CompatibilityHints.setHintEnabled(key, value);
        }

        @Override
        protected void run() throws Exception {
            setHint(CompatibilityHints.KEY_RELAXED_UNFOLDING, mSettings.getIcal4jUnfoldingRelaxed());
            setHint(CompatibilityHints.KEY_RELAXED_PARSING, mSettings.getIcal4jParsingRelaxed());
            setHint(CompatibilityHints.KEY_RELAXED_VALIDATION, mSettings.getIcal4jValidationRelaxed());
            setHint(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, mSettings.getIcal4jCompatibilityOutlook());
            setHint(CompatibilityHints.KEY_NOTES_COMPATIBILITY, mSettings.getIcal4jCompatibilityNotes());
            setHint(CompatibilityHints.KEY_VCARD_COMPATIBILITY, mSettings.getIcal4jCompatibilityVcard());

            if (mCalendarBuilder == null)
                mCalendarBuilder = new CalendarBuilder();

            mCalendar = mCalendarBuilder.build(getSelectedURI());


            runOnUiThread(new Runnable() {
                              public void run() {
                                  if (mCalendar == null) {
                                      mInsertDeleteLayout.setVisibility(View.GONE);
                                      return;
                                  }

                                  Resources res = getResources();
                                  final int n = mCalendar.getComponents(VEvent.VEVENT).size();
                                  mInsertButton.setText(get(res, R.plurals.insert_n_entries, n));
                                  mDeleteButton.setText(get(res, R.plurals.delete_n_entries, n));
                                  mInsertDeleteLayout.setVisibility(View.VISIBLE);
                                  mScrollViewMain.post(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               mScrollViewMain.fullScroll(ScrollView.FOCUS_DOWN);
                                                           }
                                  });
                              }
                              private String get(Resources res, int id, int n) {
                                  return res.getQuantityString(id, n, n);
                              }
                          });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.SetUrlButton:
                UrlDialog.show(this);
                break;
            case R.id.SearchButton:
                new SearchForFiles(this).start();
                break;
            case R.id.LoadButton:
                new LoadFile(this).start();
                break;
            case R.id.SaveButton:
                new SaveCalendar(this).start();
                break;
            case R.id.InsertButton:
            case R.id.DeleteButton:
                new ProcessVEvent(this, mCalendar, view.getId() == R.id.InsertButton).start();
                break;
        }
    }
}
