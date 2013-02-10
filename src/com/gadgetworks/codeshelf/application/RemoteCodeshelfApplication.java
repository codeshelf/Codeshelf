/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: RemoteCodeshelfApplication.java,v 1.1 2013/02/10 01:11:41 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.controller.CodeShelfController;
import com.gadgetworks.codeshelf.controller.ControllerABC;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.controller.SnapInterface;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.google.inject.Inject;

public final class RemoteCodeshelfApplication implements ICodeShelfApplication {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(RemoteCodeshelfApplication.class);

	private boolean							mIsRunning	= true;
	private List<IController>				mControllerList;
	private WirelessDeviceEventHandler		mWirelessDeviceEventHandler;
	private IDatabase						mDatabase;
	private IUtil							mUtil;
	private Thread							mShutdownHookThread;
	private Runnable						mShutdownRunnable;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>				mFacilityDao;
	private IWirelessDeviceDao				mWirelessDeviceDao;
	private ITypedDao<User>					mUserDao;

	@Inject
	public RemoteCodeshelfApplication(final IDatabase inDatabase,
		final IUtil inUtil,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final IWirelessDeviceDao inWirelessDeviceDao,
		final ITypedDao<User> inUserDao) {
		mDatabase = inDatabase;
		mUtil = inUtil;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mWirelessDeviceDao = inWirelessDeviceDao;
		mUserDao = inUserDao;
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

		Collection<Organization> organizations = mOrganizationDao.getAll();
		for (Organization organization : organizations) {
			initPreferencesStore(organization);
			mUtil.setLoggingLevelsFromPrefs(organization, mPersistentPropertyDao);
			for (Facility facility : organization.getFacilities()) {

				List<IWirelessInterface> interfaceList = new ArrayList<IWirelessInterface>();
				// Create a CodeShelf interface for each CodeShelf network we have.
				for (CodeShelfNetwork network : facility.getNetworks()) {
					SnapInterface snapInterface = new SnapInterface(network, mWirelessDeviceDao);
					network.setWirelessInterface(snapInterface);
					interfaceList.add(snapInterface);
				}

				mControllerList.add(new CodeShelfController(interfaceList, facility, mWirelessDeviceDao));
			}
		}

		mWirelessDeviceEventHandler = new WirelessDeviceEventHandler(mControllerList, mWirelessDeviceDao);

		// Start the controllers.
		LOGGER.info("Starting controllers");
		for (IController controller : mControllerList) {
			controller.startController();
		}

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
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stopApplication() {

		LOGGER.info("Stopping application");

		// Shutdown the controllers
		for (IController controller : mControllerList) {
			controller.stopController();
		}

		mDatabase.stop();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

	}

	private void initPreferencesStore(Organization inOrganization) {
		initPreference(inOrganization, PersistentProperty.FORCE_CHANNEL, "Preferred wireless channel", ControllerABC.NO_PREFERRED_CHANNEL_TEXT);
		initPreference(inOrganization, PersistentProperty.GENERAL_INTF_LOG_LEVEL, "Preferred general log level", Level.INFO.toString());
		initPreference(inOrganization, PersistentProperty.GATEWAY_INTF_LOG_LEVEL, "Preferred gateway log level", Level.INFO.toString());
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

		// Create two dummy users for testing.
		createOrganzation("O1");
		createOrganzation("O2");
		createOrganzation("O3");

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
