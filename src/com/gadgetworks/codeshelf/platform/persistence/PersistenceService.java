package com.gadgetworks.codeshelf.platform.persistence;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
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
	private Configuration configuration;
	private String	hibernateConfigurationFile;

	private String connectionUrl;
	private String	userId;
	private String	password;
	private String schemaName;

	@Getter
	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	// stores the factories for different tenants
	private Map<Tenant,SessionFactory> factories = new HashMap<Tenant, SessionFactory>();

	// temp solution to get current tenant, while multitenancy has not been built out
	private Tenant fixedTenant;

	// TODO: for debugging only, remove
	private Map<Session,StackTraceElement[]> sessionStarted=new HashMap<Session,StackTraceElement[]>();
	private Map<Transaction,StackTraceElement[]> transactionStarted=new HashMap<Transaction,StackTraceElement[]>();

	private PersistenceService() {
		setInstance();
		//TODO inject since this is essentially the messaging mechanism
		objectChangeBroadcaster = new ObjectChangeBroadcaster(); 
		fixedTenant = new Tenant("Tenant #1",1);
	}

	private void setInstance() {
		PersistenceService.theInstance = this;
	}

	public final synchronized static boolean isRunning() {
		return (theInstance != null && theInstance.isInitialized());
	}

	public final synchronized static PersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new PersistenceService();
			theInstance.configure();
			theInstance.start();
			//LOGGER.warn("Unless this is a test, PersistanceService should have been initialized already but was not!");
		}
		else if (!theInstance.isInitialized()) {
			theInstance.start();
			LOGGER.info("PersistanceService was stopped and restarted");
		}
		return theInstance;
	}

	private final void configure() {
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

    	configuration = new Configuration().configure(this.hibernateConfigurationFile);
    	// String connectionUrl = "jdbc:postgresql://"+shard.getHost()+":"+shard.getPort()+"/shard"+shard.getShardId();
    	configuration.setProperty("hibernate.connection.url", this.connectionUrl);
    	//configuration.setProperty("hibernate.connection.username", tenant.getName());
    	//configuration.setProperty("hibernate.connection.password", tenant.getDbPassword());
    	configuration.setProperty("hibernate.connection.username", this.userId);
    	configuration.setProperty("hibernate.connection.password", this.password);

    	if(this.schemaName != null) {
	    	configuration.setProperty("hibernate.default_schema", this.schemaName);
    	}
    	configuration.setProperty("javax.persistence.schema-generation-source","metadata-then-script");

    	// we do not attempt to manage the in-memory test db; that will be done by Hibernate
		if(!this.connectionUrl.startsWith("jdbc:h2:mem")) {
			SchemaManager sm = new SchemaManager(this.connectionUrl,this.userId,this.password,this.schemaName);
			boolean schemaMatches = sm.checkSchema();
			
			if(!schemaMatches) {
/*					try {
						this.createNewSchema();
					} catch (SQLException e) {
						LOGGER.error("SQL exception generating schema",e);
						throw new RuntimeException("Cannot start, failed to initialize schema");
					}
				} else*/ {
					throw new RuntimeException("Cannot start, schema does not match");
				}
			}
		}
	}

	public SessionFactory createTenantSessionFactory(Tenant tenant) {
		if (this.isInitialized()==false) {
			throw new ServiceNotInitializedException();
		}
		// ignore tenant and shard for now using static config data
        try {
        	// Shard shard = this.shardingService.getShard(tenant.getShardId());
        	LOGGER.info("Creating session factory for "+tenant);
        	BootstrapServiceRegistryBuilder bootstrapBuilder = 
        			new BootstrapServiceRegistryBuilder()
        				.with(new EventListenerIntegrator(getObjectChangeBroadcaster()));
        				
	        StandardServiceRegistryBuilder ssrb = 
	        		new StandardServiceRegistryBuilder(bootstrapBuilder.build())
	        			.applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());
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
		if(this.isInitialized()) {
			LOGGER.error("Attempted to start persistence service more than once");
		} else {
			LOGGER.info("Starting "+PersistenceService.class.getSimpleName());
			this.setInitialized(true);
		}
		return true;
	}

	@Override
	public final boolean stop() {
		LOGGER.info("PersistenceService stopping");
		if(this.hasActiveTransaction()) {
			this.rollbackTenantTransaction();
		}

		/*
		 * keep session factories...
		for(SessionFactory sf : this.factories.values()) {
			sf.close();
		}
		this.factories = new HashMap<Tenant, SessionFactory>(); // unlink session factories
		 */

		// unlink debugging stuff
		this.sessionStarted=new HashMap<Session,StackTraceElement[]>();
		this.transactionStarted=new HashMap<Transaction,StackTraceElement[]>();

		this.setInitialized(false);
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
				// StackTraceElement[] tst=transactionStarted.get(tx);
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
		if(this.isInitialized() == false) {
			return false;
		} //else
		Tenant tenant = fixedTenant;
		if(this.fixedTenant == null) {
			return false;
		}
		SessionFactory sf = this.factories.get(tenant);
		if(sf == null || sf.isClosed()) {
			return false;
		}
		Session session = sf.getCurrentSession();
		if (session==null) {
			return false;
		} //else
		Transaction tx = session.getTransaction();
		if (tx==null) {
			return false;
		} //else
		return tx.isActive();
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

	public void resetDatabase() {
		SchemaExport se = new SchemaExport(this.configuration);
		se.create(false, true);
	}

	public void createNewSchema() throws SQLException {
		Connection conn = DriverManager.getConnection(configuration.getProperty("hibernate.connection.url"),
			configuration.getProperty("hibernate.connection.username"),
			configuration.getProperty("hibernate.connection.password"));

		Statement stmt = conn.createStatement();
//		ResultSet result=stmt.executeQuery("SELECT schema_name FROM information_schema.schemata WHERE schema_name = '"+configuration.getProperty("hibernate.default_schema")+"';");
		boolean result = stmt.execute("CREATE SCHEMA IF NOT EXISTS "+configuration.getProperty("hibernate.default_schema")+" AUTHORIZATION "+configuration.getProperty("hibernate.connection.username"));
		stmt.close();
		conn.close();

		resetDatabase();
	}

}
