package com.codeshelf.persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.codeshelf.service.CodeshelfService;

public interface PersistenceService extends CodeshelfService {

	public Configuration getHibernateConfiguration();

	public String getMasterChangeLogFilename();
	
	public EventListenerIntegrator generateEventListenerIntegrator(); // can be null
	public EventListenerIntegrator getEventListenerIntegrator();

	public boolean hasAnyActiveTransactions();
	public boolean rollbackAnyActiveTransactions();

	public Transaction beginTransaction();
	public void commitTransaction();
	public void rollbackTransaction();
	public Session getSession();
	public Session getSessionWithTransaction();

	void forgetInitialActions(String tenantIdentifier);
	void forgetSchemaInitialization(String tenantIdentifier);
}