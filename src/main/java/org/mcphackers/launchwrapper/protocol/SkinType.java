package org.mcphackers.launchwrapper.protocol;

public enum SkinType {
	// clang-format off
    PRE_19A("pre-c0.0.19a"),     // Entire model is mirrored
    CLASSIC("classic"),          // Before c0.24_ST. Lack of head layer
    PRE_B1_9("pre-b1.9-pre4"),   // Before b1.9-pre4. Flipped bottom faces on all body parts
    PRE_1_8("pre-1.8"),          // Before 14w03a. Lack of skin layers
    // PRE_1_8_PRE("pre-1.8-pre1"),	  // Before 1.8-pre1. Lack of slim model (Use SkinOption.IGNORE_ALEX with pre-1.9)
    PRE_1_9("pre-1.9"),          // Before 15w47a. Lack of semi-transparency
    DEFAULT("default");          // Latest skin format
	// clang-format on

	public final String name;

	private SkinType(String name) {
		this.name = name;
	}

	public static SkinType getEnum(String name) {
		for (SkinType skinType : values()) {
			if (skinType.name.equals(name)) {
				return skinType;
			}
		}
		return null;
	}
}
