package com.codeshelf.manager;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.SingleTenantPersistenceService;

public class ManagerPersistenceService extends SingleTenantPersistenceService<ManagerSchema> {
	private static SingleTenantPersistenceService<ManagerSchema> theInstance = null;

	private ManagerPersistenceService() {
		super(new ManagerSchema());
	}
	
	public final synchronized static SingleTenantPersistenceService<ManagerSchema> getMaybeRunningInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
		}
		return theInstance;
	}
	public final synchronized static SingleTenantPersistenceService<ManagerSchema> getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final static SingleTenantPersistenceService<ManagerSchema> getInstance() {
		getMaybeRunningInstance().awaitRunningOrThrow();		
		return theInstance;
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
