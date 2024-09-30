package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.protocol.SkinRequests.Profile;

public class ProfileURLConnection extends HttpURLConnection {

	protected SkinRequests skinRequests;
	protected JSONObject response;

	public ProfileURLConnection(URL url, SkinRequests skinRequests) {
		super(url);
		this.skinRequests = skinRequests;
		responseCode = 200;
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String uuid = url.getPath().substring("/session/minecraft/profile/".length());
		Profile profile = skinRequests.getProfile(uuid);
		return new ByteArrayInputStream(profile.json.toString().getBytes());
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}
}
