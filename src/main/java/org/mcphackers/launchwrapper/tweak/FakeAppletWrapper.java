package org.mcphackers.launchwrapper.tweak;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.mcphackers.launchwrapper.applet.Applet;
import org.mcphackers.launchwrapper.applet.AppletStub;

public class FakeAppletWrapper extends Applet implements AppletStub {
	private static final long serialVersionUID = 1L;

	private final Map<String, String> args;

	public FakeAppletWrapper(Map<String, String> argsAsMap) {
		args = argsAsMap;
	}

	public void appletResize(int width, int height) {
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public URL getDocumentBase() {
		try {
			return new URL("http://www.minecraft.net/game/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public URL getCodeBase() {
		try {
			return new URL("http://www.minecraft.net/game/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getParameter(String paramName) {
		return args.get(paramName);
	}

	public static void startApplet(Class<? extends Applet> appletClass, int width, int height, String title) {
		startApplet(appletClass, width, height, title, null);
	}

	public static void startApplet(Class<? extends Applet> appletClass, int width, int height, String title, Image icon) {
		try {
			final Applet applet = appletClass.newInstance();
			applet.setStub(new FakeAppletWrapper(Collections.<String, String>emptyMap()));
			final Frame frame = new Frame(title);
			frame.setBackground(Color.BLACK);
			frame.setIconImage(icon);
			frame.setLayout(new BorderLayout());
			frame.add(applet, "Center");
			frame.setPreferredSize(new Dimension(width, height));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent event) {
					applet.stop();
					frame.dispose();
				}
			});
			applet.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
