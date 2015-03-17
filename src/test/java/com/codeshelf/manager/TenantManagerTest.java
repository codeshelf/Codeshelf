package com.codeshelf.manager;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.api.TenantsResource;
import com.codeshelf.manager.api.UsersResource;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.testframework.HibernateTest;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class TenantManagerTest extends HibernateTest {
	final public static Logger LOGGER = LoggerFactory.getLogger(TenantManagerTest.class);

	UsersResource usersResource;
	TenantsResource tenantsResource; 
	
	@Override
	public void doBefore() {
		super.doBefore();
		usersResource = new UsersResource();
		tenantsResource = new TenantsResource();
	}

	@Test
	public void defaultTenantExists() {
		Tenant tenant = this.tenantManagerService.getDefaultTenant();
		Assert.assertEquals(TenantManagerService.DEFAULT_TENANT_NAME, tenant.getName());
		
		Shard shard = this.tenantManagerService.getDefaultShard();
		Assert.assertEquals(TenantManagerService.DEFAULT_SHARD_NAME, shard.getName());
		 
		User user = this.tenantManagerService.getUser(CodeshelfNetwork.DEFAULT_SITECON_USERNAME);
		Assert.assertEquals(tenant,user.getTenant());
		Assert.assertEquals(shard,tenant.getShard());
		
		List<User> users = this.tenantManagerService.getUsers(null);
		Assert.assertTrue(users.contains(user));
		
		users = this.tenantManagerService.getUsers(tenant);
		Assert.assertTrue(users.contains(user));
		
		List<Tenant> tenants = this.tenantManagerService.getTenants();
		Assert.assertTrue(tenants.contains(tenant));		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void manipulateUsers() {
		String existingUsername = CodeshelfNetwork.DEFAULT_SITECON_USERNAME; 
		// cannot create user that already exists
		Assert.assertFalse(this.tenantManagerService.canCreateUser(existingUsername));
		
		// fails cleanly if you try
		Assert.assertNull(this.tenantManagerService.createUser(this.getDefaultTenant(), existingUsername, "12345", UserType.SITECON, null));
		
		// can authenticate default
		Assert.assertNotNull(this.tenantManagerService.authenticate(existingUsername,CodeshelfNetwork.DEFAULT_SITECON_PASS));
		
		// can fail to authenticate on wrong password
		Assert.assertNull(this.tenantManagerService.authenticate(existingUsername,"badpassword"));

		// (note: don't alter site controller user for this test, could have side effects)
		
		// can create new user		
		User newUser = this.tenantManagerService.createUser(this.getDefaultTenant(), "testuser", "goodpassword", UserType.APPUSER, null);		

		// can look up by id or name or list
		Assert.assertTrue(this.tenantManagerService.getUser(newUser.getId()).equals(newUser));
		Assert.assertTrue(this.tenantManagerService.getUser(newUser.getUsername()).equals(newUser));
		Assert.assertTrue(this.tenantManagerService.getUsers(null).contains(newUser));
		Assert.assertTrue(this.tenantManagerService.getUsers(newUser.getTenant()).contains(newUser));

		// can authenticate
		Assert.assertNotNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
		
		// can disable
		newUser.setActive(false);
		newUser = this.tenantManagerService.updateUser(newUser);
		Assert.assertNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
		
		// can reenable
		newUser.setActive(true);
		newUser = this.tenantManagerService.updateUser(newUser);
		Assert.assertNotNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
		
		// can change password
		newUser.setHashedPassword(authProviderService.hashPassword("newpassword"));
		newUser = this.tenantManagerService.updateUser(newUser);
		Assert.assertNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
		Assert.assertNotNull(this.tenantManagerService.authenticate(newUser.getUsername(),"newpassword"));
		
		// can look up via REST API several ways
		List<User> users = (List<User>) this.usersResource.get(null,null).getEntity();
		Assert.assertTrue(users.contains(newUser));
		Assert.assertEquals(this.getDefaultTenant().getId(), users.get(users.indexOf(newUser)).getTenant().getId());

		String htpasswd = (String) this.usersResource.getHtpasswd().getEntity();
		Assert.assertTrue(htpasswd.indexOf(newUser.getUsername()+":") >= 0);

		users = (List<User>) this.usersResource.get(null,newUser.getTenant().getId()).getEntity();
		Assert.assertTrue(users.contains(newUser));
		Assert.assertEquals(this.getDefaultTenant().getName(), users.get(users.indexOf(newUser)).getTenant().getName());

		User user = (User) this.usersResource.get(existingUsername,null).getEntity();
		Assert.assertTrue(user.getUsername().equals(existingUsername));
		
		user = (User) this.usersResource.getUser(newUser.getId()).getEntity();
		Assert.assertTrue(user.getUsername().equals(newUser.getUsername()));
		
		// can create via API
		MultivaluedMap<String,String> params = new MultivaluedMapImpl();
		params.putSingle("username", "apiuser");
		params.putSingle("password", "goodpassword");
		params.putSingle("type", "APPUSER");
		params.putSingle("tenantid", Integer.toString(this.getDefaultTenant().getId()));
		User apiUser = (User) this.usersResource.createUser(params).getEntity();
		Assert.assertTrue(apiUser.getUsername().equals("apiuser"));
		// TODO: secure auth over REST
		Assert.assertNotNull(this.tenantManagerService.authenticate("apiuser","goodpassword"));

		// create fails if user already exists
		Assert.assertNull(this.usersResource.createUser(params).getEntity());
		
		// create fails if duplicate parameter
		params.putSingle("username", "apiuser2");
		params.add("type","SITECON");
		Assert.assertNull(this.usersResource.createUser(params).getEntity());
		params.putSingle("type","SITECON");
		
		// create fails if unrecognized parameter
		params.putSingle("garbage", "wakkawakka");
		Assert.assertNull(this.usersResource.createUser(params).getEntity());
		params.remove("garbage");

		// but works on a clean map even if it failed before
		Assert.assertNotNull(this.usersResource.createUser(params).getEntity());
		
		// fails to update if unrecognized parameter
		params.clear();
		params.putSingle("password", "newpassword");
		params.putSingle("garbage", "");
		Assert.assertNull(this.usersResource.updateUser(apiUser.getId(), params).getEntity());
		params.remove("garbage");
		
		// succeeds update with clean map
		Assert.assertNotNull(this.usersResource.updateUser(apiUser.getId(), params).getEntity());
		Assert.assertNull(this.tenantManagerService.authenticate("apiuser","goodpassword"));
		Assert.assertNotNull(this.tenantManagerService.authenticate("apiuser","newpassword"));

		// can disable
		params.clear();
		params.putSingle("active", "false");
		Assert.assertNotNull(this.usersResource.updateUser(apiUser.getId(), params).getEntity());
		Assert.assertNull(this.tenantManagerService.authenticate("apiuser","newpassword"));
		
		// and reenable
		params.putSingle("active", "true");
		Assert.assertNotNull(this.usersResource.updateUser(apiUser.getId(), params).getEntity());
		Assert.assertNotNull(this.tenantManagerService.authenticate("apiuser","newpassword"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void manipulateTenants() {
		String shardName = TenantManagerService.DEFAULT_SHARD_NAME;
		// cannot create tenant that already exists
		Assert.assertFalse(this.tenantManagerService.canCreateTenant(TenantManagerService.DEFAULT_TENANT_NAME, "whatever"));
		
		// fails cleanly if you try
		Assert.assertNull(this.tenantManagerService.createTenant(TenantManagerService.DEFAULT_TENANT_NAME, shardName, "bob"));
		
		// can create new tenant
		Tenant newTenant = this.tenantManagerService.createTenant("New Tenant", shardName, "alice");
		Assert.assertNotNull(newTenant);
		// with user
		User newUser = this.tenantManagerService.createUser(newTenant, "tenantuser", "goodpassword", UserType.APPUSER, null);		
		// can authenticate 
		Assert.assertNotNull(this.tenantManagerService.authenticate("tenantuser", "goodpassword"));

		// can look up by id or name or list
		Assert.assertTrue(this.tenantManagerService.getTenant(newTenant.getId()).equals(newTenant));
		Assert.assertTrue(this.tenantManagerService.getTenantByName(newTenant.getName()).equals(newTenant));
		Assert.assertTrue(this.tenantManagerService.getTenantByUsername(newUser.getUsername()).equals(newTenant));

		// can disable
		newTenant.setActive(false);
		newTenant = this.tenantManagerService.updateTenant(newTenant);
		Assert.assertNull(this.tenantManagerService.authenticate("tenantuser", "goodpassword"));
		
		// can reenable
		newTenant.setActive(true);
		newTenant = this.tenantManagerService.updateTenant(newTenant);
		Assert.assertNotNull(this.tenantManagerService.authenticate("tenantuser", "goodpassword"));
		
		// can look up via REST API couple of ways
		List<Tenant> tenants = (List<Tenant>) this.tenantsResource.get().getEntity();
		Assert.assertTrue(tenants.contains(newTenant));

		Tenant tenant = (Tenant) this.tenantsResource.getTenant(newTenant.getId()).getEntity();
		Assert.assertTrue(tenant.getName().equals(newTenant.getName()));
		
		// can create via API
		MultivaluedMap<String,String> params = new MultivaluedMapImpl();
		params.putSingle("name", "mytenant");
		params.putSingle("schemaname", "myschema");
		Tenant apiTenant = (Tenant) this.tenantsResource.createTenant(params).getEntity();
		Assert.assertTrue(apiTenant.getName().equals("mytenant"));
		// TODO: create user by API 
		newUser = this.tenantManagerService.createUser(apiTenant, "apiuser", "goodpassword", UserType.APPUSER, null);		
		// can authenticate 
		Assert.assertNotNull(this.tenantManagerService.authenticate("apiuser", "goodpassword"));

		// create fails if tenant already exists
		Assert.assertNull(this.tenantsResource.createTenant(params).getEntity());
		
		// create fails if duplicate parameter
		params.putSingle("name", "mytenant2");
		params.putSingle("schemaname", "myschema2");
		params.add("name","can I have two names?");
		Assert.assertNull(this.tenantsResource.createTenant(params).getEntity());
		params.putSingle("name","mytenant2");
		
		// create fails if unrecognized parameter
		params.putSingle("garbage", "wakkawakka");
		Assert.assertNull(this.tenantsResource.createTenant(params).getEntity());
		params.remove("garbage");

		// but works on a clean map even if it failed before
		Assert.assertNotNull(this.tenantsResource.createTenant(params).getEntity());
		
		// fails to update if unrecognized parameter
		params.clear();
		params.putSingle("name", "newname");
		params.putSingle("garbage", "");
		Assert.assertNull(this.tenantsResource.updateTenant(apiTenant.getId(), params).getEntity());
		params.remove("garbage");
		
		// succeeds update with clean map
		Assert.assertNotNull(this.tenantsResource.updateTenant(apiTenant.getId(), params).getEntity());
		apiTenant = (Tenant) this.tenantsResource.getTenant(apiTenant.getId()).getEntity();
		Assert.assertTrue(apiTenant.getName().equals("newname"));

		// can disable
		Assert.assertTrue(apiTenant.isActive());
		params.clear();
		params.putSingle("active", "false");
		Assert.assertNotNull(this.tenantsResource.updateTenant(apiTenant.getId(), params).getEntity());
		apiTenant = this.tenantManagerService.getTenantByName(apiTenant.getName());
		Assert.assertNotNull(apiTenant);
		Assert.assertFalse(apiTenant.isActive());
		Assert.assertNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
		
		// and reenable
		params.putSingle("active", "true");
		apiTenant = (Tenant) this.tenantsResource.updateTenant(apiTenant.getId(), params).getEntity();
		Assert.assertNotNull(apiTenant);
		Assert.assertTrue(apiTenant.isActive());
		newUser = this.tenantManagerService.getUser(newUser.getId());
		Assert.assertNotNull(this.tenantManagerService.authenticate(newUser.getUsername(),"goodpassword"));
	}

}
