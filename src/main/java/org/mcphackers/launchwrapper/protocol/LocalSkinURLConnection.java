package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocalSkinURLConnection extends HttpURLConnection {

	private File file;

	public LocalSkinURLConnection(URL url) {
		super(url);
		this.file = new File(url.getPath());
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}

	@Override
	public int getResponseCode() {
		return file.exists() ? 200 : 404;
	}
}
