/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Database.java,v 1.5 2012/11/20 04:10:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.LogLevel;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class Database implements IDatabase {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Database.class);

	private ISchemaManager		mSchemaManager;
	private IUtil				mUtil;

	@Inject
	public Database(final ISchemaManager inSchemaManager, final IUtil inUtil) {
		mSchemaManager = inSchemaManager;
		mUtil = inUtil;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean start() {

		boolean result = false;

		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		String appDataDir = mUtil.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);
		System.setProperty("app.database.url", mSchemaManager.getApplicationDatabaseURL());
		System.setProperty("ebean.props.file", "conf/ebean.properties");
		System.setProperty("java.util.logging.config.file", "conf/logging.properties");

		File appDir = new File(appDataDir);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				LOGGER.error("", e);
				mUtil.exitSystem();
			}
		}

		File dataDir = new File(appDataDir + System.getProperty("file.separator") + "db");
		if (!dataDir.exists()) {
			try {
				dataDir.mkdir();
			} catch (SecurityException e) {
				LOGGER.error("", e);
				mUtil.exitSystem();
			}
		}

		mSchemaManager.verifySchema();

		DataSourceConfig dataSourceConfig = new DataSourceConfig();
		dataSourceConfig.setUsername(mSchemaManager.getDbUserId());
		dataSourceConfig.setPassword(mSchemaManager.getDbPassword());
		dataSourceConfig.setUrl(mSchemaManager.getApplicationDatabaseURL());
		dataSourceConfig.setDriver(mSchemaManager.getDriverName());
		dataSourceConfig.setMinConnections(1);
		dataSourceConfig.setMaxConnections(25);
		dataSourceConfig.setIsolationLevel(Transaction.READ_COMMITTED);
//		dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		// Setup the EBean server configuration.
		ServerConfig config = new ServerConfig();
		config.setName("codeshelf.primary");
		//		config.loadFromProperties();
		config.setDataSourceConfig(dataSourceConfig);
		config.setNamingConvention(new GWEbeanNamingConvention());
		config.setDefaultServer(true);
		config.setResourceDirectory(mUtil.getApplicationDataDirPath());
		config.setDebugLazyLoad(true);
		config.setDebugSql(false);
		config.setLoggingLevel(LogLevel.SQL);
		config.setLoggingToJavaLogger(true);
		config.setPackages(new ArrayList<String>(Arrays.asList("com.gadgetworks.codeshelf.model.domain")));
		config.setJars(new ArrayList<String>(Arrays.asList("codeshelf.jar")));
		config.setUpdateChangesOnly(true);
		config.setDdlGenerate(false);
		config.setDdlRun(false);

		EbeanServer server = EbeanServerFactory.create(config);
		if (server == null) {
			mUtil.exitSystem();
		}

		result = true;

		LOGGER.info("Database started");

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean stop() {

		boolean result = false;

		LOGGER.info("Stopping DAO");

		//		try {
		//			Connection connection = DriverManager.getConnection(mUtil.getApplicationDatabaseURL(), "codeshelf", "codeshelf");
		//
		//			// Try to switch to the proper schema.
		//			Statement stmt = connection.createStatement();
		//			stmt.execute("SHUTDOWN COMPACT");
		//			stmt.close();
		//			connection.close();
		//		} catch (SQLException e) {
		//			LOGGER.error("", e);
		//		}

		ShutdownManager.shutdown();
		result = true;

		LOGGER.info("Database shutdown");

		return result;
	}

}
