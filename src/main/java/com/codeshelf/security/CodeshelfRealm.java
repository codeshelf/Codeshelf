/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfRealm.java,v 1.1 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.security;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;

/**
 * @author jeffw
 *
 */
public class CodeshelfRealm extends AuthorizingRealm {
	
	public CodeshelfRealm() {
	}
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		// look up user
		Integer userId = (Integer)principals.getPrimaryPrincipal();
		User user = TenantManagerService.getInstance().getUser(userId);
		
		// default role = usertype
		Set<String> roles = new HashSet<String>();
		roles.add(user.getType().toString());
		roles.addAll(user.getRoleNames());
		
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

		info.setStringPermissions(user.getPermissions());
		
		info.setRoles(roles);
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		return null; // authentication handled elsewhere
	}

}
