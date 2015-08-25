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

package org.sufficientlysecure.ical.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

public class CredentialInputAdapter {
    private URL mUrl;
    private String mUsername;
    private String mPassword;

    public CredentialInputAdapter(URL url, String username, String password) {
        mUrl = url;
        mUsername = username;
        mPassword = password;
    }

    public URLConnection getConnection() throws IOException {
        if (mUsername == null) {
            return mUrl.openConnection();
        }

        String protocol = mUrl.getProtocol();
        String userPass = mUsername + ":" + mPassword;

        if (protocol.equalsIgnoreCase("ftp") || protocol.equalsIgnoreCase("ftps")) {
            URL url = new URL(protocol + "://" + userPass + "@" + mUrl.toExternalForm().substring(6));
            return url.openConnection();
        }

        if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
            String encoded = new String(new Base64().encode(userPass.getBytes()));
            URLConnection connection = mUrl.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            return connection;
        }

        return mUrl.openConnection();
    }

    @Override
    public String toString() {
        return mUrl.toString();
    }
}
