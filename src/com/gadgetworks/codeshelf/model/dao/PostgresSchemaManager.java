/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PostgresSchemaManager.java,v 1.1 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class PostgresSchemaManager extends SchemaManagerABC {

	private static final Log	LOGGER	= LogFactory.getLog(PostgresSchemaManager.class);

	@Inject
	public PostgresSchemaManager(final IUtil inUtil,
		@Named(DATABASE_USERID_PROPERTY) final String inDbUserId,
		@Named(DATABASE_PASSWORD_PROPERTY) final String inDbPassword,
		@Named(DATABASE_NAME_PROPERTY) final String inDbName,
		@Named(DATABASE_SCHEMANAME_PROPERTY) final String inDbSchemaName,
		@Named(DATABASE_ADDRESS_PROPERTY) final String inDbAddress,
		@Named(DATABASE_PORTNUM_PROPERTY) final String inDbPortnum) {
		super(inUtil, inDbUserId, inDbPassword, inDbName, inDbSchemaName, inDbAddress, inDbPortnum);
	}

	public String getDriverName() {
		return "org.postgresql.Driver";
	}

	protected boolean doUpgradeSchema() {
		return true;
	}

	protected boolean doDowngradeSchema() {
		return false;
	}

	public String getApplicationInitDatabaseURL() {
		return "jdbc:postgresql://" + getDbAddress() + ":" + getDbPortnum() + "/" + getDbName();
	}

	public String getApplicationDatabaseURL() {
		return "jdbc:postgresql://" + getDbAddress() + ":" + getDbPortnum() + "/" + getDbName();
	}

}
