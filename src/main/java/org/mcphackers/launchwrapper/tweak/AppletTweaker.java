package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AppletTweaker implements Tweaker {

	Map<String, String> renames = getRenames();
	Remapper remapper = new SimpleRemapper(Opcodes.ASM9, renames);

	private static Map<String, String> getRenames() {
		Map<String, String> renames = new HashMap<String, String>();
		renames.put("java/applet/Applet", "org/mcphackers/launchwrapper/applet/Applet");
		renames.put("java/applet/AppletStub", "org/mcphackers/launchwrapper/applet/AppletStub");
		return Collections.unmodifiableMap(renames);
	}

	public boolean tweakClass(ClassNodeSource source, String name) {
		ClassNode node = source.getClass(name);
		if (node == null) {
			return false;
		}
		boolean changed = false;
		// Decrease class version
		if (renames.get(node.superName) != null) {
			node.superName = renames.get(node.superName);
			changed = true;
		}
		List<String> newInterfaces = new ArrayList<String>();
		for (String itf : node.interfaces) {
			if (renames.get(itf) != null) {
				newInterfaces.add(renames.get(itf));
				changed = true;
			} else {
				newInterfaces.add(itf);
			}
		}
		node.interfaces = newInterfaces;

		for (MethodNode m : node.methods) {
			for (AbstractInsnNode insn : iterator(m.instructions)) {
				if (insn.getType() == METHOD_INSN) {
					MethodInsnNode mInsn = (MethodInsnNode)insn;
					if (renames.get(mInsn.owner) != null) {
						mInsn.owner = renames.get(mInsn.owner);
						changed = true;
					}
					String desc = remapper.mapMethodDesc(mInsn.desc);
					mInsn.desc = desc;
					changed |= !desc.equals(mInsn.desc);
				} else if (insn.getType() == FIELD_INSN) {
					FieldInsnNode fInsn = (FieldInsnNode)insn;
					if (renames.get(fInsn.owner) != null) {
						fInsn.owner = renames.get(fInsn.owner);
						changed = true;
					}
					String desc = remapper.mapDesc(fInsn.desc);
					fInsn.desc = desc;
					changed |= !desc.equals(fInsn.desc);
				}
			}
			String desc = remapper.mapDesc(m.desc);
			m.desc = desc;
			changed |= !desc.equals(m.desc);
		}
		for (FieldNode f : node.fields) {
			String desc = remapper.mapDesc(f.desc);
			f.desc = desc;
			changed |= !desc.equals(f.desc);
		}
		if (changed) {
			source.overrideClass(node);
		}
		return changed;
	}
}
