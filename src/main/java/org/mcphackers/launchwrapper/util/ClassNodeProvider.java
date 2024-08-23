package org.mcphackers.launchwrapper.util;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeProvider {

	ClassNode getClass(String name);

}
