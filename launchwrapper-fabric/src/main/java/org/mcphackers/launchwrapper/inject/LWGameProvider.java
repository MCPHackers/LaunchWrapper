package org.mcphackers.launchwrapper.inject;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.fabric.FabricBridge;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.FabricLoaderTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.minecraft.McVersion;
import net.fabricmc.loader.impl.game.minecraft.McVersionLookup;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.metadata.ModDependencyImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.SystemProperties;

public class LWGameProvider implements GameProvider {

    public LaunchConfig config = FabricBridge.getInstance().config;
    public MainLaunchTarget target = null;

	private Arguments arguments;
    private Path gameJar;
    private List<Path> lwjglJars = new ArrayList<>();
    private Path launchwrapperJar;
    private EnvType envType;
	private final List<Path> miscGameLibraries = new ArrayList<>();
    private final List<Path> validParentClassPath = new ArrayList<>();
	private McVersion versionData;
	private boolean hasModLoader;
	private final GameTransformer transformer = new LWGameTransformer(this);

	public Tweak getTweak(ClassNodeSource source) {
		return new FabricLoaderTweak(Tweak.get(source, config), config);
	}
    
    @Override
	public void launch(ClassLoader loader) {
        String targetClass = target.targetClass;
        String[] arguments = target.args;

		MethodHandle invoker;

		try {
			Class<?> c = loader.loadClass(targetClass);
			invoker = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
			throw FormattedException.ofLocalized("exception.minecraft.invokeFailure", e);
		}

		try {
			invoker.invokeExact(arguments);
		} catch (Throwable t) {
			throw FormattedException.ofLocalized("exception.minecraft.generic", t);
		}
	}

	private Path getLaunchwrapperSource() {
		CodeSource source = Launch.class.getProtectionDomain().getCodeSource();
		if(source != null) {
			String path = source.getLocation().getPath();
			if(path.startsWith("file:")) {
				path = path.substring("file:".length());
				int i = path.lastIndexOf('!');
				if(i != -1) {
					path = path.substring(0, i);
				}
			}
			return Paths.get(path);
		}
		return null;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.envType = launcher.getEnvironmentType();
		this.arguments = new Arguments();
		arguments.parse(args);
		
		String entrypoint = null;
		try {
			LibClassifier<LWLib> classifier = new LibClassifier<>(LWLib.class, envType, this);
			LWLib envGameLib = envType == EnvType.CLIENT ? LWLib.MC_CLIENT : LWLib.MC_SERVER;
			Path envGameJar = GameProviderHelper.getEnvGameJar(envType);
			if (envGameJar != null) {
				classifier.process(envGameJar);
			}

			classifier.process(launcher.getClassPath());

			envGameJar = classifier.getOrigin(envGameLib);
			if (envGameJar == null) return false;
			
			entrypoint = classifier.getClassName(envGameLib);
			if(entrypoint == null) return false;

			gameJar = envGameJar;
			launchwrapperJar = classifier.getOrigin(LWLib.LAUNCHWRAPPER);
			if(launchwrapperJar == null) {
				launchwrapperJar = getLaunchwrapperSource();
			}
			if(classifier.has(LWLib.LWJGL)) {
				lwjglJars.add(classifier.getOrigin(LWLib.LWJGL));
			}
			if(classifier.has(LWLib.LWJGL3)) {
				lwjglJars.add(classifier.getOrigin(LWLib.LWJGL3));
			}
			hasModLoader = classifier.has(LWLib.MODLOADER);
			miscGameLibraries.addAll(lwjglJars);
			miscGameLibraries.addAll(classifier.getUnmatchedOrigins());
			if(launchwrapperJar != null) {
				validParentClassPath.add(launchwrapperJar);
			}
			validParentClassPath.addAll(classifier.getSystemLibraries());
		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}
		
		ObjectShare share = FabricLoaderImpl.INSTANCE.getObjectShare();
		share.put("fabric-loader:inputGameJar", gameJar); // deprecated
		share.put("fabric-loader:inputGameJars", Collections.singletonList(gameJar));

		String version = arguments.remove(Arguments.GAME_VERSION);
		if (version == null) version = System.getProperty(SystemProperties.GAME_VERSION);
		versionData = McVersionLookup.getVersion(Collections.singletonList(gameJar), entrypoint, version);
		return true;
	}

