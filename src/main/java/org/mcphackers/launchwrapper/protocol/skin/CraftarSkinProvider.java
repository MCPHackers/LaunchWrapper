package org.mcphackers.launchwrapper.protocol.skin;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.IOException;
import java.net.URL;

import org.mcphackers.launchwrapper.util.Util;

public class CraftarSkinProvider implements SkinProvider {

	public Skin getSkin(String uuid, String name, SkinTexture type) {
		return new CraftarSkin(uuid, type);
	}

	public static class CraftarSkin implements Skin {
		private String uuid;
		private SkinTexture type;

		public CraftarSkin(String uuid, SkinTexture type) {
			this.uuid = uuid;
			this.type = type;
		}

		public String getSHA256() {
			return null;
		}

		public byte[] getData() throws IOException {
			String url = getURL();
			if (url != null) {
				return Util.readStream(openDirectConnection(new URL(url)).getInputStream());
			}
			return null;
		}

		public String getURL() {
			if(type == SkinTexture.SKIN) {
				return "https://crafatar.com/skins/" + uuid;
			}
			if(type == SkinTexture.CAPE) {
				return "https://crafatar.com/capes/" + uuid;
			}
			return null;
		}

		public boolean isSlim() {
			return false;
		}
	}

}
