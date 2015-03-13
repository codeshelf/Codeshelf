package com.codeshelf.testframework;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;

import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.Shard;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService.ShutdownCleanupReq;
import com.codeshelf.manager.User;
import com.codeshelf.model.domain.UserType;
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
	public List<Tenant> getTenants() {
		List<Tenant> tenants = new ArrayList<Tenant>(1);
		tenants.add(this.defaultTenant);
		return tenants;
	}
	@Override
	public void setShutdownCleanupRequest(ShutdownCleanupReq request) {
	}
	@Override
	public String serviceName() {
		return this.getClass().getSimpleName();
	}
	@Override
	public void awaitRunningOrThrow() {
	}
	@Override
	public void awaitTerminatedOrThrow() {
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
	public List<User> getUsers(Tenant t) {
		List<User> list = new ArrayList<User>(1);
		list.add(getUser(""));
		return list;
	}
	@Override
	public User getUser(Integer id) {
		return getUser("");
	}
	@Override
	public User updateUser(User user) {
		return user;
	}
	@Override
	public Tenant getTenant(Integer id) {
		return this.defaultTenant;
	}
	@Override
	public Tenant updateTenant(Tenant tenant) {
		return tenant;
	}
	@Override
	public boolean canCreateTenant(String tenantName,String schemaName) {
		return true;
	}
	@Override
	public void destroyTenant(Tenant tenant) {
	}
	@Override
	public byte[] getHtpasswd() {
		return "".getBytes();
	}
}
