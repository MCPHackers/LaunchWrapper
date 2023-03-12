package org.mcphackers.launchwrapper.inject;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public interface ClassNodeSource {

	ClassNode getClass(String name);

	FieldNode getField(String owner, String name, String desc);

	MethodNode getMethod(String owner, String name, String desc);

	void overrideClass(ClassNode node);

}
