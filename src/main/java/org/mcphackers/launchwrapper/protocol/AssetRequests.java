package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Util;

public class AssetRequests {
	public class AssetObject {
		public File file;
		public String path;
		public String hash;
		public long size;

		public AssetObject(File f, String name, String sha1, long s) {
			file = f;
            path = name;
			hash = sha1;
			size = s;
		}
	}

	private Map<String, AssetObject> mappedAssets = new HashMap<String, AssetObject>();

    public AssetRequests(File assetsDir, String index) {
		if(index == null || assetsDir == null) {
			return;
		}
		try {
			String inputString = new String(Util.readStream(new FileInputStream(new File(assetsDir, "indexes/" + index + ".json"))));

			JSONObject objects = new JSONObject(inputString).getJSONObject("objects");
			for(String s : objects.keySet()) {
				JSONObject entry = objects.optJSONObject(s);
				if(entry == null) {
					continue;
				}
				String hash = entry.optString("hash");
				long size = entry.optLong("size");
				if(hash == null) {
					System.out.println("[LaunchWrapper] Invalid resource: " + s);
					continue;
				}
				File object = new File(assetsDir, "objects/" + hash.substring(0, 2) + "/" + hash);
				if(!object.exists() || object.length() != size) {
					System.out.println("[LaunchWrapper] Invalid resource: " + s);
					continue;
				}
				// A little slow and probably pointless
				// String sha1 = Util.getSHA1(new FileInputStream(new File(assetsDir, "objects/" + hash.substring(0, 2) + "/" + hash)));
				// if(!sha1.equals(hash)) {
				// 	continue;
				// }
				AssetObject obj = new AssetObject(object, s, hash, size);
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
