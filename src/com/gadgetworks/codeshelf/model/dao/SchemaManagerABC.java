/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SchemaManagerABC.java,v 1.40 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@Singleton
public abstract class SchemaManagerABC implements ISchemaManager {

	private static final Logger	LOGGER			= LoggerFactory.getLogger(SchemaManagerABC.class);

	// CHAR36 is the official byte size of a UUID.
	private static final String	UUID_TYPE		= "CHAR(36)";
	// CHAR for a key (which domain ID is) is 20% faster than VARCHAR, but it has tons of ramifications for string compares.
	// Might be worth a change later, but it could be painful to maintain it on-going.
	private static final String	DOMAINID_TYPE	= "TEXT";

	@Getter(value = AccessLevel.PROTECTED)
	private final IUtil			util;
	@Getter
	private final String		dbUserId;
	@Getter
	private final String		dbPassword;
	@Getter
	private final String		dbName;
	@Getter
	private final String		dbSchemaName;
	@Getter
	private final String		dbAddress;
	@Getter
	private final String		dbPortnum;
	@Getter
	private final String		dbSsl;

	@Inject
	public SchemaManagerABC(final IUtil inUtil,
		final String inDbUserId,
		final String inDbPassword,
		final String inDbName,
		final String inDbSchemaName,
		final String inDbAddress,
		final String inDbPortnum,
		final String inSsl) {
		util = inUtil;
		dbUserId = inDbUserId;
		dbPassword = inDbPassword;
		dbName = inDbName;
		dbSchemaName = inDbSchemaName;
		dbAddress = inDbAddress;
		dbPortnum = inDbPortnum;
		dbSsl = inSsl;
	}

	protected abstract boolean doUpgradeSchema();

	protected abstract boolean doDowngradeSchema();

	protected abstract String getSchemaCheckerString();

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
				stmt.executeUpdate(getSchemaSetterString());
				stmt.close();

				// Get the schema version.
				stmt = connection.createStatement();
				ResultSet resultSet = stmt.executeQuery("SELECT version FROM " + getDbSchemaName() + ".db_property");

				if (!resultSet.next()) {
					LOGGER.error("Cannot create DB schema");
					util.exitSystem();
				} else {
					Integer schemaVersion = resultSet.getInt("version");
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
				Properties props = new Properties();
				props.setProperty("user", getDbUserId());
				props.setProperty("password", getDbPassword());
				if ((getDbSsl() != null) && (getDbSsl().compareToIgnoreCase("true") == 0)) {
					props.setProperty("ssl", "true");
				}

				result = DriverManager.getConnection(inDbUrl, props);
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
			// If the SQL command doesn't cause an exception then the schema exists.
			Statement stmt = connection.createStatement();
			if (stmt.execute(getSchemaCheckerString())) {
				ResultSet resultSet = stmt.getResultSet();
				resultSet.next();
				// If we get here then we were able to switch to the schema and it exists.
				result = resultSet.getBoolean(1);
			} else {
				int num = stmt.getUpdateCount();
				LOGGER.debug("Num: " + num);

				// If we get here then we were able to switch to the schema and it exists.
				result = true;
			}
			stmt.close();

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
			stmt.executeUpdate("INSERT INTO " + getDbSchemaName() + ".db_property (version, modified) VALUES (" + inVersion + ",'"
					+ new Timestamp(System.currentTimeMillis()) + "')");
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
			stmt.executeUpdate("UPDATE " + getDbSchemaName() + ".db_property SET version = " + inVersion + ", modified = '"
					+ new Timestamp(System.currentTimeMillis()) + "'");
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
			result &= createIndexes();
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

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_3) {
			result &= doUpgrade3();
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_4) {
			result &= doUpgrade4();
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_5) {
			result &= doUpgrade5();
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_6) {
			result &= doUpgrade6();
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_7) {
			result &= doUpgrade7();
		}

