/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfApplication.java,v 1.19 2012/03/18 09:03:39 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;

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
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GWEbeanNamingConvention;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork.ICodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.DBProperty.IDBPropertyDao;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.Organization.IOrganizationDao;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty.IPersistentPropertyDao;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.model.persist.User.IUserDao;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.google.inject.Inject;

public final class CodeShelfApplication implements ICodeShelfApplication {

	private static final Log			LOGGER		= LogFactory.getLog(CodeShelfApplication.class);

	private boolean						mIsRunning	= true;
	private IDaoRegistry				mDaoRegistry;
	private IController					mController;
	@SuppressWarnings("unused")
	private WirelessDeviceEventHandler	mWirelessDeviceEventHandler;
	private IWebSocketListener			mWebSocketListener;
	private IUserDao					mUserDao;
	private IOrganizationDao			mOrganizationDao;
	private IWirelessDeviceDao			mWirelessDeviceDao;
	private IPersistentPropertyDao		mPersistentPropertyDao;
	private ICodeShelfNetworkDao		mCodeShelfNetworkDao;
	private IDBPropertyDao				mDBPropertyDao;
	private Thread						mShutdownHookThread;
	private Runnable					mShutdownRunnable;

	@Inject
	public CodeShelfApplication(final IDaoRegistry inDaoRegistry,
		final IWebSocketListener inWebSocketManager,
		final IUserDao inUserDao,
		final IOrganizationDao inOrganizationDao,
		final IWirelessDeviceDao inWirelessDeviceDao,
		final IPersistentPropertyDao inPersistentPropertyDao,
		final ICodeShelfNetworkDao inCodeShelfNetworkDao,
		final IDBPropertyDao inDBPropertyDao) {
		mDaoRegistry = inDaoRegistry;
		mWebSocketListener = inWebSocketManager;
		mUserDao = inUserDao;
		mOrganizationDao = inOrganizationDao;
		mWirelessDeviceDao = inWirelessDeviceDao;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mCodeShelfNetworkDao = inCodeShelfNetworkDao;
		mDBPropertyDao = inDBPropertyDao;
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

		// Create two dummy users for testing.
		createUser("1234", "passowrd");
		createUser("12345", null);

		// Some radio device fields have no meaning from the last invocation of the application.
		for (WirelessDevice wirelessDevice : mWirelessDeviceDao.getAll()) {
			LOGGER.debug("Init data for wireless device id: " + wirelessDevice.getMacAddress());
			wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.INVALID);
			try {
				mWirelessDeviceDao.store(wirelessDevice);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param userID
	 * @param password
	 */
	private void createUser(String userID, String password) {
		Organization organization = mOrganizationDao.findById(userID);
		if (organization == null) {
			organization = new Organization();
			organization.setId(userID);
			try {
				mOrganizationDao.store(organization);
			} catch (DaoException e) {
				e.printStackTrace();
			}
		}

		User user = mUserDao.findById(userID);
		if (user == null) {
			user = new User();
			user.setActive(true);
			user.setId(userID);
			if (password != null) {
				user.setHashedPassword(password);
			}
			user.setParentOrganization(organization);
			try {
				mUserDao.store(user);
			} catch (DaoException e) {
				e.printStackTrace();
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

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("Process info: " + processName);

		installShutdownHook();

		LOGGER.info("Starting database");
		startEmbeddedDB();
		LOGGER.info("Database started");

		PreferencesStore.initPreferencesStore(mPersistentPropertyDao);

		Util.setLoggingLevelsFromPrefs(mPersistentPropertyDao);

		List<IWirelessInterface> interfaceList = new ArrayList<IWirelessInterface>();
		// Create a CodeShelf interface for each CodeShelf network we have.
		for (CodeShelfNetwork network : mCodeShelfNetworkDao.getAll()) {
			SnapInterface snapInterface = new SnapInterface(network, mCodeShelfNetworkDao, mWirelessDeviceDao);
			network.setWirelessInterface(snapInterface);
			interfaceList.add(snapInterface);
		}
		mController = new CodeShelfController(mWirelessDeviceDao, interfaceList);

		mWirelessDeviceEventHandler = new WirelessDeviceEventHandler(mController, mWirelessDeviceDao);

		// Start the WebSocket UX handler
		mWebSocketListener.start();

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

		// Remove all listeners from all of the DAOs.
		for (IDao dao : mDaoRegistry.getDaoList()) {
			dao.removeDAOListeners();
		}

		// Stop the web socket manager.
		mWebSocketListener.stop();

		// First shutdown the FlyWeight controller if there is one.
		mController.stopController();

		stopEmbeddedDB();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

		LogFactory.releaseAll();
		LogManager.shutdown();
	}

	/* --------------------------------------------------------------------------
	 * Get the preferred channel from the preferences store.
	 */
	public byte getPreferredChannel() {
		byte result = 0;
		PersistentProperty preferredChannelProp = mPersistentPropertyDao.findById(PersistentProperty.FORCE_CHANNEL);
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

		//		try {
		//			Connection connection = DriverManager.getConnection(Util.getApplicationDatabaseURL(), "codeshelf", "codeshelf");
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
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void validateDatabase() {

		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		DBProperty dbVersionProp = mDBPropertyDao.findById(DBProperty.DB_SCHEMA_VERSION);
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

	// --------------------------------------------------------------------------
	/**
	 */
	private void installShutdownHook() {
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
					// Set the class loader for this thread, so we can get stuff out of our own JARs.
					//shutdownThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
					shutdownThread.start();
					long time = System.currentTimeMillis();
					// Wait until the shutdown thread succeeds, but not more than 20 sec.
					while ((mIsRunning) && ((System.currentTimeMillis() - time) < 20000)) {
						Thread.sleep(1000);
					}
					System.out.println("Shutdown signal handled");
				} catch (Exception e) {
					System.out.println("Shutdown signal exception:" + e);
					e.printStackTrace();
				}
			}
		};
		mShutdownHookThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
		Runtime.getRuntime().addShutdownHook(mShutdownHookThread);
	}
}
