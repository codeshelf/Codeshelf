package com.codeshelf.manager;

import java.util.List;
import java.util.Set;

import com.codeshelf.model.domain.UserType;
import com.codeshelf.service.ICodeshelfService;

/**
 * API for Tenant Manager interface
 * 
 * @author ivan
 *
 */
public interface ITenantManagerService extends ICodeshelfService {
	// shards
	Shard getDefaultShard();
	
	// users
	boolean canCreateUser(String username);	
	User createUser(Tenant tenant,String username,String password,UserType type, Set<UserRole> roles);
	User getUser(Integer id);
	User getUser(String username);
	User updateUser(User user);
	User authenticate(String username,String password);
	List<User> getUsers(Tenant tenant);
	byte[] getHtpasswd();

	// tenants
	boolean canCreateTenant(String tenantName,String schemaName);	
	void resetTenant(Tenant tenant);
	Tenant getTenant(Integer id);
	Tenant getTenantByUsername(String username);
	Tenant getTenantByName(String name);
	Tenant createTenant(String name,String shardName,String dbUsername);
	Tenant updateTenant(Tenant tenant);
	Tenant getDefaultTenant();
	List<Tenant> getTenants();
	void deleteTenant(Tenant tenant); // needed for testing

	// roles
	List<UserRole> getRoles();
	UserRole getRole(Integer id);
	UserRole getRoleByName(String name);
	UserRole createRole(String name);
	UserRole updateRole(UserRole role);
	void deleteRole(UserRole role);
	
	// permissions
	List<UserPermission> getPermissions();
	UserPermission getPermission(Integer id);
	UserPermission getPermissionByDescriptor(String name);
	UserPermission createPermission(String name);
	UserPermission updatePermission(UserPermission permission);
	void deletePermission(UserPermission permission);
	
	// misc
	void setShutdownCleanupRequest(TenantManagerService.ShutdownCleanupReq request);


}
