/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.37 2012/09/16 07:22:15 jeffw Exp $
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
		if (inOldVersion < ISchemaManager.DATABASE_VERSION_3) {

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
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "VALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.DBPROPERTY_DOMAINID_INDEX ON CODESHELF.DBPROPERTY (DOMAINID)");

		// organization
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.ORGANIZATION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.ORGANIZATION ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.ORGANIZATION_DOMAINID_INDEX ON CODESHELF.ORGANIZATION (DOMAINID)");

		// PersistentProperty
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.PERSISTENTPROPERTY_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.PERSISTENTPROPERTY (" //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "CURRENTVALUESTR VARCHAR(256)," //
				+ "DEFAULTVALUESTR VARCHAR(256)," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.PERSISTENTPROPERTY " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.PERSISTENTPROPERTY_PARENT_ORGANIZATION ON CODESHELF.PERSISTENTPROPERTY (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.PERSISTENTPROPERTY_DOMAINID_INDEX ON CODESHELF.PERSISTENTPROPERTY (DOMAINID)");

		// Location
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.LOCATION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.LOCATION ( " //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "POSTYPE VARCHAR(64) NOT NULL, " //
				+ "POSX DOUBLE NOT NULL, " //
				+ "POSY DOUBLE NOT NULL, " //
				+ "POSZ DOUBLE, " // NOT NULL, " //
				+ "DESCRIPTION VARCHAR(64), "// NOT NULL, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PARENTORGANIZATION_PERSISTENTID LONG, "// NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		// Add the foreign key constraint for Facility organization.
		execOneSQLCommand("ALTER TABLE CODESHELF.LOCATION " //
				+ "ADD FOREIGN KEY (PARENTORGANIZATION_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		// Add an index to make the facility-organization foreign key higher performance.
		execOneSQLCommand("CREATE INDEX CODESHELF.LOCATION_PARENT_ORGANIZATION ON CODESHELF.LOCATION (PARENTORGANIZATION_PERSISTENTID)");

		// Add the foreign key constraint for parent-child location arrangements.
		execOneSQLCommand("ALTER TABLE CODESHELF.LOCATION " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.LOCATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		// Add an index to make the parent-child location foreign key higher performance.
		execOneSQLCommand("CREATE INDEX CODESHELF.LOCATION_PARENT_LOCATION ON CODESHELF.LOCATION (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.LOCATION_DOMAINID_INDEX ON CODESHELF.LOCATION (DOMAINID)");

		// Vertex
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.VERTEX_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.VERTEX ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "POSTYPE VARCHAR(64) NOT NULL, " //
				+ "POSX DOUBLE NOT NULL, " //
				+ "POSY DOUBLE NOT NULL, " //
				+ "DRAWORDER INT NOT NULL, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.VERTEX " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.LOCATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.VERTEX_PARENT_LOCATION ON CODESHELF.VERTEX (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.VERTEX_DOMAINID_INDEX ON CODESHELF.VERTEX (DOMAINID)");

		// Path
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.PATH_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.PATH ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.PATH_DOMAINID_INDEX ON CODESHELF.PATH (DOMAINID)");

		// PathSegment
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.PATHSEGMENT_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.PATHSEGMENT ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.PATHSEGMENT " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.PATH (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.PATHSEGMENT_PARENT_PATH ON CODESHELF.PATHSEGMENT (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.PATHSEGMENT_DOMAINID_INDEX ON CODESHELF.PATHSEGMENT (DOMAINID)");

		// User
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.USER_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.USER ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "HASHEDPASSWORD VARCHAR(64), " //
				+ "EMAIL VARCHAR(64), " //
				+ "CREATED TIMESTAMP, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.USER " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.ORGANIZATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.USER_PARENT_ORGANIZATION ON CODESHELF.USER (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.USER_DOMAINID_INDEX ON CODESHELF.USER (DOMAINID)");

		// UserSession
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.USERSESSION_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.USERSESSION ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "ACTIVITY VARCHAR(64) NOT NULL, " //
				+ "CREATED TIMESTAMP NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.USERSESSION " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.USER (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.USERSESSION_PARENT_USER ON CODESHELF.USERSESSION (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.USERSESSION_DOMAINID_INDEX ON CODESHELF.USERSESSION (DOMAINID)");

		// CodeShelfNetwork
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.CODESHELFNETWORK_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.CODESHELFNETWORK ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "NETWORKID BINARY(2) DEFAULT 0 NOT NULL," //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "GATEWAYADDR BINARY(3) NOT NULL, " //
				+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.CODESHELFNETWORK " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.LOCATION (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.CODESHELFNETWORK_PARENT_FACILITY ON CODESHELF.CODESHELFNETWORK (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.CODESHELFNETWORK_DOMAINID_INDEX ON CODESHELF.CODESHELFNETWORK (DOMAINID)");

		// ControlGroup
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.CONTROLGROUP_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.CONTROLGROUP ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "CONTROLGROUPID BINARY(2) DEFAULT 0 NOT NULL, " //
				+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
				+ "INTERFACEPORTNUM INT NOT NULL, " //
				+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
				+ "TAGPROTOCOLENUM VARCHAR(16) NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.CONTROLGROUP " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.CODESHELFNETWORK (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.CONTROLGROUP_PARENT_CODESHELFNETWORK ON CODESHELF.CONTROLGROUP (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.CONTROLGROUP_DOMAINID_INDEX ON CODESHELF.CONTROLGROUP (DOMAINID)");

		// WirelessDevice (includes the subclass variants in one table)
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.WIRELESSDEVICE_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.WIRELESSDEVICE (" //
				+ "DTYPE VARCHAR(20) NOT NULL," //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
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
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.CONTROLGROUP (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.WIRELESS_DEVICE_PARENT_CONTROLGROUP ON CODESHELF.WIRELESSDEVICE (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_MACADDRESS_INDEX ON CODESHELF.WIRELESSDEVICE (MACADDRESS)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_DOMAINID_INDEX ON CODESHELF.WIRELESSDEVICE (DOMAINID)");

		// EdiService
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.EDISERVICE_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.EDISERVICE ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "PROVIDERENUM VARCHAR(16) NOT NULL, " //
				+ "SERVICESTATEENUM VARCHAR(16) NOT NULL, " //
				+ "PROVIDERCREDENTIALS VARCHAR(256) NOT NULL, " //
				+ "DTYPE VARCHAR(64) NOT NULL," //
				+ "CURSOR VARCHAR(256)," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.EDISERVICE " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.FACILITY (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.EDISERVICE_PARENT_FACILITY ON CODESHELF.EDISERVICE (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.EDISERVICE_DOMAINID_INDEX ON CODESHELF.EDISERVICE (DOMAINID)");

		// EdiDocumentLocator
		execOneSQLCommand("CREATE SEQUENCE CODESHELF.EDIDOCUMENTLOCATOR_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF.EDIDOCUMENTLOCATOR ( " //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "DOCUMENTID VARCHAR(64) NOT NULL," //
				+ "DOCUMENTNAME VARCHAR(256) NOT NULL," //
				+ "DOCUMENTSTATEENUM VARCHAR(16) NOT NULL, " //
				+ "RECEIVED TIMESTAMP, " //
				+ "PROCESSED TIMESTAMP, " //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ "PARENT_PERSISTENTID LONG NOT NULL, " //
				+ "PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("ALTER TABLE CODESHELF.EDIDOCUMENTLOCATOR " //
				+ "ADD FOREIGN KEY (PARENT_PERSISTENTID) " //
				+ "REFERENCES DATABASE.CODESHELF.EDISERVICE (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		execOneSQLCommand("CREATE INDEX CODESHELF.EDIDOCUMENTLOCATOR_PARENT_EDISERVICE ON CODESHELF.EDIDOCUMENTLOCATOR (PARENT_PERSISTENTID)");
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.EDIDOCUMENTLOCATOR_DOMAINID_INDEX ON CODESHELF.EDIDOCUMENTLOCATOR (DOMAINID)");
}
}
