package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.awt.Image;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.AppletWrapper;

public class AppletLaunchTarget extends LaunchTarget {

	private final String targetClass;
	private String title;
	private int width = 200, height = 200;
	private Image icon;

	public AppletLaunchTarget(LaunchClassLoader classLoader, String classTarget) {
		super(classLoader);
		targetClass = classTarget;
		title = classTarget;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setResolution(int w, int h) {
		width = w;
		height = h;
	}

	public void setIcon(Image img) {
		icon = img;
	}

	public void launch() {
		Class<? extends Applet> appletClass;
		try {
			appletClass = classLoader.findClass(targetClass).asSubclass(Applet.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		AppletWrapper.startApplet(appletClass, width, height, title, icon);
	}
}
