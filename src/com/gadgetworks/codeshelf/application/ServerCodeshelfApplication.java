/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.6 2013/02/27 01:17:03 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketServer;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication implements ICodeshelfApplication {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private boolean							mIsRunning	= true;
	private IEdiProcessor					mEdiProcessor;
	private IWebSocketServer				mWebSocketServer;
	private IDaoProvider					mDaoProvider;
	private IHttpServer						mHttpServer;
	private IDatabase						mDatabase;
	private IUtil							mUtil;
	private Thread							mShutdownHookThread;
	private Runnable						mShutdownRunnable;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>				mFacilityDao;
	private ITypedDao<User>					mUserDao;

	@Inject
	public ServerCodeshelfApplication(final IWebSocketServer inWebSocketServer,
		final IDaoProvider inDaoProvider,
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IDatabase inDatabase,
		final IUtil inUtil,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final ITypedDao<User> inUserDao) {
		mWebSocketServer = inWebSocketServer;
		mDaoProvider = inDaoProvider;
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mDatabase = inDatabase;
		mUtil = inUtil;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mUserDao = inUserDao;
	}

	// --------------------------------------------------------------------------
	/**
	 * Setup the JVM environment and the SWT shell.
	 */
	private void setupLibraries() {

		// Set a class loader that can access the classpath when searching for resources.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		//System.loadLibrary("jd2xx");

		LOGGER.warn("CodeShelf version: " + mUtil.getVersionString());
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
		mDatabase.start();
		LOGGER.info("Database started");

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		initializeApplicationData();

		// Start the WebSocket UX handler
		mWebSocketServer.start();

		// Start the ActiveMQ test server if required.
		//		property = mPersistentPropertyDao.findById(PersistentProperty.ACTIVEMQ_RUN);
		//		if ((property != null) && (property.getCurrentValueAsBoolean())) {
		//			ActiveMqManager.startBrokerService();
		//		}
		//
		//		// Start the JMS message handler.
		//		JmsHandler.startJmsHandler();

		Organization organization = mOrganizationDao.findByDomainId(null, "O1");
		if (organization != null) {
			User user = organization.getUser("jeffw@gadgetworks.com");
			if (user != null) {
				if (user.isPasswordValid("blahdeeblah")) {
					LOGGER.info("Password is valid");
				}
			}
			Facility facility = mFacilityDao.findByDomainId(organization, "F1");
			if (facility != null) {
				facility.logLocationDistances();
			}
		}

		mEdiProcessor.startProcessor();

		mHttpServer.startServer();

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

		mHttpServer.stopServer();

		mEdiProcessor.stopProcessor();

		//		ActiveMqManager.stopBrokerService();

		//		for (ITypedDao<IDomainObject> dao : mDaoProvider.getAllDaos()) {
		//			dao.removeDAOListeners();
		//		}

		// Stop the web socket manager.
		try {
			mWebSocketServer.stop();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}

		mDatabase.stop();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

	}

	private void initPreferencesStore(Organization inOrganization) {
		initPreference(inOrganization, PersistentProperty.FORCE_CHANNEL, "Preferred wireless channel", RadioController.NO_PREFERRED_CHANNEL_TEXT);
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
			property = new PersistentProperty();
			property.setParent(inOrganization);
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

		// Create some demo organizations.
		createOrganzation("O1");
		createOrganzation("O2");
		createOrganzation("O3");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private void createOrganzation(String inOrganizationId) {
		Organization organization = mOrganizationDao.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization();
			organization.setDomainId(inOrganizationId);
			try {
				mOrganizationDao.store(organization);
			} catch (DaoException e) {
				e.printStackTrace();
			}

			// Create a user for the organization.
			User user = new User();
			user.setParent(organization);
			user.setDomainId("jeffw@gadgetworks.com");
			user.setEmail("jeffw@gadgetworks.com");
			user.setPassword("blahdeeblah");
			user.setActive(true);

			try {
				mUserDao.store(user);
			} catch (DaoException e) {
				e.printStackTrace();
			}
		}
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
