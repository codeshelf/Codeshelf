package com.codeshelf.testframework;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.mockito.Mockito;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.EventListenerIntegrator;
import com.codeshelf.persistence.TenantPersistenceService;

public class MockTenantPersistenceService extends TenantPersistenceService {
	Tenant defaultTenant = Mockito.mock(Tenant.class);
	Map<Class<? extends IDomainObject>,ITypedDao<?>> mockDaos;
	
	MockTenantPersistenceService(Map<Class<? extends IDomainObject>,ITypedDao<?>> mockDaos) {
		this.mockDaos = mockDaos;
	}
	
	@Override
	public EventListenerIntegrator getEventListenerIntegrator() {
		return new EventListenerIntegrator(Mockito.mock(ObjectChangeBroadcaster.class));
	}

	@Override
	public boolean hasAnyActiveTransactions() {
		return false;
	}

	@Override
	public boolean rollbackAnyActiveTransactions() {
		return false;
	}

	@Override
	public Transaction beginTransaction() {
		return Mockito.mock(Transaction.class);
	}

	@Override
	public void commitTransaction() {
	}

	@Override
	public void rollbackTransaction() {
	}

	@Override
	public Session getSession() {
		return Mockito.mock(Session.class);
	}

	@Override
	public Session getSessionWithTransaction() {
		return Mockito.mock(Session.class);
	}

	@Override
	public void awaitRunningOrThrow() {
	}

	@Override
	public void awaitTerminatedOrThrow() {
	}

	@Override
	public <T extends IDomainObject> ITypedDao<T> getDao(Class<T> classObject) {
		@SuppressWarnings("unchecked")
		ITypedDao<T> dao = (ITypedDao<T>) this.mockDaos.get(classObject);
		return dao;
	}

	@Override
	public String serviceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getStartupTimeoutSeconds() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getShutdownTimeoutSeconds() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void resetDaosForTest() {
	}

	@Override
	public <T extends IDomainObject> void setDaoForTest(Class<T> domainType, ITypedDao<T> testDao) {
		this.mockDaos.put(domainType, testDao);
	}

	@Override
	public Configuration getHibernateConfiguration() {
		return null;
	}

	@Override
	public String getMasterChangeLogFilename() {
		return null;
	}

	@Override
	public EventListenerIntegrator generateEventListenerIntegrator() {
		return null;
	}

	@Override
	public void forgetInitialActions(String tenantId) {
		
	}

	@Override
	public void forgetSchemaInitialization(String tenantIdentifier) {
		
	}

	@Override
	public ConnectionProvider getConnectionProvider(String tenantIdentifier, ServiceRegistryImplementor serviceRegistry) {
		return Mockito.mock(ConnectionProvider.class);
	}

	@Override
	public void forgetConnectionProvider(String tenantIdentifier) {
	}

}
