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

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Inject
	public SchemaManagerABC(final String inDbUserId,
		final String inDbPassword,
		final String inDbName,
		final String inDbSchemaName,
		final String inDbAddress,
		final String inDbPortnum) {
		dbUserId = inDbUserId;
		dbPassword = inDbPassword;
		dbName = inDbName;
		dbSchemaName = inDbSchemaName;
		dbAddress = inDbAddress;
		dbPortnum = inDbPortnum;
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
				System.exit(1);
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
					System.exit(1);
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
	@Override
	public Connection getConnection(final String inDbUrl) throws SQLException {
		Connection result = null;

		try {
			Class.forName(getDriverName());

			if ((getDbUserId() == null) || (getDbUserId().length() == 0)) {
				result = DriverManager.getConnection(inDbUrl);
			} else {
				Properties props = new Properties();
				props.setProperty("user", getDbUserId());
				props.setProperty("password", getDbPassword());

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

		// If an multiple upgrades fail part way, make sure we record only what we achieved.
		int versionOfDBAchived = inOldVersion;

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_2)) {
			result &= doUpgrade002();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_3)) {
			result &= doUpgrade003();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_4)) {
			result &= doUpgrade004();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_5)) {
			result &= doUpgrade005();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_6)) {
			result &= doUpgrade006();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_7)) {
			result &= doUpgrade007();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_8)) {
			result &= doUpgrade008();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_9)) {
			result &= doUpgrade009();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_10)) {
			result &= doUpgrade010();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_11)) {
			result &= doUpgrade011();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_12)) {
			result &= doUpgrade012();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_13)) {
			result &= doUpgrade013();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_14)) {
			result &= doUpgrade014();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_15)) {
			result &= doUpgrade015();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_16)) {
			result &= doUpgrade016();
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_17)) {
			result &= doUpgrade017();
			if (result)
				versionOfDBAchived = ISchemaManager.DATABASE_VERSION_17;
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_18)) {
			result &= doUpgrade018();
			if (result)
				versionOfDBAchived = ISchemaManager.DATABASE_VERSION_18;
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_19)) {
			result &= doUpgrade019();
			if (result)
				versionOfDBAchived = ISchemaManager.DATABASE_VERSION_19;
		}

		if ((result) && (inOldVersion < ISchemaManager.DATABASE_VERSION_20)) {
			result &= doUpgrade020();
			if (result)
				versionOfDBAchived = ISchemaManager.DATABASE_VERSION_20;
		}

		if (versionOfDBAchived > inOldVersion) {
			LOGGER.info("Updating version in db_property table");
			if (!result)
				LOGGER.error("An upgrade action failed. You may need to check the consistency of db_property.version and whatever upgrade action failed.");
			// Sounds bogus, but I saw a failure returned, when the table was successfully modified.
			// Or, we could be trying to add a column that exists already.

			result &= updateSchemaVersion(versionOfDBAchived);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade002() {
		boolean result = true;

		result &= safeAddColumn("order_header", "custoemr_id", "TEXT");
		result &= safeAddColumn("order_header", "shipment_id", "TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade003() {
		boolean result = true;

		result &= safeAddColumn("item_master", "slot_flex_id", "TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade004() {
		boolean result = true;

		result &= safeAddColumn("work_instruction", "led_cmd_stream", "TEXT");
		result &= safeAddColumn("work_instruction", "group_and_sort_code", "TEXT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade005() {
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
	private boolean doUpgrade006() {
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
	private boolean doUpgrade007() {
		boolean result = true;

		result &= safeAddColumn("order_header", "order_type_enum", "TEXT DEFAULT 'PICK' NOT NULL");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade008() {
		boolean result = true;

		result &= safeModifyColumnType("order_detail", "quantity", "INTEGER");
		result &= safeModifyColumnType("work_instruction", "plan_quantity", "INTEGER");
		result &= safeModifyColumnType("work_instruction", "actual_quantity", "INTEGER");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade009() {
		boolean result = true;

		result &= safeAddColumn("work_instruction", "container_persistentid", UUID_TYPE + " NOT NULL");
		result &= safeAddColumn("work_instruction", "item_master_persistentid", UUID_TYPE + " NOT NULL");
		result &= safeAddColumn("work_instruction", "location_persistentid", UUID_TYPE + " NOT NULL");

		result &= linkToParentTable("work_instruction", "item_master", "item_master");
		result &= linkToParentTable("work_instruction", "container", "container");
		result &= linkToParentTable("work_instruction", "location", "location");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade010() {
		boolean result = true;

		result &= safeAddColumn("location", "face_width_meters", "DOUBLE PRECISION DEFAULT 0 NOT NULL");
		result &= safeAddColumn("location", "face_height_meters", "DOUBLE PRECISION DEFAULT 0 NOT NULL");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade011() {
		boolean result = true;

		result &= safeRenameColumn("location", "pos_type_enum", "anchor_pos_type_enum");
		result &= safeRenameColumn("location", "pos_x", "anchor_pos_x");
		result &= safeRenameColumn("location", "pos_y", "anchor_pos_y");
		result &= safeRenameColumn("location", "pos_z", "anchor_pos_z");

		result &= safeAddColumn("location", "pick_face_end_pos_type_enum", "TEXT DEFAULT 'METERS_PARENT'");
		result &= safeAddColumn("location", "pick_face_end_pos_x", "DOUBLE PRECISION DEFAULT 0");
		result &= safeAddColumn("location", "pick_face_end_pos_y", "DOUBLE PRECISION DEFAULT 0");
		result &= safeAddColumn("location", "pick_face_end_pos_z", "DOUBLE PRECISION DEFAULT 0");

		result &= safeDropColumn("location", "face_width_meters");
		result &= safeDropColumn("location", "face_height_meters");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade012() {
		boolean result = true;

		result &= safeDropColumn("location", "anchor_location_persistentid");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade013() {
		boolean result = true;

		result &= safeAddColumn("path_segment", "start_pos_z", "DOUBLE PRECISION DEFAULT 0 NOT NULL");
		result &= safeAddColumn("path_segment", "end_pos_z", "DOUBLE PRECISION DEFAULT 0 NOT NULL");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade014() {
		boolean result = true;

		result &= safeAddColumn("work_instruction", "assigned_che_persistentid", UUID_TYPE);

		result &= execOneSQLCommand("CREATE  INDEX work_instruction_che_index ON " + getDbSchemaName()
				+ ".work_instruction (assigned_che_persistentid)");

		result &= execOneSQLCommand("CREATE  INDEX work_instruction_status_index ON " + getDbSchemaName()
				+ ".work_instruction (type_enum, status_enum)");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade015() {
		boolean result = true;

		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".order_header ALTER COLUMN order_date DROP NOT NULL");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".order_header ALTER COLUMN due_date DROP NOT NULL");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade016() {
		boolean result = true;

		// The schema upgrade is complicated by the fact that we want to add a column that requires a default only at init.
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
				+ ".order_detail ADD min_quantity INTEGER NOT NULL DEFAULT 0");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
				+ ".order_detail ADD max_quantity INTEGER NOT NULL DEFAULT 0");
		result &= execOneSQLCommand("UPDATE " + getDbSchemaName() + ".order_detail SET min_quantity = quantity");
		result &= execOneSQLCommand("UPDATE " + getDbSchemaName() + ".order_detail SET max_quantity = quantity");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".order_detail ALTER min_quantity DROP DEFAULT");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".order_detail ALTER max_quantity DROP DEFAULT");

		// The schema upgrade is complicated by the fact that we want to add a column that requires a default only at init.
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
				+ ".work_instruction ADD plan_min_quantity INTEGER NOT NULL DEFAULT 0");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
				+ ".work_instruction ADD plan_max_quantity INTEGER NOT NULL DEFAULT 0");
		result &= execOneSQLCommand("UPDATE " + getDbSchemaName() + ".work_instruction SET plan_min_quantity = plan_quantity");
		result &= execOneSQLCommand("UPDATE " + getDbSchemaName() + ".work_instruction SET plan_max_quantity = plan_quantity");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".work_instruction ALTER plan_min_quantity DROP DEFAULT");
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + ".work_instruction ALTER plan_max_quantity DROP DEFAULT");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean doUpgrade017() {
		boolean result = true;
		try {
			result &= safeRenameColumn("item", "pos_along_path", "meters_from_anchor");
			// will throw a PSQLException, but code is not annotated to say it throws that. So, compiler will not let us catch that.
		} catch (Exception e) {
			LOGGER.error("doUpgrade017", e);
			result = false;
		}
		if (!result)
			LOGGER.error("upgrade action 17 failed. Is meters_from_anchor column in item table present? If so, set db_property.version to 17 (or higher).");
		else
			LOGGER.info("upgrade action 17: rename column pos_along_path to meters_from_anchor");
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * upgrade 18 add column to location
	 */
	private boolean doUpgrade018() {
		boolean result = true;

		try {
			result &= safeAddColumn("location", "lower_led_near_anchor", "BOOLEAN DEFAULT TRUE");
		} catch (Exception e) {
			LOGGER.error("doUpgrade018", e);
			result = false;
		}
		if (!result)
			LOGGER.error("upgrade action 18 failed. Is lower_led_near_anchor column in location table present? If so, set db_property.version to 18 (or higher).");
		else
			LOGGER.info("upgrade action 18: add column lower_led_near_anchor");
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * upgrade 19 add site controller table; add color to che; add active to location
	 */
	private boolean doUpgrade019() {
		boolean result = true;
		LOGGER.info("start upgrade action 19");

		try {

			result &= createTable("site_controller", //
				"description VARCHAR(255), " //
						+ "device_guid BYTEA DEFAULT '' NOT NULL, " //
						+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
						+ "monitor BOOLEAN DEFAULT TRUE NOT NULL, " //
						+ "describe_location VARCHAR(255) DEFAULT 'Unknown' NOT NULL");
			execOneSQLCommand("CREATE UNIQUE INDEX site_controller_domainid_unique ON " + getDbSchemaName()
					+ ".site_controller (domainid)"); // serial number of site controller must be unique 

			result &= linkToParentTable("site_controller", "parent", "codeshelf_network");

			result &= safeAddColumn("codeshelf_network", "channel", "SMALLINT DEFAULT 10 NOT NULL");
			result &= safeAddColumn("codeshelf_network", "network_num", "SMALLINT DEFAULT 1 NOT NULL");

			// completely remove DEMO2 organization if present
			result &= execOneSQLCommand("DELETE FROM " + getDbSchemaName() + ".user WHERE parent_persistentid IN "
					+ "(SELECT persistentid FROM " + getDbSchemaName() + ".organization WHERE domainid='DEMO2')");
			result &= execOneSQLCommand("DELETE FROM " + getDbSchemaName() + ".organization WHERE domainid='DEMO2'");

			result &= safeDropColumn("user", "email"); // now using domainid
			result &= safeAddColumn("user", "site_controller_persistentid", UUID_TYPE); // null okay
			result &= linkToParentTable("user", "site_controller", "site_controller");
			result &= execOneSQLCommand("CREATE UNIQUE INDEX user_site_controller_index ON " + getDbSchemaName()
					+ ".user (site_controller_persistentid)"); // only one user per site controller
			result &= execOneSQLCommand("CREATE UNIQUE INDEX user_unique_domainid_index ON " + getDbSchemaName()
					+ ".user (domainid)"); // only one user per username

			result &= safeDropColumn("che", "public_key");
			result &= safeAddColumn("che", "color", "TEXT DEFAULT 'BLUE' NOT NULL");

			result &= safeDropColumn("led_controller", "public_key");

			result &= safeAddColumn("location", "active", "BOOLEAN DEFAULT TRUE NOT NULL");

			return result;

		} catch (Exception e) {
			LOGGER.error("error in doUpgrade019", e);
			result = false;
		}
		if (!result)
			LOGGER.error("upgrade action 19 failed. Is sitecontroller table present w/ fkey relations? If so, set db_property.version to 19+");
		else
			LOGGER.info("upgrade action 19: add sitecontroller table");
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * upgrade 20 
	 */
	private boolean doUpgrade020() {
		boolean result = true;
		LOGGER.info("start upgrade action 20");

		try {
			result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
					+ ".work_instruction ALTER COLUMN container_persistentid DROP NOT NULL");
			result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName()
					+ ".work_instruction ALTER COLUMN item_master_persistentid DROP NOT NULL");

			result &= safeAddColumn("work_instruction", "order_detail_persistentid", UUID_TYPE);
			result &= unLinkToParentTable("work_instruction", "parent", "order_detail");

			// Now the good part. Copy contents of parent_persistentid column to order_detail column. Then put the facility persistentId into parent_persistentid
			if (result) {

				Connection connection = getConnection(getApplicationInitDatabaseURL());
				Statement stmt = connection.createStatement();

				ResultSet resultSet = stmt.executeQuery("SELECT wi.parent_persistentid as wi_parent_id, wi.order_detail_persistentid, fac.persistentid as new_facility_parent_persistentid, wi.persistentid as wi_persist_id "
						+ "from "
						+ getDbSchemaName()
						+ ".work_instruction as wi "
						+ "join "
						+ getDbSchemaName()
						+ ".order_detail as od on wi.parent_persistentid = od.persistentid "
						+ "join "
						+ getDbSchemaName()
						+ ".order_header as oh on od.parent_persistentid = oh.persistentid "
						+ "join "
						+ getDbSchemaName()
						+ ".location as fac on oh.parent_persistentid = fac.persistentid");

				while (resultSet.next()) {
					String wiPersistentId = resultSet.getString("wi_persist_id");
					String detailPersistentId = resultSet.getString("wi_parent_id");
					String facilityPersistentId = resultSet.getString("new_facility_parent_persistentid");
					// Let's guard a bit against a double update. If parent is already the facility, don't copy to orderDetail field.
					if (!detailPersistentId.equals(facilityPersistentId)) {
						/*  // this could work, but doesn't
						resultSet.updateString("order_detail_persistentid", detailPersistentId);
						resultSet.updateString("wi_parent_id", facilityPersistentId);
						resultSet.updateRow();
						*/
						String sql1 = "UPDATE " + getDbSchemaName() + ".work_instruction SET order_detail_persistentid = '"
								+ detailPersistentId + "' WHERE persistentid = '" + wiPersistentId + "'";
						LOGGER.info(sql1);
						boolean thisActionValue = execOneSQLCommand(sql1);

						String sql2 = "UPDATE " + getDbSchemaName() + ".work_instruction SET parent_persistentid = '"
								+ facilityPersistentId + "' WHERE persistentid = '" + wiPersistentId + "'";
						LOGGER.info(sql2);
						thisActionValue &= execOneSQLCommand(sql2);

						if (!thisActionValue) {
							LOGGER.error("Failed to set order_detail and/or parent fields on this record.");
						}
					} else {
						LOGGER.error("oops-getting records for update that does not need it.");
					}
				}
			}
			if (result) {
				// add these links/constraints after the column changes above, so we do not get constraint violations
				result &= linkToParentTable("work_instruction", "order_detail", "order_detail");
				result &= linkToParentTable("work_instruction", "parent", "location");
			}

		} catch (Exception e) {
			LOGGER.error("doUpgrade020", e);
			result = false;
		}
		if (!result) {
			LOGGER.error("upgrade action 20 failed. In work instruction, container and itemMaster become nullable; new order_detail field populated. If so already, set db_property.version to 20+.");
		} else
			LOGGER.info("upgrade action 20: In work instruction table, container and itemMaster should be nullable. New orderDetail field. ");
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inFromSchema
	 *  @param inToSchema
	 */
	@SuppressWarnings("unused")
	private void downgradeSchema(int inOldVersion, int inNewVersion) {
		throw new UnsupportedOperationException("Cannot downgrade database");
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
	 * @param allowNullParent
	 */
	private boolean createTable(final String inTableName, final String inColumns, boolean allowNullParent) {

		boolean result = true;

		result &= execOneSQLCommand("CREATE TABLE " + getDbSchemaName() + "." + inTableName + " (" //
				+ "persistentid " + UUID_TYPE + " NOT NULL, " //
				+ "parent_persistentid " + UUID_TYPE + (allowNullParent ? ", " : " NOT NULL, ") //
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
	 * Create a standard DomainObject table with all the appropriate boilerplate and then add the stuff for the particular domain class.
	 * @param inTableName
	 * @param inColumns
	 */
	private boolean createTable(final String inTableName, final String inColumns) {
		return createTable(inTableName, inColumns, false);
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a standard DomainObject table with all the appropriate boilerplate and then add the stuff for the particular domain class.
	 * (Except in this case the parent_persistentid can be null.)
	 * @param inTableName
	 * @param inColumns
	 */
	private boolean createTableOptionalParent(final String inTableName, final String inColumns) {
		return createTable(inTableName, inColumns, true);
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
	 * @param inChildTableName
	 * @param inForeignKeyColumnName
	 * @param inParentTableName
	 */
	private boolean unLinkToParentTable(final String inChildTableName,
		final String inForeignKeyColumnName,
		final String inParentTableName) {

		boolean result = true;

		// Drop the foreign key constraint.
		// many databases use "DROP FOREIGN KEY"
		// but Postgres uses "DROP CONSTRAINT"  and notice the _fkey1.

		String constraintString = "ALTER TABLE " + getDbSchemaName() + "." + inChildTableName //
				+ " DROP CONSTRAINT " + inChildTableName + "_" + inForeignKeyColumnName + "_persistentid" + "_fkey";
		LOGGER.info("unLinkToParentTable: " + constraintString);
		result &= execOneSQLCommand(constraintString); //

		// And drop the index
		String indexString = "DROP INDEX " //
				+ inChildTableName + "_" + inForeignKeyColumnName + "_" + inParentTableName;
		LOGGER.info("unLinkToParentTable: " + indexString);
		result &= execOneSQLCommand(indexString);

		return result;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSameTableName
	 * @param inForeignKeyColumnName
	 * @param inForeignTableName
	 */
	private boolean linkToParentTableRecursive(final String inSameTableName,
		final String inSameTableColumnName,
		final String inForeignTableName,
		final String inForeignTableColumnName) {

		boolean result = true;

		//		// Add the foreign key constraint for the recursive table.
		//		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inSameTableName //
		//				+ " ADD CONSTRAINT XXX FOREIGN KEY (" + inSameTableColumnName + "_persistentid)" //
		//				+ " REFERENCES DATABASE." + getDbSchemaName() + "." + inSameTableName + " (persistentid);");
		//
		//		// Add the foreign key constraint for the final table.
		//		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inSameTableName //
		//				+ " ADD CONSTRAINT YYY FOREIGN KEY (" + inForeignTableColumnName + "_persistentid)" //
		//				+ " REFERENCES DATABASE." + getDbSchemaName() + "." + inForeignTableName + " (persistentid);");

		// Now make sure at least one of them is not null.
		result &= execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "."
				+ inSameTableName //
				+ " ADD CHECK " //
				+ " (" + inSameTableName + "." + inSameTableColumnName + "_persistentid IS NOT NULL OR " + inSameTableName + "."
				+ inForeignTableColumnName + "_persistentid IS NOT NULL )");

		// Add the index that makes it efficient to find the child objects from the parent.
		result &= execOneSQLCommand("CREATE INDEX " //
				+ inSameTableName + "_" + inSameTableColumnName + "_" + inSameTableName //
				+ " ON " + getDbSchemaName() + "." + inSameTableName + " (" + inSameTableColumnName + "_persistentid)");

		return result;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @param inTypeDef
	 * @return
	 */
	private boolean safeAddColumn(final String inTableName, final String inColumnName, final String inTypeDef) {

		return execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " ADD " + inColumnName //
				+ " " + inTypeDef // 
				+ ";");

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @return
	 */
	private boolean safeDropColumn(final String inTableName, final String inColumnName) {

		return execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " DROP " + inColumnName //
				+ ";");

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @return
	 */
	private boolean safeModifyColumnType(final String inTableName, final String inColumnName, final String inNewColumnType) {

		return execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " ALTER COLUMN " + inColumnName //
				+ " TYPE " + inNewColumnType //
				+ ";");

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTableName
	 * @param inColumnName
	 * @return
	 */
	private boolean safeRenameColumn(final String inTableName, final String inColumnName, final String inNewColumnName) {

		return execOneSQLCommand("ALTER TABLE " + getDbSchemaName() + "." + inTableName //
				+ " RENAME COLUMN " + inColumnName //
				+ " TO " + inNewColumnName //
				+ ";");

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

		result &= linkToParentTableRecursive("location", "parent", "organization", "parent_organization");
		result &= linkToParentTable("location", "path_segment", "path_segment");
		result &= linkToParentTable("location", "parent_organization", "organization");

		result &= linkToParentTable("location_alias", "parent", "location" /* facility */);

		result &= linkToParentTable("order_detail", "parent", "order_header");

		result &= linkToParentTable("order_group", "parent", "location");

		result &= linkToParentTable("order_header", "parent", "location");

		result &= linkToParentTable("order_location", "parent", "order_header");

		result &= linkToParentTable("path", "parent", "location");

		result &= linkToParentTable("path_segment", "parent", "path");

		result &= linkToParentTable("persistent_property", "parent", "organization");

		result &= linkToParentTable("site_controller", "parent", "codeshelf_network");
		execOneSQLCommand("CREATE UNIQUE INDEX site_controller_domainid_unique ON " + getDbSchemaName()
				+ ".site_controller (domainid)"); // serial number of site controller must be unique 

		result &= linkToParentTable("uom_master", "parent", "location");

		result &= linkToParentTable("user", "parent", "organization");
		result &= linkToParentTable("user", "site_controller", "site_controller");
		// Ensure only one user per site controller
		execOneSQLCommand("CREATE UNIQUE INDEX user_site_controller_index ON " + getDbSchemaName()
				+ ".user (site_controller_persistentid)");
		// Ensure only one user per username
		result &= execOneSQLCommand("CREATE UNIQUE INDEX user_unique_domainid_index ON " + getDbSchemaName() + ".user (domainid)"); // only one user per username

		result &= linkToParentTable("user_session", "parent", "USER");

		result &= linkToParentTable("vertex", "parent", "location");

		result &= linkToParentTable("work_area", "parent", "PATH");

		result &= linkToParentTable("work_instruction", "parent", "location");
		result &= linkToParentTable("work_instruction", "item_master", "item_master");
		result &= linkToParentTable("work_instruction", "container", "container");
		result &= linkToParentTable("work_instruction", "order_detail", "order_detail");

		result &= execOneSQLCommand("CREATE  INDEX work_instruction_che_index ON " + getDbSchemaName()
				+ ".work_instruction (assigned_che_persistentid)");

		result &= execOneSQLCommand("CREATE  INDEX work_instruction_status_index ON " + getDbSchemaName()
				+ ".work_instruction (type_enum, status_enum)");

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
					+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
					+ "serial_bus_position INT DEFAULT 0, " //
					+ "current_user_persistentid " + UUID_TYPE + ", " //
					+ "current_work_area_persistentid " + UUID_TYPE + ", " //
					+ "color TEXT DEFAULT 'BLUE' NOT NULL");

		// CodeshelfNetwork
		result &= createTable("codeshelf_network", //
			"description VARCHAR(255) NOT NULL, " //
					+ "channel SMALLINT DEFAULT 10 NOT NULL, " //
					+ "network_num SMALLINT DEFAULT 1 NOT NULL, " //
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
					+ "meters_from_anchor DOUBLE PRECISION, " //
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
					+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
					+ "serial_bus_position INT DEFAULT 0 " //
		);

		// Location
		result &= createTableOptionalParent("location", //
			"dtype TEXT NOT NULL, " //
					+ "anchor_pos_type_enum TEXT NOT NULL, " //
					+ "anchor_pos_x DOUBLE PRECISION NOT NULL, " //
					+ "anchor_pos_y DOUBLE PRECISION NOT NULL, " //
					+ "anchor_pos_z DOUBLE PRECISION NOT NULL, " //
					+ "pick_face_end_pos_type_enum TEXT DEFAULT 'METERS_PARENT', " //
					+ "pick_face_end_pos_x DOUBLE PRECISION DEFAULT 0, " //
					+ "pick_face_end_pos_y DOUBLE PRECISION DEFAULT 0, " //
					+ "pick_face_end_pos_z DOUBLE PRECISION DEFAULT 0, " //
					+ "description TEXT, "//
					+ "path_segment_persistentid " + UUID_TYPE + ", " //
					+ "pos_along_path DOUBLE PRECISION, " //
					+ "led_controller_persistentid " + UUID_TYPE + ", "//
					+ "led_channel INTEGER, " //
					+ "first_led_num_along_path INTEGER, " //
					+ "last_led_num_along_path INTEGER, " //
					+ "first_ddc_id TEXT, " //
					+ "last_ddc_id TEXT, " //
					+ "parent_organization_persistentid " + UUID_TYPE + ", "//
					+ "lower_led_near_anchor BOOLEAN DEFAULT TRUE, " //
					+ "active BOOLEAN DEFAULT TRUE NOT NULL ");

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
					+ "min_quantity INTEGER NOT NULL, " //
					+ "max_quantity INTEGER NOT NULL, " //
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
					+ "order_date TIMESTAMP, " //
					+ "due_date TIMESTAMP, " //
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
					+ "start_pos_x DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "start_pos_y DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "start_pos_z DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "end_pos_x DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "end_pos_y DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "end_pos_z DOUBLE PRECISION DEFAULT 0 NOT NULL, " //
					+ "start_pos_along_path DOUBLE PRECISION " //
		);

		// PersistentProperty
		result &= createTable("persistent_property", //
			"current_value_str TEXT, " //
					+ "default_value_str TEXT " //
		);

		// SiteController
		result &= createTable("site_controller", //
			"description VARCHAR(255), " //
					+ "device_guid BYTEA DEFAULT '' NOT NULL, " //
					+ "last_battery_level SMALLINT DEFAULT 0 NOT NULL, " //
					+ "monitor BOOLEAN DEFAULT TRUE NOT NULL, " //
					+ "describe_location VARCHAR(255) NOT NULL");

		// UomMaster
		result &= createTable("uom_master", //
			"description TEXT " //
		);

		// User
		result &= createTable("user", //
			"hash_salt TEXT, " //
					+ "hashed_password TEXT, " //
					+ "hash_iterations INTEGER, " //
					+ "site_controller_persistentid " + UUID_TYPE + ", " //
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
					+ "container_persistentid " + UUID_TYPE + ", " //
					+ "container_id TEXT NOT NULL, " //
					+ "item_master_persistentid " + UUID_TYPE + ", " //
					+ "item_id TEXT NOT NULL, " //
					+ "description TEXT NOT NULL, " //
					+ "pick_instruction TEXT, " //
					+ "plan_quantity INTEGER NOT NULL, " //
					+ "plan_min_quantity INTEGER NOT NULL, " //
					+ "plan_max_quantity INTEGER NOT NULL, " //
					+ "actual_quantity INTEGER NOT NULL, " //
					+ "location_persistentid " + UUID_TYPE + ", " //
					+ "location_id TEXT NOT NULL, " //
					+ "pos_along_path DOUBLE PRECISION, " //
					+ "assigned_che_persistentid " + UUID_TYPE + ", " //
					+ "picker_id TEXT, " //
					+ "led_cmd_stream TEXT, " //
					+ "group_and_sort_code TEXT," //
					+ "created TIMESTAMP, " //
					+ "assigned TIMESTAMP, " //
					+ "started TIMESTAMP, " //
					+ "completed TIMESTAMP, " //
					+ "order_detail_persistentid " + UUID_TYPE //
		);

		return result;
	}
}
