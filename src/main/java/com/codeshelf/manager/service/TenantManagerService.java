package com.codeshelf.manager.service;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Setter;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.SecurityAnswer;
import com.codeshelf.manager.SecurityQuestion;
import com.codeshelf.manager.Shard;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserRole;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.persistence.AbstractPersistenceService;
import com.codeshelf.persistence.DatabaseCredentials;
import com.codeshelf.persistence.DatabaseUtils;
import com.codeshelf.persistence.DatabaseUtils.SQLSyntax;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ServiceUtility;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class TenantManagerService extends AbstractCodeshelfIdleService implements ITenantManagerService {
	public enum ShutdownCleanupReq {
		NONE,
		DROP_SCHEMA,
		DELETE_ORDERS_WIS,
		DELETE_ORDERS_WIS_INVENTORY
	}

	private static final Logger				LOGGER					= LoggerFactory.getLogger(TenantManagerService.class);
	public static final String				DEFAULT_SHARD_NAME		= "default";
	public static final String				INITIAL_TENANT_NAME		= "default";
	//@Getter
	//int defaultShardId = -1;

	private Tenant							initialTenant			= null;

	@Setter
	ShutdownCleanupReq						shutdownCleanupRequest	= ShutdownCleanupReq.NONE;

	@Inject
	private static ITenantManagerService	theInstance;

	private TokenSessionService				tokenSessionService;
	private AbstractPersistenceService		managerPersistenceService;

	@Inject
	private TenantManagerService(TokenSessionService tokenSessionService) {
		super();
		this.tokenSessionService = tokenSessionService;
	}

	public final synchronized static ITenantManagerService getMaybeRunningInstance() {
		return theInstance;
	}

	public final static ITenantManagerService getNonRunningInstance() {
		if (!getMaybeRunningInstance().state().equals(State.NEW)) {
			throw new RuntimeException("Can't get non-running instance of already-started service: "
					+ theInstance.getClass().getSimpleName());
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
		boolean initDefaultTenant = false;
		Shard shard = null;

		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			Criteria criteria = session.createCriteria(Shard.class);
			criteria.add(Restrictions.eq("name", DEFAULT_SHARD_NAME));

			@SuppressWarnings("unchecked")
			List<Shard> listShard = criteria.list();

			if (listShard.size() == 0) {
				// create
				shard = new Shard();
				shard.setName(DEFAULT_SHARD_NAME);
				shard.setUrl(System.getProperty("shard.default.db.url"));
				shard.setUsername(System.getProperty("shard.default.db.admin_username"));
				shard.setPassword(System.getProperty("shard.default.db.admin_password"));
				session.save(shard);
				initDefaultTenant = true;
			} else if (listShard.size() > 1) {
				LOGGER.error("got more than one default shard, cannot initialize");
			} // else 1 default shard, continue
		} finally {
			if (initDefaultTenant)
				managerPersistenceService.commitTransaction();
			else
				managerPersistenceService.rollbackTransaction();
		}

		if (initDefaultTenant) {
			initDefaultTenant(session, shard);
		}

	}

	private Tenant initDefaultTenant(Session session, Shard shard) {
		Tenant tenant = this.getTenantByName(INITIAL_TENANT_NAME);
		if (tenant == null) {
			String dbSchemaName = System.getProperty("tenant.default.schema");
			String dbUsername = System.getProperty("tenant.default.username");
			String dbPassword = System.getProperty("tenant.default.password");

			tenant = shard.createTenant(INITIAL_TENANT_NAME, dbSchemaName, dbUsername, dbPassword);
		}
		return tenant;
	}

	private User getUser(Session session, String username) {
		User result = (User) session.bySimpleNaturalId(User.class).load(username);
		if (result != null) {
			result.getTenant().getName();
			result = ManagerPersistenceService.<User> deproxify(result);
		}
		return result;
	}

	private boolean isUsernameAvailable(Session session, String username) {
		User user = getUser(session, username);
		return (user == null);
	}

	//////////////////////////// Manager Service API ////////////////////////////////

	@Override
	public boolean canCreateUser(String username) {
		if (!tokenSessionService.usernameMeetsRequirements(username))
			return false;

		boolean result = false;
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = isUsernameAvailable(session, username);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public User createUser(Tenant tenant, String username, String password, Set<UserRole> roles) {
		if (!tokenSessionService.usernameMeetsRequirements(username))
			throw new IllegalArgumentException("tried to create user with invalid username (caller must prevalidate)");
		if (password != null && !tokenSessionService.passwordMeetsRequirements(password))
			throw new IllegalArgumentException("tried to create user with invalid password (caller must prevalidate)");

		User result = null;
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			if (!isUsernameAvailable(session, username)) {
				throw new IllegalArgumentException("Tried to create duplicate username " + username + " (caller must prevalidate)");
			}

			tenant = (Tenant) session.load(Tenant.class, tenant.getId());

			User user = new User();
			user.setUsername(username);
			if(password != null) 
				user.setHashedPassword(tokenSessionService.hashPassword(password));
			if (roles != null)
				user.setRoles(roles);
			tenant.addUser(user);
			session.save(tenant);
			session.save(user);
			result = user;
			LOGGER.info("Created user {} for tenant {}", username, tenant.getName());
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public void resetTenant(Tenant tenant) {
		LOGGER.info("Resetting schema and users");
		eraseAllTenantData(tenant);

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();

			// reload detached tenant (might've added more users)
			tenant = (Tenant) session.load(Tenant.class, tenant.getId());

			// remove all users except default site controller
			List<User> users = new ArrayList<User>();
			users.addAll(tenant.getUsers());
			for (User u : users) {
				u = (User) session.load(User.class, u.getId());
				if (!u.isSiteController() || !u.getUsername().equals(CodeshelfNetwork.DEFAULT_SITECON_USERNAME)) {
					tenant.removeUser(u);
					session.delete(u);
				}
			}
			session.save(tenant);

		} finally {
			managerPersistenceService.commitTransaction();
		}

		// resetTenant for testing - not necessary to recreate default users here
		//createDefaultUsers(tenant);
	}

	private void eraseAllTenantData(Tenant tenant) {
		if (DatabaseUtils.getSQLSyntax(tenant).equals(SQLSyntax.H2_MEMORY)) {
			String sql = "SET REFERENTIAL_INTEGRITY FALSE;";
			for (String tableName : getTableNames(tenant)) {
				sql += "TRUNCATE TABLE " + tenant.getSchemaName() + "." + tableName + ";";
			}
			sql += "SET REFERENTIAL_INTEGRITY TRUE";
			try {
				DatabaseUtils.executeSQL(tenant, sql);
			} catch (SQLException e) {
				LOGGER.error("Truncate of tenant tables failed, falling back on SchemaExport", e);
				// reset schema old way, hbm2ddl
				DatabaseUtils.Hbm2DdlSchemaExport(TenantPersistenceService.getInstance().getHibernateConfiguration(), tenant);
			}
		}
	}

	public Set<String> getTableNames(Tenant tenant) {
		Set<String> tableNames = new HashSet<String>();
		Iterator<org.hibernate.mapping.Table> tables = TenantPersistenceService.getInstance()
			.getHibernateConfiguration()
			.getTableMappings();
		while (tables.hasNext()) {
			org.hibernate.mapping.Table table = tables.next();
			tableNames.add(table.getName());
		}
		return tableNames;
	}

	@Override
	public User getUser(Integer id) {
		User result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (User) session.get(User.class, id);
			if (result == null) {
				LOGGER.warn("user not found by id = " + id);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public User getUser(String username) {
		User result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			User user = getUser(session, username);
			if (user != null) {
				result = user;

				if (!tokenSessionService.hashIsValid(user.getHashedPassword())) {
					boolean update = false;
					if (user.getUsername().endsWith("@example.com")) {
						user.setHashedPassword(tokenSessionService.hashPassword(DefaultUsers.DEFAULT_APPUSER_PASS));
						update = true;
					} else if (user.getUsername().equals(CodeshelfNetwork.DEFAULT_SITECON_USERNAME)) {
						user.setHashedPassword(tokenSessionService.hashPassword(CodeshelfNetwork.DEFAULT_SITECON_PASS));
						update = true;
					}
					if (update) {
						LOGGER.warn("Automatic default account password reset (switch hash to apr1");
						session.save(user);
					}
				}

			} else {
				LOGGER.debug("user not found: {}", username);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public List<User> getUsers(Tenant tenant) {
		Session session = managerPersistenceService.getSessionWithTransaction();
		List<User> userList = null;
		try {
			if (tenant == null) {
				Criteria criteria = session.createCriteria(User.class);
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				@SuppressWarnings("unchecked")
				List<User> list = (List<User>) criteria.list();
				userList = list;
			} else {
				Tenant loaded = (Tenant) session.load(Tenant.class, tenant.getId());
				userList = new ArrayList<User>(loaded.getUsers().size());
				// get deep list of non-proxy objects
				for (User user : loaded.getUsers()) {
					User realUser = ManagerPersistenceService.<User> deproxify(user);
					realUser.setTenant(ManagerPersistenceService.<Tenant> deproxify(realUser.getTenant()));
					userList.add(realUser);
				}
			}
		} finally {
			managerPersistenceService.rollbackTransaction();
		}
		return userList;
	}
	
	@Override
	public List<User> getSiteControllerUsers(boolean onlyIfUpgradeNeeded) {
		List<User> allUsers = getUsers(null);
		List<User> sitecons = new ArrayList<User>();
		for(User user : allUsers) {
			if(user.isSiteController() &&
					(!onlyIfUpgradeNeeded || !user.getTenant().clientVersionIsCompatible(user.getClientVersion())) ) {
				sitecons.add(user);
			}
		}
		return sitecons;
	}

	@Override
	public byte[] getHtpasswd() {
		String result = "";
		for (User user : this.getUsers(null)) {
			result += user.getHtpasswdEntry() + "\n";
		}
		return result.getBytes(Charset.forName("ISO-8859-1"));

	}

	private Tenant inflate(Tenant tenant) {
		// call with active session
		Tenant result = ManagerPersistenceService.<Tenant> deproxify(tenant);
		return result;
	}

	@Override
	public Tenant getTenant(Integer id) {
		Tenant result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (Tenant) session.get(Tenant.class, id);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if (result == null) {
			LOGGER.warn("failed to get tenant {}", id);
		}
		return result;
	}

	@Override
	public Tenant getTenantByUser(User user) {
		Tenant result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			user = (User) session.load(User.class, user.getId());
			if (user != null) {
				result = inflate(user.getTenant());
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if (result == null) {
			LOGGER.warn("failed to get tenant for user {}", user.getUsername());
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

			if (tenantList != null && tenantList.size() == 1) {
				result = inflate(tenantList.get(0));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public Tenant getTenantBySchemaName(String schemaName) {
		Session session = managerPersistenceService.getSessionWithTransaction();
		Tenant result = null;
		try {
			result = (Tenant) session.bySimpleNaturalId(Tenant.class).load(schemaName);
			if (result != null) {
				result = inflate(result);
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
		Shard result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Shard.class);
			criteria.add(Restrictions.eq("name", name));

			@SuppressWarnings("unchecked")
			List<Shard> shardList = criteria.list();

			if (shardList == null) {
				LOGGER.error("got null shardList for name {}", name);
			} else if (shardList.isEmpty()) {
				LOGGER.warn("no shard found for name {}", name);
			} else if (shardList.size() != 1) {
				LOGGER.error("got {} shards for name {} - expected 0 or 1", shardList.size(), name);
			} else {
				// ok
				result = ManagerPersistenceService.<Shard> deproxify(shardList.get(0));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public Tenant createTenant(String name, String schemaName) {
		return createTenant(name, schemaName, this.getDefaultShard().getName());
	}

	@Override
	public Tenant createTenant(String name, String schemaName, String shardName) {
		// use schemaname as username when creating tenant
		String dbUsername = schemaName;

		Tenant result = null;

		Shard shard = this.getShardByName(shardName);
		if (shard == null) {
			LOGGER.error("failed to create tenant because couldn't find shard {}", shardName);
		} else {
			result = shard.createTenant(name, schemaName, dbUsername, UUID.randomUUID().toString());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tenant> getTenants() {
		List<Tenant> results = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(Tenant.class);
			Collection<Tenant> list = criteria.list();
			results = new ArrayList<Tenant>(list.size());
			for (Tenant tenant : list) {
				results.add(inflate(tenant));
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return results;
	}

	@Override
	public Tenant getInitialTenant() {
		if (this.initialTenant == null)
			this.initialTenant = getTenantByName(TenantManagerService.INITIAL_TENANT_NAME);
		return this.initialTenant;
	}

	private void deleteDefaultOrdersWis() {
		if (initialTenant != null) {
			try {
				Tenant tenant = initialTenant;
				String schemaName = tenant.getSchemaName();
				LOGGER.warn("Deleting all orders and work instructions from schema " + schemaName);
				DatabaseUtils.executeSQL(tenant, "UPDATE " + schemaName + ".order_header SET container_use_persistentid=null");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".container_use");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".work_instruction");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".container");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".order_location");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".order_detail");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".order_header");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".order_group");
			} catch (SQLException e) {
				LOGGER.error("Caught SQL exception trying to do shutdown database cleanup step", e);
			}
		}
	}

	private void deleteDefaultOrdersWisInventory() {
		if (initialTenant != null) {
			try {
				Tenant tenant = initialTenant;
				String schemaName = tenant.getSchemaName();
				this.deleteDefaultOrdersWis();
				LOGGER.warn("Deleting itemMasters and gtin maps ");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".gtin");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".item");
				DatabaseUtils.executeSQL(tenant, "DELETE FROM " + schemaName + ".item_master");
			} catch (SQLException e) {
				LOGGER.error("Caught SQL exception trying to do shutdown database cleanup step", e);
			}
		}
	}

	private void dropDefaultSchema() {
		// this is only supposed to be a dev/test/demo feature, not used in tests
		if (DatabaseUtils.getSQLSyntax(initialTenant).equals(SQLSyntax.H2_MEMORY))
			throw new RuntimeException("dropDefaultSchema called during test");

		if (initialTenant != null) {
			dropSchema(initialTenant.getSchemaName(), initialTenant);
		}
	}

	private void dropSchema(String schemaName, DatabaseCredentials cred) {
		LOGGER.debug("Deleting tenant schema " + schemaName);
		try {
			DatabaseUtils.executeSQL(cred, "DROP SCHEMA " + schemaName
					+ ((DatabaseUtils.getSQLSyntax(cred) == DatabaseUtils.SQLSyntax.H2_MEMORY) ? "" : " CASCADE"));

			// in case schema is recreated later, forget that we initialized it earlier
			TenantPersistenceService.getInstance().forgetInitialActions(schemaName);
			TenantPersistenceService.getInstance().forgetSchemaInitialization(schemaName);
			TenantPersistenceService.getInstance().forgetConnectionProvider(schemaName);
		} catch (SQLException e) {
			LOGGER.error("Caught SQL exception trying to remove schema", e);
		}
	}

	private void dropSchemaUser(String username, DatabaseCredentials superuser) {
		LOGGER.debug("Deleting role " + username);
		try {
			if (DatabaseUtils.getSQLSyntax(superuser) == DatabaseUtils.SQLSyntax.POSTGRES) {
				DatabaseUtils.executeSQL(superuser, "DROP OWNED BY " + username);
				DatabaseUtils.executeSQL(superuser, "DROP ROLE " + username);
			} else if (DatabaseUtils.getSQLSyntax(superuser) == DatabaseUtils.SQLSyntax.H2_MEMORY) {
				DatabaseUtils.executeSQL(superuser, "DROP USER " + username);
			}
		} catch (SQLException e) {
			LOGGER.error("Caught SQL exception trying to remove schema", e);
		}
	}

	@Override
	protected void startUp() throws Exception {
		this.managerPersistenceService = ManagerPersistenceService.getInstance();
		initDefaultShard();
		DefaultUsers.sync(this.getInitialTenant(), this.tokenSessionService);
	}

	@Override
	protected void shutDown() throws Exception {
		if (!this.shutdownCleanupRequest.equals(ShutdownCleanupReq.NONE)) {
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
		// assume input is an existing detached User
		Session session = managerPersistenceService.getSessionWithTransaction();
		User persistentUser = null;
		try {
			User mergeResult = (User) session.merge(user);
			managerPersistenceService.commitTransaction();
			persistentUser = mergeResult;
		} finally {
			if (persistentUser == null)
				managerPersistenceService.rollbackTransaction();
		}
		return persistentUser;
	}

	@Override
	public Tenant updateTenant(Tenant tenant) {
		// assume input is an existing detached Tenant
		Session session = managerPersistenceService.getSessionWithTransaction();
		Tenant persistentTenant = null;
		try {
			Tenant mergeResult = (Tenant) session.merge(tenant);
			managerPersistenceService.commitTransaction();
			persistentTenant = mergeResult;
		} finally {
			if (persistentTenant == null)
				managerPersistenceService.rollbackTransaction();
		}
		return persistentTenant;
	}

	@Override
	public boolean canCreateTenant(String tenantName, String schemaName) {
		List<Tenant> tenants = this.getTenants();
		boolean conflictFound = false;

		for (Tenant tenant : tenants) {
			if (tenant.getName().equals(tenantName))
				conflictFound = true;
			else if (tenant.getSchemaName().equals(schemaName))
				conflictFound = true;
		}

		return !conflictFound;
	}

	@Override
	public void deleteTenant(Tenant tenant) {
		if (tenant.equals(this.getInitialTenant())) {
			throw new UnsupportedOperationException("cannot destroy default tenant");
		}
		// delete all users for this tenant, then delete it, then drop its schema
		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			LOGGER.info("DELETING tenant {} including users and schema and schema-user", tenant.getSchemaName());
			tenant = (Tenant) session.load(Tenant.class, tenant.getId());
			for (User user : tenant.getUsers()) {
				user = (User) session.load(User.class, user.getId());
				if (!user.getRoles().isEmpty()) {
					user.setRoles(Sets.<UserRole> newHashSet());
				}
				session.save(user);
				session.delete(user);
			}

			Shard shard = (Shard) session.load(Shard.class, tenant.getShard().getId());
			shard.removeTenant(tenant);
			session.delete(tenant);
			session.save(shard);

			this.dropSchema(tenant.getSchemaName(), shard);
			this.dropSchemaUser(tenant.getUsername(), shard);
			tenant = null;
		} catch (Exception e) {
			LOGGER.error("unexpected exception deleting tenant", e);
		} finally {
			if (tenant == null) { // finished ok
				ManagerPersistenceService.getInstance().commitTransaction();
			} else {
				ManagerPersistenceService.getInstance().rollbackTransaction();
			}
		}

	}

	@Override
	public List<UserRole> getRoles(boolean includeRestrictedRoles) {
		List<UserRole> results = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(UserRole.class);
			if(!includeRestrictedRoles) {
				criteria.add(Restrictions.eq("restricted", false));
			}
			criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			@SuppressWarnings("unchecked")
			Collection<UserRole> list = criteria.list();
			results = new ArrayList<UserRole>(list.size());
			for (UserRole role : list) {
				results.add(role);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return results;
	}

	@Override
	public UserRole getRole(Integer id) {
		UserRole result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (UserRole) session.get(UserRole.class, id);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if (result == null) {
			LOGGER.warn("failed to get UserRole {}", id);
		}
		return result;
	}

	@Override
	public UserRole getRoleByName(String name) {
		UserRole result = null;
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (UserRole) session.bySimpleNaturalId(UserRole.class).load(name);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}
	
	@Override
	public Set<UserRole> getUserRoles(String listOfRoles, boolean allowRestrictedRoles) {
		Set<UserRole> result = new HashSet<UserRole>();
		if(listOfRoles == null || listOfRoles.isEmpty())
			return result;
		
		String[] roleNames = listOfRoles.split(UserRole.TOKEN_SEPARATOR);
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			for(int i=0;i<roleNames.length;i++) {
				UserRole role = (UserRole) session.bySimpleNaturalId(UserRole.class).load(roleNames[i]);
				if(role != null && (allowRestrictedRoles || (!role.isRestricted() ))  ) {
					result.add(role);
				} else {
					LOGGER.warn("invalid role name in list: {}",roleNames[i]);
					return null;
				}
			}			
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}



	@Override
	public UserRole createRole(String name) {
		if (this.getRoleByName(name) != null) {
			LOGGER.warn("tried to create duplicate role name {}", name);
			return null;
		}

		UserRole result = null;
		UserRole newRole = new UserRole();
		newRole.setName(name);
		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			session.save(newRole);
			LOGGER.info("created role {}", newRole.getName());
			result = newRole;
		} finally {
			if (result == null)
				managerPersistenceService.rollbackTransaction();
			else
				managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public UserRole updateRole(UserRole role) {
		// assume input is an existing detached role
		Session session = managerPersistenceService.getSessionWithTransaction();
		UserRole persistentRole = null;
		try {
			UserRole mergeResult = (UserRole) session.merge(role);
			managerPersistenceService.commitTransaction();
			LOGGER.info("saved changes to role {} {}", role.getId(), role.getName());
			persistentRole = mergeResult;
		} finally {
			if (persistentRole == null)
				managerPersistenceService.rollbackTransaction();
		}
		return persistentRole;
	}

	@Override
	public void deleteRole(UserRole role) {
		// remove existing permissions from role 1st
		role.setPermissions(Sets.<UserPermission> newHashSet());
		role = this.updateRole(role);

		// assume input is an existing detached role
		Session session = managerPersistenceService.getSessionWithTransaction();
		boolean deleted = false;

		try {
			UserRole loadResult = (UserRole) session.load(UserRole.class, role.getId());
			session.delete(loadResult);
			managerPersistenceService.commitTransaction();
			LOGGER.info("deleted role {} {}", role.getId(), role.getName());
			deleted = true;
		} finally {
			if (!deleted)
				managerPersistenceService.rollbackTransaction();
		}
	}

	@Override
	public List<UserPermission> getPermissions() {
		List<UserPermission> results = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			Criteria criteria = session.createCriteria(UserPermission.class);
			@SuppressWarnings("unchecked")
			Collection<UserPermission> list = criteria.list();
			results = new ArrayList<UserPermission>(list.size());
			for (UserPermission permission : list) {
				results.add(permission);
			}
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return results;
	}

	@Override
	public UserPermission getPermission(Integer id) {
		UserPermission result = null;

		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (UserPermission) session.get(UserPermission.class, id);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		if (result == null) {
			LOGGER.warn("failed to get UserRole {}", id);
		}
		return result;
	}

	@Override
	public UserPermission getPermissionByDescriptor(String descriptor) {
		UserPermission result = null;
		try {
			Session session = managerPersistenceService.getSessionWithTransaction();
			result = (UserPermission) session.bySimpleNaturalId(UserPermission.class).load(descriptor);
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public UserPermission createPermission(String descriptor) {
		if (this.getPermissionByDescriptor(descriptor) != null) {
			LOGGER.warn("tried to create duplicate permission descriptor {}", descriptor);
			return null;
		}

		UserPermission result = null;
		UserPermission newPermission = new UserPermission();
		newPermission.setDescriptor(descriptor);
		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			session.save(newPermission);
			LOGGER.info("created permission {}", newPermission.getDescriptor());
			result = newPermission;
		} finally {
			if (result == null)
				managerPersistenceService.rollbackTransaction();
			else
				managerPersistenceService.commitTransaction();
		}
		return result;
	}

	@Override
	public UserPermission updatePermission(UserPermission permission) {
		// assume input is an existing detached permission
		Session session = managerPersistenceService.getSessionWithTransaction();
		UserPermission persistentRole = null;
		try {
			UserPermission mergeResult = (UserPermission) session.merge(permission);
			managerPersistenceService.commitTransaction();
			LOGGER.info("saved changes to permission {} {}", permission.getId(), permission.getDescriptor());
			persistentRole = mergeResult;
		} finally {
			if (persistentRole == null)
				managerPersistenceService.rollbackTransaction();
		}
		return persistentRole;
	}

	@Override
	public void deletePermission(UserPermission permission) {
		// assume input is an existing detached permission
		Session session = managerPersistenceService.getSessionWithTransaction();
		boolean deleted = false;
		try {
			UserPermission loadResult = (UserPermission) session.load(UserPermission.class, permission.getId());
			session.delete(loadResult);
			managerPersistenceService.commitTransaction();
			LOGGER.info("deleted permission {} {}", permission.getId(), permission.getDescriptor());
			deleted = true;
		} finally {
			if (!deleted)
				managerPersistenceService.rollbackTransaction();
		}
	}

	@Override
	public Map<String,String> getActiveSecurityQuestions() {
		List<SecurityQuestion> resultList = listSecurityQuestions(true);
		Map<String,String> result = new HashMap<String,String>();
		for(SecurityQuestion question : resultList) {
			result.put(question.getCode(),question.getQuestion());
		}
		return result;
	}

	@Override
	public Map<String,SecurityQuestion> getAllSecurityQuestions() {
		List<SecurityQuestion> resultList = listSecurityQuestions(true);
		Map<String,SecurityQuestion> result = new HashMap<String,SecurityQuestion>();
		for(SecurityQuestion question : resultList) {
			result.put(question.getCode(),question);
		}
		return result;
	}

	private List<SecurityQuestion> listSecurityQuestions(boolean activeOnly) {
		List<SecurityQuestion> resultList = null;
		Session session = managerPersistenceService.getSessionWithTransaction();
		try {
			Criteria criteria = session.createCriteria(SecurityQuestion.class);
			if(activeOnly) {
				criteria.add(Restrictions.eq("active", true));
			}
			@SuppressWarnings("unchecked")
			List<SecurityQuestion> list = criteria.list();
			resultList = list;
		} finally {
			managerPersistenceService.commitTransaction();
		}
		return resultList;
	}

	@Override
	public User setSecurityAnswers(User user, Map<SecurityQuestion,String> questionAndAnswer) {		
		TokenSessionService sessionService = TokenSessionService.getInstance();
		if(questionAndAnswer == null || questionAndAnswer.size() < sessionService.getSecurityAnswerMinCount()) {
			throw new IllegalArgumentException("no valid question/answer map provided of at least size "+sessionService.getSecurityAnswerMinCount());
		}

		// first, delete old security answers
		Session session = managerPersistenceService.getSessionWithTransaction();
		boolean success = false;
		int deleted = 0;
		try {
			user = (User) session.load(User.class, user.getId());
			for(SecurityAnswer currentAnswer : user.getSecurityAnswers().values()) {
				// delete old answers
				session.delete(currentAnswer);
				deleted++;
			} 
			user.setSecurityAnswers(new HashMap<SecurityQuestion,SecurityAnswer>());
			session.saveOrUpdate(user);
			success = true;
		} finally {
			if(success) {
				LOGGER.debug("User {} deleted {} old security questions",user.getUsername(),deleted);
				managerPersistenceService.commitTransaction();
			} else {
				LOGGER.error("User {} failed to delete {} old security questions",user.getUsername(),deleted);
				managerPersistenceService.rollbackTransaction();			
			}
		}
		
		// now create answers 
		if(success) {
			session = managerPersistenceService.getSessionWithTransaction();
			success = false;
			try {
				user = (User) session.load(User.class, user.getId());
				Map<SecurityQuestion, SecurityAnswer> newMap = new HashMap<SecurityQuestion, SecurityAnswer>();
				for(SecurityQuestion question : questionAndAnswer.keySet()) {
					String newAnswerString = questionAndAnswer.get(question);
					if(question != null && sessionService.securityAnswerMeetsRequirements(newAnswerString)) {
						String newHashedAnswer = sessionService.hashSecurityAnswer(newAnswerString);
						SecurityAnswer answer = new SecurityAnswer();
						answer.setUser(user);
						answer.setQuestion(question);
						answer.setHashedAnswer(newHashedAnswer);
						session.save(answer);
						newMap.put(question, answer);
					} else {
						throw new IllegalArgumentException("Invalid question code provided, cannot create security question map");
					}
				}
				user.setSecurityAnswers(newMap);
				session.saveOrUpdate(user);
				success=true;
			} finally {
				if(success) {
					LOGGER.info("User {} set {} security questions",user.getUsername(),questionAndAnswer.size());
					managerPersistenceService.commitTransaction();
				} else {
					LOGGER.error("User {} failure to set security questions",user.getUsername());
					managerPersistenceService.rollbackTransaction();			
				}
			}
		}
		return success?user:null;
	}

}