		if (inOldVersion < ISchemaManager.DATABASE_VERSION_8) {
			result &= doUpgrade8();
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

		result &= safeAddColumn("order_header", "custoemr_id TEXT");
		result &= safeAddColumn("order_header", "shipment_id TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade3() {
		boolean result = true;

		result &= safeAddColumn("item_master", "slot_flex_id TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade4() {
		boolean result = true;

		result &= safeAddColumn("work_instruction", "led_cmd_stream TEXT");
		result &= safeAddColumn("work_instruction", "group_and_sort_code TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade5() {
		boolean result = true;

		// LocationAlias
		result &= createTable("location_alias", //
			"mapped_location_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL " //
		);

		result &= linkToParentTable("location_alias", "parent", "location" /* facility */);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade6() {
		boolean result = true;

		// OrderLocation
		result &= createTable("order_location", //
			"location_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL " //
		);

		result &= linkToParentTable("order_location", "parent", "order_header");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade7() {
		boolean result = true;

		result &= safeAddColumn("order_header", "order_type_enum TEXT DEFAULT 'PICK' NOT NULL");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade8() {
		boolean result = true;

		result &= safeModifyColumnType("order_detail", "quantity", "INTEGER");
		result &= safeModifyColumnType("work_instruction", "plan_quantity", "INTEGER");
		result &= safeModifyColumnType("work_instruction", "actual_quantity", "INTEGER");

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

		result &= execOneSQLCommand("CREATE SEQUENCE " + getDbSchemaName() + ".organization_seq");
		result &= execOneSQLCommand("CREATE TABLE " + getDbSchemaName() + ".organization (" //
				+ "persistentid " + UUID_TYPE + " NOT NULL, " //
				+ "domainid VARCHAR(255) NOT NULL, " //
				+ "version TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (persistentid));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX organization_domainid_index ON " + getDbSchemaName()
				+ ".organization (domainid)");

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

		//result &= execOneSQLCommand("CREATE SEQUENCE " + getDbSchemaName() + "." + inTableName + "_seq");
		result &= execOneSQLCommand("CREATE TABLE " + getDbSchemaName() + "." + inTableName + " (" //
				+ "persistentid " + UUID_TYPE + " NOT NULL, " //
				+ "parent_persistentid " + UUID_TYPE + " NOT NULL, " //
				+ "domainid " + DOMAINID_TYPE + " NOT NULL, " //
				+ "version TIMESTAMP, " //
				+ inColumns //
				+ ", PRIMARY KEY (persistentid));");

		result &= execOneSQLCommand("CREATE UNIQUE INDEX " + inTableName + "_domainid_index ON " + getDbSchemaName() + "."
				+ inTableName + " (parent_persistentid, domainid)");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChildTableName
	 * @param inForeignKeyColumnName
	 * @param inParentTableName
	 */
	private boolean linkToParentTable(final String inChildTableName,
		final String inForeignKeyColumnName,
		final String inParentTableName) {

		boolean result = true;

		// Add the foreign key constraint.
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inChildTableName //
				+ " ADD FOREIGN KEY (" + inForeignKeyColumnName + "_persistentid)" //
				+ " REFERENCES DATABASE." + getDbSchemaName() + "." + inParentTableName + " (persistentid)" //
				+ " ON DELETE RESTRICT ON UPDATE RESTRICT;");

		// Add the index that makes it efficient to find the child objects from the parent.
		result &= execOneSQLCommand("CREATE INDEX " //
				+ inChildTableName + "_" + inForeignKeyColumnName + "_" + inParentTableName //
				+ " ON " + getDbSchemaName() + "." + inChildTableName + " (" + inForeignKeyColumnName + "_persistentid)");

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

		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " ADD " + inColumnName //
				+ ";");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @return
	 */
	private boolean safeModifyColumnType(final String inTableName, final String inColumnName, final String inNewColumnType) {
		boolean result = false;

		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " ALTER COLUMN " + inColumnName //
				+ " TYPE " + inNewColumnType //
				+ ";");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create all of the indexes needed to maintain the systems referential integrity and performance.
	 * @return
	 */
	private boolean createIndexes() {
		boolean result = true;

		result &= linkToParentTable("che", "parent", "codeshelf_network");
		result &= linkToParentTable("che", "current_work_area", "work_area");
		result &= linkToParentTable("che", "current_user", "user");
		// One extra index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE UNIQUE INDEX che_deviceguid_index ON " + getDbSchemaName()
				+ ".che (device_guid, parent_persistentid)");

		result &= linkToParentTable("codeshelf_network", "parent", "location");

		result &= linkToParentTable("container", "parent", "location");

		result &= linkToParentTable("container_kind", "parent", "location");

		result &= linkToParentTable("container_use", "parent", "container");
		result &= linkToParentTable("container_use", "order_header", "order_header");
		result &= linkToParentTable("container_use", "current_che", "che");

		result &= linkToParentTable("edi_document_locator", "parent", "edi_service");

		result &= linkToParentTable("edi_service", "parent", "location");

		result &= linkToParentTable("item", "parent", "item_master");
		// One extra index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE INDEX item_stored_location_persistentid_index ON " + getDbSchemaName()
				+ ".item (stored_location_persistentid)");

		result &= linkToParentTable("item_ddc_group", "parent", "location");

		result &= linkToParentTable("item_master", "parent", "location");

		result &= linkToParentTable("led_controller", "parent", "codeshelf_network");
		// One extra index: to ensure uniqueness of the MAC addresses, and to find them fast by that address.
		execOneSQLCommand("CREATE UNIQUE INDEX led_controller_deviceguid_index ON " + getDbSchemaName()
				+ ".led_controller (device_guid, parent_persistentid)");

		result &= linkToParentTable("location", "parent", "location");
		result &= linkToParentTable("location", "path_segment", "path_segment");
		result &= linkToParentTable("location", "parent_organization", "organization");

		result &= linkToParentTable("location_alias", "parent", "location" /* facility */);

		result &= linkToParentTable("order_detail", "parent", "order_header");

		result &= linkToParentTable("order_group", "parent", "location");

		result &= linkToParentTable("order_header", "parent", "location");

		result &= linkToParentTable("order_location", "parent", "order_header");

		result &= linkToParentTable("path", "parent", "location");

		result &= linkToParentTable("path_segment", "parent", "path");
		result &= linkToParentTable("path_segment", "anchor_location", "location");

		result &= linkToParentTable("persistent_property", "parent", "organization");

		result &= linkToParentTable("uom_master", "parent", "location");

		result &= linkToParentTable("user", "parent", "organization");

		result &= linkToParentTable("user_session", "parent", "USER");

		result &= linkToParentTable("vertex", "parent", "location");

		result &= linkToParentTable("work_area", "parent", "PATH");

		result &= linkToParentTable("work_instruction", "parent", "order_detail");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the initial structures for the DB as of DATABASE_VERSION_CUR.
	 */
	private boolean createTables() {

		boolean result = true;

		// Che
		result &= createTable("che", //
			"description VARCHAR(255), " //
					+ "device_guid BYTEA DEFAULT '' NOT NULL, " //
					+ "public_key VARCHAR(255) NOT NULL, " //
					+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
					+ "serial_bus_position INT DEFAULT 0, " //
					+ "current_user_persistentid " + UUID_TYPE + ", " //
					+ "current_work_area_persistentid " + UUID_TYPE //
		);

		// CodeShelfNetwork
		result &= createTable("codeshelf_network", //
			"description VARCHAR(255) NOT NULL, " //
					+ "credential VARCHAR(255) NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL " //
		);

		// Container
		result &= createTable("container", //
			"kind_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL " //
		);

		// ContainerKind
		result &= createTable("container_kind", //
			"class_id VARCHAR(255) NOT NULL, " //
					+ "length_meters DECIMAL NOT NULL, " //
					+ "height_meters DECIMAL NOT NULL, " //
					+ "width_meters DECIMAL NOT NULL " //
		);

		// ContainerUse
		result &= createTable("container_use", //
			"used_on TIMESTAMP NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "order_header_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "current_che_persistentid " + UUID_TYPE //
		);

		// DBProperty
		result &= execOneSQLCommand("CREATE TABLE " + getDbSchemaName() + ".db_property (" //
				+ "version INTEGER, " //
				+ "modified TIMESTAMP);");

		// EdiDocumentLocator
		result &= createTable("edi_document_locator", //
			"document_path VARCHAR(255) NOT NULL, " //
					+ "document_name VARCHAR(255) NOT NULL, " //
					+ "document_state_enum VARCHAR(255) NOT NULL, " //
					+ "received TIMESTAMP, " //
					+ "processed TIMESTAMP " //
		);

		// EdiService
		result &= createTable("edi_service", //
			"dtype TEXT NOT NULL, " //
					+ "provider_enum TEXT NOT NULL, " //
					+ "service_state_enum TEXT NOT NULL, " //
					+ "provider_credentials TEXT, " //
					+ "cursor TEXT " //
		);

		// Item
		result &= createTable("item", //
			"quantity INTEGER NOT NULL, " //
					+ "pos_along_path DOUBLE PRECISION, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "stored_location_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "uom_master_persistentid " + UUID_TYPE + " NOT NULL " //
		);

		// ItemDdcGroup
		result &= createTable("item_ddc_group", //
			"start_pos_along_path DECIMAL NOT NULL," //
					+ "end_pos_along_path DECIMAL NOT NULL" //
		);

		// ItemMaster
		result &= createTable("item_master", //
			"description TEXT, " //
					+ "lot_handling_enum TEXT NOT NULL, " //
					+ "ddc_id TEXT, " //
					+ "slot_flex_id TEXT, " //
					+ "ddc_pack_depth INTEGER, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "standard_uom_persistentid " + UUID_TYPE + " NOT NULL " //
		);

		// LedController
		result &= createTable("led_controller", //
			"description TEXT, " //
					+ "device_guid BYTEA DEFAULT '' NOT NULL, " //
					+ "public_key TEXT NOT NULL, " //
					+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
					+ "serial_bus_position INT DEFAULT 0 " //
		);

		// Location
		result &= createTable("location", //
			"dtype TEXT NOT NULL, " //
					+ "pos_type_enum TEXT NOT NULL, " //
					+ "pos_x DOUBLE PRECISION NOT NULL, " //
					+ "pos_y DOUBLE PRECISION NOT NULL, " //
					+ "pos_z DOUBLE PRECISION, " + "description TEXT, "//
					+ "path_segment_persistentid " + UUID_TYPE + ", " //
					+ "pos_along_path DOUBLE PRECISION, " //
					+ "led_controller_persistentid " + UUID_TYPE + ", "//
					+ "led_channel INTEGER, " //
					+ "first_led_num_along_path INTEGER, " //
					+ "last_led_num_along_path INTEGER, " //
					+ "first_ddc_id TEXT, " //
					+ "last_ddc_id TEXT, " //
					+ "parent_organization_persistentid " + UUID_TYPE + " "//
		);

		// LocationAlias
		result &= createTable("location_alias", //
			"mapped_location_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL " //
		);

		// OrderDetail
		result &= createTable("order_detail", //
			"status_enum TEXT NOT NULL, " //
					+ "item_master_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "description TEXT, " //
					+ "quantity INTEGER NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "uom_master_persistentid " + UUID_TYPE + " NOT NULL " //
		);

		// OrderGroup
		result &= createTable("order_group", //
			"status_enum TEXT NOT NULL, " //
					+ "work_sequence " + UUID_TYPE + ", " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "description TEXT " //
		);

		// OrderHeader
		result &= createTable("order_header", //
			"status_enum TEXT NOT NULL, " //
					+ "pick_strategy_enum TEXT NOT NULL, " //
					+ "order_type_enum TEXT DEFAULT 'PICK' NOT NULL, " //
					+ "order_group_persistentid " + UUID_TYPE + ", " //
					+ "work_sequence " + UUID_TYPE + ", " //
					+ "order_date TIMESTAMP NOT NULL, " //
					+ "due_date TIMESTAMP NOT NULL, " //
					+ "shipment_id TEXT, " //
					+ "container_use_persistentid " + UUID_TYPE + ", " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL, " //
					+ "customer_id TEXT " //
		);

		// OrderLocation
		result &= createTable("order_location", //
			"location_persistentid " + UUID_TYPE + " NOT NULL, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "updated TIMESTAMP NOT NULL " //
		);

		// Organization - this is the top-level object that owns all other objects.
		result &= createOrganizationTable("description TEXT NOT NULL " //
		);

		// Path
		result &= createTable("path", //
			"description TEXT NOT NULL, " //
					+ "travel_dir_enum TEXT NOT NULL, " //
					+ "length DOUBLE PRECISION ");

		// PathSegment
		result &= createTable("path_segment", //
			"segment_order INTEGER NOT NULL, " //
					+ "pos_type_enum TEXT NOT NULL, " //
					+ "start_pos_x DOUBLE PRECISION NOT NULL, " //
					+ "start_pos_y DOUBLE PRECISION NOT NULL, " //
					+ "end_pos_x DOUBLE PRECISION NOT NULL, " //
					+ "end_pos_y DOUBLE PRECISION NOT NULL, " //
					+ "start_pos_along_path DOUBLE PRECISION, " //
					+ "anchor_location_persistentid " + UUID_TYPE //
		);

		// PersistentProperty
		result &= createTable("persistent_property", //
			"current_value_str TEXT, " //
					+ "default_value_str TEXT " //
		);

		// UomMaster
		result &= createTable("uom_master", //
			"description TEXT " //
		);

		// User
		result &= createTable("user", //
			"hash_salt TEXT, " //
					+ "hashed_password TEXT, " //
					+ "hash_iterations INTEGER, " //
					+ "email TEXT, " //
					+ "created TIMESTAMP, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL " //
		);

		// UserSession
		result &= createTable("user_session", //
			"activity TEXT NOT NULL, " //
					+ "created TIMESTAMP NOT NULL, " //
					+ "ended TIMESTAMP " //
		);

		// Vertex
		result &= createTable("vertex", //
			"pos_type_enum TEXT NOT NULL, " //
					+ "pos_x DOUBLE PRECISION NOT NULL, " //
					+ "pos_y DOUBLE PRECISION NOT NULL, " //
					+ "pos_z DOUBLE PRECISION, " //
					+ "draw_order INT NOT NULL " //
		);

		// WorkArea
		result &= createTable("work_area", //
			"work_area_id TEXT NOT NULL, " //
					+ "description TEXT NOT NULL " //
		);

		// WorkInstruction
		result &= createTable("work_instruction", //
			"type_enum TEXT NOT NULL, " //
					+ "status_enum TEXT NOT NULL, " //
					+ "container_id TEXT NOT NULL, " //
					+ "item_id TEXT NOT NULL, " //
					+ "description TEXT NOT NULL, " //
					+ "pick_instruction TEXT, " //
					+ "plan_quantity INTEGER NOT NULL, " //
					+ "actual_quantity INTEGER NOT NULL, " //
					+ "location_id TEXT NOT NULL, " //
					+ "pos_along_path DOUBLE PRECISION, " //
					+ "picker_id TEXT, " //
					+ "led_cmd_stream TEXT, " //
					+ "group_and_sort_code TEXT," //
					+ "created TIMESTAMP, " //
					+ "assigned TIMESTAMP, " //
					+ "started TIMESTAMP, " //
					+ "completed TIMESTAMP " //
		);

		return result;
	}
}
