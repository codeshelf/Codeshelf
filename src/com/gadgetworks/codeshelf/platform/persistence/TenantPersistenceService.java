package com.gadgetworks.codeshelf.platform.persistence;

import java.lang.reflect.Field;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.google.inject.Singleton;

@Singleton
public class TenantPersistenceService extends PersistenceService {
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

	public IPersistentCollection getDefaultCollection() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	public Transaction beginTenantTransaction() {
		return this.beginTransaction();
	}
	
	public void commitTenantTransaction() {
		this.commitTransaction();
	}
	
	public Session getCurrentTenantSession() {
		return this.getSession();
	}
}
