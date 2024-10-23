package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Fixes Z-fighting issues on certain graphics drivers by increasing bit depth to 24
 */
public class BitDepthFix extends InjectionWithContext<MinecraftGetter> {

	public BitDepthFix(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "BitDepthFix";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		MethodNode init = context.getInit();
		for (TryCatchBlockNode tryCatch : init.tryCatchBlocks) {
			if (!"org/lwjgl/LWJGLException".equals(tryCatch.type)) {
				continue;
			}
			AbstractInsnNode insn = tryCatch.end;
			while (insn != null && insn != tryCatch.start) {
				if (compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "(Lorg/lwjgl/opengl/PixelFormat;)V")) {
					return false;
				}
				if (compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "()V")) {
					InsnList insert = new InsnList();
					insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/PixelFormat"));
					insert.add(new InsnNode(DUP));
					insert.add(new MethodInsnNode(INVOKESPECIAL, "org/lwjgl/opengl/PixelFormat", "<init>", "()V"));
					insert.add(intInsn(24));
					insert.add(new MethodInsnNode(INVOKEVIRTUAL, "org/lwjgl/opengl/PixelFormat", "withDepthBits", "(I)Lorg/lwjgl/opengl/PixelFormat;"));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "(Lorg/lwjgl/opengl/PixelFormat;)V"));
					init.instructions.insert(insn, insert);
					init.instructions.remove(insn);
					return true;
				}
				insn = previousInsn(insn);
			}
		}
		return false;
	}
}
