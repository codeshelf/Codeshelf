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
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.Service;
import com.codeshelf.platform.ServiceNotInitializedException;

public abstract class PersistenceService extends Service {
	public enum SQLSyntax {
		H2,POSTGRES,OTHER;
	}
	
	static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	
	// define behavior of service
	abstract public IManagedSchema getDefaultCollection(); // default (or single) tenant definition
	abstract protected void performStartupActions(IManagedSchema collection); // actions to perform after initializing collecttion
	abstract protected EventListenerIntegrator generateEventListenerIntegrator(); // per collection; null if not wanted

	// stores the factories for different collections
	private Map<IManagedSchema,SessionFactory> factories = new HashMap<IManagedSchema, SessionFactory>();

	// store the event listener integrators
	private Map<IManagedSchema,EventListenerIntegrator> listenerIntegrators = new HashMap<IManagedSchema, EventListenerIntegrator>();

	private SessionFactory createSessionFactory(IManagedSchema collection) {
		if (this.isRunning()==false) {
			throw new ServiceNotInitializedException();
		}
        try {
			SchemaUtil.applySchemaUpdates(collection);

			LOGGER.info("Creating session factory for "+collection.getSchemaName());
			Configuration configuration = SchemaUtil.getHibernateConfiguration(collection);
        	
			// initialize hibernate session factory
        	BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder();
        	EventListenerIntegrator integrator = this.generateEventListenerIntegrator();
        	if(integrator != null) { // use subclass definition to attach optional hibernate integrator
        		bootstrapBuilder.with(integrator);
        		this.listenerIntegrators.put(collection, integrator);
        	}
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());

	        // enable statistics
	        factory.getStatistics().setStatisticsEnabled(true);
	        			
	        // add to factory map
			this.factories.put(collection, factory);
	        
	        // sync up property defaults (etc) 
			this.performStartupActions(collection);

	        return factory;
        } catch (Exception ex) {
        	LOGGER.error("SessionFactory creation for "+collection.getSchemaName()+" failed",ex);
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

	private final SessionFactory getSessionFactory(IManagedSchema collection) {
		SessionFactory fac = this.factories.get(collection);
		if (fac==null) {
			fac = createSessionFactory(collection);
		}
		return fac;
	}
	
	public final Session getSession(IManagedSchema collection) {
		SessionFactory fac = this.getSessionFactory(collection);
		Session session = fac.getCurrentSession();

		return session;
	}

	public final Session getSessionWithTransaction(IManagedSchema collection) {
		Session session = getSession(collection);
		PersistenceService.beginTransaction(session);
		return session;
	}

	public final Transaction beginTransaction(IManagedSchema collection) {
		Session session = getSession(collection);
		return PersistenceService.beginTransaction(session);
	}
	
	public final void commitTransaction(IManagedSchema collection) {
		Session session = getSession(collection);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive Tenant transaction");
		}
	}

	public final void rollbackTransaction(IManagedSchema collection) {
		Session session = getSession(collection);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
	}

	public final boolean hasActiveTransaction(IManagedSchema collection) {
		if(this.isRunning() == false) {
			return false;
		} 
		SessionFactory sf = this.factories.get(collection);
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
		for(IManagedSchema collection : this.factories.keySet()) {
			if(hasActiveTransaction(collection)) {
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
	
	private final EventListenerIntegrator getEventListenerIntegrator(IManagedSchema collection) {
		getSessionFactory(collection); // ensure this collection has been initialized
		return this.listenerIntegrators.get(collection);
	}
	public final EventListenerIntegrator getEventListenerIntegrator() {
		return getEventListenerIntegrator(getDefaultCollection());
	}
	
	/* Methods for using default collection */
	public final Transaction beginTransaction() {
		return beginTransaction(getDefaultCollection());
	}
	
	public final void commitTransaction() {
		commitTransaction(getDefaultCollection());
	}
	
	public final void rollbackTenantTransaction() {
		rollbackTransaction(getDefaultCollection());
	}
	
	public final Session getSession() {
		return getSession(getDefaultCollection());
	}
	
	public final Session getSessionWithTransaction() {
		return getSessionWithTransaction(getDefaultCollection());
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
