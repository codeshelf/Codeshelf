/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: VertexDao.java,v 1.1 2012/04/13 18:54:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.Vertex;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class VertexDao extends GenericDao<Vertex> implements IGenericDao<Vertex> {
	@Inject
	public VertexDao(final IDaoRegistry inDaoRegistry) {
		super(Vertex.class, inDaoRegistry);
	}
}
