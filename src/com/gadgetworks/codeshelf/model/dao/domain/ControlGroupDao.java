/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupDao.java,v 1.5 2012/07/11 07:15:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class ControlGroupDao extends GenericDao<ControlGroup> implements ITypedDao<ControlGroup> {
	@Inject
	public ControlGroupDao(final IDaoRegistry inDaoRegistry) {
		super(ControlGroup.class, inDaoRegistry);
	}
}
