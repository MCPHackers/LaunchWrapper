package org.mcphackers.launchwrapper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.mcphackers.launchwrapper.protocol.skin.MojangSkinProvider;
import org.mcphackers.launchwrapper.protocol.skin.SkinOption;
import org.mcphackers.launchwrapper.protocol.skin.SkinType;
import org.mcphackers.launchwrapper.util.OS;

@SuppressWarnings("unused")
public class LaunchConfig {
	private static final File DEFAULT_GAME_DIR = getDefaultGameDir();
	// Store all arguments into this list, in order of whatever has been passed by a launcher or the user
	// NOTE: this is supposed to hold String and LaunchParameter objects primarily
	private final List<Object> arguments = new ArrayList<Object>();
	private final Map<String, LaunchParameter<?>> parameters = new HashMap<String, LaunchParameter<?>>();

	// clang-format off

	// Minecraft options
	public final LaunchParameterSwitch demo 				= new LaunchParameterSwitch("demo");
	public final LaunchParameterSwitch fullscreen			= new LaunchParameterSwitch("fullscreen");
	public final LaunchParameterString server 				= new LaunchParameterString("server");
	public final LaunchParameterNumber port 				= new LaunchParameterNumber("port", 25565);
	public final LaunchParameterFile gameDir 				= new LaunchParameterFile("gameDir", DEFAULT_GAME_DIR).altName("workDir");
	public final LaunchParameterFile assetsDir 				= new LaunchParameterFile("assetsDir");
	public final LaunchParameterFile resourcePackDir 		= new LaunchParameterFile("resourcePackDir");
	public final LaunchParameterString username 			= new LaunchParameterString("username", "Player");
	public final LaunchParameterString session 				= new LaunchParameterString("session", "-").altName("sessionid");
	public final LaunchParameterString password 			= new LaunchParameterString("password");
	public final LaunchParameterString uuid 				= new LaunchParameterString("uuid");
	public final LaunchParameterString accessToken 			= new LaunchParameterString("accessToken");
	public final LaunchParameterString version 				= new LaunchParameterString("version");
	public final LaunchParameterNumber width 				= new LaunchParameterNumber("width", 854);
	public final LaunchParameterNumber height 				= new LaunchParameterNumber("height", 480);
	public final LaunchParameterString userProperties 		= new LaunchParameterString("userProperties");
	public final LaunchParameterString profileProperties 	= new LaunchParameterString("profileProperties");
	public final LaunchParameterString assetIndex 			= new LaunchParameterString("assetIndex");
	public final LaunchParameterString userType 			= new LaunchParameterString("userType");
	public final LaunchParameterString versionType 			= new LaunchParameterString("versionType");
	// Applet arguments
	private final LaunchParameterSwitch haspaid				= new LaunchParameterSwitch("haspaid", true);
	public final LaunchParameterString loadmapUser	 		= new LaunchParameterString("loadmap_user");
	public final LaunchParameterNumber loadmapId 			= new LaunchParameterNumber("loadmap_id");
	public final LaunchParameterString mppass 				= new LaunchParameterString("mppass", "");
	// Built-in wrapper options
	public final LaunchParameterString tweakClass 			= new LaunchParameterString("tweakClass", null, true);
	public final LaunchParameterString brand				= new LaunchParameterString("brand", "launchwrapper", true);
	public final LaunchParameterString realmsServer	 		= new LaunchParameterString("realmsServer", null, true);
	public final LaunchParameterNumber realmsPort	 		= new LaunchParameterNumber("realmsPort", 80, true);
	public final LaunchParameterSwitch lwjglFrame	 		= new LaunchParameterSwitch("lwjglFrame", true, true);
	private final LaunchParameterSwitch awtFrame 			= new LaunchParameterSwitchReverse("awtFrame", lwjglFrame);
	public final LaunchParameterSwitch disableSkinFix 		= new LaunchParameterSwitch("disableSkinFix", false, true);
	public final LaunchParameterEnum<SkinType> skinProxy 	= new LaunchParameterEnum<SkinType>("skinProxy", SkinType.DEFAULT, true);
	public final LaunchParameterSkinOptions skinOptions		= new LaunchParameterSkinOptions("skinOptions");
	public final LaunchParameterFile levelsDir				= new LaunchParameterFile("levelsDir", null, true);
	public final LaunchParameterSwitch isom 				= new LaunchParameterSwitch("isom", false, true);
	public final LaunchParameterFileList icon 				= new LaunchParameterFileList("icon", null, true);
	public final LaunchParameterString title 				= new LaunchParameterString("title", null, true);
	public final LaunchParameterSwitch vsync 				= new LaunchParameterSwitch("vsync", false, true);
	public final LaunchParameterSwitch resizable 			= new LaunchParameterSwitch("resizable", false, true);
	public final LaunchParameterSwitch applet 				= new LaunchParameterSwitch("applet", false);
	public final LaunchParameterSwitch oneSixFlag 			= new LaunchParameterSwitch("oneSixFlag", false, true);
	public final LaunchParameterSwitch unlicensedCopy		= new LaunchParameterSwitchReverse("unlicensedCopy", haspaid);
	public final LaunchParameterSwitch creative	 			= new LaunchParameterSwitch("creative", false, true);
	public final LaunchParameterSwitch survival	 			= new LaunchParameterSwitch("survival", false, true);
	public final LaunchParameterString serverURL 			= new LaunchParameterString("serverURL", null, true);
	public final LaunchParameterString serverSHA1 			= new LaunchParameterString("serverSHA1", null, true);
	// clang-format on

