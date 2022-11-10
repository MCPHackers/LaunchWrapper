package org.mcphackers.launchwrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Util {
	
	public static void setField(Object instance, Field field, Object value) {
		if(field != null) {
			try {
				field.set(instance, value);
			}
			catch (Exception e) { }
		}
	}
	
	public static boolean isStatic(Field field) {
		return (field.getModifiers() & Modifier.STATIC) != 0;
	}

}
