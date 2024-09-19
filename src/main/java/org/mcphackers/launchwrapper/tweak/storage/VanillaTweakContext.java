package org.mcphackers.launchwrapper.tweak.storage;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class VanillaTweakContext implements MinecraftGetter {
	public ClassNode minecraft;
	public MethodNode run;
	public FieldNode running;
    public List<String> availableParameters = new ArrayList<String>();
    public String[] args;

    public ClassNode getMinecraft() {
        return minecraft;
    }

    public MethodNode getRun() {
        return run;
    }

    public FieldNode getIsRunning() {
        return running;
    }
}
