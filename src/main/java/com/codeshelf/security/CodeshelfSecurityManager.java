package com.codeshelf.security;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.ModularRealmAuthorizer;
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

import com.codeshelf.manager.User;
import com.google.inject.Inject;

public class CodeshelfSecurityManager extends AuthorizingSecurityManager {

	Realm oneRealm;
    protected SubjectFactory subjectFactory;

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
		Object userObj = ThreadContext.get(AuthFilter.REQUEST_ATTR);
		User user = null;
		if(userObj instanceof User) {
			user = (User)userObj;
		}
		Subject subject = null;
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

}
