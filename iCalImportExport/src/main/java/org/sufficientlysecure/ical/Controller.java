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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.SettingsActivity;
import org.sufficientlysecure.ical.ui.dialogs.Credentials;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;
import org.sufficientlysecure.ical.util.BasicInputAdapter;
import org.sufficientlysecure.ical.util.CredentialInputAdapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

@SuppressLint("NewApi")
public class Controller implements OnClickListener {
    private static final String TAG = Controller.class.getName();
    private static final String PREF_LAST_URL = "lastUrl";
    private static final String PREF_LAST_USERNAME = "lastUrlUsername";
    private static final String PREF_LAST_PASSWORD = SettingsActivity.PREF_LAST_URL_PASSWORD;

    private MainActivity activity;
    public static CalendarBuilder calendarBuilder;
    private Calendar calendar;

    public Controller(MainActivity activity) {
        this.activity = activity;
    }

    public void init(long calendarId) {
        List<AndroidCalendar> cals = AndroidCalendar.loadAll(activity.getContentResolver());
        if (cals.isEmpty()) {
            noCalendarFinish();
        }
        activity.setCalendars(cals);
        activity.selectCalendar(calendarId);
    }

    private void noCalendarFinish() {
        Runnable task;
        task = new Runnable() {
            @Override
            public void run() {
                DialogInterface.OnClickListener buttonTask;
                buttonTask = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        activity.finish();
                    }
                };
                AlertDialog d = new AlertDialog.Builder(activity)
                                .setMessage(R.string.dialog_exiting)
                                .setIcon(R.drawable.icon)
                                .setTitle(R.string.dialog_information_title)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, buttonTask).create();
                d.show();
            }
        };
        activity.runOnUiThread(task);
    }

    private void setHint(SharedPreferences prefs, String key) {
        CompatibilityHints.setHintEnabled(key, prefs.getBoolean(key, true));
    }

    private void searchFiles(File root, List<File> files, String... extension) {
        if (root.isFile()) {
            for (String string: extension) {
                if (root.toString().endsWith(string)) {
                    files.add(root);
                }
            }
        } else {
            File[] children = root.listFiles();
            if (children != null) {
                for (File file: children) {
                    searchFiles(file, files, extension);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        RunnableWithProgress task = null;
        int style = ProgressDialog.STYLE_SPINNER;

        // Handling search for file event
        if (v.getId() == R.id.SearchButton) {

            task = new RunnableWithProgress(activity) {
                @Override
                public void run(ProgressDialog dialog) {
                    setMessage(R.string.progress_searching_ical_files);

                    File root = Environment.getExternalStorageDirectory();
                    List<File> files = new ArrayList<File>();
                    searchFiles(root, files, "ics", "ical", "icalendar");
                    List<BasicInputAdapter> urls = new ArrayList<BasicInputAdapter>(files.size());

                    for (File file: files) {
                        try {
                            urls.add(new BasicInputAdapter(file.toURI().toURL()));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                    // Collections.sort(urls, new );
                    activity.setUrls(urls);
                }
            };
        }
        else if (v.getId() == R.id.LoadButton) {

            task = new RunnableWithProgress(activity) {
                @Override
                public void run(ProgressDialog dialog) {
                    if (calendarBuilder == null) {
                        setMessage(R.string.progress_loading_builder);
                        calendarBuilder = new CalendarBuilder();
                    }
                    try {
                        setMessage(R.string.progress_reading_ical);
                        InputStream in = activity.getSelectedURL().getConnection().getInputStream();
                        if (in != null) {
                            SharedPreferences prefs = activity.preferences;
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_UNFOLDING);
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_PARSING);
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_VALIDATION);
                            setHint(prefs, CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY);
                            setHint(prefs, CompatibilityHints.KEY_NOTES_COMPATIBILITY);
                            setHint(prefs, CompatibilityHints.KEY_VCARD_COMPATIBILITY);
                            calendar = calendarBuilder.build(in);
                        }
                        activity.setCalendar(calendar);
                    } catch (Exception exc) {
                        String msg = activity.getString(R.string.dialog_error_unparseable)
                                     + exc.getMessage();
                        DialogTools.info(activity, R.string.dialog_error_title, msg);
                        Log.d(TAG, "Error", exc);
                    }
                }
            };
        } else if (v.getId() == R.id.SetUrlButton) {

            task = new RunnableWithProgress(activity) {
                @Override
                public void run(ProgressDialog dialog) {
                    // FIXME: This should really be a dialog or something
                    SharedPreferences prefs = activity.preferences;
                    String url = DialogTools.ask(activity, R.string.dialog_enter_url_title,
                                                 R.string.dialog_enter_url_message,
                                                 prefs.getString(PREF_LAST_URL, ""), true, false);

                    if (!TextUtils.isEmpty(url)) {
                        try {
                            String user = DialogTools.ask(activity,
                                                          R.string.dialog_enter_username_title,
                                                          R.string.dialog_enter_username_message,
                                                          prefs.getString(PREF_LAST_USERNAME, ""),
                                                          true, false);
                            String pass = null;
                            if (!TextUtils.isEmpty(user)) {
                                pass = DialogTools.ask(activity,
                                                       R.string.dialog_enter_password_title,
                                                       R.string.dialog_enter_password_message,
                                                       prefs.getString(PREF_LAST_PASSWORD, ""),
                                                       true, true);
                            }
                            setMessage(R.string.progress_parsing_url);
                            boolean save = prefs.getBoolean("setting_save_passwords", false);

                            Editor editor = prefs.edit();
                            editor.putString(PREF_LAST_URL, url);
                            editor.putString(PREF_LAST_USERNAME, user);
                            editor.putString(PREF_LAST_PASSWORD, save ? pass : "");
                            editor.commit();

                            if (!TextUtils.isEmpty(user) && pass != null) {
                                Credentials creds = new Credentials(user, pass);
                                activity.setUrl(new CredentialInputAdapter(new URL(url), creds));
                            } else {
                                activity.setUrl(new BasicInputAdapter(new URL(url)));
                            }
                        } catch (MalformedURLException exc) {
                            Log.d(TAG, "Controller", exc);

                            String msg = "URL was not parsable..." + exc.getMessage();
                            DialogTools.info(activity, R.string.dialog_error_title, msg);
                        }
                    }
                }
            };

        } else if (v.getId() == R.id.SaveButton) {

            task = new SaveCalendar(activity);
            style = ProgressDialog.STYLE_HORIZONTAL;

        } else if (v.getId() == R.id.InsertButton || v.getId() == R.id.DeleteButton) {

            task = new ProcessVEvent(activity, calendar, v.getId() == R.id.InsertButton);
            style = ProgressDialog.STYLE_HORIZONTAL;
        }

        if (task != null) {
            DialogTools.progress(activity, task, false, style);
        }
    }
}
