package com.codeshelf.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import lombok.Setter;

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
import com.codeshelf.platform.persistence.PersistenceService;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ServiceUtility;
import com.google.inject.Inject;

public class TenantManagerService extends AbstractCodeshelfIdleService implements ITenantManagerService {
	public enum ShutdownCleanupReq {
	NONE , 
	DROP_SCHEMA , 
	DELETE_ORDERS_WIS ,
	DELETE_ORDERS_WIS_INVENTORY
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TenantManagerService.class);
	public static final String DEFAULT_SHARD_NAME = "default";
	public static final String DEFAULT_TENANT_NAME = "default";
	//@Getter
	//int defaultShardId = -1;
	
	private Tenant defaultTenant = null;

	@Setter
	ShutdownCleanupReq shutdownCleanupRequest = ShutdownCleanupReq.NONE;

	@Inject
	private static ITenantManagerService theInstance;
	
	@Inject
	private TenantManagerService() {
		super();
	}
	
	public final synchronized static ITenantManagerService getMaybeRunningInstance() {
		return theInstance;
	}
	public final static ITenantManagerService getNonRunningInstance() {
		if(!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "+theInstance.getClass().getSimpleName());
		}
		return theInstance;
	}
	public final static ITenantManagerService getInstance() {
		ITenantManagerService instance = theInstance;
		ServiceUtility.awaitRunningOrThrow(instance);
		return theInstance;
	}
	public final static void setInstance(ITenantManagerService testInstance) {
		// testing only!
		theInstance = testInstance;
	}
	public static boolean exists() {
		return (theInstance != null);
	}
	
	private void initDefaultShard() {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();
		boolean initDefaultTenant = false;
		Shard shard = null;
		
		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			Criteria criteria = session.createCriteria(Shard.class);
			criteria.add(Restrictions.eq("name", DEFAULT_SHARD_NAME));
			
			@SuppressWarnings("unchecked")
			List<Shard> listShard = criteria.list();

			if(listShard.size() == 0) {
				// create
				shard = new Shard();
				shard.setName(DEFAULT_SHARD_NAME);
				shard.setUrl(System.getProperty("shard.default.db.url"));
				shard.setUsername(System.getProperty("shard.default.db.admin_username"));
				shard.setPassword(System.getProperty("shard.default.db.admin_password"));
				session.save(shard);
				initDefaultTenant = true;
			} else if(listShard.size() > 1) {
				LOGGER.error("got more than one default shard, cannot initialize");
			} // else 1 default shard, continue
		} finally {
			if(initDefaultTenant)
				managerPersistenceService.commitTransaction();
			else 
				managerPersistenceService.rollbackTransaction();
		}

