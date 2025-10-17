package org.mcphackers.launchwrapper.util.asm;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class InsnHelper {

	private InsnHelper() {
	}

	public static AbstractInsnNode[] fill(AbstractInsnNode insn, int size) {
		return fill(insn, size, false);
	}

	public static AbstractInsnNode[] fill(AbstractInsnNode insn, int size, boolean casts) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = 0;
		while (insn != null && i < size) {
			while (!casts && insn.getOpcode() == CHECKCAST) {
				insn = nextInsn(insn);
			}
			arr[i] = insn;
			insn = nextInsn(insn);
			i++;
		}
		return arr;
	}

	public static AbstractInsnNode[] fillBackwards(AbstractInsnNode insn, int size) {
		return fillBackwards(insn, size, false);
	}

	public static AbstractInsnNode[] fillBackwards(AbstractInsnNode insn, int size, boolean casts) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = size - 1;
		while (insn != null && i >= 0) {
			while (!casts && insn.getOpcode() == CHECKCAST) {
				insn = previousInsn(insn);
			}
			arr[i] = insn;
			insn = previousInsn(insn);
			i--;
		}
		return arr;
	}

	public static void removeRange(InsnList insns, AbstractInsnNode first, AbstractInsnNode last) {
		if (first == null || last == null) {
			return;
		}
		AbstractInsnNode next = first;
		while (next != null && next != last) {
			AbstractInsnNode forRemoval = next;
			next = next.getNext();
			insns.remove(forRemoval);
		}
		insns.remove(last);
	}

	public static void remove(InsnList insns, AbstractInsnNode... toRemove) {
		for (AbstractInsnNode insn : toRemove) {
			insns.remove(insn);
		}
	}

	public static AbstractInsnNode[] range(AbstractInsnNode start, AbstractInsnNode end) {
		List<AbstractInsnNode> list = new LinkedList<AbstractInsnNode>();
		AbstractInsnNode insn = start;
		while (insn != end) {
			list.add(insn);
			insn = insn.getNext();
		}
		list.add(end);
		AbstractInsnNode[] arr = new AbstractInsnNode[list.size()];
		return list.toArray(arr);
	}

	public static InsnList clone(Iterable<AbstractInsnNode> insnList) {
		Map<LabelNode, LabelNode> labels = new HashMap<LabelNode, LabelNode>();
		for (AbstractInsnNode insn : insnList) {
			if (insn.getType() == LABEL) {
				LabelNode label = (LabelNode)insn;
				labels.put(label, new LabelNode());
			}
		}

		InsnList destList = new InsnList();
		for (AbstractInsnNode insn : insnList) {
			AbstractInsnNode insnCopy = insn.clone(labels);
			destList.add(insnCopy);
		}
		return destList;
	}

	public static InsnList clone(AbstractInsnNode[] insnList) {
		return clone(Arrays.asList(insnList));
	}

	// Can cause frames to be invalid
	public static LabelNode labelBefore(AbstractInsnNode current) {
		current = current.getPrevious();
		while (current.getType() == LINE) { // Skip line number nodes
			current = current.getPrevious();
		}
		if (current.getType() == LABEL) {
			return (LabelNode)current;
		}
		return null;
	}

	public static AbstractInsnNode getFirst(InsnList insnList) {
		AbstractInsnNode current = insnList.getFirst();
		if (current != null && current.getOpcode() == -1) {
			return nextInsn(current);
		}
		return current;
	}

	public static AbstractInsnNode getLast(InsnList insnList) {
		AbstractInsnNode current = insnList.getLast();
		if (current != null && current.getOpcode() == -1) {
			return previousInsn(current);
		}
		return current;
	}

	public static AbstractInsnNode nextInsn(AbstractInsnNode current) {
		while ((current = current.getNext()) != null && current.getOpcode() == -1) {
		}
		return current;
	}

	public static AbstractInsnNode previousInsn(AbstractInsnNode current) {
		while ((current = current.getPrevious()) != null && current.getOpcode() == -1) {
		}
		return current;
	}

	public static InsnListIterator iterator(InsnList insns) {
		return new InsnListIterator(insns);
	}

	public static boolean containsInvoke(InsnList insns, MethodInsnNode invoke) {
		for (AbstractInsnNode insn : insns) {
			if (insn.getOpcode() == invoke.getOpcode()) {
				MethodInsnNode invoke2 = (MethodInsnNode)insn;
				if (invoke2.owner.equals(invoke.owner) && invoke2.name.equals(invoke.name) && invoke2.desc.equals(invoke.desc)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void addTryCatch(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, String exception) {
		addTryCatch(method, startInsn, endInsn, null, exception, null);
	}

	public static void addTryCatch(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, InsnList handle, String exception) {
		addTryCatch(method, startInsn, endInsn, handle, exception, null);
	}

	public static void addTryCatch(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, InsnList handle, String exception, TryCatchBlockNode afterBlock) {
		InsnList instructions = method.instructions;
		if (!instructions.contains(startInsn) || !instructions.contains(endInsn)) {
			throw new IllegalArgumentException("Instruction does not belong to the method");
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
		if (handle == null) {
			insert.add(new InsnNode(POP));
		} else {
			insert.add(handle);
		}
		insert.add(after);
		instructions.insert(endInsn, insert);
		if (afterBlock == null) {
			method.tryCatchBlocks.add(0, new TryCatchBlockNode(start, end, handler, exception));
		} else {
			method.tryCatchBlocks.add(method.tryCatchBlocks.indexOf(afterBlock) + 1, new TryCatchBlockNode(start, end, handler, exception));
		}
	}

	public static int getFreeIndex(InsnList instructions) {
		return getFreeIndex(instructions, null);
	}

	public static int getFreeIndex(InsnList instructions, AbstractInsnNode until) {
		int lastFree = 1;
		AbstractInsnNode insn = instructions.getFirst();
		while (insn != null) {
			if (insn == until) {
				break;
			}
			if (insn.getType() == VAR_INSN) {
				VarInsnNode var = (VarInsnNode)insn;
				lastFree = Math.max(lastFree, var.var + Math.abs(OPHelper.getStackSizeDelta(insn)));
			}
			insn = insn.getNext();
		}
		return lastFree;
	}

	public static AbstractInsnNode intInsn(int value) {
		switch (value) {
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
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					return new IntInsnNode(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				} else {
					return new LdcInsnNode(value);
				}
		}
	}

	public static AbstractInsnNode longInsn(long value) {
		if (value == 0L) {
			return new InsnNode(LCONST_0);
		} else if (value == 1L) {
			return new InsnNode(LCONST_1);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode booleanInsn(boolean value) {
		return new InsnNode(value ? ICONST_1 : ICONST_0);
	}

	public static AbstractInsnNode floatInsn(float value) {
		if (value == 0F) {
			return new InsnNode(FCONST_0);
		} else if (value == 1F) {
			return new InsnNode(FCONST_1);
		} else if (value == 2F) {
			return new InsnNode(FCONST_2);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode doubleInsn(double value) {
		if (value == 0D) {
			return new InsnNode(DCONST_0);
		} else if (value == 1D) {
			return new InsnNode(DCONST_1);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static int intValue(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
			case ICONST_M1:
				return -1;
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
			case SIPUSH:
				return ((IntInsnNode)insn).operand;
			case LDC:
				return (Integer)((LdcInsnNode)insn).cst;
			default:
				return 0;
		}
	}

	public static long longValue(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
			case LCONST_0:
				return 0L;
			case LCONST_1:
				return 1L;
			case LDC:
				return (Long)((LdcInsnNode)insn).cst;
			default:
				return 0L;
		}
	}

	public static boolean booleanValue(AbstractInsnNode insn) {
		return insn.getOpcode() == ICONST_1;
	}

	public static float floatValue(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
			case FCONST_0:
				return 0F;
			case FCONST_1:
				return 1F;
			case FCONST_2:
				return 2F;
			case LDC:
				return (Float)((LdcInsnNode)insn).cst;
			default:
				return 0F;
		}
	}

	public static double doubleValue(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
			case DCONST_0:
				return 0D;
			case DCONST_1:
				return 1D;
			case LDC:
				return (Double)((LdcInsnNode)insn).cst;
			default:
				return 0D;
		}
	}

	public static AbstractInsnNode getSuper(AbstractInsnNode first) {
		AbstractInsnNode insn = first;
		while (insn != null) {
			AbstractInsnNode prev = insn.getPrevious();
			if (prev != null && prev.getOpcode() == ALOAD && ((VarInsnNode)prev).var == 0 && insn.getOpcode() == INVOKESPECIAL && ((MethodInsnNode)insn).name.equals("<init>")) {
				break;
			}
			insn = insn.getNext();
		}
		return insn;
	}

	public static AbstractInsnNode getLastReturn(AbstractInsnNode last) {
		AbstractInsnNode insn = last;
		while (insn != null) {
			if (OPHelper.isReturn(insn.getOpcode())) {
				break;
			}
			insn = insn.getPrevious();
		}
		return insn;
	}

	@SuppressWarnings("unused")
	public static boolean compareInsn(AbstractInsnNode insn, int opcode, Object... compare) {
		if (insn == null) {
			return false;
		}
		boolean opcodeEqual = opcode == insn.getOpcode() || opcode == -1;
		if (!opcodeEqual) {
			return false;
		}
		if (compare.length == 0) {
			return true;
		}
		boolean matches = true;
		switch (insn.getType()) {
			case INSN:
				InsnNode insn1 = (InsnNode)insn;
				// Nothing to compare
				return matches;
			case INT_INSN:
				IntInsnNode insn2 = (IntInsnNode)insn;
				matches &= insn2.operand == (Integer)compare[0];
				return matches;
			case VAR_INSN:
				VarInsnNode insn3 = (VarInsnNode)insn;
				matches &= insn3.var == (Integer)compare[0];
				return matches;
			case TYPE_INSN:
				TypeInsnNode insn4 = (TypeInsnNode)insn;
				matches &= insn4.desc.equals(compare[0]);
				return matches;
			case FIELD_INSN:
				FieldInsnNode insn5 = (FieldInsnNode)insn;
				matches &= compare[0] == null || insn5.owner.equals(compare[0]);
				if (compare.length > 1) {
					matches &= compare[1] == null || insn5.name.equals(compare[1]);
				}
				if (compare.length > 2) {
					matches &= compare[2] == null || insn5.desc.equals(compare[2]);
				}
				return matches;
			case METHOD_INSN:
				MethodInsnNode insn6 = (MethodInsnNode)insn;
				matches &= compare[0] == null || insn6.owner.equals(compare[0]);
				if (compare.length > 1) {
					matches &= compare[1] == null || insn6.name.equals(compare[1]);
				}
				if (compare.length > 2) {
					matches &= compare[2] == null || insn6.desc.equals(compare[2]);
				}
				return matches;
			case INVOKE_DYNAMIC_INSN:
				InvokeDynamicInsnNode insn7 = (InvokeDynamicInsnNode)insn;
				matches &= compare[0] == null || insn7.bsm.equals(compare[0]);
				if (compare.length > 1) {
					matches &= compare[1] == null || insn7.name.equals(compare[1]);
				}
				if (compare.length > 2) {
					matches &= compare[2] == null || insn7.desc.equals(compare[2]);
				}
				return matches;
			case JUMP_INSN:
				JumpInsnNode insn8 = (JumpInsnNode)insn;
				if (compare.length > 0) {
					LabelNode label = (LabelNode)compare[0];
					matches &= label == insn8.label;
				}
				return matches;
			case LABEL:
				LabelNode insn9 = (LabelNode)insn;
				if (compare.length > 0) {
					LabelNode label = (LabelNode)compare[0];
					matches &= label == insn9;
				}
				return matches;
			case LDC_INSN:
				LdcInsnNode insn10 = (LdcInsnNode)insn;
				Object o = compare[0];
				matches &= o == null && insn10.cst == null || o != null && o.equals(insn10.cst);
				return matches;
			case IINC_INSN:
				IincInsnNode insn11 = (IincInsnNode)insn;
				if (compare.length > 0) {
					Integer i = (Integer)compare[0];
					matches &= i == null || insn11.var == i;
				}
				if (compare.length > 1) {
					Integer i = (Integer)compare[1];
					matches &= i == null || insn11.incr == i;
				}
				return matches;
			case TABLESWITCH_INSN:
				TableSwitchInsnNode insn12 = (TableSwitchInsnNode)insn;
				return matches;
			case LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode insn13 = (LookupSwitchInsnNode)insn;
				return matches;
			case MULTIANEWARRAY_INSN:
				MultiANewArrayInsnNode insn14 = (MultiANewArrayInsnNode)insn;
				if (compare.length > 0) {
					matches &= compare[0] == null || insn14.desc.equals(compare[0]);
				}
				if (compare.length > 1) {
					Integer i = (Integer)compare[0];
					matches &= i == null || insn14.dims == i;
				}
				return matches;
			case FRAME:
				return matches;
			case LINE:
				LineNumberNode insn16 = (LineNumberNode)insn;
				if (compare.length > 0) {
					matches &= insn16.line == (Integer)compare[0];
				}
				return matches;
		}
		return false;
	}
}
