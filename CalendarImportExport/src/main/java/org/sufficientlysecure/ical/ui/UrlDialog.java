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
import org.sufficientlysecure.ical.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

        Settings settings = mActivity.getSettings();
        mTextCalendarUrl.setText(settings.getString(Settings.PREF_LASTURL));
        mTextUsername.setText(settings.getString(Settings.PREF_LASTURLUSERNAME));
        mTextPassword.setText(settings.getString(Settings.PREF_LASTURLPASSWORD));

        mCheckboxLoginRequired.setChecked(mTextUsername.getText().length() != 0);
        mTextCalendarUrl.selectAll();

        DialogInterface.OnClickListener okTask;
        okTask = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface iface, int id) {
                // We override this in onStart()
            }
        };

        DialogInterface.OnClickListener cancelTask;
        cancelTask = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface iface, int id) {
                iface.cancel();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        AlertDialog dlg = builder.setIcon(R.mipmap.ic_launcher)
                                 .setTitle(R.string.enter_source_url)
                                 .setView(view)
                                 .setPositiveButton(android.R.string.ok, okTask)
                                 .setNegativeButton(android.R.string.cancel, cancelTask)
                                 .create();
        dlg.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dlg;
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dlg = (AlertDialog) getDialog();
        if (dlg == null)
            return;

        View.OnClickListener onClickTask;
        onClickTask = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = mTextCalendarUrl.getText().toString();
                boolean loginRequired = mCheckboxLoginRequired.isChecked();
                String username = loginRequired ? mTextUsername.getText().toString() : "";
                String password = loginRequired ? mTextPassword.getText().toString() : "";

                if (!mActivity.setSource(url, null, username, password)) {
                    TextView label = (TextView) dlg.findViewById(R.id.TextViewUrlError);
                    label.setText(R.string.invalid_url);
                    return;
                }

                Settings settings = mActivity.getSettings();
                settings.putString(Settings.PREF_LASTURL, url);
                if (loginRequired) {
                    settings.putString(Settings.PREF_LASTURLUSERNAME, username);
                    if (settings.getSavePasswords())
                        settings.putString(Settings.PREF_LASTURLPASSWORD, password);
                }
                dlg.dismiss();
            }
        };

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(onClickTask);
    }

    public static void show(final Activity activity) {
        FragmentManager fm = ((MainActivity) activity).getSupportFragmentManager();
        new UrlDialog().show(fm, "UrlDialogTag");
    }
}
