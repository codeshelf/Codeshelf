package com.codeshelf.platform.persistence;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;

import org.apache.mina.util.ConcurrentHashSet;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.AbstractCodeshelfIdleService;

public abstract class PersistenceService extends AbstractCodeshelfIdleService implements IPersistenceService {
	static final private Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);

	abstract public EventListenerIntegrator generateEventListenerIntegrator(); // can be null
	abstract public Configuration getHibernateConfiguration(); // TODO: stop using deprecated Configuration object
	abstract public String getHibernateConfigurationFilename(); // liquibase uses this filename
	abstract public String getMasterChangeLogFilename(); // liquibase
	
	abstract public void initializeTenantData(); // optional startup init after schema prepared

	abstract public String getCurrentTenantIdentifier(); // might get from thread context
	abstract protected DatabaseCredentials getDatabaseCredentials(String tenantIdentifier); // lookup
	abstract protected DatabaseCredentials getSuperDatabaseCredentials(String tenantIdentifier);

	SessionFactory sessionFactory;
	
	ConcurrentHashSet<String> initializingTenantIdentifiers = new ConcurrentHashSet<String>();
	ConcurrentHashSet<String> initializedTenantIdentifiers = new ConcurrentHashSet<String>();
	
	@Getter
	EventListenerIntegrator eventListenerIntegrator;

	@Override
	public String serviceName() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public Session getSession() {
		// initialize database schema before returning a session, if necessary
		String tenantIdentifier = this.getCurrentTenantIdentifier();
		if(!initializedTenantIdentifiers.contains(tenantIdentifier)) {
			
			// NOTE: putting tenant initialization in this block means multiple threads 
			// accessing the same tenant will block while one does the init.

			// it *also* means that only one tenant can initialize at a time, which may
			// or may not be desirable.
			
			// note that if initializeTenant attempts to obtain additional sessions on
			// this thread, they will be obtained though initialization is not complete.
			synchronized(this.initializingTenantIdentifiers) {
				if(!this.initializingTenantIdentifiers.contains(tenantIdentifier)) {
					this.initializingTenantIdentifiers.add(tenantIdentifier);

					initializeTenantSchema(tenantIdentifier);
					
					initializeTenantData();
					
					initializedTenantIdentifiers.add(tenantIdentifier);
				}
			}
		}
		Session session = this.sessionFactory.getCurrentSession();

		return session;
	}
	
	private void initializeTenantSchema(String tenantIdentifier) {
		DatabaseCredentials cred = this.getDatabaseCredentials(tenantIdentifier);
		DatabaseCredentials superCred = this.getSuperDatabaseCredentials(tenantIdentifier);

		if(DatabaseUtils.getSQLSyntax(cred) == DatabaseUtils.SQLSyntax.POSTGRES) {
			// create schema automatically for dev environments
			try {
				DatabaseUtils.executeSQL(superCred,"CREATE SCHEMA IF NOT EXISTS "+cred.getSchemaName());
			} catch (SQLException e) {
				throw new RuntimeException("Cannot start, failed to verify/create schema (check db admin rights)",e);
			}

			// liquibase -> postgres
			applyLiquibaseSchemaUpdates(cred);
		} else if(DatabaseUtils.getSQLSyntax(cred) == DatabaseUtils.SQLSyntax.H2_MEMORY) {
			// for automated tests, schema needs to have been created explicitly

			// this is faster than liquibase although maybe we should test with liquibase
			DatabaseUtils.Hbm2DdlSchemaExport(this.getHibernateConfiguration(),cred);
		} else {
			LOGGER.warn("Will not attempt to apply Liquibase updates to unknown syntax");
		}
		
	}
	@Override
	public void forgetInitialActions(String tenantIdentifier) {
		this.initializedTenantIdentifiers.remove(tenantIdentifier);
		this.initializingTenantIdentifiers.remove(tenantIdentifier);
	}

	protected static Transaction beginTransaction(Session session) {
		if(session==null)
			return null;
		
		Transaction tx = session.getTransaction();
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return tx;
		}
		} // else we will begin new transaction
		Transaction txBegun = session.beginTransaction();
		return txBegun;
	}
	
	@Override
	public Session getSessionWithTransaction() {
		Session session = getSession();
		beginTransaction(session);
		return session;
	}

	@Override
	public Transaction beginTransaction() {
		Session session = getSession();
		return beginTransaction(session);
	}
	
	@Override
	public void commitTransaction() {
		Session session = getSession();
		if(session != null) {
			Transaction tx = session.getTransaction();
			if (tx.isActive()) {
				tx.commit();
			} else {
				LOGGER.error("tried to close inactive Tenant transaction");
			}
		}
	}

	@Override
	public void rollbackTransaction() {
		Session session = getSession();
		if(session != null) {
			Transaction tx = session.getTransaction();
			if (tx.isActive()) {
				tx.rollback();
			} else {
				LOGGER.error("tried to roll back inactive Tenant transaction");
			}
		}
	}
	
	@Override
	public boolean hasAnyActiveTransactions() {
		return checkActiveTransactions(false);
	}
	
	@Override
	public boolean rollbackAnyActiveTransactions() {
		return checkActiveTransactions(true);
	}
	
	private boolean checkActiveTransactions(boolean rollback) {
		if(!sessionFactory.isClosed()) {
			Session session = sessionFactory.getCurrentSession();
			if(session != null) {
				Transaction tx = session.getTransaction();
				if(tx != null) {
					if(tx.isActive()) {
						if(rollback) {
							tx.rollback();
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	public static <T>T deproxify(T object) {
		if (object==null) {
			return null;
		} if (object instanceof HibernateProxy) {
	        Hibernate.initialize(object);
	        @SuppressWarnings("unchecked")
			T realDomainObject = (T) ((HibernateProxy) object)
	                  .getHibernateLazyInitializer()
	                  .getImplementation();
	        return (T)realDomainObject;
	    }
		return object;
	}
	
	@Override
	protected void startUp() throws Exception {
		this.eventListenerIntegrator = this.generateEventListenerIntegrator();

		LOGGER.debug("Creating session factory for "+this.getClass().getSimpleName());
		Configuration configuration = this.getHibernateConfiguration();
    	
    	BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder()
    			.with(new JpaIntegrator()); // support for JPA annotations e.g. @PrePersist

    	// use subclass definition to attach optional custom hibernate integrator if desired
    	EventListenerIntegrator integrator = this.generateEventListenerIntegrator();
    	if(integrator != null) { 
    		bootstrapBuilder.with(integrator);
    		this.eventListenerIntegrator = integrator;
    	}

		// initialize hibernate session factory
    	StandardServiceRegistryBuilder ssrb = 
        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
        			.applySettings(configuration.getProperties());
        this.sessionFactory = configuration.buildSessionFactory(ssrb.build());
        
        // enable statistics
        this.sessionFactory.getStatistics().setStatisticsEnabled(true);
	}
	
	@Override
	protected void shutDown() throws Exception {
		this.sessionFactory = null;
		this.eventListenerIntegrator = null;
	}
	
	private void applyLiquibaseSchemaUpdates(DatabaseCredentials cred) {
		Database appDatabase = DatabaseUtils.getAppDatabase(cred);
		if(appDatabase==null) {
			throw new RuntimeException("Failed to access app database, cannot continue");
		}
		
		ResourceAccessor fileOpener = new ClassLoaderResourceAccessor(); 
		
		LOGGER.debug("initializing Liquibase");
		Contexts contexts = new Contexts(); //empty context
		Liquibase liquibase;
		try {
			liquibase = new Liquibase(this.getMasterChangeLogFilename(), fileOpener, appDatabase);
		} catch (LiquibaseException e) {
			LOGGER.error("Failed to initialize liquibase, cannot continue.", e);
			throw new RuntimeException("Failed to initialize liquibase, cannot continue.",e);
		}
	
		List<ChangeSet> pendingChanges;
		try {
			pendingChanges = liquibase.listUnrunChangeSets(contexts);
		} catch (LiquibaseException e1) {
			LOGGER.error("Could not get pending schema changes, cannot continue.", e1);
			throw new RuntimeException("Could not get pending schema changes, cannot continue.",e1);
		}
		
		if(pendingChanges.size() > 0) {	
			LOGGER.info("Now updating db schema - will apply "+pendingChanges.size()+" changesets to {}",cred.getSchemaName());
			try {
				liquibase.update(contexts);
			} catch (LiquibaseException e) {
				LOGGER.error("Failed to apply changes to app database, cannot continue. Database might be corrupt.", e);
				throw new RuntimeException("Failed to apply changes to app database, cannot continue. Database might be corrupt.",e);
			}
			LOGGER.info("Done applying Liquibase changesets: {}",cred.getSchemaName());
		} else {
			LOGGER.info("Liquibase initializing with 0 changesets: {}",cred.getSchemaName());
		}
	
		if(!this.liquibaseCheckSchema(cred)) {
			throw new RuntimeException("Cannot start, schema does not match");
		}
	}

	private boolean liquibaseCheckSchema(DatabaseCredentials conn) {
		
		// TODO: this, but cleanly without using unsupported CommandLineUtils interface
		Database hibernateDatabase;
		try {
			hibernateDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				"hibernate:classic:"+this.getHibernateConfigurationFilename(), 
				null, null, null, 
				null, null,
				false, false,
				null,null,
				null,null);
		} catch (DatabaseException e1) {
			LOGGER.error("Database exception evaluating Hibernate configuration", e1);
			return false;
		}
		
	    /*
	    CommandLineUtils.createDatabaseObject(classLoader, url, 
	    	username, password, driver, 
	    	defaultCatalogName, defaultSchemaName, 
	    	Boolean.parseBoolean(outputDefaultCatalog), Boolean.parseBoolean(outputDefaultSchema), 
	    	null, null, 
	    	this.liquibaseCatalogName, this.liquibaseSchemaName);
	     */
	
		Database appDatabase = DatabaseUtils.getAppDatabase(conn);
		if(appDatabase==null) {
			return false;
		}
	    
		DiffGeneratorFactory diffGen = DiffGeneratorFactory.getInstance();
	
		DiffResult diff;
		try {
			diff = diffGen.compare(hibernateDatabase, appDatabase, CompareControl.STANDARD);
		} catch (LiquibaseException e1) {
			LOGGER.error("Liquibase exception diffing Hibernate/database configuration", e1);
			return false;
		}
		
		DiffOutputControl diffOutputCtrl =  new DiffOutputControl();
		diffOutputCtrl.setIncludeCatalog(false);
		diffOutputCtrl.setIncludeSchema(false);
		
		DiffToChangeLog diff2cl = new DiffToChangeLog(diff,diffOutputCtrl);
	
		if(diff2cl.generateChangeSets().size() > 0) {
			try {
				diff2cl.print(System.out);
			} catch (ParserConfigurationException | IOException | DatabaseException e) {
				LOGGER.error("Unexpected exception outputing diff", e);
			}
			return false;
		} //else
		return true;
	}

}
