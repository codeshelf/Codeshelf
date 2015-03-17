package com.codeshelf.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.google.inject.Inject;

import edu.emory.mathcs.backport.java.util.Arrays;

public class CodeshelfSecurityManager extends AuthorizingSecurityManager {
	static final private Logger	LOGGER						= LoggerFactory.getLogger(CodeshelfSecurityManager.class);

	Realm oneRealm;
    protected SubjectFactory subjectFactory;
	public static final String	THREAD_CONTEXT_USER_KEY	= "user";

	@Inject
	CodeshelfSecurityManager(Realm realm) {
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
		User user = getCurrentUser();
		if(user != null) {
			PrincipalCollection principals = new SimplePrincipalCollection(user.getId(),oneRealm.getName());
			context.setSecurityManager(this);
			context.setAuthenticated(true);
			context.setPrincipals(principals);
			context.setSessionCreationEnabled(false);
			subject = this.subjectFactory.createSubject(context);
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

	public static User getCurrentUser() {
		User user = null;
		Object uObj = ThreadContext.get(THREAD_CONTEXT_USER_KEY);
		if(uObj != null) {
			if(uObj instanceof User) {
				user = (User) uObj;
			} else {
				LOGGER.error("Value {} on ThreadContext was not a User but a {}",THREAD_CONTEXT_USER_KEY,
					uObj.getClass().getCanonicalName());
			}
		}
		return user;
	}

	public static void setCurrentUser(User user) {
		User oldUser=getCurrentUser();
		if(oldUser != null) {
			LOGGER.error("setCurrentUser {} called but there was already a current user {}",oldUser.getId(),user.getId());
		}
		ThreadContext.put(THREAD_CONTEXT_USER_KEY,user);
	}

	public static void removeCurrentUser() {
		if(getCurrentUser() == null) {
			LOGGER.error("removeCurrentUser called but no current user existed");
		}
		ThreadContext.remove(THREAD_CONTEXT_USER_KEY);
	}

	public static void removeCurrentUserIfPresent() {
		ThreadContext.remove(THREAD_CONTEXT_USER_KEY);
	}

	public static void authorizeAnnotatedClass(Class<?> clazz) throws AuthorizationException {		
        Subject subject = SecurityUtils.getSubject();

        // authentication checks
        if ((subject == null || !subject.isAuthenticated()) && clazz.isAnnotationPresent(RequiresAuthentication.class)) {
            throw new UnauthenticatedException("Authentication required");
        }
        if ((subject != null && subject.getPrincipal() != null) && clazz.isAnnotationPresent(RequiresGuest.class)) {
            throw new UnauthenticatedException("Guest required");
        }
        if ((subject == null || subject.getPrincipal() == null) && clazz.isAnnotationPresent(RequiresUser.class)) {
            throw new UnauthenticatedException("User required");
        }

        // role checks
        RequiresRoles roles = clazz.getAnnotation(RequiresRoles.class);
        if (roles != null) {
            @SuppressWarnings("unchecked")
			List<String> roleList = Arrays.asList(roles.value());
			subject.checkRoles(roleList);
        }

        // permission checks
        RequiresPermissions permissions = clazz.getAnnotation(RequiresPermissions.class);
        if (permissions != null) {
             subject.checkPermissions(permissions.value());
        }
		
	}

}
