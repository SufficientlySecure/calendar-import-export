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

import android.content.SharedPreferences;

import org.sufficientlysecure.ical.Settings;
import org.sufficientlysecure.ical.util.Log;

public class SettingsActivity extends SettingsActivityBase {

    public SettingsActivity() {
        super();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        super.onSharedPreferenceChanged(prefs, key);

        switch (key) {
            case Settings.PREF_SAVE_PASSWORDS:
                // Blank any stored password when this setting is changed
                new Settings(prefs).putString(Settings.PREF_LASTURLPASSWORD, "");
                break;

            case Settings.PREF_DEBUG_LOGGING:
            case Settings.PREF_NET_FORTUNA_ICAL4J_TIMEZONE_UPDATE_ENABLED:
                processSettings(new Settings(prefs));
                break;
        }
    }

    public static void processSettings(Settings settings) {

        // Enable or disable debug logging in release builds
        Log.setIsUserEnabled(settings.getDebugLogging());

        // Turn TimeZone updates on or off
        String v = settings.getNetFortunaIcal4jTimezoneUpdateEnabled() ? "true" : "false";
        System.setProperty(Settings.PREF_NET_FORTUNA_ICAL4J_TIMEZONE_UPDATE_ENABLED, v);
    }
}
