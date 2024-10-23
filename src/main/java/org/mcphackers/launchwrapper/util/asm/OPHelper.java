package org.mcphackers.launchwrapper.util.asm;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public final class OPHelper {
	private static final int[] STACK_SIZE_DELTA = new int[0xFF];

	private OPHelper() {
	}

	static {
		STACK_SIZE_DELTA[NOP] = 0;
		STACK_SIZE_DELTA[ACONST_NULL] = 1;
		STACK_SIZE_DELTA[ICONST_M1] = 1;
		STACK_SIZE_DELTA[ICONST_0] = 1;
		STACK_SIZE_DELTA[ICONST_1] = 1;
		STACK_SIZE_DELTA[ICONST_2] = 1;
		STACK_SIZE_DELTA[ICONST_3] = 1;
		STACK_SIZE_DELTA[ICONST_4] = 1;
		STACK_SIZE_DELTA[ICONST_5] = 1;
		STACK_SIZE_DELTA[LCONST_0] = 2;
		STACK_SIZE_DELTA[LCONST_1] = 1;
		STACK_SIZE_DELTA[FCONST_0] = 1;
		STACK_SIZE_DELTA[FCONST_1] = 1;
		STACK_SIZE_DELTA[FCONST_2] = 1;
		STACK_SIZE_DELTA[DCONST_0] = 2;
		STACK_SIZE_DELTA[DCONST_1] = 2;
		STACK_SIZE_DELTA[BIPUSH] = 1;
		STACK_SIZE_DELTA[SIPUSH] = 1;
		STACK_SIZE_DELTA[LDC] = 1; // special case
		STACK_SIZE_DELTA[ILOAD] = 1;
		STACK_SIZE_DELTA[LLOAD] = 2;
		STACK_SIZE_DELTA[FLOAD] = 1;
		STACK_SIZE_DELTA[DLOAD] = 2;
		STACK_SIZE_DELTA[ALOAD] = 1;
		STACK_SIZE_DELTA[IALOAD] = -1;
		STACK_SIZE_DELTA[LALOAD] = 0;
		STACK_SIZE_DELTA[FALOAD] = -1;
		STACK_SIZE_DELTA[DALOAD] = 0;
		STACK_SIZE_DELTA[AALOAD] = -1;
		STACK_SIZE_DELTA[BALOAD] = -1;
		STACK_SIZE_DELTA[CALOAD] = -1;
		STACK_SIZE_DELTA[SALOAD] = -1;
		STACK_SIZE_DELTA[ISTORE] = -1;
		STACK_SIZE_DELTA[LSTORE] = -2;
		STACK_SIZE_DELTA[FSTORE] = -1;
		STACK_SIZE_DELTA[DSTORE] = -2;
		STACK_SIZE_DELTA[ASTORE] = -1;
		STACK_SIZE_DELTA[IASTORE] = -3;
		STACK_SIZE_DELTA[LASTORE] = -4;
		STACK_SIZE_DELTA[FASTORE] = -3;
		STACK_SIZE_DELTA[DASTORE] = -4;
		STACK_SIZE_DELTA[AASTORE] = -3;
		STACK_SIZE_DELTA[BASTORE] = -3;
		STACK_SIZE_DELTA[CASTORE] = -3;
		STACK_SIZE_DELTA[SASTORE] = -3;
		STACK_SIZE_DELTA[POP] = -1;
		STACK_SIZE_DELTA[POP2] = -2;
		STACK_SIZE_DELTA[DUP] = 1;
		STACK_SIZE_DELTA[DUP_X1] = 1;
		STACK_SIZE_DELTA[DUP_X2] = 1;
		STACK_SIZE_DELTA[DUP2] = 2;
		STACK_SIZE_DELTA[DUP2_X1] = 2;
		STACK_SIZE_DELTA[DUP2_X2] = 2;
		STACK_SIZE_DELTA[SWAP] = 0;
		STACK_SIZE_DELTA[IADD] = -1;
		STACK_SIZE_DELTA[LADD] = -2;
		STACK_SIZE_DELTA[FADD] = -1;
		STACK_SIZE_DELTA[DADD] = -2;
		STACK_SIZE_DELTA[ISUB] = -1;
		STACK_SIZE_DELTA[LSUB] = -2;
		STACK_SIZE_DELTA[FSUB] = -1;
		STACK_SIZE_DELTA[DSUB] = -2;
		STACK_SIZE_DELTA[IMUL] = -1;
		STACK_SIZE_DELTA[LMUL] = -2;
		STACK_SIZE_DELTA[FMUL] = -1;
		STACK_SIZE_DELTA[DMUL] = -2;
		STACK_SIZE_DELTA[IDIV] = -1;
		STACK_SIZE_DELTA[LDIV] = -2;
		STACK_SIZE_DELTA[FDIV] = -1;
		STACK_SIZE_DELTA[DDIV] = -2;
		STACK_SIZE_DELTA[IREM] = -1;
		STACK_SIZE_DELTA[LREM] = -2;
		STACK_SIZE_DELTA[FREM] = -1;
		STACK_SIZE_DELTA[DREM] = -2;
		STACK_SIZE_DELTA[INEG] = 0;
		STACK_SIZE_DELTA[LNEG] = 0;
		STACK_SIZE_DELTA[FNEG] = 0;
		STACK_SIZE_DELTA[DNEG] = 0;
		STACK_SIZE_DELTA[ISHL] = -1;
		STACK_SIZE_DELTA[LSHL] = -1;
		STACK_SIZE_DELTA[ISHR] = -1;
		STACK_SIZE_DELTA[LSHR] = -1;
		STACK_SIZE_DELTA[IUSHR] = -1;
		STACK_SIZE_DELTA[LUSHR] = -1;
		STACK_SIZE_DELTA[IAND] = -1;
		STACK_SIZE_DELTA[LAND] = -2;
		STACK_SIZE_DELTA[IOR] = -1;
		STACK_SIZE_DELTA[LOR] = -2;
		STACK_SIZE_DELTA[IXOR] = -1;
		STACK_SIZE_DELTA[LXOR] = -2;
		STACK_SIZE_DELTA[IINC] = 0;
		STACK_SIZE_DELTA[I2L] = 1;
		STACK_SIZE_DELTA[I2F] = 0;
		STACK_SIZE_DELTA[I2D] = 1;
		STACK_SIZE_DELTA[L2I] = -1;
		STACK_SIZE_DELTA[L2F] = -1;
		STACK_SIZE_DELTA[L2D] = 0;
		STACK_SIZE_DELTA[F2I] = 0;
		STACK_SIZE_DELTA[F2L] = 1;
		STACK_SIZE_DELTA[F2D] = 1;
		STACK_SIZE_DELTA[D2I] = -1;
		STACK_SIZE_DELTA[D2L] = 0;
		STACK_SIZE_DELTA[D2F] = -1;
		STACK_SIZE_DELTA[I2B] = 0;
		STACK_SIZE_DELTA[I2C] = 0;
		STACK_SIZE_DELTA[I2S] = 0;
		STACK_SIZE_DELTA[LCMP] = -3;
		STACK_SIZE_DELTA[FCMPL] = -1;
		STACK_SIZE_DELTA[FCMPG] = -1;
		STACK_SIZE_DELTA[DCMPL] = -3;
		STACK_SIZE_DELTA[DCMPG] = -3;
		STACK_SIZE_DELTA[IFEQ] = -1;
		STACK_SIZE_DELTA[IFNE] = -1;
		STACK_SIZE_DELTA[IFLT] = -1;
		STACK_SIZE_DELTA[IFGE] = -1;
		STACK_SIZE_DELTA[IFGT] = -1;
		STACK_SIZE_DELTA[IFLE] = -1;
		STACK_SIZE_DELTA[IF_ICMPEQ] = -2;
		STACK_SIZE_DELTA[IF_ICMPNE] = -2;
		STACK_SIZE_DELTA[IF_ICMPLT] = -2;
		STACK_SIZE_DELTA[IF_ICMPGE] = -2;
		STACK_SIZE_DELTA[IF_ICMPGT] = -2;
		STACK_SIZE_DELTA[IF_ICMPLE] = -2;
		STACK_SIZE_DELTA[IF_ACMPEQ] = -2;
		STACK_SIZE_DELTA[IF_ACMPNE] = -2;
		STACK_SIZE_DELTA[GOTO] = 0;
		STACK_SIZE_DELTA[JSR] = 1;
		STACK_SIZE_DELTA[RET] = 0;
		STACK_SIZE_DELTA[TABLESWITCH] = -1;
		STACK_SIZE_DELTA[LOOKUPSWITCH] = -1;
		STACK_SIZE_DELTA[IRETURN] = -1;
		STACK_SIZE_DELTA[LRETURN] = -2;
		STACK_SIZE_DELTA[FRETURN] = -1;
		STACK_SIZE_DELTA[DRETURN] = -2;
		STACK_SIZE_DELTA[ARETURN] = -1;
		STACK_SIZE_DELTA[RETURN] = 0;
		STACK_SIZE_DELTA[GETSTATIC] = 0;	   // special case
		STACK_SIZE_DELTA[PUTSTATIC] = 0;	   // special case
		STACK_SIZE_DELTA[GETFIELD] = 0;		   // special case
		STACK_SIZE_DELTA[PUTFIELD] = 0;		   // special case
		STACK_SIZE_DELTA[INVOKEVIRTUAL] = 0;   // special case
		STACK_SIZE_DELTA[INVOKESPECIAL] = 0;   // special case
		STACK_SIZE_DELTA[INVOKESTATIC] = 0;	   // special case
		STACK_SIZE_DELTA[INVOKEINTERFACE] = 0; // special case
		STACK_SIZE_DELTA[INVOKEDYNAMIC] = 0;   // special case
		STACK_SIZE_DELTA[NEW] = 1;
		STACK_SIZE_DELTA[NEWARRAY] = 0;
		STACK_SIZE_DELTA[ANEWARRAY] = 0;
		STACK_SIZE_DELTA[ARRAYLENGTH] = 0;
		STACK_SIZE_DELTA[ATHROW] = 0; // special case
		STACK_SIZE_DELTA[CHECKCAST] = 0;
		STACK_SIZE_DELTA[INSTANCEOF] = 0;
		STACK_SIZE_DELTA[MONITORENTER] = -1;
		STACK_SIZE_DELTA[MONITOREXIT] = -1;
		STACK_SIZE_DELTA[MULTIANEWARRAY] = 0; // special case
		STACK_SIZE_DELTA[IFNULL] = -1;
		STACK_SIZE_DELTA[IFNONNULL] = -1;
	}

	public static int getStackSizeDelta(AbstractInsnNode insn) {
		int opcode = insn.getOpcode();
		if (opcode == -1) {
			return 0;
		}
		if (insn.getType() == AbstractInsnNode.LDC_INSN) {
			LdcInsnNode ldc = (LdcInsnNode)insn;
			if (ldc.cst instanceof Double || ldc.cst instanceof Long) {
				return 2;
			}
			return 1;
		}
		if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
			MethodInsnNode invoke = (MethodInsnNode)insn;
			int sizes = Type.getArgumentsAndReturnSizes(invoke.desc);
			int argumentSizes = sizes >> 2;
			if (opcode == INVOKESTATIC) {
				argumentSizes--;
			}
			int returnSize = sizes & 3;
			return returnSize - argumentSizes;
		}
		if (insn.getType() == AbstractInsnNode.FIELD_INSN) {
			FieldInsnNode field = (FieldInsnNode)insn;
			int returnSize = Type.getType(field.desc).getSize();
			if (opcode == GETSTATIC) {
				return returnSize;
			}
			return returnSize - 1;
		} else if (opcode != ATHROW && opcode != MULTIANEWARRAY && opcode != INVOKEDYNAMIC) {
			return STACK_SIZE_DELTA[opcode];
		}
		throw new IllegalArgumentException("Could not get stack size delta for " + opcode);
	}

	public static boolean isReturn(int opcode) {
		return opcode == RETURN ||
			opcode == IRETURN ||
			opcode == LRETURN ||
			opcode == FRETURN ||
			opcode == DRETURN ||
			opcode == ARETURN;
	}

	public static boolean isArrayLoad(int opcode) {
		switch (opcode) {
			case AALOAD:
			case BALOAD: // for booleans and bytes
			case CALOAD:
			case DALOAD:
			case FALOAD:
			case IALOAD:
			case LALOAD:
			case SALOAD:
				return true;
			default:
				return false;
		}
	}

	public static boolean isVarLoad(int opcode) {
		switch (opcode) {
			case ALOAD:
			// case BLOAD: // bytes & booleans are also regular integers
			// case CLOAD: // characters are regular integers (?) under the hood
			case DLOAD:
			case FLOAD:
			case ILOAD:
			case LLOAD:
				// case SLOAD: // and so are shorts
				return true;
			default:
				return false;
		}
	}

	public static int reverseVarOpcode(int opcode) {
		switch (opcode) {
			case AALOAD:
				return AASTORE;
			case BALOAD:
				return BASTORE;
			case CALOAD:
				return CASTORE;
			case IALOAD:
				return IASTORE;
			case SALOAD:
				return IASTORE;
			case DALOAD:
				return IASTORE;
			case FALOAD:
				return IASTORE;
			case LALOAD:
				return IASTORE;
			case ALOAD:
				return IASTORE;
			case DLOAD:
				return IASTORE;
			case FLOAD:
				return IASTORE;
			case ILOAD:
				return IASTORE;
			case LLOAD:
				return IASTORE;
			case AASTORE:
				return AALOAD;
			case BASTORE:
				return BALOAD;
			case CASTORE:
				return CALOAD;
			case IASTORE:
				return IALOAD;
			case SASTORE:
				return SALOAD;
			case DASTORE:
				return DALOAD;
			case FASTORE:
				return FALOAD;
			case LASTORE:
				return LALOAD;
			case ASTORE:
				return ALOAD;
			case DSTORE:
				return DLOAD;
			case FSTORE:
				return FLOAD;
			case ISTORE:
				return ILOAD;
			case LSTORE:
				return LLOAD;
			default:
				throw new IllegalArgumentException("Opcode not valid.");
		}
	}

	public static boolean isVarSimilarType(int opcode1, int opcode2) {
		switch (opcode1) {
			case AALOAD:
				return opcode2 == ALOAD;
			case BALOAD:
			case CALOAD:
			case IALOAD:
			case SALOAD:
				return opcode2 == ILOAD;
			case DALOAD:
				return opcode2 == DLOAD;
			case FALOAD:
				return opcode2 == FLOAD;
			case LALOAD:
				return opcode2 == LLOAD;
			case ALOAD:
				return opcode2 == AALOAD;
			case DLOAD:
				return opcode2 == DALOAD;
			case FLOAD:
				return opcode2 == FALOAD;
			case ILOAD:
				return opcode2 == IALOAD || opcode2 == SALOAD || opcode2 == BALOAD || opcode2 == CALOAD;
			case LLOAD:
				return opcode2 == LALOAD;
			// -- The same story for the store operation family --
			case AASTORE:
				return opcode2 == ASTORE;
			case BASTORE:
			case CASTORE:
			case IASTORE:
			case SASTORE:
				return opcode2 == ISTORE;
			case DASTORE:
				return opcode2 == DSTORE;
			case FASTORE:
				return opcode2 == FSTORE;
			case LASTORE:
				return opcode2 == LSTORE;
			case ASTORE:
				return opcode2 == ASTORE;
			case DSTORE:
				return opcode2 == DSTORE;
			case FSTORE:
				return opcode2 == FSTORE;
			case ISTORE:
				return opcode2 == IASTORE || opcode2 == SASTORE || opcode2 == BASTORE || opcode2 == CASTORE;
			case LSTORE:
				return opcode2 == LSTORE;
			default:
				throw new IllegalArgumentException("Opcode1 not valid.");
		}
	}

	public static boolean isVarStore(int opcode) {
		switch (opcode) {
			case ASTORE:
			// case BSTORE: // bytes & booleans are also regular integers
			// case CSTORE: // characters are regular integers (?) under the hood
			case DSTORE:
			case FSTORE:
			case ISTORE:
			case LSTORE:
				// case SSTORE: // and so are shorts
				return true;
			default:
				return false;
		}
	}
}
