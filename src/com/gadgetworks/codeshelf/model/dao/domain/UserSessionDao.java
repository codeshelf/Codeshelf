/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: UserSessionDao.java,v 1.5 2012/07/11 07:15:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.persist.UserSession;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class UserSessionDao extends GenericDao<UserSession> implements ITypedDao<UserSession> {
	public UserSessionDao(final IDaoRegistry inDaoRegistry) {
		super(UserSession.class, inDaoRegistry);
	}
}
