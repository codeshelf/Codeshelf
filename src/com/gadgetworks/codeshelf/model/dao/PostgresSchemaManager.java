/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PostgresSchemaManager.java,v 1.5 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class PostgresSchemaManager extends SchemaManagerABC {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PostgresSchemaManager.class);

	@Inject
	public PostgresSchemaManager(
		@Named(DATABASE_USERID_PROPERTY) final String inDbUserId,
		@Named(DATABASE_PASSWORD_PROPERTY) final String inDbPassword,
		@Named(DATABASE_NAME_PROPERTY) final String inDbName,
		@Named(DATABASE_SCHEMANAME_PROPERTY) final String inDbSchemaName,
		@Named(DATABASE_ADDRESS_PROPERTY) final String inDbAddress,
		@Named(DATABASE_PORTNUM_PROPERTY) final String inDbPortnum) {
		super(inDbUserId, inDbPassword, inDbName, inDbSchemaName, inDbAddress, inDbPortnum);
	}

	public String getDriverName() {
		return "org.postgresql.Driver";
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#doUpgradeSchema()
	 */
	protected boolean doUpgradeSchema() {
		return true;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#doDowngradeSchema()
	 */
	protected boolean doDowngradeSchema() {
		return false;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#getApplicationInitDatabaseURL()
	 */
	public String getApplicationInitDatabaseURL() {
		return getApplicationDatabaseURL();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#getApplicationDatabaseURL()
	 */
	public String getApplicationDatabaseURL() {
		return "jdbc:postgresql://" + getDbAddress() + ":" + getDbPortnum() + "/" + getDbName() + "?user=" + getDbUserId()
				+ "&password=" + getDbPassword();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#getSchemaCheckerString()
	 */
	protected String getSchemaCheckerString() {
		return "select exists (select * from pg_catalog.pg_namespace where nspname = '" + getDbSchemaName() + "');";
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#getSchemaSetterString()
	 */
	protected String getSchemaSetterString() {
		return "SET SCHEMA '" + getDbSchemaName() + "'";
	}

	@Override
	public void deleteDatabase() {
		throw new RuntimeException("Delete Database not supported for Postgres");
	}		
}
