/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentPropertyDao.java,v 1.1 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty.IPersistentPropertyDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class PersistentPropertyDao extends GenericDao<PersistentProperty> implements IPersistentPropertyDao {
	@Inject
	public PersistentPropertyDao(final IDaoRegistry inDaoRegistry) {
		super(PersistentProperty.class, inDaoRegistry);
	}
}