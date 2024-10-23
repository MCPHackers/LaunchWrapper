package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.util.ClassNodeSource;

public interface Tweaker {

	/**
	 * Called on every class node loaded by LaunchClassLoader
	 * @param name Name of ClassNode
	 * @return true if the given ClassNode was modified
	 */
	boolean tweakClass(ClassNodeSource source, String name);
}
