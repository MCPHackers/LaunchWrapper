package org.mcphackers.launchwrapper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.launchwrapper.protocol.SkinOption;
import org.mcphackers.launchwrapper.protocol.SkinType;
import org.mcphackers.launchwrapper.util.OS;

public class LaunchConfig {
	private static final File defaultGameDir = getDefaultGameDir();
	private Map<String, LaunchParameter<?>> parameters = new HashMap<String, LaunchParameter<?>>();
	public Map<String, Object> unknownParameters = new HashMap<String, Object>();

	public LaunchParameterSwitch demo 				= new LaunchParameterSwitch("demo", false);
	public LaunchParameterSwitch fullscreen 		= new LaunchParameterSwitch("fullscreen", false);
	public LaunchParameterSwitch checkGlErrors 		= new LaunchParameterSwitch("checkGlErrors", false);
	public LaunchParameterString server 			= new LaunchParameterString("server");
	public LaunchParameterNumber port 				= new LaunchParameterNumber("port", 25565);
	public LaunchParameterFile gameDir 				= new LaunchParameterFile("gameDir", defaultGameDir).altName("workDir");
	public LaunchParameterFile assetsDir 			= new LaunchParameterFile("assetsDir");
	public LaunchParameterFile resourcePackDir 		= new LaunchParameterFile("resourcePackDir");
	public LaunchParameterString proxyHost 			= new LaunchParameterString("proxyHost");
	public LaunchParameterNumber proxyPort 			= new LaunchParameterNumber("proxyPort", 8080);
	public LaunchParameterString proxyUser 			= new LaunchParameterString("proxyUser");
	public LaunchParameterString proxyPass 			= new LaunchParameterString("proxyPass");
	public LaunchParameterString username 			= new LaunchParameterString("username", "Player" + System.currentTimeMillis() % 1000L);
	public LaunchParameterString session 			= new LaunchParameterString("session", null).altName("sessionid");
	public LaunchParameterString password 			= new LaunchParameterString("password");
	public LaunchParameterString uuid 				= new LaunchParameterString("uuid");
	public LaunchParameterString accessToken 		= new LaunchParameterString("accessToken");
	public LaunchParameterString version 			= new LaunchParameterString("version");
	public LaunchParameterNumber width 				= new LaunchParameterNumber("width", 854);
	public LaunchParameterNumber height 			= new LaunchParameterNumber("height", 480);
	public LaunchParameterString userProperties 	= new LaunchParameterString("userProperties");
	public LaunchParameterString profileProperties 	= new LaunchParameterString("profileProperties");
	public LaunchParameterString assetIndex 		= new LaunchParameterString("assetIndex");
	public LaunchParameterString userType 			= new LaunchParameterString("userType");
	public LaunchParameterString versionType 		= new LaunchParameterString("versionType");
	public LaunchParameterSwitch applet 			= new LaunchParameterSwitch("applet", false);
	public LaunchParameterSwitch haspaid 			= new LaunchParameterSwitch("haspaid", true);
	public LaunchParameterString loadmap_user 		= new LaunchParameterString("loadmap_user");
	public LaunchParameterNumber loadmap_id 		= new LaunchParameterNumber("loadmap_id");
	public LaunchParameterString mppass 			= new LaunchParameterString("mppass", "");
	public LaunchParameterSwitch lwjglFrame 		= new LaunchParameterSwitch("awtFrame", true, true);
	public LaunchParameterSwitch isom 				= new LaunchParameterSwitch("isom", false, true);
	public LaunchParameterSwitch forceVsync 		= new LaunchParameterSwitch("forceVsync", false, true);
	public LaunchParameterSwitch forceResizable 	= new LaunchParameterSwitch("forceResizable", false, true);
	public LaunchParameterEnum<SkinType> skinProxy 	= new LaunchParameterEnum<SkinType>("skinProxy", SkinType.DEFAULT, true);
	public LaunchParameterString serverURL 			= new LaunchParameterString("serverURL", null, true);
	public LaunchParameterString serverSHA1 		= new LaunchParameterString("serverSHA1", null, true);
	public LaunchParameterFileList icon 			= new LaunchParameterFileList("icon", null, true);
	public LaunchParameterString title 				= new LaunchParameterString("title", null, true);
	public LaunchParameterSwitch oneSixFlag 		= new LaunchParameterSwitch("oneSixFlag", false, true);
	public LaunchParameterString tweakClass 		= new LaunchParameterString("tweakClass", null, true);
	public LaunchParameterSwitch discordRPC 		= new LaunchParameterSwitch("discordRPC", false, true);

