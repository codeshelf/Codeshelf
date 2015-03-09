package com.codeshelf.platform.persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.codeshelf.service.ICodeshelfService;

public interface IPersistenceService<SCHEMA_TYPE extends Schema> extends ICodeshelfService {

	public SCHEMA_TYPE getDefaultSchema();
	public Session getSession(SCHEMA_TYPE schema);
	public SessionFactory getSessionFactory(SCHEMA_TYPE schema);
	public EventListenerIntegrator getEventListenerIntegrator(SCHEMA_TYPE schema);
	public void forgetInitialActions(SCHEMA_TYPE schema);
	public boolean hasActiveTransaction(SCHEMA_TYPE schema);

	public boolean hasAnyActiveTransactions();
	public boolean rollbackAnyActiveTransactions();

	public Session getSessionWithTransaction(SCHEMA_TYPE schema);
	public Transaction beginTransaction(SCHEMA_TYPE schema);
	public void commitTransaction(SCHEMA_TYPE schema);
	public void rollbackTransaction(SCHEMA_TYPE schema);

	// methods for using default schema
	public Transaction beginTransaction();
	public void commitTransaction();
	public void rollbackTransaction();
	public Session getSession();
	public Session getSessionWithTransaction();
	public SessionFactory getSessionFactory();
	public EventListenerIntegrator getEventListenerIntegrator();

}