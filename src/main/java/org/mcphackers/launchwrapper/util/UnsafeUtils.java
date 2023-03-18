package org.mcphackers.launchwrapper.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class UnsafeUtils {
	private static final Unsafe theUnsafe = getUnsafe();

	private static Unsafe getUnsafe() {
		try {
			final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			final Unsafe unsafe = (Unsafe) unsafeField.get(null);
			return unsafe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Static set

	public static void setStaticBoolean(final Field field, boolean value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putBoolean(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticInt(final Field field, int value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putInt(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticLong(final Field field, long value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putLong(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticFloat(final Field field, float value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putFloat(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticDouble(final Field field, double value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putDouble(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticObject(final Field field, Object value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putObject(staticFieldBase, staticFieldOffset, value);
	}
	
	// Non-static set

	public static void setBoolean(final Object base, final Field field, boolean value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putBoolean(base, staticFieldOffset, value);
	}

	public static void setInt(final Object base, final Field field, int value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putInt(base, staticFieldOffset, value);
	}

	public static void setLong(final Object base, final Field field, long value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putLong(base, staticFieldOffset, value);
	}

	public static void setFloat(final Object base, final Field field, float value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putFloat(base, staticFieldOffset, value);
	}

	public static void setDouble(final Object base, final Field field, double value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putDouble(base, staticFieldOffset, value);
	}

	public static void setObject(final Object base, final Field field, Object value) {
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putObject(base, staticFieldOffset, value);
	}
	
	// Static get

	public static boolean getStaticBoolean(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getBoolean(staticFieldBase, staticFieldOffset);
	}

	public static int getStaticInt(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getInt(staticFieldBase, staticFieldOffset);
	}

	public static long getStaticLong(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getLong(staticFieldBase, staticFieldOffset);
	}

	public static float getStaticFloat(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getFloat(staticFieldBase, staticFieldOffset);
	}

	public static double getStaticDouble(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getDouble(staticFieldBase, staticFieldOffset);
	}

	public static Object getStaticObject(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getObject(staticFieldBase, staticFieldOffset);
	}
	
	// Non-static get

	public static boolean getBoolean(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getBoolean(base, fieldOffset);
	}

	public static int getInt(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getInt(base, fieldOffset);
	}

	public static long getLong(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getLong(base, fieldOffset);
	}

	public static float getFloat(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getFloat(base, fieldOffset);
	}

	public static double getDouble(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getDouble(base, fieldOffset);
	}

	public static Object getObject(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getObject(base, fieldOffset);
	}
}
