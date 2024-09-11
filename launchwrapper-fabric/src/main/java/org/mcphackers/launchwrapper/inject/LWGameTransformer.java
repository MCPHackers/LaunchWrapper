package org.mcphackers.launchwrapper.inject;

import static org.objectweb.asm.ClassWriter.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipError;

import org.mcphackers.launchwrapper.loader.SafeClassWriter;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;
import net.fabricmc.loader.impl.util.SimpleClassPath.CpEntry;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class LWGameTransformer extends GameTransformer implements ClassNodeSource {
	private LWGameProvider gameProvider;
	private Map<String, ClassNode> modified;
	private Function<String, ClassNode> classSource;
	private boolean entrypointsLocated = false;

	public LWGameTransformer(LWGameProvider gameProvider) {
		this.gameProvider = gameProvider;
	}

	public void locateEntrypoints(FabricLauncher launcher, List<Path> gameJars) {
		if (entrypointsLocated) {
			return;
		}

		modified = new HashMap<>();

		try (SimpleClassPath cp = new SimpleClassPath(gameJars)) {
			classSource = name -> {
				ClassNode node = modified.get(name);

                if (node != null) {
                    return node;
				}

				try {
					CpEntry entry = cp.getEntry(LoaderUtil.getClassFileName(name));
					if (entry == null) return null;

					try (InputStream is = entry.getInputStream()) {
						node = new ClassNode();
						ClassReader reader = new ClassReader(is);
						reader.accept(node, 0);
						return node;
					} catch (IOException | ZipError e) {
						throw new RuntimeException(String.format("error reading %s in %s: %s", name, LoaderUtil.normalizePath(entry.getOrigin()), e), e);
					}
				} catch (IOException e) {
					throw ExceptionUtil.wrap(e);
				}
			};

            Tweak tweak = gameProvider.getTweak(this);
			tweak.transform(this);
			gameProvider.target = (MainLaunchTarget)tweak.getLaunchTarget();
			
		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}

		Log.debug(LogCategory.GAME_PATCH, "Patched %d class%s", modified.size(), modified.size() != 1 ? "s" : "");
		entrypointsLocated = true;
	}

	public ClassNode getClass(String name) {
		return classSource.apply(name.replace("/", "."));
	}

	public void overrideClass(ClassNode node) {
		modified.put(node.name.replace("/", "."), node);
	}

	public byte[] transform(String className) {
        ClassNode node = modified.get(className);
        if(node == null) {
            return null;
        }
        // Fabric's GameTransformer did not compute max stack and local
        // Tweaks rely on writer computing maxes for it
		// Also don't trust existing frames
        ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS | COMPUTE_FRAMES);
        node.accept(writer);
		return writer.toByteArray();
	}

}
