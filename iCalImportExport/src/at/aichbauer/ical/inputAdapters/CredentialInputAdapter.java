package at.aichbauer.ical.inputAdapters;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

import at.aichbauer.tools.dialogs.Credentials;

public class CredentialInputAdapter extends BasicInputAdapter {
	private Credentials credentials;

	public CredentialInputAdapter(URL url, Credentials credentials) {
		super(url);
		this.credentials = credentials;
	}

	@Override
	public URLConnection getConnection() throws IOException {
		if (getURL().getProtocol().equals("ftp")) {
			URL u = new URL("ftp://" + credentials.getUsername() + ":" + credentials.getPassword() + "@"
					+ getURL().toExternalForm().substring(6));
			return u.openConnection();
		} else if (getURL().getProtocol().equals("http")) {
			Base64 enc = new Base64();
			byte[] encoded = enc.encode((credentials.getUsername() + ":" + credentials.getPassword()).getBytes());
			URLConnection connection = getURL().openConnection();
			connection.setRequestProperty("Authorization", "Basic " + new String(encoded));
			return connection;
		}
		return super.getConnection();
	}

}
