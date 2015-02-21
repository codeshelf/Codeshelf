package com.codeshelf.platform.persistence;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
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

import com.google.common.util.concurrent.AbstractIdleService;

import edu.emory.mathcs.backport.java.util.Collections;

public abstract class PersistenceService<SCHEMA_TYPE extends Schema> extends AbstractIdleService {
	static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	private static final int	MAX_INITIALIZE_WAIT_SECONDS	= 10;
	
	// define behavior of service
	abstract public SCHEMA_TYPE getDefaultSchema(); // default (or single) tenant definition
	abstract protected void initialize(SCHEMA_TYPE schema); // actions to perform after initializing schema
	abstract protected EventListenerIntegrator generateEventListenerIntegrator(); // can be null

	// stores the factories for different schemas
	private Map<SCHEMA_TYPE,SessionFactory> factories;
	
	// whether each schema has been initalized (factory might be created during service startup, but init will not happen)
	private Set<SessionFactory> initializedFactories;

	// store the event listener integrators
	private Map<SCHEMA_TYPE,EventListenerIntegrator> listenerIntegrators;

	private SessionFactory createSessionFactory(SCHEMA_TYPE schema) {
        try {
			schema.applyLiquibaseSchemaUpdates();

			LOGGER.debug("Creating session factory for "+schema.getSchemaName());
			Configuration configuration = schema.getHibernateConfiguration();
        	
        	BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder()
        			.with(new JpaIntegrator()); // support for JPA annotations e.g. @PrePersist

        	// use subclass definition to attach optional custom hibernate integrator if desired
        	EventListenerIntegrator integrator = this.generateEventListenerIntegrator();
        	if(integrator != null) { 
        		bootstrapBuilder.with(integrator);
        		this.listenerIntegrators.put(schema, integrator);
        	}

			// initialize hibernate session factory
        	StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());
	        
	        // enable statistics
	        factory.getStatistics().setStatisticsEnabled(true);
	        			
	        // add to factory map
			this.factories.put(schema, factory);
	        
	        return factory;
        } catch (Exception ex) {
        	LOGGER.error("SessionFactory creation for "+schema.getSchemaName()+" failed",ex);
        	if(ex instanceof HibernateException) {
        		throw ex;
        	} else {
                throw new RuntimeException(ex);
        	}
        }
	}
	
	private final static Transaction beginTransaction(Session session) {
		Transaction tx = session.getTransaction();
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return tx;
			} // else we will begin new transaction
		}
		Transaction txBegun = session.beginTransaction();
		return txBegun;
	}
	
	private final synchronized SessionFactory getSessionFactoryWithoutInitialActions(SCHEMA_TYPE schema) {
		SessionFactory fac = this.factories.get(schema);
		if (fac==null) {
			fac = createSessionFactory(schema);
		}
		return fac;
	}

	public final SessionFactory getSessionFactory(SCHEMA_TYPE schema) {
		SessionFactory fac = getSessionFactoryWithoutInitialActions(schema);
		if(initializedFactories.add(fac)) {
	        // sync up property defaults (etc) 
			this.initialize(schema);
		}
		return fac;
	}

	public void forgetInitialActions(SCHEMA_TYPE schema) {
		initializedFactories.remove(schema);
	}

	public final Session getSession(SCHEMA_TYPE schema) {
		SessionFactory fac = this.getSessionFactory(schema);
		Session session = fac.getCurrentSession();

		return session;
	}

	public final Session getSessionWithTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		PersistenceService.beginTransaction(session);
		return session;
	}

	public final Transaction beginTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		return PersistenceService.beginTransaction(session);
	}
	
	public final void commitTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive Tenant transaction");
		}
	}

	public final void rollbackTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
	}

	private boolean hasActiveTransaction(SessionFactory sf,boolean rollback) {
		if(!sf.isClosed()) {
			Session session = sf.getCurrentSession();
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
	
	public final boolean hasActiveTransaction(SCHEMA_TYPE schema) {
		if(this.isRunning() == false) {
			return false;
		} 
		SessionFactory sf = this.factories.get(schema);
		if(hasActiveTransaction(sf,false)) {
			return true;
		} // else
		return false;
	}

	public final boolean hasAnyActiveTransactions() {
		if(this.isRunning() == false) {
			return false;
		} 
		for(SCHEMA_TYPE schema : this.factories.keySet()) {
			if(hasActiveTransaction(schema)) {
				return true;
			}
		}
		return false;
	}
	
	public final boolean rollbackAnyActiveTransactions() {
		if(this.isRunning() == false) {
			throw new RuntimeException("tried to rollback transactions on non-running service "+this.serviceName());
		}
		
		int rollback=0;
		for(SessionFactory sf : this.factories.values()) {
			if(hasActiveTransaction(sf,true))
				rollback++;
		}
		if(rollback>0) {
			LOGGER.warn("rolled back "+rollback+" active transactions");
		}
		
		return (rollback>0);
	}

	public final static <T>T deproxify(T object) {
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
	
	private final EventListenerIntegrator getEventListenerIntegrator(SCHEMA_TYPE schema) {
		getSessionFactory(schema); // ensure this schema has been initialized
		return this.listenerIntegrators.get(schema);
	}
	public final EventListenerIntegrator getEventListenerIntegrator() {
		return getEventListenerIntegrator(getDefaultSchema());
	}
	
	/* Methods for using default schema */
	public final Transaction beginTransaction() {
		return beginTransaction(getDefaultSchema());
	}
	
	public final void commitTransaction() {
		commitTransaction(getDefaultSchema());
	}
	
	public final void rollbackTransaction() {
		rollbackTransaction(getDefaultSchema());
	}
	
	public final Session getSession() {
		return getSession(getDefaultSchema());
	}
	
	public final Session getSessionWithTransaction() {
		return getSessionWithTransaction(getDefaultSchema());
	}
	
	public final SessionFactory getSessionFactory() {
		return getSessionFactory(getDefaultSchema());
	}

	@Override
	protected void startUp() throws Exception {
		// stores the factories for different schemas
		factories = new ConcurrentHashMap<SCHEMA_TYPE, SessionFactory>();
		
		// notes whether each schema has been initalized (factory might be created during service startup but init will not happen)
		@SuppressWarnings("unchecked")
		Set<SessionFactory> set = Collections.newSetFromMap(new ConcurrentHashMap<SessionFactory,Boolean>());
		initializedFactories = set;

		// store the event listener integrators
		listenerIntegrators = new ConcurrentHashMap<SCHEMA_TYPE, EventListenerIntegrator>();
		
		// to confirm started service, successfully create a transaction with default tenant
		SCHEMA_TYPE defaultSchema = this.getDefaultSchema();
		SessionFactory fac = this.getSessionFactoryWithoutInitialActions(defaultSchema);
		Session session = fac.getCurrentSession();
		Transaction tx = session.beginTransaction();
		tx.commit();
		
	}
	@Override
	protected void shutDown() throws Exception {
		factories = null;
		initializedFactories = null;
		listenerIntegrators = null;
	}
	@Override
	abstract protected String serviceName();
	
	public void awaitRunningOrThrow() {
		try {
			this.awaitRunning(MAX_INITIALIZE_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+serviceName(),e);
		}
	}
}
