/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBPropertyDao.java,v 1.2 2012/03/22 06:21:47 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.DBProperty.IDBPropertyDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class DBPropertyDao extends GenericDao<DBProperty> implements IDBPropertyDao {
	@Inject
	public DBPropertyDao(final IDaoRegistry inDaoRegistry, final IDbFacade<DBProperty> inDbFacade) {
		super(DBProperty.class, inDaoRegistry, inDbFacade);
	}
}
