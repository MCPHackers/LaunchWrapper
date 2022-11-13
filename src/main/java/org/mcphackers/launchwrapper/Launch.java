package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.inject.AppletWrapper;
import org.mcphackers.launchwrapper.inject.DefaultTweak;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public class Launch {

	private static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();

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
			.setResolution(854, 480)
			//.setIsom(true)
			.launch();
	}
	
	public void launch() {
		CLASS_LOADER.addException(Launch.class);
		DefaultTweak tweak = new DefaultTweak(CLASS_LOADER, this);
		tweak.transform();
		
		if(isom) {
			try {
				AppletWrapper.startApplet(CLASS_LOADER.findClass("net/minecraft/isom/IsomPreviewApplet").asSubclass(Applet.class), width, height, "Isometric Preview", ImageIO.read(getClass().getResourceAsStream("/favicon.png")));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		if(tweak.getLaunchTarget() == null) {
			try {
				launchWrapped(tweak);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		CLASS_LOADER.invokeMain(tweak.getLaunchTarget(), username, sessionId);
	}
	
	private void launchWrapped(final DefaultTweak launchTarget) throws InstantiationException, IllegalAccessException, SecurityException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class<?> minecraft = CLASS_LOADER.findClass(launchTarget.minecraft.name);
		final Field running = minecraft.getDeclaredField(launchTarget.running.name);
		final Field userField = minecraft.getDeclaredField(launchTarget.userField.name);
		Class<? extends Applet> minecraftApplet = CLASS_LOADER.findClass(launchTarget.minecraftApplet.name).asSubclass(Applet.class);
		Constructor<?> minecraftImpl = CLASS_LOADER.findClass(launchTarget.minecraftImpl.name).getConstructors()[0];
		Constructor<?> user = CLASS_LOADER.findClass(launchTarget.user.name).getConstructor(String.class, String.class);
		Frame frame = null;
		Canvas canvas = null;
		Applet applet = minecraftApplet.newInstance();
		if(applet != null) {
			applet.setStub(new AppletWrapper());
		}
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
		final Runnable mc = newInstance(minecraftImpl, minecraftApplet, frame, canvas, applet, width, height, fullscreen);
		if(mc == null) {
			System.err.println("Could not create Minecraft instance!");
			if(frame != null) frame.dispose();
			return;
		}
		Util.setField(mc, userField, user.newInstance(username, sessionId));
		final Thread mcThread = new Thread(mc, "Minecraft main thread");
		mcThread.setPriority(10);
		
		if(!useLWJGLFrame) {
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent event) {
					Util.setField(mc, running, false);
					try {
						mcThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.exit(0);
				}
			});
		}
		mcThread.start();
	}

	public Runnable newInstance(Constructor<?> minecraftImpl, Class<?> minecraftApplet, Frame frame, Canvas canvas, Applet applet, int w, int h, boolean fullscreen) {
		
		Class<?>[] types = minecraftImpl.getParameterTypes();
		try {
			if(types.length == 0) {
				return (Runnable)minecraftImpl.newInstance();
			}
			if(Arrays.equals(types, new Class[] { Canvas.class, int.class, int.class, boolean.class })) {
				return (Runnable)minecraftImpl.newInstance(canvas, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)minecraftImpl.newInstance(canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { Component.class, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)minecraftImpl.newInstance(frame, canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { minecraftApplet, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)minecraftImpl.newInstance(applet, canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { minecraftApplet, Component.class, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)minecraftImpl.newInstance(applet, frame, canvas, applet, w, h, fullscreen);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
