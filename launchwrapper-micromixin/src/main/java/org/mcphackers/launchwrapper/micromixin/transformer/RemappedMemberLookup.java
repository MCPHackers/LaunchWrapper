package org.mcphackers.launchwrapper.micromixin.transformer;

import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.stianloader.remapper.MappingLookup;
import org.stianloader.remapper.Remapper;

public class RemappedMemberLookup extends MemberLookup {
    private StringBuilder sb = new StringBuilder();
    private MappingLookup named2ObfMappings;
    private MappingLookup obf2NamedMappings;
    public RemappedMemberLookup(ClassNodeSource obfuscatedSource, Mappings mappings) {
        super(obfuscatedSource);
        this.named2ObfMappings = new MappingProvider(mappings);
        this.obf2NamedMappings = new MappingProvider(mappings.reverse());
    }

    // public Collection<MemberRef> tryInferMember(String owner, String name, String desc) {
    //     return super.tryInferMember(owner, name, desc);
    // }

    protected ClassNode getClass(String name) {
        // Remapped member lookup relies on the fact that it returns class node for both obfuscated and named classes
        String remappedName = named2ObfMappings.getRemappedClassName(name);
        return source.getClass(remappedName);
    }

    protected FieldNode getField(ClassNode owner, String name, String desc) {
        // name and desc are named
        String namedOwner = obf2NamedMappings.getRemappedClassName(owner.name);
        String obfDesc = Remapper.getRemappedFieldDescriptor(named2ObfMappings, desc, sb);
        return NodeHelper.getField(owner, named2ObfMappings.getRemappedFieldName(namedOwner, name, desc), obfDesc);
    }

    protected MethodNode getMethod(ClassNode owner, String name, String desc) {
        // name and desc are named
        String namedOwner = obf2NamedMappings.getRemappedClassName(owner.name);
        String obfDesc = Remapper.getRemappedMethodDescriptor(named2ObfMappings, desc, sb);
        return NodeHelper.getMethod(owner, named2ObfMappings.getRemappedMethodName(namedOwner, name, desc), obfDesc);
    }
}
