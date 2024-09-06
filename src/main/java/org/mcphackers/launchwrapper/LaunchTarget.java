package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

/**
 * Different types of entry point (main method, applet, etc)
 */
public interface LaunchTarget {

	void launch(LaunchClassLoader classLoader);
}
