package org.mcphackers.launchwrapper.protocol.skin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.mcphackers.launchwrapper.util.Util;

public class LocalSkin implements Skin {
	private File skinFile;
	private boolean slim;

	public LocalSkin(File file, boolean slim) {
		this.skinFile = file;
		this.slim = slim;
	}

	public String getSHA256() {
		try {
			return Util.getSHA256(new FileInputStream(skinFile));
		} catch (IOException e) {
			return null;
		}
	}

	public byte[] getData() throws IOException {
		return Util.readStream(new FileInputStream(skinFile));
	}

	public boolean isSlim() {
		return slim;
	}
}
