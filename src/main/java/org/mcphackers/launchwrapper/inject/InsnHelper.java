package org.mcphackers.launchwrapper.inject;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import org.mcphackers.rdi.util.OPHelper;
import org.objectweb.asm.tree.*;

public class InsnHelper {

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
			if(OPHelper.isReturn(insn.getOpcode())) {
				break;
			}
			insn = insn.getPrevious();
		}
		return insn;
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
