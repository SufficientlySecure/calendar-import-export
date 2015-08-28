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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class UrlDialog extends DialogFragment {
    private static final String PREF_LAST_URL = "lastUrl";
    private static final String PREF_LAST_USERNAME = "lastUrlUsername";
    private static final String PREF_LAST_PASSWORD = SettingsActivity.PREF_LAST_URL_PASSWORD;

    private MainActivity mActivity;
    private EditText mTextCalendarUrl;
    private CheckBox mCheckboxLoginRequired;
    private EditText mTextUsername;
    private EditText mTextPassword;

    public UrlDialog() {
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mActivity = (MainActivity) getActivity();

        ViewGroup nullViewGroup = null; // Avoid bad lint warning in inflate()
        View view = mActivity.getLayoutInflater().inflate(R.layout.urldialog, nullViewGroup);

        mCheckboxLoginRequired = (CheckBox) view.findViewById(R.id.CheckboxLoginRequired);
        CompoundButton.OnCheckedChangeListener loginRequiredTask;
        loginRequiredTask = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                mTextUsername.setEnabled(isChecked);
                mTextPassword.setEnabled(isChecked);
            }
        };
        mCheckboxLoginRequired.setOnCheckedChangeListener(loginRequiredTask);

        mTextCalendarUrl = (EditText) view.findViewById(R.id.TextCalendarUrl);
        mTextUsername = (EditText) view.findViewById(R.id.TextUsername);
        mTextPassword = (EditText) view.findViewById(R.id.TextPassword);

        mTextCalendarUrl.setText(MainActivity.preferences.getString(PREF_LAST_URL, ""));
        mTextUsername.setText(MainActivity.preferences.getString(PREF_LAST_USERNAME, ""));
        mTextPassword.setText(MainActivity.preferences.getString(PREF_LAST_PASSWORD, ""));

        mCheckboxLoginRequired.setChecked(mTextUsername.getText().length() != 0);
        mTextCalendarUrl.selectAll();

        DialogInterface.OnClickListener okTask;
        okTask = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // We override this in onStart()
            }
        };

        DialogInterface.OnClickListener cancelTask;
        cancelTask = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setIcon(R.drawable.icon)
               .setTitle(R.string.enter_source_url)
               .setView(view)
               .setPositiveButton(android.R.string.ok, okTask)
               .setNegativeButton(android.R.string.cancel, cancelTask);

        AlertDialog dialog = builder.create();
        if (mTextCalendarUrl.getText().length() != 0) {
            dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }

        View.OnClickListener onClickTask;
        onClickTask = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mTextCalendarUrl.getText().toString();
                boolean loginRequired = mCheckboxLoginRequired.isChecked();
                String username = loginRequired ? mTextUsername.getText().toString() : "";
                String password = loginRequired ? mTextPassword.getText().toString() : "";

                if (!mActivity.setUrl(url, username, password)) {
                    TextView label = (TextView) dialog.findViewById(R.id.TextViewUrlError);
                    label.setText(R.string.invalid_url);
                    return;
                }

                SharedPreferences.Editor e = MainActivity.preferences.edit();
                e.putString(PREF_LAST_URL, url);
                if (loginRequired) {
                    e.putString(PREF_LAST_USERNAME, username);
                    if (MainActivity.preferences.getBoolean("setting_save_passwords", false)) {
                        e.putString(PREF_LAST_PASSWORD, password);
                    }
                }
                e.commit();
                dialog.dismiss();
            }
        };

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(onClickTask);
    }

    public static void show(final Activity activity) {
        FragmentManager fm = ((MainActivity) activity).getSupportFragmentManager();
        new UrlDialog().show(fm, "UrlDialogTag");
    }
}
