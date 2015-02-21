package com.codeshelf.platform.multitenancy;

import java.util.Collection;

import com.codeshelf.model.domain.UserType;
import com.google.common.util.concurrent.Service;

/**
 * API for Tenant Manager interface
 * 
 * @author ivan
 *
 */
public interface ITenantManager extends Service {
	// shards
	int getDefaultShardId();
	
	// users
	boolean canCreateUser(String username);	
	User createUser(Tenant tenant,String username,String password,UserType type);
	User getUser(String username);
	User authenticate(String username,String password);

	// tenants
	void resetTenant(Tenant tenant);
	Tenant getTenantByUsername(String username);
	Tenant getTenantByName(String name);
	Tenant createTenant(String name,int shardId,String dbUsername);
	Tenant getDefaultTenant();
	Collection<Tenant> getTenants();
}
