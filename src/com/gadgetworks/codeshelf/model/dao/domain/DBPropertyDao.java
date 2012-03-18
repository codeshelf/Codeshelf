/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBPropertyDao.java,v 1.1 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
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
	public DBPropertyDao(final IDaoRegistry inDaoRegistry) {
		super(DBProperty.class, inDaoRegistry);
	}
}
