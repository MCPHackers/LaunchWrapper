package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler.SkinType;
import org.mcphackers.launchwrapper.util.Util;

public class SkinURLConnection extends HttpURLConnection {
	
	private static final String[] CLOAKS = {"/cloak/", "/MinecraftCloaks/"};
	private static final String[] SKINS = {"/skin/", "/MinecraftSkins/"};
	
	protected SkinType skinType;
	protected InputStream inputStream;
	
	public SkinURLConnection(URL url, SkinType skin) {
		super(url);
		responseCode = 200;
		skinType = skin == null ? SkinType.DEFAULT : skin;
	}

	@Override
	public void disconnect() {}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public void connect() throws IOException {
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