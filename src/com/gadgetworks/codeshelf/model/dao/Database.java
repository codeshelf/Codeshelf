/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Database.java,v 1.16 2013/09/18 00:40:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.config.AutofetchConfig;
import com.avaje.ebean.config.AutofetchMode;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.gadgetworks.codeshelf.application.Configuration;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class Database implements IDatabase {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(Database.class);

	@Getter
	private final ISchemaManager	schemaManager;

	@Inject
	public Database(final ISchemaManager inSchemaManager) {
		schemaManager = inSchemaManager;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean start() {
		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		// bhe: commented out - this should not be here
		// Configuration.loadConfig("test");

		System.setProperty("app.database.url", schemaManager.getApplicationDatabaseURL());
		//System.setProperty("ebean.props.file", "conf/ebean.properties");
		//System.setProperty("java.util.logging.config.file", "conf/logging.properties");

		schemaManager.verifySchema();

		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setName(schemaManager.getDbSchemaName());

		// Give the properties file a chance to setup values.
		serverConfig.loadFromProperties();

		// Now set values we never want changed by properties file.
		serverConfig.setDefaultServer(false);
		serverConfig.setResourceDirectory(Configuration.getApplicationDataDirPath());
		//		serverConfig.setDebugLazyLoad(false);
		//		serverConfig.setDebugSql(false);
		//		serverConfig.setLoggingLevel(LogLevel.NONE);
		//		serverConfig.setLoggingToJavaLogger(true);
		//		serverConfig.setLoggingDirectory(mUtil.getApplicationLogDirPath());
		serverConfig.setPackages(new ArrayList<String>(Arrays.asList("com.gadgetworks.codeshelf.model.domain")));
		serverConfig.setJars(new ArrayList<String>(Arrays.asList("server.codeshelf.jar")));
		serverConfig.setUpdateChangesOnly(true);
		serverConfig.setDdlGenerate(false);
		serverConfig.setDdlRun(false);
		// serverConfig.setNamingConvention(new GWEbeanNamingConvention());
		UnderscoreNamingConvention namingConvetion = new UnderscoreNamingConvention();
		namingConvetion.setSchema(schemaManager.getDbSchemaName());
		serverConfig.setNamingConvention(namingConvetion);

		DataSourceConfig dataSourceConfig = serverConfig.getDataSourceConfig();
		dataSourceConfig.setUsername(schemaManager.getDbUserId());
		dataSourceConfig.setPassword(schemaManager.getDbPassword());
		dataSourceConfig.setUrl(schemaManager.getApplicationDatabaseURL());
		dataSourceConfig.setDriver(schemaManager.getDriverName());
		dataSourceConfig.setMinConnections(5);
		dataSourceConfig.setMaxConnections(25);
		dataSourceConfig.setIsolationLevel(Transaction.READ_COMMITTED);
		// dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		AutofetchConfig autofetchConfig = serverConfig.getAutofetchConfig();
		autofetchConfig.setMode(AutofetchMode.DEFAULT_OFF);
		autofetchConfig.setLogDirectory(Configuration.getApplicationLogDirPath());
		// autofetchConfig.setUseFileLogging(true);
		
		EbeanServer server = EbeanServerFactory.create(serverConfig);
		if (server == null) {
			System.exit(1);
		}
		LOGGER.info("Database started");
		return true;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean stop() {
		LOGGER.info("Stopping Database");
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
		LOGGER.info("Database shutdown");
		return true;
	}
	
	@Override
	public void deleteDatabase() {
		schemaManager.deleteDatabase();
	}

}
