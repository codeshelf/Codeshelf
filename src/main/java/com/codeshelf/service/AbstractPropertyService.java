package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;

public abstract class AbstractPropertyService extends AbstractCodeshelfIdleService implements IPropertyService {
	private final Logger LOGGER = LoggerFactory.getLogger(AbstractPropertyService.class);
	
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
	public String getPropertyFromConfig(final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getProperty(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return null;
		}
		// the toCanonicalForm() call here ensures that small typos in the liquidbase xml get rectified to the coded canonical forms.
		return theProperty.toCanonicalForm(theProperty.getValue());
	}

	public boolean getBooleanPropertyFromConfig(final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getProperty(inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return false;
		}
		return theProperty.getBooleanValue();
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

	@Override
	protected void startUp() throws Exception {
	}

	@Override
	protected void shutDown() throws Exception {
	}
}
