package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
		if (p == null) {
			return null;
		}
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
				String uuid = obj.optString("id", null);

				nameToUUID.put(username, uuid);
				return uuid;
			}
		}

		return null;
	}

	private static final int ATTEMPTS = 3;
	private static final String[] USERNAME_LOOKUP = { "https://api.mojang.com/users/profiles/minecraft/", "https://api.minecraftservices.com/minecraft/profile/lookup/name/" };

	private static JSONObject requestUUIDfromName(String name) {
		try {
		next:
			for (String url : USERNAME_LOOKUP) {
				for (int i = 0; i < ATTEMPTS; i++) {
					URL profileURL = new URL(url + name);
					HttpURLConnection connection = (HttpURLConnection)openDirectConnection(profileURL);
					if (connection.getResponseCode() == 429) {
						Thread.sleep(1000);
						continue;
					}
					if (connection.getResponseCode() == 404) {
						return null;
					}
					if (connection.getResponseCode() != 200) {
						continue next;
					}
					InputStream connStream = connection.getInputStream();
					JSONObject uuidJson = new JSONObject(new String(Util.readStream(connStream), "UTF-8"));

					return uuidJson;
				}
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
		public final Map<SkinTexture, String> textures;
		public final boolean slim;

		public Profile(String name, String uuid, Map<SkinTexture, String> textures, boolean slim) {
			this.name = name;
			this.uuid = uuid;
			this.textures = textures;
			this.slim = slim;
		}
	}

	public static Profile getProfile(String uuid) {
		if (uuid == null) {
			return null;
		}
		try {
			URL uuidtoprofileURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			HttpURLConnection connection = (HttpURLConnection)openDirectConnection(uuidtoprofileURL);
			// https://sessionserver.mojang.com/session/minecraft/profile/bbfd0136758c3f73a31ecc4963da4394
			if (connection.getResponseCode() == 204) {
				return null;
			}
			String response = new String(Util.readStream(connection.getInputStream()), "UTF-8");
			if (response.isEmpty()) {
				return null;
			}
			JSONObject profileJson = new JSONObject(response);
			JSONArray properties = profileJson.optJSONArray("properties");
			if (properties == null) {
				return null;
			}
			String texjsonstr = null;
			for (int i = 0; i < properties.length(); i++) {
				if (!"textures".equals(properties.getJSONObject(i).optString("name"))) {
					continue;
				}
				String base64tex = properties.getJSONObject(i).optString("value", null);
				if (base64tex == null) {
					continue;
				}
				texjsonstr = new String(Base64.decode(base64tex), "UTF-8");
			}
			String name = profileJson.optString("name", null);
			if (texjsonstr == null) {
				return new Profile(name, uuid, Collections.<SkinTexture, String>emptyMap(), false);
			}
			Map<SkinTexture, String> texMap = new HashMap<SkinTexture, String>();
			boolean slim = false;

			JSONObject texjson = new JSONObject(texjsonstr);
			JSONObject txts = texjson.optJSONObject("textures");
			if (txts != null) {
				for (SkinTexture tex : SkinTexture.values()) {
					JSONObject texJson = txts.optJSONObject(tex.name());
					if (texJson != null) {
						JSONObject metadata = texJson.optJSONObject("metadata");
						if (metadata != null && tex == SkinTexture.SKIN) {
							slim = "slim".equals(metadata.optString("model"));
						}
						String skinURLstr = texJson.optString("url");
						texMap.put(tex, skinURLstr.substring(skinURLstr.lastIndexOf('/') + 1));
					}
				}
			}
			Profile p = new Profile(name, uuid, texMap, slim);
			return p;

		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public Skin getSkin(String uuid, SkinTexture type) {
		if (uuid == null) {
			return null;
		}
		Profile profile = getProfile(uuid);
		if (profile == null) {
			return null;
		}
		return new MojangSkin(profile, type);
	}

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		Skin skin = getSkin(uuid, type);
		if (skin == null) {
			uuid = getUUIDfromName(name);
			skin = getSkin(uuid, type);
		}
		return skin;
	}

	public static class MojangSkin implements Skin {
		private Profile profile;
		private SkinTexture type;

		public MojangSkin(Profile profile, SkinTexture type) {
			this.profile = profile;
			this.type = type;
		}

		public String getSHA256() {
			return profile.textures.get(type);
		}

		public byte[] getData() throws IOException {
			String url = getURL();
			if (url != null) {
				return Util.readStream(openDirectConnection(new URL(url)).getInputStream());
			}
			return null;
		}

		public String getURL() {
			return "http://textures.minecraft.net/texture/" + getSHA256();
		}

		public boolean isSlim() {
			return profile.slim;
		}
	}
}
