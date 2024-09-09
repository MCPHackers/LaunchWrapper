package org.mcphackers.launchwrapper.target;

import java.applet.Applet;
import java.awt.Image;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.AppletWrapper;

/**
 * Runs an applet
 * (For example net.minecraft.client.MinecraftApplet)
 */
public class AppletLaunchTarget implements LaunchTarget {

	private final String targetClass;
	private String title;
	private int width = 200, height = 200;
	private Image icon;

	public AppletLaunchTarget(String classTarget) {
		targetClass = classTarget;
		title = classTarget;
	}

	public AppletLaunchTarget setTitle(String title) {
		this.title = title;
		return this;
	}

	public AppletLaunchTarget setResolution(int w, int h) {
		width = w;
		height = h;
		return this;
	}

	public AppletLaunchTarget setIcon(Image img) {
		icon = img;
		return this;
	}

	public void launch(LaunchClassLoader classLoader) {
		Class<? extends Applet> appletClass;
		try {
			appletClass = classLoader.loadClass(targetClass).asSubclass(Applet.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		AppletWrapper.startApplet(appletClass, width, height, title, icon);
	}
}
