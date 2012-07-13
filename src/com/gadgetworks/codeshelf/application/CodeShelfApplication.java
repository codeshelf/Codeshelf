/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfApplication.java,v 1.35 2012/07/13 08:08:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GWEbeanNamingConvention;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.Facility;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.google.inject.Inject;

public final class CodeShelfApplication implements ICodeShelfApplication {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(CodeShelfApplication.class);

	private boolean							mIsRunning	= true;
	private List<IController>				mControllerList;
	private WirelessDeviceEventHandler		mWirelessDeviceEventHandler;
	private IWebSocketListener				mWebSocketListener;
	private IDaoProvider					mDaoProvider;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>				mFacilityDao;
	private ITypedDao<Aisle>				mAisleDao;
	private ITypedDao<User>					mUserDao;
	private IWirelessDeviceDao				mWirelessDeviceDao;
	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<CodeShelfNetwork>		mCodeShelfNetworkDao;
	private ITypedDao<DBProperty>			mDBPropertyDao;
	private Thread							mShutdownHookThread;
	private Runnable						mShutdownRunnable;

	@Inject
	public CodeShelfApplication(final IWebSocketListener inWebSocketManager,
		final IDaoProvider inDaoProvider,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final ITypedDao<Aisle> inAisleDao,
		final ITypedDao<User> inUserDao,
		final IWirelessDeviceDao inWirelessDeviceDao,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<CodeShelfNetwork> inCodeShelfNetworkDao,
		final ITypedDao<DBProperty> inDBPropertyDao) {
		mWebSocketListener = inWebSocketManager;
		mDaoProvider = inDaoProvider;
		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mAisleDao = inAisleDao;
		mUserDao = inUserDao;
		mWirelessDeviceDao = inWirelessDeviceDao;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mCodeShelfNetworkDao = inCodeShelfNetworkDao;
		mDBPropertyDao = inDBPropertyDao;
		mControllerList = new ArrayList<IController>();
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
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Process info: " + processName);

		installShutdownHook();

		LOGGER.info("Starting database");
		startEmbeddedDB();
		LOGGER.info("Database started");

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		initializeApplicationData();

		Collection<Organization> organizations = mOrganizationDao.getAll();
		for (Organization organization : organizations) {
			initPreferencesStore(organization, mPersistentPropertyDao);
			Util.setLoggingLevelsFromPrefs(organization, mPersistentPropertyDao);
			for (Facility facility : organization.getFacilities()) {

				List<IWirelessInterface> interfaceList = new ArrayList<IWirelessInterface>();
				// Create a CodeShelf interface for each CodeShelf network we have.
				for (CodeShelfNetwork network : facility.getNetworks()) {
					SnapInterface snapInterface = new SnapInterface(network, mCodeShelfNetworkDao, mWirelessDeviceDao);
					network.setWirelessInterface(snapInterface);
					interfaceList.add(snapInterface);
				}

				mControllerList.add(new CodeShelfController(mWirelessDeviceDao, interfaceList, facility, mPersistentPropertyDao));
			}
		}

		mWirelessDeviceEventHandler = new WirelessDeviceEventHandler(mControllerList, mWirelessDeviceDao);

		// Start the WebSocket UX handler
		mWebSocketListener.start();

		// Start the ActiveMQ test server if required.
		//		property = PersistentProperty.DAO.findById(PersistentProperty.ACTIVEMQ_RUN);
		//		if ((property != null) && (property.getCurrentValueAsBoolean())) {
		//			ActiveMqManager.startBrokerService();
		//		}
		//
		//		// Start the JMS message handler.
		//		JmsHandler.startJmsHandler();

		// Start the background startup and wait until it's finished.
		LOGGER.info("Starting controllers");
		for (IController controller : mControllerList) {
			controller.startController();
		}

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
		//		for (IDao dao : mDaoRegistry.getDaoList()) {
		//			dao.removeDAOListeners();
		//		}
		for (ITypedDao<PersistABC> dao : mDaoProvider.getAllDaos()) {
			dao.removeDAOListeners();
		}

		// Stop the web socket manager.
		mWebSocketListener.stop();

		// Shutdown the controllers
		for (IController controller : mControllerList) {
			controller.stopController();
		}

		stopEmbeddedDB();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

	}

