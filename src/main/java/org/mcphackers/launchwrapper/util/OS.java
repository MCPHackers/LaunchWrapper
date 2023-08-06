package org.mcphackers.launchwrapper.util;

public enum OS {
    LINUX,
    SOLARIS,
    WINDOWS,
    OSX,
    UNKNOWN;

    public static OS os;


    public static OS getOs() {
        if(os != null) {
            return os;
        }
        String s = System.getProperty("os.name").toLowerCase();
        os
                = s.contains("win") ? OS.WINDOWS
                : s.contains("mac") ? OS.OSX
                : s.contains("solaris") ? OS.SOLARIS
                : s.contains("sunos") ? OS.SOLARIS
                : s.contains("linux") ? OS.LINUX
                : s.contains("unix") ? OS.LINUX
                : OS.UNKNOWN;
        return os;
    }
}