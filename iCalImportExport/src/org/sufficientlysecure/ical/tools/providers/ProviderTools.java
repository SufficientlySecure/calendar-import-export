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

package org.sufficientlysecure.ical.tools.providers;

import java.io.FileOutputStream;
import java.io.PrintStream;

import android.content.ContentValues;

public abstract class ProviderTools {
    private ProviderTools() {

    }

    public static void writeException(String path, Exception exc) throws Exception {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        exc.printStackTrace(out);
    }

    public static String buildWhereAnd(String... columns) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                builder.append(" AND ");
            }
            builder.append(columns[i] + " = ?");
        }
        return builder.toString();
    }

    public static String[] contentValuesToArray(ContentValues values, String... keys) {
        String[] columns = new String[keys.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = values.getAsString(keys[i]);
        }
        return columns;
    }
}
