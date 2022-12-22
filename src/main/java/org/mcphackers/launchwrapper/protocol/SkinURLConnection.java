package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.protocol.SkinRequests.SkinType;
import org.mcphackers.launchwrapper.util.Util;

public class SkinURLConnection extends HttpURLConnection {
	
	private static final String[] CLOAKS = new String[] {"/cloak/", "/MinecraftCloaks/"};
	private static final String[] SKINS = new String[] {"/skin/", "/MinecraftSkins/"};
	
	protected SkinType skinType = SkinType.PRE_1_8;
	protected InputStream inputStream;
	
	public SkinURLConnection(URL url) {
		super(url);
		responseCode = 200;
	}

	@Override
	public void disconnect() {}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public void connect() throws IOException {
		File skinDir = new File(Launch.INSTANCE.config.gameDir.get(), "skins");
		Map<String, String> queryMap = Util.queryMap(url);
		String username = queryMap.get("user");
		String path = url.getPath();

		for (String template : CLOAKS) {
			if (path.startsWith(template)) {
				if(username == null)
					username = path.substring(template.length()).replace(".png", "");
				byte[] skinData = SkinRequests.getCape(username, skinType);
				if(skinData != null) {
					inputStream = new ByteArrayInputStream(skinData);
					return;
				}
			}
		}

		for (String template : SKINS) {
			if (path.startsWith(template)) {
				if(username == null)
					username = path.substring(template.length()).replace(".png", "");
				File cached = new File(skinDir, username + ".png");
				if(cached.exists()) {
					inputStream = new FileInputStream(cached);
					return;
				}
				byte[] skinData = SkinRequests.getSkin(username, skinType);
				if(skinData != null) {
					inputStream = new ByteArrayInputStream(skinData);
					return;
				}
			}
		}
		responseCode = 404;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}
}