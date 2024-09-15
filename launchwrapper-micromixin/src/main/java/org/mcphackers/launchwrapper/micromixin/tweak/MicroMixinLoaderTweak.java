package org.mcphackers.launchwrapper.micromixin.tweak;

import org.mcphackers.launchwrapper.tweak.LazyTweaker;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.ClassNode;
import org.stianloader.micromixin.transform.api.MixinTransformer;

public class MicroMixinLoaderTweak implements LazyTweaker {

    private MixinTransformer<ClassNodeSource> transformer;
    public MicroMixinLoaderTweak(MixinTransformer<ClassNodeSource> transformer) {
        this.transformer = transformer;
    }
    @Override
    public boolean tweakClass(ClassNodeSource source, String name) {
        if(!transformer.isMixinTarget(name)) {
            return false;
        }
        ClassNode node = source.getClass(name);
        if(node == null) {
            return false;
        }
        transformer.transform(node);
        source.overrideClass(node);
        return true;
    }
}
