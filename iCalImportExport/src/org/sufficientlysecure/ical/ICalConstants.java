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

package org.sufficientlysecure.ical;

public interface ICalConstants {
    public static final String HELP = "<b>iCal Import/Export</b><br>"
            + "It is a simple tool to import iCal files into your Android calendar."
            + "<br>To successfully import iCal events please follow the given steps below:<br><br>"
            + "  +<i>Select a calendar</i><br><small>The selected calendar will be editet</small><br>"
            + "  +<i>Search iCal files</i> or <i>Set URL</i><br><small>Searches the SD card for iCal files or enter URL of an iCal file directly (http and ftp support)</small><br>"
            + "  +<i>Select an iCal file</i><br>"
            + "  +<i>Load iCal file</i><br><small>The iCal file will be parsed, if successfull a number of events should appear next to the button</small><br>"
            + "  +<i>Insert events</i> or <i>Delete events</i><br><small>Starts the import process. When finished a status information should be displayed.</small><br>"
            + "<br>"
            + "If you are considering errors, please add a new issue to https://github.com/dschuermann/ical-import-export and provide an iCal file you would like to import.<br>"
            + "<br>Thanks to iCal4j Project for the parser/interpreter<br><br>"
            + "<i>To view this information again: menu --> help</i>";
    public static final String PREFERENCE_LAST_URL = "lastUrl";
    public static final String PREFERENCE_LAST_USERNAME = "lastUsername";
    public static final String PREFERENCE_LAST_PASSWORD = "lastPassword";
}
