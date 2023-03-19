package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public abstract class LaunchTarget {

	public abstract void launch(LaunchClassLoader classLoader);
}
