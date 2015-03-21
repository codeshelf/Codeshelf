package com.codeshelf.service;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;

public interface IPropertyService extends CodeshelfService, IApiService {
	// direct getter/setter
	public DomainObjectProperty getProperty(Tenant tenant,IDomainObject object, String name);
	public void changePropertyValue(Tenant tenant,final Facility inFacility, final String inPropertyName, final String inNewStringValue);		

	// convenience methods
	public void setBayChangeChoice(Tenant tenant,Facility inFacility, BayChangeChoice inBayChangeChoice);
	public void setRepeatPosChoice(Tenant tenant,Facility inFacility, RepeatPosChoice inRepeatPosChoice);
	public void restoreHKDefaults(Tenant tenant,Facility inFacility);
	public void turnOffHK(Tenant tenant,Facility inFacility);
	public String getPropertyFromConfig(Tenant tenant,final Facility inFacility, final String inPropertyName);
	public boolean getBooleanPropertyFromConfig(Tenant tenant,final Facility inFacility, final String inPropertyName);
	public ColorEnum getPropertyAsColor(Tenant tenant,IDomainObject object, String name, ColorEnum defaultColor);
	public int getPropertyAsInt(Tenant tenant,IDomainObject object, String name, int defaultValue);
}
