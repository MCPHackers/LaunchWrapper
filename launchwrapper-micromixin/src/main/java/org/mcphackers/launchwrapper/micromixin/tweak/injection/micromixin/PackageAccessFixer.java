package org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin;

import org.mcphackers.launchwrapper.tweak.LazyTweaker;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class PackageAccessFixer implements LazyTweaker {

    @Override
    public boolean tweakClass(ClassNodeSource source, String name) {
        ClassNode node = source.getClass(name);
        if(node == null) {
            return false;
        }

        boolean changed = false;
        
        int oldAccess = node.access;
        node.access = getAccess(oldAccess);
        changed |= node.access != oldAccess;

        for(FieldNode f : node.fields) {
            oldAccess = f.access;
            f.access = getAccess(f.access);
            changed |= f.access != oldAccess;
        }
        for(MethodNode m : node.methods) {
            oldAccess = m.access;
            m.access = getAccess(m.access);
            changed |= m.access != oldAccess;
        }
        if(changed) {
            source.overrideClass(node);
        }
        return changed;
    }

    protected int getAccess(int access) {
        // ACC_PRIVATE, ACC_PROTECTED and ACC_PUBLIC are the first 3 bits
        if ((access & 0x7) != Opcodes.ACC_PRIVATE) {
			return (access & (~0x7)) | Opcodes.ACC_PUBLIC;
		} else {
			return access;
		}
    }
}
