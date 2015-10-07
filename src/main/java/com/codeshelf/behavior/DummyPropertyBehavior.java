package com.codeshelf.behavior;

import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.service.AbstractPropertyService;

public class DummyPropertyBehavior extends AbstractPropertyService {

	@Override
	public DomainObjectProperty getProperty(IDomainObject object, String name) {
		return null;
	}

	@Override
	public void changePropertyValue(Facility inFacility, String inPropertyName, String inNewStringValue) {
	}

}
