package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Util;

public class ElyBySkinProvider implements SkinProvider {

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
		try {
			boolean slim = false;
			String url = null;

			URL texturesURL = new URL("http://skinsystem.ely.by/textures/" + name);
			JSONObject texturesJson = new JSONObject(new String(Util.readStream(openDirectConnection(texturesURL).getInputStream()), "UTF-8"));
			if (texturesJson != null && (type == SkinTexture.SKIN || type == SkinTexture.CAPE)) {
				JSONObject skinjson = texturesJson.optJSONObject(type == SkinTexture.SKIN ? "SKIN" : "CAPE");
				if (skinjson != null) {
					JSONObject metadata = skinjson.optJSONObject("metadata");
					if (metadata != null) {
						slim = "slim".equals(metadata.optString("model"));
					}
					url = skinjson.optString("url", null);
				}
			}
			if (url != null) {
				return new ElyBySkin(url, slim);
			}
			return null;

		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public static class ElyBySkin implements Skin {
		private boolean slim;
		private String url;

		public ElyBySkin(String url, boolean slim) {
			this.url = url;
			this.slim = slim;
		}

		public String getSHA256() {
			return null;
		}

		public byte[] getData() throws IOException {
			return Util.readStream(openDirectConnection(new URL(url)).getInputStream());
		}

		public boolean isSlim() {
			return slim;
		}
	}
}
