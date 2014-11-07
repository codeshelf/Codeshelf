package com.gadgetworks.codeshelf.platform.persistence;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.Service;
import com.gadgetworks.codeshelf.platform.ServiceNotInitializedException;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.google.inject.Singleton;

/**
 * @author bheckel
 */
@Singleton
public class PersistenceService extends Service {
	private static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceService.class);
	private static PersistenceService theInstance = null;
	private String	hibernateConfigurationFile; 
	
	private String connectionUrl;
	private String	userId;
	private String	password;
	private String schemaName;
	
	// stores the factories for different tenants
	Map<Tenant,SessionFactory> factories = new HashMap<Tenant, SessionFactory>();
	
	// temp solution to get current tenant, while multitenancy has not been built out
	Tenant fixedTenant;
	
	// TODO: for debugging only, remove
	Map<Session,StackTraceElement[]> sessionStarted=new HashMap<Session,StackTraceElement[]>(); 
	Map<Transaction,StackTraceElement[]> transactionStarted=new HashMap<Transaction,StackTraceElement[]>();

	private PersistenceService() {
		setInstance();
		fixedTenant = new Tenant("Tenant #1",1);
	}
	
	private void setInstance() {
		PersistenceService.theInstance = this;
	}
	
	public final synchronized static PersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new PersistenceService();
			theInstance.start();
			LOGGER.warn("Unless this is a test, PersistanceService should have been initialized already but was not!");
		}
		return theInstance;
	}
	
	public SessionFactory createTenantSessionFactory(Tenant tenant) {
		if (this.isInitialized()==false) {
			throw new ServiceNotInitializedException();
		}
		// ignore tenant and shard for now using static config data
        try {
        	// Shard shard = this.shardingService.getShard(tenant.getShardId());
        	LOGGER.info("Creating session factory for "+tenant);
	    	Configuration configuration = new Configuration().configure(this.hibernateConfigurationFile);	    	
	    	// String connectionUrl = "jdbc:postgresql://"+shard.getHost()+":"+shard.getPort()+"/shard"+shard.getShardId();
	    	configuration.setProperty("hibernate.connection.url", this.connectionUrl);
	    	configuration.setProperty("hibernate.connection.username", this.userId);
	    	configuration.setProperty("hibernate.connection.password", this.password);
	    	
	    	if(this.schemaName != null) {
		    	configuration.setProperty("hibernate.default_schema", this.schemaName);
	    	}
	    	configuration.setInterceptor(new HibernateInterceptor());
	    	//configuration.setProperty("hibernate.connection.username", tenant.getName());
	    	//configuration.setProperty("hibernate.connection.password", tenant.getDbPassword());
	        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());
	        return factory;
        } catch (Exception ex) {
        	LOGGER.error("SessionFactory creation for "+tenant+" failed",ex);
            throw new RuntimeException(ex);
        }
	}
	
	@Override
	public final boolean start() {
		if(this.isInitialized()) {
			LOGGER.error("Attempted to start persistence service twice");
		} else {
			LOGGER.info("Starting "+PersistenceService.class.getSimpleName());
			
			/*
			Properties probs = System.getProperties();
			for (Entry<Object, Object> e : probs.entrySet()) {
				LOGGER.debug(e.getKey()+" - "+e.getValue());
			}
			*/
			
			// fetch database config from properties file
			this.hibernateConfigurationFile="hibernate." + System.getProperty("db.hibernateconfig") + ".xml";
			if (this.hibernateConfigurationFile==null) {
				LOGGER.error("hibernateConfigurationFile is not defined.");
				System.exit(-1);
			}
			
			this.connectionUrl = System.getProperty("db.connectionurl");
			if (this.connectionUrl==null) {
				LOGGER.error("Database URL is not defined.");
				System.exit(-1);
			}
			
			this.schemaName = System.getProperty("db.schemaname"); //optional
			
			this.userId = System.getProperty("db.userid");
			if (this.userId==null) {
				LOGGER.error("Database User ID is not defined.");
				System.exit(-1);
			}
			this.password = System.getProperty("db.password");
			this.setInitialized(true);
			LOGGER.info(PersistenceService.class.getSimpleName()+" started");
		}
		return true;
	}

	@Override
	public final boolean stop() {
		LOGGER.info("Database shutdown");
		return true;
	}

	public Session getCurrentTenantSession() {
		Tenant tenant = getCurrentTenant();
		SessionFactory fac = this.factories.get(tenant);
		if (fac==null) {
			fac = createTenantSessionFactory(tenant);
			this.factories.put(tenant, fac);
		}
		Session session = fac.getCurrentSession();
		
		if(!this.sessionStarted.containsKey(session)) {
			this.sessionStarted.put(session, Thread.currentThread().getStackTrace());
		}
		
		return session;
	}

	private Tenant getCurrentTenant() {
		return this.fixedTenant;
	}
	
	public final Transaction beginTenantTransaction() {
		Session session = getCurrentTenantSession();
		StackTraceElement st[]=this.sessionStarted.get(session);
		Transaction tx = session.getTransaction();
		if(!this.transactionStarted.containsKey(tx)) {
			this.transactionStarted.put(tx,Thread.currentThread().getStackTrace());
		}
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				StackTraceElement[] tst=transactionStarted.get(tx);
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return tx;
			} // else we will begin new transaction
		}
		
		Transaction txBegun = session.beginTransaction();
		return txBegun;
	}

	public final void endTenantTransaction() {
		Session session = getCurrentTenantSession();
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.commit();
		} else {
			LOGGER.error("tried to close inactive Tenant transaction");
		}
	}
	
	public final void rollbackTenantTransaction() {
		Session session = getCurrentTenantSession();
		Transaction tx = session.getTransaction();
		if (tx.isActive()) {
			tx.rollback();
		} else {
			LOGGER.error("tried to roll back inactive Tenant transaction");
		}
	}
	
	public boolean hasActiveTransaction() {
		Session session = getCurrentTenantSession();
		if (session==null) {
			return false;
		}
		Transaction tx = session.getTransaction();
		if (tx==null) {
			return false;
		}
		if (tx.isActive()) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static ITypedDao<IDomainObject> getDao(Class<?> classObject) throws NoSuchFieldException, IllegalAccessException {
		if (IDomainObject.class.isAssignableFrom(classObject)) {
			Class<IDomainObject> domainClass = (Class<IDomainObject>) classObject;
			Field field = domainClass.getField("DAO");
			ITypedDao<IDomainObject> dao = (ITypedDao<IDomainObject>) field.get(null);
			return dao;
		}
		// not a domain object
		return null;
	}	
}
