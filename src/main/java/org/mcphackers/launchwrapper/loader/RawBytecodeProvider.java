package org.mcphackers.launchwrapper.loader;

public interface RawBytecodeProvider {
    byte[] getClassBytecode(String name);

    String getClassName(String name);
}
