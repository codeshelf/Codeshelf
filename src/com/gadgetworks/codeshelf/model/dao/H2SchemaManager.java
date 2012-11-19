/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.61 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class H2SchemaManager extends SchemaManagerABC {

	private static final Log	LOGGER	= LogFactory.getLog(H2SchemaManager.class);

	@Inject
	public H2SchemaManager(final IUtil inUtil,
		@Named(DATABASE_USERID_PROPERTY) final String inDbUserId,
		@Named(DATABASE_PASSWORD_PROPERTY) final String inDbPassword,
		@Named(DATABASE_NAME_PROPERTY) final String inDbName,
		@Named(DATABASE_SCHEMANAME_PROPERTY) final String inDbSchemaName,
		@Named(DATABASE_ADDRESS_PROPERTY) final String inDbAddress,
		@Named(DATABASE_PORTNUM_PROPERTY) final String inDbPortnum) {
		super(inUtil, inDbUserId, inDbPassword, inDbName, inDbSchemaName, inDbAddress, inDbPortnum);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#getDriverName()
	 */
	public String getDriverName() {
		return "org.h2.Driver";
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getApplicationInitDatabaseURL() {
		String result = "";

		// Setup the data directory for this application.
		result = "jdbc:h2:" + getUtil().getApplicationDataDirPath() + System.getProperty("file.separator") + "db" + System.getProperty("file.separator") + "database"
				+ ";TRACE_LEVEL_FILE=0;AUTO_SERVER=TRUE";

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getApplicationDatabaseURL() {
		String result = "";

		// Setup the data directory for this application.
		result = "jdbc:h2:" + getUtil().getApplicationDataDirPath() + System.getProperty("file.separator") + "db" + System.getProperty("file.separator") + "database"
				+ ";SCHEMA=CODESHELF;TRACE_LEVEL_FILE=0;AUTO_SERVER=TRUE";

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#doUpgradeSchema()
	 */
	protected boolean doUpgradeSchema() {

		// First get rid of the eBean dictionary file, so that the internal schema dictionary gets rebuilt.
		File dictFile = new File(getUtil().getApplicationLogDirPath() + System.getProperty("file.separator") + ".ebean.h2.dictionary");
		if (dictFile.exists()) {
			try {
				dictFile.delete();
			} catch (SecurityException e) {
				LOGGER.error("", e);
			}
		}
		return true;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#doDowngradeSchema()
	 */
	protected boolean doDowngradeSchema() {
		return false;
	}

}
