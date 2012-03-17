/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoRegistry.java,v 1.1 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;

/**
 * @author jeffw
 *
 */
public interface IDaoRegistry {

	void addDao(IDao inDao);

	List<IDao> getDaoList();
}
