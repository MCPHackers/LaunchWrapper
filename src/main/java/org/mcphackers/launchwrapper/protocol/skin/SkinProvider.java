package org.mcphackers.launchwrapper.protocol.skin;

public interface SkinProvider {
	Skin getSkin(String uuid, String name, SkinTexture type);
}
