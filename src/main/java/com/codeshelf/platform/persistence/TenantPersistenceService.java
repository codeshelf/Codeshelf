package com.codeshelf.platform.persistence;

import java.lang.reflect.Field;

import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.google.inject.Inject;

public class TenantPersistenceService extends PersistenceServiceImpl<Tenant> implements ITenantPersistenceService {
	private static final Logger LOGGER	= LoggerFactory.getLogger(TenantPersistenceService.class);
	
	@Inject
	private static ITenantPersistenceService theInstance;

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
	public Tenant getDefaultSchema() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	@Override
	protected void initialize(Tenant schema) {
		Transaction t = this.beginTransaction(schema);
		PropertyDao.getInstance().syncPropertyDefaults();
        t.commit();		
	}
	
	@Override
	protected EventListenerIntegrator generateEventListenerIntegrator() {
		return new EventListenerIntegrator(new ObjectChangeBroadcaster());
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
}
