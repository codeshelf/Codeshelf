package com.codeshelf.platform.persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.codeshelf.manager.Tenant;
import com.codeshelf.service.CodeshelfService;

public interface IPersistenceService extends CodeshelfService {

	public Configuration getHibernateConfiguration();

	public void applyLiquibaseSchemaUpdates(DatabaseCredentials cred, DatabaseCredentials superCred); // superCred optional, used to create schema if doesn't exist

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
}