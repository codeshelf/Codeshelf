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

public class TenantPersistenceService extends PersistenceService<Tenant> {
	private static final Logger LOGGER	= LoggerFactory.getLogger(TenantPersistenceService.class);
	
	private static TenantPersistenceService theInstance = null;

	private TenantPersistenceService() {
		super();
	}

	public final synchronized static TenantPersistenceService getInstance() {
		if (theInstance == null) {
			theInstance = new TenantPersistenceService();
			theInstance.start();
		}
		else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was stopped and restarted");
		}
		return theInstance;
	}

	@Override
	public Tenant getDefaultSchema() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	@Override
	protected void performStartupActions(Tenant schema) {
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
