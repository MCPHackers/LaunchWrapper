package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.net.URL;

import org.mcphackers.launchwrapper.util.Util;

public class SkinMCCapeProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (type != SkinTexture.CAPE) {
			return null;
		}
		if (uuid == null) {
			uuid = MojangSkinProvider.getUUIDfromName(name);
		}
		return new SkinCape(convertUUID(uuid));
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

	public static class SkinCape implements Skin {
		private String uuid;

		public SkinCape(String uuid) {
			this.uuid = uuid;
		}

		public String getSHA256() {
			return null;
		}

		public byte[] getData() throws IOException {
			return Util.readStream(openDirectConnection(new URL("https://skinmc.net/api/v1/skinmcCape/" + uuid)).getInputStream());
		}

		public boolean isSlim() {
			return false;
		}

	}
}
