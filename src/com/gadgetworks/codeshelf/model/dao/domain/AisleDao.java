/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDao.java,v 1.2 2012/03/22 06:21:47 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.Aisle.IAisleDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class AisleDao extends GenericDao<Aisle> implements IAisleDao {
	@Inject
	public AisleDao(final IDaoRegistry inDaoRegistry, final IDbFacade<Aisle> inDbFacade) {
		super(Aisle.class, inDaoRegistry, inDbFacade);
	}
}
