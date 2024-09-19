package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.storage.LegacyTweakContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * WIP
 */
public class FixClassicSession extends InjectionWithContext<LegacyTweakContext> {

    public FixClassicSession(LegacyTweakContext context) {
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

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        for(MethodNode m : context.minecraft.methods) {
            if(m.name.equals("<init>")) {
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
                        for(MethodNode m2 : session.methods) {
                            if(m2.desc.equals("(Ljava/lang/String;Ljava/lang/String;)V") && m2.name.equals("<init>")) {
                                InsnList inject = new InsnList();
                                inject.add(new TypeInsnNode(NEW, session.name));
                                inject.add(new InsnNode(DUP));
                                inject.add(new LdcInsnNode(config.username.get()));
                                inject.add(new LdcInsnNode(config.session.get()));
                                inject.add(new MethodInsnNode(INVOKESPECIAL, session.name, m2.name, m2.desc));
                                m.instructions.insertBefore(insns[1], inject);
                                m.instructions.remove(insns[0]);
                                source.overrideClass(context.minecraft);
                                return true;
                            }
                        }

                    }
                }
            }
        }
        return false;
    }
    
}
