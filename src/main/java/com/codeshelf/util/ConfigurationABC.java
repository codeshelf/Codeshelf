package com.codeshelf.util;

public abstract class ConfigurationABC implements IConfiguration {
	
	@Override
	public int getInt(String name) {
		String stringValue = getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		int value = Integer.parseInt(stringValue);
		return value;
	}

	@Override
	public boolean getBoolean(String name) {
		String stringValue = getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		boolean value = Boolean.parseBoolean(stringValue);
		return value;
	}

	@Override
	public String getString(String name, String defaultValue) {
		String stringValue = getString(name);
		if (stringValue==null) {
			return defaultValue;
		}
		return stringValue;
	}

	@Override
	public int getInt(String name, int defaultValue) {
		String stringValue = getString(name);
		if (stringValue==null) {
			return defaultValue;
		}
		int value = Integer.parseInt(stringValue);
		return value;
	}

	@Override
	public Byte getByte(String name) {
		String stringValue = getString(name);
		if (stringValue==null) {
			throw new RuntimeException("Property is not defined: "+name);
		}
		Byte value = Byte.parseByte(stringValue);
		return value;
	}

	@Override
	public boolean getBoolean(String name, boolean defaultValue) {
		String stringValue = getString(name);
		if (stringValue==null) {
			return defaultValue;
		}
		boolean value = Boolean.parseBoolean(stringValue);
		return value;
	}
}
