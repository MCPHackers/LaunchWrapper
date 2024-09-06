package org.mcphackers.launchwrapper.tweak.injection.legacy;

import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LegacyTweakContext {
	public ClassNode minecraft;
	public ClassNode minecraftApplet;
	/** Field that determines if Minecraft should exit */
	public FieldNode running;
	/** Frame width */
	public FieldNode width;
	/** Frame height */
	public FieldNode height;
	/** Working game directory */
	public FieldNode mcDir;
	/** Whenever the game runs in an applet */
	public FieldNode appletMode;
	/** Default width when toggling fullscreen */
	public FieldInsnNode defaultWidth;
	/** Default height when toggling fullscreen */
	public FieldInsnNode defaultHeight;
	public FieldInsnNode fullscreenField;
	/** Name (usually obfuscated) of the MouseHelper class */
	public String mouseHelperName;
	public boolean supportsResizing;
	/** public static main(String[]) */
	public MethodNode main;
	public MethodNode run;

	public boolean isClassic() {
		for(String s : LegacyTweak.MAIN_CLASSES) {
			if(s.equals("net/minecraft/client/Minecraft")) {
				continue;
			}
			if(s.equals(minecraft.name)) {
				return true;
			}
		}
		if(minecraftApplet != null) {
			for(String s : LegacyTweak.MAIN_APPLETS) {
				if(s.equals("net/minecraft/client/MinecraftApplet")) {
					continue;
				}
				if(s.equals(minecraftApplet.name)) {
					return true;
				}
			}
		}
		return false;
	}

	public MethodNode getInit() {
		for(AbstractInsnNode insn : run.instructions) {
			if(insn.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				if(invoke.owner.equals(minecraft.name)) {
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				} else {
					return run;
				}
			}
		}
		return run;
	}
}
