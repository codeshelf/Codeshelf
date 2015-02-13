package com.codeshelf.platform.persistence;

import java.util.HashMap;
import java.util.Map;

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

import com.codeshelf.platform.Service;
import com.codeshelf.platform.ServiceNotInitializedException;

public abstract class PersistenceService<SCHEMA_TYPE extends Schema> extends Service {
	static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	
	// define behavior of service
	abstract public SCHEMA_TYPE getDefaultSchema(); // default (or single) tenant definition
	abstract protected void performStartupActions(SCHEMA_TYPE schema); // actions to perform after initializing schema
	abstract protected EventListenerIntegrator generateEventListenerIntegrator(); // can be null

	// stores the factories for different schemas
	private Map<SCHEMA_TYPE,SessionFactory> factories = new HashMap<SCHEMA_TYPE, SessionFactory>();

	// store the event listener integrators
	private Map<SCHEMA_TYPE,EventListenerIntegrator> listenerIntegrators = new HashMap<SCHEMA_TYPE, EventListenerIntegrator>();

	private SessionFactory createSessionFactory(SCHEMA_TYPE schema) {
		if (this.isRunning()==false) {
			throw new ServiceNotInitializedException();
		}
        try {
			schema.applyLiquibaseSchemaUpdates();

			LOGGER.info("Creating session factory for "+schema.getSchemaName());
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
	        
	        // sync up property defaults (etc) 
			this.performStartupActions(schema);

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

	public final SessionFactory getSessionFactory(SCHEMA_TYPE schema) {
		SessionFactory fac = this.factories.get(schema);
		if (fac==null) {
			fac = createSessionFactory(schema);
		}
		return fac;
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

	public final boolean hasActiveTransaction(SCHEMA_TYPE schema) {
		if(this.isRunning() == false) {
			return false;
		} 
		SessionFactory sf = this.factories.get(schema);
		if(!sf.isClosed()) {
			Session session = sf.getCurrentSession();
			if(session != null) {
				Transaction tx = session.getTransaction();
				if(tx != null) {
					if(tx.isActive()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public final boolean hasAnyActiveTransaction() {
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
	
	public final void rollbackTenantTransaction() {
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
	
	/* Service methods */
	@Override
	public final boolean start() {
		if(this.isRunning()) {
			LOGGER.error("Attempted to start "+this.getClass().getSimpleName()+" more than once");
		} else {
			LOGGER.info("Starting "+this.getClass().getSimpleName());
			this.setRunning(true);
		}
		return true;
	}

	@Override
	public final boolean stop() {
		LOGGER.info("Stopping "+this.getClass().getSimpleName());
		
		int rollback=0;
		for(SessionFactory sf : this.factories.values()) {
			if(!sf.isClosed()) {
				Session session = sf.getCurrentSession();
				if(session != null) {
					Transaction tx = session.getTransaction();
					if(tx != null) {
						if(tx.isActive()) {
							tx.rollback();
							rollback++;
						}
					}
					//session.close();
				}
				//sf.close();
			}
		}
		if(rollback>0) {
			LOGGER.warn("rolled back "+rollback+" active transactions while stopping persistence");
		}

		//this.factories = new HashMap<Tenant, SessionFactory>(); // unlink session factories
		
		this.setRunning(false);
		return true;
	}
}
