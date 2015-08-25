/**
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
import android.app.ProgressDialog;

public abstract class RunnableWithProgress {
    private Activity mActivity;
    private ProgressDialog mDialog;

    public RunnableWithProgress(Activity activity) {
        mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    protected void setProgressDialog(ProgressDialog dialog) {
        mDialog = dialog;
    }

    public void setMessage(final int resource) {
        mActivity.runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       mDialog.setMessage(mActivity.getString(resource));
                                   }
                               });
    }

    public void incrementProgressBy(final int progress) {
        mActivity.runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       mDialog.incrementProgressBy(progress);
                                   }
                               });
    }

    public abstract void run(ProgressDialog dialog);
}
