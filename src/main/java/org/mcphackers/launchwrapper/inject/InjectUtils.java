package org.mcphackers.launchwrapper.inject;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class InjectUtils {
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
