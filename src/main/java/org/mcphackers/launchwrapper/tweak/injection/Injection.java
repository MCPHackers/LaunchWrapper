package org.mcphackers.launchwrapper.tweak.injection;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

/**
 * The base interface for any Injection
 */
public interface Injection {

	/**
	 * Used when printing information about applied tweaks
	 *
	 * @return Injection name
	 */
	String name();

	/**
	 * @return Whenever failure to apply this injection should prevent game execution
	 */
	boolean required();

	/**
	 * Called when ClassNodeSource is being transformed
	 * For any modified class you must call source.overrideClass(node);
	 *
	 * @param source
	 * @param config
	 * @return whenever this injection succeeded to apply. Depending on injection, you may return true if the injection got partially applied.
	 */
	boolean apply(ClassNodeSource source, LaunchConfig config);
}
