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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CalendarUtils {
    public static List<File> searchFiles(File root, String... extension) {
        List<File> files = new ArrayList<File>();
        searchFiles(root, files, extension);
        return files;
    }

    private static void searchFiles(File root, List<File> files, String... extension) {
        if (root.isFile()) {
            for (String string : extension) {
                if (root.toString().endsWith(string)) {
                    files.add(root);
                }
            }
        } else {
            File[] children = root.listFiles();
            if (children != null) {
                for (File file : children) {
                    searchFiles(file, files, extension);
                }
            }
        }
    }
}
