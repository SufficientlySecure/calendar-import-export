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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public abstract class SpinnerTools {
    private SpinnerTools() {

    }

    public static <E> void simpleSpinner(Context context, Spinner spinner, List<E> input) {
        ArrayAdapter<E> adapter = new ArrayAdapter<E>(context,
                android.R.layout.simple_spinner_item, input);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (input != null && input.size() != 0) {
            spinner.setVisibility(View.VISIBLE);
        }
    }

    public static <E> void simpleSpinnerInUI(final Activity activity, final Spinner spinner,
            final List<E> input) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                simpleSpinner(activity, spinner, input);
            }
        });
    }
}
