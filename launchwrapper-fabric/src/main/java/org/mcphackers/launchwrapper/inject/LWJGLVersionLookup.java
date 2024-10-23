package org.mcphackers.launchwrapper.inject;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;

import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class LWJGLVersionLookup {

	private static final String LWJGL2_VER_CLASS = "org/lwjgl/Sys";
	private static final String LWJGL2_VER_FIELD = "VERSION";
	private static final String LWJGL3_VER_CLASS = "org/lwjgl/Version";
	private static final String[] LWJGL3_VER_FIELDS = { "VERSION_MAJOR", "VERSION_MINOR", "VERSION_REVISION" };

	private static int getInsnValue(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
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
			String version;
			try (InputStream is = cp.getInputStream(LoaderUtil.getClassFileName(LWJGL2_VER_CLASS))) {
				version = getLWJGL2Version(NodeHelper.readClass(is, ClassReader.SKIP_FRAMES));
				if (version != null) {
					return version;
				}
			}
			try (InputStream is = cp.getInputStream(LoaderUtil.getClassFileName(LWJGL3_VER_CLASS))) {
				version = getLWJGL3Version(NodeHelper.readClass(is, ClassReader.SKIP_FRAMES));
				if (version != null) {
					return version;
				}
			}

		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}
		return "0.0.0";
	}

	private static String getLWJGL3Version(ClassNode node) {
		if (node == null) {
			return null;
		}
		MethodNode m = NodeHelper.getMethod(node, "<clinit>", "()V");
		if (m == null) {
			return null;
		}
		int[] version = new int[3];

		for (int i = 0; i < LWJGL3_VER_FIELDS.length; i++) {
			FieldNode f = NodeHelper.getField(node, LWJGL3_VER_FIELDS[i], "I");
			if (f != null && f.value != null) {
				version[i] = (int)f.value;
				continue;
			}
			AbstractInsnNode insn = getFirst(m.instructions);
			while (insn != null) {
				if (compareInsn(insn, PUTSTATIC, null, LWJGL3_VER_FIELDS[i])) {
					version[i] = getInsnValue(insn.getPrevious());
				}
				insn = nextInsn(insn);
			}
		}
		return version[0] + "." + version[1] + "." + version[2];
	}

	private static String getLWJGL2Version(ClassNode node) {
		if (node == null) {
			return null;
		}
		MethodNode m = NodeHelper.getMethod(node, "<clinit>", "()V");
		if (m == null) {
			return null;
		}

		FieldNode f = NodeHelper.getField(node, LWJGL2_VER_FIELD, "Ljava/lang/String;");
		if (f != null && f.value != null) {
			return (String)f.value;
		}
		AbstractInsnNode insn = getFirst(m.instructions);
		while (insn != null) {
			if (compareInsn(insn, PUTSTATIC, null, LWJGL2_VER_FIELD) &&
				compareInsn(previousInsn(insn), LDC)) {
				return (String)((LdcInsnNode)previousInsn(insn)).cst;
			}
			insn = nextInsn(insn);
		}
		return null;
	}
}
