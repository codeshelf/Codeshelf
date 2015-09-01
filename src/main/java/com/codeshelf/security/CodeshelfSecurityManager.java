package com.codeshelf.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Getter;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.mgt.AuthorizingSecurityManager;
import org.apache.shiro.mgt.DefaultSubjectFactory;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Arrays;

public class CodeshelfSecurityManager extends AuthorizingSecurityManager implements WebSecurityManager {
	static final private Logger	LOGGER						= LoggerFactory.getLogger(CodeshelfSecurityManager.class);

	@Getter
	private static UserContext userContextSYSTEM; 
	static {
		userContextSYSTEM = new UserContext() {
			@Override
			public String getUsername() {
				return "SYSTEM";
			}
			@Override
			public Integer getId() {
				return -1;
			}
			@Override
			public boolean isSiteController() {
				return false;
			}
			@Override
			public Collection<? extends String> getRoleNames() {
				return Collections.<String>emptySet();
			}
			@Override
			public Set<String> getPermissionStrings() {
				return Sets.<String>newHashSet("*");
			}};		
	}
	
	Realm oneRealm;
    protected SubjectFactory subjectFactory;
	public static final String	THREAD_CONTEXT_USER_KEY	= "user";
	public static final String	THREAD_CONTEXT_TENANT_KEY	= "tenant";

	@Inject
	public CodeshelfSecurityManager(Realm realm) {
		super();

		this.oneRealm = realm;
		Collection<Realm> realms = new ArrayList<Realm>(1);
		realms.add(realm);
		this.subjectFactory = new DefaultSubjectFactory();
		ModularRealmAuthorizer authorizer = (ModularRealmAuthorizer) this.getAuthorizer();
		authorizer.setRealms(realms);
	}
	
	@Override
	public Subject login(Subject subject, AuthenticationToken authenticationToken) throws AuthenticationException {
		return null;
	}

	@Override
	public void logout(Subject subject) {
	}

	@Override
	public Subject createSubject(SubjectContext context) {
		Subject subject = null;
		UserContext user = getCurrentUserContext();
		if(user != null) {
			PrincipalCollection principals = new SimplePrincipalCollection(user,oneRealm.getName());
			context.setSecurityManager(this);
			context.setAuthenticated(true);
			context.setPrincipals(principals);
			context.setSessionCreationEnabled(false);
			subject = this.subjectFactory.createSubject(context);
			LOGGER.debug("created subject {} [{}]",user.getUsername(),user.getId());
		} else {
			LOGGER.debug("failed to create subject, no current user");
		}
		return subject;
	}

	@Override
	public Session start(SessionContext context) {
		return null;
	}

	@Override
	public Session getSession(SessionKey key) throws SessionException {
		return null;
	}

	public static UserContext getCurrentUserContext() {
		UserContext user = null;
		Object uObj = ThreadContext.get(THREAD_CONTEXT_USER_KEY);
		if(uObj != null) {
			if(uObj instanceof UserContext) {
				user = (UserContext) uObj;
			} else {
				LOGGER.error("Value {} on ThreadContext was not a UserContext but a {}",THREAD_CONTEXT_USER_KEY,
					uObj.getClass().getCanonicalName());
			}
		}
		return user;
	}

	public static Tenant getCurrentTenant() {
		Tenant tenant = null;
		Object uObj = ThreadContext.get(THREAD_CONTEXT_TENANT_KEY);
		if(uObj != null) {
			if(uObj instanceof Tenant) {
				tenant = (Tenant) uObj;
			} else {
				LOGGER.error("Value {} on ThreadContext was not a Tenant but a {}",THREAD_CONTEXT_TENANT_KEY,
					uObj.getClass().getCanonicalName());
			}
		}
		return tenant;
	}

	public static void setContext(UserContext user, Tenant tenant) {
		if(user == null)
			throw new NullPointerException("Attempt to set current user context to null");
		if(tenant == null)
			throw new NullPointerException("Attempt to set current tenant to null");
		
		// TODO: require that user not be null - use "system" user
		
		// Shiro will call the SecurityManager to get the current subject, which will determine it from this key
		// Also used for context logging.
		UserContext oldUser=getCurrentUserContext();
		if(oldUser != null && !oldUser.equals(user)) {
			LOGGER.error("setContext {} called but there was already a current user {}",user,oldUser);
		}
		Tenant oldTenant = getCurrentTenant();
		if(oldTenant != null && !oldTenant.equals(tenant)) {
			LOGGER.error("setContext {} called but there was already a current tenant {}",tenant,oldTenant.getId());
		}
		ThreadContext.put(THREAD_CONTEXT_USER_KEY,user);
		ThreadContext.put(THREAD_CONTEXT_TENANT_KEY,tenant);
		if(user != null) {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_USER_KEY,user.getUsername());
		}
	}

	public static void removeContext() {
		// Remove both the User and Shiro Subject from ThreadContext.
		// It is an error if there is no UserContext or Tenant 
		
		if(getCurrentTenant() == null) {
			LOGGER.error("removeContext called but no current tenant existed");
		}
		if(getCurrentUserContext() == null) {
			LOGGER.error("removeContext called but no user context existed");
		}
		removeContextIfPresent();
	}

	public static void removeContextIfPresent() {
		// Remove both the User and Shiro Subject from ThreadContext if present.
		ThreadContext.remove(ThreadContext.SUBJECT_KEY); // TODO: merge User and Subject somehow
		ThreadContext.remove(THREAD_CONTEXT_USER_KEY);
		ThreadContext.remove(THREAD_CONTEXT_TENANT_KEY);
		org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_USER_KEY);
	}

	public static boolean authorizeAnnotatedClass(Class<?> clazz) throws AuthorizationException {
		boolean annotated = false;
        Subject subject = SecurityUtils.getSubject();

        // authentication checks
        if(clazz.isAnnotationPresent(RequiresAuthentication.class)) {
        	annotated = true;
            if ((subject == null || !subject.isAuthenticated())) {
                throw new UnauthenticatedException("Authentication required");
            }
        }
        if(clazz.isAnnotationPresent(RequiresGuest.class)) {
        	annotated = true;
            if ((subject != null && subject.getPrincipal() != null)) {
                throw new UnauthenticatedException("Guest required");
            }
        }
        if(clazz.isAnnotationPresent(RequiresUser.class)) {
        	annotated = true;
            if ((subject == null || subject.getPrincipal() == null)) {
                throw new UnauthenticatedException("User required");
            }
        }

        // role checks
        RequiresRoles roles = clazz.getAnnotation(RequiresRoles.class);
        if (roles != null) {
        	annotated = true;
        	if(subject == null) {
        		throw new AuthorizationException("Roles required, but no subject available");
        	}
            @SuppressWarnings("unchecked")
			List<String> roleList = Arrays.asList(roles.value());
			subject.checkRoles(roleList);
        }

        // permission checks
        RequiresPermissions permissions = clazz.getAnnotation(RequiresPermissions.class);
        if (permissions != null) {
        	annotated = true;
        	if(subject == null) {
        		throw new AuthorizationException("Permissions required, but no subject available");
        	}
            subject.checkPermissions(permissions.value());
        }

        // passed all checks
        return annotated;
	}

	@Override
	public boolean isHttpSessionMode() {
		return false;
	}

}
