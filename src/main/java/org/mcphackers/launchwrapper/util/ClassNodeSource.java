package org.mcphackers.launchwrapper.util;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeSource extends ClassNodeProvider {

	void overrideClass(ClassNode node);

}
