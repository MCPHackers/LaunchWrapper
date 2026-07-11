package org.mcphackers.launchwrapper.protocol.skin;

public class BetacraftSkinProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (name == null) {
			name = MojangSkinProvider.getNameFromUUID(uuid);
		}
		return getSkin(name, type);
	}

	public Skin getSkin(String name, SkinTexture type) {
		if (name == null) {
			return null;
		}
		if (type == SkinTexture.SKIN) {
			return new HttpSkin("https://betacraft.uk/api/skin/" + name);
		}
		if (type == SkinTexture.CAPE) {
			return new HttpSkin("https://betacraft.uk/api/cape/" + name);
		}
		return null;
	}
}
