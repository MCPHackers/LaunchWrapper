package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.Util;

public class MinecraftCapesProvider implements SkinProvider {
	private final File assetsDir;

	public MinecraftCapesProvider(File assetsDir) {
		this.assetsDir = assetsDir;
	}

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (type != SkinTexture.CAPE) {
			return null;
		}
		Skin skin = getCape(uuid);
		if (skin == null) {
			uuid = MojangSkinProvider.getUUIDfromName(name);
			skin = getCape(uuid);
		}
		return skin;
	}

	private Skin getCape(String uuid) {
		if (uuid == null) {
			return null;
		}
		try {
			URL profileURL = new URL("https://api.minecraftcapes.net/profile/" + uuid);
			JSONObject profileJson = new JSONObject(new String(Util.readStream(openDirectConnection(profileURL).getInputStream()), "UTF-8"));

			JSONObject txts = profileJson.optJSONObject("textures");
			if (txts != null) {
				String png = txts.optString("cape", null);
				if (png != null) {
					byte[] cape = Base64.decode(png);
					return new SkinMCCape(cape);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public class SkinMCCape implements Skin {
		private String sha256;
		private byte[] data;

		public SkinMCCape(byte[] data) {
			this.data = data;
			try {
				this.sha256 = Util.getSHA256(new ByteArrayInputStream(data));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getSHA256() {
			return sha256;
		}

		public byte[] getData() throws IOException {
			return data;
		}

		public String getURL() {
			String hash = getSHA256();
			File f = new File(assetsDir, "skins/" + hash.substring(0, 2) + "/" + hash);
			try {
				// Download to assetsDir then return a file:// url
				File dir = f.getParentFile();
				if(dir != null) {
					dir.mkdirs();
				}
				Util.copyStream(new ByteArrayInputStream(data), new FileOutputStream(f));
				return f.toURI().toString();
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		public boolean isSlim() {
			return false;
		}
	}
}
