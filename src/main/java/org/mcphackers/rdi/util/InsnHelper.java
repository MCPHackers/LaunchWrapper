package org.mcphackers.rdi.util;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InsnHelper {

	public static AbstractInsnNode[] fill(AbstractInsnNode insn, int size) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = 0;
		while(insn != null && i < size) {
			arr[i] = insn;
			insn = insn.getNext();
			i++;
		}
		return arr;
	}

	public static void removeRange(InsnList insns, AbstractInsnNode first, AbstractInsnNode last) {
		if(first == null || last == null) {
			return;
		}
		AbstractInsnNode next = first;
		while(next != null && next != last) {
			AbstractInsnNode forRemoval = next;
			next = next.getNext();
			insns.remove(forRemoval);
		}
		insns.remove(last);
	}

	public static void remove(InsnList insns, AbstractInsnNode... toRemove) {
		for(AbstractInsnNode insn : toRemove) {
			insns.remove(insn);
		}
	}

	public static AbstractInsnNode[] range(AbstractInsnNode start, AbstractInsnNode end) {
		List<AbstractInsnNode> list = new LinkedList<AbstractInsnNode>();
		AbstractInsnNode insn = start;
		while(insn != end) {
			list.add(insn);
			insn = insn.getNext();
		}
		list.add(end);
		AbstractInsnNode[] arr = new AbstractInsnNode[list.size()];
		return list.toArray(arr);
	}

	public static InsnList clone(Iterable<AbstractInsnNode> insnList) {
		Map<LabelNode, LabelNode> labels = new HashMap<LabelNode, LabelNode>();
		for(AbstractInsnNode insn : insnList) {
			if(insn.getType() == LABEL) {
				LabelNode label = (LabelNode) insn;
				labels.put(label, new LabelNode());
			}
		}

		InsnList destList = new InsnList();
		for(AbstractInsnNode insn : insnList) {
			AbstractInsnNode insnCopy = insn.clone(labels);
			destList.add(insnCopy);
		}
		return destList;
	}

	public static InsnList clone(AbstractInsnNode[] insnList) {
		return clone(Arrays.asList(insnList));
	}

	public static LabelNode labelBefore(AbstractInsnNode current) {
		current = current.getPrevious();
		while(current.getType() == LINE) { // Skip line number nodes
			current = current.getPrevious();
		}
		if(current.getType() == LABEL) {
			return (LabelNode) current;
		}
		return null;
	}

	public static AbstractInsnNode nextInsn(AbstractInsnNode current) {
		while((current = current.getNext()) != null && current.getOpcode() == -1);
		return current;
	}

	public static AbstractInsnNode previousInsn(AbstractInsnNode current) {
		while((current = current.getPrevious()) != null && current.getOpcode() == -1);
		return current;
	}

	public static boolean containsInvoke(InsnList insns, MethodInsnNode invoke) {
		for(AbstractInsnNode insn : insns) {
			if(insn.getType() == METHOD_INSN) {
				MethodInsnNode invoke2 = (MethodInsnNode) insn;
				if(invoke2.getOpcode() == invoke.getOpcode() && invoke2.owner.equals(invoke.owner) && invoke2.name.equals(invoke.name) && invoke2.desc.equals(invoke.desc)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void addTryCatch(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, String exception) {
		addTryCatch(method, startInsn, endInsn, null, exception);
	}

	public static void addTryCatch(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, InsnList handle, String exception) {
		InsnList instructions = method.instructions;
		if(!instructions.contains(startInsn) || !instructions.contains(endInsn)) {
			throw new IllegalArgumentException("Instruction does not belong to the list");
		}
		LabelNode start = new LabelNode();
		LabelNode end = new LabelNode();
		LabelNode handler = new LabelNode();
		LabelNode after = new LabelNode();
		instructions.insertBefore(startInsn, start);
		InsnList insert = new InsnList();
		insert.add(end);
		insert.add(new JumpInsnNode(GOTO, after));
		insert.add(handler);
		if(handle == null) {
			insert.add(new InsnNode(POP));
		} else {
			insert.add(handle);
		}
		insert.add(after);
		instructions.insert(endInsn, insert);
		int index = 0; // FIXME Shouldn't be 0 if there are try/catch blocks within the range
		method.tryCatchBlocks.add(index, new TryCatchBlockNode(start, end, handler, exception));
	}

	public static int getFreeIndex(InsnList instructions) {
		int lastFree = 1;
		AbstractInsnNode insn = instructions.getFirst();
		while(insn != null) {
			if(insn.getType() == VAR_INSN) {
				VarInsnNode var = (VarInsnNode) insn;
				lastFree = Math.max(lastFree, var.var + 1); // FIXME 2 if it's a double or long?
			}
			insn = insn.getNext();
		}
		return lastFree;
	}

	public static AbstractInsnNode intInsn(int value) {
		switch(value) {
			case -1:
				return new InsnNode(ICONST_M1);
			case 0:
				return new InsnNode(ICONST_0);
			case 1:
				return new InsnNode(ICONST_1);
			case 2:
				return new InsnNode(ICONST_2);
			case 3:
				return new InsnNode(ICONST_3);
			case 4:
				return new InsnNode(ICONST_4);
			case 5:
				return new InsnNode(ICONST_5);
			default:
				if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					return new IntInsnNode(BIPUSH, value);
				} else if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				} else {
					return new LdcInsnNode(value);
				}
		}
	}

	public static AbstractInsnNode longInsn(long value) {
		if(value == 0L) {
			return new InsnNode(LCONST_0);
		} else if(value == 1L) {
			return new InsnNode(LCONST_1);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode booleanInsn(boolean value) {
		return new InsnNode(value ? ICONST_1 : ICONST_0);
	}

	public static AbstractInsnNode floatInsn(float value) {
		if(value == 0F) {
			return new InsnNode(FCONST_0);
		} else if(value == 1F) {
			return new InsnNode(FCONST_1);
		} else if(value == 2F) {
			return new InsnNode(FCONST_2);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode doubleInsn(double value) {
		if(value == 0D) {
			return new InsnNode(DCONST_0);
		} else if(value == 1D) {
			return new InsnNode(DCONST_1);
		} else {
			return new LdcInsnNode(value);
		}
	}

}
