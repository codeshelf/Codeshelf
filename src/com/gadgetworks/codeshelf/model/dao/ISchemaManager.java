/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISchemaManager.java,v 1.11 2012/11/20 04:10:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface ISchemaManager {

	String	DATABASE_SCHEMA_NAME_PROPERTY	= "DATABASE_SCHEMA_NAME_PROPERTY";
	String	DATABASE_NAME_PROPERTY			= "DATABASE_NAME_PROPERTY";
	String	DATABASE_SCHEMANAME_PROPERTY	= "DATABASE_SCHEMANAME_PROPERTY";
	String	DATABASE_ADDRESS_PROPERTY		= "DATABASE_ADDRESS_PROPERTY";
	String	DATABASE_PORTNUM_PROPERTY		= "DATABASE_PORTNUM_PROPERTY";
	String	DATABASE_USERID_PROPERTY		= "DATABASE_USERID_PROPERTY";
	String	DATABASE_PASSWORD_PROPERTY		= "DATABASE_PASSWORD_PROPERTY";

	// Database schema version increases monotonically as there are incompatible changes to the DB schema.	
	int		DATABASE_VERSION_1				= 1;								// Original DB

	int		DATABASE_VERSION_CUR			= DATABASE_VERSION_1;

	String getDbAddress();
	
	String getDbName();
	
	String getDbPassword();
	
	String getDbPortnum();
	
	String getDbSchemaName();
	
	String getDbUserId();
	
	boolean verifySchema();

	String getDriverName();

	String getApplicationInitDatabaseURL();

	String getApplicationDatabaseURL();

}
