package com.codeshelf.platform.multitenancy;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.PersistenceService;
import com.codeshelf.platform.persistence.PersistenceServiceImpl;

public class ManagerPersistenceService extends PersistenceServiceImpl<ManagerSchema> {
	private static PersistenceService<ManagerSchema> theInstance = null;

	private ManagerSchema managerSchema = new ManagerSchema();

	private ManagerPersistenceService() {
		super();
	}
	
	public final synchronized static PersistenceService<ManagerSchema> getMaybeRunningInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
		}
		return theInstance;
	}
	public final synchronized static PersistenceService<ManagerSchema> getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final static PersistenceService<ManagerSchema> getInstance() {
		getMaybeRunningInstance().awaitRunningOrThrow();		
		return theInstance;
	}

	@Override
	public ManagerSchema getDefaultSchema() {
		return this.managerSchema;
	}

	@Override
	protected void initialize(ManagerSchema schema) {
		return;
	}

	@Override
	protected EventListenerIntegrator generateEventListenerIntegrator() {
		return null;
	}
}
