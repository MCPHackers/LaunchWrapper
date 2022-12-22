package org.mcphackers.launchwrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LaunchConfig {
	private Map<String, LaunchParameter<Object>> parameters = new HashMap<String, LaunchParameter<Object>>();
	public LaunchParameter<Boolean> demo = new LaunchParameter<Boolean>("demo", Boolean.class, false);
	public LaunchParameter<Boolean> fullscreen = new LaunchParameter<Boolean>("fullscreen", Boolean.class, false);
	public LaunchParameter<Boolean> checkGlErrors = new LaunchParameter<Boolean>("checkGlErrors", Boolean.class, false);
	public LaunchParameter<String> server = new LaunchParameter<String>("server", String.class);
	public LaunchParameter<Integer> port = new LaunchParameter<Integer>("port", Integer.class, 25565);
	public LaunchParameter<File> gameDir = new LaunchParameter<File>("gameDir", File.class);
	public LaunchParameter<File> assetsDir = new LaunchParameter<File>("assetsDir", File.class);
	public LaunchParameter<File> resourcePackDir = new LaunchParameter<File>("resourcePackDir", File.class);
	public LaunchParameter<String> proxyHost = new LaunchParameter<String>("proxyHost", String.class);
	public LaunchParameter<Integer> proxyPort = new LaunchParameter<Integer>("proxyPort", Integer.class, 8080);
	public LaunchParameter<String> proxyUser = new LaunchParameter<String>("proxyUser", String.class);
	public LaunchParameter<String> proxyPass = new LaunchParameter<String>("proxyPass", String.class);
	public LaunchParameter<String> username = new LaunchParameter<String>("username", String.class, "Player" + System.currentTimeMillis() % 1000L);
	public LaunchParameter<String> sessionid = new LaunchParameter<String>("sessionid", String.class, "-");
	public LaunchParameter<String> password = new LaunchParameter<String>("password", String.class);
	public LaunchParameter<String> uuid = new LaunchParameter<String>("uuid", String.class);
	public LaunchParameter<String> accessToken = new LaunchParameter<String>("accessToken", String.class);
	public LaunchParameter<String> version = new LaunchParameter<String>("version", String.class);
	public LaunchParameter<Integer> width = new LaunchParameter<Integer>("width", Integer.class, 854);
	public LaunchParameter<Integer> height = new LaunchParameter<Integer>("height", Integer.class, 480);
	public LaunchParameter<String> userProperties = new LaunchParameter<String>("userProperties", String.class, "{}");
	public LaunchParameter<String> profileProperties = new LaunchParameter<String>("profileProperties", String.class);
	public LaunchParameter<String> assetIndex = new LaunchParameter<String>("assetIndex", String.class);
	public LaunchParameter<String> userType = new LaunchParameter<String>("userType", String.class);
	public LaunchParameter<String> versionType = new LaunchParameter<String>("versionType", String.class);
	public LaunchParameter<Boolean> applet = new LaunchParameter<Boolean>("applet", Boolean.class, false);
	public LaunchParameter<Boolean> haspaid = new LaunchParameter<Boolean>("haspaid", Boolean.class, true);
	public LaunchParameter<Boolean> lwjglFrame = new LaunchParameter<Boolean>("lwjglFrame", Boolean.class, true, true);
	public LaunchParameter<Boolean> isom = new LaunchParameter<Boolean>("isom", Boolean.class, false, true);
	public LaunchParameter<Boolean> forceVsync = new LaunchParameter<Boolean>("forceVsync", Boolean.class, false, true);

	public LaunchConfig(String[] args) {
		int i = 0;
		while(i < args.length) {
			if(args[i].startsWith("--")) {
				String paramName = args[i].substring(2);
				LaunchParameter<Object> param = parameters.get(paramName);
				if(param != null && param.type == Boolean.class) {
					param.set(true);
				}
				else if(paramName.equalsIgnoreCase("awtFrame")) {
					lwjglFrame.set(false);
				}
				else if(i + 1 < args.length) {
					try {
						LaunchParameter<Object> param2 = parameters.get(paramName);
						if(param2 != null) {
							if(param.type == String.class) {
								param2.set(args[i + 1]);
							}
							else if(param.type == File.class) {
								param2.set(new File(args[i + 1]));
							}
							else if(param.type == Integer.class) {
								param2.set(Integer.valueOf(args[i + 1]));
							}
							i++;
						}
					} catch (IllegalArgumentException e) {}
				}
			}
			i++;
		}
	}

	public Map<String, String> getArgsAsMap() {
		Map<String, String> map = new HashMap<String, String>();
		for(Entry<String, LaunchParameter<Object>> param : parameters.entrySet()) {
			if(!param.getValue().wrapperOnly && param.getValue().value != null) {
				map.put(param.getKey(), param.getValue().getString());
			}
		}
		return map;
	}
	
	public String[] getArgs() {
		List<String> list = new ArrayList<String>();
		for(Entry<String, LaunchParameter<Object>> param : parameters.entrySet()) {
			if(!param.getValue().wrapperOnly && param.getValue().value != null) {
				if(param.getValue().type == Boolean.class) {
					if(param.getValue().get().equals(true))
						list.add("--" + param.getKey());
				} else {
					list.add("--" + param.getKey());
					list.add(param.getValue().getString());
				}
			}
		}
		String[] arr = new String[list.size()];
		return list.toArray(arr);
	}
	
	public class LaunchParameter<T> {
		private Class<?> type;
		private T value;
		public boolean wrapperOnly;
		
		public LaunchParameter(String name, Class<?> type) {
			this(name, type, null);
		}
		
		public LaunchParameter(String name, Class<?> type, T defaultValue, boolean wrapper) {
			this(name, type, defaultValue);
			wrapperOnly = wrapper;
		}
		
		@SuppressWarnings("unchecked")
		public LaunchParameter(String name, Class<?> type, T defaultValue) {
			this.type = type;
			this.value = defaultValue;
			parameters.put(name, (LaunchParameter<Object>) this);
		}
		
		public String getString() {
			if(value == null) return null;
			if(value instanceof File) {
				return ((File)value).getAbsolutePath();
			}
			return value.toString();
		}
		
		public T get() {
			return value;
		}
		
		public void set(T value) {
			this.value = value;
		}
	}

}
