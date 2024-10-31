package net.minecraft.launchwrapper;

@Deprecated
public interface IClassTransformer {

	byte[] transform(String name, String transformedName, byte[] basicClass);
}
