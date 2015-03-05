package com.codeshelf.platform.persistence;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

import edu.emory.mathcs.backport.java.util.Collections;

public abstract class PersistenceServiceImpl<SCHEMA_TYPE extends Schema> extends PersistenceService<SCHEMA_TYPE> {
	static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	
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

	@Override
	public void forgetInitialActions(SCHEMA_TYPE schema) {
		if(initializedFactories != null) // in case stopping/stopped, ignore 
			initializedFactories.remove(schema);
	}

	@Override
	public final Session getSession(SCHEMA_TYPE schema) {
		SessionFactory fac = this.getSessionFactory(schema);
		Session session = fac.getCurrentSession();

		return session;
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
	
	@Override
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

	@Override
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
	
	@Override
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
	
	public final EventListenerIntegrator getEventListenerIntegrator(SCHEMA_TYPE schema) {
		getSessionFactory(schema); // ensure this schema has been initialized
		return this.listenerIntegrators.get(schema);
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

}
