package com.codeshelf.platform.persistence;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;

public interface ITenantPersistenceService extends IPersistenceService {

	public <T extends IDomainObject> ITypedDao<T> getDao(Class<T> classObject);

	// methods for testing
	public void resetDaosForTest();
	public <T extends IDomainObject> void setDaoForTest(Class<T> domainType, ITypedDao<T> testDao);

	ConnectionProvider getConnectionProvider(String tenantIdentifier, ServiceRegistryImplementor serviceRegistry);
	void forgetConnectionProvider(String tenantIdentifier);
}
