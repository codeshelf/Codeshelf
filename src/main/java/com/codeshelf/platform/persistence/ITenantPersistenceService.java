package com.codeshelf.platform.persistence;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.platform.multitenancy.Tenant;

public interface ITenantPersistenceService extends IPersistenceService<Tenant> {
	public <T extends IDomainObject> ITypedDao<T> getDao(Class<T> classObject);
}