		if(initDefaultTenant) {
			Tenant tenant = initDefaultTenant(session,shard);
			createDefaultUsers(tenant);
		}
		
	}

	private void createDefaultUsers(Tenant tenant) {
		// Create initial users
		createUser(tenant,"a@example.com", "testme",UserType.APPUSER); //view
		createUser(tenant,"view@example.com", "testme",UserType.APPUSER); //view
		createUser(tenant,"configure@example.com", "testme",UserType.APPUSER); //all
		createUser(tenant,"simulate@example.com", "testme",UserType.APPUSER); //simulate + configure
		createUser(tenant,"che@example.com", "testme",UserType.APPUSER); //view + simulate
		createUser(tenant,"work@example.com", "testme",UserType.APPUSER); //view + simulate
		
		createUser(tenant,"view@accu-logistics.com", "accu-logistics",UserType.APPUSER); //view

		createUser(tenant,CodeshelfNetwork.DEFAULT_SITECON_USERNAME,CodeshelfNetwork.DEFAULT_SITECON_PASS,UserType.SITECON);
	}

	private Tenant initDefaultTenant(Session session,Shard shard) {
		Tenant tenant = shard.getTenant(DEFAULT_TENANT_NAME);
		if(tenant == null) {
			String dbSchemaName = System.getProperty("tenant.default.schema");
			String dbUsername = System.getProperty("tenant.default.username");
			String dbPassword = System.getProperty("tenant.default.password");
			
			tenant = shard.createTenant(DEFAULT_TENANT_NAME, dbSchemaName, dbUsername, dbPassword);
		}
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
			result.getTenant().getName();
			result = ManagerPersistenceService.<User>deproxify(result);
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
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();
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
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		User result=null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			tenant = (Tenant) session.load(Tenant.class, tenant.getId());
			if(checkUsername(session,username)) {
				// ok to create
				User user = new User();
				user.setUsername(username);
				user.setPassword(password);
				user.setType(type);
				tenant.addUser(user);
				session.save(tenant);
				session.save(user);
				result = user;
				LOGGER.info("Created user {} for tenant {}",username,tenant.getName());
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
		eraseAllTenantData(tenant);

		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		LOGGER.warn("Resetting schema and users for "+tenant.toString());
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();

			// reload detached tenant (might've added more users)
			tenant = inflate((Tenant) session.load(Tenant.class, tenant.getId()));
			
			// remove all users except site controller
			List<User> users = tenant.getUserList();
			for(User u : users) {
				u = (User) session.load(User.class, u.getId());
				if(u.getType() != UserType.SITECON || !u.getUsername().equals(CodeshelfNetwork.DEFAULT_SITECON_USERNAME)) {
					tenant.removeUser(u);
					session.delete(u);
				}
			}

		} finally {
			managerPersistenceService.commitTransaction();	
		}        
		
		// resetTenant for testing - not necessary to recreate default users here
		//createDefaultUsers(tenant);
	}

	private void eraseAllTenantData(Tenant tenant) {		
		String sql = "SET REFERENTIAL_INTEGRITY FALSE;";
		for(String tableName : getTableNames(tenant)) {
			sql += "TRUNCATE TABLE "+tenant.getSchemaName()+"."+tableName+";";
		}
		sql += "SET REFERENTIAL_INTEGRITY TRUE";
		try {
			tenant.executeSQL(sql);
		} catch (SQLException e) {
			LOGGER.error("Truncate of tenant tables failed, falling back on SchemaExport", e);
			// reset schema old way, hbm2ddl
			SchemaExport se = new SchemaExport(tenant.getHibernateConfiguration());
			se.create(false, true);
		}
	}

	public Set<String> getTableNames(Tenant tenant) {
		Set<String> tableNames = new HashSet<String>();
		Iterator<org.hibernate.mapping.Table> tables = tenant.getHibernateConfiguration().getTableMappings();
		while(tables.hasNext()) {
			org.hibernate.mapping.Table table = tables.next();
			tableNames.add(table.getName());
		}
		return tableNames;
	}

	@Override
	public User authenticate(String username,String password) {
		User user = getUser(username);
		if(user!=null) {
			boolean passwordValid = user.isPasswordValid(password);
			if(user.getTenant().isActive()) {
				if(user.isActive()) {
					if(passwordValid) {
						LOGGER.debug("Password valid for user {}",user);
						return user;
					} else {
						LOGGER.info("Invalid password for user {}",user);
					}
				} else {
					LOGGER.warn("Inactive user {} attempted login, password correct = {}",user,passwordValid);
				}
			} else {
				LOGGER.warn("User of {} inactive tenant {} attempted login, password correct = {}",user,user.getTenant().getName(),passwordValid);
			}
		} else {
			LOGGER.info("User not found {}",user);
		}
		return null;
	}
	

	@Override
	public User getUser(Integer id) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		User result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (User) session.get(User.class, id);
			if(result == null) {
				LOGGER.warn("user not found by id = "+id);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}

	@Override
	public User getUser(String username) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		User result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			User user = getUser(session,username); 
			if(user != null) {
				result = user;	
			} else {
				LOGGER.warn("user not found: "+username);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}
	

	@Override
	public List<User> getUsers() {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();
		Session session = managerPersistenceService.getSessionWithTransaction();
		List<User> userList = null;
		try {
			@SuppressWarnings("unchecked")
			List<User> list = session.createCriteria(User.class).list();
			userList = list;
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return userList;
	}

	private Tenant inflate(Tenant tenant) {
		// call with active session
		Tenant result = ManagerPersistenceService.<Tenant>deproxify(tenant);
		return result;
	}

	@Override
	public Tenant getTenant(Integer id) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		Tenant result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (Tenant) session.get(Tenant.class, id);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if(result == null) {
			LOGGER.warn("failed to get tenant {}",id);
		}
		return result;
	}

	@Override
	public Tenant getTenantByUsername(String username) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		Tenant result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			User user = getUser(session,username); 
			if(user != null) {
				result = inflate(user.getTenant());
			}		
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if(result == null) {
			LOGGER.warn("failed to get tenant for username {}",username);
		}
		return result;
	}

	@Override
	public Tenant getTenantByName(String name) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		Tenant result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Tenant.class);
			criteria.add(Restrictions.eq("name", name));
			
			@SuppressWarnings("unchecked")
			List<Tenant> tenantList = criteria.list();
			
			if(tenantList != null && tenantList.size() == 1) {
				result = inflate(tenantList.get(0));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}

	@Override
	public Shard getDefaultShard() {
		return this.getShardByName(TenantManagerService.DEFAULT_SHARD_NAME);
	}
	
	private Shard getShardByName(String name) {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		Shard result = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Shard.class);
			criteria.add(Restrictions.eq("name", name));
			
			@SuppressWarnings("unchecked")
			List<Shard> shardList = criteria.list();
			
			if(shardList == null) {
				LOGGER.error("got null shardList for name {}",name);
			} else if(shardList.isEmpty()) {
				LOGGER.warn("no shard found for name {}",name);
			} else if(shardList.size() != 1) {
				LOGGER.error("got {} shards for name {} - expected 0 or 1",shardList.size(),name);
			} else {
				// ok
				result = ManagerPersistenceService.<Shard>deproxify(shardList.get(0));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}		
		return result;
	}

	@Override
	public Tenant createTenant(String name, String shardName, String dbUsername) {
		// use username as schemaname when creating tenant
		String schemaName = dbUsername;
		
		Tenant result = null;

		Shard shard = this.getShardByName(shardName);
		if(shard == null) {
			LOGGER.error("failed to create tenant because couldn't find shard {}",shardName);
		} else {
			result = shard.createTenant(name,schemaName,dbUsername,this.getDefaultTenant().getPassword());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tenant> getTenants() {
		PersistenceService<ManagerSchema> managerPersistenceService=ManagerPersistenceService.getInstance();

		List<Tenant> results = null;
		
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Tenant.class);
			Collection<Tenant> list = criteria.list();
			results = new ArrayList<Tenant>(list.size());
			for(Tenant tenant : list) {
				results.add(inflate(tenant));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return results;
	}

	@Override
	public Tenant getDefaultTenant() {
		if(this.defaultTenant == null)
			this.defaultTenant = getTenantByName(TenantManagerService.DEFAULT_TENANT_NAME);
		return this.defaultTenant;
	}
	
	private void deleteDefaultOrdersWis() {
		if(defaultTenant != null) {
			try {
				Tenant tenant = defaultTenant;
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
			} catch(SQLException e) {
				LOGGER.error("Caught SQL exception trying to do shutdown database cleanup step", e);
			}
		}
	}
	
	private void deleteDefaultOrdersWisInventory() {
		if(defaultTenant != null) {
			try {
				Tenant tenant = defaultTenant;
				String schemaName = tenant.getSchemaName();
				this.deleteDefaultOrdersWis();
				LOGGER.warn("Deleting itemMasters and gtin maps ");
				tenant.executeSQL("DELETE FROM "+schemaName+".gtin_map");
				tenant.executeSQL("DELETE FROM "+schemaName+".item");
				tenant.executeSQL("DELETE FROM "+schemaName+".item_master");
			} catch (SQLException e) {
				LOGGER.error("Caught SQL exception trying to do shutdown database cleanup step", e);
			}
		}
	}

	private void dropDefaultSchema() {
		if(defaultTenant != null) {
			dropSchema(defaultTenant);
		}
	}

	private void dropSchema(Tenant tenant) {
		try {
			String schemaName = tenant.getSchemaName();
			LOGGER.warn("Deleting tenant schema "+schemaName);
			tenant.executeSQL("DROP SCHEMA "+schemaName+
				((tenant.getSQLSyntax()==DatabaseConnection.SQLSyntax.H2)?"":" CASCADE"));
		} catch(SQLException e) {
			LOGGER.error("Caught SQL exception trying to do database cleanup", e);
		}
	}

	@Override
	protected void startUp() throws Exception {
		initDefaultShard();
	}

	@Override
	protected void shutDown() throws Exception {
		if(!this.shutdownCleanupRequest.equals(TenantManagerService.ShutdownCleanupReq.NONE)) {
			ServiceUtility.awaitTerminatedOrThrow(theInstance);
			switch (shutdownCleanupRequest) {
				case DROP_SCHEMA:
					dropDefaultSchema();
					break;
				case DELETE_ORDERS_WIS:
					deleteDefaultOrdersWis();
					break;
				case DELETE_ORDERS_WIS_INVENTORY:
					deleteDefaultOrdersWisInventory();
					break;
				default:
					break;
			}
		}
	}

	@Override
	public User updateUser(User user) {
		PersistenceService<ManagerSchema> persistence = ManagerPersistenceService.getInstance();
		// assume input is an existing detached User
		Session session = persistence.getSessionWithTransaction();
		User persistentUser = null;
		try {
			User mergeResult = (User) session.merge(user);
			persistence.commitTransaction();
			persistentUser = mergeResult;
		} finally {
			if(persistentUser == null)
				persistence.rollbackTransaction();
		}
		return persistentUser;
	}

	@Override
	public Tenant updateTenant(Tenant tenant) {
		PersistenceService<ManagerSchema> persistence = ManagerPersistenceService.getInstance();
		// assume input is an existing detached Tenant
		Session session = persistence.getSessionWithTransaction();
		Tenant persistentTenant = null;
		try {
			Tenant mergeResult = (Tenant) session.merge(tenant);
			persistence.commitTransaction();
			persistentTenant = mergeResult;
		} finally {
			if(persistentTenant == null)
				persistence.rollbackTransaction();
		}
		return persistentTenant;
	}

	@Override
	public boolean canCreateTenant(String tenantName,String schemaName) {
		List<Tenant> tenants = this.getTenants();
		boolean conflictFound = false;
		
		for(Tenant tenant : tenants) {
			if(tenant.getName().equals(tenantName))
				conflictFound=true;
			else if(tenant.getSchemaName().equals(schemaName))
				conflictFound=true;
		}
		
		return !conflictFound;
	}

	@Override
	public void destroyTenant(Tenant tenant) {
		if(tenant.equals(this.getDefaultTenant())) {
			throw new UnsupportedOperationException("cannot destroy default tenant");
		}
		// delete all users for this tenant, then drop its schema, then delete it
		Session session = ManagerPersistenceService.getInstance().getSessionWithTransaction();
		try {
			LOGGER.warn("DESTROYING tenant {} including users and schema",tenant);
			tenant = (Tenant) session.load(Tenant.class, tenant.getId());
			for(User user : tenant.getUserList()) {
				user = (User) session.load(User.class, user.getId());
				session.delete(user);
			}
			
			Shard shard = (Shard) session.load(Shard.class, tenant.getShard().getId());
			shard.removeTenant(tenant);
			session.delete(tenant);
			session.save(shard);
			
			this.dropSchema(tenant);
			tenant = null;
		} finally {
			if(tenant == null) { // finished ok
				ManagerPersistenceService.getInstance().commitTransaction();
			} else {
				ManagerPersistenceService.getInstance().rollbackTransaction();
			}
		}
		
	}

}
