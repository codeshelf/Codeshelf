/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.19 2012/03/18 04:12:26 jeffw Exp $
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
		if (inOldVersion < ISchemaManager.DATABASE_VERSION_2) {

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
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "VALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		// PersistentProperty
		execOneSQLCommand("CREATE TABLE CODESHELF.PERSISTENTPROPERTY (" //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "CURRENTVALUESTR VARCHAR(256)," //
				+ "DEFAULTVALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		// organization
		execOneSQLCommand("CREATE TABLE CODESHELF.ORGANIZATION ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		// Facility
		execOneSQLCommand("CREATE TABLE CODESHELF.FACILITY ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "PARENTORGANIZATION_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.FACILITY " //
			+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.FACILITY_PARENT_ORGANIZATION ON CODESHELF.FACILITY (PARENTORGANIZATION_PERSISTENTID)");

		// User
		execOneSQLCommand("CREATE TABLE CODESHELF.USER ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "HASHEDPASSWORD VARCHAR(64), " //
				+ "EMAIL VARCHAR(64), " //
				+ "CREATED TIMESTAMP, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PARENTORGANIZATION_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.USER_ID_KEY ON CODESHELF.USER (ID)");

		execOneSQLCommand("ALTER TABLE CODESHELF.USER " //
			+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.USER_PARENT_ORGANIZATION ON CODESHELF.USER (PARENTORGANIZATION_PERSISTENTID)");

		// UserSession
		execOneSQLCommand("CREATE TABLE CODESHELF.USERSESSION ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTUSERSESSION_PERSISTENTID LONG NOT NULL, " //
				+ "ACTIVITY VARCHAR(64) NOT NULL, " //
				+ "CREATED TIMESTAMP NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.USERSESSION " //
				+ "ADD FOREIGN KEY (PARENTUSERSESSION_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.USER (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.USERSESSION_PARENT_USER ON CODESHELF.USERSESSION (PARENTUSERSESSION_PERSISTENTID)");

		// Aisle
		execOneSQLCommand("CREATE TABLE CODESHELF.AISLE ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTFACILITY_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.AISLE " //
				+ "ADD FOREIGN KEY (PARENTFACILITY_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.FACILITY (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.AISLE_PARENT_FACILITY ON CODESHELF.AISLE (PARENTFACILITY_PERSISTENTID)");

		// CodeShelfNetwork
		execOneSQLCommand("CREATE TABLE CODESHELF.CODESHELFNETWORK ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "NETWORKID BINARY(2) DEFAULT 0 NOT NULL," //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "GATEWAYADDR BINARY(3) NOT NULL, " //
				+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PRIMARY KEY (PERSISTENTID));");

		// ControlGroup
		execOneSQLCommand("CREATE TABLE CODESHELF.CONTROLGROUP ( " //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTCODESHELFNETWORK_PERSISTENTID LONG NOT NULL, " //
				+ "CONTROLGROUPID BINARY(2) DEFAULT 0 NOT NULL, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "INTERFACEPORTNUM INT NOT NULL, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "TAGPROTOCOLENUM VARCHAR(16) NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.CONTROLGROUP " //
				+ "ADD FOREIGN KEY (PARENTCODESHELFNETWORK_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.CODESHELFNETWORK (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.CONTROLGROUP_PARENT_CODESHELFNETWORK ON CODESHELF.CONTROLGROUP (PARENTCODESHELFNETWORK_PERSISTENTID)");

		// WirelessDevice (includes the subclass variants in one table)
		execOneSQLCommand("CREATE TABLE CODESHELF.WIRELESSDEVICE (" //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "PERSISTENTID IDENTITY NOT NULL, " //
				+ "ID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTCONTROLGROUP_PERSISTENTID LONG NOT NULL, " //
				+ "MACADDRESS BINARY(8) DEFAULT 0 NOT NULL," //
				+ "PUBLICKEY VARCHAR(16) NOT NULL," //
				+ "DESCRIPTION VARCHAR(64)," //
				+ "LASTBATTERYLEVEL SMALLINT DEFAULT 0 NOT NULL," //
				+ "NETWORKADDRESS BINARY(3) DEFAULT 0 NOT NULL," //
				+ "NETWORKDEVICESTATUS VARCHAR(16) DEFAULT 'INVALID'," //
				+ "SERIALBUSPOSITION INT DEFAULT 0," //
				+ "LASTCONTACTTIME BIGINT DEFAULT 0," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.WIRELESSDEVICE " //
				+ "ADD FOREIGN KEY (PARENTCONTROLGROUP_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.CONTROLGROUP (PERSISTENTID);");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESS_DEVICE_PRIMARY_KEY ON CODESHELF.WIRELESSDEVICE (PERSISTENTID)");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_MACADDRESS_INDEX ON CODESHELF.WIRELESSDEVICE (MACADDRESS)");
	}
}
