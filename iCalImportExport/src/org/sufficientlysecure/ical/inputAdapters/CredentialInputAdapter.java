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

import org.apache.commons.codec.binary.Base64;
import org.sufficientlysecure.ical.tools.dialogs.Credentials;


public class CredentialInputAdapter extends BasicInputAdapter {
    private Credentials credentials;

    public CredentialInputAdapter(URL url, Credentials credentials) {
        super(url);
        this.credentials = credentials;
    }

    @Override
    public URLConnection getConnection() throws IOException {
        if (getURL().getProtocol().equals("ftp")) {
            URL u = new URL("ftp://" + credentials.getUsername() + ":" + credentials.getPassword()
                    + "@" + getURL().toExternalForm().substring(6));
            return u.openConnection();
        } else if (getURL().getProtocol().equals("http")) {
            Base64 enc = new Base64();
            byte[] encoded = enc.encode((credentials.getUsername() + ":" + credentials
                    .getPassword()).getBytes());
            URLConnection connection = getURL().openConnection();
            connection.setRequestProperty("Authorization", "Basic " + new String(encoded));
            return connection;
        }
        return super.getConnection();
    }

}
