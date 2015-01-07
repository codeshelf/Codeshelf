/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

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
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerWatchdogThread;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor			mEdiProcessor;
	private IPickDocumentGenerator	mPickDocumentGenerator;

	@Getter
	private PersistenceService		persistenceService;

	private ITypedDao<Facility>		mFacilityDao;

	private BlockingQueue<String>	mEdiProcessSignalQueue;

	private IConfiguration			configuration;

	private final ServerWatchdogThread watchdog;

	@Inject
	public ServerCodeshelfApplication(final IConfiguration configuration,
			final IEdiProcessor inEdiProcessor,
			final IPickDocumentGenerator inPickDocumentGenerator,
			final ITypedDao<User> inUserDao,
			final WebApiServer inWebApiServer,
			final PersistenceService persistenceService) {
			
		super(inWebApiServer);
		
		this.configuration = configuration;
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
		this.persistenceService = persistenceService;
			
		// create and configure watch dog
		this.watchdog = new ServerWatchdogThread(SessionManager.getInstance());
		boolean suppressKeepAlive = configuration.getBoolean("websocket.idle.suppresskeepalive");
		boolean killIdle = configuration.getBoolean("websocket.idle.kill");
		this.watchdog.setSuppressKeepAlive(suppressKeepAlive);
		this.watchdog.setKillIdle(killIdle);

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

		// Start the EDI process.
		mEdiProcessSignalQueue = new ArrayBlockingQueue<>(100);
		mEdiProcessor.startProcessor(mEdiProcessSignalQueue);

		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(mEdiProcessSignalQueue);

		startApiServer(null);
		startTsdbReporter();
		registerSystemMetrics();

		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck(persistenceService);
		MetricsService.registerHealthCheck(dbCheck);

		ActiveSiteControllerHealthCheck sessionCheck = new ActiveSiteControllerHealthCheck();
		MetricsService.registerHealthCheck(sessionCheck);

		DropboxServiceHealthCheck dbxCheck = new DropboxServiceHealthCheck(mFacilityDao);
		MetricsService.registerHealthCheck(dbxCheck);

		// start server watchdog
		watchdog.start();        

	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		LOGGER.info("Stopping application");
		watchdog.setExit(true);
		mEdiProcessor.stopProcessor();
		mPickDocumentGenerator.stopProcessor();
		this.stopApiServer();
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
