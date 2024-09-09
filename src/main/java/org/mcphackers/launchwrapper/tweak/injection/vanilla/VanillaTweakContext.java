package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

public class VanillaTweakContext {
	public ClassNode minecraft;
    public List<String> availableParameters = new ArrayList<String>();
    public String[] args;
}
