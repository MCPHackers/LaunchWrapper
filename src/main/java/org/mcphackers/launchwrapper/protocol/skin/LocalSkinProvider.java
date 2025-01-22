package org.mcphackers.launchwrapper.protocol.skin;

import java.io.File;

public class LocalSkinProvider implements SkinProvider {
	private File skinsDirectory;

	public LocalSkinProvider(File dir) {
		this.skinsDirectory = dir;
	}

	public Skin getSkinByName(String name, SkinTexture type) {
		Skin skin = getLocalSkin(name, type);
		if (skin != null) {
			return skin;
		}
		String uuid = MojangSkinProvider.getUUIDfromName(name);
		if (uuid != null) {
			return getLocalSkin(uuid, type);
		}
		return null;
	}

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (uuid != null) {
			Skin skin = getLocalSkin(uuid, type);
			if (skin != null) {
				return skin;
			}
		}
		if (name != null) {
			Skin skin = getLocalSkin(name, type);
			if (skin != null) {
				return skin;
			}
		}
		if (uuid != null) {
			name = MojangSkinProvider.getNameFromUUID(uuid);
			if (name != null) {
				return getLocalSkin(name, type);
			}
		}
		return null;
	}

	private Skin getLocalSkin(String path, SkinTexture type) {
		String dirName = null;
		switch (type) {
			case SKIN:
				dirName = "skin";
				break;
			case CAPE:
				dirName = "cape";
				break;
			case ELYTRA:
				dirName = "elytra";
				break;
			default:
				break;
		}
		File f = new File(skinsDirectory, dirName + "/" + path + ".png");
		if (f.isFile() && f.canRead()) {
			return new LocalSkin(f, false);
		}
		if (type == SkinTexture.SKIN) {
			f = new File(skinsDirectory, "skin/" + path + ".slim.png");
			if (f.isFile() && f.canRead()) {
				return new LocalSkin(f, true);
			}
		}
		return null;
	}
}
