package com.codeshelf.platform.persistence;

import lombok.Getter;

import org.hibernate.Session;
import org.hibernate.Transaction;

abstract public class SingleTenantPersistenceService<SCHEMA_TYPE extends Schema> extends PersistenceService<SCHEMA_TYPE> {

	@Override
	abstract protected void initialize(SCHEMA_TYPE schema);
	@Override
	abstract protected EventListenerIntegrator generateEventListenerIntegrator();

	@Getter
	SCHEMA_TYPE schema;

	protected SingleTenantPersistenceService(SCHEMA_TYPE theSchema) {
		this.schema = theSchema;
	}
	
	public final Session getSessionWithTransaction() {
		return super.getSessionWithTransaction(schema);
	}

	public final Transaction beginTransaction() {
		return super.beginTransaction(schema);
	}
	
	public final void commitTransaction() {
		super.commitTransaction(schema);
	}

	public final void rollbackTransaction() {
		super.rollbackTransaction(schema);
	}

}
