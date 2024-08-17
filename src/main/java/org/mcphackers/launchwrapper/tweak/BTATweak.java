package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.MethodNode;

public class BTATweak extends LegacyTweak {

	public BTATweak(ClassNodeSource source, LaunchConfig launch) {
		super(source, launch);
	}

    protected MethodNode getMain() { // Do not replace main
        return main;
    }
    
}
