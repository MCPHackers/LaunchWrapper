package org.mcphackers.launchwrapper.protocol.skin;

public class CraftarSkinProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		String url = getTextureURL(uuid, type);
		return url == null ? null : new HttpSkin(url);
	}

	private static String getTextureURL(String uuid, SkinTexture type) {
		if (type == SkinTexture.SKIN) {
			return "https://crafatar.com/skins/" + uuid;
		}
		if (type == SkinTexture.CAPE) {
			return "https://crafatar.com/capes/" + uuid;
		}
		return null;
	}
}
