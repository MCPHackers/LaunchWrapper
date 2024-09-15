package org.mcphackers.launchwrapper.micromixin.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.stianloader.remapper.MemberRef;
import org.stianloader.remapper.Remapper;

public class Mappings {
	public final Map<MemberRef, String> fields = new HashMap<MemberRef, String>();
	public final Map<MemberRef, String> methods = new HashMap<MemberRef, String>();
	public final Map<String, String> classes = new HashMap<String, String>();
	private StringBuilder sb = new StringBuilder();

	public boolean hasMappings(String clazz) {
		if(classes.containsKey(clazz)) {
			return true;
		}
		for(MemberRef ref : fields.keySet()) {
			if(ref.getOwner().equals(clazz)) {
				return true;
			}
		}
		for(MemberRef ref : methods.keySet()) {
			if(ref.getOwner().equals(clazz)) {
				return true;
			}
		}
		return false;
	}

	public void clear() {
		fields.clear();
		methods.clear();
		classes.clear();
	}

	public void copyTo(Mappings mappings) {
		mappings.classes.putAll(this.classes);
		mappings.fields.putAll(this.fields);
		mappings.methods.putAll(this.methods);
	}

	public Mappings reverse() {
		Mappings newMappings = new Mappings();
		MappingProvider provider = new MappingProvider(this);
		for(Entry<String, String> entry : classes.entrySet()) {
			newMappings.classes.put(entry.getValue(), entry.getKey());
		}
		for(Entry<MemberRef, String> entry : methods.entrySet()) {
			MemberRef ref = entry.getKey();
			newMappings.methods.put(
				new MemberRef(
					classes.get(ref.getOwner()),
					entry.getValue(),
					Remapper.getRemappedMethodDescriptor(provider, ref.getDesc(), sb)
				), ref.getName());
		}
		for(Entry<MemberRef, String> entry : fields.entrySet()) {
			MemberRef ref = entry.getKey();
			newMappings.fields.put(
				new MemberRef(
					classes.get(ref.getOwner()),
					entry.getValue(),
					Remapper.getRemappedFieldDescriptor(provider, ref.getDesc(), sb)
				), ref.getName());
		}
		return newMappings;
	}
}
