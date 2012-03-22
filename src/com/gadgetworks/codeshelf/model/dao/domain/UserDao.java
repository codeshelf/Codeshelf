/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: UserDao.java,v 1.3 2012/03/22 06:58:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.User;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class UserDao extends GenericDao<User> implements IGenericDao<User> {

	@Inject
	public UserDao(final IDaoRegistry inDaoRegistry, final IDbFacade<User> inDbFacade) {
		super(User.class, inDaoRegistry, inDbFacade);
	}
}
