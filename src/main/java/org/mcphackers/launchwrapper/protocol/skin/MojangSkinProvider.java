package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.Util;

public class MojangSkinProvider implements SkinProvider {
	// name->uuid requests have a rate limit, so here's cache
	private static HashMap<String, String> nameToUUID = new HashMap<String, String>();

	// No skins and capes for you. Sorry guys
	private static final String[] BLACKLISTED_NAMES = { "Player", "DemoUser" };

	public static String getNameFromUUID(String uuid) {
		Profile p = getProfile(uuid);
		return p.name;
	}

	public static String getUUIDfromName(String username) {
		for (String s : BLACKLISTED_NAMES) {
			if (s.equals(username)) {
				return null;
			}
		}
		String cachedUUID = nameToUUID.get(username);

		if (cachedUUID != null) {
			return cachedUUID;
		} else {
			JSONObject obj = requestUUIDfromName(username);
			if (obj != null) {
				String uuid = obj.optString("id");

				nameToUUID.put(username, uuid);
				return uuid;
			}
		}

		return null;
	}

	private static final int ATTEMPTS = 3;

	private static JSONObject requestUUIDfromName(String name) {
		try {
			for (int i = 0; i < ATTEMPTS; i++) {
				URL profileURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
				HttpURLConnection connection = (HttpURLConnection)openDirectConnection(profileURL);
				if (connection.getResponseCode() == 429) {
					Thread.sleep(1000);
					continue;
				}
				if (connection.getResponseCode() == 404) {
					return null;
				}
				InputStream connStream = connection.getInputStream();
				JSONObject uuidJson = new JSONObject(new String(Util.readStream(connStream), "UTF-8"));

				return uuidJson;
			}
			return null;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public static class Profile {
		public final String name;
		public final String uuid;
		public final String skinTex;
		public final String capeTex;
		public final boolean slim;

		public Profile(String name, String uuid, String skinTex, String capeTex, boolean slim) {
			this.name = name;
			this.uuid = uuid;
			this.skinTex = skinTex;
			this.capeTex = capeTex;
			this.slim = slim;
		}
	}

	public static Profile getProfile(String uuid) {
		try {
			URL uuidtoprofileURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			JSONObject profileJson = new JSONObject(new String(Util.readStream(openDirectConnection(uuidtoprofileURL).getInputStream()), "UTF-8"));
			JSONArray properties = profileJson.optJSONArray("properties");
			if (properties == null) {
				return null;
			}
			String texjsonstr = null;
			for (int i = 0; i < properties.length(); i++) {
				if (!"textures".equals(properties.getJSONObject(i).optString("name"))) {
					continue;
				}
				String base64tex = properties.getJSONObject(i).optString("value");
				texjsonstr = new String(Base64.decode(base64tex), "UTF-8");
			}
			String name = profileJson.optString("name", null);
			if (texjsonstr == null) {
				return new Profile(name, uuid, null, null, false);
			}
			boolean slim = false;
			String skinHash = null;
			String capeHash = null;

			JSONObject texjson = new JSONObject(texjsonstr);
			JSONObject txts = texjson.optJSONObject("textures");
			if (txts != null) {
				JSONObject skinjson = txts.optJSONObject("SKIN");
				if (skinjson != null) {
					JSONObject metadata = skinjson.optJSONObject("metadata");
					if (metadata != null) {
						slim = "slim".equals(metadata.optString("model"));
					}
					String skinURLstr = skinjson.optString("url");
					skinHash = skinURLstr.substring(skinURLstr.lastIndexOf('/') + 1);
				}
				skinjson = txts.optJSONObject("CAPE");
				if (skinjson != null) {
					String skinURLstr = skinjson.optString("url");
					capeHash = skinURLstr.substring(skinURLstr.lastIndexOf('/') + 1);
				}
			}
			Profile p = new Profile(name, uuid, skinHash, capeHash, slim);
			return p;

		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (uuid == null) {
			uuid = getUUIDfromName(name);
		}
		return new MojangSkin(uuid, type);
	}

	public static class MojangSkin implements Skin {
		private Profile profile;
		private SkinTexture type;

		public MojangSkin(String uuid, SkinTexture type) {
			this.profile = getProfile(uuid);
			this.type = type;
		}

		public String getSHA256() {
			if (type == SkinTexture.SKIN) {
				return profile.skinTex;
			}
			if (type == SkinTexture.CAPE) {
				return profile.capeTex;
			}
			return null;
		}

		public byte[] getData() throws IOException {
			if (type == SkinTexture.SKIN) {
				URL skinURL = new URL("http://textures.minecraft.net/texture/" + profile.skinTex);
				return Util.readStream(openDirectConnection(skinURL).getInputStream());
			}
			if (type == SkinTexture.CAPE) {
				URL skinURL = new URL("http://textures.minecraft.net/texture/" + profile.capeTex);
				return Util.readStream(openDirectConnection(skinURL).getInputStream());
			}
			return null;
		}

		public boolean isSlim() {
			return profile.slim;
		}
	}
}
