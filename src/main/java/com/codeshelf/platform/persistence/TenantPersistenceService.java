package com.codeshelf.platform.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.util.ConcurrentHashSet;
import org.hibernate.Transaction;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Path;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.google.inject.Inject;

public class TenantPersistenceService extends PersistenceService implements ITenantPersistenceService {
	private static final String TENANT_CHANGELOG_FILENAME= "liquibase/db.changelog-master.xml";

	@SuppressWarnings("unused")
	private static final Logger LOGGER	= LoggerFactory.getLogger(TenantPersistenceService.class);
	
	@Inject
	private static ITenantPersistenceService theInstance;

	Configuration hibernateConfiguration;
	private Map<Class<? extends IDomainObject>,ITypedDao<?>> daos;

	private ConcurrentHashMap<String,ConnectionProvider> connectionProviders = new ConcurrentHashMap<String,ConnectionProvider>();
	private ConcurrentHashSet<String> initalizingConnectionProviders = new ConcurrentHashSet<String>();
	
	@Inject
	private TenantPersistenceService() {
		super();
	}

	public final synchronized static ITenantPersistenceService getMaybeRunningInstance() {
		return theInstance;
	}
	public final synchronized static ITenantPersistenceService getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final synchronized static boolean exists() {
		return (theInstance != null);
	}
	
	/**
	 * singleton service: access before service is initialized will block; 
	 * only the service manager should start service 
	 */
	public final static ITenantPersistenceService getInstance() {
		getMaybeRunningInstance().awaitRunningOrThrow();		
		return theInstance;
	}
	public final static void setInstance(ITenantPersistenceService instance) {
		// for testing only!
		theInstance = instance;
	}
	
	@Override
	public EventListenerIntegrator generateEventListenerIntegrator() {
		return new EventListenerIntegrator(new ObjectChangeBroadcaster());
	}

	@SuppressWarnings("unchecked")
	public <T extends IDomainObject> ITypedDao<T> getDao(Class<T> classObject) {
		if (classObject==null) {
			throw new NullPointerException("classObject was null calling getDao");
		}
		return (ITypedDao<T>) this.daos.get(classObject);
	}

	@Override
	protected void startUp() throws Exception {
		setupDaos();
		super.startUp();
	}

	private void setupDaos() {
		this.daos = new HashMap<Class<? extends IDomainObject>, ITypedDao<?>>();
		this.daos.putAll(DomainObjectABC.getDaos());
	}

	@Override
	public void resetDaosForTest() {
		setupDaos();
	}

	@Override
	public <T extends IDomainObject> void setDaoForTest(Class<T> domainType, ITypedDao<T> testDao) {
		this.daos.put(domainType,testDao);
	}

	@Override
	public Configuration getHibernateConfiguration() {
		if(hibernateConfiguration == null) {
			
			hibernateConfiguration = new Configuration().configure(getHibernateConfigurationFilename());
			// not tenant specific - our connection provider will add database URL and credentials
		}
		return hibernateConfiguration;
	}

	@Override
	public String getMasterChangeLogFilename() {
		return TENANT_CHANGELOG_FILENAME;
	}

	@Override
	public String getHibernateConfigurationFilename() {
		return "hibernate/"+System.getProperty("tenant.hibernateconfig");
	}

	@Override
	public void initializeTenantData() {
		Transaction t = this.beginTransaction();

		List<Facility> facilities = Facility.staticGetDao().getAll();
		
		for (Facility facility : facilities) {
			for (Path path : facility.getPaths()) {
				// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
				facility.recomputeLocationPathDistances(path);
			}
		}
        t.commit();

        t = this.beginTransaction();
        // create or update tenant default settings
        PropertyDao.getInstance().syncPropertyDefaults();
        t.commit();
	}

	@Override
	public String getCurrentTenantIdentifier() {
		return CodeshelfSecurityManager.getCurrentTenant().getTenantIdentifier();
	}

	@Override
	protected DatabaseCredentials getDatabaseCredentials(String tenantIdentifier) {
		return TenantManagerService.getInstance().getTenantBySchemaName(tenantIdentifier);
	}

	@Override
	protected DatabaseCredentials getSuperDatabaseCredentials(String tenantIdentifier) {
		Tenant tenant = TenantManagerService.getInstance().getTenantBySchemaName(tenantIdentifier);
		return tenant.getShard();
	}
	

	private ConnectionProvider createConnectionProvider(Tenant tenant, ServiceRegistryImplementor serviceRegistry) {
	    // in synchronized, do not do long actions here

		// create connection provider for this tenant
		C3P0ConnectionProvider cp = new C3P0ConnectionProvider();
	    cp.injectServices(serviceRegistry);
	    
	    // configure the provider (we might be in init, trust that we can at least get a hibernate config)
	    Configuration genericConfiguration = TenantPersistenceService.getMaybeRunningInstance().getHibernateConfiguration();
	    Map<Object,Object> properties = new HashMap<Object,Object>(genericConfiguration.getProperties());
	    
	    properties.put("hibernate.connection.url", tenant.getUrl());
	    properties.put("hibernate.connection.username", tenant.getUsername());
	    properties.put("hibernate.connection.password", tenant.getPassword());
	    properties.put("hibernate.default_schema", tenant.getSchemaName());
	    
	    cp.configure(properties);
	    
	    return cp;
	}

	@Override
	public ConnectionProvider getConnectionProvider(String tenantIdentifier, ServiceRegistryImplementor serviceRegistry) {
		if(!connectionProviders.containsKey(tenantIdentifier)) {
			synchronized(this.initalizingConnectionProviders) {
				if(!this.initalizingConnectionProviders.contains(tenantIdentifier)) {
					this.initalizingConnectionProviders.add(tenantIdentifier);

					LOGGER.info("Creating connection to tenant {}",tenantIdentifier);
					Tenant tenant = TenantManagerService.getInstance().getTenantBySchemaName(tenantIdentifier);
					connectionProviders.put(tenantIdentifier, createConnectionProvider(tenant,serviceRegistry));
				} else {
					throw new RuntimeException("This shouldn't happen - not recursive and other threads should block");
				}
			}			
		}		
		return connectionProviders.get(tenantIdentifier);
	}

	@Override
	public void forgetConnectionProvider(String tenantIdentifier) {
		ConnectionProvider cp = this.connectionProviders.get(tenantIdentifier);
		if(cp != null) {
			if(cp instanceof C3P0ConnectionProvider) {
				((C3P0ConnectionProvider)cp).stop();	
			}
			this.connectionProviders.remove(tenantIdentifier);
		}
		this.initalizingConnectionProviders.remove(tenantIdentifier);
	}

}
