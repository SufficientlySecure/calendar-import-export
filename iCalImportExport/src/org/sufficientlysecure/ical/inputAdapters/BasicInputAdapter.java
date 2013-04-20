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

package org.sufficientlysecure.ical.inputAdapters;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class BasicInputAdapter {
    private URL url;

    public BasicInputAdapter(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return this.url;
    }

    public URLConnection getConnection() throws IOException {
        return this.url.openConnection();
    }

    @Override
    public String toString() {
        return this.url.toString();
    }
}
