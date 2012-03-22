/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentPropertyDao.java,v 1.3 2012/03/22 06:58:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class PersistentPropertyDao extends GenericDao<PersistentProperty> implements IGenericDao<PersistentProperty> {
	@Inject
	public PersistentPropertyDao(final IDaoRegistry inDaoRegistry, final IDbFacade<PersistentProperty> inDbFacade) {
		super(PersistentProperty.class, inDaoRegistry, inDbFacade);
	}
}
