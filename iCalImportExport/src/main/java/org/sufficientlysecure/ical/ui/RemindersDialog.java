/**
 *  Copyright (C) 2015  Jon Griffiths (jon_p_griffiths@yahoo.com)
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

import org.sufficientlysecure.ical.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class RemindersDialog extends DialogPreference {

    private static final String PREF_KEY = "default_reminders";

    // Must match the list items in strings.xml
    private static final int[] MINUTES = {
        5, 10, 15, 30, 45, 60, 120, 240, 480, 720, 1440, 2880, 4320, 5760,
        7200, 8640, 10080, 20160, 30240, 40320
    };

    private LinearLayout mReminderItemsHolder;
    private LayoutInflater mInflater;
    private int mNewId;

    public RemindersDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = ((Activity) context).getLayoutInflater();
        mNewId = 1;
    }

    public static List<Integer> getSavedRemindersInMinutes() {
        List<Integer> result = new ArrayList<Integer>();
        for (String item: MainActivity.preferences.getString(PREF_KEY, "").split(",")) {
            if (item.length() > 0) {
                result.add(MINUTES[Integer.parseInt(item)]);
            }
        }
        return result;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        mReminderItemsHolder = (LinearLayout) view.findViewById(R.id.reminder_holder);

        for (String item: MainActivity.preferences.getString(PREF_KEY, "").split(",")) {
            if (item.length() > 0) {
                addReminder(Integer.parseInt(item));
            }
        }

        View.OnClickListener addClick = new View.OnClickListener() {
            public void onClick(View v) {
                addReminder(0);
            }
        };
        Button addButton = (Button) view.findViewById(R.id.button_reminder_add);
        addButton.setOnClickListener(addClick);
    }

    private void addReminder(int index) {

        View newItem = mInflater.inflate(R.layout.reminder, mReminderItemsHolder, false);
        final int id = mNewId++;
        newItem.setId(id);
        Spinner dropDown = (Spinner) newItem.findViewById(R.id.reminder_item);
        dropDown.setSelection(index);

        View.OnClickListener delClick = new View.OnClickListener() {
            public void onClick(View v) {
                mReminderItemsHolder.removeView(mReminderItemsHolder.findViewById(id));
            }
        };
        Button delButton = (Button) newItem.findViewById(R.id.button_reminder_delete);
        delButton.setOnClickListener(delClick);

        mReminderItemsHolder.addView(newItem);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) {
            return;
        }

        // Save the (reordered and unique) chosen reminders to settings
        Set<Integer> values = new HashSet<Integer>();
        final int count = mReminderItemsHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = mReminderItemsHolder.getChildAt(i);
            Spinner dropDown = (Spinner) item.findViewById(R.id.reminder_item);
            values.add(dropDown.getSelectedItemPosition());
        }

        List<Integer> sorted = new ArrayList<Integer>(values);
        Collections.sort(sorted);

        StringBuilder b = new StringBuilder();
        for (Integer v: sorted) {
            if (b.length() != 0) {
                b.append(",");
            }
            b.append(v);
        }

        MainActivity.preferences.edit().putString(PREF_KEY, b.toString()).commit();
    }
}
