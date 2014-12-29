package com.gadgetworks.codeshelf.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PropertyService implements IApiService {

	private static final Logger				LOGGER							= LoggerFactory.getLogger(PropertyService.class);


	public void ProperyService() {

	}

	// --------------------------------------------------------------------------
	/**
	 * API to update one property. Add facility persistentId? Add user login.
	 */
	public void changePropertyValue(final String inPropertyName, final String inNewStringValue) {
		LOGGER.info("call to update property " + inPropertyName + " to " + inNewStringValue);
	}

	
}
