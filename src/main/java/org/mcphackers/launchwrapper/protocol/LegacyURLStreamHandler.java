package org.mcphackers.launchwrapper.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class LegacyURLStreamHandler extends URLStreamHandlerProxy {
	
//	private LegacyProxyData data;
//	
//	public LegacyURLStreamHandler(LegacyProxyData proxyData) {
//		data = proxyData;
//	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String host = url.getHost();
		String path = url.getPath();
		if(host.endsWith(".minecraft.net") || host.equals("s3.amazonaws.com")) {
			if (path.equals("/game/joinserver.jsp"))
				return new BasicResponseURLConnection(url, "ok");
			else if (path.equals("/login/session.jsp"))
				return new BasicResponseURLConnection(url, "ok");
			else if (host.equals("login.minecraft.net") && path.equals("/session"))
				return new BasicResponseURLConnection(url, "ok");
			else if (path.equals("/game/"))
				return new BasicResponseURLConnection(url, "42069");
			else if (path.equals("/haspaid.jsp"))
				return new BasicResponseURLConnection(url, "true");
			else if (path.contains("/level/save.html"))
				return new SaveLevelURLConnection(url);
			else if (path.contains("/level/load.html"))
				return new LoadLevelURLConnection(url);
			else if (path.equals("/listmaps.jsp"))
				return new ListLevelsURLConnection(url);
			else if (path.startsWith("/MinecraftResources/"))
				return new ResourceIndexURLConnection(url, true);
			else if (path.startsWith("/resources/"))
				return new ResourceIndexURLConnection(url, false);
			else if (path.startsWith("/MinecraftSkins/") || path.startsWith("/skin/"))
				return new SkinURLConnection(url);
			else if (path.startsWith("/MinecraftCloaks/") ||  path.startsWith("/cloak/"))
				return new SkinURLConnection(url);
			else if(host.equals("assets.minecraft.net") && path.equals("/1_6_has_been_released.flag")) {
				return new BasicResponseURLConnection(url, "");
				//return new BasicResponseURLConnection(url, "https://web.archive.org/web/20130702232237if_/https://mojang.com/2013/07/minecraft-the-horse-update/");
			}
		}
		return super.openConnection(url);
	}
}