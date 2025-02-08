package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.Util;

public class AssetRequests {
	public static class AssetObject {
		public File file;
		public String path;
		public String hash;
		public long size;
		public String url;

		public AssetObject(File f, String name, String sha1, long s, String urlString) {
			file = f;
			path = name;
			hash = sha1;
			size = s;
			url = urlString;
		}
	}

	private final Map<String, AssetObject> mappedAssets = new HashMap<String, AssetObject>();

	public AssetRequests(File assetsDir, String index) {
		if (index == null || assetsDir == null) {
			return;
		}
		try {
			String inputString = new String(Util.readStream(new FileInputStream(new File(assetsDir, "indexes/" + index + ".json"))));

			JSONObject objects = new JSONObject(inputString).getJSONObject("objects");
			for (String s : objects.keySet()) {
				JSONObject entry = objects.optJSONObject(s);
				if (entry == null) {
					continue;
				}
				String hash = entry.optString("hash", null);
				long size = entry.optLong("size");
				// Only resources in a folder are valid
				if (!s.contains("/") || hash == null) {
					Launch.LOGGER.logDebug("Invalid resource: " + s);
					continue;
				}
				String url = null;
				File object = new File(assetsDir, "objects/" + hash.substring(0, 2) + "/" + hash);
				if (!object.exists() || object.length() != size /* || !hash.equals(Util.getSHA1(new FileInputStream(object))) */) {
					// Download if missing
					// Some sounds in betacraft indexes are downloaded from custom url which isn't handled by other launchers

					if (entry.has("custom_url")) {
						url = entry.getString("custom_url");
					} else if (entry.has("url")) {
						url = entry.getString("url");
					} else {
						url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
					}
				}
				AssetObject obj = new AssetObject(object, s, hash, size, url);
				mappedAssets.put(s, obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AssetObject get(String key) {
		return mappedAssets.get(key);
	}

	public Collection<AssetObject> list() {
		return mappedAssets.values();
	}
}
