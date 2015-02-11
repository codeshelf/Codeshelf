package com.gadgetworks.codeshelf.platform.persistence;

import org.hibernate.Session;

public interface IPersistentCollection {
	String getShortName();
	SchemaManager getSchemaManager();
	
	EventListenerIntegrator generateEventListenerIntegrator();
	
	boolean hasStartupActions();
	void performStartupActions(Session session);
}
