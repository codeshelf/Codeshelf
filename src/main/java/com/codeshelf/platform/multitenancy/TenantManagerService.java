package com.codeshelf.platform.multitenancy;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.Getter;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.persistence.DatabaseConnection;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;

public class TenantManagerService extends AbstractIdleService implements ITenantManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TenantManagerService.class);
	public static final String DEFAULT_SHARD_NAME = "default";
	public static final String DEFAULT_TENANT_NAME = "default";
	public static final int MAX_TENANT_MANAGER_WAIT_SECS = 15;
	private static TenantManagerService theInstance = null;
	
	@Getter
	int defaultShardId = -1;
	
	@Deprecated
	@Getter
	int defaultTenantId = -1;
	
	private TenantManagerService() {
		super();
	}
	
	public final synchronized static ITenantManager getMaybeRunningInstance() {
		if (theInstance == null) {
			theInstance = new TenantManagerService();
		}
		return theInstance;
	}
	public final static ITenantManager getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.serviceName());
		}
		return theInstance;
	}
	public final static ITenantManager getInstance() {
		try {
			getMaybeRunningInstance().awaitRunning(MAX_TENANT_MANAGER_WAIT_SECS,TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Timeout contacting tenant manager",e);
		}
		return theInstance;
	}
	
	private void initDefaultShard() {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		Session session = managerPersistenceService.getSessionWithTransaction();
		Criteria criteria = session.createCriteria(Shard.class);
		criteria.add(Restrictions.eq("name", DEFAULT_SHARD_NAME));
		
		@SuppressWarnings("unchecked")
		List<Shard> listShard = criteria.list();

		Tenant tenant = null;
		if(listShard.size() == 0) {
			// create
			Shard shard = new Shard();
			shard.setName(DEFAULT_SHARD_NAME);
			shard.setUrl(System.getProperty("shard.default.db.url"));
			shard.setUsername(System.getProperty("shard.default.db.admin_username"));
			shard.setPassword(System.getProperty("shard.default.db.admin_password"));
			session.save(shard);

			this.defaultShardId = shard.getShardId();
			
			tenant = initDefaultTenant(session,shard);
		} else if(listShard.size() == 1) {
			// use existing
			this.defaultShardId = listShard.get(0).getShardId();
		} else {
			LOGGER.error("got more than one default shard, cannot initialize");
		}
		managerPersistenceService.commitTransaction();

		if(tenant != null) {
			// Create initial users
			createUser(tenant,"a@example.com", "testme",UserType.APPUSER); //view
			createUser(tenant,"view@example.com", "testme",UserType.APPUSER); //view
			createUser(tenant,"configure@example.com", "testme",UserType.APPUSER); //all
			createUser(tenant,"simulate@example.com", "testme",UserType.APPUSER); //simulate + configure
			createUser(tenant,"che@example.com", "testme",UserType.APPUSER); //view + simulate
			createUser(tenant,"work@example.com", "testme",UserType.APPUSER); //view + simulate
			
			createUser(tenant,"view@accu-logistics.com", "accu-logistics",UserType.APPUSER); //view

			createUser(tenant,Integer.toString(CodeshelfNetwork.DEFAULT_SITECON_SERIAL),CodeshelfNetwork.DEFAULT_SITECON_PASS,UserType.SITECON);
		}

	}

	@Deprecated
	private Tenant initDefaultTenant(Session session,Shard shard) {
		// must be called within active transaction
		Tenant tenant = shard.getTenant(DEFAULT_TENANT_NAME);
		if(tenant == null) {
			String dbSchemaName = System.getProperty("tenant.default.schema");
			String dbUsername = System.getProperty("tenant.default.username");
			String dbPassword = System.getProperty("tenant.default.password");
			
			tenant = shard.createTenant(DEFAULT_TENANT_NAME, dbSchemaName, dbUsername, dbPassword);
		}
		this.defaultTenantId = tenant.getTenantId();
		return tenant;
	}

	@SuppressWarnings("unchecked")
	private User getUser(Session session,String username) {
		User result = null;

		Criteria criteria = session.createCriteria(User.class);
		criteria.add(Restrictions.eq("username", username));

		List<User> userMatch = null;
		
		try {
			userMatch = (List<User>)criteria.list();
		} catch (HibernateException e) {
			LOGGER.error("",e);;
		}
		
		if(userMatch == null) {
			LOGGER.error("Unable to load user "+ username);
		} else if(userMatch.isEmpty()) {
			LOGGER.trace("No user named "+ username);
		} else if(userMatch.size() > 1) {
			LOGGER.error("More than 1 match for "+ username);
		} else {
			// ok 
			result = userMatch.get(0);
		}

		return result;
	}
	
	public boolean checkUsername(Session session,String username) {
		User user = getUser(session,username);
		return (user == null);
	}
	
	//////////////////////////// Manager Service API ////////////////////////////////
	
	@Override
	public boolean canCreateUser(String username) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();
		// for UI
		boolean result = false;
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = checkUsername(session,username);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}
	
	@Override
	public User createUser(Tenant tenant,String username,String password,UserType type) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		User result=null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			tenant = (Tenant) session.load(Tenant.class, tenant.getTenantId());
			if(checkUsername(session,username)) {
				// ok to create
				User user = new User();
				user.setUsername(username);
				user.setPassword(password);
				user.setType(type);
				tenant.addUser(user);
				session.save(user);
				result = user;
			} else {
				LOGGER.error("Tried to create duplicate username "+username);
			}
		} finally {
			managerPersistenceService.commitTransaction();	
		}        
		return result;
	}

	@Override
	public void resetTenant(Tenant tenant) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		LOGGER.warn("Resetting schema and users for "+tenant.toString());
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			tenant = (Tenant) session.load(Tenant.class, tenant.getTenantId());
			
			// remove all users
			for(User u : tenant.getUsers()) {
				tenant.removeUser(u);
				session.delete(u);
			}

		} finally {
			managerPersistenceService.commitTransaction();	
		}        
		// reset schema
		SchemaExport se = new SchemaExport(tenant.getHibernateConfiguration());
		se.create(false, true);
	}

	@Override
	public User authenticate(String username,String password) {
		User user = getUser(username);
		if(user!=null) {
			if(user.isPasswordValid(password)) {
				return user;
			}
		}
		return null;
	}
	
	@Override
	public User getUser(String username) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		User result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			User user = getUser(session,username); 
			if(user != null) {
				result = user;	
			} else {
				LOGGER.warn("authentication failed for user "+username);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}

	@Override
	public Tenant getTenantByUsername(String username) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		Tenant result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			User user = getUser(session,username); 
			if(user != null) {
				result = user.getTenant();
			}		
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public Tenant getTenantByName(String name) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		Tenant result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Tenant.class);
			criteria.add(Restrictions.eq("name", name));
			
			@SuppressWarnings("unchecked")
			List<Tenant> tenantList = criteria.list();
			
			if(tenantList != null && tenantList.size() == 1) {
				result = tenantList.get(0);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}

	@Override
	public Tenant createTenant(String name, int shardId, String dbUsername) {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		// use username as schemaname when creating tenant
		String schemaName = dbUsername;
		
		Tenant result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Shard shard = (Shard) session.get(Shard.class, shardId);
			result = shard.createTenant(name,schemaName,dbUsername,null);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Tenant> getTenants() {
		ManagerPersistenceService managerPersistenceService=ManagerPersistenceService.getInstance();

		Collection<Tenant> results = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Tenant.class);
			results = criteria.list();
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return results;
	}

	@Override
	public Tenant getDefaultTenant() {
		return getTenantByName(TenantManagerService.DEFAULT_TENANT_NAME);
	}

	public static void deleteOrdersWis(Tenant tenant) throws SQLException {
		String schemaName = tenant.getSchemaName();
		LOGGER.warn("Deleting all orders and work instructions from schema "+schemaName);
		tenant.executeSQL("UPDATE "+schemaName+".order_header SET container_use_persistentid=null");
		tenant.executeSQL("DELETE FROM "+schemaName+".container_use");
		tenant.executeSQL("DELETE FROM "+schemaName+".work_instruction");
		tenant.executeSQL("DELETE FROM "+schemaName+".container");
		tenant.executeSQL("DELETE FROM "+schemaName+".order_location");
		tenant.executeSQL("DELETE FROM "+schemaName+".order_detail");
		tenant.executeSQL("DELETE FROM "+schemaName+".order_header");
		tenant.executeSQL("DELETE FROM "+schemaName+".order_group");
	}
	
	public static void deleteOrdersWisInventory(Tenant tenant) throws SQLException {
		String schemaName = tenant.getSchemaName();
		TenantManagerService.deleteOrdersWis(tenant);
		LOGGER.warn("Deleting itemMasters ");
		tenant.executeSQL("DELETE FROM "+schemaName+".item");
		tenant.executeSQL("DELETE FROM "+schemaName+".item_master");
	}

	public static void dropSchema(Tenant tenant) throws SQLException {
		String schemaName = tenant.getSchemaName();
		LOGGER.warn("Deleting tenant schema "+schemaName);
		tenant.executeSQL("DROP SCHEMA "+schemaName+
			((tenant.getSQLSyntax()==DatabaseConnection.SQLSyntax.H2)?"":" CASCADE"));
	}

	@Override
	protected void startUp() throws Exception {
		initDefaultShard();
	}

	@Override
	protected void shutDown() throws Exception {
	}

}
