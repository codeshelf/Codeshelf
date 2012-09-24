/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.40 2012/09/24 08:23:47 jeffw Exp $
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

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_4) {

			// OrderHeader
			createTable("ORDERHEADER", //
				"ORDERID VARCHAR(64)," //
						+ "PARENT_PERSISTENTID LONG NOT NULL " //
			);

			linkToParentTable("ORDERHEADER", "PARENT", "LOCATION");

			// One extra wireless device index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
			execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.FACILITY_ORDERID_INDEX ON CODESHELF.ORDERHEADER (PARENT_PERSISTENTID, ORDERID)");

			// OrderDetail
			createTable("ORDERDETAIL", //
				"ORDERID VARCHAR(64) NOT NULL," //
						+ "DETAILID VARCHAR(64) NOT NULL," //
						+ "SKU VARCHAR(64) NOT NULL," //
						+ "DESCRIPTION VARCHAR(256) NOT NULL," //
						+ "QUANTITY INTEGER NOT NULL, " //
						+ "UOM VARCHAR(16) NOT NULL," //
						+ "ORDERDATE TIMESTAMP, " //
						+ "PARENT_PERSISTENTID LONG NOT NULL " //
			);

			linkToParentTable("ORDERDETAIL", "PARENT", "ORDERHEADER");
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
	 * Create a standard DomainObject table with all the appropriate boilerplate and then add the stuff for the particular domain class.
	 * @param inTableName
	 * @param inColumns
	 */
	private void createTable(final String inTableName, final String inColumns) {

		execOneSQLCommand("CREATE SEQUENCE CODESHELF." + inTableName + "_SEQ");
		execOneSQLCommand("CREATE TABLE CODESHELF." + inTableName + " (" //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL," //
				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (PERSISTENTID));");

		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF." + inTableName + "_DOMAINID_INDEX ON CODESHELF." + inTableName + " (DOMAINID)");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChildTableName
	 * @param inForeignKeyColumnName
	 * @param inParentTableName
	 */
	private void linkToParentTable(final String inChildTableName, final String inForeignKeyColumnName, final String inParentTableName) {

		// Add the foreign key constraint.
		execOneSQLCommand("ALTER TABLE CODESHELF." + inChildTableName //
				+ " ADD FOREIGN KEY (" + inForeignKeyColumnName + "_PERSISTENTID)" //
				+ " REFERENCES DATABASE.CODESHELF." + inParentTableName + " (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		// Add the index that makes it efficient to find the child objects from the parent.
		execOneSQLCommand("CREATE INDEX CODESHELF." //
				+ inChildTableName + "_" + inForeignKeyColumnName + "_" + inParentTableName //
				+ " ON CODESHELF." + inChildTableName + " (" + inForeignKeyColumnName + "_PERSISTENTID)");

	}

	// --------------------------------------------------------------------------
	/**
	 * Create the initial structures for the DB as of DATABASE_VERSION_CUR.
	 */
	private void createStructures() {

		// DBProperty
		createTable("DBPROPERTY", //
			"VALUESTR VARCHAR(256)" //
		);

		// Organization
		createTable("ORGANIZATION", //
			"DESCRIPTION VARCHAR(64) NOT NULL " //
		);

		// PersistentProperty
		createTable("PERSISTENTPROPERTY", //
			"CURRENTVALUESTR VARCHAR(256)," //
					+ "DEFAULTVALUESTR VARCHAR(256)," //
					+ "PARENT_PERSISTENTID LONG NOT NULL" //
		);

		linkToParentTable("PERSISTENTPROPERTY", "PARENT", "ORGANIZATION");

		// Location
		createTable("LOCATION", //
			"DTYPE VARCHAR(64) NOT NULL," //
					+ "POSTYPE VARCHAR(64) NOT NULL, " //
					+ "POSX DOUBLE NOT NULL, " //
					+ "POSY DOUBLE NOT NULL, " //
					+ "POSZ DOUBLE, " // NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64), "// NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL, " //
					+ "PARENTORGANIZATION_PERSISTENTID LONG "// NOT NULL, " //
		);

		linkToParentTable("LOCATION", "PARENTORGANIZATION", "ORGANIZATION");
		linkToParentTable("LOCATION", "PARENT", "LOCATION");

		// Vertex
		createTable("VERTEX", //
			"POSTYPE VARCHAR(64) NOT NULL, " //
					+ "POSX DOUBLE NOT NULL, " //
					+ "POSY DOUBLE NOT NULL, " //
					+ "DRAWORDER INT NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("VERTEX", "PARENT", "LOCATION");

		// Path
		createTable("PATH", //
			"DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("PATH", "PARENT", "LOCATION");

		// PathSegment
		createTable("PATHSEGMENT", //
			"DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("PATHSEGMENT", "PARENT", "PATH");

		// User
		createTable("USER", //
			"HASHEDPASSWORD VARCHAR(64), " //
					+ "EMAIL VARCHAR(64), " //
					+ "CREATED TIMESTAMP, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("USER", "PARENT", "ORGANIZATION");

		// UserSession
		createTable("USERSESSION", //
			"ACTIVITY VARCHAR(64) NOT NULL, " //
					+ "CREATED TIMESTAMP NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("USERSESSION", "PARENT", "USER");

		// CodeShelfNetwork
		createTable("CODESHELFNETWORK", //
			"NETWORKID BINARY(2) DEFAULT 0 NOT NULL," //
					+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "GATEWAYADDR BINARY(3) NOT NULL, " //
					+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("CODESHELFNETWORK", "PARENT", "LOCATION");

		// ControlGroup
		createTable("CONTROLGROUP", //
			"CONTROLGROUPID BINARY(2) DEFAULT 0 NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "INTERFACEPORTNUM INT NOT NULL, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL," //
					+ "TAGPROTOCOLENUM VARCHAR(16) NOT NULL, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("CONTROLGROUP", "PARENT", "CODESHELFNETWORK");

		// WirelessDevice (includes the subclass variants in one table)
		createTable("WIRELESSDEVICE", //
			"DTYPE VARCHAR(64) NOT NULL," //
					+ "MACADDRESS BINARY(8) DEFAULT 0 NOT NULL," //
					+ "PUBLICKEY VARCHAR(16) NOT NULL," //
					+ "DESCRIPTION VARCHAR(64)," //
					+ "LASTBATTERYLEVEL SMALLINT DEFAULT 0 NOT NULL," //
					+ "NETWORKADDRESS BINARY(3) DEFAULT 0 NOT NULL," //
					+ "NETWORKDEVICESTATUS VARCHAR(16) DEFAULT 'INVALID'," //
					+ "SERIALBUSPOSITION INT DEFAULT 0," //
					+ "LASTCONTACTTIME BIGINT DEFAULT 0," //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("WIRELESSDEVICE", "PARENT", "CONTROLGROUP");

		// One extra wireless device index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_MACADDRESS_INDEX ON CODESHELF.WIRELESSDEVICE (MACADDRESS)");

		// EdiService
		createTable("EDISERVICE", //
			"DTYPE VARCHAR(64) NOT NULL," //
					+ "PROVIDERENUM VARCHAR(16) NOT NULL, " //
					+ "SERVICESTATEENUM VARCHAR(16) NOT NULL, " //
					+ "PROVIDERCREDENTIALS VARCHAR(256), " //
					+ "CURSOR VARCHAR(256)," //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("EDISERVICE", "PARENT", "LOCATION");

		// EdiDocumentLocator
		createTable("EDIDOCUMENTLOCATOR", //
			"DOCUMENTID VARCHAR(64) NOT NULL," //
					+ "DOCUMENTPATH VARCHAR(256) NOT NULL," //
					+ "DOCUMENTNAME VARCHAR(256) NOT NULL," //
					+ "DOCUMENTSTATEENUM VARCHAR(16) NOT NULL, " //
					+ "RECEIVED TIMESTAMP, " //
					+ "PROCESSED TIMESTAMP, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("EDIDOCUMENTLOCATOR", "PARENT", "EDISERVICE");

		// OrderHeader
		createTable("ORDERHEADER", //
			"ORDERID VARCHAR(64)," //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("ORDERHEADER", "PARENT", "LOCATION");

		// One extra wireless device index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.FACILITY_ORDERID_INDEX ON CODESHELF.ORDERHEADER (PARENT_PERSISTENTID, ORDERID)");

		// OrderDetail
		createTable("ORDERDETAIL", //
			"ORDERID VARCHAR(64) NOT NULL," //
					+ "DETAILID VARCHAR(64) NOT NULL," //
					+ "SKU VARCHAR(64) NOT NULL," //
					+ "DESCRIPTION VARCHAR(256) NOT NULL," //
					+ "QUANTITY INTEGER NOT NULL, " //
					+ "UOM VARCHAR(16) NOT NULL," //
					+ "ORDERDATE TIMESTAMP, " //
					+ "PARENT_PERSISTENTID LONG NOT NULL " //
		);

		linkToParentTable("ORDERDETAIL", "PARENT", "ORDERHEADER");
	}
}
