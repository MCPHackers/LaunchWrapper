package org.mcphackers.launchwrapper.inject;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import org.objectweb.asm.tree.*;

public class InsnHelper {

	public static AbstractInsnNode[] fill(AbstractInsnNode insn, int size) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = 0;
		while(insn != null && i < size) {
			arr[i] = insn;
			insn = nextInsn(insn);
			i++;
		}
		return arr;
	}

	public static AbstractInsnNode[] fillBackwards(AbstractInsnNode insn, int size) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = size - 1;
		while(insn != null && i >= 0) {
			arr[i] = insn;
			insn = previousInsn(insn);
			i--;
		}
		return arr;
	}

	public static InsnList copyInsns(InsnList insnList) {
		MethodNode mv = new MethodNode();
		insnList.accept(mv);
		return mv.instructions;
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
				if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				}
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

	public static LabelNode labelBefore(AbstractInsnNode current) {
		if(current != null) {
			current = current.getPrevious();
		}
		while(current != null && current.getType() == LINE) {
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

	public static AbstractInsnNode getSuper(AbstractInsnNode first) {
		AbstractInsnNode insn = first;
		while(insn != null) {
			AbstractInsnNode prev = insn.getPrevious();
			if(prev != null && prev.getOpcode() == ALOAD && ((VarInsnNode) prev).var == 0 && insn.getOpcode() == INVOKESPECIAL && ((MethodInsnNode) insn).name.equals("<init>")) {
				break;
			}
			insn = insn.getNext();
		}
		return insn;
	}

	public static AbstractInsnNode getLastReturn(AbstractInsnNode last) {
		AbstractInsnNode insn = last;
		while(insn != null) {
			if(isReturn(insn.getOpcode())) {
				break;
			}
			insn = insn.getPrevious();
		}
		return insn;
	}

	public static boolean isReturn(int opcode) {
		return opcode == RETURN || opcode == IRETURN || opcode == LRETURN || opcode == FRETURN || opcode == DRETURN || opcode == ARETURN;
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

	public static void tryCatchAdd(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, String exception) {
		tryCatchAdd(method, startInsn, endInsn, null, exception);
	}

	public static void tryCatchAdd(MethodNode method, AbstractInsnNode startInsn, AbstractInsnNode endInsn, InsnList handle, String exception) {
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
				lastFree = Math.max(lastFree, var.var + 1);
			}
			insn = insn.getNext();
		}
		return lastFree;
	}

	public static boolean compareInsn(AbstractInsnNode insn, int opcode, Object... compare) {
		if(insn == null) {
			return false;
		}
		boolean opcodeEqual = opcode == insn.getOpcode() || opcode == -1;
		if(!opcodeEqual)
			return false;
		if(compare.length == 0 && opcodeEqual) {
			return true;
		}
		boolean matches = true;
		switch(insn.getType()) {
			case INSN:
				InsnNode insn1 = (InsnNode) insn;
				// Nothing to compare
				return matches;
			case INT_INSN:
				IntInsnNode insn2 = (IntInsnNode) insn;
				if(compare.length > 0) {
					matches &= insn2.operand == (Integer) compare[0];
				}
				return matches;
			case VAR_INSN:
				VarInsnNode insn3 = (VarInsnNode) insn;
				if(compare.length > 0) {
					matches &= insn3.var == (Integer) compare[0];
				}
				return matches;
			case TYPE_INSN:
				TypeInsnNode insn4 = (TypeInsnNode) insn;
				if(compare.length > 0) {
					matches &= insn4.desc.equals(compare[0]);
				}
				return matches;
			case FIELD_INSN:
				FieldInsnNode insn5 = (FieldInsnNode) insn;
				if(compare.length > 0) {
					matches &= compare[0] == null || insn5.owner.equals(compare[0]);
				}
				if(compare.length > 1) {
					matches &= compare[1] == null || insn5.name.equals(compare[1]);
				}
				if(compare.length > 2) {
					matches &= compare[2] == null || insn5.desc.equals(compare[2]);
				}
				return matches;
			case METHOD_INSN:
				MethodInsnNode insn6 = (MethodInsnNode) insn;
				if(compare.length > 0) {
					matches &= compare[0] == null || insn6.owner.equals(compare[0]);
				}
				if(compare.length > 1) {
					matches &= compare[1] == null || insn6.name.equals(compare[1]);
				}
				if(compare.length > 2) {
					matches &= compare[2] == null || insn6.desc.equals(compare[2]);
				}
				return matches;
			case INVOKE_DYNAMIC_INSN:
				InvokeDynamicInsnNode insn7 = (InvokeDynamicInsnNode) insn;
				if(compare.length > 0) {
					matches &= compare[0] == null || insn7.bsm.equals(compare[0]); // TODO ?
				}
				if(compare.length > 1) {
					matches &= compare[1] == null || insn7.name.equals(compare[1]);
				}
				if(compare.length > 2) {
					matches &= compare[2] == null || insn7.desc.equals(compare[2]);
				}
				return matches;
			case JUMP_INSN:
				JumpInsnNode insn8 = (JumpInsnNode) insn;
				if(compare.length > 0) {
					LabelNode label = (LabelNode) compare[0];
					matches &= label == insn8.label;
				}
				return matches;
			case LABEL:
				LabelNode insn9 = (LabelNode) insn;
				// TODO
				return matches;
			case LDC_INSN:
				LdcInsnNode insn10 = (LdcInsnNode) insn;
				if(compare.length > 0) {
					Object o = compare[0];
					matches &= o == null && insn10.cst == null || o != null && o.equals(insn10.cst);
				}
				return matches;
			case IINC_INSN:
				IincInsnNode insn11 = (IincInsnNode) insn;
				if(compare.length > 0) {
					Integer i = (Integer) compare[0];
					matches &= i == null || insn11.var == i;
				}
				if(compare.length > 1) {
					Integer i = (Integer) compare[1];
					matches &= i == null || insn11.incr == i;
				}
				return matches;
			case TABLESWITCH_INSN:
				TableSwitchInsnNode insn12 = (TableSwitchInsnNode) insn;
				// TODO
				return matches;
			case LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode insn13 = (LookupSwitchInsnNode) insn;
				// TODO
				return matches;
			case MULTIANEWARRAY_INSN:
				MultiANewArrayInsnNode insn14 = (MultiANewArrayInsnNode) insn;
				if(compare.length > 0) {
					matches &= compare[0] == null || insn14.desc.equals(compare[0]);
				}
				if(compare.length > 1) {
					Integer i = (Integer) compare[0];
					matches &= i == null || insn14.dims == i;
				}
				return matches;
			case FRAME:
				FrameNode insn15 = (FrameNode) insn;
				// TODO
				return matches;
			case LINE:
				LineNumberNode insn16 = (LineNumberNode) insn;
				if(compare.length > 0) {
					matches &= insn16.line == (Integer) compare[0];
				}
				return matches;

		}
		return false;
	}

}
