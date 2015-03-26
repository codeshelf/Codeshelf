package com.codeshelf.platform.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
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
	public void initializeTenant() {
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
		return CodeshelfSecurityManager.getCurrentTenant().getSchemaName();
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
}
