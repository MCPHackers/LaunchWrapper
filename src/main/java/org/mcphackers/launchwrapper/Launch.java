package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.LaunchTarget.Type;
import org.mcphackers.launchwrapper.inject.AppletWrapper;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.DefaultTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	private static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	public static final BufferedImage ICON = getIcon();

	public static void main(String[] args) {
		File gameDir = null;
		String username = null;
		String sessionId = null;
		for(int i = 0; i < args.length; i++) {
			switch(i) {
			case 0:
				username = args[i];
				break;
			case 1:
				sessionId = args[i];
				break;
			case 2:
				gameDir = new File(args[i]);
				break;
			}
		}

		Launch.create()
			.setUser(username, sessionId)
			.setGameDirectory(gameDir)
			.setResolution(1280, 720)
			.setLWJGLFrame(true)
			.launch();
	}

	public void launch() {
		CLASS_LOADER.addException(Launch.class);
		Tweak mainTweak = DefaultTweak.get(CLASS_LOADER, this);
		if(mainTweak.transform()) {
			launch(mainTweak.getLaunchTarget());
		}
	}
	
	public void launch(LaunchTarget target) {
		if(target.type == Type.MAIN) {
			CLASS_LOADER.invokeMain(target.targetClass, username, sessionId);
		} else if(target.type == Type.APPLET) {
			Class<? extends Applet> appletClass;
			try {
				appletClass = CLASS_LOADER.findClass(target.targetClass).asSubclass(Applet.class);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			AppletWrapper.startApplet(appletClass, width, height, isom ? "Isometric Preview" : "Minecraft");
		}
	}
	
	private static BufferedImage getIcon() {
		try {
			return ImageIO.read(Launch.class.getResourceAsStream("/favicon.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

    public static ByteBuffer loadIcon() {
    	final BufferedImage icon = ICON;
        final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());

        final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
        for (int color : rgb) {
            buffer.putInt(color << 8 | ((color >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }
	
	public static LaunchBuilder create() {
		return new LaunchBuilder();
	}
	
	public static class LaunchBuilder {
		private String username = "Player" + System.currentTimeMillis() % 1000L;
		private String sessionId = "-";	
		private int width = 854;
		private int height = 480;
		private boolean fullscreen;
		private File gameDir;
		private String serverIp;
		private int serverPort;
		private boolean lwjglFrame;
		private boolean isom;
		
		private LaunchBuilder() {
		}

		public LaunchBuilder setIsom(boolean b) {
			this.isom = b;
			return this;
		}

		public LaunchBuilder setUser(String username, String sessionId) {
			this.username = username != null ? username : "Player" + System.currentTimeMillis() % 1000L;
			this.sessionId = sessionId != null ? sessionId : "-";
			return this;
		}
		
		public LaunchBuilder setServer(String ip, int port) {
			this.serverIp = ip;
			this.serverPort = port;
			return this;
		}
		
		public LaunchBuilder setResolution(int w, int h) {
			this.width = w;
			this.height = h;
			return this;
		}
		
		public LaunchBuilder setFullscreen(boolean fullscreen) {
			this.fullscreen = fullscreen;
			return this;
		}
		
		public LaunchBuilder setGameDirectory(File file) {
			this.gameDir = file;
			return this;
		}
		
		public LaunchBuilder setLWJGLFrame(boolean f) {
			lwjglFrame = f;
			return this;
		}
		
		public Launch build() {
			return new Launch(username, sessionId, width, height, fullscreen, gameDir, serverIp, serverPort, lwjglFrame, isom);
		}
		
		public void launch() {
			build().launch();
		}
	}
	
	public final String username, sessionId;	
	public final int width, height;
	public final boolean fullscreen;
	public final File gameDir;
	public final String serverIp;
	public final int serverPort;
	public final boolean lwjglFrame;
	public final boolean isom;
	
	private Launch(String username, String sessionId, int width, int height, boolean fullscreen, File gameDir, String serverIp, int serverPort, boolean lwjglFrame, boolean isom) {
		this.username = username;
		this.sessionId = sessionId;
		this.width = width;
		this.height = height;
		this.fullscreen = fullscreen;
		this.gameDir = gameDir;
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		this.lwjglFrame = lwjglFrame;
		this.isom = isom;
	}
}
