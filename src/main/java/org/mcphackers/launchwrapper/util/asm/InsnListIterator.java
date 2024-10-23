package org.mcphackers.launchwrapper.util.asm;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;

import java.util.Iterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public final class InsnListIterator implements Iterator<AbstractInsnNode>, Iterable<AbstractInsnNode> {
	AbstractInsnNode insn;

	public InsnListIterator(InsnList insnList) {
		insn = getFirst(insnList);
	}

	@Override
	public boolean hasNext() {
		return insn != null;
	}

	@Override
	public AbstractInsnNode next() {
		AbstractInsnNode current = insn;
		insn = nextInsn(insn);
		return current;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Unimplemented method 'remove'");
	}

	@Override
	public Iterator<AbstractInsnNode> iterator() {
		return this;
	}
}
