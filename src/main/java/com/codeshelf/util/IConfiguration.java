package com.gadgetworks.codeshelf.util;

public interface IConfiguration {

	public String getString(String name);
	
	public int getInt(String name);

	public boolean getBoolean(String name);

	public String getString(String name, String defaultValue);

	public int getInt(String name, int defaultValue);

	public Byte getByte(String name);

	public boolean getBoolean(String name, boolean defaultValue);

}
