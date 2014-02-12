/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.monitor.IMonitor;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketServer;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor					mEdiProcessor;
	private IWebSocketServer				mWebSocketServer;
	private IHttpServer						mHttpServer;
	private IPickDocumentGenerator			mPickDocumentGenerator;
	private IDatabase						mDatabase;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>				mFacilityDao;
	private ITypedDao<User>					mUserDao;

	private IMonitor						mMonitor;

	private BlockingQueue<String>			mEdiProcessSignalQueue;

	@Inject
	public ServerCodeshelfApplication(final IWebSocketServer inWebSocketServer,
		final IMonitor inMonitor,
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IPickDocumentGenerator inPickDocumentGenerator,
		final IDatabase inDatabase,
		final IUtil inUtil,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final ITypedDao<User> inUserDao) {
		super(inUtil);
		mMonitor = inMonitor;
		mWebSocketServer = inWebSocketServer;
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mDatabase = inDatabase;
		mPickDocumentGenerator = inPickDocumentGenerator;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mUserDao = inUserDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.ApplicationABC#doLoadLibraries()
	 */
	@Override
	protected void doLoadLibraries() {
		//System.loadLibrary("jd2xx");
		//System.loadLibrary("libjSSC-0.9_x86_64");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Process info: " + processName);

		//		mMonitor.logToCentralAdmin("Startup: codeshelf server " + processName);

		mDatabase.start();

		// Start the WebSocket UX handler
		mWebSocketServer.start();

		Organization organization = mOrganizationDao.findByDomainId(null, "O1");
		if (organization != null) {
			Facility facility = mFacilityDao.findByDomainId(organization, "F1");
			if (facility != null) {
				facility.logLocationDistances();
				facility.recomputeDdcPositions();

				CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_ID);
				if (network != null) {
					Che che1 = network.getChe("CHE1");
					if (che1 == null) {
						che1 = network.createChe("CHE1", new NetGuid("0x00000003"));
					}
					Che che2 = network.getChe("CHE2");
					if (che2 == null) {
						che2 = network.createChe("CHE2", new NetGuid("0x00000006"));
					}
					LedController ledController1 = network.getLedController("0x00000002");
					if (ledController1 == null) {
						ledController1 = network.createLedController("0x00000002", new NetGuid("0x00000002"));
					}
					ledController1 = network.getLedController("0x00000001");
					if (ledController1 == null) {
						ledController1 = network.createLedController("0x00000001", new NetGuid("0x00000001"));
					}
				}
			}
		}

		// Start the EDI process.
		mEdiProcessSignalQueue = new ArrayBlockingQueue<>(100);
		mEdiProcessor.startProcessor(mEdiProcessSignalQueue);

		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(mEdiProcessSignalQueue);

		mHttpServer.startServer();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		mMonitor.logToCentralAdmin("Shutodwn: codeshelf server " + processName);

		LOGGER.info("Stopping application");

		mHttpServer.stopServer();

		mEdiProcessor.stopProcessor();
		mPickDocumentGenerator.stopProcessor();

		// Stop the web socket manager.
		try {
			mWebSocketServer.stop();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}

		mDatabase.stop();

		LOGGER.info("Application terminated normally");

	}

	private void initPreferencesStore(Organization inOrganization) {
		initPreference(inOrganization,
			PersistentProperty.FORCE_CHANNEL,
			"Preferred wireless channel",
			RadioController.NO_PREFERRED_CHANNEL_TEXT);
		initPreference(inOrganization,
			PersistentProperty.GENERAL_INTF_LOG_LEVEL,
			"Preferred general log level",
			Level.INFO.toString());
		initPreference(inOrganization,
			PersistentProperty.GATEWAY_INTF_LOG_LEVEL,
			"Preferred gateway log level",
			Level.INFO.toString());
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
	protected void doInitializeApplicationData() {

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
				organization.createUser("jeffw@gadgetworks.com", "blahdeeblah");
			} catch (DaoException e) {
				e.printStackTrace();
			}

		}
	}
}
