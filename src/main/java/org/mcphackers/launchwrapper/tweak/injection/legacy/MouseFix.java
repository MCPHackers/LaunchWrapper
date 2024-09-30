package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Mouse deAWT. Replaces AWT mouse calls with org.lwjgl.input.Mouse calls
 */
public class MouseFix extends InjectionWithContext<LegacyTweakContext> {

    public MouseFix(LegacyTweakContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "Mouse fix";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        if(!config.lwjglFrame.get()) {
			return false;
		}
		ClassNode minecraft = context.getMinecraft();
        MethodNode init = context.getInit();

        String canvasName = null;
		int thisIndex = 0;
        String mouseHelperName = null;
        
		AbstractInsnNode insn1 = init.instructions.getFirst();
        while(insn1 != null) {
            AbstractInsnNode[] insns = fill(insn1, 5);
			// This bit required sanitization from LWJGLPatch
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
			) {
				thisIndex = ((VarInsnNode) insns[0]).var;
			}
            boolean found
             = compareInsn(insns[0], ALOAD, thisIndex)
            && compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
            && compareInsn(insns[2], INVOKESPECIAL, null, "<init>", "(Ljava/awt/Component;)V");

            if(found
            || compareInsn(insns[0], ALOAD, thisIndex)
            && compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
            && compareInsn(insns[2], ALOAD, thisIndex)
            && compareInsn(insns[3], GETFIELD, minecraft.name)
            && compareInsn(insns[4], INVOKESPECIAL, null, "<init>")) {
                mouseHelperName = found ? ((MethodInsnNode) insns[2]).owner : ((MethodInsnNode) insns[4]).owner;
            }
            insn1 = nextInsn(insn1);
        }
		if(mouseHelperName == null) {
			return false;
		}
		ClassNode mouseHelper = source.getClass(mouseHelperName);
		MethodNode setDelta = null;
		MethodNode setGrabbed = null;
		MethodNode setUngrabbed = null;
		String dx = null;
		String dy = null;
		method:
		for(MethodNode m : mouseHelper.methods) {
			if(!m.desc.equals("()V") || m.name.equals("<init>") || m.name.equals("<clinit>")) {
				continue;
			}
			AbstractInsnNode insn = m.instructions.getFirst();
			while(insn != null) {
				AbstractInsnNode[] insns = fill(insn, 4);
				if(compareInsn(insns[0], INVOKESTATIC, "java/awt/MouseInfo", "getPointerInfo", "()Ljava/awt/PointerInfo;")
				&& compareInsn(insns[1], INVOKEVIRTUAL, "java/awt/PointerInfo", "getLocation", "()Ljava/awt/Point;")) {
					setDelta = m;
					while(insn != null) {
						insns = fill(insn, 7);
						if(compareInsn(insns[0], ALOAD)
						&& compareInsn(insns[1], ALOAD)
						&& compareInsn(insns[2], GETFIELD, "java/awt/Point", null, "I")
						&& compareInsn(insns[3], ALOAD)
						&& compareInsn(insns[4], GETFIELD, mouseHelper.name, null, "I")
						&& compareInsn(insns[5], ISUB)
						&& compareInsn(insns[6], PUTFIELD, mouseHelper.name, null, "I")) {
							FieldInsnNode point = (FieldInsnNode) insns[2];
							FieldInsnNode putfield = (FieldInsnNode) insns[6];
							if(point.name.equals("x")) {
								dx = putfield.name;
							} else if(point.name.equals("y")) {
								dy = putfield.name;
							}
						}
						insn = nextInsn(insn);
					}
					continue method;
				}
				if(compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/input/Mouse", "setNativeCursor", "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;")) {
					if(compareInsn(insns[0], GETFIELD, mouseHelper.name, null, "Lorg/lwjgl/input/Cursor;")) {
						setGrabbed = m;
					} else if(compareInsn(insns[0], ACONST_NULL)) {
						setUngrabbed = m;
					}
					continue method;
				}
				insn = nextInsn(insn);
			}
		}
		if(dx != null && dy != null) {
			InsnList insns;
			if(setGrabbed != null) {
				setGrabbed.localVariables = null;
				setGrabbed.tryCatchBlocks.clear();
				insns = new InsnList();
				insns.add(new InsnNode(ICONST_1));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setGrabbed", "(Z)V"));
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new InsnNode(ICONST_0));
				insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dx, "I"));
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new InsnNode(ICONST_0));
				insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dy, "I"));
				insns.add(new InsnNode(RETURN));
				setGrabbed.instructions = insns;
			}
			if(setUngrabbed != null) {
				// inf-630
				setUngrabbed.localVariables = null;
				setUngrabbed.tryCatchBlocks.clear();
				insns = new InsnList();
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I"));
				insns.add(new InsnNode(ICONST_2));
				insns.add(new InsnNode(IDIV));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I"));
				insns.add(new InsnNode(ICONST_2));
				insns.add(new InsnNode(IDIV));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setCursorPosition", "(II)V"));
				insns.add(new InsnNode(ICONST_0));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setGrabbed", "(Z)V"));
				insns.add(new InsnNode(RETURN));
				setUngrabbed.instructions = insns;
			}

			if(setDelta != null) {
				setDelta.localVariables = null;
				setDelta.tryCatchBlocks.clear();
				insns = new InsnList();
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "getDX", "()I"));
				insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dx, "I"));
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "getDY", "()I"));
				insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dy, "I"));
				insns.add(new InsnNode(RETURN));
				setDelta.instructions = insns;
			}

			for(MethodNode m : minecraft.methods) {
				if(!m.desc.equals("()V") || m.tryCatchBlocks.isEmpty()) {
					continue;
				}
				for(TryCatchBlockNode tryCatch : m.tryCatchBlocks) {
					if(tryCatch.type != null && tryCatch.type.equals("org/lwjgl/LWJGLException")) {
						AbstractInsnNode[] insns2 = fill(nextInsn(tryCatch.start), 3);
						// setUngrabbed in inf-625 or earlier
						if(compareInsn(insns2[0], ACONST_NULL)
						&& compareInsn(insns2[1], INVOKESTATIC, "org/lwjgl/input/Mouse", "setNativeCursor", "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;")
						&& compareInsn(insns2[2], POP)) {
							insns = new InsnList();
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I"));
							insns.add(new InsnNode(ICONST_2));
							insns.add(new InsnNode(IDIV));
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I"));
							insns.add(new InsnNode(ICONST_2));
							insns.add(new InsnNode(IDIV));
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setCursorPosition", "(II)V"));
							insns.add(new InsnNode(ICONST_0));
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setGrabbed", "(Z)V"));
							m.instructions.insertBefore(insns2[0], insns);
							removeRange(m.instructions, insns2[0], insns2[2]);
							m.tryCatchBlocks.remove(tryCatch);
							break;
						}
					}
				}
			}
			for(FieldNode field : minecraft.fields) {
				if(field.desc.startsWith("L") && field.desc.endsWith(";")) {
					boolean success = false;
					ClassNode node = source.getClass(field.desc.substring(1, field.desc.length() - 1));
					if(node == null) {
						continue;
					}
					method:
					for(MethodNode m : node.methods) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while(insn != null) {
							// We need to patch these calls out to not discard mouse delta
							if(insn.getOpcode() == INVOKESTATIC) {
								AbstractInsnNode[] insns2 = fill(insn, 3);
								if((compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDX", "()I")
								|| compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDY", "()I"))
								&& compareInsn(insns2[1], POP)) {
									// Triggers in inf-625 and earlier
									m.instructions.remove(insns2[0]);
									m.instructions.remove(insns2[1]);
									insn = insns2[2];
									success = true;
								}
								if((compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDX", "()I")
								|| compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDY", "()I"))
								&& compareInsn(insns2[1], ICONST_0)
								&& compareInsn(insns2[2], IMUL)) {
									// inf-630. Uses multiply by 0 instead of POPing return value
									m.instructions.set(insns2[0], new InsnNode(ICONST_0));
									insn = insns2[2];
									success = true;
								}
							} else {
								AbstractInsnNode[] insns2 = fill(insn, 3);
								if(compareInsn(insns2[0], GETFIELD, minecraft.name, null, "L" + mouseHelper.name + ";")
								&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, dy, "I")
								&& compareInsn(insns2[2], ISUB)) {
									// Vertical mouse delta is reversed
									m.instructions.set(insns2[2], new InsnNode(IADD));
									success = true;
									break method;
								}
							}
							insn = nextInsn(insn);
						}
					}
					if(success) {
						source.overrideClass(node);
						break;
					}
				}
			}
		} else {
			for(MethodNode m : mouseHelper.methods) {
				AbstractInsnNode insn = m.instructions.getFirst();
				while(insn != null) {
					AbstractInsnNode[] insns = fill(insn, 4);

					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
					&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getParent", "()Ljava/awt/Container;")
					&& compareInsn(insns[3], IFNULL)) {
						LabelNode gotoLabel = ((JumpInsnNode) insns[3]).label;
						m.instructions.insertBefore(insns[0], new JumpInsnNode(GOTO, gotoLabel));
					}

					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
					&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getWidth", "()I")) {
						MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
						m.instructions.insert(insns[2], invoke);
						m.instructions.remove(insns[0]);
						m.instructions.remove(insns[1]);
						m.instructions.remove(insns[2]);
						insn = invoke;
					}
					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
					&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getHeight", "()I")) {
						MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
						m.instructions.insert(insns[2], invoke);
						m.instructions.remove(insns[0]);
						m.instructions.remove(insns[1]);
						m.instructions.remove(insns[2]);
						insn = invoke;
					}
					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
					&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getLocationOnScreen", "()Ljava/awt/Point;")) {
						InsnList insert = new InsnList();
						insert.add(new TypeInsnNode(NEW, "java/awt/Point"));
						insert.add(new InsnNode(DUP));
						insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getX", "()I"));
						insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getY", "()I"));
						insert.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Point", "<init>", "(II)V"));
						insn = insert.getLast();
						m.instructions.insert(insns[2], insert);
						m.instructions.remove(insns[0]);
						m.instructions.remove(insns[1]);
						m.instructions.remove(insns[2]);
					}
					insn = nextInsn(insn);
				}
			}
		}
		source.overrideClass(mouseHelper);
        return true;
    }

}
