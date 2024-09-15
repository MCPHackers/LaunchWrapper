package org.mcphackers.launchwrapper.micromixin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.micromixin.transformer.AccessWidenerImpl;
import org.mcphackers.launchwrapper.micromixin.transformer.Mappings;
import org.mcphackers.launchwrapper.micromixin.tweak.MicroMixinTweak;
import org.mcphackers.launchwrapper.micromixin.util.ClasspathAdapter;
import org.mcphackers.launchwrapper.micromixin.util.DirectoryClasspath;
import org.mcphackers.launchwrapper.micromixin.util.JarClasspath;
import org.mcphackers.launchwrapper.util.Util;
import org.stianloader.micromixin.transform.api.MixinConfig;
import org.stianloader.micromixin.transform.api.MixinConfig.InvalidMixinConfigException;
import org.stianloader.remapper.MemberRef;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class FileMixinMod implements MixinMod {

    private MixinConfig config;
    private MappingSource mappingSource;
	private AccessWidenerImpl aw;
    private Collection<String> classes;
	private ModMetadata metadata;

    public FileMixinMod(File modLocation) throws IOException, InvalidMixinConfigException {
		if(!modLocation.exists()) {
            throw new FileNotFoundException("Mod at " + modLocation.getAbsolutePath() + " doesn't exist");
        };
		String modid;
		String version;
		String sourceNamespace = "named";
		String targetNamespace = MicroMixinTweak.DEV ? "named" : "official";
        String mappingSourceClass = null;
		try(ClasspathAdapter adapter = getAdapter(modLocation)) {
			// The intended configuration for LaunchWrapper-only mods
            InputStream is = adapter.getResource("launchwrapper.mod.json");
            if(is != null) {
				JSONObject modconfig = new JSONObject(new String(Util.readStream(is)));
				modid = modconfig.optString("id", "launchwrapper");
				version = modconfig.optString("version", "0.0.0");
				sourceNamespace = modconfig.optString("sourceNamespace", sourceNamespace);
				targetNamespace = modconfig.optString("targetNamespace", targetNamespace);
				mappingSourceClass = modconfig.optString("mappingSource", null);
            } else {
				// Fabric mod compatibility
				is = adapter.getResource("fabric.mod.json");
				if(is != null) {
					JSONObject modconfig = new JSONObject(new String(Util.readStream(is)));
					modid = modconfig.optString("id", "fabric");
					version = modconfig.optString("version", "0.0.0");
					sourceNamespace = "intermediary"; // All fabric mods should be intermediary. In theory
				} else {
					throw new IOException("Jar is missing mod manifest");
				}
			}
			metadata = new ModMetadata(modid, version);

			// File name isn't always modid.mixin.json. Fabric mod json allows specifying mixin config with any file name.
			// We don't support such cases. And neither we support multiple mixin configurations per mod
            is = adapter.getResource(modid + ".mixins.json");
            if(is == null) {
                throw new IOException("Jar is missing mixin configuration json");
            }
            JSONObject jsonConfig = new JSONObject(new String(Util.readStream(is)));
            config = MixinConfig.fromJson(jsonConfig);
            
			if(mappingSourceClass == null) {
				Mappings mappings;
				is = adapter.getResource("mappings/mappings.tiny");
				if(is != null) {
					// Read mappings from mod jar
					mappings = readMappings(is, sourceNamespace, targetNamespace);
				} else {
					// Read mappings from classpath (Usually only contain intermediary and official namespaces)
					is = getClass().getClassLoader().getResourceAsStream("mappings/mappings.tiny");
					if(is != null) {
						mappings = readMappings(new BufferedInputStream(is), sourceNamespace, targetNamespace);
					} else {
						mappings = new Mappings();
					}
				}
				mappingSource = MappingSource.fromMappings(mappings);
			} else {
				try {
					Class<?> clazz = Class.forName(mappingSourceClass);
					mappingSource = (MappingSource)clazz.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			is = adapter.getResource(modid + ".accesswidener");
			if(is != null) {
				aw = new AccessWidenerImpl();
                AccessWidenerReader reader = new AccessWidenerReader(aw);
				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {
					reader.read(bufferedReader);
				}
			}
			classes = adapter.getClasses();
        }

    }

	private ClasspathAdapter getAdapter(File f) throws IOException {
		if(f.isDirectory()) {
			return new DirectoryClasspath(f.toPath());
		}
		return new JarClasspath(f);
	}

	@Override
	public ModMetadata getMetadata() {
		return metadata;
	}

    @Override
    public MixinConfig getMixinConfig() {
        return config;
    }

    @Override
    public MappingSource getMappingSource() {
        return mappingSource;
    }

	@Override
	public Collection<String> getClasses() {
		return classes;
	}

	@Override
	public AccessWidenerImpl getAccessWidener() {
		return aw;
	}

	private static Mappings readMappings(InputStream is, String srcNamespace, String targetNamespace) {
		Mappings mappings = new Mappings();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			MappingReader.read(br, mappingTree);

			for(ClassMapping classMapping : mappingTree.getClasses()) {
				String className = classMapping.getName(srcNamespace);
				if(className == null) {
					className = classMapping.getSrcName();
				}
				else if (classMapping.getName(targetNamespace) != null) {
					mappings.classes.put(classMapping.getName(srcNamespace), classMapping.getName(targetNamespace));
				}
				
				for(FieldMapping fieldMapping : classMapping.getFields()) {
					if(fieldMapping.getName(srcNamespace) == null || fieldMapping.getName(targetNamespace) == null) {
						continue;
					}
					mappings.fields.put(new MemberRef(className, fieldMapping.getName(srcNamespace), fieldMapping.getDesc(srcNamespace)),
							fieldMapping.getName(targetNamespace));
				}
				
				for(MethodMapping methodMapping : classMapping.getMethods()) {
					if(methodMapping.getName(srcNamespace) == null || methodMapping.getName(targetNamespace) == null) {
						continue;
					}
					mappings.methods.put(new MemberRef(className, methodMapping.getName(srcNamespace), methodMapping.getDesc(srcNamespace)),
							methodMapping.getName(targetNamespace));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mappings;
	}

}
