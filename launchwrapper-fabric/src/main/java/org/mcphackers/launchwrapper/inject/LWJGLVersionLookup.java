package org.mcphackers.launchwrapper.inject;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;

public final class LWJGLVersionLookup {

    private static final String LWJGL2_VER_CLASS = "org/lwjgl/Sys";
    private static final String LWJGL2_VER_FIELD = "VERSION";
    private static final String LWJGL3_VER_CLASS = "org/lwjgl/Version";
    private static final String[] LWJGL3_VER_FIELDS = {"VERSION_MAJOR", "VERSION_MINOR", "VERSION_REVISION"};

    private static int getInsnValue(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case ICONST_0:
                return 0;
            case ICONST_1:
                return 1;
            case ICONST_2:
                return 2;
            case ICONST_3:
                return 3;
            case ICONST_4:
                return 4;
            case ICONST_5:
                return 5;
            case BIPUSH:
                return ((IntInsnNode)insn).operand;
            case LDC:
                return (int)((LdcInsnNode)insn).cst;
        }
        return 0;
    }

    public static String getVersion(List<Path> lwjgl) {
        try (SimpleClassPath cp = new SimpleClassPath(lwjgl)) {
			ClassNode node;
            MethodNode m;
            for(String s : new String[] {LWJGL2_VER_CLASS, LWJGL3_VER_CLASS}) {
                int[] version = new int[3];
                try (InputStream is = cp.getInputStream(LoaderUtil.getClassFileName(s))) {
                    if(is == null) {
                        continue;
                    }
                    ClassReader reader = new ClassReader(is);
                    node = new ClassNode();
                    reader.accept(node, 0);
                    m = NodeHelper.getMethod(node, "<clinit>", "()V");
                    if(m == null) {
                        continue;
                    }
                    AbstractInsnNode insn = m.instructions.getFirst();
                    if(s == LWJGL2_VER_CLASS) {
                        FieldNode f = NodeHelper.getField(node, LWJGL2_VER_FIELD, "Ljava/lang/String;");
                        if(f.value != null) {
                            return (String)f.value;
                        }
                        while(insn != null) {
                            if(compareInsn(insn, PUTSTATIC, null, LWJGL2_VER_FIELD)
                            && compareInsn(insn.getPrevious(), LDC)) {
                                return (String)((LdcInsnNode)insn.getPrevious()).cst;
                            }
                            insn = nextInsn(insn);
                        }
                    }
                    if(s == LWJGL3_VER_CLASS) {
                        for(int i = 0; i < LWJGL3_VER_FIELDS.length; i++) {
                            FieldNode f = NodeHelper.getField(node, LWJGL3_VER_FIELDS[i], "I");
                            if(f.value != null) {
                                version[i] = (int)f.value;
                                continue;
                            }
                            while(insn != null) {
                                if(compareInsn(insn, PUTSTATIC, null, LWJGL3_VER_FIELDS[i])) {
                                    version[i] = getInsnValue(insn.getPrevious());
                                }
                            }
                            insn = nextInsn(insn);
                        }
                    }
                }
                if(s == LWJGL2_VER_CLASS) {
                    continue;
                }
                return version[0] + "." + version[1] + "." + version[2];
            }

		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}
        return "0.0.0";
    }

}
