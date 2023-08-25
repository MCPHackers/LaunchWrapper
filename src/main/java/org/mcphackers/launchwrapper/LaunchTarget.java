package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

/**
 * Different types of entry point (main method, applet, etc)
 */
public abstract class LaunchTarget {

	public abstract void launch(LaunchClassLoader classLoader);
}
