package org.mcphackers.launchwrapper.micromixin.transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;

import org.stianloader.remapper.MappingLookup;
import org.stianloader.remapper.MemberRef;
import org.stianloader.remapper.Remapper;

public class AccessWidenerImpl implements AccessWidenerVisitor {
	final List<ClassAccess> classAccess = new ArrayList<>();
	final List<MemberAccess> methodAccess = new ArrayList<>();
	final List<MemberAccess> fieldAccess = new ArrayList<>();
	// Contains the class-names that are affected by loaded wideners.
	// Names are period-separated binary names (i.e. a.b.C).
	final Set<String> targets = new HashSet<>();
	public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		classAccess.add(new ClassAccess(name, access));
		targets.add(name);
	}

	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		methodAccess.add(new MemberAccess(new MemberRef(owner, name, descriptor), access));
		targets.add(owner);
	}

	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		fieldAccess.add(new MemberAccess(new MemberRef(owner, name, descriptor), access));
		targets.add(owner);
	}

	public AccessWidener getRemappedAccessWidener(MappingLookup mappings) {
		AccessWidener aw = new AccessWidener();
		for (ClassAccess entry : classAccess) {
			aw.visitClass(mappings.getRemappedClassName(entry.clazz), entry.access, false);
		}
		StringBuilder sb = new StringBuilder();
		for (MemberAccess entry : methodAccess) {
			MemberRef ref = entry.member;
			String owner = mappings.getRemappedClassName(ref.getOwner());
			String name = mappings.getRemappedMethodName(ref.getOwner(), ref.getName(), ref.getDesc());
			String desc = Remapper.getRemappedMethodDescriptor(mappings, ref.getDesc(), sb);
			aw.visitMethod(owner, name, desc, entry.access, false);
		}
		for (MemberAccess entry : fieldAccess) {
			MemberRef ref = entry.member;
			String owner = mappings.getRemappedClassName(ref.getOwner());
			String name = mappings.getRemappedFieldName(ref.getOwner(), ref.getName(), ref.getDesc());
			String desc = Remapper.getRemappedFieldDescriptor(mappings, ref.getDesc(), sb);
			aw.visitField(owner, name, desc, entry.access, false);
		}
		return aw;
	}

	private static class ClassAccess {
		public final String clazz;
		public final AccessWidenerReader.AccessType access;

		public ClassAccess(String cls, AccessWidenerReader.AccessType acc) {
			clazz = cls;
			access = acc;
		}
	}

	private static class MemberAccess {
		public final MemberRef member;
		public final AccessWidenerReader.AccessType access;

		public MemberAccess(MemberRef ref, AccessWidenerReader.AccessType acc) {
			member = ref;
			access = acc;
		}
	}
}
