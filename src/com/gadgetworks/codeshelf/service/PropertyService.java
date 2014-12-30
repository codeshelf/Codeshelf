package com.gadgetworks.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;

public class PropertyService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PropertyService.class);

	public void ProperyService() {

	}

	// --------------------------------------------------------------------------
	/**
	 * API to update one property. Add user login for logability?.
	 * Let's not throw so that proper answer goes back to UI.
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
			return;
		}

		// stored the string version, so type does not matter. We assume all validation
		theProperty.setValue(canonicalForm);
		theDao.store(theProperty);
	}

}
