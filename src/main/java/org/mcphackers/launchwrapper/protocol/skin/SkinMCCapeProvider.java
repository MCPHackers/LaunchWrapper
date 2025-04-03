package org.mcphackers.launchwrapper.protocol.skin;

public class SkinMCCapeProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (type != SkinTexture.CAPE) {
			return null;
		}
		if (uuid == null) {
			uuid = MojangSkinProvider.getUUIDfromName(name);
		}
		if (uuid != null) {
			return new HttpSkin("https://skinmc.net/api/v1/cape/" + convertUUID(uuid));
		}
		return null;
	}

	private String convertUUID(String uuid) {
		StringBuilder sb = new StringBuilder();
		sb.append(uuid.substring(0, 8));
		sb.append('-');
		for (int i = 0; i < 3; i++) {
			sb.append(uuid.substring(8 + i * 4, 8 + i * 4 + 4));
			sb.append('-');
		}
		sb.append(uuid.substring(20, uuid.length()));
		return sb.toString();
	}
}
