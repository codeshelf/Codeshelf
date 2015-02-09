package com.gadgetworks.codeshelf.platform.persistence;

import lombok.Getter;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.platform.Service;
import com.google.inject.Singleton;

@Singleton
public class ManagerPersistenceService extends Service {
	private static final String MASTER_CHANGELOG_NAME = "liquibase/mgr.changelog-master.xml";
	private static final Logger LOGGER = LoggerFactory.getLogger(ManagerPersistenceService.class);
	
	private static ManagerPersistenceService theInstance = null;
	SessionFactory sessionFactory = null;
	
	@Getter
	SchemaManager schemaManager = null;
	
	public ManagerPersistenceService() {		
		setInstance();
	}

	private void setInstance() {
		ManagerPersistenceService.theInstance = this;
	} 
	
	public final synchronized static ManagerPersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new ManagerPersistenceService();
			theInstance.start();
		} else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was restarted");
		}
		return theInstance;
	}

	@Override
	public boolean start() {
		if(this.isRunning()) {
			LOGGER.error("Attempted to start persistence service more than once");
		} else {
			LOGGER.info("Starting "+PersistenceService.class.getSimpleName());
			configure();
			this.setRunning(true);
		}
		return true;
	}
	
	private void configure() {		
		// fetch hibernate configuration from properties file
		String hibernateConfigurationFile = System.getProperty("manager.hibernateconfig");
		if (hibernateConfigurationFile==null) {
			LOGGER.error("manager.hibernateconfig is not defined.");
			System.exit(-1);
		}
		
		Configuration configuration = new Configuration().configure("hibernate/"+hibernateConfigurationFile);

		// add database connection info to configuration
		String connectionUrl = System.getProperty("manager.db.url");
		if (connectionUrl==null) {
			LOGGER.error("manager.db.url is not defined.");
			System.exit(-1);
		}
		String url = System.getProperty("manager.db.url");
		String username = System.getProperty("manager.db.username");
		String password = System.getProperty("manager.db.password");
    	configuration.setProperty("hibernate.connection.url", url);
    	configuration.setProperty("hibernate.connection.username", username);
    	configuration.setProperty("hibernate.connection.password", password);

		String schemaName = System.getProperty("db.schemaname"); //optional
    	if(schemaName != null) {
	    	configuration.setProperty("hibernate.default_schema", schemaName);
    	}

    	// wait why is this again
    	configuration.setProperty("javax.persistence.schema-generation-source","metadata-then-script");

		schemaManager = new SchemaManager(MASTER_CHANGELOG_NAME,url,username,password,schemaName);			

		// do not attempt to actively manage schema of the in-memory testing db; that will be done by Hibernate
		if(!url.startsWith("jdbc:h2:mem")) {
			// this only runs for postgres database
			schemaManager.applySchemaUpdates();			
			if(!schemaManager.checkSchema()) {
				throw new RuntimeException("Cannot start, schema does not match");
			}
		}

		// set up session factory
        try {
        	LOGGER.info("Creating manager session factory");
        	BootstrapServiceRegistryBuilder bootstrapBuilder = 
        			new BootstrapServiceRegistryBuilder();
        				
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());

	        this.sessionFactory = configuration.buildSessionFactory(ssrb.build());
        } catch (Exception ex) {
        	if(ex instanceof HibernateException) {
        		throw ex;
        	} else {
            	LOGGER.error("SessionFactory creation failed",ex);
                throw new RuntimeException(ex);
        	}
        }

	}

	@Override
	public final boolean stop() {
		LOGGER.info("PersistenceService stopping");
		if(this.hasActiveTransaction()) {
			this.rollbackTransactionAndCloseSession();
		}
		this.setRunning(false);
		return true;
	}

	public Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public void save(Object entity) {
		Session session = this.beginSessionAndTransaction();
		session.saveOrUpdate(entity);
		this.commitTransactionAndCloseSession();
	}

	public final Session beginSessionAndTransaction() {
		Session session = getSession();
		Transaction tx = session.getTransaction();
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				// StackTraceElement[] tst=transactionStarted.get(tx);
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return session;
			} // else we will begin new transaction
		}
		Transaction txBegun = session.beginTransaction();
		return session;
	}

	public final void commitTransactionAndCloseSession() {
		Session session = getSession();
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive transaction");
		}
		//session.close();
	}

	public final void rollbackTransactionAndCloseSession() {
		Session session = getSession();
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
		//session.close();
	}

	public boolean hasActiveTransaction() {
		if(this.isRunning() == false) {
			return false;
		} //else
		Session session = getSession();
		if (session==null) {
			return false;
		} //else
		Transaction tx = session.getTransaction();
		if (tx==null) {
			return false;
		} //else
		return tx.isActive();
	}
}
