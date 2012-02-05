/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfApplication.java,v 1.12 2012/02/05 08:41:31 jeffw Exp $
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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

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
import com.gadgetworks.codeshelf.ui.CodeShelfNetworkMgrWindow;
import com.gadgetworks.codeshelf.ui.LocaleUtils;
import com.gadgetworks.codeshelf.ui.preferences.Preferences;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websocket.WebSocketManager;

public final class CodeShelfApplication {

	private static final Log			LOGGER		= LogFactory.getLog(CodeShelfApplication.class);

	private static boolean				isDBStarted;

	private boolean						mIsRunning	= true;
	private Display						mDisplay;
	private CodeShelfNetworkMgrWindow	mCodeShelfApplicationWindow;
	private Tray						mSystemTray;
	private TrayItem					mSystemTrayItem;
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
				stopApplication();
			}
		};
		mShutdownHookThread = new Thread() {
			public void run() {
				try {
					LOGGER.info("Shutdown signal received");
					// We can't syncExec() here on Windows, so we instead sleep in the next statement.
					Display.getDefault().asyncExec(mShutdownRunnable);
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

		Display.setAppName(LocaleUtils.getStr("application.name"));
		DeviceData data = new DeviceData();
		data.tracking = true;
		String debugStr = System.getProperty("com.gadgetworks.memorydebug");
		if ((debugStr != null) && (debugStr.equalsIgnoreCase("true"))) {
			mDisplay = new Display(data);
			Sleak sleak = new Sleak();
			sleak.open();
		} else {
			mDisplay = new Display();
		}

		// Start the background startup and wait until it's finished.
		Thread starterThread = new Thread(new StartThread());
		// Set the class loader for this thread, so we can get stuff out of our own JARs.
		starterThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
		// Start the thread.
		starterThread.start();

		// While waiting for background startup, we process SWT events, so the application is active.
		while (!isDBStarted) {
			if (!mDisplay.readAndDispatch())
				try {
					Thread.sleep(500);
				} catch (InterruptedException inException) {
					LOGGER.error("", inException);
				}
		}

		PreferencesStore.initPreferencesStore();

		Util.setLoggingLevelsFromPrefs();

		// Start the console and then check to see if the user wants it to open at startup.
		Shell shell = new Shell(mDisplay);
		Util.startConsole(shell, this);
		PersistentProperty property = PersistentProperty.DAO.findById(PersistentProperty.SHOW_CONSOLE_PREF);
		if ((property != null) && (property.getCurrentValueAsBoolean()))
			Util.openConsole();

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

		launchNewApplicationWindow();

		//setupSystemTray();

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

		// Start the event harvester background processing.
		//EventHarvester.startEventHarvester(mController);

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

		if (mCodeShelfApplicationWindow != null) {
			mCodeShelfApplicationWindow.close();
		}

		if (mSystemTrayItem != null) {
			mSystemTrayItem.dispose();
			mSystemTray.dispose();
		}

		//		ActiveMqManager.stopBrokerService();

		Util.closeConsole();

		// Remove all listeners from the DAO.
		DaoManager.gDaoManager.removeDAOListeners();

		// Stop the event harvester
		//EventHarvester.stopEventHarvester();

		// Stop the web socket manager.
		mWebSocketManager.stop();

		// First shutdown the FlyWeight controller if there is one.
		mController.stopController();

		Util.stopConsole();

		// Stop/close any shells (mostly modal) that are still lurking.
		for (Shell shell : mDisplay.getShells()) {
			shell.dispose();
		}

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
	 * Edit the application preferences.
	 */
	public void editPreferences() {
		int result = Preferences.editPreferences();
		if (result == Window.OK) {
			// The user may have changed logging levels.
			Util.setLoggingLevelsFromPrefs();

			// The user may have changed the preferred channel.
			mController.setRadioChannel(getPreferredChannel());

			// The user may have changed the ActiveMQ settings.
			//ActiveMqManager.setServerFromSettings();
		}
	}

	/* --------------------------------------------------------------------------
	 * Launch a new application window.
	 */
	private void launchNewApplicationWindow() {

		if (mCodeShelfApplicationWindow != null) {
			Shell shell = mCodeShelfApplicationWindow.getShell();
			if (shell == null) {
				mCodeShelfApplicationWindow = null;
			} else if (shell.isDisposed()) {
				mCodeShelfApplicationWindow = null;
			}
		}

		if (mCodeShelfApplicationWindow == null) {
			// Setup the main application window.
			Shell shell = new Shell(mDisplay);
			mCodeShelfApplicationWindow = new CodeShelfNetworkMgrWindow(shell, this);
			shell.pack();
			mCodeShelfApplicationWindow.setBlockOnOpen(false);
			mCodeShelfApplicationWindow.open();
		} else {
		}

		if (mCodeShelfApplicationWindow != null) {
			mCodeShelfApplicationWindow.getShell().forceActive();
			mCodeShelfApplicationWindow.getShell().forceFocus();
		}

	}

	/* --------------------------------------------------------------------------
	 * Handle the SWT application/UI events.
	 */
	public void handleEvents() {

		// We have to do this, because SWT stopped supporting STANDARD window behavior in OS X.
		mDisplay.addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event inEvent) {

				if ((inEvent.stateMask == SWT.MOD1) && ((inEvent.character == 'w') || inEvent.character == SWT.F4)) {
					// Try to find a shell somewhere in the parentage of the thing that has keyboard focus.
					if (inEvent.widget instanceof Control) {
						Control control = (Control) inEvent.widget;
						while ((control != null) && (!(control instanceof Shell))) {
							control = control.getParent();
						}

						// We found the shell, so close it.
						if (control != null) {
							// Add a listener that makes all of the windows have cmd-w behavior on OS X.
							Shell shell = (Shell) control;
							shell.close();
							inEvent.doit = false;
						}
					}
				}

			}
		});

		while (mIsRunning) {
			try {
				if ((mDisplay != null) && (!mDisplay.isDisposed())) {
					if (!mDisplay.readAndDispatch()) {
						mDisplay.sleep();
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							LOGGER.error("", e);
						}
					} else {

					}
				}
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

	// --------------------------------------------------------------------------
	/**
	 */
	private void setupSystemTray() {

		mSystemTray = mDisplay.getSystemTray();
		mSystemTrayItem = new TrayItem(mSystemTray, SWT.NONE);
		mSystemTrayItem.setToolTipText(LocaleUtils.getStr("application.name"));
		//		mSystemTrayItem.addListener(SWT.Show, new Listener() {
		//			public void handleEvent(Event inEvent) {
		//				LOGGER.info("system tray show");
		//			}
		//		});
		//		mSystemTrayItem.addListener(SWT.Hide, new Listener() {
		//			public void handleEvent(Event inEvent) {
		//				LOGGER.info("system tray hide");
		//			}
		//		});
		mSystemTrayItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				LOGGER.info("system tray selection");
				launchNewApplicationWindow();
			}
		});
		//		mSystemTrayItem.addListener(SWT.DefaultSelection, new Listener() {
		//			public void handleEvent(Event inEvent) {
		//				LOGGER.info("system tray default selection");
		//			}
		//		});

		Shell shell = new Shell(mDisplay);
		final Menu menu = new Menu(shell, SWT.POP_UP);

		// About menu item.
		MenuItem mi = new MenuItem(menu, SWT.PUSH);
		mi.setText(LocaleUtils.getStr("system.tray.about"));
		mi.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				Util.showAbout();
			}
		});

		// Preferences menu item.
		mi = new MenuItem(menu, SWT.PUSH);
		mi.setText(LocaleUtils.getStr("system.tray.preferences"));
		mi.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				editPreferences();
			}
		});

		// Open control menu item.
		mi = new MenuItem(menu, SWT.PUSH);
		mi.setText(LocaleUtils.getStr("system.tray.open_window"));
		mi.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				// Open the CodeShelfApplicationWindow if it's not already open.
				launchNewApplicationWindow();
			}
		});
		menu.setDefaultItem(mi);

		// Exit menu item (only on Windows).
		if (Util.isWindows()) {
			mi = new MenuItem(menu, SWT.PUSH);
			mi.setText(LocaleUtils.getStr("system.tray.exit"));
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event inEvent) {
					// Exit the application.
					stopApplication();
				}
			});
		}

		mSystemTrayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event inEvent) {
				menu.setVisible(true);
			}
		});
		updateSystemTrayImage();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSystemTrayImage
	 */
	public void updateSystemTrayImage() {
		if (mSystemTrayItem != null) {
			//			if (Util.getSystemDAO().getCardPendingUpdateCount() == 0) {
			//				mSystemTrayItem.setImage(Util.getImageRegistry().get(Util.SYSTEM_TRAY_ICON_CLEAR));
			//			} else {
			//				mSystemTrayItem.setImage(Util.getImageRegistry().get(Util.SYSTEM_TRAY_ICON_UPDATES));
			//			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  This class runs embedded DB start-up in the background, so that SWT can handle events and report progress.
	 *  @author jeffw
	 */
	private class StartThread implements Runnable {

		public void run() {

			LOGGER.info("Starting database");
			startEmbeddedDB();
			LOGGER.info("Database isDBStarted");
			isDBStarted = true;
		}
	}
}
