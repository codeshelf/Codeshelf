/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: VertexDao.java,v 1.2 2012/07/11 07:15:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.persist.Vertex;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class VertexDao extends GenericDao<Vertex> implements ITypedDao<Vertex> {
	@Inject
	public VertexDao(final IDaoRegistry inDaoRegistry) {
		super(Vertex.class, inDaoRegistry);
	}
}
