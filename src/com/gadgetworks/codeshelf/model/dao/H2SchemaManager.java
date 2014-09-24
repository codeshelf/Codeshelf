/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.67 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
import com.google.inject.Inject;
import com.google.inject.name.Named;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class H2SchemaManager extends SchemaManagerABC {

	private static final String	DB_INIT_URL	= "jdbc:h2:mem:database;DB_CLOSE_DELAY=-1";
	@SuppressWarnings("unused")
	private static final String	DB_URL		= "jdbc:h2:mem:database;SCHEMA=CODESHELF;DB_CLOSE_DELAY=-1";

	private static final Logger	LOGGER		= LoggerFactory.getLogger(H2SchemaManager.class);

	@Inject
	public H2SchemaManager(
		@Named(DATABASE_USERID_PROPERTY) final String inDbUserId,
		@Named(DATABASE_PASSWORD_PROPERTY) final String inDbPassword,
		@Named(DATABASE_NAME_PROPERTY) final String inDbName,
		@Named(DATABASE_SCHEMANAME_PROPERTY) final String inDbSchemaName,
		@Named(DATABASE_ADDRESS_PROPERTY) final String inDbAddress,
		@Named(DATABASE_PORTNUM_PROPERTY) final String inDbPortnum) {
		super(inDbUserId, inDbPassword, inDbName, inDbSchemaName, inDbAddress, inDbPortnum);

		// The H2 database has a serious problem with deleting temp files for LOBs.  We have to do it ourselves, or it will grow without bound.
		String[] extensions = { "temp.lob.db" };
		boolean recursive = true;

		File dbDir = new File(Configuration.getApplicationDataDirPath());
		@SuppressWarnings("unchecked")
		Collection<File> files = FileUtils.listFiles(dbDir, extensions, recursive);
		for (File file : files) {
			if (file.delete()) {
				LOGGER.debug("Deleted temporary LOB file = " + file.getPath());
			}
		}
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
		String result = DB_INIT_URL;

		// Setup the data directory for this application.
		// We switched H2 to in-memory instances only.		
		//		result = "jdbc:h2:" + getUtil().getApplicationDataDirPath() + System.getProperty("file.separator") + "db" + System.getProperty("file.separator") + "database"
		//				+ ";TRACE_LEVEL_FILE=0;AUTO_SERVER=TRUE";

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getApplicationDatabaseURL() {
		String result = DB_INIT_URL;

		// Setup the data directory for this application.
		// We switched H2 to in-memory instances only.
		//		result = "jdbc:h2:" + getUtil().getApplicationDataDirPath() + System.getProperty("file.separator") + "db" + System.getProperty("file.separator") + "database"
		//				+ ";SCHEMA=CODESHELF;TRACE_LEVEL_FILE=0;AUTO_SERVER=TRUE";

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#doUpgradeSchema()
	 */
	protected boolean doUpgradeSchema() {

		// First get rid of the eBean dictionary file, so that the internal schema dictionary gets rebuilt.
		File dictFile = new File(Configuration.getApplicationLogDirPath() + System.getProperty("file.separator") + ".ebean.h2.dictionary");
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#getSchemaCheckerString()
	 */
	protected String getSchemaCheckerString() {
		return "SET SCHEMA " + getDbSchemaName();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.SchemaManagerABC#getSchemaSetterString()
	 */
	protected String getSchemaSetterString() {
		return "SET SCHEMA " + getDbSchemaName();
	}

	@Override
	public void deleteDatabase() {
		try {
			Connection connection = DriverManager.getConnection(getApplicationDatabaseURL(), "codeshelf", "codeshelf");
			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			String sql = "DROP ALL OBJECTS";
			stmt.execute(sql);
			/*
			ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database where datname = '"+schemaManager.getDbName()+"'");
			if (rs.getFetchSize()==1) {
				LOGGER.debug("Database "+schemaManager.getDbName()+" exists.");
			}
			else {
				LOGGER.debug("Database "+schemaManager.getDbName()+" does not exist.");
			}
			*/
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			LOGGER.error("Failed to delete H2 Database", e);
		}		
	}
}
