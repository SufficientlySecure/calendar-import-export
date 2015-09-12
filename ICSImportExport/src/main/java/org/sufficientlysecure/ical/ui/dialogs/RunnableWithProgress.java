/**
 *  Copyright (C) 2015  Jon Griffiths (jon_p_griffiths@yahoo.com)
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
import org.sufficientlysecure.ical.ui.MainActivity;
import org.sufficientlysecure.ical.util.Log;

import android.app.ProgressDialog;

public abstract class RunnableWithProgress extends ProgressDialog {
    private final MainActivity mActivity;

    protected RunnableWithProgress(MainActivity activity, boolean isHorizontal) {
        super(activity);
        mActivity = activity;
        setProgressStyle(isHorizontal ? STYLE_HORIZONTAL : STYLE_SPINNER);
        setCancelable(false);
        setMessage("");
        setTitle("");
        show();
    }

    public void start() {
        new Thread(new Runnable() {
                       public void run() {
                           try {
                               RunnableWithProgress.this.run();
                           } catch (Exception e) {
                               Log.e("ICS_RunnableWithProgress", "An exception occurred", e);
                               DialogTools.info(getActivity(), R.string.error, "Error:\n" + e.getMessage());
                           }
                           cancel();
                       }
                   }).start();
    }

    protected MainActivity getActivity() {
        return mActivity;
    }

    protected void setMessage(final int id) {
        mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        RunnableWithProgress.super.setMessage(mActivity.getString(id));
                                    }
                                });
    }

    protected void incrementProgress() {
        mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        RunnableWithProgress.super.incrementProgressBy(1);
                                    }
                                });
    }

    protected abstract void run() throws Exception;
}
