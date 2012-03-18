/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: UserSessionDao.java,v 1.1 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.persist.UserSession;
import com.gadgetworks.codeshelf.model.persist.UserSession.IUserSessionDao;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class UserSessionDao extends GenericDao<UserSession> implements IUserSessionDao {
	public UserSessionDao(final IDaoRegistry inDaoRegistry) {
		super(UserSession.class, inDaoRegistry);
	}
}
