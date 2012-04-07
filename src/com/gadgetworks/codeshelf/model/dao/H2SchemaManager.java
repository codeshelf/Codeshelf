/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.24 2012/04/07 19:42:16 jeffw Exp $
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
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.DBPROPERTY_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.DBPROPERTY (" //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "VALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		// PersistentProperty
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.PERSISTENTPROPERTY_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.PERSISTENTPROPERTY (" //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "PARENTORGANIZATION_PERSISTENTID LONG NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "CURRENTVALUESTR VARCHAR(256)," //
				+ "DEFAULTVALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.PERSISTENTPROPERTY " //
			+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.PERSISTENTPROPERTY (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.PERSISTENTPROPERTY_PARENT_ORGANIZATION ON CODESHELF.PERSISTENTPROPERTY (PARENTORGANIZATION_PERSISTENTID)");

		// organization
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.ORGANIZATION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.ORGANIZATION ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		// Location
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.LOCATION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.LOCATION ( " //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "POSX LONG NOT NULL, " //
				+ "POSY LONG NOT NULL, " //
				+ "POSZ LONG NOT NULL, " //
				+ "DESCRIPTION VARCHAR(64), "// NOT NULL, " //
				+ "PARENTLOCATION_PERSISTENTID LONG NOT NULL, " //
				+ "PARENTORGANIZATION_PERSISTENTID LONG, "// NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		// Add the foreign key constraint for Facility organization.
		execOneSQLCommand("ALTER TABLE CODESHELF.LOCATION " //
			+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID);");

		// Add an index to make the facility-organization foreign key higher performance.
		execOneSQLCommand("CREATE INDEX CODESHELF.FACILITY_PARENT_ORGANIZATION ON CODESHELF.LOCATION (PARENTORGANIZATION_PERSISTENTID)");

		// Add the foreign key constraint for parent-child location arrangements.
		execOneSQLCommand("ALTER TABLE CODESHELF.LOCATION " //
			+ "ADD FOREIGN KEY (PARENTLOCATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.LOCATION (PERSISTENTID);");

		// Add an index to make the parent-child location foreign key higher performance.
		execOneSQLCommand("CREATE INDEX CODESHELF.LOCATION_PARENT_LOCATION ON CODESHELF.LOCATION (PARENTLOCATION_PERSISTENTID)");

		// Vertex
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.VERTEX_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.VERTEX ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "POSX LONG NOT NULL, " //
				+ "POSY LONG NOT NULL, " //
				+ "SORTORDER INT NOT NULL, " //
				+ "PARENTLOCATION_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.VERTEX " //
			+ "ADD FOREIGN KEY (PARENTLOCATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.LOCATION (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.VERTEX_PARENT_LOCATION ON CODESHELF.VERTEX (PARENTLOCATION_PERSISTENTID)");

		// User
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.USER_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.USER ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "HASHEDPASSWORD VARCHAR(64), " //
				+ "EMAIL VARCHAR(64), " //
				+ "CREATED TIMESTAMP, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PARENTORGANIZATION_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.USER_ID_KEY ON CODESHELF.USER (PERSISTENTID)");

		execOneSQLCommand("ALTER TABLE CODESHELF.USER " //
			+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.USER_PARENT_ORGANIZATION ON CODESHELF.USER (PARENTORGANIZATION_PERSISTENTID)");

		// UserSession
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.USERSESSION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.USERSESSION ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTUSERSESSION_PERSISTENTID LONG NOT NULL, " //
				+ "ACTIVITY VARCHAR(64) NOT NULL, " //
				+ "CREATED TIMESTAMP NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.USERSESSION " //
				+ "ADD FOREIGN KEY (PARENTUSERSESSION_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.USER (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.USERSESSION_PARENT_USER ON CODESHELF.USERSESSION (PARENTUSERSESSION_PERSISTENTID)");

		// CodeShelfNetwork
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.CODESHELFNETWORK_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.CODESHELFNETWORK ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "VERSION TIMESTAMP, " //
				+ "PARENTFACILITY_PERSISTENTID LONG NOT NULL, " //
				+ "NETWORKID BINARY(2) DEFAULT 0 NOT NULL," //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "GATEWAYADDR BINARY(3) NOT NULL, " //
				+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.CODESHELFNETWORK " //
			+ "ADD FOREIGN KEY (PARENTFACILITY_PERSISTENTID) " //
			+ "REFERENCES DATABASE.CODESHELF.CODESHELFNETWORK (PERSISTENTID);");

		execOneSQLCommand("CREATE INDEX CODESHELF.CODESHELFNETWORK_PARENT_FACILITY ON CODESHELF.CODESHELFNETWORK (PARENTFACILITY_PERSISTENTID)");

		// ControlGroup
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.CONTROLGROUP_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.CONTROLGROUP ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
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
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.WIRELESSDEVICE_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.WIRELESSDEVICE (" //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
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
