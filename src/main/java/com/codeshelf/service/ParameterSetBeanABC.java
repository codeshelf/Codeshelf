package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class ParameterSetBeanABC {
	private static final Logger	LOGGER					= LoggerFactory.getLogger(ParameterSetBeanABC.class);
	
	public ParameterSetBeanABC() {
		super();
	}
	
	public String getParametersDescription() {
		return "need to implement getParametersDescription";
	}

	protected int getCleanValue(String fieldName, String inValue, int defaultValue) {
		try {
			int value = Integer.valueOf(inValue);
			if (value < 1) {
				LOGGER.warn("bad value for {}: {}", fieldName, inValue);
				value = defaultValue;
			}
			return value;
		} catch (NumberFormatException e) {
			LOGGER.warn("bad value for {}: {}", fieldName, inValue);
			return defaultValue;
		}		
	}

}