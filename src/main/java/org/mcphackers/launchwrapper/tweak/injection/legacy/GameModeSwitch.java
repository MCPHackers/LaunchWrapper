package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Enabled when either --survival or --creative flags are specified.
 * These flags should force specified game mode. Primarily tested in Classic and Indev.
 * Other versions are untested and are not expected to work.
 */
public class GameModeSwitch extends InjectionWithContext<LegacyTweakContext> {

	public GameModeSwitch(LegacyTweakContext context) {
		super(context);
	}

	public String name() {
		return "Game Mode switch";
	}

	public boolean required() {
		return false;
	}

	protected void addConstructorIfMissing(ClassNodeSource source, ClassNode node, String desc) {
		if (NodeHelper.getMethod(node, "<init>", desc) != null) {
			return;
		}
		MethodNode mNode = new MethodNode(ACC_PUBLIC, "<init>", desc, null, null);
		InsnList l = mNode.instructions;
		l.add(new VarInsnNode(ALOAD, 0));
		l.add(new VarInsnNode(ALOAD, 1)); // Assumes descriptor is (LObject;)V
		l.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", desc));
		l.add(new InsnNode(RETURN));
		node.methods.add(mNode);
		source.overrideClass(node);
	}

	private MethodNode getWorldSetter() {
		ClassNode minecraft = context.getMinecraft();
		MethodNode setWorld = null;
		for (MethodNode m : minecraft.methods) {
			// Indev
			if (m.desc.equals("(IIII)V")) {
				AbstractInsnNode first = getFirst(m.instructions);
				while (first.getOpcode() == -1) {
					first = nextInsn(first);
				}
				AbstractInsnNode[] insns = fill(first, 3);
				if (compareInsn(insns[0], ALOAD, 0) &&
					compareInsn(insns[1], ACONST_NULL) &&
					compareInsn(insns[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode)insns[2];
					setWorld = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
			} else {
				// Infdev and Alpha
				if (Type.getReturnType(m.desc) != Type.VOID_TYPE) {
					continue;
				}
				Type[] args = Type.getArgumentTypes(m.desc);
				if (args.length != 1 || args[0].getSort() != Type.OBJECT) {
					continue;
				}
				AbstractInsnNode first = getFirst(m.instructions);
				while (first != null && first.getOpcode() == -1) {
					first = nextInsn(first);
				}
				AbstractInsnNode[] insns = fill(first, 4);
				if (compareInsn(insns[0], ALOAD, 0) &&
					(compareInsn(insns[1], ALOAD, 1) || compareInsn(insns[1], ACONST_NULL)) && // Null in heavily obfuscated versions
					compareInsn(insns[2], LDC, "") &&
					compareInsn(insns[3], INVOKESPECIAL, minecraft.name, null, null)) {
					setWorld = m;
				}
			}
		}
		return setWorld;
	}

	protected MethodNode getTick() {
		ClassNode minecraft = context.getMinecraft();
		MethodNode run = context.getRun();

		for (AbstractInsnNode insn = getFirst(run.instructions); insn != null; insn = nextInsn(insn)) {
			AbstractInsnNode[] insns = fill(insn, 6);
			if (compareInsn(insns[0], GETFIELD, null, null, "I") &&
				compareInsn(insns[1], ICONST_1) &&
				compareInsn(insns[2], IADD) &&
				compareInsn(insns[3], PUTFIELD, null, null, "I") &&
				compareInsn(insns[4], ALOAD, 0) && insns[5] != null && insns[5].getType() == AbstractInsnNode.METHOD_INSN &&
				compareInsn(insns[5], -1, minecraft.name, null, "()V")) {
				FieldInsnNode getField = (FieldInsnNode)insn;
				FieldInsnNode putField = (FieldInsnNode)insns[3];
				if (!getField.name.equals(putField.name)) {
					continue;
				}
				MethodInsnNode invoke = (MethodInsnNode)insns[5];
				return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
			}
		}
		return run;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if (!config.survival.get() && !config.creative.get()) {
			return false;
		}
		String creativeType = null;
		String survivalType = null;
		ClassNode minecraft = context.getMinecraft();
		ClassNode hud = null;
		MethodNode tick = getTick();
		for (AbstractInsnNode insn = getFirst(tick.instructions); insn != null; insn = nextInsn(insn)) {
			AbstractInsnNode[] insns = fill(insn, 3);
			if (compareInsn(insns[0], GETFIELD, null, null, "Ljava/util/List;") &&
				compareInsn(insns[1], INVOKEINTERFACE, "java/util/List", "size", "()I") && hud == null) {
				hud = source.getClass(((FieldInsnNode)insns[0]).owner);
			}
			if (compareInsn(insns[0], INSTANCEOF) &&
				compareInsn(insns[1], IFEQ) &&
				compareInsn(insns[2], INVOKESTATIC, "org/lwjgl/input/Keyboard", "getEventKey", "()I")) {
				creativeType = ((TypeInsnNode)insn).desc;
			}
		}
		if (hud != null) {
			for (MethodNode m : hud.methods) {
				if (!m.desc.equals("(FZII)V") && !m.desc.equals("(F)V")) {
					continue;
				}
				for (AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
					if (compareInsn(insn, INSTANCEOF)) {
						survivalType = ((TypeInsnNode)insn).desc;
						break;
					}
				}
				break;
			}
		}
		if (creativeType == null) {
			for (MethodNode m : minecraft.methods) {
				if (!m.desc.equals("(I)V")) {
					continue;
				}
				for (AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
					if (compareInsn(insn, INSTANCEOF)) {
						AbstractInsnNode[] insns = fill(insn, 5);
						if (compareInsn(insns[1], IFNE) &&
							compareInsn(insns[2], ALOAD, 0) &&
							compareInsn(insns[3], BIPUSH, 10) &&
							compareInsn(insns[4], PUTFIELD)) {
							creativeType = ((TypeInsnNode)insn).desc;
						}
						break;
					}
				}
				break;
			}
		}
		String type = config.creative.get() ? creativeType : survivalType;
		if (type == null) {
			return false;
		}

		for (MethodNode m : minecraft.methods) {
			if (!m.name.equals("<init>")) {
				continue;
			}
			for (AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
				AbstractInsnNode[] insns = fill(insn, 6);
				if (compareInsn(insns[0], ALOAD) &&
					compareInsn(insns[1], NEW) &&
					compareInsn(insns[2], DUP) &&
					compareInsn(insns[3], ALOAD) &&
					compareInsn(insns[4], INVOKESPECIAL, null, "<init>") &&
					compareInsn(insns[5], PUTFIELD)) {
					TypeInsnNode newType = (TypeInsnNode)insns[1];
					FieldInsnNode field = (FieldInsnNode)insns[5];
					MethodInsnNode init = (MethodInsnNode)insns[4];
					ClassNode currentMode = source.getClass(newType.desc);
					if (currentMode == null) {
						continue;
					}
					ClassNode s = source.getClass(type);
					if (s.superName.equals(field.desc.substring(1, field.desc.length() - 1))) {
						addConstructorIfMissing(source, s, init.desc);
						m.instructions.set(insns[1], new TypeInsnNode(NEW, type));
						m.instructions.set(insns[4], new MethodInsnNode(INVOKESPECIAL, type, "<init>", init.desc));
						source.overrideClass(minecraft);
						return true;
					}
				}
			}
		}
		MethodNode setWorld = getWorldSetter();
		if (setWorld == null) {
			return false;
		}
		for (FieldNode f : minecraft.fields) {
			if (f.desc.charAt(0) != 'L') {
				continue;
			}
			ClassNode s = source.getClass(type);
			if (s.superName.equals(f.desc.substring(1, f.desc.length() - 1))) {
				String desc = "(L" + minecraft.name + ";)V";
				addConstructorIfMissing(source, s, desc);
				InsnList inject = new InsnList();
				inject.add(new VarInsnNode(ALOAD, 0));
				inject.add(new TypeInsnNode(NEW, type));
				inject.add(new InsnNode(DUP));
				inject.add(new VarInsnNode(ALOAD, 0));
				inject.add(new MethodInsnNode(INVOKESPECIAL, type, "<init>", desc));
				inject.add(new FieldInsnNode(PUTFIELD, minecraft.name, f.name, f.desc));
				setWorld.instructions.insert(inject);
				return true;
			}
		}

		return false;
	}
}
