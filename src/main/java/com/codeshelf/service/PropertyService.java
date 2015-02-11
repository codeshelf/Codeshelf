package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;

public class PropertyService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PropertyService.class);

	public PropertyService() {
	}

	// --------------------------------------------------------------------------
	/**
	 * API to update one property from the UI. Add user login for logability?.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void changePropertyValueUI(final String inFacilityPersistId, final String inPropertyName, final String inNewStringValue) {
		LOGGER.info("call to update property " + inPropertyName + " to " + inNewStringValue);
		Facility facility = Facility.DAO.findByPersistentId(inFacilityPersistId);
		if (facility == null) {
			DefaultErrors errors = new DefaultErrors(DomainObjectProperty.class);
			String instruction = "unknown facility";
			errors.rejectValue(inNewStringValue, instruction, ErrorCode.FIELD_INVALID); // "{0} invalid. {1}"
			throw new InputValidationException(errors);
		}
		changePropertyValue(facility, inPropertyName, inNewStringValue);
	}

	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. Extensively used in JUnit testing, so will not log. Caller should log.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void changePropertyValue(final Facility inFacility, final String inPropertyName, final String inNewStringValue) {		
		PropertyDao theDao = PropertyDao.getInstance();

		DomainObjectProperty theProperty = theDao.getPropertyWithDefault(inFacility, inPropertyName);
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
	}

	/**
	 * Necessary convenience APIs to change Housekeeping config values. Should the be on the HousekeepingService? Does not seem right.
	 */
	public void setBayChangeChoice(Facility inFacility, BayChangeChoice inBayChangeChoice) {
		switch(inBayChangeChoice){
			case BayChangeNone:
				changePropertyValue(inFacility, DomainObjectProperty.BAYCHANG, "None");
				break;
			case BayChangeBayChange:
				changePropertyValue(inFacility, DomainObjectProperty.BAYCHANG, "BayChange");
				break;
			case BayChangePathSegmentChange:
				changePropertyValue(inFacility, DomainObjectProperty.BAYCHANG, "PathSegmentChange");
				break;
			case BayChangeExceptSamePathDistance:
				changePropertyValue(inFacility, DomainObjectProperty.BAYCHANG, "BayChangeExceptAcrossAisle");
				break;
			default:
				LOGGER.error("unknown value in setBayChangeChoice");
		}
	}
	
	public void setRepeatPosChoice(Facility inFacility, RepeatPosChoice inRepeatPosChoice) {
		switch(inRepeatPosChoice){
			case RepeatPosNone:
				changePropertyValue(inFacility, DomainObjectProperty.RPEATPOS, "None");
				break;
			case RepeatPosContainerOnly:
				changePropertyValue(inFacility, DomainObjectProperty.RPEATPOS, "ContainerOnly");
				break;
			case RepeatPosContainerAndCount:
				changePropertyValue(inFacility, DomainObjectProperty.RPEATPOS, "ContainerAndCount");
				break;
			default:
				LOGGER.error("unknown value in setRepeatPosChoice");
		}
	}

	public void restoreHKDefaults(Facility inFacility) {
		setRepeatPosChoice(inFacility, RepeatPosChoice.RepeatPosContainerOnly);
		setBayChangeChoice(inFacility, BayChangeChoice.BayChangeBayChange);
	}
	public void turnOffHK(Facility inFacility) {
		setRepeatPosChoice(inFacility, RepeatPosChoice.RepeatPosNone);
		setBayChangeChoice(inFacility, BayChangeChoice.BayChangeNone);

	}

	/**
	 * Convenient API for application code.
	 */
	public static String getPropertyFromConfig(final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getPropertyObject(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return null;
		}
		// the toCanonicalForm() call here ensures that small typos in the liquidbase xml get rectified to the coded canonical forms.
		return theProperty.toCanonicalForm(theProperty.getValue());
	}

	public static boolean getBooleanPropertyFromConfig(final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getPropertyObject(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return false;
		}
		return theProperty.getBooleanValue();
	}
	/**
	 * Helper to avoid cloning.
	 */
	public static DomainObjectProperty getPropertyObject(final Facility inFacility, final String inPropertyName) {
		PropertyDao theDao = PropertyDao.getInstance();
		if (theDao == null || inFacility == null) {
			LOGGER.error("getPropertyObject called before DAO or facility exists");
			return null;
		}
		DomainObjectProperty theProperty = theDao.getPropertyWithDefault(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyObject()");
			return null;
		}
		return theProperty;
	}


	public DomainObjectProperty getProperty(IDomainObject object, String name) {
		PropertyDao theDao = PropertyDao.getInstance();
		DomainObjectProperty prop = theDao.getPropertyWithDefault(object, name);
		return prop;
	}

	public ColorEnum getPropertyAsColor(IDomainObject object, String name, ColorEnum defaultColor) {
		DomainObjectProperty prop = getProperty(object, name);
		if (prop == null) {
			return defaultColor;
		}
		return prop.getColorValue();
	}

	public int getPropertyAsInt(IDomainObject object, String name, int defaultValue) {
		DomainObjectProperty prop = getProperty(object, name);
		if (prop == null) {
			return defaultValue;
		}
		return prop.getIntValue();
	}

}
