package org.mcphackers.launchwrapper.protocol;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.openDirectConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.Util;

public class JoinServerURLConnection extends HttpURLConnection {
	// Modern Minecraft (1.16+) reads this parameter, allowing users to set a custom session server for the game
	private static final String SESSION_URL = System.getProperty("minecraft.api.session.host", "https://sessionserver.mojang.com");

	private final String accessToken;
	private final String uuid;

	// joinserver.jsp: default response
	private String response = "Bad login";

	public JoinServerURLConnection(URL url, String accessToken, String uuid) {
		super(url);
		this.accessToken = accessToken;
		this.uuid = uuid;
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
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String serverId = Util.queryMap(url).get("serverId");

		Launch.LOGGER.logDebug("Session API | Handle: \"%s\"", url.toString());

		// The API expects JSON data to be sent via POST
		JSONObject json = new JSONObject();
		json.put("accessToken", accessToken);
		json.put("selectedProfile", uuid);
		json.put("serverId", serverId);

		URL sessionURL = new URL(SESSION_URL + "/session/minecraft/join");
		Launch.LOGGER.logDebug("Session API | Connect: \"%s\"", sessionURL.toString());

		HttpURLConnection connection = (HttpURLConnection) openDirectConnection(sessionURL);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		connection.setDoOutput(true);
		connection.getOutputStream().write(json.toString().getBytes("UTF-8"));
		connection.connect();

		// Session API responds with '204/No Content' upon success
		if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
			response = "OK"; // joinserver.jsp: session validated

		Launch.LOGGER.logDebug("Session API | Validation:\n- Token: <%s>\n- UUID: <%s>\n- Hash: <%s>\n- Response: '%s'", accessToken, uuid, serverId, response);

		return new ByteArrayInputStream(response.getBytes());
	}

	@Override
	public int getResponseCode() {
		return HttpURLConnection.HTTP_OK;
	}

	@Override
	public String getResponseMessage() {
		return "OK";
	}
}
