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

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import org.sufficientlysecure.ical.R;
import org.sufficientlysecure.ical.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemindersDialog extends DialogPreference {

    // Must match the list items in strings.xml
    private static final int[] MINUTES = {
        5, 10, 15, 30, 45, 60, 120, 240, 480, 720, 1440, 2880, 4320, 5760,
        7200, 8640, 10080, 20160, 30240, 40320
    };

    private LinearLayout mItemsHolder;
    private final SettingsActivity mActivity;
    private int mNewId;

    public RemindersDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (SettingsActivity) context;
        mNewId = 1;
    }

    public static List<Integer> getSavedRemindersInMinutes(Settings settings) {
        List<Integer> result = new ArrayList<>();
        for (String item: settings.getString(Settings.PREF_DEFAULT_REMINDERS).split(",")) {
            if (item.length() > 0)
                result.add(MINUTES[Integer.parseInt(item)]);
        }
        return result;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        mItemsHolder = (LinearLayout) view.findViewById(R.id.reminder_holder);
        String def = new Settings(mActivity.getPreferences()).getString(Settings.PREF_DEFAULT_REMINDERS);

        for (String item: def.split(",")) {
            if (item.length() > 0)
                addReminder(Integer.parseInt(item));
        }

        View.OnClickListener addClick = new View.OnClickListener() {
            public void onClick(View view) {
                addReminder(0);
            }
        };
        Button button = (Button) view.findViewById(R.id.button_reminder_add);
        button.setOnClickListener(addClick);
    }

    private void addReminder(int index) {

        View newItem = mActivity.getLayoutInflater().inflate(R.layout.reminder, mItemsHolder, false);
        final int id = mNewId++;
        newItem.setId(id);
        Spinner dropDown = (Spinner) newItem.findViewById(R.id.reminder_item);
        dropDown.setSelection(index);

        View.OnClickListener delClick = new View.OnClickListener() {
            public void onClick(View view) {
                mItemsHolder.removeView(mItemsHolder.findViewById(id));
            }
        };
        Button button = (Button) newItem.findViewById(R.id.button_reminder_delete);
        button.setOnClickListener(delClick);

        mItemsHolder.addView(newItem);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult)
            return;

        // Save the (reordered and unique) chosen reminders to settings
        Set<Integer> values = new HashSet<>();
        final int count = mItemsHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = mItemsHolder.getChildAt(i);
            Spinner dropDown = (Spinner) item.findViewById(R.id.reminder_item);
            values.add(dropDown.getSelectedItemPosition());
        }

        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        StringBuilder b = new StringBuilder();
        for (Integer value: sorted) {
            if (b.length() != 0)
                b.append(",");
            b.append(value);
        }

        new Settings(mActivity.getPreferences()).putString(Settings.PREF_DEFAULT_REMINDERS, b.toString());
    }
}