	private static File getDefaultGameDir() {
		String game = "minecraft";
		String homeDir = System.getProperty("user.home", ".");
		File gameDir;
		switch (OS.getOs()) {
			case LINUX:
			case SOLARIS:
				String dataHome = System.getProperty("XDG_DATA_HOME");
				if (dataHome != null) {
					gameDir = new File(dataHome, "." + game + '/');
				} else {
					gameDir = new File(homeDir, ".local/share/" + game + '/');
				}
				break;
			case WINDOWS:
				String appdata = System.getenv("APPDATA");
				if (appdata != null) {
					gameDir = new File(appdata, "." + game + '/');
				} else {
					gameDir = new File(homeDir, '.' + game + '/');
				}
				break;
			case OSX:
				gameDir = new File(homeDir, "Library/Application Support/" + game);
				break;
			default:
				gameDir = new File(homeDir, game + '/');
		}

		if (!gameDir.exists() && !gameDir.mkdirs()) {
			throw new RuntimeException("The working directory could not be created: " + gameDir);
		} else {
			return gameDir;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public LaunchConfig clone() {
		LaunchConfig newConfig = new LaunchConfig();
		for (Object argument : arguments) {
			if (argument == null) {
				continue;
			}
			if (argument instanceof LaunchParameter) {
				LaunchParameter<Object> param = (LaunchParameter<Object>)newConfig.parameters.get(((LaunchParameter<?>)argument).name);
				if (param == null) {
					continue;
				}
				// This should add the new config's parameters to its arguments list
				newConfig.arguments.add(param);
			} else {
				newConfig.arguments.add(argument);
			}
		}
		for (Map.Entry<String, LaunchParameter<?>> entry : parameters.entrySet()) {
			LaunchParameter<Object> param = (LaunchParameter<Object>)newConfig.parameters.get(entry.getKey());
			if (param == null) {
				continue;
			}
			param.set(entry.getValue().get());
		}
		return newConfig;
	}

	public LaunchConfig() {
		levelsDir.set(new File(DEFAULT_GAME_DIR, "levels"));
	}

	public LaunchConfig(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("--")) {
				arguments.add(args[i]);
				continue;
			}
			String paramName = args[i].substring(2);
			LaunchParameter<?> param = parameters.get(paramName);
			if (param == null) {
				arguments.add(args[i]);
				continue;
			}
			if (param.isSwitch()) {
				((LaunchParameterSwitch)param).setFlag();
				arguments.add(param);
				continue;
			}
			if (i + 1 < args.length) {
				try {
					param.setString(args[i + 1]);
					i++;
				} catch (IllegalArgumentException ignored) {
				}
			}
			arguments.add(param);
		}
		for (LaunchParameter<?> param : parameters.values()) {
			if (!arguments.contains(param)) {
				arguments.add(param);
			}
		}
		if (levelsDir.get() == null) {
			levelsDir.set(new File(gameDir.get(), "levels"));
		}
		if (uuid.get() == null && username.get() != null) {
			// Resolve profile UUID if we only provide username
			uuid.set(MojangSkinProvider.getUUIDfromName(username.get()));
		}
		if (accessToken.get() == null && session.get() != null && session.get().startsWith("token:")) {
			// Parse the session ID for the access token if we don't have it
			// 'token:<ACCESS_TOKEN>:<UUID>'
			String[] token = session.get().split(":");
			accessToken.set(token[1]);
		}
	}

