package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BasicResponseURLConnection extends HttpURLConnection {
	final String responseMessage;
	final int responseCode;

	public BasicResponseURLConnection(URL url, String response) {
		this(url, 200, response);
	}

	public BasicResponseURLConnection(URL url, int responseCode, String response) {
		super(url);
		this.responseMessage = response;
		this.responseCode = responseCode;
	}

	@Override
	public void connect() throws IOException {}

	@Override
	public void disconnect() {}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(responseMessage.getBytes());
	}
	
	public int getResponseCode() {
		return responseCode;
	}
}