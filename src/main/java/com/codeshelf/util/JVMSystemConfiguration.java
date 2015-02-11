package com.gadgetworks.codeshelf.util;

public class JVMSystemConfiguration extends ConfigurationABC {
	
	@Override
	public String getString(String name) {
		String stringValue = System.getProperty(name);
		return stringValue;
	}

}
