package org.mcphackers.launchwrapper.inject;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class InjectUtils {

	public static enum Access {
		PRIVATE, DEFAULT, PROTECTED, PUBLIC;

		public static Access getFromBytecode(int acc) {
			if((acc & ACC_PRIVATE) == ACC_PRIVATE)
				return PRIVATE;
			if((acc & ACC_PROTECTED) == ACC_PROTECTED)
				return PROTECTED;
			if((acc & ACC_PUBLIC) == ACC_PUBLIC)
				return PUBLIC;
			return DEFAULT;
		}

		public int setAccess(int acc) {
			acc &= ~(ACC_PRIVATE | ACC_PROTECTED | ACC_PUBLIC);
			acc |= this == PRIVATE ? ACC_PRIVATE : 0;
			acc |= this == PROTECTED ? ACC_PROTECTED : 0;
			acc |= this == PUBLIC ? ACC_PUBLIC : 0;
			return acc;
		}
	}

	public static FieldNode getField(ClassNode node, String name, String desc) {
		for(FieldNode field : node.fields) {
			if(field.name.equals(name) && field.desc.equals(desc)) {
				return field;
			}
		}
		return null;
	}

	public static MethodNode getMethod(ClassNode node, String name, String desc) {
		for(MethodNode method : node.methods) {
			if(method.name.equals(name) && method.desc.equals(desc)) {
				return method;
			}
		}
		return null;
	}

	public static List<MethodNode> getConstructors(ClassNode node) {
		List<MethodNode> constructors = new ArrayList<MethodNode>();
		for(MethodNode method : node.methods) {
			if(method.name.equals("<init>")) {
				constructors.add(method);
			}
		}
		return constructors;
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
