package com.codeshelf.manager;

import lombok.Getter;

import org.hibernate.cfg.Configuration;

import com.codeshelf.persistence.AbstractPersistenceService;
import com.codeshelf.persistence.DatabaseCredentials;
import com.codeshelf.persistence.EventListenerIntegrator;

public class ManagerPersistenceService extends AbstractPersistenceService implements DatabaseCredentials {
	private static final String MASTER_CHANGELOG_NAME = "liquibase/mgr.changelog-master.xml";
	
	@Getter
	private String url;
	@Getter
	private String username;
	@Getter
	private String password;
	@Getter
	private String schemaName;
	
	private static AbstractPersistenceService theInstance = null;

	private ManagerPersistenceService() {
		super();
	}
	
	public final synchronized static AbstractPersistenceService getMaybeRunningInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
		}
		return theInstance;
	}
	public final synchronized static AbstractPersistenceService getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final static AbstractPersistenceService getInstance() {
		getMaybeRunningInstance().awaitRunningOrThrow();		
		return theInstance;
	}

	@Override
	public EventListenerIntegrator generateEventListenerIntegrator() {
		return null;
	}

	@Override
	public synchronized Configuration getHibernateConfiguration() {
		// put together hibernate configuration from XML and properties file
		String hibernateConfigurationFilename = this.getHibernateConfigurationFilename();		
		Configuration hibernateConfiguration = new Configuration().configure(hibernateConfigurationFilename);
		hibernateConfiguration.setProperty("hibernate.connection.url", url);
		hibernateConfiguration.setProperty("hibernate.connection.username", username);
		hibernateConfiguration.setProperty("hibernate.connection.password", password);
		hibernateConfiguration.setProperty("hibernate.default_schema", schemaName);
	
		return hibernateConfiguration;
	}

	public String getHibernateConfigurationFilename() {
		return "hibernate/"+System.getProperty("manager.hibernateconfig");
	}

	@Override
	public String getMasterChangeLogFilename() {
		return MASTER_CHANGELOG_NAME;
	}

	@Override
	protected void startUp() throws Exception {
		url = System.getProperty("manager.db.url");
		username = System.getProperty("manager.db.username");
		password = System.getProperty("manager.db.password");		
		schemaName = System.getProperty("manager.db.schema");
		
		super.startUp();
	}

	@Override
	public void initializeTenantData() {
	}

	@Override
	public String getCurrentTenantIdentifier() {
		return "";
	}

	@Override
	protected DatabaseCredentials getDatabaseCredentials(String tenantIdentifier) {
		return this;
	}

	@Override
	protected DatabaseCredentials getSuperDatabaseCredentials(String tenantIdentifier) {
		return this;
	}
}
