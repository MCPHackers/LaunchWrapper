package org.mcphackers.launchwrapper.tweak;

import org.objectweb.asm.tree.ClassNode;

public abstract class ClassLoaderTweak {
	
	public abstract boolean tweakClass(ClassNode node);
}
