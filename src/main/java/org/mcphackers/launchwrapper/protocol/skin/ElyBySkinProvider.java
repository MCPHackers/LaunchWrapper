package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
			String response = new String(Util.readStream(openDirectConnection(texturesURL).getInputStream()), "UTF-8");
			if (response.length() == 0) {
				return null;
			}
			JSONObject texturesJson = new JSONObject(response);
			if (texturesJson != null) {
				JSONObject skinjson = texturesJson.optJSONObject(type.name());
				if (skinjson != null) {
					JSONObject metadata = skinjson.optJSONObject("metadata");
					if (metadata != null && type == SkinTexture.SKIN) {
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
			return url.substring(url.lastIndexOf('/') + 1);
		}

		public InputStream getData() throws IOException {
			HttpURLConnection connection = (HttpURLConnection)openDirectConnection(new URL(url));
			if (connection.getResponseCode() != 200) {
				return null;
			}
			return connection.getInputStream();
		}

		public String getURL() {
			return url;
		}

		public boolean isSlim() {
			return slim;
		}
	}
}
