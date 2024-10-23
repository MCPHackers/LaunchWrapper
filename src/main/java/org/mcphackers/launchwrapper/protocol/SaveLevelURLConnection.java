package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SaveLevelURLConnection extends HttpURLConnection {

	ByteArrayOutputStream levelOutput = new ByteArrayOutputStream();
	private File levelsDir;

	public SaveLevelURLConnection(URL url, File levelsDir) {
		super(url);
		this.levelsDir = levelsDir;
	}

	@Override
	public void connect() {
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	@SuppressWarnings("unused")
	public InputStream getInputStream() throws IOException {
		Exception exception = null;
		try {
			byte[] data = levelOutput.toByteArray();
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			String username = in.readUTF();
			String sessionid = in.readUTF();
			String levelName = in.readUTF();
			byte levelId = in.readByte();
			int length = in.readInt();
			byte[] levelData = new byte[length];
			in.read(levelData);
			in.close();
			SaveRequests.saveLevel(levelsDir, levelId, levelName, levelData);
		} catch (Exception e) {
			exception = e;
		}
		if (exception != null) {
			try {
				Thread.sleep(100L); // Needs some sleep because it won't display error message if it's too fast
			} catch (InterruptedException e) {
			}
			String err = "error\n" + exception.getMessage();
			exception.printStackTrace();
			return new ByteArrayInputStream(err.getBytes());
		}
		return new ByteArrayInputStream("ok".getBytes());
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return levelOutput;
	}
}
