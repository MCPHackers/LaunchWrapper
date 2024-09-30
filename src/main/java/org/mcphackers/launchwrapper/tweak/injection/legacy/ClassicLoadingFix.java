package org.mcphackers.launchwrapper.tweak.injection.legacy;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public class ClassicLoadingFix extends InjectionWithContext<MinecraftGetter>{

    public ClassicLoadingFix(MinecraftGetter context) {
        super(context);
    }

    @Override
    public String name() {
        return "Classic loading fix";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        
        return false;
    }

}
