package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.Util;

public class MinecraftCapesProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		if (type != SkinTexture.CAPE) {
			return null;
		}
		if (uuid == null) {
			uuid = MojangSkinProvider.getUUIDfromName(name);
		}
		return getCape(uuid);
	}

	private Skin getCape(String uuid) {
		try {
			URL profileURL = new URL("https://api.minecraftcapes.net/profile/" + uuid);
			JSONObject profileJson = new JSONObject(new String(Util.readStream(openDirectConnection(profileURL).getInputStream()), "UTF-8"));

			JSONObject txts = profileJson.optJSONObject("textures");
			if (txts != null) {
				String png = txts.optString("cape");
				if (png != null) {
					byte[] cape = Base64.decode(png);
					return new SkinCape(cape);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public static class SkinCape implements Skin {
		private String sha256;
		private byte[] data;

		public SkinCape(byte[] data) {
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

		public boolean isSlim() {
			return false;
		}
	}
}
