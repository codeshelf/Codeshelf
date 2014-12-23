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

import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.gadgetworks.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.gadgetworks.codeshelf.metrics.DropboxServiceHealthCheck;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor			mEdiProcessor;
	private IHttpServer				mHttpServer;
	private IPickDocumentGenerator	mPickDocumentGenerator;
	
	@Getter
	private PersistenceService		persistenceService;

	private ITypedDao<Facility>		mFacilityDao;

	private BlockingQueue<String>	mEdiProcessSignalQueue;

	JettyWebSocketServer			webSocketServer;

	private IConfiguration			configuration;

	@Inject
	public ServerCodeshelfApplication(final IConfiguration configuration,
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IPickDocumentGenerator inPickDocumentGenerator,
		final ITypedDao<User> inUserDao,
		final AdminServer inAdminServer,
		final JettyWebSocketServer inAlternativeWebSocketServer,
		final PersistenceService persistenceService) {
		super(inAdminServer);
		this.configuration = configuration;
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
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
			this.getPersistenceService().commitTenantTransaction();
		} catch (HibernateException e) {
			LOGGER.error("Failed to initialize Hibernate. Server is shutting down.", e);
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
		if (bayChangeWI != null && bayChangeWI.equals("None")) {
			LOGGER.info("BayChange housekeeping work instructions disabled");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeNone);
		} else if (bayChangeWI != null && bayChangeWI.equals("BayChange")) {
			LOGGER.info("BayChange housekeeping set to BayChange");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeBayChange);
		} else if (bayChangeWI != null && bayChangeWI.equals("PathSegmentChange")) {
			LOGGER.info("BayChange housekeeping set to PathSegmentChange");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangePathSegmentChange);
		} else if (bayChangeWI != null && bayChangeWI.equals("ExceptSamePathDistance")) {
			LOGGER.info("BayChange housekeeping set to ExceptSamePathDistance");
			HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeExceptSamePathDistance);
		} else {
			LOGGER.info("Using default BayChange housekeeping work instructions setting");
		}

		// configure repeatposition housekeeping work instructions
		// TODO: replace with configuration via database table		
		String useRepeatPosWI = configuration.getString("facility.housekeeping.repeatposition");
		if (useRepeatPosWI != null && useRepeatPosWI.equals("ContainerAndCount")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerAndCount");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		} else if (useRepeatPosWI != null && useRepeatPosWI.equals("None")) {
			LOGGER.info("RepeatPosition housekeeping work instructions disabled");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosNone);
		} else if (useRepeatPosWI != null && useRepeatPosWI.equals("ContainerOnly")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerOnly");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerOnly);
		} else {
			LOGGER.info("Using default RepeatPosition housekeeping work instructions setting");
		}
		if (useRepeatPosWI != null && useRepeatPosWI.equals("ContainerAndCount")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerAndCount");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		} else if (useRepeatPosWI != null && useRepeatPosWI.equals("None")) {
			LOGGER.info("RepeatPosition housekeeping work instructions disabled");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosNone);
		} else if (useRepeatPosWI != null && useRepeatPosWI.equals("ContainerOnly")) {
			LOGGER.info("RepeatPosition housekeeping work instructions set to ContainerOnly");
			HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerOnly);
		} else {
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

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {
		try {
			this.getPersistenceService().beginTenantTransaction();
			Organization.CreateDemo();
			this.getPersistenceService().commitTenantTransaction();
		} catch (RuntimeException e) {
			this.getPersistenceService().rollbackTenantTransaction();
			LOGGER.error("unable to create demo organization", e);
			throw e;
		}
	}
}
