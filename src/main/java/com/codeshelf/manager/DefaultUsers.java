package com.codeshelf.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.yaml.snakeyaml.Yaml;

import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.security.TokenSessionService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class DefaultUsers {
	final public static String				USER_DEFAULTS_FILENAME	= "user-defaults.yml";
	final public static String				DEFAULT_APPUSER_PASS	= "testme";

	private static Multimap<String, String>	multimap;

	private DefaultUsers() {
	}

	static {
		InputStream is = DefaultUsers.class.getResourceAsStream(USER_DEFAULTS_FILENAME);
		if (is == null) {
			is = DefaultUsers.class.getClassLoader().getResourceAsStream(USER_DEFAULTS_FILENAME);
		}
		String defaultsFileContents;
		try {
			defaultsFileContents = IOUtils.toString(is);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read user defaults resource", e);
		}

		DefaultUsers.multimap = ArrayListMultimap.<String, String> create();
		Yaml yaml = new Yaml();
		Object loaded = yaml.load(defaultsFileContents);
		if (loaded instanceof Map) {
			Map<?, ?> userMap = (Map<?, ?>) loaded;
			Set<?> userSet = (userMap).keySet();
			for (Object user : userSet) {
				Object roleList = userMap.get(user);
				if (roleList instanceof List) {
					List<?> roles = (List<?>) roleList;
					for (Object role : roles) {
						multimap.put(user.toString(), role.toString());
					}
				} else {
					throw new RuntimeException("Invalid format in users defaults");
				}
			}
		} else {
			throw new RuntimeException("Invalid format in users defaults");
		}
	}

	public static void sync(Tenant initTenant, TokenSessionService tokenSessionService) {
		Session session = ManagerPersistenceService.getInstance().getSessionWithTransaction();
		boolean completed = false;
		try {
			for (String username : multimap.keySet()) {
				syncUser(session, username, multimap.get(username), initTenant, tokenSessionService);
			}
			completed = true;
		} finally {
			if (completed)
				ManagerPersistenceService.getInstance().commitTransaction();
			else
				ManagerPersistenceService.getInstance().rollbackTransaction();
		}
	}

	private static void syncUser(Session session,
		String username,
		Collection<String> roleNames,
		Tenant initTenant,
		TokenSessionService tokenSessionService) {
		
		User user = (User) session.bySimpleNaturalId(User.class).load(username);
		if (user == null) {
			user = new User();
			user.setUsername(username);
			if (username.equals(CodeshelfNetwork.DEFAULT_SITECON_USERNAME)) {
				user.setHashedPassword(tokenSessionService.hashPassword(CodeshelfNetwork.DEFAULT_SITECON_PASS));
			} else {
				user.setHashedPassword(tokenSessionService.hashPassword(DEFAULT_APPUSER_PASS));
			}
			initTenant = (Tenant) session.get(Tenant.class, initTenant.getId());
			initTenant.addUser(user);
			session.save(initTenant);
		}
		user.setRoles(lookupRoles(session, roleNames));
		session.saveOrUpdate(user);
	}

	private static Set<UserRole> lookupRoles(Session session, Collection<String> roleNames) {
		Set<UserRole> roles = new HashSet<UserRole>();
		for (String roleName : roleNames) {
			UserRole role = (UserRole) session.bySimpleNaturalId(UserRole.class).load(roleName);
			if (role != null) {
				roles.add(role);
			} else {
				throw new RuntimeException("could not sync a default user, no role named " + roleName);
			}
		}
		return roles;
	}
}
