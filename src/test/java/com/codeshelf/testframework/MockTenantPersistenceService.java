package com.codeshelf.testframework;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.mockito.Mockito;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

public class MockTenantPersistenceService implements ITenantPersistenceService {
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
	public ListenableFuture<State> start() {
		return null;
	}

	@Override
	public State startAndWait() {
		return State.RUNNING;
	}

	@Override
	public Service startAsync() {
		return this;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public State state() {
		return State.RUNNING;
	}

	@Override
	public ListenableFuture<State> stop() {
		return null;
	}

	@Override
	public State stopAndWait() {
		return State.RUNNING;
	}

	@Override
	public Service stopAsync() {
		return this;
	}

	@Override
	public void awaitRunning() {
	}

	@Override
	public void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
	}

	@Override
	public void awaitTerminated() {
	}

	@Override
	public void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
	}

	@Override
	public Throwable failureCause() {
		return null;
	}

	@Override
	public void addListener(Listener listener, Executor executor) {
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

}
