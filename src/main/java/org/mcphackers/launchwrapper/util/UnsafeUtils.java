package org.mcphackers.launchwrapper.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

@SuppressWarnings("sunapi")
public final class UnsafeUtils {
	private static final Unsafe theUnsafe = getUnsafe();

	private UnsafeUtils() {
	}

	private static Unsafe getUnsafe() {
		try {
			final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			final Unsafe unsafe = (Unsafe)unsafeField.get(null);
			return unsafe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static void ensureClassInitialized(Class<?> clazz) {
		theUnsafe.ensureClassInitialized(clazz);
	}

	// Static set

	public static void setStaticBoolean(final Field field, boolean value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putBoolean(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticByte(final Field field, byte value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putByte(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticShort(final Field field, short value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putShort(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticChar(final Field field, char value) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		theUnsafe.putChar(staticFieldBase, staticFieldOffset, value);
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
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putBoolean(base, fieldOffset, value);
	}

	public static void setByte(final Object base, final Field field, byte value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putByte(base, fieldOffset, value);
	}

	public static void setShort(final Object base, final Field field, short value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putShort(base, fieldOffset, value);
	}

	public static void setChar(final Object base, final Field field, char value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putChar(base, fieldOffset, value);
	}

	public static void setInt(final Object base, final Field field, int value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putInt(base, fieldOffset, value);
	}

	public static void setLong(final Object base, final Field field, long value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putLong(base, fieldOffset, value);
	}

	public static void setFloat(final Object base, final Field field, float value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putFloat(base, fieldOffset, value);
	}

	public static void setDouble(final Object base, final Field field, double value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putDouble(base, fieldOffset, value);
	}

	public static void setObject(final Object base, final Field field, Object value) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		theUnsafe.putObject(base, fieldOffset, value);
	}

	// Static get

	public static boolean getStaticBoolean(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getBoolean(staticFieldBase, staticFieldOffset);
	}

	public static byte getStaticByte(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getByte(staticFieldBase, staticFieldOffset);
	}

	public static short getStaticShort(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getShort(staticFieldBase, staticFieldOffset);
	}

	public static char getStaticChar(final Field field) {
		final Object staticFieldBase = theUnsafe.staticFieldBase(field);
		final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
		return theUnsafe.getChar(staticFieldBase, staticFieldOffset);
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

	public static byte getByte(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getByte(base, fieldOffset);
	}

	public static short getShort(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getShort(base, fieldOffset);
	}

	public static char getChar(final Object base, final Field field) {
		final long fieldOffset = theUnsafe.objectFieldOffset(field);
		return theUnsafe.getChar(base, fieldOffset);
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
