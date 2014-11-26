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

import lombok.Getter;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.gadgetworks.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.gadgetworks.codeshelf.metrics.DropboxServiceHealthCheck;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.platform.persistence.SchemaManager;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);
	
	private IEdiProcessor					mEdiProcessor;
	private IHttpServer						mHttpServer;
	private IPickDocumentGenerator			mPickDocumentGenerator;
	
	@Getter
	private PersistenceService				persistenceService;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>	mFacilityDao;

	private BlockingQueue<String>			mEdiProcessSignalQueue;

	JettyWebSocketServer webSocketServer;

	private IConfiguration	configuration;
	
	@Inject
	public ServerCodeshelfApplication(
		final IConfiguration configuration,
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IPickDocumentGenerator inPickDocumentGenerator,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<User> inUserDao,
		final AdminServer inAdminServer,
		final JettyWebSocketServer inAlternativeWebSocketServer,
		final PersistenceService persistenceService) {
		super(inAdminServer);
		this.configuration = configuration;
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		webSocketServer = inAlternativeWebSocketServer;
		this.persistenceService = persistenceService;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.ApplicationABC#doLoadLibraries()
	 */
	@Override
	protected void doLoadLibraries() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() throws Exception {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("Process info: " + processName);
		
		this.getPersistenceService().start();
		
		try {
			this.getPersistenceService().beginTenantTransaction();
			this.getPersistenceService().endTenantTransaction();
		} catch (HibernateException e) {
			LOGGER.error("Failed to initialize Hibernate. Server is shutting down.",e);
			Thread.sleep(3000);
			System.exit(1);
		}
		
		// Start the WebSocket server
		webSocketServer.start();

		// Start the EDI process.
		mEdiProcessSignalQueue = new ArrayBlockingQueue<>(100);
		mEdiProcessor.startProcessor(mEdiProcessSignalQueue);

		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(mEdiProcessSignalQueue);

		mHttpServer.startServer();

		startAdminServer(null);
		startTsdbReporter();
		registerSystemMetrics();
		
		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck(persistenceService);
		MetricsService.registerHealthCheck(dbCheck);
		
		ActiveSiteControllerHealthCheck sessionCheck = new ActiveSiteControllerHealthCheck();
		MetricsService.registerHealthCheck(sessionCheck);	
		
		DropboxServiceHealthCheck dbxCheck = new DropboxServiceHealthCheck(mFacilityDao);
		MetricsService.registerHealthCheck(dbxCheck);
		
		// configure baychange housekeeping work instructions
		// TODO: replace with configuration via database table
		String bayChangeWI = configuration.getString("facility.housekeeping.baychange");
		if (bayChangeWI!=null && bayChangeWI.equals("None")) {
			LOGGER.info("BayChange housekeeping work instructions disabled");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeNone);
		}
		else  if (bayChangeWI!=null && bayChangeWI.equals("BayChange")) {
			LOGGER.info("BayChange housekeeping set to BayChange");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeBayChange);
		}
		else  if (bayChangeWI!=null && bayChangeWI.equals("PathSegmentChange")) {
			LOGGER.info("BayChange housekeeping set to PathSegmentChange");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangePathSegmentChange);
		}
		else  if (bayChangeWI!=null && bayChangeWI.equals("ExceptSamePathDistance")) {
			LOGGER.info("BayChange housekeeping set to ExceptSamePathDistance");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeExceptSamePathDistance);
		}
		else {
			LOGGER.info("Using default BayChange housekeeping work instructions setting");
		}
		
		// configure repeatposition housekeeping work instructions
		// TODO: replace with configuration via database table		
		String useRepeatPosWI = configuration.getString("facility.housekeeping.repeatposition");
		if (useRepeatPosWI!=null && useRepeatPosWI.equals("ContainerAndCount")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerAndCount");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		}
		else if (useRepeatPosWI!=null && useRepeatPosWI.equals("None")) {
			LOGGER.info("RepeatPosition housekeeping work instructions disabled");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosNone);
		}
		else if (useRepeatPosWI!=null && useRepeatPosWI.equals("ContainerOnly")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerOnly");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerOnly);
		}
		else {
			LOGGER.info("Using default RepeatPosition housekeeping work instructions setting");			
		}		
		if (useRepeatPosWI!=null && useRepeatPosWI.equals("ContainerAndCount")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerAndCount");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		}
		else if (useRepeatPosWI!=null && useRepeatPosWI.equals("None")) {
			LOGGER.info("RepeatPosition housekeeping work instructions disabled");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosNone);
		}
		else if (useRepeatPosWI!=null && useRepeatPosWI.equals("ContainerOnly")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerOnly");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerOnly);
		}
		else {
			LOGGER.info("Using default RepeatPosition housekeeping work instructions setting");			
		}		
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		LOGGER.info("Stopping application");
		mHttpServer.stopServer();
		mEdiProcessor.stopProcessor();
		mPickDocumentGenerator.stopProcessor();
		// Stop the web socket server
		try {
			webSocketServer.stop();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to stop WebSocket server", e);
		}
		this.persistenceService.stop();
		LOGGER.info("Application terminated normally");
	}
/*
	@SuppressWarnings("unused")
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

	private void initPreference(Organization inOrganization, String inPropertyID, String inDescription, String inDefaultValue) {
		boolean shouldUpdate = false;

		// Find the property in the DB.
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inOrganization, inPropertyID);

		// If the property doesn't exist then create it.
		if (property == null) {
			property = new PersistentProperty();
			property.setDomainId(inPropertyID);
			property.setCurrentValueAsStr(inDefaultValue);
			property.setDefaultValueAsStr(inDefaultValue);
			inOrganization.addPersistentProperty(property);
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
	 */

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {
		this.getPersistenceService().beginTenantTransaction();

		// Create a demo organization
		createOrganizationUser("DEMO1", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO1", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO1", "che@example.com", "testme"); //view + simulate
		createOrganizationUser("DEMO1", "work@example.com", "testme"); //view + simulate

		createOrganizationUser("DEMO1", "view@goodeggs.com", "goodeggs"); //view
		createOrganizationUser("DEMO1", "view@accu-logistics.com", "accu-logistics"); //view

		// Recompute path positions,
		//   and ensure IronMq configuration
		//   and create a default site controller user if doesn't already exist
		for (Organization organization : mOrganizationDao.getAll()) {
			for (Facility facility : organization.getFacilities()) {
				for (Path path : facility.getPaths()) {
					// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
					facility.recomputeLocationPathDistances(path);
				}

				// create a default site controller and user for the first facility you see
				// this should go away
				for(CodeshelfNetwork network : facility.getNetworks()) {
					network.createDefaultSiteControllerUser(); // does nothing if user already exists
				}

			}
		}

		this.getPersistenceService().endTenantTransaction();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private User createOrganizationUser(String inOrganizationId, String inDefaultUserId, String inDefaultUserPw) {
		Organization organization = mOrganizationDao.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization();
			organization.setDomainId(inOrganizationId);
			try {
				mOrganizationDao.store(organization);

			} catch (DaoException e) {
				e.printStackTrace();
			}

		}
		User user = organization.getUser(inDefaultUserId);
		if (user == null) {
			user = organization.createUser(inDefaultUserId, inDefaultUserPw, UserType.APPUSER);
		}
		return user;
	}
}
