package at.aichbauer.ical.inputAdapters;

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
