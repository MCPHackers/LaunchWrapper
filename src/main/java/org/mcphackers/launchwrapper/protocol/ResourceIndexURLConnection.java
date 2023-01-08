package org.mcphackers.launchwrapper.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler.ResourceIndex;
import org.mcphackers.launchwrapper.util.Util;

public class ResourceIndexURLConnection extends URLConnection {
	private static final String[] RESOURCES = new String[] {"/resources/", "/MinecraftResources/"};

	ResourceIndex index = null;
	int port = -1;
	
	public ResourceIndexURLConnection(URL url, int port) {
		super(url);
		this.port = port;
	}

	@Override
	public void connect() throws IOException {}

	@Override
	public InputStream getInputStream() throws IOException {
		String path = url.getPath();
		for(String template : RESOURCES) {
			if(path.startsWith(template)) {
				return Util.replaceHost(url, "betacraft.uk", getPort()).openStream();
			}
		}
		return url.openStream();
	}
	
	protected int getPort() {
		return port;
	}
}
