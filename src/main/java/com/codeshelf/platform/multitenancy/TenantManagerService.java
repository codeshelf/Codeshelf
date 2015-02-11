package com.codeshelf.platform.multitenancy;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

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
import com.codeshelf.platform.Service;
import com.codeshelf.platform.persistence.PersistenceService;
import com.codeshelf.platform.persistence.SchemaUtil;
import com.google.inject.Singleton;

@Singleton
public class TenantManagerService extends Service implements ITenantManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TenantManagerService.class);
	public static final String DEFAULT_SHARD_NAME = "default";
	public static final String DEFAULT_TENANT_NAME = "default";
	private static TenantManagerService theInstance = null;
	
	ManagerPersistenceService managerPersistenceService;
	
	@Getter
	int defaultShardId = -1;
	
	@Deprecated
	@Getter
	int defaultTenantId = -1;
	
	private TenantManagerService() {
		super();
		setInstance();
	}
	
	private void setInstance() {
		TenantManagerService.theInstance = this;
	}
	
	public final synchronized static ITenantManager getInstance() {
		if (theInstance == null) {
			theInstance = new TenantManagerService();
			theInstance.start();
			//LOGGER.warn("Unless this is a test, PersistanceService should have been initialized already but was not!");
		}
		else if (!theInstance.isRunning()) {
			theInstance.start();
			LOGGER.info("PersistanceService was stopped and restarted");
		}
		return theInstance;
	}

	@Override
	public boolean start() {
		if(isRunning()) {
			LOGGER.error("tried to start TenantManagerService but was already running");
			return false;
		} // else
		managerPersistenceService=ManagerPersistenceService.getInstance();
		
		initDefaultShard();
		
		this.setRunning(true);
		return true;
	}
	
	private void initDefaultShard() {
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
			shard.setDbUrl(System.getProperty("shard.default.db.url"));
			shard.setDbAdminUsername(System.getProperty("shard.default.db.admin_username"));
			shard.setDbAdminPassword(System.getProperty("shard.default.db.admin_password"));
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

	@Override
	public boolean stop() {
		if(isRunning()) {
			managerPersistenceService.stop();
			this.setRunning(false);
			return true;
		} // else
		LOGGER.error("Tried to stop TenantManagerService but was not running");
		return false;
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
	
	@Override
	public boolean connect() {
		if(!isRunning()) {
			start();
		}
		return true;
	}

	@Override
	public void disconnect() {
		if(isRunning()) {
			stop();
		}
	}
	
	//////////////////////////// Manager Service API ////////////////////////////////
	
	@Override
	public boolean canCreateUser(String username) {
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
		SchemaExport se = new SchemaExport(SchemaUtil.getHibernateConfiguration(tenant));
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
		SchemaUtil.executeSQL(tenant,"UPDATE "+schemaName+".order_header SET container_use_persistentid=null");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".container_use");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".work_instruction");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".container");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".order_location");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".order_detail");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".order_header");
		SchemaUtil.executeSQL(tenant,"DELETE FROM "+schemaName+".order_group");
	}

	public static void dropSchema(Tenant tenant) throws SQLException {
		String schemaName = tenant.getSchemaName();
		LOGGER.warn("Deleting tenant schema "+schemaName);
		SchemaUtil.executeSQL(tenant,"DROP SCHEMA "+schemaName+
			((SchemaUtil.getSQLSyntax(tenant.getUrl())==PersistenceService.SQLSyntax.H2)?"":" CASCADE"));
	}

}