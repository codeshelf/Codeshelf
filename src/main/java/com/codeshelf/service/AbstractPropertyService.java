package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.manager.Tenant;
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
	public void setBayChangeChoice(Tenant tenant,Facility inFacility, BayChangeChoice inBayChangeChoice) {
		switch(inBayChangeChoice){
			case BayChangeNone:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.BAYCHANG, "None");
				break;
			case BayChangeBayChange:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.BAYCHANG, "BayChange");
				break;
			case BayChangePathSegmentChange:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.BAYCHANG, "PathSegmentChange");
				break;
			case BayChangeExceptSamePathDistance:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.BAYCHANG, "BayChangeExceptAcrossAisle");
				break;
			default:
				LOGGER.error("unknown value in setBayChangeChoice");
		}
	}
	
	public void setRepeatPosChoice(Tenant tenant,Facility inFacility, RepeatPosChoice inRepeatPosChoice) {
		switch(inRepeatPosChoice){
			case RepeatPosNone:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.RPEATPOS, "None");
				break;
			case RepeatPosContainerOnly:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.RPEATPOS, "ContainerOnly");
				break;
			case RepeatPosContainerAndCount:
				changePropertyValue(tenant,inFacility, DomainObjectProperty.RPEATPOS, "ContainerAndCount");
				break;
			default:
				LOGGER.error("unknown value in setRepeatPosChoice");
		}
	}

	public void restoreHKDefaults(Tenant tenant,Facility inFacility) {
		setRepeatPosChoice(tenant,inFacility, RepeatPosChoice.RepeatPosContainerOnly);
		setBayChangeChoice(tenant,inFacility, BayChangeChoice.BayChangeBayChange);
	}
	public void turnOffHK(Tenant tenant,Facility inFacility) {
		setRepeatPosChoice(tenant,inFacility, RepeatPosChoice.RepeatPosNone);
		setBayChangeChoice(tenant,inFacility, BayChangeChoice.BayChangeNone);

	}

	/**
	 * Convenient API for application code.
	 */
	public String getPropertyFromConfig(Tenant tenant,final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getProperty(tenant,inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return null;
		}
		// the toCanonicalForm() call here ensures that small typos in the liquidbase xml get rectified to the coded canonical forms.
		return theProperty.toCanonicalForm(theProperty.getValue());
	}

	public boolean getBooleanPropertyFromConfig(Tenant tenant,final Facility inFacility, final String inPropertyName) {
		DomainObjectProperty theProperty = getProperty(tenant,inFacility, inPropertyName);
		if (theProperty == null) {
			LOGGER.error("Unknown property in getPropertyFromConfig()");
			return false;
		}
		return theProperty.getBooleanValue();
	}

	public ColorEnum getPropertyAsColor(Tenant tenant,IDomainObject object, String name, ColorEnum defaultColor) {
		DomainObjectProperty prop = getProperty(tenant,object, name);
		if (prop == null) {
			return defaultColor;
		}
		return prop.getColorValue();
	}

	public int getPropertyAsInt(Tenant tenant,IDomainObject object, String name, int defaultValue) {
		DomainObjectProperty prop = getProperty(tenant,object, name);
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
