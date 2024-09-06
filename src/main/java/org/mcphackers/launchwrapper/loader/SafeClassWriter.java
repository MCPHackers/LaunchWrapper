package org.mcphackers.launchwrapper.loader;

import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class SafeClassWriter extends ClassWriter {
	protected ClassNodeSource classLoader;
	
	public SafeClassWriter(ClassNodeSource classLoader, int flags) {
		super(flags);
		this.classLoader = classLoader;
	}
	
	protected ClassNode getClass(String name) {
		return classLoader.getClass(name);
	}
	
	protected String getCommonSuperClass(final String type1, final String type2) {
		ClassNode class1 = this.getClass(type1);
		ClassNode class2 = this.getClass(type2);
		if(class1 == null || class2 == null) {
			return "java/lang/Object";
		}
		return this.getCommonSuperClass(class1, class2).name;
	}

    public boolean canAssign(ClassNode superType, ClassNode subType) {
        final String name = superType.name;
        if ((superType.access & Opcodes.ACC_INTERFACE) != 0) {
            return isImplementingInterface(subType, name);
        } else {
            while (subType != null) {
                if (name.equals(subType.name) || name.equals(subType.superName)) {
                    return true;
                }
                if (subType.name.equals("java/lang/Object")) {
                    return false;
                }
                subType = getClass(subType.superName);
            }
        }
        return false;
    }

    public boolean isImplementingInterface(ClassNode clazz, String itf) {
        if (clazz.name.equals("java/lang/Object")) {
            return false;
        }
        ClassNode superClass = getClass(clazz.superName);
        for (String interfaceName : superClass.interfaces) {
            if (interfaceName.equals(itf)) {
                return true;
            } else {
                if (isImplementingInterface(getClass(interfaceName), itf)) {
                    return true;
                }
            }
        }
        if ((clazz.access & Opcodes.ACC_INTERFACE) != 0) {
            return false;
        }
        return isImplementingInterface(getClass(clazz.superName), itf);
    }

    public ClassNode getCommonSuperClass(ClassNode class1, ClassNode class2) {
        if (class1.name.equals("java/lang/Object")) {
            return class1;
        }
        if (class2.name.equals("java/lang/Object")) {
            return class2;
        }
        if (canAssign(class1, class2)) {
            return class1;
        }
        if (canAssign(class2, class1)) {
            return class2;
        }
        if ((class1.access & Opcodes.ACC_INTERFACE) != 0 || (class2.access & Opcodes.ACC_INTERFACE) != 0) {
            return getClass("java/lang/Object");
        }
        return getCommonSuperClass(class1, getClass(class2.superName));
    }
}
