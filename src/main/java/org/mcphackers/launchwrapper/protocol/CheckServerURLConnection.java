package org.mcphackers.launchwrapper.protocol;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.openDirectConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.Util;

public class CheckServerURLConnection extends HttpURLConnection {
	// Modern Minecraft (1.16+) reads this parameter, allowing users to set a custom session server for the game
	private static final String SESSION_URL = System.getProperty("minecraft.api.session.host", "https://sessionserver.mojang.com");

	// checkserver.jsp: default response
	private String response = "NO";

	public CheckServerURLConnection(URL url) {
		super(url);
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
		Map<String, String> queryMap = Util.queryMap(url);
		String username = queryMap.get("user");
		String serverId = queryMap.get("serverId");

		Launch.LOGGER.logDebug("Session API | Handle: \"%s\"", url.toString());

		String params = String.format("?username=%s&serverId=%s", username, serverId);
		URL sessionURL = new URL(SESSION_URL + "/session/minecraft/hasJoined" + params);
		Launch.LOGGER.logDebug("Session API | Connect: \"%s\"", sessionURL.toString());

		HttpURLConnection connection = (HttpURLConnection) openDirectConnection(sessionURL);
		connection.setRequestMethod("GET");
		connection.connect();

		// Session API responds with '200/OK' upon success
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
			response = "YES"; // checkserver.jsp: session validated

		Launch.LOGGER.logDebug("Session API | Validation:\n- Username: <%s>\n- Hash: <%s>\n- Response: '%s'", username, serverId, response);

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
