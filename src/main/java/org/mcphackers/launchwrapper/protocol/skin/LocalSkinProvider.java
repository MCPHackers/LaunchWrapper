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
		return getLocalSkin(uuid, type);
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
			return getLocalSkin(MojangSkinProvider.getNameFromUUID(uuid), type);
		}
		return null;
	}

	private Skin getLocalSkin(String path, SkinTexture type) {
		File f;
		if (type == SkinTexture.SKIN) {
			f = new File(skinsDirectory, "skins/" + path + ".png");
			if (f.isFile() && f.canRead()) {
				return new LocalSkin(f, false);
			}
			f = new File(skinsDirectory, "skins/" + path + ".slim.png");
			if (f.isFile() && f.canRead()) {
				return new LocalSkin(f, true);
			}
		} else if (type == SkinTexture.CAPE) {
			f = new File(skinsDirectory, "capes/" + path + ".png");
			if (f.isFile() && f.canRead()) {
				return new LocalSkin(f, false);
			}
		} else if (type == SkinTexture.ELYTRA) {
			f = new File(skinsDirectory, "elytra/" + path + ".png");
			if (f.isFile() && f.canRead()) {
				return new LocalSkin(f, false);
			}
		}
		return null;
	}
}
