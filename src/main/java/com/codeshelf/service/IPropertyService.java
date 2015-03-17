package com.codeshelf.service;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;

public interface IPropertyService extends CodeshelfService, IApiService {
	// direct getter/setter
	public DomainObjectProperty getProperty(IDomainObject object, String name);
	public void changePropertyValue(final Facility inFacility, final String inPropertyName, final String inNewStringValue);		

	// convenience methods
	public void setBayChangeChoice(Facility inFacility, BayChangeChoice inBayChangeChoice);
	public void setRepeatPosChoice(Facility inFacility, RepeatPosChoice inRepeatPosChoice);
	public void restoreHKDefaults(Facility inFacility);
	public void turnOffHK(Facility inFacility);
	public String getPropertyFromConfig(final Facility inFacility, final String inPropertyName);
	public boolean getBooleanPropertyFromConfig(final Facility inFacility, final String inPropertyName);
	public ColorEnum getPropertyAsColor(IDomainObject object, String name, ColorEnum defaultColor);
	public int getPropertyAsInt(IDomainObject object, String name, int defaultValue);
}
