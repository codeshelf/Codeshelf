package com.gadgetworks.codeshelf.util;

import java.util.Map;

public class MemoryConfiguration extends ConfigurationABC {

	private final Map<String, String> configuration;
	
	public MemoryConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public String getString(String name) {
		return configuration.get(name);
	}

}
