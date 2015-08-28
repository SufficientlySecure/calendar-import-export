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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.ui.UrlDialog;
import org.sufficientlysecure.ical.ui.dialogs.DialogTools;
import org.sufficientlysecure.ical.ui.dialogs.RunnableWithProgress;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

@SuppressLint("NewApi")
public class Controller implements OnClickListener {
    private static final String TAG = Controller.class.getName();

    private MainActivity mActivity;
    private CalendarBuilder mCalendarBuilder;
    private Calendar mCalendar;

    public Controller(MainActivity activity) {
        mActivity = activity;
    }

    public void init(long calendarId) {
        List<AndroidCalendar> cals = AndroidCalendar.loadAll(mActivity.getContentResolver());
        if (cals.isEmpty()) {
            noCalendarFinish();
        }
        mActivity.setCalendars(cals);
        mActivity.selectCalendar(calendarId);
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
                        mActivity.finish();
                    }
                };
                AlertDialog d = new AlertDialog.Builder(mActivity)
                                .setMessage(R.string.no_calendars_found)
                                .setIcon(R.drawable.icon)
                                .setTitle(R.string.information)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, buttonTask).create();
                d.show();
            }
        };
        mActivity.runOnUiThread(task);
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

        // Handling search for file event
        if (v.getId() == R.id.SearchButton) {

            task = new RunnableWithProgress(mActivity) {
                @Override
                public void run() {
                    setMessage(R.string.searching_for_files);

                    File root = Environment.getExternalStorageDirectory();
                    List<File> files = new ArrayList<File>();
                    searchFiles(root, files, "ics", "ical", "icalendar");
                    mActivity.setFiles(files);
                }
            };
        } else if (v.getId() == R.id.LoadButton) {

            task = new RunnableWithProgress(mActivity) {
                @Override
                public void run() {
                    if (mCalendarBuilder == null) {
                        setMessage(R.string.performing_first_time_setup);
                        mCalendarBuilder = new CalendarBuilder();
                    }
                    try {
                        setMessage(R.string.reading_file_please_wait);
                        URLConnection c = mActivity.getSelectedURL();
                        InputStream in = c == null ? null : c.getInputStream();
                        if (in != null) {
                            SharedPreferences prefs = MainActivity.preferences;
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_UNFOLDING);
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_PARSING);
                            setHint(prefs, CompatibilityHints.KEY_RELAXED_VALIDATION);
                            setHint(prefs, CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY);
                            setHint(prefs, CompatibilityHints.KEY_NOTES_COMPATIBILITY);
                            setHint(prefs, CompatibilityHints.KEY_VCARD_COMPATIBILITY);
                            mCalendar = mCalendarBuilder.build(in);
                        }
                        mActivity.setCalendar(mCalendar);
                    } catch (Exception exc) {
                        String msg = mActivity.getString(R.string.could_not_parse_file)
                                     + exc.getMessage();
                        DialogTools.info(mActivity, R.string.error, msg);
                        Log.d(TAG, "Error", exc);
                    }
                }
            };
        } else if (v.getId() == R.id.SetUrlButton) {

            UrlDialog.show(mActivity);

        } else if (v.getId() == R.id.SaveButton) {

            task = new SaveCalendar(mActivity);

        } else if (v.getId() == R.id.InsertButton || v.getId() == R.id.DeleteButton) {

            task = new ProcessVEvent(mActivity, mCalendar, v.getId() == R.id.InsertButton);
        }

        if (task != null) {
            task.start();
        }
    }
}
