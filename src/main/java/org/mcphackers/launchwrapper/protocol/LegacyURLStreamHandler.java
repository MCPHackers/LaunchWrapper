package org.mcphackers.launchwrapper.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class LegacyURLStreamHandler extends URLStreamHandlerProxy {
	
	private SkinType skins;
	private int port;
	
	public LegacyURLStreamHandler(SkinType skins, int port) {
		this.skins = skins;
		this.port = port;
	}

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
			else if (path.startsWith("/MinecraftResources/") || path.startsWith("/resources/"))
				return new ResourceIndexURLConnection(url, port);
			else if (path.startsWith("/MinecraftSkins/") ||  path.startsWith("/skin/")
				  || path.startsWith("/MinecraftCloaks/") || path.startsWith("/cloak/"))
				return new SkinURLConnection(url, skins);
			else if(host.equals("assets.minecraft.net") && path.equals("/1_6_has_been_released.flag")) {
				return new BasicResponseURLConnection(url, "");
				//return new BasicResponseURLConnection(url, "https://web.archive.org/web/20130702232237if_/https://mojang.com/2013/07/minecraft-the-horse-update/");
			}
		}
		return super.openConnection(url);
	}
	
	public enum SkinType {
		CLASSIC("classic"),
		PRE_B1_9("pre-b1.9-pre4"),
		PRE_1_8("pre-1.8"),
		DEFAULT("default");
		
		private String name;
		
		private SkinType(String name) {
			this.name = name;
		}
		
		public static SkinType get(String name) {
			for(SkinType skinType : values()) {
				if(skinType.name.equals(name)) {
					return skinType;
				}
			}
			return null;
		}
	}
	
	public enum ResourceIndex {
	}
}