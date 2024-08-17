package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class DevBetaTweak extends LegacyTweak {

	public DevBetaTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source, launch);
	}
	public boolean transform() {
		if(!super.transform()) {
            return false;
        }
        ClassNode titleScreen = source.getClass("net/minecraft/client/title/TitleScreen");
        if(titleScreen != null) {
            MethodNode init = NodeHelper.getMethod(titleScreen, "init", "()V");
            if(init != null) {
                AbstractInsnNode insn1 = init.instructions.getFirst();
                while(insn1 != null) {
                    AbstractInsnNode[] insns = fill(insn1, 4);
                    if(compareInsn(insns[0], ALOAD, 0)
                    && compareInsn(insns[1], GETFIELD, null, null, "L" + minecraft.name + ";")
                    && compareInsn(insns[2], NEW, "net/minecraft/client/gamemode/secret/SecretMode")
                    && compareInsn(insns[3], DUP)) {
                        tweakInfo("Dev build titlescreen patch");
                        init.instructions.insertBefore(insn1, new InsnNode(RETURN));
                        break;
                    }
                    insn1 = nextInsn(insn1);
                }
                source.overrideClass(titleScreen);
            }
        }
        boolean bindsAdded = false;
        ClassNode options = source.getClass("net/minecraft/client/Options");
        if(options != null) {
            String[] keys = {"drop", "inventory", "chat"}; 
            String[] fields = {"keyDrop", "keyInventory", "keyChat"};
            int[] codes = {16, 23, 20};
            MethodNode init = NodeHelper.getMethod(options, "<init>", "()V");
            MethodNode init2 = NodeHelper.getMethod(options, "<init>", "(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V");
            MethodNode[] initNodes = new MethodNode[]{init, init2};
            for(MethodNode current : initNodes) {
                if(current == null) {
                    break;
                }
                AbstractInsnNode insn1 = current.instructions.getFirst();
                AbstractInsnNode initBefore = null;
                AbstractInsnNode storeBefore = null;
                AbstractInsnNode arraySize = null;
                AbstractInsnNode fogKeyIndex = null;
                while(insn1 != null) {
                    AbstractInsnNode[] insns = fill(insn1, 5);
                    if(compareInsn(insns[0], ALOAD, 0)
                    && compareInsn(insns[1], BIPUSH, 7)
                    && compareInsn(insns[2], ANEWARRAY, "net/minecraft/client/KeyMapping")) {
                        initBefore = insns[0];
                        arraySize = insns[1];
                    }
                    if(arraySize != null
                    && compareInsn(insns[0], DUP)
                    && compareInsn(insns[1], BIPUSH, 6)
                    && compareInsn(insns[2], ALOAD, 0)
                    && compareInsn(insns[3], GETFIELD, null, "keyFog", "Lnet/minecraft/client/KeyMapping;")
                    && compareInsn(insns[4], AASTORE)) {
                        storeBefore = insns[0];
                        fogKeyIndex = insns[1];
                        break;
                    }
                    insn1 = nextInsn(insn1);
                }
                if(arraySize != null && fogKeyIndex != null && initBefore != null && storeBefore != null) {
                    init.instructions.set(fogKeyIndex, intInsn(9));
                    init.instructions.set(arraySize, intInsn(10));
                    InsnList insns = new InsnList();
                    InsnList insns2 = new InsnList();
                    int n = 6;
                    for(int i = 0; i < 3; i++) {
                        insns2.add(new IntInsnNode(ALOAD, 0));
                        insns2.add(new TypeInsnNode(NEW, "net/minecraft/client/KeyMapping"));
                        insns2.add(new InsnNode(DUP));
                        insns2.add(new LdcInsnNode("key." + keys[i]));
                        insns2.add(intInsn(codes[i]));
                        insns2.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/client/KeyMapping", "<init>", "(Ljava/lang/String;I)V"));
                        insns2.add(new FieldInsnNode(PUTFIELD, "net/minecraft/client/Options", fields[i], "Lnet/minecraft/client/KeyMapping;"));
                        
                        insns.add(new InsnNode(DUP));
                        insns.add(intInsn(n+i));
                        insns.add(new IntInsnNode(ALOAD, 0));
                        insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Options", fields[i], "Lnet/minecraft/client/KeyMapping;"));
                        insns.add(new InsnNode(AASTORE));
                    }
                    init.instructions.insertBefore(initBefore, insns2);
                    init.instructions.insertBefore(storeBefore, insns);
                    tweakInfo("Add options patch");
                    source.overrideClass(options);
                    bindsAdded = true;
                }
            }
            for(int i = 0; i < 3; i++) {
                options.fields.add(new FieldNode(ACC_PUBLIC, fields[i], "Lnet/minecraft/client/KeyMapping;", null, null));
            }
        }
        
        MethodNode runTick = NodeHelper.getMethod(minecraft, "tick", "()V");
        if(bindsAdded && runTick != null) {
            
            AbstractInsnNode insn1 = runTick.instructions.getFirst();
            while(insn1 != null) {
                AbstractInsnNode[] insns = fill(insn1, 8);
                if(compareInsn(insns[0], INVOKESTATIC, "org/lwjgl/input/Keyboard", "getEventKey", "()I")
                && compareInsn(insns[1], ICONST_1)
                && compareInsn(insns[2], IF_ICMPNE)
                && compareInsn(insns[3], ALOAD, 0)
                && compareInsn(insns[4], INVOKEVIRTUAL)) {
                    LabelNode label = new LabelNode();
                    InsnList list = new InsnList();
                    list.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Keyboard", "getEventKey", "()I"));
                    list.add(new IntInsnNode(ALOAD, 0));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "options", "Lnet/minecraft/client/Options;"));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Options", "keyInventory", "Lnet/minecraft/client/KeyMapping;"));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/KeyMapping", "key", "I"));
                    list.add(new JumpInsnNode(IF_ICMPNE, label));
                    list.add(new IntInsnNode(ALOAD, 0));
                    list.add(new TypeInsnNode(NEW, "net/minecraft/client/gui/inventory/InventoryScreen"));
                    list.add(new InsnNode(DUP));
                    list.add(new IntInsnNode(ALOAD, 0));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "player", "Lnet/minecraft/client/player/LocalPlayer;"));
                    list.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/client/gui/inventory/InventoryScreen", "<init>", "(Lnet/minecraft/world/entity/player/Player;)V"));
                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/client/Minecraft", "setScreen", "(Lnet/minecraft/client/gui/Screen;)V"));
                    list.add(label);
                    label = new LabelNode();
                    list.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Keyboard", "getEventKey", "()I"));
                    list.add(new IntInsnNode(ALOAD, 0));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "options", "Lnet/minecraft/client/Options;"));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Options", "keyDrop", "Lnet/minecraft/client/KeyMapping;"));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/KeyMapping", "key", "I"));
                    list.add(new JumpInsnNode(IF_ICMPNE, label));
                    list.add(new IntInsnNode(ALOAD, 0));
                    list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "player", "Lnet/minecraft/client/player/LocalPlayer;"));
                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/world/entity/player/Player", "drop", "()V"));
                    list.add(label);
                    tweakInfo("Restore inventory and drop");
                    runTick.instructions.insertBefore(insns[0], list);
                }
                insn1 = nextInsn(insn1);
            }
        }
        ClassNode container = source.getClass("net/minecraft/client/gui/inventory/AbstractContainerScreen");
        if(container != null) {
            MethodNode keyPressed = NodeHelper.getMethod(container, "keyPressed", "(CI)V");
            if(keyPressed != null) {
                InsnList insns = new InsnList();
                LabelNode label = new LabelNode();
                LabelNode label2 = new LabelNode();
                insns.add(new IntInsnNode(ILOAD, 2));
                insns.add(intInsn(1));
                insns.add(new JumpInsnNode(IF_ICMPNE, label));
                insns.add(new JumpInsnNode(GOTO, label2));
                insns.add(label);
                label = new LabelNode();
                insns.add(new IntInsnNode(ILOAD, 2));
                insns.add(new IntInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/gui/Screen", "minecraft", "Lnet/minecraft/client/Minecraft;"));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "options", "Lnet/minecraft/client/Options;"));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Options", "keyInventory", "Lnet/minecraft/client/KeyMapping;"));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/KeyMapping", "key", "I"));
                insns.add(new JumpInsnNode(IF_ICMPNE, label));
                insns.add(label2);
                insns.add(new IntInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/gui/Screen", "minecraft", "Lnet/minecraft/client/Minecraft;"));
                insns.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft", "player", "Lnet/minecraft/client/player/LocalPlayer;"));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/client/player/LocalPlayer", "closeContainer", "()V"));
                insns.add(label);
                insns.add(new InsnNode(RETURN));
                keyPressed.instructions = insns;
                source.overrideClass(container);
                tweakInfo("Fix container close");
            }

        }

		return true;
	}
    
}
