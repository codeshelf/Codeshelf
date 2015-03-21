package com.codeshelf.service;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;

public class DummyPropertyService extends AbstractPropertyService {

	@Override
	public DomainObjectProperty getProperty(Tenant tenant,IDomainObject object, String name) {
		return null;
	}

	@Override
	public void changePropertyValue(Tenant tenant,Facility inFacility, String inPropertyName, String inNewStringValue) {
	}

}
