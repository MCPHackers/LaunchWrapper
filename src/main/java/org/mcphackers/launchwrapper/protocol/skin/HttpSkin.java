package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpSkin implements Skin {

	private String url;

	public HttpSkin(String url) {
		this.url = url;
	}

	public InputStream getData() throws IOException {
		HttpURLConnection httpConnection = (HttpURLConnection)openDirectConnection(new URL(getURL()));
		if (httpConnection.getResponseCode() != 200) {
			return null;
		}
		return httpConnection.getInputStream();
	}

	public String getURL() {
		return url;
	}

	public String getSHA256() {
		return null;
	}

	public boolean isSlim() {
		return false;
	}
}
