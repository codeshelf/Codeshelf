package com.codeshelf.manager.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.yaml.snakeyaml.Yaml;

import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserRole;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class DefaultRolesPermissions {
	final public static String				ROLE_DEFAULTS_FILENAME	= "role-defaults.yml";
	
	final public static String 				SITE_CONTROLLER_ROLE = "SiteController";
	final public static String				RESTRICTED_ROLE_NAME_PREFIX = "Cs";

	private static Multimap<String, String>	multimap;

	private DefaultRolesPermissions() {
	}

	static {
		InputStream is = DefaultRolesPermissions.class.getResourceAsStream(ROLE_DEFAULTS_FILENAME);
		if (is == null) {
			is = DefaultRolesPermissions.class.getClassLoader().getResourceAsStream(ROLE_DEFAULTS_FILENAME);
		}
		String defaultsFileContents;
		try {
			defaultsFileContents = IOUtils.toString(is);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read property defaults resource", e);
		}

		DefaultRolesPermissions.multimap = ArrayListMultimap.<String, String> create();
		Yaml yaml = new Yaml();
		Object loaded = yaml.load(defaultsFileContents);
		boolean siteControllerRoleExists = false;
		if (loaded instanceof Map) {
			Map<?, ?> roleMap = (Map<?, ?>) loaded;
			Set<?> roleSet = (roleMap).keySet();
			for (Object role : roleSet) {
				Object permList = roleMap.get(role);
				if (permList instanceof List) {
					List<?> permissions = (List<?>) permList;
					for (Object permission : permissions) {
						multimap.put(role.toString(), permission.toString());
						if(role.toString().equals(SITE_CONTROLLER_ROLE)) {
							siteControllerRoleExists = true;
						}
					}
				} else {
					throw new RuntimeException("Invalid format in roles defaults");
				}
			}
		} else {
			throw new RuntimeException("Invalid format in roles defaults");
		}
		if(!siteControllerRoleExists) {
			throw new RuntimeException("Invalid default roles: Did not find a role called "+SITE_CONTROLLER_ROLE);
		}
	}

	public static void sync() {
		Session session = ManagerPersistenceService.getInstance().getSessionWithTransaction();
		boolean completed = false;
		try {
			for (String roleName : multimap.keySet()) {
				syncRole(session, roleName, multimap.get(roleName));
			}
			completed = true;
		} finally {
			if (completed)
				ManagerPersistenceService.getInstance().commitTransaction();
			else
				ManagerPersistenceService.getInstance().rollbackTransaction();
		}
	}

	private static void syncRole(Session session, String roleName, Collection<String> permissionDescriptors) {
		UserRole role = (UserRole) session.bySimpleNaturalId(UserRole.class).load(roleName);
		Serializable roleId;
		if (role == null) {
			role = new UserRole();
			role.setName(roleName);
			roleId = session.save(role);
		} else {
			roleId = role.getId();
		}

		Set<UserPermission> permissions = new HashSet<UserPermission>();

		// ensure each permission exists
		for (String permissionDescriptor : permissionDescriptors) {
			permissions.add(syncPermission(session, permissionDescriptor));
		}

		role = (UserRole) session.load(UserRole.class, roleId);
		role.setPermissions(permissions);
		role.setRestricted(roleName.equals(SITE_CONTROLLER_ROLE) || roleName.startsWith(RESTRICTED_ROLE_NAME_PREFIX));
		session.saveOrUpdate(role);
	}

	private static UserPermission syncPermission(Session session, String permissionDescriptor) {
		UserPermission permission = lookupPermission(session, permissionDescriptor);
		if (permission == null) {
			permission = new UserPermission();
			permission.setDescriptor(permissionDescriptor);
			session.saveOrUpdate(permission);
			permission = lookupPermission(session, permissionDescriptor);
		}
		return permission;
	}

	private static UserPermission lookupPermission(Session session, String permissionDescriptor) {
		return (UserPermission) session.bySimpleNaturalId(UserPermission.class).load(permissionDescriptor);
	}

	public static boolean isDefaultRole(String name) {
		return multimap.containsKey(name);
	}

	public static boolean isDefaultPermission(String descriptor) {
		return multimap.containsValue(descriptor);
	}
}
