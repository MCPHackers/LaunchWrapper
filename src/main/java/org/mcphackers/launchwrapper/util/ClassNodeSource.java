package org.mcphackers.launchwrapper.util;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeSource {

	ClassNode getClass(String name);

	void overrideClass(ClassNode node);

}
