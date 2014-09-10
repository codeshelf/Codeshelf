package com.gadgetworks.codeshelf.util;

public class PropertyUtils {

	public static String getString(String name) {
		String stringValue = System.getProperty(name);
		return stringValue;
	}
	
	public static int getInt(String name) {
		String stringValue = PropertyUtils.getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		int value = Integer.parseInt(stringValue);
		return value;
	}

	public static boolean getBoolean(String name) {
		String stringValue = PropertyUtils.getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		boolean value = Boolean.parseBoolean(stringValue);
		return value;
	}

	public static String getString(String name, String defaultValue) {
		String stringValue = PropertyUtils.getString(name);
		if (stringValue==null) {
			// return default value
			return defaultValue;
		}
		return stringValue;
	}

	public static int getInt(String name, int defaultValue) {
		String stringValue = PropertyUtils.getString(name);
		if (stringValue==null) {
			// return default value
			return defaultValue;
		}
		int value = Integer.parseInt(stringValue);
		return value;
	}

	public static Byte getByte(String name) {
		String stringValue = PropertyUtils.getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		Byte value = Byte.parseByte(stringValue);
		return value;
	}
}
