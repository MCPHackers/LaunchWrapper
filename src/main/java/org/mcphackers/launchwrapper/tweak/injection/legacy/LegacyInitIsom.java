package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.IsomTweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LegacyInitIsom extends LegacyInit {

	public ClassNode isomApplet;
	public ClassNode isomCanvas;
	public FieldNode mcDirIsom;

    public LegacyInitIsom(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "IsomTweak init";
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        if(!super.apply(source, config)) {
            return false;
        }
        
		isomApplet = source.getClass(IsomTweak.MAIN_ISOM);
		if(isomApplet != null) {
			String desc = isomApplet.fields.get(0).desc;
			if(desc.startsWith("L") && desc.endsWith(";")) {
				isomCanvas = source.getClass(desc.substring(1, desc.length() - 1));
			}
		}
		if(isomCanvas != null) {
			for(FieldNode field : isomApplet.fields) {
				if(field.desc.equals("Ljava/io/File;")) {
					mcDirIsom = field;
				}
			}
		}
		if(mcDirIsom != null) {
			for(MethodNode m : isomCanvas.methods) {
				if(m.name.equals("<init>")) {
					InsnList insns = new InsnList();
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new TypeInsnNode(NEW, "java/io/File"));
					insns.add(new InsnNode(DUP));
					insns.add(new LdcInsnNode(config.gameDir.getString()));
					insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
					insns.add(new FieldInsnNode(PUTFIELD, isomCanvas.name, mcDirIsom.name, mcDirIsom.desc));
					m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
				}
			}
		}
		source.overrideClass(isomCanvas);
        return true;
    }
    
}
