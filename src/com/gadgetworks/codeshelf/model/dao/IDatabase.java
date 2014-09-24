/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDatabase.java,v 1.1 2012/10/11 09:04:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

/**
 * @author jeffw
 *
 */
public interface IDatabase {

	boolean start();

	boolean stop();

	ISchemaManager getSchemaManager();

	void deleteDatabase();
}
