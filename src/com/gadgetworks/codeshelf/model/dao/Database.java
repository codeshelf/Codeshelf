/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Database.java,v 1.1 2012/10/11 09:04:36 jeffw Exp $
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
		System.setProperty("app.database.url", mUtil.getApplicationDatabaseURL());
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
		dataSourceConfig.setUsername("codeshelf");
		dataSourceConfig.setPassword("codeshelf");
		dataSourceConfig.setUrl(mUtil.getApplicationDatabaseURL());
		dataSourceConfig.setDriver("org.h2.Driver");
		dataSourceConfig.setMinConnections(1);
		dataSourceConfig.setMaxConnections(25);
		dataSourceConfig.setIsolationLevel(Transaction.READ_COMMITTED);
		dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		// Setup the EBean server configuration.
		ServerConfig config = new ServerConfig();
		config.setName("h2");
		//		config.loadFromProperties();
		config.setDataSourceConfig(dataSourceConfig);
		config.setNamingConvention(new GWEbeanNamingConvention());
		config.setDefaultServer(true);
		config.setDebugSql(false);
		config.setLoggingToJavaLogger(false);
		config.setResourceDirectory(mUtil.getApplicationDataDirPath());
		config.setDebugLazyLoad(true);
		config.setDebugSql(false);
		config.setLoggingLevel(LogLevel.SUMMARY);
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

		// The H2 database has a serious problem with deleting temp files for LOBs.  We have to do it ourselves, or it will grow without bound.
		String[] extensions = { "temp.lob.db" };
		boolean recursive = true;

		File dbDir = new File(mUtil.getApplicationDataDirPath());
		@SuppressWarnings("unchecked")
		Collection<File> files = FileUtils.listFiles(dbDir, extensions, recursive);
		for (File file : files) {
			if (file.delete()) {
				LOGGER.debug("Deleted temporary LOB file = " + file.getPath());
			}
		}

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
