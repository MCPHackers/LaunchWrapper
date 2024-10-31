package net.minecraft.launchwrapper;

@Deprecated
public interface IClassNameTransformer {

	String unmapClassName(String name);

	String remapClassName(String name);
}
