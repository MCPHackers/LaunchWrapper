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
        if("java/lang/Object".equals(name)) {
            return null;
        }
		ClassNode node = classLoader.getClass(name);
        if(node == null) {
            throw new TypeNotPresentException(name, new NullPointerException());
        }
        return node;
	}
	
	protected String getCommonSuperClass(final String type1, final String type2) {
		return this.getCommonSuperClass(this.getClass(type1), this.getClass(type2));
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
        if (clazz == null) {
            return false;
        }
        for (String interfaceName : clazz.interfaces) {
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

    public String getCommonSuperClass(ClassNode superType, ClassNode subType) {
		if(superType == null || subType == null) {
			return "java/lang/Object";
		}
        if (canAssign(superType, subType)) {
            return superType.name;
        }
        if (canAssign(subType, superType)) {
            return subType.name;
        }
        if ((superType.access & Opcodes.ACC_INTERFACE) != 0 || (subType.access & Opcodes.ACC_INTERFACE) != 0) {
            return "java/lang/Object";
        }
        return getCommonSuperClass(superType, getClass(subType.superName));
    }
}
