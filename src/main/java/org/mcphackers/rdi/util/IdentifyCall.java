package org.mcphackers.rdi.util;

import static org.mcphackers.rdi.util.InsnHelper.range;
import static org.mcphackers.rdi.util.InsnHelper.remove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

public class IdentifyCall {
	protected MethodInsnNode invokeInsn;
	protected List<AbstractInsnNode[]> arguments;
	
	public IdentifyCall(MethodInsnNode invoke) {
		invokeInsn = invoke;
		arguments = initArguments();
	}
	
	private List<AbstractInsnNode[]> initArguments() {
		boolean isStatic = invokeInsn.getOpcode() == Opcodes.INVOKESTATIC;
		Type[] argTypes = Type.getArgumentTypes(invokeInsn.desc);
		List<AbstractInsnNode[]> args = new ArrayList<AbstractInsnNode[]>(argTypes.length);
		if(argTypes.length == 0) {
			return Collections.emptyList();
		}
		AbstractInsnNode insn = invokeInsn.getPrevious();
		AbstractInsnNode start = null;
		AbstractInsnNode end = insn;
		for(int currentArg = argTypes.length - 1; currentArg >= 0; currentArg--) {
			int stack = argTypes[currentArg].getSize();
			end = insn;
			while(stack > 0) {
				stack -= OPHelper.getStackSizeDelta(insn);
				if(stack == 0) {
					start = insn;
				}
				insn = insn.getPrevious();
			}
			if(start != null) {
				while(args.size() <= currentArg) {
					args.add(null);
				}
				args.set(currentArg, range(start, end));
			}
		}
		if(!isStatic) {
			int stack = 1;
			end = insn;
			while(stack > 0) {
				stack -= OPHelper.getStackSizeDelta(insn);
				if(stack == 0) {
					start = insn;
				}
				insn = insn.getPrevious();
			}
			args.add(0, range(start, end));
		}
		return args;
	}
	
	/**
	 * Returns a collection of insn node arrays with each argument
	 * Non-static methods include the owner of a call at index 0
	 * @return
	 */
	public List<AbstractInsnNode[]> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	/**
	 * Returns an array of insn nodes belonging to the argument at that index
	 * Non-static methods include the owner of a call at index 0
	 * @return
	 */
	public AbstractInsnNode[] getArgument(int index) {
		return arguments.get(index);
	}

	public static void removeCall(InsnList insns, MethodInsnNode invoke) {
		for(AbstractInsnNode[] arg : new IdentifyCall(invoke).getArguments()) {
			remove(insns, arg);
		}
		insns.remove(invoke);
	}
}