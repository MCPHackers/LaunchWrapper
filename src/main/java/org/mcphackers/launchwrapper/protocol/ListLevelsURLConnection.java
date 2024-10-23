package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ListLevelsURLConnection extends URLConnection {

	private final File levelsDir;

	public ListLevelsURLConnection(URL url, File levelsDir) {
		super(url);
		this.levelsDir = levelsDir;
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!levelsDir.exists()) {
			levelsDir.mkdirs();
		}
		String[] lvlNames = SaveRequests.getLevelNames(levelsDir);
		StringBuilder lvls = new StringBuilder();
		for (int i = 0; i < SaveRequests.MAX_LEVELS; i++) {
			lvls.append(lvlNames[i]).append(";");
		}
		return new ByteArrayInputStream(lvls.toString().getBytes());
	}
}
