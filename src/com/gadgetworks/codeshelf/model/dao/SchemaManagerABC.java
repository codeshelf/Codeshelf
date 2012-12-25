/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SchemaManagerABC.java,v 1.6 2012/12/25 10:48:14 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import lombok.AccessLevel;
import lombok.Getter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class SchemaManagerABC implements ISchemaManager {

	private static final Log	LOGGER		= LogFactory.getLog(SchemaManagerABC.class);

	private static final String	UUID_TYPE	= "CHAR(36)";

	@Getter(value = AccessLevel.PROTECTED)
	private IUtil				util;
	@Getter
	private String				dbUserId;
	@Getter
	private String				dbPassword;
	@Getter
	private String				dbName;
	@Getter
	private String				dbSchemaName;
	@Getter
	private String				dbAddress;
	@Getter
	private String				dbPortnum;

	@Inject
	public SchemaManagerABC(final IUtil inUtil,
		final String inDbUserId,
		final String inDbPassword,
		final String inDbName,
		final String inDbSchemaName,
		final String inDbAddress,
		final String inDbPortnum) {
		util = inUtil;
		dbUserId = inDbUserId;
		dbPassword = inDbPassword;
		dbName = inDbName;
		dbSchemaName = inDbSchemaName;
		dbAddress = inDbAddress;
		dbPortnum = inDbPortnum;
	}

	protected abstract boolean doUpgradeSchema();

	protected abstract boolean doDowngradeSchema();

	protected abstract String getSchemaSetterString();

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#verifySchema()
	 */
	public final boolean verifySchema() {
		boolean result = false;

		if (!doesSchemaExist()) {
			if (!creatNewSchema()) {
				LOGGER.error("Cannot create DB schema");
				util.exitSystem();
			} else {
				result = true;
			}
		} else {
			try {
				Connection connection = getConnection(getApplicationDatabaseURL());

				// Try to switch to the proper schema.
				Statement stmt = connection.createStatement();
				ResultSet resultSet = stmt.executeQuery("SELECT VERSION FROM " + getDbSchemaName() + ".DBPROPERTY");

				if (!resultSet.next()) {
					LOGGER.error("Cannot create DB schema");
					util.exitSystem();
				} else {
					Integer schemaVersion = resultSet.getInt("VERSION");
					if (schemaVersion < ISchemaManager.DATABASE_VERSION_CUR) {
						result = upgradeSchema(schemaVersion, ISchemaManager.DATABASE_VERSION_CUR);
					} else {
						result = true;
					}
				}
				stmt.close();
				connection.close();
			} catch (SQLException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	};

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private Connection getConnection(final String inDbUrl) throws SQLException {
		Connection result = null;

		try {
			Class.forName(getDriverName());

			if ((getDbUserId() == null) || (getDbUserId().length() == 0)) {
				result = DriverManager.getConnection(inDbUrl);
			} else {
				result = DriverManager.getConnection(inDbUrl, getDbUserId(), getDbPassword());
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#doesSchemaExist()
	 */
	private boolean doesSchemaExist() {
		boolean result = false;

		try {
			Connection connection = getConnection(getApplicationDatabaseURL());

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(getSchemaSetterString());
			stmt.close();
			// If we get here then we were able to switch to the schema and it exists.
			result = true;

			connection.close();
		} catch (SQLException e) {
			LOGGER.error("", e);
		}
		return result;
	};

	// --------------------------------------------------------------------------
	/**
	 * @param version
	 * @return
	 */
	private boolean setSchemaVersion(Integer inVersion) {
		boolean result = false;

		try {
			Class.forName(getDriverName());
			Connection connection = getConnection(getApplicationDatabaseURL());

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("INSERT INTO " + getDbSchemaName() + ".DBPROPERTY (VERSION, MODTIME) VALUES (" + inVersion + ",'" + new Timestamp(System.currentTimeMillis()) + "')");
			stmt.close();
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
	 * @param version
	 * @return
	 */
	private boolean updateSchemaVersion(Integer inVersion) {
		boolean result = false;

		try {
			Class.forName(getDriverName());
			Connection connection = getConnection(getApplicationDatabaseURL());

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE " + getDbSchemaName() + ".DBPROPERTY SET VERSION = " + inVersion + ", MODTIME = '" + new Timestamp(System.currentTimeMillis()) + "'");
			stmt.close();
			connection.close();

			result = true;

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SQLException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#creatNewSchema()
	 */
	private boolean creatNewSchema() {
		boolean result = false;
		try {
			Connection connection = getConnection(getApplicationInitDatabaseURL());

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE SCHEMA " + getDbSchemaName());
			stmt.close();

			// Try to switch to the proper schema.
			stmt = connection.createStatement();
			stmt.executeUpdate(getSchemaSetterString());
			stmt.close();

			// If we get here then we were able to switch to the schema and it exists.
			result = createTables();
			result &= createIndicies();
			connection.close();

			result &= setSchemaVersion(ISchemaManager.DATABASE_VERSION_CUR);

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
	private boolean upgradeSchema(int inOldVersion, int inNewVersion) {

		boolean result = true;

		doUpgradeSchema();

		// IMPORTANT:
		// Apply these upgrades in version order.
		if (inOldVersion < ISchemaManager.DATABASE_VERSION_2) {
			result &= doUpgrade2();
		}

		result &= updateSchemaVersion(ISchemaManager.DATABASE_VERSION_CUR);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade2() {
		boolean result = true;

		result &= safeAddColumn("ORDERHEADER", "CUSTOMERID VARCHAR(64)");
		result &= safeAddColumn("ORDERHEADER", "SHIPMENTID VARCHAR(64)");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inFromSchema
	 *  @param inToSchema
	 */
	private void downgradeSchema(int inOldVersion, int inNewVersion) {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inSQLCommand
	 *  @return
	 */
	private boolean execOneSQLCommand(String inSQLCommand) {
		boolean result = false;

		try {
			Connection connection = getConnection(getApplicationDatabaseURL());

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(inSQLCommand);
			stmt.close();

			connection.close();

			result = true;
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
	private boolean createOrganizationTable(final String inColumns) {

		boolean result = true;

		result &= execOneSQLCommand("CREATE SEQUENCE CODESHELF.ORGANIZATION_SEQ");
		result &= execOneSQLCommand("CREATE TABLE CODESHELF.ORGANIZATION (" //
				+ "PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (PERSISTENTID));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX ORGANIZATION_DOMAINID_INDEX ON CODESHELF.ORGANIZATION (DOMAINID)");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a standard DomainObject table with all the appropriate boilerplate and then add the stuff for the particular domain class.
	 * @param inTableName
	 * @param inColumns
	 */
	private boolean createTable(final String inTableName, final String inColumns) {

		boolean result = true;

		result &= execOneSQLCommand("CREATE SEQUENCE CODESHELF." + inTableName + "_SEQ");
		result &= execOneSQLCommand("CREATE TABLE CODESHELF." + inTableName + " (" //
				+ "PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
				+ "PARENT_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (PERSISTENTID));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX " + inTableName + "_DOMAINID_INDEX ON CODESHELF." + inTableName + " (PARENT_PERSISTENTID, DOMAINID)");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChildTableName
	 * @param inForeignKeyColumnName
	 * @param inParentTableName
	 */
	private boolean linkToParentTable(final String inChildTableName, final String inForeignKeyColumnName, final String inParentTableName) {

		boolean result = true;

		// Add the foreign key constraint.
		result &= execOneSQLCommand("ALTER TABLE CODESHELF." + inChildTableName //
				+ " ADD FOREIGN KEY (" + inForeignKeyColumnName + "_PERSISTENTID)" //
				+ " REFERENCES DATABASE.CODESHELF." + inParentTableName + " (PERSISTENTID)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		// Add the index that makes it efficient to find the child objects from the parent.
		result &= execOneSQLCommand("CREATE INDEX " //
				+ inChildTableName + "_" + inForeignKeyColumnName + "_" + inParentTableName //
				+ " ON CODESHELF." + inChildTableName + " (" + inForeignKeyColumnName + "_PERSISTENTID)");

		return result;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @return
	 */
	private boolean safeAddColumn(final String inTableName, final String inColumnName) {
		boolean result = false;

		result &= execOneSQLCommand("ALTER TABLE CODESHELF." + inTableName //
				+ " ADD " + inColumnName //
				+ ";");

		return result;
	}

	private boolean createIndicies() {
		boolean result = true;

		result &= linkToParentTable("CODESHELFNETWORK", "PARENT", "LOCATION");

		result &= linkToParentTable("CONTAINER", "PARENT", "LOCATION");

		result &= linkToParentTable("CONTAINERKIND", "PARENT", "LOCATION");

		result &= linkToParentTable("CONTAINERUSE", "PARENT", "CONTAINER");

		result &= linkToParentTable("CONTROLGROUP", "PARENT", "CODESHELFNETWORK");

		result &= linkToParentTable("EDIDOCUMENTLOCATOR", "PARENT", "EDISERVICE");

		result &= linkToParentTable("EDISERVICE", "PARENT", "LOCATION");

		result &= linkToParentTable("ITEM", "PARENT", "LOCATION");

		result &= linkToParentTable("ITEMMASTER", "PARENT", "LOCATION");

		result &= linkToParentTable("LOCATION", "PARENTORGANIZATION", "ORGANIZATION");
		result &= linkToParentTable("LOCATION", "PARENT", "LOCATION");

		result &= linkToParentTable("ORDERDETAIL", "PARENT", "ORDERHEADER");

		result &= linkToParentTable("ORDERGROUP", "PARENT", "LOCATION");

		result &= linkToParentTable("ORDERHEADER", "PARENT", "LOCATION");

		result &= linkToParentTable("PATH", "PARENT", "LOCATION");

		result &= linkToParentTable("PATHSEGMENT", "PARENT", "PATH");

		result &= linkToParentTable("PERSISTENTPROPERTY", "PARENT", "ORGANIZATION");

		result &= linkToParentTable("UOMMASTER", "PARENT", "LOCATION");

		result &= linkToParentTable("USER", "PARENT", "ORGANIZATION");

		result &= linkToParentTable("USERSESSION", "PARENT", "USER");

		result &= linkToParentTable("VERTEX", "PARENT", "LOCATION");

		result &= linkToParentTable("WIRELESSDEVICE", "PARENT", "CONTROLGROUP");
		// One extra wireless device index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE UNIQUE INDEX WIRELESSDEVICE_MACADDRESS_INDEX ON CODESHELF.WIRELESSDEVICE (MACADDRESS)");

		result &= linkToParentTable("WORKAREA", "PARENT", "PATH");

		result &= linkToParentTable("WORKINSTRUCTION", "PARENT", "WORKAREA");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the initial structures for the DB as of DATABASE_VERSION_CUR.
	 */
	private boolean createTables() {

		boolean result = true;

		// CodeShelfNetwork
		result &= createTable("CODESHELFNETWORK", //
			"SERIALIZEDID BYTEA DEFAULT '' NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "GATEWAYADDR BYTEA DEFAULT '' NOT NULL, " //
					+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL " //
		);

		// Container
		result &= createTable("CONTAINER", //
			"KIND_PERSISTENTID " + UUID_TYPE + " NOT NULL " //
		);

		// ContainerKind
		result &= createTable("CONTAINERKIND", //
			"CLASSID VARCHAR(64) NOT NULL, " //
					+ "LENGTHMETERS DECIMAL NOT NULL, " //
					+ "HEIGHTMETERS DECIMAL NOT NULL, " //
					+ "WIDTHMETERS DECIMAL NOT NULL " //
		);

		// ContainerUse
		result &= createTable("CONTAINERUSE", //
			"USETIMESTAMP TIMESTAMP NOT NULL " //
		);

		// ControlGroup
		result &= createTable("CONTROLGROUP", //
			"SERIALIZEDID BYTEA DEFAULT '' NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "INTERFACEPORTNUM INT NOT NULL, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "TAGPROTOCOLENUM VARCHAR(16) NOT NULL " //
		);

		// DBProperty
		result &= execOneSQLCommand("CREATE TABLE CODESHELF.DBPROPERTY (" //
				+ "VERSION INTEGER, " //
				+ "MODTIME TIMESTAMP);");

		// EdiDocumentLocator
		result &= createTable("EDIDOCUMENTLOCATOR", //
			"DOCUMENTPATH VARCHAR(256) NOT NULL, " //
					+ "DOCUMENTNAME VARCHAR(256) NOT NULL, " //
					+ "DOCUMENTSTATEENUM VARCHAR(16) NOT NULL, " //
					+ "RECEIVED TIMESTAMP, " //
					+ "PROCESSED TIMESTAMP " //
		);

		// EdiService
		result &= createTable("EDISERVICE", //
			"DTYPE VARCHAR(64) NOT NULL, " //
					+ "PROVIDERENUM VARCHAR(16) NOT NULL, " //
					+ "SERVICESTATEENUM VARCHAR(16) NOT NULL, " //
					+ "PROVIDERCREDENTIALS VARCHAR(256), " //
					+ "CURSOR VARCHAR(256) " //
		);

		// Item
		result &= createTable("ITEM", //
			"QUANTITY DECIMAL NOT NULL, " //
					+ "ITEMMASTER_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "UOMMASTER_PERSISTENTID " + UUID_TYPE + " NOT NULL " //
		);

		// ItemMaster
		result &= createTable("ITEMMASTER", //
			"DESCRIPTION VARCHAR(256), " //
					+ "LOTHANDLINGENUM VARCHAR(16) NOT NULL, " //
					+ "STANDARDUOM_PERSISTENTID " + UUID_TYPE + " NOT NULL " //
		);

		// Location
		result &= createTable("LOCATION", //
			"DTYPE VARCHAR(64) NOT NULL, " //
					+ "POSTYPE VARCHAR(64) NOT NULL, " //
					+ "POSX DOUBLE PRECISION NOT NULL, " //
					+ "POSY DOUBLE PRECISION NOT NULL, " //
					+ "POSZ DOUBLE PRECISION, " // NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64), "// NOT NULL, " //
					+ "PARENTORGANIZATION_PERSISTENTID " + UUID_TYPE + " "// NOT NULL, " //
		);

		// OrderDetail
		result &= createTable("ORDERDETAIL", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "ITEMMASTER_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "DESCRIPTION VARCHAR(256) NOT NULL, " //
					+ "QUANTITY INTEGER NOT NULL, " //
					+ "UOMMASTER_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "ORDERDATE TIMESTAMP " //
		);

		// OrderGroup
		result &= createTable("ORDERGROUP", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "WORKSEQUENCE " + UUID_TYPE + ", " //
					+ "DESCRIPTION VARCHAR(256) " //
		);

		// OrderHeader
		result &= createTable("ORDERHEADER", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "PICKSTRATEGYENUM VARCHAR(16) NOT NULL, " //
					+ "ORDERGROUP_PERSISTENTID " + UUID_TYPE + ", " //
					+ "WORKSEQUENCE " + UUID_TYPE + ", " //
					+ "SHIPMENTID VARCHAR(64), " //
					+ "CUSTOMERID VARCHAR(64) " //
		);

		// Organization - this is the top-level object that owns all other objects.
		result &= createOrganizationTable( //
		"DESCRIPTION VARCHAR(64) NOT NULL " //
		);

		// Path
		result &= createTable("PATH", //
			"DESCRIPTION VARCHAR(64) NOT NULL " //
		);

		// PathSegment
		result &= createTable("PATHSEGMENT", //
			"ASSOCIATEDLOCATION_PERSISTENTID " + UUID_TYPE + ", " //
					+ "DIRECTIONENUM VARCHAR(16) NOT NULL, " //
					+ "SEGMENTORDER INTEGER NOT NULL, " //
					+ "HEADPOSTYPEENUM VARCHAR(16) NOT NULL, " //
					+ "HEADPOSX DOUBLE PRECISION NOT NULL, " //
					+ "HEADPOSY DOUBLE PRECISION NOT NULL, " //
					+ "TAILPOSTYPEENUM VARCHAR(16) NOT NULL, " //
					+ "TAILPOSX DOUBLE PRECISION NOT NULL, " //
					+ "TAILPOSY DOUBLE PRECISION NOT NULL " //
		);

		// PersistentProperty
		result &= createTable("PERSISTENTPROPERTY", //
			"CURRENTVALUESTR VARCHAR(256), " //
					+ "DEFAULTVALUESTR VARCHAR(256) " //
		);

		// UomMaster
		result &= createTable("UOMMASTER", //
			"DESCRIPTION VARCHAR(256) " //
		);

		// User
		result &= createTable("USER", //
			"HASHSALT VARCHAR(64), " //
					+ "HASHEDPASSWORD VARCHAR(64), " //
					+ "HASHITERATIONS INTEGER, " //
					+ "EMAIL VARCHAR(64), " //
					+ "CREATED TIMESTAMP, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL " //
		);

		// UserSession
		result &= createTable("USERSESSION", //
			"ACTIVITY VARCHAR(64) NOT NULL, " //
					+ "CREATED TIMESTAMP NOT NULL " //
		);

		// Vertex
		result &= createTable("VERTEX", //
			"POSTYPEENUM VARCHAR(16) NOT NULL, " //
					+ "POSX DOUBLE PRECISION NOT NULL, " //
					+ "POSY DOUBLE PRECISION NOT NULL, " //
					+ "POSZ DOUBLE PRECISION, " //
					+ "DRAWORDER INT NOT NULL " //
		);

		// WirelessDevice (includes the subclass variants in one table)
		result &= createTable("WIRELESSDEVICE", //
			"DTYPE VARCHAR(64) NOT NULL, " //
					+ "MACADDRESS BYTEA DEFAULT '' NOT NULL, " //
					+ "PUBLICKEY VARCHAR(16) NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64), " //
					+ "LASTBATTERYLEVEL SMALLINT DEFAULT 0 NOT NULL, " //
					+ "NETWORKADDRESS BYTEA DEFAULT '' NOT NULL, " //
					+ "NETWORKDEVICESTATUS VARCHAR(16) DEFAULT 'INVALID', " //
					+ "SERIALBUSPOSITION INT DEFAULT 0, " //
					+ "LASTCONTACTTIME BIGINT DEFAULT 0 " //
		);

		// WorkArea
		result &= createTable("WORKAREA", //
			"WORKAREAID VARCHAR(64) NOT NULL, " //
					+ "DESCRIPTION VARCHAR(256) NOT NULL " //
		);

		// WorkInstruction
		result &= createTable("WORKINSTRUCTION", //
			"OPENUM VARCHAR(16) NOT NULL, " //
					+ "PLANENUM VARCHAR(16) NOT NULL, " //
					+ "STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "SUBJECTCONTAINER_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "SUBJECTITEM_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "FROMLOCATION_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "TOLOCATION_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "FROMCONTAINER_PERSISTENTID " + UUID_TYPE + " NOT NULL, " //
					+ "TOCONTAIENR_PERSISTENTID " + UUID_TYPE + " NOT NULL " //
		);

		return result;
	}
}
