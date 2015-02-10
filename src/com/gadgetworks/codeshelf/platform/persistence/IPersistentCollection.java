package com.gadgetworks.codeshelf.platform.persistence;

import org.hibernate.Session;
import org.hibernate.integrator.spi.Integrator;

public interface IPersistentCollection {
	String getShortName();
	SchemaManager getSchemaManager();
	Integrator getIntegrator();
	void performStartupActions(Session session);
}
