package com.codeshelf.platform.persistence;

import java.sql.SQLException;

import lombok.Getter;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class ManagerPersistenceService extends PersistenceService implements IPersistentCollection {
	private static final String MASTER_CHANGELOG_NAME = "liquibase/mgr.changelog-master.xml";
	private static final Logger LOGGER = LoggerFactory.getLogger(ManagerPersistenceService.class);
	
	private static ManagerPersistenceService theInstance = null;
	
	@Getter
	SchemaManager schemaManager = null;
	
	private ManagerPersistenceService() {		
	}

	public final synchronized static ManagerPersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
			theInstance.configure();
			theInstance.start();
		} else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was restarted");
		}
		return theInstance;
	}

	private void configure() {		
		// fetch hibernate configuration from properties file
		String hibernateConfigurationFile = System.getProperty("manager.hibernateconfig");
		if (hibernateConfigurationFile==null) {
			LOGGER.error("manager.hibernateconfig is not defined.");
			System.exit(-1);
		}
		hibernateConfigurationFile = "hibernate/"+hibernateConfigurationFile; // look in hibernate folder
		
		String url = System.getProperty("manager.db.url");
		String username = System.getProperty("manager.db.username");
		String password = System.getProperty("manager.db.password");		
    	String schemaName = System.getProperty("manager.db.schema");

    	this.schemaManager = new SchemaManager(MASTER_CHANGELOG_NAME,url,username,password,schemaName,hibernateConfigurationFile);

    	try {
			schemaManager.createSchemaIfNotExists();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot start, failed to verify/create schema (check db admin rights)");
		}
    	
    	this.schemaManager.applySchemaUpdates();
	}

	public void save(Object entity) {
		Session session = this.getSessionWithTransaction();
		session.saveOrUpdate(entity);
		this.commitTransaction();
	}

	@Override
	public String getShortName() {
		return "TenantManager";
	}

	@Override
	public IPersistentCollection getDefaultCollection() {
		return this;
	}

	@Override
	public EventListenerIntegrator generateEventListenerIntegrator() {
		return null;
	}

	@Override
	public boolean hasStartupActions() {
		return false;
	}
	
	@Override
	public void performStartupActions(Session session) {
		return;
	}
}
