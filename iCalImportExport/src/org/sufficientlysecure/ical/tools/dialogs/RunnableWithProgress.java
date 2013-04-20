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

package org.sufficientlysecure.ical.tools.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

public abstract class RunnableWithProgress {
    private Activity activity;
    private ProgressDialog dialog;

    public RunnableWithProgress(Activity activity) {
        this.activity = activity;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setProgressMessage(final String message) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d(DialogTools.class.getSimpleName(), Thread.currentThread().toString());
                dialog.setMessage(message);
            }
        });
    }

    protected void setProgressDialog(ProgressDialog dialog) {
        this.dialog = dialog;
    }

    public void setProgressTitle(final int titleResource) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dialog.setTitle(activity.getString(titleResource));
            }
        });
    }

    public void setProgressTitle(final String title) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dialog.setTitle(title);
            }
        });
    }

    public void setProgressMessage(final int resource) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dialog.setMessage(activity.getString(resource));
            }
        });
    }

    public void setProgress(final int progress) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dialog.setProgress(progress);
            }
        });
    }

    public void incrementProgress(final int progress) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dialog.incrementProgressBy(progress);
            }
        });
    }

    public abstract void run(ProgressDialog dialog);

    public int getProgress() {
        return -1;
    }
}
