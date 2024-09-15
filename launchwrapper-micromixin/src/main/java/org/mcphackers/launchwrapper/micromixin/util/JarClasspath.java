package org.mcphackers.launchwrapper.micromixin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarClasspath implements ClasspathAdapter {
    ZipFile jar;

    public JarClasspath(File jar) throws IOException {
        this.jar = new ZipFile(jar); 
    }

    @Override
    public void close() throws IOException {
        jar.close();
    }

    @Override
    public InputStream getResource(String entry) {
        ZipEntry zipEntry = jar.getEntry(entry);
        if(zipEntry != null) {
            try {
                return jar.getInputStream(zipEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public Collection<String> getClasses() {
        List<String> classes = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = jar.entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if(name.endsWith(".class")) {
                classes.add(name.substring(0, name.length() - 6));
            }
        }
        return classes;
    }
    
}
