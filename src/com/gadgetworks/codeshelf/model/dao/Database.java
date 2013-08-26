/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Database.java,v 1.15 2013/08/26 02:14:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.config.AutofetchConfig;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class Database implements IDatabase {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(Database.class);

	private final ISchemaManager	mSchemaManager;
	private final IUtil				mUtil;

	@Inject
	public Database(final ISchemaManager inSchemaManager, final IUtil inUtil) {
		mSchemaManager = inSchemaManager;
		mUtil = inUtil;

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

		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setName(mSchemaManager.getDbSchemaName());

		// Give the properties file a chance to setup values.
		serverConfig.loadFromProperties();

		// Now set values we never want changed by properties file.
		serverConfig.setDefaultServer(false);
		serverConfig.setResourceDirectory(mUtil.getApplicationDataDirPath());
		//		serverConfig.setDebugLazyLoad(false);
		//		serverConfig.setDebugSql(false);
		//		serverConfig.setLoggingLevel(LogLevel.NONE);
		serverConfig.setLoggingToJavaLogger(true);
		serverConfig.setLoggingDirectory(mUtil.getApplicationLogDirPath());
		serverConfig.setPackages(new ArrayList<String>(Arrays.asList("com.gadgetworks.codeshelf.model.domain")));
		serverConfig.setJars(new ArrayList<String>(Arrays.asList("server.codeshelf.jar")));
		serverConfig.setUpdateChangesOnly(true);
		serverConfig.setDdlGenerate(false);
		serverConfig.setDdlRun(false);
		//serverConfig.setNamingConvention(new GWEbeanNamingConvention());
		serverConfig.setNamingConvention(new UnderscoreNamingConvention());

		DataSourceConfig dataSourceConfig = serverConfig.getDataSourceConfig();
		dataSourceConfig.setUsername(mSchemaManager.getDbUserId());
		dataSourceConfig.setPassword(mSchemaManager.getDbPassword());
		dataSourceConfig.setUrl(mSchemaManager.getApplicationDatabaseURL());
		dataSourceConfig.setDriver(mSchemaManager.getDriverName());
		dataSourceConfig.setMinConnections(5);
		dataSourceConfig.setMaxConnections(25);
		dataSourceConfig.setIsolationLevel(Transaction.READ_COMMITTED);
		//		dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		AutofetchConfig autofetchConfig = serverConfig.getAutofetchConfig();
		autofetchConfig.setLogDirectory(mUtil.getApplicationLogDirPath());
		autofetchConfig.setUseFileLogging(true);

		EbeanServer server = EbeanServerFactory.create(serverConfig);
		if (server == null) {
			mUtil.exitSystem();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean start() {

		boolean result = false;

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
