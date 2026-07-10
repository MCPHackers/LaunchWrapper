package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class GLFWWaylandUpgrade implements Injection {

	public String name() {
		return "GLFW Wayland Upgrade";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode glfwClass = source.getClass("org/lwjgl/glfw/GLFW");
		if (glfwClass != null) {
			MethodNode glfwDefaultHints = NodeHelper.getMethod(glfwClass, "glfwDefaultWindowHints", "()V");
			MethodNode glfwInit = NodeHelper.getMethod(glfwClass, "glfwInit", "()Z");
			FieldNode platformWayland = NodeHelper.getField(glfwClass, "GLFW_PLATFORM_WAYLAND", "I");
			if ("wayland".equals(System.getenv("XDG_SESSION_TYPE")) && glfwInit != null && platformWayland != null) {
				InsnList insert = new InsnList();
				insert.add(intInsn(0x50003)); // GLFW_PLATFORM
				insert.add(intInsn(0x60003)); // GLFW_PLATFORM_WAYLAND
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwInitHint", "(II)V"));
				glfwInit.instructions.insertBefore(glfwInit.instructions.getFirst(), insert);
			}
			if (glfwDefaultHints != null && LaunchConfig.WM_CLASS_NAME != null) {
				InsnList insert = new InsnList();
				insert.add(intInsn(155649)); // GLFW_WAYLAND_APP_ID
				insert.add(new LdcInsnNode(LaunchConfig.WM_CLASS_NAME));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHintString", "(ILjava/lang/CharSequence;)V"));
				insert.add(intInsn(147457)); // GLFW_X11_CLASS_NAME
				insert.add(new LdcInsnNode(LaunchConfig.WM_CLASS_NAME));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHintString", "(ILjava/lang/CharSequence;)V"));
				insert.add(intInsn(147458)); // GLFW_X11_INSTANCE_NAME
				insert.add(new LdcInsnNode(LaunchConfig.WM_CLASS_NAME));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHintString", "(ILjava/lang/CharSequence;)V"));
				glfwDefaultHints.instructions.insertBefore(glfwDefaultHints.instructions.getFirst(), insert);
			}
			source.overrideClass(glfwClass);
			return true;
		}
		return false;
	}
}
