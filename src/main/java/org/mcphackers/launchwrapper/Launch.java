package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;

public class Launch {

	public static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	
	public static final String[] MAIN_CLASSES = {
			"net.minecraft.client.Minecraft",
			"com.mojang.minecraft.Minecraft",
			"com.mojang.minecraft.RubyDung",
			"com.mojang.rubydung.RubyDung"
	};
	public static final String[] MAIN_APPLETS = {
			"net.minecraft.client.MinecraftApplet",
			"com.mojang.minecraft.MinecraftApplet"
	};


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

		new Launch()
			.setUser(username, sessionId)
			.setGameDirectory(gameDir)
			//.setLWJGLFrame(true)
			//.setFullscreen(true)
			.launch();
	}
	
	private String username, sessionId;
	private int width, height;
	private boolean fullscreen;
	private File gameDir;
	private String serverIp;
	private int serverPort;
	private boolean lwjglFrame;
	private boolean forceWrapper = true;
	
	public Launch() {
		setUser(null, null);
		setResolution(854, 480);
	}
	
	public Launch(String username, String sessionId) {
		this();
		setUser(username, sessionId);
	}
	
	public Launch setUser(String username, String sessionId) {
		this.username = username != null ? username : "Player" + System.currentTimeMillis() % 1000L;
		this.sessionId = sessionId != null ? sessionId : "-";
		return this;
	}
	
	public Launch setServer(String ip, int port) {
		this.serverIp = ip;
		this.serverPort = port;
		return this;
	}
	
	public Launch setResolution(int w, int h) {
		this.width = w;
		this.height = h;
		return this;
	}
	
	public Launch setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
		return this;
	}
	
	public Launch setGameDirectory(File file) {
		this.gameDir = file;
		return this;
	}
	
	public Launch setLWJGLFrame(boolean f) {
		lwjglFrame = f;
		return this;
	}
	
	public void launch() {
		LaunchTarget launchTarget = getLaunchTarget();
		
		if(launchTarget == null) {
			System.err.println("Could not find launch target");
			return;
		}
		
		if(!forceWrapper && launchTarget.main != null && Util.isStatic(launchTarget.mcDir)) {
			launchMain(launchTarget);
		} else {
			launchWrapped(launchTarget);
		}
	}
	
	private void launchWrapped(final LaunchTarget launchTarget) {
		Frame frame = null;
		Canvas canvas = null;
		Applet applet = launchTarget.getAppletInstance();
		boolean useLWJGLFrame = lwjglFrame || !launchTarget.supportsCanvas();
		if(!useLWJGLFrame) {
			canvas = new Canvas();
			applet.setLayout(new BorderLayout());
			applet.add(canvas, "Center");
			frame = new Frame("Minecraft");
			frame.setBackground(Color.BLACK);
			try {
				frame.setIconImage(ImageIO.read(getClass().getResource("/favicon.png")));
			} catch (Exception exception10) { }
			frame.setLayout(new BorderLayout());
			frame.add(applet, "Center");
			canvas.setPreferredSize(new Dimension(width, height));
			frame.pack();
			frame.setLocationRelativeTo(null);
		}
		if(applet != null) {
			applet.setStub(new AppletWrapper());
		}
		final Runnable mc = launchTarget.getInstance(frame, canvas, applet, width, height, fullscreen, false);
		if(mc == null) {
			System.err.println("Could not create Minecraft instance! Attempting to use main method...");
			frame.dispose();
			launchMain(launchTarget);
			return;
		}
    	Util.setField(mc, launchTarget.width, width);
    	Util.setField(mc, launchTarget.height, height);
		
    	//TODO Better way to resize?
		if(canvas != null && launchTarget.minecraft.getName().equals("com.mojang.minecraft.Minecraft")) {
			final Canvas canvas1 = canvas;
			canvas.addComponentListener(new ComponentAdapter() {
	            public void componentResized(ComponentEvent e) {
	            	Dimension dim = canvas1.getSize();
	            	int w = (int)dim.getWidth();
	            	int h = (int)dim.getHeight();
	            	if(w > 0) Util.setField(mc, launchTarget.width, w);
	            	if(h > 0) Util.setField(mc, launchTarget.height, h);
	            }
	        });
		}
		Thread mcThread = new Thread(mc, "Minecraft main thread");
		mcThread.setPriority(10);
		
		launchTarget.setAppletMode(mc, false);
		launchTarget.setUser(mc, username, sessionId);
		if(gameDir != null && launchTarget.mcDir != null) {
			Util.setField(mc, launchTarget.mcDir, gameDir);
		}

		if(serverIp != null) {
			launchTarget.setServer(mc, serverIp, serverPort);
		}
		if(!useLWJGLFrame) {
			frame.setVisible(true);
			frame.addWindowListener(launchTarget.getWindowListener(mc, mcThread));
		}
		mcThread.start();
	}
	
	private void launchMain(final LaunchTarget launchTarget) {
		if(launchTarget.main == null) {
			System.err.println("Couldn't start Minecraft!");
			return;
		}
		try {
			Util.setField(null, launchTarget.mcDir, gameDir);
			launchTarget.main.invoke(null, (Object) new String[] {username, sessionId});
		} catch (Exception e) {
			System.err.println("Couldn't start Minecraft!");
			e.printStackTrace();
		}
	}
	
	public static LaunchTarget getLaunchTarget() {
		try {
			Class<? extends Applet> applet = getApplet();
			LaunchTarget target = new LaunchTarget(getMinecraft(applet), applet);
			return target;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Class<? extends Applet> getApplet() {
		Class<?> applet = null;
		for(String main : MAIN_APPLETS) {
			try {
				applet = CLASS_LOADER.findClass(main);
			}
			catch (ClassNotFoundException e) {};
		}
		return applet == null ? null : applet.asSubclass(Applet.class);
	}
	
	public static Class<?> getMinecraft(Class<?> applet) {
		Class<?> launchTarget = null;
		searchMain:
		for(String main : MAIN_CLASSES) {
			try {
				Class<?> cls = CLASS_LOADER.findClass(main);
				for(Class<?> cls2 : cls.getInterfaces()) {
					if(cls2 == Runnable.class) {
						launchTarget = cls;
						break searchMain;
					}
				}
			}
			catch (ClassNotFoundException e) {};
		}
		if(launchTarget == null && applet != null) {
			for(Field field : applet.getDeclaredFields()) {
				Class<?> type = field.getType();
				if(type != Canvas.class && type != Thread.class) {
					launchTarget = type;
				}
			}
		}
		return launchTarget;
	}
	
}
