package com.gadgetworks.codeshelf.platform.services;

import lombok.Getter;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.multitenancy.Tenant;
import com.google.inject.Singleton;

/**
 * @author bheckel
 */
@Singleton
public class PersistencyService implements Service {

	private static final Logger LOGGER	= LoggerFactory.getLogger(PersistencyService.class);

	@Getter
	int port;
	
	@Getter
	String hostName;
	
	@Getter
	String databaseName;

	private String	userId;

	private String	password;

	String schemaName;
	
	public PersistencyService() {
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
//		serverConfig.setLoggingToJavaLogger(true);
//		serverConfig.setLoggingDirectory(mUtil.getApplicationLogDirPath());
		serverConfig.setPackages(new ArrayList<String>(Arrays.asList("com.gadgetworks.codeshelf.model.domain")));
		serverConfig.setJars(new ArrayList<String>(Arrays.asList("server.codeshelf.jar")));
		serverConfig.setUpdateChangesOnly(true);
		serverConfig.setDdlGenerate(false);
		serverConfig.setDdlRun(false);
		//serverConfig.setNamingConvention(new GWEbeanNamingConvention());
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
		//		dataSourceConfig.setHeartbeatSql("select count(*) from dual");

		AutofetchConfig autofetchConfig = serverConfig.getAutofetchConfig();
		autofetchConfig.setMode(AutofetchMode.DEFAULT_OFF);
		autofetchConfig.setLogDirectory(mUtil.getApplicationLogDirPath());
//		autofetchConfig.setUseFileLogging(true);

		EbeanServer server = EbeanServerFactory.create(serverConfig);
		if (server == null) {
			mUtil.exitSystem();
		}
	}
	*/

	public SessionFactory createTenantSessionFactory(Tenant tenant) {
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
	
	@Override
	public final boolean start() {
		boolean result = false;
		// fetch database config from properties file
		this.hostName = System.getProperty("db.address");
		this.port = Integer.parseInt(System.getProperty("db.portnum"));
		this.databaseName = System.getProperty("db.name");
		this.schemaName = System.getProperty("db.schemaname");
		this.userId = System.getProperty("db.userid");
		this.password = System.getProperty("db.password");
		LOGGER.info("Database started");
		result = true;
		return result;
	}

	@Override
	public final boolean stop() {
		boolean result = false;
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
		// ShutdownManager.shutdown();
		result = true;
		LOGGER.info("Database shutdown");
		return result;
	}

	public Session getCurrentSession() {
		return null;
	}

}