	public void initPreferencesStore(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
		initPreference(inOrganization, PersistentProperty.FORCE_CHANNEL, "Preferred wireless channel", ControllerABC.NO_PREFERRED_CHANNEL_TEXT);
		initPreference(inOrganization, PersistentProperty.GENERAL_INTF_LOG_LEVEL, "Preferred general log level", Level.INFO.toString());
		initPreference(inOrganization, PersistentProperty.GATEWAY_INTF_LOG_LEVEL, "Preferred gateway log level", Level.INFO.toString());
		//		initPreference(PersistentProperty.ACTIVEMQ_RUN, "Run ActiveMQ", String.valueOf(false));
		//		initPreference(PersistentProperty.ACTIVEMQ_USERID, "ActiveMQ User Id", "");
		//		initPreference(PersistentProperty.ACTIVEMQ_PASSWORD, "ActiveMQ Password", "");
		//		initPreference(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM, "ActiveMQ STOMP Portnum", "61613");
		//		initPreference(PersistentProperty.ACTIVEMQ_JMS_PORTNUM, "ActiveMQ JMS Portnum", "61616");
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inPropertyID
	 *  @param inDescription
	 *  @param inDefaultValue
	 */
	private void initPreference(Organization inOrganization, String inPropertyID, String inDescription, String inDefaultValue) {
		boolean shouldUpdate = false;

		// Find the property in the DB.
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inOrganization, inPropertyID);

		// If the property doesn't exist then create it.
		if (property == null) {
			property = new PersistentProperty(mPersistentPropertyDao);
			property.setParentOrganization(inOrganization);
			property.setDomainId(inPropertyID);
			property.setCurrentValueAsStr(inDefaultValue);
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the stored default value doesn't match then change it.
		if (!property.getDefaultValueAsStr().equals(inDefaultValue)) {
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the property changed then we need to persist the change.
		if (shouldUpdate) {
			try {
				mPersistentPropertyDao.store(property);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	private void initializeApplicationData() {

		// Create two dummy users for testing.
		createOrganzation("O1", "F1", "New Facility");
		createOrganzation("O2", "F2", "New Facility");
		createOrganzation("O3", "F3", "New Facility");

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
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private void createOrganzation(String inOrganizationId, String inFacilityId, String inFacilityName) {
		Organization organization = mOrganizationDao.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization(mOrganizationDao);
			organization.setDomainId(inOrganizationId);
			try {
				mOrganizationDao.store(organization);
			} catch (DaoException e) {
				e.printStackTrace();
			}
		}

		//		Facility facility = mFacilityDao.findByDomainId(organization, inFacilityId);
		//		if (facility == null) {
		//			facility = new Facility();
		//			facility.setDomainId(organization, inFacilityId);
		//			facility.setDescription(inFacilityName);
		//			facility.setparentOrganization(organization);
		//			try {
		//				mFacilityDao.store(facility);
		//			} catch (DaoException e) {
		//				LOGGER.error(null, e);
		//			}
		//		}
		//
		//		Aisle aisle = mAisleDao.findByDomainId(facility, inFacilityId);
		//		if (aisle == null) {
		//			aisle = new Aisle();
		//			aisle.setDomainId(facility, "A1");
		//			aisle.setParentLocation(facility);
		//			try {
		//				mAisleDao.store(aisle);
		//			} catch (DaoException e) {
		//				LOGGER.error(null, e);
		//			}
		//		}
		//
		//		aisle = mAisleDao.findByDomainId(facility, inFacilityId);
		//		if (aisle == null) {
		//			aisle = new Aisle();
		//			aisle.setDomainId(facility, "A2");
		//			aisle.setParentLocation(facility);
		//			try {
		//				mAisleDao.store(aisle);
		//			} catch (DaoException e) {
		//				LOGGER.error(null, e);
		//			}
		//		}
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
	private EbeanServer startEmbeddedDB() {

		EbeanServer result = null;

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
		config.setLoggingToJavaLogger(false);
		config.setResourceDirectory(Util.getApplicationDataDirPath());
		EbeanServer server = EbeanServerFactory.create(config);
		if (server == null) {
			Util.exitSystem();
		}

		result = server;

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

		validateDatabase(server);

		LOGGER.info("Database started");

		return result;
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
	private void validateDatabase(final EbeanServer inServer) {

		// Set our class loader to the system classloader, so ebean can find the enhanced classes.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		DBProperty dbVersionProp = mDBPropertyDao.findByDomainId(null, DBProperty.DB_SCHEMA_VERSION);
		if (dbVersionProp == null) {
			// No database schema version has been set yet, so set it to the current schema version.
			dbVersionProp = new DBProperty(mDBPropertyDao);
			dbVersionProp.setDomainId(DBProperty.DB_SCHEMA_VERSION);
			dbVersionProp.setValueStr(Integer.toString(ISchemaManager.DATABASE_VERSION_CUR));
			inServer.save(dbVersionProp);
		} else {
			// The database schema version is set, so make sure that we're compatible.
			String dbVersion = dbVersionProp.getValueStr();
			int verInt = Integer.decode(dbVersion);
			if (verInt < ISchemaManager.DATABASE_VERSION_CUR) {
				ISchemaManager schemaManager = new H2SchemaManager();
				schemaManager.upgradeSchema(verInt, ISchemaManager.DATABASE_VERSION_CUR);
				dbVersionProp.setValueStr(Integer.toString(ISchemaManager.DATABASE_VERSION_CUR));
				inServer.save(dbVersionProp);
			} else if (verInt > ISchemaManager.DATABASE_VERSION_CUR) {
				// We don't actually support downgrading a DB.
				//				ISchemaManager schemaManager = new H2SchemaManager();
				//				schemaManager.downgradeSchema(verInt, DATABASE_VERSION_CUR);
				//				dbVersionProp.setValueStr(Integer.toString(DATABASE_VERSION_CUR));
				//				inServer.save(dbVersionProp);
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
