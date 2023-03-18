package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.Util;

public class LoadLevelURLConnection extends HttpURLConnection {

	Exception exception;

	public LoadLevelURLConnection(URL url) {
		super(url);
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
		try {
			Map<String, String> query = Util.queryMap(url);
			if(!query.containsKey("id")) {
				throw new MalformedURLException("Query is missing \"id\" parameter");
			}
			int levelId = Integer.parseInt(query.get("id"));
			File levels = new File(Launch.getConfig().gameDir.get(), "levels");
			File level = new File(levels, "level" + levelId + ".dat");
			if(!level.exists()) {
				throw new FileNotFoundException("Level doesn't exist");
			}
			DataInputStream in = new DataInputStream(new FileInputStream(level));
			byte[] levelData = Util.readStream(in);
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
			ByteArrayOutputStream errorData = new ByteArrayOutputStream();
			DataOutputStream errorOutput = new DataOutputStream(errorData);
			errorOutput.writeUTF("error");
			errorOutput.writeUTF(exception.getMessage());
			errorOutput.close();
			return new ByteArrayInputStream(errorData.toByteArray());
		}
		return new ByteArrayInputStream(data.toByteArray());
	}
}
