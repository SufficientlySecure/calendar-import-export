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
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    public static final String LOAD_CALENDAR = "org.sufficientlysecure.ical.LOAD_CALENDAR";
    public static final String EXTRA_CALENDAR_ID = "calendarId";

    private Settings mSettings;

    private CalendarBuilder mCalendarBuilder;
    private Calendar mCalendar;

    // UID generation
    private long mUidMs = 0;
    private String mUidTail;

    // Views
    private Spinner mCalendarSpinner;
    private Spinner mFileSpinner;
    //private Button mLoadButton;
    private Button mInsertButton;
    //private Button mDeleteButton;
    private Button mExportButton;
    private Button mClearEventButton;
    private Button mClearReminderButton;

    /*private TextView mTextCalName;
    private TextView mTextCalAccountName;
    private TextView mTextCalAccountType;
    private TextView mTextCalOwner;
    private TextView mTextCalId;
    */
    private TextView mTextCalTimezone;
    private TextView mTextCalSize;
    private TextView mTextCalState;
    private TextView mtextIcsNbEntries;
    private TextView mtextIcsFirstDate;
    private TextView mtextIcsLastDate;

    // Values
    private List<AndroidCalendar> mCalendars;
    private ScrollView mScrollViewMain;
    //private LinearLayout mInsertDeleteLayout;

    //preferences
    private boolean isErasePreviousCalendar;
    private boolean istoggleEraseImportedReminder;

    private MainActivity mMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainActivity = this;

        setContentView(R.layout.main);

        mSettings = new Settings(PreferenceManager.getDefaultSharedPreferences(this));
        SettingsActivity.processSettings(mSettings);

        // Retrieve views
        mCalendarSpinner = (Spinner) findViewById(R.id.SpinnerChooseCalendar);
        AdapterView.OnItemSelectedListener calListener;
        calListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AndroidCalendar calendar = mCalendars.get(pos);
                /*mTextCalName.setText(calendar.mName);
                mTextCalAccountName.setText(calendar.mAccountName);
                mTextCalAccountType.setText(calendar.mAccountType);
                mTextCalOwner.setText(calendar.mOwner);
                mTextCalId.setText(calendar.mIdStr);
                */
                mTextCalState.setText(calendar.mIsActive ? R.string.active : R.string.inactive);
                mSettings.putLong(Settings.PREF_CALENDAR, calendar.mId);
                if (calendar.mTimezone == null)
                    mTextCalTimezone.setText(R.string.not_applicable);
                else
                    mTextCalTimezone.setText(calendar.mTimezone);
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
                new LoadFile(mMainActivity).start();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        mFileSpinner.setOnItemSelectedListener(fileListener);

        setupButton(R.id.SearchButton);
        mClearEventButton = setupButton(R.id.ClearEventButton);
        mClearReminderButton = setupButton(R.id.ClearReminderButton);
        //mLoadButton = setupButton(R.id.LoadButton);
        mInsertButton = setupButton(R.id.InsertButton);
        mExportButton = setupButton(R.id.SaveButton);
        mScrollViewMain = (ScrollView) findViewById(R.id.ScrollViewMain);
        //mInsertDeleteLayout = (LinearLayout) findViewById(R.id.InsertDeleteLayout);
        setupButton(R.id.SetUrlButton);

        /*mTextCalName = (TextView) findViewById(R.id.TextCalName);
        mTextCalAccountName = (TextView) findViewById(R.id.TextCalAccountName);
        mTextCalAccountType = (TextView) findViewById(R.id.TextCalAccountType);
        mTextCalOwner = (TextView) findViewById(R.id.TextCalOwner);
        mTextCalId = (TextView) findViewById(R.id.TextCalId);
*/
        mTextCalState = (TextView) findViewById(R.id.TextCalState);
        mTextCalTimezone = (TextView) findViewById(R.id.TextCalTimezone);
        mTextCalSize = (TextView) findViewById(R.id.TextCalSize);
        mtextIcsNbEntries = (TextView) findViewById(R.id.textIcsNbEntries);
        mtextIcsFirstDate = (TextView) findViewById(R.id.textIcsFirstDate);
        mtextIcsLastDate = (TextView) findViewById(R.id.textIcsLastDate);

        //toggle for export
        ToggleButton toggleErasePreviousCalendar = (ToggleButton) findViewById(R.id.toggleErasePreviousCalendar);
        isErasePreviousCalendar = mSettings.getBoolean(Settings.PREF_IS_ERASE_PREVIOUS_CALENDAR);
        toggleErasePreviousCalendar.setChecked(isErasePreviousCalendar);
        toggleErasePreviousCalendar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.putBoolean(Settings.PREF_IS_ERASE_PREVIOUS_CALENDAR, isChecked);
                isErasePreviousCalendar = isChecked;
            }
        });

        ToggleButton toggleEraseImportedReminder = (ToggleButton) findViewById(R.id.toggleEraseImportedReminder);
        istoggleEraseImportedReminder = mSettings.getBoolean(Settings.PREF_IS_ERASE_REMINDER);
        toggleEraseImportedReminder.setChecked(istoggleEraseImportedReminder);
        toggleEraseImportedReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.putBoolean(Settings.PREF_IS_ERASE_REMINDER, isChecked);
                istoggleEraseImportedReminder = isChecked;
            }
        });

        //intent gestion and preload stuff
        Intent intent = getIntent();
        if (intent == null)
            return;

        String action = intent.getAction();

        final long id = action.equals(LOAD_CALENDAR) ? intent.getLongExtra(EXTRA_CALENDAR_ID, -1) : -1;

        new Thread(new Runnable() {
                       public void run() {
                           MainActivity.this.init(id);
                       }
                   }).start();

        if (action.equals(Intent.ACTION_VIEW))
            setUri(intent.getData(), null, null); // File intent
    }

    public Settings getSettings() {
        return mSettings;
    }

    private void init(long calendarId) {
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

        mCalendars = calendars;


        setupSpinner(mCalendarSpinner, mCalendars);

        //get the previews choice
        if (calendarId == -1) {
            calendarId = mSettings.getLong(mSettings.PREF_CALENDAR);
        }
        for (int i = 0; i < mCalendars.size(); i++) {
            if (mCalendars.get(i).mId == calendarId) {
                final int index = i;
                runOnUiThread(new Runnable() {
                                  public void run() {
                                      mCalendarSpinner.setSelection(index);
                                  }
                              });
                break;
            }
        }
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
                mClearEventButton.setEnabled(entries > 0);
                mClearReminderButton.setEnabled(entries > 0);
                //mInsertDeleteLayout.setVisibility(View.GONE);
            }
        });
    }

    private Button setupButton(int id) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(this);
        return button;
    }

    private <E> void setupSpinner(final Spinner spinner, final List<E> list) {
        final int id = android.R.layout.simple_spinner_item;
        final int dropId = android.R.layout.simple_spinner_dropdown_item;
        final Context ctx = this;

        runOnUiThread(new Runnable() {
            public void run() {
                ArrayAdapter<E> adaptor = new ArrayAdapter<>(ctx, id, list);
                adaptor.setDropDownViewResource(dropId);
                spinner.setAdapter(adaptor);

                /*if (list.size() != 0) {
                    spinner.setVisibility(View.VISIBLE);
                }
                button.setVisibility(View.VISIBLE);*/
            }
        });
    }

    private void setSources(List<CalendarSource> sources) {
        setupSpinner(mFileSpinner, sources);
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

    public boolean setUri(Uri uri, String username, String password) {
        try {
            CalendarSource source = new CalendarSource(uri, username, password);
            setSources(Collections.singletonList(source));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public AndroidCalendar getSelectedCalendar() {
        return (AndroidCalendar) mCalendarSpinner.getSelectedItem();
    }

    public InputStream getSelectedICSasInputStream() throws IOException {

        CalendarSource sel = (CalendarSource) mFileSpinner.getSelectedItem();

        return sel == null ? null : sel.getStream();
    }

    /*public URLConnection getSelectedURL() throws IOException {
        CalendarSource sel = (CalendarSource) mFileSpinner.getSelectedItem();
        return sel == null ? null : sel.getConnection();
    }*/

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

        long ms = System.currentTimeMillis();
        if (mUidMs == ms)
            ms++; // Force ms to be unique

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

    @Override
    public void onClick(View view) {
        AlertDialog.Builder builder;
        switch (view.getId()) {
            case R.id.ClearEventButton:
                builder = new AlertDialog.Builder(this);
                //builder.setMessage("Message above the image");
                builder.setPositiveButton("Ok ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ClearCalendar(mMainActivity, true, false).start();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setMessage(R.string.clear_warning);
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setTitle(R.string.warning);
                builder.create().show();
                break;
            case R.id.ClearReminderButton:
                builder = new AlertDialog.Builder(this);
                //builder.setMessage("Message above the image");
                builder.setPositiveButton("Ok ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ClearCalendar(mMainActivity, false, true).start();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setMessage(R.string.clear_warning);
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setTitle(R.string.warning);
                builder.create().show();
                break;
            case R.id.SetUrlButton:
                UrlDialog.show(this);
                break;
            case R.id.SearchButton:
                new SearchForFiles(this).start();
                break;
            //case R.id.LoadButton:
            //    new LoadFile(this).start();
            //    break;
            case R.id.SaveButton:
                new SaveCalendar(this).start();
                break;
            case R.id.InsertButton:
                if (isErasePreviousCalendar)
                    new ClearCalendar(mMainActivity, true, false).start();
                new ProcessVEvent(this, mCalendar, true).start();
                if (istoggleEraseImportedReminder)
                    new ClearCalendar(mMainActivity, false, true).start();
                break;
        }
    }

    private class CalendarSource {
        private static final String HTTP_SEP = "://";
        private final String mUsername;
        private final String mPassword;
        private URL mUrl = null;
        private Uri mUri = null;
        private String mString;

        public CalendarSource(URL url, String username, String password) {
            mUrl = url;
            mString = url.toString();
            mUsername = username;
            mPassword = password;
        }

        public CalendarSource(Uri uri, String username, String password) {
            mUri = uri;
            mString = uri.toString();
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
                    String encoded = new String(new Base64().encode(userPass.getBytes("UTF-8")));
                    URLConnection connection = mUrl.openConnection();
                    connection.setRequestProperty("Authorization", "Basic " + encoded);
                    return connection;
                }
            }
            return mUrl.openConnection();
        }

        public InputStream getStream() throws IOException {
            InputStream in;

            if (mUri != null) {
                ContentResolver contentResolver = getContentResolver();
                in = contentResolver.openInputStream(mUri);
            } else {
                URLConnection c = this.getConnection();
                in = c == null ? null : c.getInputStream();
            }
            return in;
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
                        sources.add(new CalendarSource(root.toURI().toURL(), null, null));
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

            //URLConnection c = getSelectedURL();
            //InputStream in = c == null ? null : c.getInputStream();
            //mCalendar = in == null ? null : mCalendarBuilder.build(in);
            mCalendar = mCalendarBuilder.build(getSelectedICSasInputStream());

            //get basic information on ICS calendar (start date end date and nb of event)
            ComponentList mIcsEventList = mCalendar.getComponents(VEvent.VEVENT);
            final int n = mIcsEventList.size();
            Date first = null;
            Date last = null;
            for (Object temp : mIcsEventList) {
                VEvent e = (VEvent) temp;
                Date current = e.getStartDate().getDate();
                if (first == null || first.after(current))
                    first = current;
                if (last == null || last.before(current))
                    last = current;
            }
            final String strfirst = first.toString().substring(0, 8);
            final String strlast = last.toString().substring(0, 8);
            final String numberOfEvent = Integer.toString(n);

            runOnUiThread(new Runnable() {
                public void run() {


                    if (mCalendar == null) {
                        mtextIcsNbEntries.setText(R.string.not_available);
                        mtextIcsFirstDate.setText(R.string.not_available);
                        mtextIcsLastDate.setText(R.string.not_available);
                        //mInsertDeleteLayout.setVisibility(View.GONE);
                        return;
                    }


                    Resources res = getResources();

                    //mInsertButton.setText(get(res, R.plurals.insert_n_entries, n));

                    mtextIcsNbEntries.setText(numberOfEvent);
                    mtextIcsFirstDate.setText(strfirst);
                    mtextIcsLastDate.setText(strlast);
                    //mDeleteButton.setText(get(res, R.plurals.delete_n_entries, n));
                    //mInsertDeleteLayout.setVisibility(View.VISIBLE);
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

    private class ClearCalendar extends RunnableWithProgress {
        private MainActivity mMainActivity;
        private boolean isClearEvent = false;
        private boolean isClearReminder = false;

        public ClearCalendar(MainActivity activity, boolean clearEvent, boolean clearReminder) {
            super(activity, R.string.erasing_data, false);
            isClearEvent = clearEvent;
            isClearReminder = clearReminder;
            mMainActivity = activity;
        }


        @Override
        protected void run() throws Exception {
            final AndroidCalendar selectedCal = mMainActivity.getSelectedCalendar();
            ContentResolver contentResolver = getContentResolver();

            Uri reminderUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                reminderUri = Uri.parse("content://com.android.calendar/reminders");
            else
                reminderUri = Uri.parse("content://calendar/reminders");

            Uri eventUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                eventUri = Uri.parse("content://com.android.calendar/events");
            else
                eventUri = Uri.parse("content://calendar/events");

            if (isClearEvent || isClearReminder) {

                Cursor cursor = contentResolver.query(eventUri, new String[]{"_id"}, "calendar_id = " + selectedCal.mId, null, null); // calendar_id can change in new versions
                while (cursor.moveToNext()) {

                    if (isClearEvent) {
                        Uri deleteUri = ContentUris.withAppendedId(eventUri, cursor.getInt(0));
                        contentResolver.delete(deleteUri, null, null);
                    } else {
                        if (isClearReminder) {
                            Cursor reminderCursor = contentResolver.query(reminderUri, new String[]{"_id"}, "event_id = " + cursor.getInt(0), null, null); // calendar_id can change in new versions
                            while (reminderCursor.moveToNext()) {
                                Uri deleteUri = ContentUris.withAppendedId(reminderUri, reminderCursor.getInt(0));
                                contentResolver.delete(deleteUri, null, null);
                            }
                        }
                    }
                }
            }
        }
    }
}
