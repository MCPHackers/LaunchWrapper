package org.mcphackers.launchwrapper.micromixin;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.micromixin.transformer.Mappings;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public abstract class MappingSource implements Injection {

    protected Mappings mappings = new Mappings();

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean required() {
        return true;
    }

    public Mappings getMappings() {
        return mappings;
    }
    
    public static MappingSource fromMappings(Mappings maps) {
        return new MappingSource() {
            @Override
            public Mappings getMappings() {
                return maps;
            }

            @Override
            public boolean apply(ClassNodeSource source, LaunchConfig config) {
                return true;
            }
        };
    }
}
