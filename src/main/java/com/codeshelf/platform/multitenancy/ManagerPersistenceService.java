package com.codeshelf.platform.multitenancy;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.PersistenceService;

public class ManagerPersistenceService extends PersistenceService<ManagerSchema> {
	private static ManagerPersistenceService theInstance = null;

	private ManagerSchema managerSchema = new ManagerSchema();

	private ManagerPersistenceService() {
		super();
	}
	
	public final synchronized static ManagerPersistenceService getMaybeRunningInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
		}
		return theInstance;
	}
	public final synchronized static ManagerPersistenceService getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final static ManagerPersistenceService getInstance() {
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

	@Override
	protected String serviceName() {
		return this.getClass().getSimpleName();
	}
}
