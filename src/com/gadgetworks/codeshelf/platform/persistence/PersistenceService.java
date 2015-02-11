package com.gadgetworks.codeshelf.platform.persistence;

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

import com.gadgetworks.codeshelf.platform.Service;
import com.gadgetworks.codeshelf.platform.ServiceNotInitializedException;

public abstract class PersistenceService extends Service {
	private static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	
	abstract public IPersistentCollection getDefaultCollection();

	// stores the factories for different collections
	private Map<IPersistentCollection,SessionFactory> factories = new HashMap<IPersistentCollection, SessionFactory>();

	private Map<IPersistentCollection,EventListenerIntegrator> listenerIntegrators = new HashMap<IPersistentCollection, EventListenerIntegrator>();

	private SessionFactory createTenantSessionFactory(IPersistentCollection collection) {
		if (this.isRunning()==false) {
			throw new ServiceNotInitializedException();
		}
        try {
			LOGGER.info("Creating session factory for "+collection.getShortName());
			SchemaManager schemaManager = collection.getSchemaManager();
			Configuration configuration = schemaManager.getHibernateConfiguration();
        	
			// initialize hibernate session factory
        	BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder();
        	EventListenerIntegrator integrator = collection.generateEventListenerIntegrator();
        	if(integrator != null) {
        		bootstrapBuilder.with(integrator);
        		this.listenerIntegrators.put(collection, integrator);
        	}
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());

	        // add to factory map
			this.factories.put(collection, factory);
	        
	        // enable statistics
	        factory.getStatistics().setStatisticsEnabled(true);
	        			
	        // sync up property defaults (etc) 
			Session session = factory.getCurrentSession();
			collection.performStartupActions(session);
			if(session.isOpen()) {
				session.close();
			}

	        return factory;
        } catch (Exception ex) {
        	if(ex instanceof HibernateException) {
        		throw ex;
        	} else {
            	LOGGER.error("SessionFactory creation for "+collection.getShortName()+" failed",ex);
                throw new RuntimeException(ex);
        	}
        }
	}

	private SessionFactory getTenantSessionFactory(IPersistentCollection collection) {
		SessionFactory fac = this.factories.get(collection);
		if (fac==null) {
			fac = createTenantSessionFactory(collection);
		}
		return fac;
	}
	
	public final Session getSession(IPersistentCollection collection) {
		SessionFactory fac = this.getTenantSessionFactory(collection);
		Session session = fac.getCurrentSession();

		return session;
	}

	public final static Transaction beginTransaction(Session session) {
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
	
	public final Session getSessionWithTransaction(IPersistentCollection collection) {
		Session session = getSession(collection);
		PersistenceService.beginTransaction(session);
		return session;
	}

	public final Transaction beginTransaction(IPersistentCollection collection) {
		Session session = getSession(collection);
		return PersistenceService.beginTransaction(session);
	}
	
	public final void commitTransaction(IPersistentCollection collection) {
		Session session = getSession(collection);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive Tenant transaction");
		}
	}

	public final void rollbackTransaction(IPersistentCollection collection) {
		Session session = getSession(collection);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
	}

	public final boolean hasActiveTransaction(IPersistentCollection collection) {
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
		for(IPersistentCollection collection : this.factories.keySet()) {
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
	
	public EventListenerIntegrator getEventListenerIntegrator(IPersistentCollection collection) {
		getTenantSessionFactory(collection); // ensure this collection has been initialized
		return this.listenerIntegrators.get(collection);
	}
	
	/* Methods for using default collection */
	public final EventListenerIntegrator getEventListenerIntegrator() {
		return getEventListenerIntegrator(getDefaultCollection());
	}
	
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
