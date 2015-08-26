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

package org.sufficientlysecure.ical.ui.dialogs;

import org.sufficientlysecure.ical.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class DialogTools {
    private DialogTools() {
    }

    public static void info(final Activity activity, final int title, final int message) {
        info(activity, title, activity.getString(message));
    }

    public static void info(final Activity activity, final int title, final CharSequence message) {
        Runnable task;
        task = new Runnable() {
            @Override
            public void run() {
                DialogInterface.OnClickListener okTask;
                okTask = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface iface, int which) {
                        iface.cancel();
                    }
                };
                AlertDialog d = new AlertDialog.Builder(activity)
                                .setMessage(message)
                                .setIcon(R.drawable.icon)
                                .setPositiveButton(android.R.string.ok, okTask)
                                .setTitle(activity.getString(title)).create();
                d.show();
                TextView message = (TextView) d.findViewById(android.R.id.message);
                message.setMovementMethod(LinkMovementMethod.getInstance());
            }
        };
        activity.runOnUiThread(task);
    }

    public static String ask(final Activity activity, final int titleResource,
                             final int messageResource, final String input,
                             final boolean cancelable, final boolean password) {
        final String[] result = new String[2];
        Runnable task;
        task = new Runnable() {
            @Override
            public void run() {
                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                        LayoutParams.MATCH_PARENT));
                layout.setMinimumWidth(300);

                TextView view = new TextView(activity);
                view.setPadding(10, 10, 10, 10);
                view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                      LayoutParams.WRAP_CONTENT));
                view.setTextSize(16);
                layout.addView(view);
                view.setText(activity.getString(messageResource));

                final EditText editText = new EditText(activity);
                if (password) {
                    editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    editText.setSingleLine();
                }
                editText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                          LayoutParams.WRAP_CONTENT));
                if (input != null) {
                    editText.setText(input);
                    editText.selectAll();
                }
                layout.addView(editText);

                DialogInterface.OnClickListener yesTask;
                yesTask = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface iface, int w) {
                        result[0] = editText.getText().toString();
                        result[1] = "";
                        iface.cancel();
                    }
                };

                DialogInterface.OnClickListener noTask;
                noTask = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface iface, int w) {
                        result[0] = "";
                        result[1] = "";
                        iface.cancel();
                    }
                };

                DialogInterface.OnCancelListener cancelTask;
                cancelTask = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface iface) {
                        result[1] = "";
                    }
                };


                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                                              .setTitle(activity.getString(titleResource))
                                              .setPositiveButton(android.R.string.ok, yesTask);
                if (cancelable) {
                    builder.setNegativeButton(android.R.string.cancel, noTask);
                }

                builder.setCancelable(cancelable).setOnCancelListener(cancelTask);
                AlertDialog dialog = builder.setView(layout).create();

                int state = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
                dialog.getWindow().setSoftInputMode(state);
                dialog.show();
            }
        };

        activity.runOnUiThread(task);

        while (result[1] == null) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
            }
        }
        return result[0];
    }
}
