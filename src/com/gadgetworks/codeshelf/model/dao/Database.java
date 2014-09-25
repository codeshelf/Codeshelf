/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Database.java,v 1.16 2013/09/18 00:40:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import lombok.Getter;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class Database {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(Database.class);

	@Getter
	int port;
	
	@Getter
	String hostName;
	
	@Getter
	String databaseName;

	private String	userId;

	private String	password;

	String schemaName;
	
	public Database() {
	}
	
	/*
	@Inject
	public Database(final Util inUtil) {
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

		schemaManager.verifySchema();

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
		//		serverConfig.setLoggingToJavaLogger(true);
		//		serverConfig.setLoggingDirectory(mUtil.getApplicationLogDirPath());
		serverConfig.setPackages(new ArrayList<String>(Arrays.asList("com.gadgetworks.codeshelf.model.domain")));
		serverConfig.setJars(new ArrayList<String>(Arrays.asList("server.codeshelf.jar")));
		serverConfig.setUpdateChangesOnly(true);
		serverConfig.setDdlGenerate(false);
		serverConfig.setDdlRun(false);
		// serverConfig.setNamingConvention(new GWEbeanNamingConvention());
		UnderscoreNamingConvention namingConvetion = new UnderscoreNamingConvention();
		namingConvetion.setSchema(mSchemaManager.getDbSchemaName());
		serverConfig.setNamingConvention(namingConvetion);

		DataSourceConfig dataSourceConfig = serverConfig.getDataSourceConfig();
		dataSourceConfig.setUsername(mSchemaManager.getDbUserId());
		dataSourceConfig.setPassword(mSchemaManager.getDbPassword());
		dataSourceConfig.setUrl(mSchemaManager.getApplicationDatabaseURL());
		dataSourceConfig.setDriver(mSchemaManager.getDriverName());
		dataSourceConfig.setMinConnections(5);
		dataSourceConfig.setMaxConnections(25);
		dataSourceConfig.setIsolationLevel(Transaction.READ_COMMITTED);
		// dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		AutofetchConfig autofetchConfig = serverConfig.getAutofetchConfig();
		autofetchConfig.setMode(AutofetchMode.DEFAULT_OFF);
		autofetchConfig.setLogDirectory(mUtil.getApplicationLogDirPath());
		// autofetchConfig.setUseFileLogging(true);
		
		EbeanServer server = EbeanServerFactory.create(serverConfig);
		if (server == null) {
			mUtil.exitSystem();
		}		
	}
	*/

	private SessionFactory createTenantSessionFactory(Tenant tenant) {
		// ignore tenant and shard for now using static config data
        try {
        	// Shard shard = this.shardingService.getShard(tenant.getShardId());
        	LOGGER.info("Creating session factory for "+tenant);
	    	Configuration configuration = new Configuration().configure("hibernate.tenant.xml");	    	
	    	// String connectionUrl = "jdbc:postgresql://"+shard.getHost()+":"+shard.getPort()+"/shard"+shard.getShardId();
	    	String connectionUrl = "jdbc:postgresql://"+this.hostName+":"+this.port+"/"+this.databaseName;
	    	configuration.setProperty("hibernate.connection.url", connectionUrl);
	    	configuration.setProperty("hibernate.connection.username", this.userId);
	    	configuration.setProperty("hibernate.connection.password", this.password);
	    	//configuration.setProperty("hibernate.connection.username", tenant.getName());
	    	//configuration.setProperty("hibernate.connection.password", tenant.getDbPassword());
	        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
	        SessionFactory factory = configuration.buildSessionFactory(ssrb.build());
	        return factory;
        } catch (Exception ex) {
        	LOGGER.error("SessionFactory creation for "+tenant+" failed:"+ex);
            throw new ExceptionInInitializerError(ex);
        }
	}
	

	// --------------------------------------------------------------------------
	/**
	 */
	public final boolean start() {
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
}
