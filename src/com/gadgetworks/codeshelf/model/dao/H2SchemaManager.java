/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: H2SchemaManager.java,v 1.56 2012/11/03 03:24:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class H2SchemaManager implements ISchemaManager {

	private static final Log	LOGGER	= LogFactory.getLog(H2SchemaManager.class);

	private IUtil				mUtil;

	@Inject
	public H2SchemaManager(final IUtil inUtil) {
		mUtil = inUtil;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#verifySchema()
	 */
	public boolean verifySchema() {
		boolean result = false;

		if (!doesSchemaExist()) {
			if (!creatNewSchema()) {
				LOGGER.error("Cannot create DB schema");
				mUtil.exitSystem();
			} else {
				result = true;
			}
		} else {
			try {
				Class.forName("org.h2.Driver");
				Connection connection = DriverManager.getConnection(mUtil.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

				// Try to switch to the proper schema.
				Statement stmt = connection.createStatement();
				ResultSet resultSet = stmt.executeQuery("SELECT VERSION FROM " + ISchemaManager.DATABASE_SCHEMA_NAME + ".DBPROPERTY");

				if (!resultSet.next()) {
					LOGGER.error("Cannot create DB schema");
					mUtil.exitSystem();
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
			} catch (ClassNotFoundException e) {
				LOGGER.error("", e);
			} catch (SQLException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISchemaManager#doesSchemaExist()
	 */
	private boolean doesSchemaExist() {
		boolean result = false;

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mUtil.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

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

	// --------------------------------------------------------------------------
	/**
	 * @param version
	 * @return
	 */
	private boolean setSchemaVersion(Integer inVersion) {
		boolean result = false;

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mUtil.getApplicationInitDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("INSERT INTO " + ISchemaManager.DATABASE_SCHEMA_NAME + ".DBPROPERTY SET VERSION = " + inVersion + ", MODTIME = '" + new Timestamp(System.currentTimeMillis())
					+ "'");
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
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mUtil.getApplicationInitDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE " + ISchemaManager.DATABASE_SCHEMA_NAME + ".DBPROPERTY SET VERSION = " + inVersion + ", MODTIME = '" + new Timestamp(System.currentTimeMillis()) + "'");
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
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mUtil.getApplicationInitDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE SCHEMA " + ISchemaManager.DATABASE_SCHEMA_NAME);
			stmt.close();

			// Try to switch to the proper schema.
			stmt = connection.createStatement();
			stmt.executeUpdate("set SCHEMA " + ISchemaManager.DATABASE_SCHEMA_NAME);
			stmt.close();

			// If we get here then we were able to switch to the schema and it exists.
			result = createTables();
			result &= createIndicies();
			connection.close();

			result &= setSchemaVersion(ISchemaManager.DATABASE_VERSION_CUR);

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
	private boolean upgradeSchema(int inOldVersion, int inNewVersion) {

		boolean result = true;

		// IMPORTANT:
		// Apply these upgrades in version order.

		// First get rid of the eBean dictionary file, so that the internal schema dictionary gets rebuilt.
		File dictFile = new File(mUtil.getApplicationLogDirPath() + System.getProperty("file.separator") + ".ebean.h2.dictionary");
		if (dictFile.exists()) {
			try {
				dictFile.delete();
			} catch (SecurityException e) {
				LOGGER.error("", e);
			}
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_1) {
			result &= doUpdate1();
		}

		result &= updateSchemaVersion(ISchemaManager.DATABASE_VERSION_CUR);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpdate1() {
		boolean result = true;

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
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mUtil.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(inSQLCommand);
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
	 * Create a standard DomainObject table with all the appropriate boilerplate and then add the stuff for the particular domain class.
	 * @param inTableName
	 * @param inColumns
	 */
	private boolean createOrganizationTable(final String inColumns) {

		boolean result = true;

		result &= execOneSQLCommand("CREATE SEQUENCE CODESHELF.ORGANIZATION_SEQ");
		result &= execOneSQLCommand("CREATE TABLE CODESHELF.ORGANIZATION (" //
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL, " //
				//				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (PERSISTENTID));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.ORGANIZATION_DOMAINID_INDEX ON CODESHELF.ORGANIZATION (DOMAINID)");

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
				+ "PERSISTENTID BIGINT NOT NULL, " //
				+ "PARENT_PERSISTENTID BIGINT NOT NULL, " //
				+ "DOMAINID VARCHAR(64) NOT NULL, " //
				//				+ "LASTDEFAULTSEQUENCEID INT NOT NULL, " //
				+ "VERSION TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (PERSISTENTID));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF." + inTableName + "_DOMAINID_INDEX ON CODESHELF." + inTableName + " (PARENT_PERSISTENTID, DOMAINID)");

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
		result &= execOneSQLCommand("CREATE INDEX CODESHELF." //
				+ inChildTableName + "_" + inForeignKeyColumnName + "_" + inParentTableName //
				+ " ON CODESHELF." + inChildTableName + " (" + inForeignKeyColumnName + "_PERSISTENTID)");

		return result;

	}

	private boolean createIndicies() {
		boolean result = true;

		result &= linkToParentTable("CODESHELFNETWORK", "PARENT", "LOCATION");

		result &= linkToParentTable("CONTAINER", "PARENT", "CONTAINERKIND");

		result &= linkToParentTable("CONTAINERKIND", "PARENT", "LOCATION");

		result &= linkToParentTable("CONTAINERUSE", "PARENT", "CONTAINER");

		result &= linkToParentTable("CONTROLGROUP", "PARENT", "CODESHELFNETWORK");

		result &= linkToParentTable("EDIDOCUMENTLOCATOR", "PARENT", "EDISERVICE");

		result &= linkToParentTable("EDISERVICE", "PARENT", "LOCATION");

		result &= linkToParentTable("ITEM", "PARENT", "ITEMMASTER");

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
		execOneSQLCommand("CREATE UNIQUE INDEX CODESHELF.WIRELESSDEVICE_MACADDRESS_INDEX ON CODESHELF.WIRELESSDEVICE (MACADDRESS)");

		result &= linkToParentTable("WORKAREA", "PARENT", "LOCATION");

		result &= linkToParentTable("WORKINSTRUCTION", "PARENT", "LOCATION");

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
			"SERIALIZEDID BINARY(2) DEFAULT 0 NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64) NOT NULL, " //
					+ "GATEWAYADDR BINARY(3) NOT NULL, " //
					+ "GATEWAYURL VARCHAR(64) NOT NULL, " //
					+ "ACTIVE BOOLEAN DEFAULT TRUE NOT NULL " //
		);

		// Container
		result &= createTable("CONTAINER", //
			"KIND_PERSISTENTID BIGINT NOT NULL " //
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
			"SERIALIZEDID BINARY(2) DEFAULT 0 NOT NULL, " //
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
					+ "ITEMMASTER_PERSISTENTID BIGINT NOT NULL, " //
					+ "UOMMASTER_PERSISTENTID BIGINT NOT NULL " //
		);

		// ItemMaster
		result &= createTable("ITEMMASTER", //
			"DESCRIPTION VARCHAR(256), " //
					+ "LOTHANDLINGENUM VARCHAR(16) NOT NULL, " //
					+ "STANDARDUOM_PERSISTENTID BIGINT NOT NULL " //
		);

		// Location
		result &= createTable("LOCATION", //
			"DTYPE VARCHAR(64) NOT NULL, " //
					+ "POSTYPE VARCHAR(64) NOT NULL, " //
					+ "POSX DOUBLE NOT NULL, " //
					+ "POSY DOUBLE NOT NULL, " //
					+ "POSZ DOUBLE, " // NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64), "// NOT NULL, " //
					+ "PARENTORGANIZATION_PERSISTENTID BIGINT "// NOT NULL, " //
		);

		// OrderDetail
		result &= createTable("ORDERDETAIL", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "ITEMMASTER_PERSISTENTID BIGINT NOT NULL, " //
					+ "DESCRIPTION VARCHAR(256) NOT NULL, " //
					+ "QUANTITY INTEGER NOT NULL, " //
					+ "UOMMASTER_PERSISTENTID BIGINT NOT NULL, " //
					+ "ORDERDATE TIMESTAMP " //
		);

		// OrderGroup
		result &= createTable("ORDERGROUP", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "WORKSEQUENCE LONG, " //
					+ "DESCRIPTION VARCHAR(256) " //
		);

		// OrderHeader
		result &= createTable("ORDERHEADER", //
			"STATUSENUM VARCHAR(16) NOT NULL, " //
					+ "PICKSTRATEGYENUM VARCHAR(16) NOT NULL, " //
					+ "ORDERGROUP_PERSISTENTID BIGINT, " //
					+ "WORKSEQUENCE LONG " //
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
			"ASSOCIATEDLOCATION_PERSISTENTID BIGINT NOT NULL, " //
					+ "HEADPOSTYPEENUM VARCHAR(16) NOT NULL, " //
					+ "HEADPOSX DOUBLE NOT NULL, " //
					+ "HEADPOSY DOUBLE NOT NULL, " //
					+ "TAILPOSTYPEENUM VARCHAR(16) NOT NULL, " //
					+ "TAILPOSX DOUBLE NOT NULL, " //
					+ "TAILPOSY DOUBLE NOT NULL " //
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
			"HASHEDPASSWORD VARCHAR(64), " //
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
					+ "POSX DOUBLE NOT NULL, " //
					+ "POSY DOUBLE NOT NULL, " //
					+ "POSZ DOUBLE, " //
					+ "DRAWORDER INT NOT NULL " //
		);

		// WirelessDevice (includes the subclass variants in one table)
		result &= createTable("WIRELESSDEVICE", //
			"DTYPE VARCHAR(64) NOT NULL, " //
					+ "MACADDRESS BINARY(8) DEFAULT 0 NOT NULL, " //
					+ "PUBLICKEY VARCHAR(16) NOT NULL, " //
					+ "DESCRIPTION VARCHAR(64), " //
					+ "LASTBATTERYLEVEL SMALLINT DEFAULT 0 NOT NULL, " //
					+ "NETWORKADDRESS BINARY(3) DEFAULT 0 NOT NULL, " //
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
					+ "SUBJECTCONTAINER_PERSISTENTID BIGINT NOT NULL, " //
					+ "SUBJECTITEM_PERSISTENTID BIGINT NOT NULL, " //
					+ "FROMLOCATION_PERSISTENTID BIGINT NOT NULL, " //
					+ "TOLOCATION_PERSISTENTID BIGINT NOT NULL, " //
					+ "FROMCONTAINER_PERSISTENTID BIGINT NOT NULL, " //
					+ "TOCONTAIENR_PERSISTENTID BIGINT NOT NULL " //
		);

		return result;
	}
}
