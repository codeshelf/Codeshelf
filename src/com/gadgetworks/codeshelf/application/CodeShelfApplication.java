/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfApplication.java,v 1.14 2012/03/16 15:59:09 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.LogLevel;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.gadgetworks.codeshelf.controller.CodeShelfController;
import com.gadgetworks.codeshelf.controller.ControllerABC;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.controller.SnapInterface;
import com.gadgetworks.codeshelf.model.PreferencesStore;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.dao.DaoManager;
import com.gadgetworks.codeshelf.model.dao.GWEbeanNamingConvention;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websocket.WebSocketManager;

public final class CodeShelfApplication {

	private static final Log			LOGGER		= LogFactory.getLog(CodeShelfApplication.class);

	private static boolean				isDBStarted;

	private boolean						mIsRunning	= true;
	private IController					mController;
	@SuppressWarnings("unused")
	private WirelessDeviceEventHandler	mWirelessDeviceEventHandler;
	private WebSocketManager			mWebSocketManager;
	private Thread						mShutdownHookThread;
	private Runnable					mShutdownRunnable;

	public CodeShelfApplication() {

		// Prepare the shutdown hook.
		mShutdownRunnable = new Runnable() {
			public void run() {
				// Only execute this hook if the application is still running at (external) shutdown.
				// (This is to help where the shutdown is done externally and not through our own means.)
				if (mIsRunning) {
					stopApplication();
				}
			}
		};
		mShutdownHookThread = new Thread() {
			public void run() {
				try {
					LOGGER.info("Shutdown signal received");
					// Start the shutdown thread to cleanup and shutdown everything in an orderly manner.
					Thread shutdownThread = new Thread(mShutdownRunnable);
					shutdownThread.start();
					// Indicate that we're shutdown.
					Thread.sleep(1000 * 7);
					System.out.println("Shutdown signal handled");
				} catch (Exception e) {
					System.out.println("Shutdown signal exception:" + e);
					e.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(mShutdownHookThread);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IController getController() {
		return mController;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	private void initializeApplicationData() {

		// Some radio device fields have no meaning from the last invocation of the application.
		for (WirelessDevice wirelessDevice : WirelessDevice.DAO.getAll()) {
			LOGGER.debug("Init data for wireless device id: " + wirelessDevice.getMacAddress());
			wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.INVALID);
			try {
				WirelessDevice.DAO.store(wirelessDevice);
			} catch (DAOException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Setup the JVM environment and the SWT shell.
	 */
	private void setupLibraries() {

		// Set a class loader that can access the classpath when searching for resources.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		//System.loadLibrary("jd2xx");

		LOGGER.warn("CodeShelf version: " + Util.getVersionString());
		LOGGER.info("user.dir = " + System.getProperty("user.dir"));
		LOGGER.info("java.class.path = " + System.getProperty("java.class.path"));
		LOGGER.info("java.library.path = " + System.getProperty("java.library.path"));

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startApplication() {

		setupLibraries();

		LOGGER.info("Starting database");
		startEmbeddedDB();
		LOGGER.info("Database isDBStarted");
		isDBStarted = true;

		PreferencesStore.initPreferencesStore();

		Util.setLoggingLevelsFromPrefs();

		List<IWirelessInterface> interfaceList = new ArrayList<IWirelessInterface>();
		// Create a CodeShelf interface for each CodeShelf network we have.
		for (CodeShelfNetwork network : CodeShelfNetwork.DAO.getAll()) {
			SnapInterface snapInterface = new SnapInterface(network);
			network.setWirelessInterface(snapInterface);
			interfaceList.add(snapInterface);
		}
		mController = new CodeShelfController(WirelessDevice.DAO, interfaceList);

		mWirelessDeviceEventHandler = new WirelessDeviceEventHandler(mController);
		//		mServerConnectionManager = new FacebookConnectionManager(mController);

		// Start the WebSocket UX handler
		WebSessionManager webSessionManager = new WebSessionManager();
		mWebSocketManager = new WebSocketManager(webSessionManager);
		mWebSocketManager.start();

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		initializeApplicationData();

		// Start the ActiveMQ test server if required.
		//		property = PersistentProperty.DAO.findById(PersistentProperty.ACTIVEMQ_RUN);
		//		if ((property != null) && (property.getCurrentValueAsBoolean())) {
		//			ActiveMqManager.startBrokerService();
		//		}
		//
		//		// Start the JMS message handler.
		//		JmsHandler.startJmsHandler();

		byte preferredChannel = getPreferredChannel();

		// Start the background startup and wait until it's finished.
		LOGGER.info("Starting controller");
		mController.startController(preferredChannel);

		// Initialize the TTS system.
		// (Do it on a thread, so we don't pause the start of the application.)
		//		Runnable runner = new Runnable() {
		//			public void run() {
		//				//TextToAudioFreeTTS.initVoiceSystem();
		//			}
		//		};
		//		new Thread(runner).start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stopApplication() {

		LOGGER.info("Stopping application");

		//		ActiveMqManager.stopBrokerService();

		// Remove all listeners from the DAO.
		DaoManager.gDaoManager.removeDAOListeners();

		// Stop the web socket manager.
		mWebSocketManager.stop();

		// First shutdown the FlyWeight controller if there is one.
		mController.stopController();

		// If the AWT thread came up (Smack debugger windows is AWT) then we have no choice, but to force a shutdown.
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		int count = group.activeCount();
		Thread[] threadArray = new Thread[count];
		group.enumerate(threadArray);
		for (int i = 0; i < threadArray.length; i++) {
			if ((threadArray[i] != null) && (threadArray[i].getName().equals("AWT-Shutdown"))) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				stopEmbeddedDB();
				LOGGER.info("Application terminated with hung threads");
				Util.exitSystem();
			}
		}

		stopEmbeddedDB();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");
	}

	/* --------------------------------------------------------------------------
	 * Get the preferred channel from the preferences store.
	 */
	public byte getPreferredChannel() {
		byte result = 0;
		PersistentProperty preferredChannelProp = PersistentProperty.DAO.findById(PersistentProperty.FORCE_CHANNEL);
		if (preferredChannelProp != null) {
			if (ControllerABC.NO_PREFERRED_CHANNEL_TEXT.equals(preferredChannelProp.getCurrentValueAsStr())) {
				result = ControllerABC.NO_PREFERRED_CHANNEL;
			} else {
				result = (byte) preferredChannelProp.getCurrentValueAsInt();
			}
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * Handle the SWT application/UI events.
	 */
	public void handleEvents() {

		while (mIsRunning) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			} catch (RuntimeException inRuntimeException) {
				// We have to catch RuntimeExceptions, because SWT natives do throw them sometime and then don't handle them.
				LOGGER.error("Caught runtime exception", inRuntimeException);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void startEmbeddedDB() {

		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		String appDataDir = Util.getApplicationDataDirPath();
		// Set Derby's base directory to the application's data directory.
		//System.setProperty("derby.system.home", appDataDir);
		//String origUserHome = System.getProperty("user.home");
		System.setProperty("app.data.dir", appDataDir);
		System.setProperty("app.database.url", Util.getApplicationDatabaseURL());
		System.setProperty("ebean.props.file", "conf/ebean.properties");
		System.setProperty("java.util.logging.config.file", "conf/logging.properties");

		File appDir = new File(appDataDir);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				LOGGER.error("", e);
				Util.exitSystem();
			}
		}

		File dataDir = new File(appDataDir + System.getProperty("file.separator") + "db");
		if (!dataDir.exists()) {
			try {
				dataDir.mkdir();
			} catch (SecurityException e) {
				LOGGER.error("", e);
				Util.exitSystem();
			}
		}

		ISchemaManager schemaManager = new H2SchemaManager();
		if (!schemaManager.doesSchemaExist()) {
			if (!schemaManager.creatNewSchema()) {
				LOGGER.error("Cannot create DB schema");
				Util.exitSystem();
			}
		}

		// Setup the EBean server configuration.
		ServerConfig config = new ServerConfig();
		config.setName("h2");
		config.loadFromProperties();
		config.setNamingConvention(new GWEbeanNamingConvention());
		config.setDefaultServer(true);
		config.setDebugSql(false);
		config.setLoggingLevel(LogLevel.NONE);
		//		config.setLoggingLevelQuery(LogLevelStmt.NONE);
		//		config.setLoggingLevelSqlQuery(LogLevelStmt.NONE);
		//		config.setLoggingLevelIud(LogLevelStmt.NONE);
		//		config.setLoggingLevelTxnCommit(LogLevelTxnCommit.DEBUG);
		config.setLoggingToJavaLogger(true);
		config.setResourceDirectory(Util.getApplicationDataDirPath());
		EbeanServer server = EbeanServerFactory.create(config);
		if (server == null) {
			Util.exitSystem();
		}

		// The H2 database has a serious problem with deleting temp files for LOBs.  We have to do it ourselves, or it will grow without bound.
		String[] extensions = { "temp.lob.db" };
		boolean recursive = true;

		File dbDir = new File(Util.getApplicationDataDirPath());
		@SuppressWarnings("unchecked")
		Collection<File> files = FileUtils.listFiles(dbDir, extensions, recursive);
		for (File file : files) {
			if (file.delete()) {
				LOGGER.debug("Deleted temporary LOB file = " + file.getPath());
			}
		}

		validateDatabase();

		LOGGER.info("Database started");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void stopEmbeddedDB() {
		LOGGER.info("Stopping DAO");

		// Shutdown HSQLDB
		// org.hsqldb.DatabaseManager.closeDatabases(0);

		// Shutdown Derby
		//		try {
		//			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		//		} catch (SQLException inException) {
		//			
		//		}
		try {
			Connection connection = DriverManager.getConnection(Util.getApplicationDatabaseURL(), "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			stmt.execute("SHUTDOWN COMPACT");
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			LOGGER.error("", e);
		}

		ShutdownManager.shutdown();

		LOGGER.info("Database shutdown");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void validateDatabase() {

		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		DBProperty dbVersionProp = DBProperty.DAO.findById(DBProperty.DB_SCHEMA_VERSION);
		if (dbVersionProp == null) {
			// No database schema version has been set yet, so set it to the current schema version.
			dbVersionProp = new DBProperty();
			dbVersionProp.setId(DBProperty.DB_SCHEMA_VERSION);
			dbVersionProp.setValueStr(Integer.toString(ISchemaManager.DATABASE_VERSION_CUR));
			Ebean.save(dbVersionProp);
		} else {
			// The database schema version is set, so make sure that we're compatible.
			String dbVersion = dbVersionProp.getValueStr();
			int verInt = Integer.decode(dbVersion);
			if (verInt < ISchemaManager.DATABASE_VERSION_CUR) {
				ISchemaManager schemaManager = new H2SchemaManager();
				schemaManager.upgradeSchema(verInt, ISchemaManager.DATABASE_VERSION_CUR);
				dbVersionProp.setValueStr(Integer.toString(ISchemaManager.DATABASE_VERSION_CUR));
				Ebean.save(dbVersionProp);
			} else if (verInt > ISchemaManager.DATABASE_VERSION_CUR) {
				// We don't actually support downgrading a DB.
				//				ISchemaManager schemaManager = new H2SchemaManager();
				//				schemaManager.downgradeSchema(verInt, DATABASE_VERSION_CUR);
				//				dbVersionProp.setValueStr(Integer.toString(DATABASE_VERSION_CUR));
				//				Ebean.save(dbVersionProp);
			}
		}
	}
}
