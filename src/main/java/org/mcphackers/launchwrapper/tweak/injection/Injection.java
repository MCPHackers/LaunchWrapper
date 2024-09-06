package org.mcphackers.launchwrapper.tweak.injection;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public interface Injection {

    String name();

    boolean required();

    boolean apply(ClassNodeSource source, LaunchConfig config);
}
