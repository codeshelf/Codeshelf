/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDao.java,v 1.1 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

/**
 * @author jeffw
 *
 */
public interface IDao {

	void registerDAOListener(IDaoListener inListener);

	void unregisterDAOListener(IDaoListener inListener);

	void removeDAOListeners();

}
