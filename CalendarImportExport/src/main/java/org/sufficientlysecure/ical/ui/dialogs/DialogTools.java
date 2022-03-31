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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import org.sufficientlysecure.ical.R;

public final class DialogTools {
    private DialogTools() {
    }

    public static void info(final Activity activity, final int title, final CharSequence message) {
        Runnable task = new Runnable() {
            public void run() {
                DialogInterface.OnClickListener okTask;
                okTask = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface iface, int id) {
                        iface.cancel();
                    }
                };
                AlertDialog dlg = new AlertDialog.Builder(activity)
                                                 .setMessage(message)
                                                 .setIcon(R.mipmap.ic_launcher)
                                                 .setPositiveButton(android.R.string.ok, okTask)
                                                 .setTitle(activity.getString(title)).create();
                dlg.show();
                TextView message = (TextView) dlg.findViewById(android.R.id.message);
                message.setMovementMethod(LinkMovementMethod.getInstance());
            }
        };
        activity.runOnUiThread(task);
    }
}
