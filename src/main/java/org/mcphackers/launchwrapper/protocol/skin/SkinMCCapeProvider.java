package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.net.HttpURLConnection;
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
		if (uuid != null) {
			return new SkinMCCape(convertUUID(uuid));
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

	public static class SkinMCCape implements Skin {
		private String uuid;

		public SkinMCCape(String uuid) {
			this.uuid = uuid;
		}

		public String getSHA256() {
			return null;
		}

		public byte[] getData() throws IOException {
			HttpURLConnection httpConnection = (HttpURLConnection)openDirectConnection(new URL(getURL()));
			if (httpConnection.getResponseCode() != 200) {
				return null;
			}
			return Util.readStream(httpConnection.getInputStream());
		}

		public String getURL() {
			return "https://skinmc.net/api/v1/skinmcCape/" + uuid;
		}

		public boolean isSlim() {
			return false;
		}
	}
}
