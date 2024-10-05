package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.IdentifyCall;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Brings back an unused error screen which was previously used to catch fatal
 * exceptions in Minecraft Classic and Indev
 * 
 * Tested to work from late indev to 1.12.2
 * 
 * Note that some crashes related to rendering may break Tesselator which will make it stuck in an exception loop
 */
public class ClassicCrashScreen extends InjectionWithContext<MinecraftGetter> {

	public ClassicCrashScreen(MinecraftGetter context) {
		super(context);
	}

	@Override
	public String name() {
		return "Classic crash screen";
	}

	@Override
	public boolean required() {
		return false;
	}

	public MethodNode getOpenScreen() {
		ClassNode minecraft = context.getMinecraft();

		for(MethodNode m : minecraft.methods) {
			if(!m.desc.equals("()V")) {
				continue;
			}
			AbstractInsnNode insn = m.instructions.getFirst();
			if(insn != null && insn.getOpcode() == -1) {
				insn = nextInsn(insn);
			}
			if(!compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "isActive", "()Z")) {
				continue;
			}
			AbstractInsnNode insn2 = insn;
			while(insn2 != null) {
				AbstractInsnNode[] insns2 = fill(insn2, 4);
				if(compareInsn(insns2[0], ALOAD, 0)
				&& compareInsn(insns2[1], ACONST_NULL)
				&& compareInsn(insns2[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode) insns2[2];
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
				if(compareInsn(insns2[0], ALOAD, 0)
				&& compareInsn(insns2[1], ACONST_NULL)
				&& compareInsn(insns2[2], CHECKCAST)
				&& compareInsn(insns2[3], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode) insns2[3];
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
				insn2 = nextInsn(insn2);
			}
		}
		return null;
	}

	@Override
	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		// TODO catch MinecraftServer crashes in 1.3+
		ClassNode minecraft = context.getMinecraft();
		MethodNode run = context.getRun();
		FieldNode running = context.getIsRunning();
		if(run == null || running == null) {
			return false;
		}
		AbstractInsnNode insn1 = run.instructions.getFirst();
		FieldInsnNode putWorld = null;
		LabelNode start = null;
		AbstractInsnNode end = null;
		TryCatchBlockNode afterCatch = null;
		MethodInsnNode cleanup = null;
		while(insn1 != null) {
			AbstractInsnNode[] insns1 = fill(insn1, 6);
			if(compareInsn(insns1[0], ALOAD, 0)
			&& compareInsn(insns1[1], GETFIELD, minecraft.name, running.name, running.desc)
			&& compareInsn(insns1[2], IFEQ)) {
				start = new LabelNode();
				run.instructions.insertBefore(insn1, start);
				JumpInsnNode jmp = (JumpInsnNode) insns1[2];
				end = previousInsn(jmp.label); // GOTO outside of loop
				for(TryCatchBlockNode tryCatch : run.tryCatchBlocks) {
					if(afterCatch == null && tryCatch.type != null && !tryCatch.type.startsWith("java/lang/")) {
						afterCatch = tryCatch;
					}
					AbstractInsnNode testInsn = tryCatch.handler;
					while(testInsn != null && cleanup == null && putWorld == null) {
						testInsn = nextInsn(testInsn);
						// earlier beta and alpha used setWorld(null) instead of cleanup
						if(putWorld == null
						&& compareInsn(testInsn, ACONST_NULL)
						&& compareInsn(nextInsn(testInsn), PUTFIELD)) {
							putWorld = (FieldInsnNode) nextInsn(testInsn);
						}
						// first invocation in catch OutOfMemoryError is cleanup()
						// Around 1.5 or above
						if("java/lang/OutOfMemoryError".equals(tryCatch.type)) {
							if(testInsn == null || cleanup != null) {
								continue;
							}
							AbstractInsnNode testInsn2 = nextInsn(testInsn);
							if(compareInsn(testInsn, ALOAD, 0)
							&& compareInsn(testInsn2, INVOKEVIRTUAL, minecraft.name, null, "()V")) {
								cleanup = (MethodInsnNode)testInsn2;
								continue;
							}
						}
					}
				}
			}
			insn1 = nextInsn(insn1);
		}
		if(start == null || end == null) {
			return false;
		}
		MethodNode setWorld = getWorldSetter(putWorld);

		ClassNode titleScreen = null;
		ClassNode errScreen = null;
		MethodNode openScreen = getOpenScreen();
		if(openScreen == null) {
			return false;
		}
		AbstractInsnNode instanceOf = null;
		
		for(AbstractInsnNode insn = openScreen.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
			AbstractInsnNode[] insns2 = fill(insn, 8);
			if(compareInsn(insns2[0], ALOAD, 1)
			&& compareInsn(insns2[1], IFNONNULL)
			&& compareInsn(insns2[2], ALOAD, 0)
			&& compareInsn(insns2[3], GETFIELD)
			&& compareInsn(insns2[4], IFNONNULL)
			&& compareInsn(insns2[5], NEW)
			&& compareInsn(insns2[6], DUP)
			&& compareInsn(insns2[7], INVOKESPECIAL)) {
				titleScreen = source.getClass(((TypeInsnNode)insns2[5]).desc);
			}
			if(compareInsn(insns2[0], INSTANCEOF)
			&& compareInsn(insns2[1], IFEQ)
			&& compareInsn(insns2[2], RETURN)) {
				errScreen = source.getClass(((TypeInsnNode)insn).desc);
				instanceOf = insn;
				break;
			}
		}

		// In indev and early infdev crash screen is already present, we just need to patch it to not exit game loop
		if(errScreen != null && setWorld != null) {
			boolean patched = false;
			for(TryCatchBlockNode tryCatch : run.tryCatchBlocks) {
				if(!"java/lang/Exception".equals(tryCatch.type)) {
					continue;
				}
				int var = -1;
				for(AbstractInsnNode insn = tryCatch.handler; insn != null; insn = nextInsn(insn)) {
					AbstractInsnNode[] insns = fill(insn, 5);
					if(compareInsn(insns[0], ASTORE) // exception
					&& compareInsn(insns[1], ALOAD, 0)
					&& compareInsn(insns[2], NEW, errScreen.name)
					&& compareInsn(insns[3], DUP)
					&& compareInsn(insns[4], LDC, "Client error")) {
						VarInsnNode store = (VarInsnNode)insn;
						var = store.var;
						InsnList inject = new InsnList();
						inject.add(new VarInsnNode(ALOAD, 0));
						inject.add(new InsnNode(ACONST_NULL));
						inject.add(new MethodInsnNode(INVOKESPECIAL, minecraft.name, setWorld.name, setWorld.desc));
						run.instructions.insertBefore(insns[1], inject);
					}
					if(var > 0
					&& compareInsn(insns[0], ALOAD, var)
					&& compareInsn(insns[1], INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V")
					&& compareInsn(insns[2], GOTO)) {
						JumpInsnNode gotoInsn = (JumpInsnNode)insns[2];
						gotoInsn.label = start;
						patched = true;
					}
				}
			}
			if(patched) {
				return true;
			}
		}
		for(MethodNode m : minecraft.methods) {
			if(!m.desc.equals("()V")) {
				continue;
			}
			AbstractInsnNode insn = m.instructions.getFirst();
			while(insn != null) {
				if(compareInsn(insn, LDC, "Manually triggered debug crash")) {
					FieldInsnNode crashTimer = null;
					AbstractInsnNode insn2 = insn;
					while(insn2 != null) {
						if(compareInsn(insn2, GETFIELD, minecraft.name, null, "J")) {
							crashTimer = (FieldInsnNode)insn2;
							break;
						}
						insn2 = previousInsn(insn2);
					}
					insn2 = insn;
					while(insn2 != null && crashTimer != null) {
						if(insn2.getOpcode() == ATHROW) {
							// Reset crash timer, because we can still re-enter a world, where it would trigger crash again
							InsnList inject = new InsnList();
							inject.add(new VarInsnNode(ALOAD, 0));
							inject.add(longInsn(-1));
							inject.add(new FieldInsnNode(PUTFIELD, crashTimer.owner, crashTimer.name, crashTimer.desc));
							m.instructions.insertBefore(insn2, inject);
							break;
						}
						insn2 = nextInsn(insn2);
					}
				}
				insn = nextInsn(insn);
			}
		}
		if(errScreen == null && titleScreen != null) {
			ClassNode worldSelectScreen = null;
			methods:
			for(MethodNode m : titleScreen.methods) {
				// buttonClicked(Button)V
				if(Type.getReturnType(m.desc) != Type.VOID_TYPE) {
					continue;
				}
				Type[] args = Type.getArgumentTypes(m.desc); 
				if(args.length != 1 || args[0].getSort() != Type.OBJECT) {
					continue;
				}

				for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
					AbstractInsnNode[] insns2 = fill(insn, 10);
					
					if(compareInsn(insns2[0], ALOAD, 1)
					&& compareInsn(insns2[1], GETFIELD, null, null, "I")
					&& compareInsn(insns2[2], ICONST_1)
					&& compareInsn(insns2[3], IF_ICMPNE) // if(button.id == 1)
					&& compareInsn(insns2[4], ALOAD, 0)
					&& compareInsn(insns2[5], GETFIELD) // this.minecraft
					&& compareInsn(insns2[6], NEW)
					&& compareInsn(insns2[7], DUP)
					&& compareInsn(insns2[8], ALOAD, 0)
					&& compareInsn(insns2[9], INVOKESPECIAL)) { // new WorldSelectScreen(this)
						worldSelectScreen = source.getClass(((TypeInsnNode)insns2[6]).desc);
						break methods;
					}
				}
			}
			if(worldSelectScreen != null) {
				methods:
				for(MethodNode m : worldSelectScreen.methods) {
					// init()V
					if(!m.desc.equals("()V")) {
						continue;
					}
					// new ErrorScreen("Unable to load words" ....
					for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
						if(compareInsn(insn, LDC, "Unable to load words") // Yes, words. Mojang made a typo
						|| compareInsn(insn, LDC, "Unable to load worlds")) {
							AbstractInsnNode[] insns = fillBackwards(insn, 3);
							if(compareInsn(insns[0], NEW)
							&& compareInsn(insns[1], DUP)) {
								errScreen = source.getClass(((TypeInsnNode)insns[0]).desc);
							}
							break methods;
						}
						if(compareInsn(insn, INVOKESPECIAL, worldSelectScreen.superName, m.name, m.desc)) {
							AbstractInsnNode[] insns = fill(insn, 4);
							if(compareInsn(insns[1], ALOAD, 0)
							&& compareInsn(insns[2], GETFIELD, worldSelectScreen.name)
							&& compareInsn(insns[3], INVOKEVIRTUAL, null, null, "()V")) {
								FieldInsnNode field = (FieldInsnNode)insns[2];
								ClassNode scrollArea = source.getClass(field.desc.substring(1, field.desc.length()-1));
								if(scrollArea == null) {
									break;
								}

								for(MethodNode m2 : scrollArea.methods) {
									for(AbstractInsnNode insn2 = m2.instructions.getFirst(); insn2 != null; insn2 = nextInsn(insn2)) {
										if(compareInsn(insn2, LDC, "selectWorld.unable_to_load")
										|| compareInsn(insn2, LDC, "Unable to load worlds")) {
											AbstractInsnNode[] insns2 = fillBackwards(insn2, 3);
											if(compareInsn(insns2[0], NEW)
											&& compareInsn(insns2[1], DUP)) {
												errScreen = source.getClass(((TypeInsnNode)insns2[0]).desc);
											}
											break methods;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		if(errScreen == null) {
			return false;
		}
		if(patchErrorScreen(source, errScreen, openScreen) && instanceOf != null) {
			openScreen.instructions.set(instanceOf, new InsnNode(ICONST_0));
		}
		if(setWorld == null && cleanup == null) {
			return false;
		} 

		int n = getFreeIndex(run.instructions);
		InsnList handle = new InsnList();
		handle.add(new VarInsnNode(ASTORE, n));
		if(cleanup != null) {
			handle.add(new VarInsnNode(ALOAD, 0));
			handle.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, cleanup.name, cleanup.desc));
		} else {
			// set world to null to crash without saving (bad idea)
			// handle.add(new VarInsnNode(ALOAD, 0));
			// handle.add(new InsnNode(ACONST_NULL));
			// handle.add(new FieldInsnNode(PUTFIELD, minecraft.name, putWorld.name, putWorld.desc));
			handle.add(new VarInsnNode(ALOAD, 0));
			handle.add(new InsnNode(ACONST_NULL));
			handle.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, setWorld.name, setWorld.desc));
		}
		handle.add(new VarInsnNode(ALOAD, n));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V"));
		handle.add(new VarInsnNode(ALOAD, 0));
		handle.add(new TypeInsnNode(NEW, errScreen.name));
		handle.add(new InsnNode(DUP));
		handle.add(new LdcInsnNode("Client error"));
		handle.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		handle.add(new InsnNode(DUP));
		handle.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
		handle.add(new LdcInsnNode("The game broke! ["));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new VarInsnNode(ALOAD, n));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new LdcInsnNode("]"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
		handle.add(new MethodInsnNode(INVOKESPECIAL, errScreen.name, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, openScreen.name, openScreen.desc));

		handle.add(new JumpInsnNode(GOTO, start));
		addTryCatch(run, start, end, handle, "java/lang/Exception", afterCatch);
		source.overrideClass(minecraft);
		return true;
	}

	private MethodNode getWorldSetter(FieldInsnNode putWorld) {
		ClassNode minecraft = context.getMinecraft();
		MethodNode setWorld = null;
		// If world field is known
		if(putWorld != null) {
			for(MethodNode m : minecraft.methods) {
				if(m.desc.equals("(" + putWorld.desc + ")V")) {
					setWorld = m;
					break;
				}
			}
			if(setWorld != null) {
				return setWorld;
			}
		}
		for(MethodNode m : minecraft.methods) {
			// Indev
			if(m.desc.equals("(IIII)V")) {
				AbstractInsnNode first = m.instructions.getFirst();
				while(first.getOpcode() == -1) {
					first = nextInsn(first);
				}
				AbstractInsnNode[] insns = fill(first, 3);
				if(compareInsn(insns[0], ALOAD, 0)
				&& compareInsn(insns[1], ACONST_NULL)
				&& compareInsn(insns[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode)insns[2];
					setWorld = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
			}
			// Infdev and Alpha
			else {
				if(Type.getReturnType(m.desc) != Type.VOID_TYPE) {
					continue;
				}
				Type[] args = Type.getArgumentTypes(m.desc);
				if(args.length != 1 || args[0].getSort() != Type.OBJECT) {
					continue;
				}
				AbstractInsnNode first = m.instructions.getFirst();
				while(first != null && first.getOpcode() == -1) {
					first = nextInsn(first);
				}
				AbstractInsnNode[] insns = fill(first, 4);
				if(compareInsn(insns[0], ALOAD, 0)
				&& compareInsn(insns[1], ALOAD, 1) || compareInsn(insns[1], ACONST_NULL) // Null in heavily obfuscated versions
				&& compareInsn(insns[2], LDC, "")
				&& compareInsn(insns[3], INVOKESPECIAL, minecraft.name, null, null)) {
					setWorld = m;
				}
			}
		}
		return setWorld;
	}

	public boolean patchErrorScreen(ClassNodeSource source, ClassNode errScreen, MethodNode openScreen) {
		boolean needCancelButton = true;
		String[] fields = {"message", "description"};
		MethodNode init = NodeHelper.getMethod(errScreen, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
		if(init == null) {
			MethodNode newInit = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", null, null);
			InsnList insnList = newInit.instructions;
			insnList.add(new VarInsnNode(ALOAD, 0));
			insnList.add(new MethodInsnNode(INVOKESPECIAL, errScreen.superName, "<init>", "()V"));
			int c = 1;
			if(errScreen.fields.isEmpty()) {
				errScreen.fields.add(new FieldNode(ACC_PRIVATE, fields[0], "Ljava/lang/String;", null, null));
				errScreen.fields.add(new FieldNode(ACC_PRIVATE, fields[1], "Ljava/lang/String;", null, null));
			}
			for(FieldNode f : errScreen.fields) {
				if(!f.desc.equals("Ljava/lang/String;")) {
					continue;
				}
				insnList.add(new VarInsnNode(ALOAD, 0));
				insnList.add(new VarInsnNode(ALOAD, c));
				insnList.add(new FieldInsnNode(PUTFIELD, errScreen.name, f.name, f.desc));
				if(c == 2) {
					break;
				}
				c++;
			}
			insnList.add(new InsnNode(RETURN));
			errScreen.methods.add(newInit);
		}
		// Restore text rendering in infdev
		// It was stripped out and replaced with null instead of those fields
		for(MethodNode m : errScreen.methods) {
			if(!m.desc.equals("(IIF)V")) {
				continue;
			}
			int i = 0;
			for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
				if(i == 2) {
					break;
				}
				if(insn.getOpcode() == INVOKESTATIC) {
					MethodInsnNode invoke = (MethodInsnNode)insn;
					if(Type.getArgumentCount(invoke.desc) != 5) {
						continue;
					}
					Type[] args = Type.getArgumentTypes(invoke.desc);
					if(args[1].getInternalName().equals("java/lang/String")
					&& args[2] == Type.INT_TYPE
					&& args[3] == Type.INT_TYPE
					&& args[4] == Type.INT_TYPE) {
						IdentifyCall call = new IdentifyCall(invoke);
						AbstractInsnNode[] arg1 = call.getArgument(1);
						if(arg1.length == 1 && arg1[0].getOpcode() == ACONST_NULL) {
							InsnList inject = new InsnList();
							inject.add(new VarInsnNode(ALOAD, 0));
							inject.add(new FieldInsnNode(GETFIELD, errScreen.name, fields[i], "Ljava/lang/String;"));
							m.instructions.insertBefore(arg1[0], inject);
							m.instructions.remove(arg1[0]);
							i++;
						}
					}
				}
			}
		}
		// Cancel button patch
		ClassNode screen = source.getClass(errScreen.superName);
		cancelButton:
		if(screen != null) {
			FieldNode mcField = null;
			FieldNode buttonsList = null;
			for(FieldNode f : screen.fields) {
				if(f.desc.equals("Ljava/util/List;") && buttonsList == null) {
					buttonsList = f;
				}
				if(f.desc.equals("L" + context.getMinecraft().name + ";")) {
					mcField = f;
				}
			}
			if(buttonsList == null || mcField == null) {
				break cancelButton;
			}
			String buttonType = null;
			for(MethodNode m : screen.methods) {
				for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
					AbstractInsnNode[] insns = fill(insn, 4); 

					if(compareInsn(insns[0], GETFIELD, screen.name, buttonsList.name, buttonsList.desc)
					&& compareInsn(insns[1], ILOAD)
					&& compareInsn(insns[2], INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;")
					&& compareInsn(insns[3], CHECKCAST)) {
						buttonType = ((TypeInsnNode)insns[3]).desc;
						break;
					}
				}
				if(buttonType != null) {
					break;
				}
			}
			MethodNode buttonClicked = null;
			for(MethodNode m : screen.methods) {
				if(!m.desc.equals("(L" + buttonType + ";)V")) {
					continue;
				}
				buttonClicked = m;
				break;
			}
			ClassNode button = source.getClass(buttonType);
			if(button != null) {
				if(NodeHelper.getMethod(button, "<init>", "(IIILjava/lang/String;)V") == null) {
					break cancelButton;
				}
			}
			FieldInsnNode width = null;
			for(MethodNode m : errScreen.methods) {
				if(!m.desc.equals("(II)V") && !m.desc.equals("(IIF)V")) {
					continue;
				}
				for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
					AbstractInsnNode[] insns = fill(insn, 6); 
					if(compareInsn(insns[0], ICONST_0)
					&& compareInsn(insns[1], ICONST_0)
					&& compareInsn(insns[2], ALOAD, 0)
					&& compareInsn(insns[3], GETFIELD, null, null, "I")
					&& compareInsn(insns[4], ALOAD, 0)
					&& compareInsn(insns[5], GETFIELD, null, null, "I")) {
						width = (FieldInsnNode)insns[3];
						break;
					}
				}
				if(width != null) {
					break;
				}
			}
			MethodNode initScreen = null;
			for(MethodNode m : errScreen.methods) {
				if(!m.desc.equals("()V") || m.name.equals("<init>")) {
					continue;
				}
				initScreen = m;
				break;
			}
			if(initScreen == null || width == null) {
				break cancelButton;
			}
			if(NodeHelper.getMethod(errScreen, buttonClicked.name, buttonClicked.desc) != null) {
				break cancelButton;
			}
			MethodNode buttonClickedNew = new MethodNode(buttonClicked.access, buttonClicked.name, buttonClicked.desc, null, null);
			errScreen.methods.add(buttonClickedNew);
			InsnList list = buttonClickedNew.instructions;
			list.add(new VarInsnNode(ALOAD, 0));
			list.add(new FieldInsnNode(GETFIELD, screen.name, mcField.name, mcField.desc));
			list.add(new InsnNode(ACONST_NULL));
			list.add(new MethodInsnNode(INVOKEVIRTUAL, context.getMinecraft().name, openScreen.name, openScreen.desc));
			list.add(new InsnNode(RETURN));
			
			list = new InsnList();
			list.add(new VarInsnNode(ALOAD, 0));
			list.add(new FieldInsnNode(GETFIELD, screen.name, buttonsList.name, buttonsList.desc));
			list.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "clear", "()V"));
			list.add(new VarInsnNode(ALOAD, 0));
			list.add(new FieldInsnNode(GETFIELD, screen.name, buttonsList.name, buttonsList.desc));
			list.add(new TypeInsnNode(NEW, buttonType));
			list.add(new InsnNode(DUP));
			list.add(new InsnNode(ICONST_0));
			list.add(new VarInsnNode(ALOAD, 0));
			list.add(new FieldInsnNode(GETFIELD, screen.name, width.name, width.desc));
			list.add(new InsnNode(ICONST_2));
			list.add(new InsnNode(IDIV));
			list.add(intInsn(100));
			list.add(new InsnNode(ISUB));
			list.add(intInsn(140));
			list.add(new LdcInsnNode("Cancel")); // TODO translate string?
			list.add(new MethodInsnNode(INVOKESPECIAL, buttonType, "<init>", "(IIILjava/lang/String;)V"));
			list.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z"));
			list.add(new InsnNode(POP));
			initScreen.instructions.insert(list);
		}
		source.overrideClass(errScreen);
		return needCancelButton;
	}

}
