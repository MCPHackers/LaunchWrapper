package org.mcphackers.launchwrapper.inject;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.Launch;

// This class will be overriden in the new class loader context
public class Inject {
	
	public static Runnable getMinecraftInstance(Frame frame, Canvas canvas, Applet applet, int width, int height, boolean fullscreen) {
		return null;
	}

	public static Applet getAppletInstance() {
		return null;
	}
	
	public static void shutdown(Runnable runnable) {
		
	}
	
	public static void launchFrame(int width, int height, boolean fullscreen) {
		Canvas canvas = new Canvas();
		Applet applet = getAppletInstance();
		applet.setLayout(new BorderLayout());
		applet.add(canvas, "Center");
		Frame frame = new Frame("Minecraft");
		frame.setBackground(Color.BLACK);
		try {
			frame.setIconImage(ImageIO.read(Launch.class.getResource("/favicon.png")));
		} catch (Exception exception10) { }
		frame.setLayout(new BorderLayout());
		frame.add(applet, "Center");
		canvas.setPreferredSize(new Dimension(width, height));
		frame.pack();
		frame.setLocationRelativeTo(null);
		if(applet != null) {
			applet.setStub(new AppletWrapper());
		}
		final Runnable mc = getMinecraftInstance(frame, canvas, applet, width, height, fullscreen);
		if(mc == null) {
			System.err.println("Could not create Minecraft instance!");
			if(frame != null) frame.dispose();
			return;
		}
		final Thread mcThread = new Thread(mc, "Minecraft main thread");
		mcThread.setPriority(10);

		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent we) {
				shutdown(mc);
	
				try {
					mcThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		mcThread.start();
	}
}
