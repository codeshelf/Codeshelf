/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfRealm.java,v 1.1 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;

/**
 * @author jeffw
 *
 */
public class CodeshelfRealm implements Realm {

	/**
	 * 
	 */
	public CodeshelfRealm() {
		// TODO Auto-generated constructor stub
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see org.apache.shiro.realm.Realm#getName()
	 */
	public final String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see org.apache.shiro.realm.Realm#supports(org.apache.shiro.authc.AuthenticationToken)
	 */
	public final boolean supports(AuthenticationToken inToken) {
		// TODO Auto-generated method stub
		return false;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see org.apache.shiro.realm.Realm#getAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)
	 */
	public final AuthenticationInfo getAuthenticationInfo(AuthenticationToken inToken) throws AuthenticationException {
		// TODO Auto-generated method stub
		return null;
	}

}
