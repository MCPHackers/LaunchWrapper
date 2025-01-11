package org.mcphackers.launchwrapper.protocol.skin;

public enum SkinOption {
	IGNORE_ALEX("ignoreAlex"),
	IGNORE_LAYERS("ignoreLayers"),
	REMOVE_HAT("removeHat"),
	USE_LEFT_ARM("useLeftArm"),
	USE_LEFT_LEG("useLeftLeg");

	public final String name;

	private SkinOption(String name) {
		this.name = name;
	}

	public static SkinOption getEnum(String name) {
		for (SkinOption skinType : values()) {
			if (skinType.name.equals(name)) {
				return skinType;
			}
		}
		return null;
	}
}
