package org.mcphackers.launchwrapper.util.asm;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class NodeHelper {

	private NodeHelper() {
	}

	public static FieldNode getField(ClassNode node, String name, String desc) {
		for (FieldNode field : node.fields) {
			if (field.name.equals(name) && field.desc.equals(desc)) {
				return field;
			}
		}
		return null;
	}

	public static MethodNode getMethod(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(desc)) {
				return method;
			}
		}
		return null;
	}

	public static List<MethodNode> getConstructors(ClassNode node) {
		List<MethodNode> constructors = new ArrayList<MethodNode>();
		for (MethodNode method : node.methods) {
			if (method.name.equals("<init>")) {
				constructors.add(method);
			}
		}
		return constructors;
	}

	public static ClassNode readClass(InputStream is, int options) throws IOException {
		if (is == null) {
			return null;
		}
		ClassReader reader = new ClassReader(is);
		ClassNode node = new ClassNode();
		reader.accept(node, options);
		return node;
	}

	public static ClassNode readClass(byte[] data, int options) throws IOException {
		if (data == null) {
			return null;
		}
		ClassReader reader = new ClassReader(data);
		ClassNode node = new ClassNode();
		reader.accept(node, options);
		return node;
	}

	public static boolean isAbstract(MethodNode method) {
		return (method.access & ACC_ABSTRACT) != 0;
	}

	public static boolean isStatic(MethodNode method) {
		return (method.access & ACC_STATIC) != 0;
	}

	public static boolean isStatic(FieldNode field) {
		return (field.access & ACC_STATIC) != 0;
	}
}
