package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.util.Util;

public class LoadLevelURLConnection extends HttpURLConnection {

	private File levelsDir;

	public LoadLevelURLConnection(URL url, File levelsDir) {
		super(url);
		this.levelsDir = levelsDir;
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(data);
		Exception exception = null;
		try {
			Map<String, String> query = Util.queryMap(url);
			if(!query.containsKey("id")) {
				throw new MalformedURLException("Query is missing \"id\" parameter");
			}
			int levelId = Integer.parseInt(query.get("id"));
			byte[] levelData = SaveRequests.loadLevel(levelsDir, levelId);
			output.writeUTF("ok");
			output.write(levelData);
			output.close();
		} catch (Exception e) {
			exception = e;
		}
		if(exception != null) {
			try {
				Thread.sleep(100L); // Needs some sleep because it won't display error message if it's too fast
			} catch (InterruptedException e) {
			}
			output.writeUTF("error");
			output.writeUTF(exception.getMessage());
			output.close();
		}
		return new ByteArrayInputStream(data.toByteArray());
	}
}
