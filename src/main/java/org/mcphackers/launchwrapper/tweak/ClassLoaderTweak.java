package org.mcphackers.launchwrapper.tweak;

import org.objectweb.asm.tree.ClassNode;

public abstract class ClassLoaderTweak {
	
	/**
	 * Called on every class node loaded by LaunchClassLoader
	 * @param node
	 * @return true if the given ClassNode was modified
	 */
	public abstract boolean tweakClass(ClassNode node);
}
