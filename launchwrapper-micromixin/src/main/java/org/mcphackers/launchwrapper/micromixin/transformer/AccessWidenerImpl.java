package org.mcphackers.launchwrapper.micromixin.transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.stianloader.remapper.MappingLookup;
import org.stianloader.remapper.MemberRef;
import org.stianloader.remapper.Remapper;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;

public class AccessWidenerImpl implements AccessWidenerVisitor {
	final Map<String, AccessWidenerReader.AccessType> classAccess = new HashMap<>();
	final Map<MemberRef, AccessWidenerReader.AccessType> methodAccess = new HashMap<>();
	final Map<MemberRef, AccessWidenerReader.AccessType> fieldAccess = new HashMap<>();
	// Contains the class-names that are affected by loaded wideners.
	// Names are period-separated binary names (i.e. a.b.C).
	final Set<String> targets = new HashSet<>();
    public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
		classAccess.put(name, access);
		targets.add(name);
	}

	public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		methodAccess.put(new MemberRef(owner, name, descriptor), access);
		targets.add(owner);
	}

	public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
		fieldAccess.put(new MemberRef(owner, name, descriptor), access);
		targets.add(owner);
	}

    public AccessWidener getRemappedAccessWidener(MappingLookup mappings) {
        AccessWidener aw = new AccessWidener();
		for(Entry<String, AccessWidenerReader.AccessType> entry : classAccess.entrySet()) {
			aw.visitClass(mappings.getRemappedClassName(entry.getKey()), entry.getValue(), false);
		}
		StringBuilder sb = new StringBuilder();
		for(Entry<MemberRef, AccessWidenerReader.AccessType> entry : methodAccess.entrySet()) {
			MemberRef ref = entry.getKey();
			String owner = mappings.getRemappedClassName(ref.getOwner());
			String name = mappings.getRemappedMethodName(ref.getOwner(), ref.getName(), ref.getDesc());
			String desc = Remapper.getRemappedMethodDescriptor(mappings, ref.getDesc(), sb);
			aw.visitMethod(owner, name, desc, entry.getValue(), false);
		}
		for(Entry<MemberRef, AccessWidenerReader.AccessType> entry : fieldAccess.entrySet()) {
			MemberRef ref = entry.getKey();
			String owner = mappings.getRemappedClassName(ref.getOwner());
			String name = mappings.getRemappedFieldName(ref.getOwner(), ref.getName(), ref.getDesc());
			String desc = Remapper.getRemappedFieldDescriptor(mappings, ref.getDesc(), sb);
			aw.visitField(owner, name, desc, entry.getValue(), false);
		}
        return aw;
    }
}
