/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDao.java,v 1.2 2012/09/08 03:03:24 jeffw Exp $
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
