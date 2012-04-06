/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationDao.java,v 1.1 2012/04/06 20:45:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.Location;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class LocationDao extends GenericDao<Location> implements IGenericDao<Location> {
	@Inject
	public LocationDao(final IDaoRegistry inDaoRegistry) {
		super(Location.class, inDaoRegistry);
	}
}