	private static File getDefaultGameDir() {
		String game = "minecraft";
		String homeDir = System.getProperty("user.home", ".");
		File gameDir;
		switch(OS.getOs()) {
			case LINUX:
			case SOLARIS:
				String dataHome = System.getProperty("XDG_DATA_HOME");
				if(dataHome != null) {
					gameDir = new File(dataHome, "." + game + '/');
				} else {
					gameDir = new File(homeDir, ".local/share/" + game + '/');
				}
				break;
			case WINDOWS:
				String appdata = System.getenv("APPDATA");
				if(appdata != null) {
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

		if(!gameDir.exists() && !gameDir.mkdirs()) {
			throw new RuntimeException("The working directory could not be created: " + gameDir);
		} else {
			return gameDir;
		}
	}

	public LaunchConfig() {
	}

	public LaunchConfig(String[] args) {
		for(int i = 0; i < args.length; i++) {
			if(args[i].startsWith("--")) {
				String paramName = args[i].substring(2);
				LaunchParameter<?> param = parameters.get(paramName);
				if(param == null) {
					if(i + 1 >= args.length || args[i+1].startsWith("--")) {
						unknownParameters.put(paramName, Boolean.TRUE);
					} else {
						unknownParameters.put(paramName, args[i+1]);
						i++;
					}
					continue;
				}
				if(param.isSwitch()) {
					((LaunchParameterSwitch) param).setFlag();
					continue;
				}
				if(i + 1 < args.length) {
					try {
						param.setString(args[i + 1]);
						i++;
					} catch (IllegalArgumentException e) {
					}
				}
			}
		}
	}

	public List<LaunchParameter<?>> getParamsAsList() {
		List<LaunchParameter<?>> list = new ArrayList<LaunchParameter<?>>();
		for(LaunchParameter<?> param : parameters.values()) {
			if(!param.wrapperOnly && param.get() != null && param.get() != Boolean.FALSE) {
				list.add(param);
			}
		}
		return list;
	}

	public Map<String, String> getArgsAsMap() {
		Map<String, String> map = new HashMap<String, String>();
		for(String key : parameters.keySet()) {
			LaunchParameter<?> param = parameters.get(key);
			if(!param.wrapperOnly && param.get() != null && param.get() != Boolean.FALSE) {
				map.put(key, param.getString());
			}
		}
		return map;
	}

	public String[] getArgs() {
		List<String> list = new ArrayList<String>();
		for(String key : parameters.keySet()) {
			LaunchParameter<?> param = parameters.get(key);
			if(!param.wrapperOnly && param.get() != null) {
				if(param.isSwitch()) {
					if(param.get().equals(true))
						list.add("--" + key);
				} else {
					list.add("--" + key);
					list.add(param.getString());
				}
			}
		}
		for(String paramName : unknownParameters.keySet()) {
			if(paramName == null) {
				continue;
			}
			Object value = unknownParameters.get(paramName); // Either Boolean.TRUE or String
			if(value == Boolean.TRUE) {
				list.add("--" + paramName);
				continue;
			}
			if(value != null) {
				list.add("--" + paramName);
				list.add(value.toString());
			}
		}
		String[] arr = new String[list.size()];
		return list.toArray(arr);
	}

	public abstract class LaunchParameter<T> {
		public final String name;
		public final boolean wrapperOnly;

		protected LaunchParameter(String name) {
			this(name, false);
		}

		protected LaunchParameter(String name, boolean wrapper) {
			this.name = name;
			this.wrapperOnly = wrapper;
			// do not toLowerCase. Parameters names are case sensitive
			parameters.put(name, this);
		}

		public LaunchParameter<T> altName(String altName) {
			parameters.put(altName, this);
			return this;
		} 

		public boolean isSwitch() {
			return false;
		}

		public abstract String getString();

		public abstract void setString(String argument);

		public abstract T get();

		public abstract void set(T value);
	}

	public class LaunchParameterFile extends LaunchParameter<File> {
		private File value;

		public LaunchParameterFile(String name) {
			super(name);
		}

		public LaunchParameterFile(String name, File defaultValue) {
			super(name);
			value = defaultValue;
		}

		public LaunchParameterFile(String name, File defaultValue, boolean wrapper) {
			super(name, wrapper);
			value = defaultValue;
		}

		public LaunchParameterFile altName(String altName) {
			return (LaunchParameterFile)super.altName(altName);
		}

		@Override
		public String getString() {
			if(value == null)
				return null;
			return this.value.getAbsolutePath();
		}

		@Override
		public void setString(String argument) {
			this.value = new File(argument);
		}

		@Override
		public File get() {
			return this.value;
		}

		@Override
		public void set(File value) {
			this.value = value;
		}
	}

	public class LaunchParameterFileList extends LaunchParameter<File[]> {
		private File[] value;

		public LaunchParameterFileList(String name) {
			super(name);
		}

		public LaunchParameterFileList(String name, File[] defaultValue) {
			super(name);
			value = defaultValue;
		}

		public LaunchParameterFileList(String name, File[] defaultValue, boolean wrapper) {
			super(name, wrapper);
			value = defaultValue;
		}

		public LaunchParameterFileList altName(String altName) {
			return (LaunchParameterFileList)super.altName(altName);
		}

		@Override
		public String getString() {
			if(value == null)
				return null;
			String s = "";
			for(int i = 0; i < value.length; i++) {
				if(i != 0) {
					s += ";";
				}
				s += value[i].getAbsolutePath();
			}
			return s;
		}

		@Override
		public void setString(String argument) {
			String[] paths = argument.split(";");
			File[] newValue = new File[paths.length];
			for(int i = 0; i < paths.length; i++) {
				newValue[i] = new File(paths[i]);
			}
			this.value = newValue;
		}

		@Override
		public File[] get() {
			return this.value;
		}

		@Override
		public void set(File[] value) {
			this.value = value;
		}
	}

	public class LaunchParameterSwitch extends LaunchParameter<Boolean> {
		private boolean value;
		private boolean defaultValue;

		public LaunchParameterSwitch(String name) {
			super(name);
		}

		public LaunchParameterSwitch(String name, Boolean defaultVal) {
			super(name);
			defaultValue = defaultVal;
			value = defaultVal;
		}

		public LaunchParameterSwitch(String name, Boolean defaultVal, boolean wrapper) {
			super(name, wrapper);
			defaultValue = defaultVal;
			value = defaultVal;
		}

		public LaunchParameterSwitch altName(String altName) {
			return (LaunchParameterSwitch)super.altName(altName);
		}

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

		@Override
		public Boolean get() {
			return this.value;
		}

		@Override
		public void set(Boolean value) {
			this.value = value == null ? false : value;
		}

		public void setFlag() {
			this.value = !defaultValue;
		}

		public void toggle() {
			this.value = !value;
		}
	}

	public class LaunchParameterString extends LaunchParameter<String> {
		private String value;

		public LaunchParameterString(String name) {
			super(name);
		}

		public LaunchParameterString(String name, String defaultValue) {
			super(name);
			value = defaultValue;
		}

		public LaunchParameterString(String name, String defaultValue, boolean wrapper) {
			super(name, wrapper);
			value = defaultValue;
		}

		public LaunchParameterString altName(String altName) {
			return (LaunchParameterString)super.altName(altName);
		}

		@Override
		public String getString() {
			return value;
		}

		@Override
		public void setString(String argument) {
			this.value = argument;
		}

		@Override
		public String get() {
			return this.value;
		}

		@Override
		public void set(String value) {
			this.value = value;
		}
	}

	public class LaunchParameterNumber extends LaunchParameter<Integer> {
		private Integer value;

		public LaunchParameterNumber(String name) {
			super(name);
		}

		public LaunchParameterNumber(String name, Integer defaultValue) {
			super(name);
			value = defaultValue;
		}

		public LaunchParameterNumber(String name, Integer defaultValue, boolean wrapper) {
			super(name, wrapper);
			value = defaultValue;
		}

		@Override
		public String getString() {
			return value.toString();
		}

		@Override
		public void setString(String argument) {
			this.value = Integer.parseInt(argument);
		}

		@Override
		public Integer get() {
			return this.value;
		}

		@Override
		public void set(Integer value) {
			this.value = value;
		}
	}

	public class LaunchParameterEnum<T extends Enum<T>> extends LaunchParameter<T> {
		private T value;
		private Class<T> enumType;

		@SuppressWarnings("unchecked")
		public LaunchParameterEnum(String name, Enum<T> defaultValue) {
			super(name);
			enumType = defaultValue.getDeclaringClass();
			value = (T)defaultValue;
		}

		@SuppressWarnings("unchecked")
		public LaunchParameterEnum(String name, Enum<T> defaultValue, boolean wrapper) {
			super(name, wrapper);
			enumType = defaultValue.getDeclaringClass();
			value = (T)defaultValue;
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
				if((getEnumFromString.getModifiers() & Modifier.STATIC) != 0) {
					this.value = (T)getEnumFromString.invoke(null, argument);
					return;
				}
			} catch (Exception e) {
			}
			this.value = Enum.valueOf(enumType, argument);
		}

		@Override
		public T get() {
			return this.value;
		}

		@Override
		public void set(T value) {
			this.value = value;
		}
	}

}
