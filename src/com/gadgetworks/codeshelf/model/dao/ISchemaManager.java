/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISchemaManager.java,v 1.5 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface ISchemaManager {

	String	DATABASE_SCHEMA_NAME	= "CODESHELF";

	// Database schema version increases monotonically as there are incompatible changes to the DB schema.	
	int		DATABASE_VERSION_1		= 1;					// Original DB
	int		DATABASE_VERSION_2		= 2;					// Facility, Aisle, Bay
	int		DATABASE_VERSION_3		= 3;					// EDI

	int		DATABASE_VERSION_CUR	= DATABASE_VERSION_3;

	boolean doesSchemaExist();

	boolean creatNewSchema();

	void upgradeSchema(int inOldVersion, int inNewVersion);

	void downgradeSchema(int inOldVersion, int inNewVersion);
}