	public List<LaunchParameter<?>> getParamsAsList() {
		List<LaunchParameter<?>> list = new ArrayList<LaunchParameter<?>>();
		for (LaunchParameter<?> param : parameters.values()) {
			if (!param.wrapperOnly && param.get() != null && !param.get().equals(Boolean.FALSE)) {
				list.add(param);
			}
		}
		return list;
	}

	public Map<String, String> getArgsAsMap() {
		// LinkedHashMap as arguments should be returned in a consistent order
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Object argument : arguments) {
			if (argument == null) {
				continue;
			}
			if (argument instanceof LaunchParameter) {
				LaunchParameter<?> param = (LaunchParameter<?>)argument;
				if (!param.wrapperOnly && param.get() != null && !param.get().equals(Boolean.FALSE)) {
					map.put(param.name, param.getString());
					if (param.getAltName() != null && param.getAltName().length() > 0) {
						map.put(param.getAltName(), param.getString());
					}
				}
			} else {
				map.put(argument.toString(), null);
			}
		}
		return map;
	}

	public String[] getArgs() {
		List<String> list = new ArrayList<String>();
		for (Object argument : arguments) {
			if (argument == null) {
				continue;
			}
			if (argument instanceof LaunchParameter) {
				LaunchParameter<?> param = (LaunchParameter<?>)argument;
				if (!param.wrapperOnly && param.get() != null) {
					if (param.isSwitch()) {
						if (param.get().equals(Boolean.TRUE)) {
							list.add("--" + param.name);
							if (param.getAltName() != null && param.getAltName().length() > 0) {
								list.add("--" + param.getAltName());
							}
						}
					} else {
						list.add("--" + param.name);
						list.add(param.getString());
						if (param.getAltName() != null && param.getAltName().length() > 0) {
							list.add("--" + param.getAltName());
							list.add(param.getString());
						}
					}
				}
			} else {
				list.add(argument.toString());
			}
		}
		String[] arr = new String[list.size()];
		return list.toArray(arr);
	}

	public abstract class LaunchParameter<T> {
		public final String name;
		public final boolean wrapperOnly;
		protected String altName;
		protected final T defaultValue;
		protected T value;

		protected LaunchParameter(String name) {
			this(name, null, false);
		}

		protected LaunchParameter(String name, T defaultValue) {
			this(name, defaultValue, false);
		}

		protected LaunchParameter(String name, T defaultValue, boolean wrapper) {
			this.defaultValue = defaultValue;
			this.value = defaultValue;
			this.name = name;
			this.wrapperOnly = wrapper;
			this.altName = null;
			// Parameters names are case sensitive
			parameters.put(name, this);
		}

		public LaunchParameter<T> altName(String altName) {
			this.altName = altName;
			parameters.put(altName, this);
			return this;
		}

		public String getAltName() {
			return this.altName;
		}

		public boolean isSwitch() {
			return false;
		}

		public abstract String getString();

		public abstract void setString(String argument);

		public T get() {
			return this.value;
		}

		public void set(T value) {
			this.value = value;
		}
	}

	public class LaunchParameterFile extends LaunchParameter<File> {
		public LaunchParameterFile(String name) {
			super(name);
		}

		public LaunchParameterFile(String name, File defaultValue) {
			super(name, defaultValue);
		}

		public LaunchParameterFile(String name, File defaultValue, boolean wrapper) {
			super(name, defaultValue, wrapper);
		}

		@Override
		public LaunchParameterFile altName(String altName) {
			super.altName(altName);
			return this;
		}

		@Override
		public String getString() {
			if (value == null) {
				return null;
			}
			return this.value.getAbsolutePath();
		}

		@Override
		public void setString(String argument) {
			this.value = new File(argument);
		}
	}

	public class LaunchParameterFileList extends LaunchParameter<File[]> {
		public LaunchParameterFileList(String name) {
			super(name);
		}

		public LaunchParameterFileList(String name, File[] defaultValue) {
			super(name, defaultValue);
		}

		public LaunchParameterFileList(String name, File[] defaultValue, boolean wrapper) {
			super(name, defaultValue, wrapper);
		}

		@Override
		public String getString() {
			if (value == null) {
				return null;
			}
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < value.length; i++) {
				if (i != 0) {
					s.append(File.pathSeparator);
				}
				s.append(value[i].getAbsolutePath());
			}
			return s.toString();
		}

		@Override
		public void setString(String argument) {
			String[] paths = argument.split(Pattern.quote(File.pathSeparator));
			File[] newValue = new File[paths.length];
			for (int i = 0; i < paths.length; i++) {
				newValue[i] = new File(paths[i]);
			}
			this.value = newValue;
		}
	}

	public class LaunchParameterSwitchReverse extends LaunchParameterSwitch {
		LaunchParameterSwitch parent;

		public LaunchParameterSwitchReverse(String name, LaunchParameterSwitch parent) {
			super(name, !parent.defaultValue, true);
			this.parent = parent;
		}

		@Override
		public void set(Boolean b) {
			super.set(b);
			parent.value = !value;
		}

		@Override
		public Boolean get() {
			return (!parent.value);
		}
	}

	public class LaunchParameterSwitch extends LaunchParameter<Boolean> {
		public LaunchParameterSwitch(String name) {
			super(name, false);
		}

		public LaunchParameterSwitch(String name, Boolean defaultVal) {
			super(name, defaultVal);
		}

		public LaunchParameterSwitch(String name, Boolean defaultVal, boolean wrapper) {
			super(name, defaultVal, wrapper);
		}

		@Override
		public boolean isSwitch() {
			return true;
		}

		@Override
		public String getString() {
			return Boolean.toString(value);
		}

		@Override
		public void setString(String argument) {
			this.value = "true".equalsIgnoreCase(argument);
		}

		public void setFlag() {
			this.set(true);
		}

		public void toggle() {
			this.set(!value);
		}
	}

	public class LaunchParameterString extends LaunchParameter<String> {
		public LaunchParameterString(String name) {
			super(name);
		}

		public LaunchParameterString(String name, String defaultValue) {
			super(name, defaultValue);
		}

		public LaunchParameterString(String name, String defaultValue, boolean wrapper) {
			super(name, defaultValue, wrapper);
		}

		@Override
		public LaunchParameterString altName(String altName) {
			super.altName(altName);
			return this;
		}

		@Override
		public String getString() {
			return value;
		}

		@Override
		public void setString(String argument) {
			this.value = argument;
		}
	}

	public class LaunchParameterNumber extends LaunchParameter<Integer> {
		public LaunchParameterNumber(String name) {
			super(name);
		}

		public LaunchParameterNumber(String name, Integer defaultValue) {
			super(name, defaultValue);
		}

		public LaunchParameterNumber(String name, Integer defaultValue, boolean wrapper) {
			super(name, defaultValue, wrapper);
		}

		@Override
		public String getString() {
			return value.toString();
		}

		@Override
		public void setString(String argument) {
			this.value = Integer.parseInt(argument);
		}
	}

	public class LaunchParameterSkinOptions extends LaunchParameter<List<SkinOption>> {
		public LaunchParameterSkinOptions(String name) {
			super(name, Collections.<SkinOption>emptyList(), true);
		}

		@Override
		public String getString() {
			if (value == null) {
				return null;
			}
			StringBuilder s = new StringBuilder();
			int i = 0;
			for (SkinOption option : value) {
				if (i != 0) {
					s.append(',');
				}
				s.append(option.name);
				i++;
			}
			return s.toString();
		}

		@Override
		public void setString(String argument) {
			String[] optionStrings = argument.split(Pattern.quote(","));
			ArrayList<SkinOption> newValue = new ArrayList<SkinOption>();
			for (String optionString : optionStrings) {
				SkinOption opt = SkinOption.getEnum(optionString);
				if (opt != null) {
					newValue.add(opt);
				}
			}
			this.value = newValue;
		}
	}

	public class LaunchParameterEnum<T extends Enum<T>> extends LaunchParameter<T> {
		private final Class<T> enumType;

		@SuppressWarnings("unchecked")
		public LaunchParameterEnum(String name, Enum<T> defaultValue) {
			super(name, (T)defaultValue);
			enumType = defaultValue.getDeclaringClass();
		}

		@SuppressWarnings("unchecked")
		public LaunchParameterEnum(String name, Enum<T> defaultValue, boolean wrapper) {
			super(name, (T)defaultValue, wrapper);
			enumType = defaultValue.getDeclaringClass();
		}

		@Override
		public String getString() {
			return value.toString();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setString(String argument) {
			try {
				Method getEnumFromString = enumType.getDeclaredMethod("getEnum", String.class);
				if ((getEnumFromString.getModifiers() & Modifier.STATIC) != 0) {
					this.value = (T)getEnumFromString.invoke(null, argument);
					return;
				}
			} catch (Exception ignored) {
			}
			this.value = Enum.valueOf(enumType, argument);
		}
	}
}
