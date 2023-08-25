package org.mcphackers.launchwrapper.tweak;

/**
 * Info about features applied by a tweak
 * Useful for tests
 */
public class FeatureInfo {
    public final String feature;
    public String[] info;

    public FeatureInfo(String name) {
        feature = name;
    }

    public FeatureInfo(String name, String[] additionalInfo) {
        feature = name;
        info = additionalInfo;
    }
}
