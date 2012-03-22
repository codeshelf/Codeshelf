/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: UserSessionDao.java,v 1.3 2012/03/22 06:58:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.UserSession;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class UserSessionDao extends GenericDao<UserSession> implements IGenericDao<UserSession> {
	public UserSessionDao(final IDaoRegistry inDaoRegistry, final IDbFacade<UserSession> inDbFacade) {
		super(UserSession.class, inDaoRegistry, inDbFacade);
	}
}