	@Override
	public String getEntrypoint() {
		return target.targetClass.replace("/", ".");
	}

	@Override
	public Path getLaunchDirectory() {
		return FabricBridge.getInstance().config.gameDir.get().toPath();
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

    @Override
    public String getGameId() {
        return "minecraft";
    }

    @Override
    public String getGameName() {
        return "Minecraft";
    }

	@Override
	public String getRawGameVersion() {
		return versionData.getRaw();
	}

	@Override
	public String getNormalizedGameVersion() {
		return versionData.getNormalized();
	}

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
		List<BuiltinMod> mods = new ArrayList<BuiltinMod>();
		BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName());

		Map<String, String> contactInfo = new HashMap<String, String>();
		contactInfo.put("homepage", "https://github.com/MCPHackers/LaunchWrapper"); 
		contactInfo.put("sources", "https://github.com/MCPHackers/LaunchWrapper");
		contactInfo.put("issues", "https://github.com/MCPHackers/LaunchWrapper/issues");
		BuiltinModMetadata.Builder metadataLW = new BuiltinModMetadata.Builder("launchwrapper", Launch.VERSION)
				.setName("LaunchWrapper")
				.setEnvironment(ModEnvironment.CLIENT)
				.setDescription("Launch wrapper for legacy Minecraft")
				.addAuthor("lassebq", Collections.emptyMap())
				.addIcon(0, "icon_256x256.png")
				.addIcon(1, "icon_48x48.png")
				.addIcon(2, "icon_32x32.png")
				.addIcon(3, "icon_16x16.png")
				.addLicense("MIT")
				.setContact(new ContactInformationImpl(contactInfo));

		BuiltinModMetadata.Builder metadataLWJGL = new BuiltinModMetadata.Builder("lwjgl", LWJGLVersionLookup.getVersion(lwjglJars))
				.setName("LWJGL")
				.setDescription("Lightweight Java Game Library");

		if (versionData.getClassVersion().isPresent()) {
			int version = versionData.getClassVersion().getAsInt() - 44;
			
			try {
				metadataLW.addDependency(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "minecraft", Collections.emptyList()));
				metadata.addDependency(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "java", Collections.singletonList(String.format(Locale.ENGLISH, ">=%d", version))));
				metadata.addDependency(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "lwjgl", Collections.emptyList()));
			} catch (VersionParsingException e) {
				throw new RuntimeException(e);
			}
		}
		
		mods.add(new BuiltinMod(Collections.singletonList(launchwrapperJar), metadataLW.build()));
		mods.add(new BuiltinMod(lwjglJars, metadataLWJGL.build()));
		mods.add(new BuiltinMod(Collections.singletonList(gameJar), metadata.build()));
		return mods;
    }

	public Path getGameJar() {
		return gameJar;
	}

    @Override
    public boolean isObfuscated() {
        return true;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return hasModLoader;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
	public boolean hasAwtSupport() {
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
		launcher.setValidParentClassPath(validParentClassPath);

		if (isObfuscated()) {
			Map<String, Path> obfJars = new HashMap<>(1);
            String clientSide = envType.name().toLowerCase(Locale.ENGLISH);
            obfJars.put(clientSide, gameJar);

			obfJars = GameProviderHelper.deobfuscate(obfJars,
					getGameId(), getNormalizedGameVersion(),
					getLaunchDirectory(),
					launcher);
            gameJar = obfJars.get(clientSide);
		}

		transformer.locateEntrypoints(launcher, Collections.singletonList(gameJar));
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        for (Path lib : miscGameLibraries) {
            launcher.addToClassPath(lib);
		}
        launcher.addToClassPath(gameJar);
    }

	@Override
    public Arguments getArguments() {
        Arguments args = new Arguments();
        args.parse(config.getArgs());
        return args;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return config.getArgs();
    }

	@Override
	public boolean canOpenErrorGui() {
        return envType == EnvType.CLIENT;
    }
}
