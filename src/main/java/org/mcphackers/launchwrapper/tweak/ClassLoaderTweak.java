package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.util.ClassNodeSource;

public interface ClassLoaderTweak {
	
	/**
	 * Called on every class node loaded by LaunchClassLoader
	 * @param node
	 * @return true if the given ClassNode was modified
	 */
	boolean tweakClass(ClassNodeSource source, String name);
}
