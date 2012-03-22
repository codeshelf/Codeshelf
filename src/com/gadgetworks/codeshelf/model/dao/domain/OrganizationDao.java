/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: OrganizationDao.java,v 1.4 2012/03/22 20:17:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class OrganizationDao extends GenericDao<Organization> implements IGenericDao<Organization> {

	@Inject
	public OrganizationDao(final IDaoRegistry inDaoRegistry) {
		super(Organization.class, inDaoRegistry);
	}
}
