package org.mcphackers.launchwrapper.tweak.injection;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Common interface for Injections to get minecraft class and other version independent fields or methods
 */
public interface MinecraftGetter {
    ClassNode getMinecraft();
    
    MethodNode getRun();

    FieldNode getIsRunning();
    
    MethodNode getInit();
}
