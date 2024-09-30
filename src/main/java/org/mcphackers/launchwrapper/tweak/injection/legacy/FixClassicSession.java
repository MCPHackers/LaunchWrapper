package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * WIP. In 0.24 ST - meant to fix level saving and skin
 * Hopefully in early infdev that's going to fix skin too
 */
public class FixClassicSession extends InjectionWithContext<MinecraftGetter> {

    public FixClassicSession(MinecraftGetter context) {
        super(context);
    }

    @Override
    public String name() {
        return "Fix Classic session";
    }

    @Override
    public boolean required() {
        return false;
    }

    private void fixMp(ClassNodeSource source) {

        ClassNode minecraft = context.getMinecraft();
        if(minecraft != null) {
            for(MethodNode m : minecraft.methods) {
                if(m.desc.equals("(Lcom/mojang/minecraft/level/Level;)V")) {
                    for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
                        if(compareInsn(insn, PUTFIELD, "com/mojang/minecraft/level/Level")
                        && ((FieldInsnNode)insn).name.startsWith("particleEngine")) {
                            InsnList inject = new InsnList();
                            LabelNode lbl = new LabelNode();
                            inject.add(new VarInsnNode(ALOAD, 1));
                            inject.add(new JumpInsnNode(IFNONNULL, lbl));
                            inject.add(new InsnNode(RETURN));
                            inject.add(lbl);
                            m.instructions.insert(inject);
                            // m.instructions.set(insn, new InsnNode(POP2));
                            break;
                        }
                    }
                    break;
                }
            }
        }
        //TODO don't depend on mojmaps
        ClassNode node = source.getClass("com/mojang/minecraft/level/Level");
        if(node != null) {
            for(FieldNode f : node.fields) {
                if((f.access & ACC_TRANSIENT) != 0) {
                    continue;
                }
                if(f.desc.codePointAt(0) != 'L') {
                    continue;
                }
                if(f.name.equals("player")) {
                    f.access |= ACC_TRANSIENT;
                    source.overrideClass(node);
                    continue;
                }
                ClassNode serializable = source.getClass(f.desc.substring(1, f.desc.length() - 1));
                if(serializable == null || serializable.interfaces.contains("java/io/Serializable")) {
                    continue;
                }
                f.access |= ACC_TRANSIENT;
                source.overrideClass(node);
            }
        }
        node = source.getClass("com/mojang/minecraft/mob/Mob");
        ClassNode player = source.getClass("com/mojang/minecraft/player/Player");
        ClassNode humanoid = source.getClass("com/mojang/minecraft/mob/HumanoidMob");
        if(node != null && player != null && humanoid != null) {
            FieldInsnNode modelGet = null;
            for(MethodNode m : node.methods) {
                Type[] params = Type.getArgumentTypes(m.desc);
                if(params.length != 2 || Type.getReturnType(m.desc) != Type.VOID_TYPE) {
                    continue;
                }
                if(params[0].getSort() != Type.OBJECT || params[1] != Type.FLOAT_TYPE) {
                    continue;
                }
                for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
                    AbstractInsnNode[] insns = fill(insn, 3);
                    if(compareInsn(insns[0], GETFIELD)
                    && compareInsn(insns[1], IFNONNULL)
                    && compareInsn(insns[2], RETURN)) {
                        modelGet = (FieldInsnNode)insn;
                    }
                }
            }
            String modelName = null;
            for(MethodNode m : player.methods) {
                if(!m.name.equals("<init>")) {
                    continue;
                }
                for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
                    AbstractInsnNode[] insns = fill(insn, 4);
                    if(compareInsn(insns[0], NEW)
                    && compareInsn(insns[1], DUP)
                    && compareInsn(insns[2], INVOKESPECIAL)
                    && compareInsn(insns[3], PUTFIELD, player.name, modelGet.name, modelGet.desc)) {
                        modelName = ((TypeInsnNode)insn).desc;
                    }
                }

            }
            if(modelName != null) {
                for(MethodNode m : humanoid.methods) {
                    if(!m.name.equals("<init>")) {
                        continue;
                    }
                    InsnList inject = new InsnList();
                    inject.add(new VarInsnNode(ALOAD, 0));
                    // inject.add(new LdcInsnNode("humanoid"));
                    inject.add(new TypeInsnNode(NEW, modelName));
                    inject.add(new InsnNode(DUP));
                    inject.add(new MethodInsnNode(INVOKESPECIAL, modelName, "<init>", "()V"));
                    inject.add(new FieldInsnNode(PUTFIELD, humanoid.name, modelGet.name, modelGet.desc));
                    m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), inject);
                }
                source.overrideClass(humanoid);
            }
        }
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        fixMp(source);

        ClassNode minecraft = context.getMinecraft();

        for(MethodNode m : minecraft.methods) {
            if(!m.name.equals("<init>")) {
                continue;
            }
            for(AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
                AbstractInsnNode[] insns = fill(insn, 2);
                if(compareInsn(insns[0], ACONST_NULL)
                && compareInsn(insns[1], PUTFIELD)) {
                    FieldInsnNode fieldPut = (FieldInsnNode)insns[1];
                    if(fieldPut.desc.codePointAt(0) != 'L') {
                        continue;
                    }
                    ClassNode session = source.getClass(fieldPut.desc.substring(1, fieldPut.desc.length() - 1));
                    if(session == null) {
                        continue;
                    }
                    boolean hasList = false;
                    for(FieldNode f : session.fields) {
                        if(f.desc.equals("Ljava/util/List;") && (f.access & ACC_STATIC) != 0) {
                            hasList = true;
                        }
                    }
                    if(!hasList) {
                        continue;
                    }
                    MethodNode m2 = NodeHelper.getMethod(session, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
                    if(m2 == null) {
                        continue;
                    }
                    InsnList inject = new InsnList();
                    inject.add(new TypeInsnNode(NEW, session.name));
                    inject.add(new InsnNode(DUP));
                    inject.add(new LdcInsnNode(config.username.get()));
                    inject.add(new LdcInsnNode(config.session.get()));
                    inject.add(new MethodInsnNode(INVOKESPECIAL, session.name, m2.name, m2.desc));
                    m.instructions.insertBefore(insns[1], inject);
                    m.instructions.remove(insns[0]);
                    source.overrideClass(context.getMinecraft());
                    return true;

                }
            }
        }
        return false;
    }
    
}
