package org.mcphackers.launchwrapper.micromixin.transformer;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.stianloader.micromixin.remapper.MemberLister;
import org.stianloader.remapper.HierarchyAwareMappingDelegator.TopLevelMemberLookup;
import org.stianloader.remapper.MemberRef;

public class MemberLookup implements MemberLister, TopLevelMemberLookup {
	protected ClassNodeSource source;
	public MemberLookup(ClassNodeSource source) {
		this.source = source;
	}

	@Override
	public boolean hasMemberInHierarchy(@NotNull String clazz, @NotNull String name, @NotNull String desc) {
		return getDefinitionNullable(new MemberRef(clazz, name, desc)) != null;
	}

	@Override
	@Nullable
	public Collection<MemberRef> getReportedClassMembers(@NotNull String owner) {
		ClassNode node = getClass(owner);
		if (node == null) {
			return Collections.emptySet();
		}
		List<MemberRef> refs = new ArrayList<>();
		for (MethodNode m : node.methods) {
			refs.add(new MemberRef(node.name, m.name, m.desc));
		}
		for (FieldNode f : node.fields) {
			refs.add(new MemberRef(node.name, f.name, f.desc));
		}
		return refs;
	}

	@Override
	public @NotNull Collection<MemberRef> tryInferMember(@NotNull String owner, @Nullable String name,
														 @Nullable String desc) {
		if (name == null || desc == null) {
			String descriptor = "L" + owner + ";" + (name == null ? "" : name) + (desc == null ? "" : desc);
			throw new UnsupportedOperationException("LaunchWrapper does not support infering members from non-fully qualified descriptor: " + descriptor);
		}
		MemberRef ref = getDefinition(new MemberRef(owner, name, desc));
		ClassNode node = getClass(ref.getOwner());
		if (node == null) {
			return Collections.emptySet();
		}
		List<MemberRef> refs = new ArrayList<>();
		MethodNode method = getMethod(node, name, desc);
		FieldNode field = method != null ? null : getField(node, name, desc);
		if (method != null || field != null) {
			refs.add(new MemberRef(owner, name, desc));
		}
		return refs;
	}

	private static String getPackage(String cls) {
		return cls.substring(cls.lastIndexOf('/') + 1);
	}

	protected static boolean isMethodMember(MemberRef ref) {
		return ref.getDesc().codePointAt(0) == '(';
	}

	public @Nullable MemberRef getDefinitionNullable(@NotNull MemberRef reference) {
		MemberRef ref;
		if (isMethodMember(reference)) {
			ref = getMethodDefinition(reference, null);
		} else {
			ref = getFieldDefinition(reference, null);
		}
		return ref;
	}

	@Override
	public @NotNull MemberRef getDefinition(@NotNull MemberRef reference) {
		MemberRef ref = getDefinitionNullable(reference);
		if (ref == null) {
			return reference;
		}
		return ref;
	}

	public @Nullable MemberRef getMethodDefinition(@NotNull MemberRef reference, @Nullable MemberRef childReference) {
		String owner = reference.getOwner();
		if (owner.equals("java/lang/Object")) {
			return null;
		}
		ClassNode current = getClass(owner);
		if (current == null) {
			return null;
		}
		MethodNode node = getMethod(current, reference.getName(), reference.getDesc());
		// Don't descend if our node is private or static
		if (node != null && ((node.access & ACC_PRIVATE) != 0 || (node.access & Opcodes.ACC_STATIC) != 0)) {
			return new MemberRef(current.name, node.name, node.desc);
		}
		// Descend all the way down
		MemberRef ref = getMethodDefinition(new MemberRef(current.superName, reference.getName(), reference.getDesc()), reference);
		if (ref != null) {
			return ref; // return found reference
		}
		// Descend all the way down, but for interfaces
		for (String ifs : current.interfaces) {
			ref = getMethodDefinition(new MemberRef(ifs, reference.getName(), reference.getDesc()), reference);
			if (ref != null) {
				return ref; // return found reference
			}
		}
		// we've hit a dead end. Checking for root reference
		if (node != null) {
			if ((node.access & ACC_PUBLIC) != 0 || (node.access & ACC_PROTECTED) != 0) {
				return new MemberRef(current.name, node.name, node.desc);
			}
			// package access
			if (childReference == null) {
				return null;
			}
			if (getPackage(childReference.getOwner()).equals(getPackage(owner))) {
				return new MemberRef(current.name, node.name, node.desc);
			}
		}
		// we found nothing
		return null;
	}

	public @Nullable MemberRef getFieldDefinition(@NotNull MemberRef reference, @Nullable MemberRef childReference) {
		String owner = reference.getOwner();
		if (owner.equals("java/lang/Object")) {
			return null;
		}
		ClassNode current = getClass(owner);
		if (current == null) {
			return null;
		}
		FieldNode node = getField(current, reference.getName(), reference.getDesc());
		// Don't descend if our node is private or static
		if (node != null && ((node.access & ACC_PRIVATE) != 0 || (node.access & Opcodes.ACC_STATIC) != 0)) {
			return new MemberRef(current.name, node.name, node.desc);
		}
		// Descend all the way down
		MemberRef ref = getFieldDefinition(new MemberRef(current.superName, reference.getName(), reference.getDesc()), reference);
		if (ref != null) {
			return ref; // return found reference
		}
		// we've hit a dead end. Checking for root reference
		if (node != null) {
			if ((node.access & ACC_PUBLIC) != 0 || (node.access & ACC_PROTECTED) != 0) {
				return new MemberRef(current.name, node.name, node.desc);
			}
			// package access
			if (childReference == null) {
				return null;
			}
			if (getPackage(childReference.getOwner()).equals(getPackage(owner))) {
				return new MemberRef(current.name, node.name, node.desc);
			}
		}
		// we found nothing, return and check if child has a matching reference
		return null;
	}

	protected ClassNode getClass(String name) {
		return source.getClass(name);
	}

	protected FieldNode getField(ClassNode owner, String name, String desc) {
		return NodeHelper.getField(owner, name, desc);
	}

	protected MethodNode getMethod(ClassNode owner, String name, String desc) {
		return NodeHelper.getMethod(owner, name, desc);
	}
}
