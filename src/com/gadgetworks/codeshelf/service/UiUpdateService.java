package com.gadgetworks.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;

// --------------------------------------------------------------------------
/**
 * This is a relatively unstructured collection of update methods that the UI may call.
 * There are other specialized services: LightService, PropertyService, WorkService
 * Much easier to add a new function to this than to create a new service.
 */
public class UiUpdateService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UiUpdateService.class);

	public void UiUpdateService() {
	}

	// --------------------------------------------------------------------------
	/**
	 * Throw InputValidationException to make proper response to go back to UI. Avoid other throws.
	 */
	public void updateItemLocation() {
	}

	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. Extensively used in JUnit testing, so will not log. Caller should log.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void updateCheEdits() {		
	}


}
