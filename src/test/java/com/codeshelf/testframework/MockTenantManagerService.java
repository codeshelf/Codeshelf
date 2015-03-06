package com.codeshelf.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;

import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.multitenancy.ITenantManagerService;
import com.codeshelf.platform.multitenancy.Shard;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService.ShutdownCleanupReq;
import com.codeshelf.platform.multitenancy.User;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

public class MockTenantManagerService implements ITenantManagerService {
	Tenant defaultTenant;
	MockTenantManagerService(Tenant defaultTenant) {
		this.defaultTenant = defaultTenant;
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
	public Shard getDefaultShard() {
		return Mockito.mock(Shard.class);
	}
	@Override
	public boolean canCreateUser(String username) {
		return false;
	}
	@Override
	public User createUser(Tenant tenant, String username, String password, UserType type) {
		return Mockito.mock(User.class);
	}
	@Override
	public User getUser(String username) {
		return Mockito.mock(User.class);
	}
	@Override
	public User authenticate(String username, String password) {
		return Mockito.mock(User.class);
	}
	@Override
	public void resetTenant(Tenant tenant) {
	}
	@Override
	public Tenant getTenantByUsername(String username) {
		return this.defaultTenant;
	}
	@Override
	public Tenant getTenantByName(String name) {
		return this.defaultTenant;
	}
	@Override
	public Tenant createTenant(String name, String shardName, String dbUsername) {
		return this.defaultTenant;
	}
	@Override
	public Tenant getDefaultTenant() {
		return this.defaultTenant;
	}
	@Override
	public Collection<Tenant> getTenants() {
		Collection<Tenant> tenants = new ArrayList<Tenant>(1);
		tenants.add(this.defaultTenant);
		return tenants;
	}
	@Override
	public void setShutdownCleanupRequest(ShutdownCleanupReq request) {
	}
}
