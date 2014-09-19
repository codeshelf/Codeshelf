package com.gadgetworks.codeshelf.model.dao;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class DaoTestListener implements IDaoListener {

	private static final Logger	LOGGER = LoggerFactory.getLogger(DaoTestListener.class);

	@Getter
	int objectsAdded = 0;
	
	@Getter
	int objectsUpdated = 0;
	
	@Getter
	int objectsDeleted = 0;
	
	@Getter
	IDomainObject lastObjectUpdated = null;
	
	@Getter
	Set<String> lastObjectPropertiesUpdated = null;
	
	@Override
	public void objectAdded(IDomainObject object) {
		objectsAdded++;
		LOGGER.debug("Object added: "+object);
	}

	@Override
	public void objectUpdated(IDomainObject object, Set<String> changedProperties) {
		objectsUpdated++;
		lastObjectUpdated = object;
		lastObjectPropertiesUpdated = changedProperties;
		LOGGER.debug("Object updated: "+object);
	}

	@Override
	public void objectDeleted(IDomainObject object) {
		objectsDeleted++;
		LOGGER.debug("Object deleted: "+object);
	}
}
