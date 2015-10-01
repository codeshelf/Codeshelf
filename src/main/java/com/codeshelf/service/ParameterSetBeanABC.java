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

	/**
	 * See callers for usage. This is useful only for integer values represented by strings in our groovy extension.
	 * pass in "fieldname" which should match the String field of the bean. Used only for logging problems in a way useful to user/administrator
	 * inValue is the value from the getter for the field.
	 * defaultValue is the value to use if there is a problem
	 */
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