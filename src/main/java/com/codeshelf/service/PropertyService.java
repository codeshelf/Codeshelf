package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;

public class PropertyService extends AbstractPropertyService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PropertyService.class);
	
	@Inject
	private static IPropertyService theInstance = null;

	@Inject
	public PropertyService() {
	}
	public static IPropertyService getMaybeRunningInstance() {
		return theInstance;
	}
	public static boolean exists() {
		return (theInstance != null);
	}
	public static IPropertyService getInstance() {
		// not self initializing, better static inject it first...
		ServiceUtility.awaitRunningOrThrow(theInstance);
		return theInstance;
	}
	public static void setInstance(IPropertyService instance) {
		// for testing only!
		theInstance = instance;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * API to update one property from the UI. Add user login for logability?.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void changePropertyValueUI(final String inFacilityPersistId, final String inPropertyName, final String inNewStringValue) {
		LOGGER.info("call to update property " + inPropertyName + " to " + inNewStringValue);
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		
		Facility facility = Facility.staticGetDao().findByPersistentId(tenant,inFacilityPersistId);
		if (facility == null) {
			DefaultErrors errors = new DefaultErrors(DomainObjectProperty.class);
			String instruction = "unknown facility";
			errors.rejectValue(inNewStringValue, instruction, ErrorCode.FIELD_INVALID); // "{0} invalid. {1}"
			throw new InputValidationException(errors);
		}
		changePropertyValue(tenant,facility, inPropertyName, inNewStringValue);
	}	
	
	@Override
	public DomainObjectProperty getProperty(Tenant tenant,IDomainObject object, String name) {
		if(object == null) {
			LOGGER.error("getProperty object was null");
			return null;
		}
		PropertyDao theDao = PropertyDao.getInstance();
		if (theDao == null) {
			LOGGER.error("getPropertyObject called before DAO exists");
			return null;
		}
		DomainObjectProperty prop = theDao.getPropertyWithDefault(tenant,object, name);
		if (prop == null) {
			LOGGER.error("Unknown property {} in getPropertyObject()",name);
			return null;
		}
		return prop;
	}

	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. 
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	@Override
	public void changePropertyValue(Tenant tenant,final Facility inFacility, final String inPropertyName, final String inNewStringValue) {		
		PropertyDao theDao = PropertyDao.getInstance();

		DomainObjectProperty theProperty = theDao.getPropertyWithDefault(tenant,inFacility, inPropertyName);
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
		theDao.store(tenant,theProperty);
	}

}
