/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISchemaManager.java,v 1.14 2013/11/11 07:46:30 jeffw Exp $
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
	String	DATABASE_SSL_PROPERTY			= "DATABASE_SSL_PROPERTY";

	// Database schema version increases monotonically as there are incompatible changes to the DB schema.	
	int		DATABASE_VERSION_1				= 1;								// Original DB
	int		DATABASE_VERSION_2				= 2;								// Add order fields
	int		DATABASE_VERSION_3				= 3;								// Add Slot Flex tag ID to item master.
	int		DATABASE_VERSION_4				= 4;								// Add LED command stream processing to WIs.
	int		DATABASE_VERSION_5				= 5;								// Add location alias table.
	int		DATABASE_VERSION_6				= 6;								// Add order location table.
	int		DATABASE_VERSION_7				= 7;								// Add order type enum.
	int		DATABASE_VERSION_8				= 8;								// Homongenize all of the quantities as decimal
	int		DATABASE_VERSION_9				= 9;								// Add object references back to WorkInstruction
	int		DATABASE_VERSION_10				= 10;								// Add face width/height to locations.
	int		DATABASE_VERSION_11				= 11;								// Cleanup anchor and pick face point structures.
	int		DATABASE_VERSION_12				= 12;								// Remove anchor location from path segement.
	int		DATABASE_VERSION_13				= 13;								// Add z-point to path segments.

	int		DATABASE_VERSION_CUR			= DATABASE_VERSION_13;

	String getDbAddress();

	String getDbName();

	String getDbPassword();

	String getDbPortnum();

	String getDbSchemaName();

	String getDbUserId();

	String getDbSsl();

	boolean verifySchema();

	String getDriverName();

	String getApplicationInitDatabaseURL();

	String getApplicationDatabaseURL();

}
