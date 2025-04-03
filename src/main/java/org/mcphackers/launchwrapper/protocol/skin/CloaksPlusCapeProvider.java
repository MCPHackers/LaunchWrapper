package org.mcphackers.launchwrapper.protocol.skin;

public class CloaksPlusCapeProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (name == null) {
			name = MojangSkinProvider.getNameFromUUID(uuid);
		}
		return getSkin(name, type);
	}

	public Skin getSkin(String name, SkinTexture type) {
		if (type != SkinTexture.CAPE) {
			return null;
		}
		if (name == null) {
			return null;
		}
		return new HttpSkin("http://161.35.130.99/capes/" + name + ".png");
	}
}
