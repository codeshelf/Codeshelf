/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.3 2011/01/21 04:25:54 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.Util;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class H2SchemaManager implements ISchemaManager {

	private static final Log	LOGGER	= LogFactory.getLog(H2SchemaManager.class);

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#doesSchemaExist()
	 */
	public boolean doesSchemaExist() {
		boolean result = false;

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(Util.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("SET SCHEMA " + ISchemaManager.DATABASE_SCHEMA_NAME);
			stmt.close();
			// If we get here then we were able to switch to the schema and it exists.
			result = true;

			connection.close();
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SQLException e) {
			LOGGER.error("", e);
		}
		return result;
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#creatNewSchema()
	 */
	public boolean creatNewSchema() {
		boolean result = false;
		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(Util.getApplicationInitDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE SCHEMA " + ISchemaManager.DATABASE_SCHEMA_NAME);
			stmt.close();

			// Try to switch to the proper schema.
			stmt = connection.createStatement();
			stmt.executeUpdate("set SCHEMA " + ISchemaManager.DATABASE_SCHEMA_NAME);
			stmt.close();

			// If we get here then we were able to switch to the schema and it exists.
			createStructures();
			connection.close();
			result = true;
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SQLException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inFromSchema
	 *  @param inToSchema
	 */
	public void upgradeSchema(int inOldVersion, int inNewVersion) {
		// IMPORTANT:
		// Apply these upgrades in version order.

		// First get rid of the eBean dictionary file, so that the internal schema dictionary gets rebuilt.
		File dictFile = new File(Util.getApplicationLogDirPath() + System.getProperty("file.separator") + ".ebean.h2.dictionary");
		if (dictFile.exists()) {
			try {
				dictFile.delete();
			} catch (SecurityException e) {
				LOGGER.error("", e);
			}
		}

		// We need to add the "default connection" to the account object.
		if (inOldVersion < ISchemaManager.DATABASE_VERSION_1) {
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inFromSchema
	 *  @param inToSchema
	 */
	public void downgradeSchema(int inOldVersion, int inNewVersion) {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inSQLCommand
	 *  @return
	 */
	private boolean execOneSQLCommand(String inSQLCommand) {
		boolean result = false;

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(Util.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(inSQLCommand);
			stmt.close();

			connection.close();
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SQLException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the initial structures for the DB as of DATABASE_VERSION_CUR.
	 */
	private void createStructures() {

		// DBProperty
		execOneSQLCommand("CREATE TABLE CODESHELF.DBPROPERTY (" //
				+ "MPERSISTENTID INT AUTO_INCREMENT NOT NULL," //
				+ "MCURRENTVALUESTR VARCHAR(256)," //
				+ "MPROPERTYID VARCHAR(10)," //
				+ "MVERSION TIMESTAMP," //
				+ "PRIMARY KEY (MPERSISTENTID));");

		// PersistentProperty
		execOneSQLCommand("CREATE TABLE CODESHELF.PERSISTENTPROPERTY (" //
				+ "MPERSISTENTID INT AUTO_INCREMENT NOT NULL," //
				+ "MPROPERTYID VARCHAR(10)," //
				+ "MCURRENTVALUESTR VARCHAR(256)," //
				+ "MDEFAULTVALUESTR VARCHAR(256)," //
				+ "MVERSION TIMESTAMP," //
				+ "PRIMARY KEY (MPERSISTENTID));");

		// WirelessDevice (includes the subclass variants in one table)
		execOneSQLCommand("CREATE TABLE CODESHELF.WIRELESSDEVICE (" //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "MPERSISTENTID INT AUTO_INCREMENT NOT NULL," //
				+ "MPARENTCONTROLGROUP_MPERSISTENTID BIGINT NOT NULL, " //
				+ "MGUID VARCHAR(16) NOT NULL," //
				+ "MPUBLICKEY VARCHAR(16) NOT NULL," //
				+ "MDESCRIPTION VARCHAR(64)," //
				+ "MLASTBATTERYLEVEL SMALLINT DEFAULT 0 NOT NULL," //
				+ "MNETWORKADDRESS INTEGER NOT NULL," //
				+ "MNETWORKDEVICESTATUS VARCHAR(16) DEFAULT 'INVALID'," //
				+ "MLASTCONTACTTIME BIGINT DEFAULT 0," //
				+ "MVERSION TIMESTAMP," //
				+ "PRIMARY KEY (MPERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.WIRELESSDEVICE " //
				+ "ADD FOREIGN KEY (MPARENTCONTROLGROUP_MPERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.CONTROLGROUP (MPERSISTENTID);");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESS_DEVICE_PRIMARY_KEY ON CODESHELF.WIRELESSDEVICE (MPERSISTENTID)");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_GUID_INDEX ON CODESHELF.WIRELESSDEVICE (MGUID)");

		// SnapNetwork
		execOneSQLCommand("CREATE TABLE CODESHELF.SNAPNETWORK ( " //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "MPERSISTENTID INT AUTO_INCREMENT NOT NULL, " //
				+ "MID VARCHAR(16) NOT NULL, " //
				+ "MDESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "MISACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "MVERSION TIMESTAMP, " //
				+ "PRIMARY KEY (MPERSISTENTID));");

		// ControlGroup
		execOneSQLCommand("CREATE TABLE CODESHELF.CONTROLGROUP ( " //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "MPERSISTENTID INT AUTO_INCREMENT NOT NULL, " //
				+ "MPARENTSNAPNETWORK_MPERSISTENTID BIGINT NOT NULL, " //
				+ "MID VARCHAR(16) NOT NULL, " //
				+ "MDESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "MISACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "MVERSION TIMESTAMP, " //
				+ "PRIMARY KEY (MPERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.CONTROLGROUP " //
				+ "ADD FOREIGN KEY (MPARENTSNAPNETWORK_MPERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.SNAPNETWORK (MPERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.CONTROLGROUP_PARENT_SNAPNETWORK ON CODESHELF.CONTROLGROUP (MPARENTSNAPNETWORK_MPERSISTENTID)");
	}
}
