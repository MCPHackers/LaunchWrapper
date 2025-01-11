package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.protocol.skin.SkinRequests;
import org.mcphackers.launchwrapper.protocol.skin.SkinTexture;
import org.mcphackers.launchwrapper.util.Util;

public class SkinURLConnection extends HttpURLConnection {

	private static final String[] CLOAKS = { "/cloak/", "/MinecraftCloaks/" };
	private static final String[] SKINS = { "/skin/", "/MinecraftSkins/" };

	protected SkinRequests skinRequests;
	protected InputStream inputStream;

	public SkinURLConnection(URL url, SkinRequests skinRequests) {
		super(url);
		responseCode = 200;
		this.skinRequests = skinRequests;
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public void connect() throws IOException {
		Map<String, String> queryMap = Util.queryMap(url);
		String username = queryMap.get("user");
		String path = url.getPath();

		for (String template : SKINS) {
			if (!path.startsWith(template)) {
				continue;
			}
			if (username == null) {
				username = path.substring(template.length(), path.length() - ".png".length());
			}
			byte[] skinData = skinRequests.getSkin(null, username, SkinTexture.SKIN);
			if (skinData != null) {
				inputStream = new ByteArrayInputStream(skinData);
				return;
			}
		}

		for (String template : CLOAKS) {
			if (!path.startsWith(template)) {
				continue;
			}
			if (username == null) {
				username = path.substring(template.length(), path.length() - ".png".length());
			}
			byte[] skinData = skinRequests.getSkin(null, username, SkinTexture.CAPE);
			if (skinData != null) {
				inputStream = new ByteArrayInputStream(skinData);
				return;
			}
		}
		responseCode = 404;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}
}
