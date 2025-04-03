package org.mcphackers.launchwrapper.protocol.skin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LocalSkin implements Skin {
	private File skinFile;
	private boolean slim;

	private static final boolean useFileURL = Boolean.parseBoolean(System.getProperty("launchwrapper.skins.useFileURL", "false"));

	public LocalSkin(File file, boolean slim) {
		this.skinFile = file;
		this.slim = slim;
	}

	public String getSHA256() {
		return null;
	}

	public InputStream getData() throws IOException {
		return new FileInputStream(skinFile);
	}

	public String getURL() {
		return useFileURL ? skinFile.toURI().toString() : "http://skins.local/" + skinFile.getAbsolutePath();
	}

	public boolean isSlim() {
		return slim;
	}
}
