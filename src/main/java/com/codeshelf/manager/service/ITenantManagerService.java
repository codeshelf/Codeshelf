package com.codeshelf.manager.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codeshelf.manager.SecurityQuestion;
import com.codeshelf.manager.Shard;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserRole;
import com.codeshelf.service.CodeshelfService;

/**
 * API for Tenant Manager interface
 * 
 * @author ivan
 *
 */
public interface ITenantManagerService extends CodeshelfService {
	// shards
	Shard getDefaultShard();
	
	// users
	boolean canCreateUser(String username);	
	User createUser(Tenant tenant,String username,String password,Set<UserRole> roles);
	User getUser(Integer id);
	User getUser(String username);
	User updateUser(User user);
	User setSecurityAnswers(User user, Map<SecurityQuestion, String> questionAndAnswer);
	Map<String, String> getActiveSecurityQuestions();
	Map<String, SecurityQuestion> getAllSecurityQuestions();
	List<User> getUsers(Tenant tenant);
	List<User> getSiteControllerUsers(boolean onlyIfUpgradeNeeded);
	byte[] getHtpasswd();

	// tenants
	boolean canCreateTenant(String tenantName,String schemaName);	
	void resetTenant(Tenant tenant);
	Tenant getTenant(Integer id);
	Tenant getTenantByUser(User user);
	Tenant getTenantByName(String name);
	Tenant getTenantBySchemaName(String schemaName);
	Tenant createTenant(String name,String schemaName, String shardName);
	Tenant createTenant(String name,String schemaName); // on default shard
	Tenant updateTenant(Tenant tenant);
	Tenant getInitialTenant();
	List<Tenant> getTenants();
	void deleteTenant(Tenant tenant); // needed for testing

	// roles
	List<UserRole> getRoles(boolean includeRestrictedRoles);
	UserRole getRole(Integer id);
	UserRole getRoleByName(String name);
	UserRole createRole(String name);
	UserRole updateRole(UserRole role);
	void deleteRole(UserRole role);
	Set<UserRole> getUserRoles(String listOfRoles, boolean allowRestrictedRoles);
	
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
