package org.mcphackers.launchwrapper.protocol;

public enum SkinType {
	PRE_19A("pre-c0.0.19a"),
	CLASSIC("classic"),
	PRE_B1_9("pre-b1.9-pre4"),
	PRE_1_8("pre-1.8"),
	DEFAULT("default");

	private String name;

	private SkinType(String name) {
		this.name = name;
	}

	public static SkinType getEnum(String name) {
		for(SkinType skinType : values()) {
			if(skinType.name.equals(name)) {
				return skinType;
			}
		}
		return null;
	}
}