package com.gadgetworks.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;

public class PropertyService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PropertyService.class);

	public void ProperyService() {

	}

	// --------------------------------------------------------------------------
	/**
	 * API to update one property from the UI. Add user login for logability?.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void changePropertyValue(final String inFacilityPersistId, final String inPropertyName, final String inNewStringValue) {
		LOGGER.info("call to update property " + inPropertyName + " to " + inNewStringValue);
		Facility facility = Facility.DAO.findByPersistentId(inFacilityPersistId);
		PropertyDao theDao = (PropertyDao) DomainObjectProperty.DAO;

		DomainObjectProperty theProperty = theDao.getOrCreateProperty(facility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property");
			return;
		}

		// The UI may have passed a string that is close enough. But we want to force it to our canonical forms.
		String canonicalForm = theProperty.toCanonicalForm(inNewStringValue);

		String validationResult = theProperty.validateNewStringValue(inNewStringValue, canonicalForm);
		// null means no error
		if (validationResult != null) {
			LOGGER.warn("Property validation rejection: " + validationResult);
			DefaultErrors errors = new DefaultErrors(DomainObjectProperty.class);
			String instruction = inPropertyName + " valid values: " + theProperty.validInputValues();
			errors.rejectValue(inNewStringValue, instruction, ErrorCode.FIELD_INVALID); // "{0} invalid. {1}"
			throw new InputValidationException(errors);
		}

		// storing the string version, so type does not matter. We assume all validation happened so the value is ok to go to the database.
		theProperty.setValue(canonicalForm);
		theDao.store(theProperty);
		
		// May need to update some local cached version of the changed config
		updateWhereNeeded(theProperty);
	}
	
	private void updateWhereNeeded(DomainObjectProperty inProperty){
		// HousekeepingInjector has a local cache for two of them.
		String propertyName = inProperty.getName();
		if (propertyName.equals(DomainObjectProperty.BAYCHANG) || propertyName.equals(DomainObjectProperty.RPEATPOS))
			HousekeepingInjector.setValuesFromConfigs();		
		else if (propertyName.equals(DomainObjectProperty.LIGHTSEC) || propertyName.equals(DomainObjectProperty.LIGHTCLR)){
			// find the LightService. How? LightService.setValuesFromConfigs();
			LOGGER.error("updateWhereNeeded() needs to find the LightService to set these local values.");
		}
	}
	
	/**
	 * Convenient API for application code.
	 */	
	public static String getPropertyFromConfig(final Facility inFacility, final String inPropertyName){
		PropertyDao theDao = (PropertyDao) DomainObjectProperty.DAO;
		if (theDao == null || inFacility == null) {
			LOGGER.error("getPropertyFromConfig called before DAO or facility exists");
			return null;
		}
		
		DomainObjectProperty theProperty = theDao.getOrCreateProperty(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return null;
		}
		// the toCanonicalForm() call here ensures that small typos in the liquidbase xml get rectified to the coded canonical forms.
		return theProperty.toCanonicalForm(theProperty.getValue());
	}

}
