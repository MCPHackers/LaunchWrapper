package org.mcphackers.launchwrapper.loader;

import org.objectweb.asm.ClassWriter;

public class CustomClassWriter extends ClassWriter {

    protected ClassLoader classLoader;

    public CustomClassWriter(ClassLoader loader, int flags) {
        super(flags);
        classLoader = loader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
