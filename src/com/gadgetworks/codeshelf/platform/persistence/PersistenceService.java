package com.gadgetworks.codeshelf.platform.persistence;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.Service;
import com.gadgetworks.codeshelf.platform.ServiceNotInitializedException;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.platform.multitenancy.User;
import com.google.inject.Singleton;

/**
 * @author bheckel
 */
@Singleton
public class PersistenceService extends Service {
	private static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	private static final String TENANT_CHANGELOG_FILENAME= "liquibase/db.changelog-master.xml";
	
	private static PersistenceService theInstance = null;

	@Getter
	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	// stores the factories for different tenants
	private Map<Tenant,SessionFactory> factories = new HashMap<Tenant, SessionFactory>();

	private PersistenceService() {
		//TODO inject since this is essentially the messaging mechanism
		objectChangeBroadcaster = new ObjectChangeBroadcaster();
	}

	public final synchronized static PersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new PersistenceService();
			theInstance.start();
		}
		else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was stopped and restarted");
		}
		return theInstance;
	}

	public String getChangeLogFilename() {
		return PersistenceService.TENANT_CHANGELOG_FILENAME;
	}

	public SessionFactory createTenantSessionFactory(Tenant tenant) {
		if (this.isRunning()==false) {
			throw new ServiceNotInitializedException();
		}
		// ignore tenant and shard for now using static config data
        try {
        	Configuration configuration = tenant.getHibernateConfiguration();

        	LOGGER.info("Creating session factory for "+tenant);
        	BootstrapServiceRegistryBuilder bootstrapBuilder = 
        			new BootstrapServiceRegistryBuilder()
        				.with(new EventListenerIntegrator(getObjectChangeBroadcaster()));
        				
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());

	        // add to factory map
			this.factories.put(tenant, factory);
	        
	        // sync up property defaults with what's defined in resource file 
			Session session = factory.getCurrentSession();
			Transaction t = session.beginTransaction();
	        PropertyDao.getInstance().syncPropertyDefaults();
	        t.commit();
	        
	        // enable statistics
	        factory.getStatistics().setStatisticsEnabled(true);
	        
	    	// we do not attempt to manage schema of the in-memory test db; that will be done by Hibernate
			if(!tenant.getShard().getDbUrl().startsWith("jdbc:h2:mem")) {
				
				// this only runs for postgres database
				SchemaManager schemaManager = tenant.getSchemaManager();
				
				schemaManager.applySchemaUpdates();			
				boolean schemaMatches = schemaManager.checkSchema();
				
				if(!schemaMatches) {
					throw new RuntimeException("Cannot start, schema does not match");
				}
			}
	        return factory;
        } catch (Exception ex) {
        	if(ex instanceof HibernateException) {
        		throw ex;
        	} else {
            	LOGGER.error("SessionFactory creation for "+tenant+" failed",ex);
                throw new RuntimeException(ex);
        	}
        }
	}

	@Override
	public final boolean start() {
		if(this.isRunning()) {
			LOGGER.error("Attempted to start persistence service more than once");
		} else {
			LOGGER.info("Starting "+PersistenceService.class.getSimpleName());
			this.setRunning(true);
		}
		return true;
	}

	@Override
	public final boolean stop() {
		LOGGER.info("PersistenceService stopping");
		
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

	public Session getSession(Tenant tenant) {
		SessionFactory fac = this.factories.get(tenant);
		if (fac==null) {
			fac = createTenantSessionFactory(tenant);
		}
		Session session = fac.getCurrentSession();

		return session;
	}

	public SessionFactory getSessionFactory(Tenant tenant) {
		SessionFactory fac = this.factories.get(tenant);
		return fac;
	}

	public final Transaction beginTenantTransaction(Tenant tenant) {
		Session session = getSession(tenant);
		//StackTraceElement st[]=this.sessionStarted.get(session);
		Transaction tx = session.getTransaction();
		//if (!this.transactionStarted.containsKey(tx)) {
		//	this.transactionStarted.put(tx,Thread.currentThread().getStackTrace());
		//}
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				// StackTraceElement[] tst=transactionStarted.get(tx);
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return tx;
			} // else we will begin new transaction
		}
		Transaction txBegun = session.beginTransaction();
		return txBegun;
	}
	
	public final void commitTenantTransaction(Tenant tenant) {
		Session session = getSession(tenant);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive Tenant transaction");
		}
	}

	public final void rollbackTenantTransaction(Tenant tenant) {
		Session session = getSession(tenant);
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
	}

	public boolean hasActiveTransaction() {
		if(this.isRunning() == false) {
			return false;
		} //else
		for(SessionFactory sf : this.factories.values()) {
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
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static ITypedDao<IDomainObject> getDao(Class<?> classObject) {
		if (classObject==null) {
			LOGGER.error("Failed to get DAO for undefined class");
			return null;
		}
		try {
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				Class<IDomainObject> domainClass = (Class<IDomainObject>) classObject;
				Field field = domainClass.getField("DAO");
				ITypedDao<IDomainObject> dao = (ITypedDao<IDomainObject>) field.get(null);
				return dao;
			}
		}
		catch (Exception e) {
			LOGGER.error("Failed to get DAO for class "+classObject.getName(),e);
		}
		// not a domain object
		return null;
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

	public Tenant getDefaultTenant() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	//@Deprecated
	public final Transaction beginTenantTransaction() {
		return beginTenantTransaction(getDefaultTenant());
	}
	
	//@Deprecated
	public final void commitTenantTransaction() {
		commitTenantTransaction(getDefaultTenant());
	}
	
	//@Deprecated
	public final void rollbackTenantTransaction() {
		rollbackTenantTransaction(getDefaultTenant());
	}
	
	//@Deprecated
	public final Session getCurrentTenantSession() {
		return getSession(getDefaultTenant());
	}
	

}
