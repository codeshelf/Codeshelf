package com.codeshelf.platform.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Singleton;

@Singleton
public class ManagerPersistenceService extends PersistenceService<ManagerSchema> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManagerPersistenceService.class);
	
	private static ManagerPersistenceService theInstance = null;

	private ManagerSchema managerSchema;

	private ManagerPersistenceService() {	
		managerSchema = new ManagerSchema();
	}

	public final synchronized static ManagerPersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
			theInstance.start();
		} else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was restarted");
		}
		return theInstance;
	}

	@Override
	public ManagerSchema getDefaultSchema() {
		return this.managerSchema;
	}

	@Override
	public EventListenerIntegrator generateEventListenerIntegrator() {
		return null;
	}

	@Override
	protected void performStartupActions(ManagerSchema schema) {
		return;
	}
}
