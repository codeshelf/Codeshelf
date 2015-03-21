package com.codeshelf.platform.persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.codeshelf.service.CodeshelfService;

public interface IPersistenceService<SCHEMA_TYPE extends Schema> extends CodeshelfService {

	// get a session, or one with a transaction, or a transaction
	public Session getSession(SCHEMA_TYPE schema);
	public Session getSessionWithTransaction(SCHEMA_TYPE schema);
	public Transaction beginTransaction(SCHEMA_TYPE schema);

	// end transaction (& session)
	public void commitTransaction(SCHEMA_TYPE schema);
	public void rollbackTransaction(SCHEMA_TYPE schema);

	// framework methods
	public SessionFactory getSessionFactory(SCHEMA_TYPE schema);
	public EventListenerIntegrator getEventListenerIntegrator(SCHEMA_TYPE schema);

	// test methods
	public void forgetInitialActions(SCHEMA_TYPE schema);
	public boolean hasAnyActiveTransactions();
	public boolean rollbackAnyActiveTransactions();
	public boolean hasActiveTransaction(SCHEMA_TYPE schema);

}