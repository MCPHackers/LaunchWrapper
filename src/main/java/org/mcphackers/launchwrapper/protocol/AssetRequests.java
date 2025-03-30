package org.mcphackers.launchwrapper.protocol;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.openDirectConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.Util;

public class AssetRequests {
	private static final boolean VERIFY_HASH = Boolean.parseBoolean(System.getProperty("launchwrapper.verifyAssetHash"));

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
			File indexFile = new File(assetsDir, "indexes/" + index + ".json");
			if (!indexFile.exists()) {
				return;
			}
			String inputString = new String(Util.readStream(new FileInputStream(indexFile)));
			JSONObject indexObj = new JSONObject(inputString);

			JSONObject objects = indexObj.getJSONObject("objects");
			boolean virtual = indexObj.optBoolean("virtual");

			for (String s : objects.keySet()) {
				JSONObject entry = objects.optJSONObject(s);
				if (entry == null) {
					continue;
				}
				String hash = entry.optString("hash", null);
				long size = entry.optLong("size");
				String url = null;
				File object = new File(assetsDir, "objects/" + hash.substring(0, 2) + "/" + hash);
				if (!object.exists() || object.length() != size || VERIFY_HASH && !hash.equals(Util.getSHA1(new FileInputStream(object)))) {
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

				// Emulate launcher's handling of virtual asset index object mapping. (Copy from {assetsDir}/objects/{hash} to {assetsDir}/{name})
				if (virtual) {
					File mappedObject = new File(assetsDir, s);
					if (url != null) { // url is only set when the resource is requested to be re-downloaded
						Launch.LOGGER.log("Downloading resource: " + s);
						object.getParentFile().mkdirs();
						URLConnection connection = openDirectConnection(new URL(url));
						connection.setRequestProperty("User-Agent", "LaunchWrapper/" + Launch.VERSION);
						Util.copyStream(connection.getInputStream(), new FileOutputStream(object));
					}
					if (!mappedObject.exists() || mappedObject.length() != size || VERIFY_HASH && !hash.equals(Util.getSHA1(new FileInputStream(mappedObject)))) {
						mappedObject.getParentFile().mkdirs();
						Util.copyStream(new FileInputStream(object), new FileOutputStream(mappedObject));
					}
				}
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
