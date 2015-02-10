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
import org.hibernate.integrator.spi.Integrator;
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
        	Integrator integrator = collection.getIntegrator();
        	if(integrator != null) {
        		bootstrapBuilder = bootstrapBuilder.with(integrator);
        	}
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());

	        // add to factory map
			this.factories.put(collection, factory);
	        
	        // enable statistics
	        factory.getStatistics().setStatisticsEnabled(true);
	        			
	    	// we do not attempt to manage schema of the in-memory test db; that will be done by Hibernate
			if(schemaManager.getSyntax() == SchemaManager.SQLSyntax.POSTGRES) {
				schemaManager.applySchemaUpdates();
				boolean schemaMatches = schemaManager.checkSchema();

				if(!schemaMatches) {
					throw new RuntimeException("Cannot start, schema does not match");
				}
			}
			
	        // sync up property defaults with what's defined in resource file 
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

	public final Session getSession(IPersistentCollection collection) {
		SessionFactory fac = this.factories.get(collection);
		if (fac==null) {
			fac = createTenantSessionFactory(collection);
		}
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
	
	@Override
	public final boolean start() {
		if(this.isRunning()) {
			LOGGER.error("Attempted to start persistence service more than once");
		} else {
			LOGGER.info("Starting "+this.getClass().getSimpleName());
			this.setRunning(true);
		}
		return true;
	}

	@Override
	public final boolean stop() {
		LOGGER.info("Stopping "+this.getClass().getSimpleName());
		
		for(SessionFactory sf : this.factories.values()) {
			if(!sf.isClosed()) {
				Session session = sf.getCurrentSession();
				if(session != null) {
					Transaction tx = session.getTransaction();
					if(tx != null) {
						if(tx.isActive()) {
							tx.rollback();
						}
					}
					//session.close();
				}
				//sf.close();
			}
		}

		//this.factories = new HashMap<Tenant, SessionFactory>(); // unlink session factories
		
		this.setRunning(false);
		return true;
	}

	private SessionFactory getSessionFactory(IPersistentCollection collection) {
		SessionFactory fac = this.factories.get(collection);
		return fac;
	}

}
