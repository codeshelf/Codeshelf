/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.management.ManagementFactory;
import java.util.Collection;
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
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.platform.multitenancy.ITenantManager;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
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
	
	@Getter
	private ITenantManager			tenantManager;

	private BlockingQueue<String>	mEdiProcessSignalQueue;

	private final ServerWatchdogThread watchdog;

	@Inject
	public ServerCodeshelfApplication(final IConfiguration configuration,
			final IEdiProcessor inEdiProcessor,
			final IPickDocumentGenerator inPickDocumentGenerator,
			final WebApiServer inWebApiServer,
			final PersistenceService persistenceService,
			final ITenantManager inTenantManager) {
			
		super(inWebApiServer);
		
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
		this.persistenceService = persistenceService;
		this.tenantManager = inTenantManager;
			
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

		if(!TenantManagerService.getInstance().isRunning()) {
			LOGGER.error("Failed to initialize Tenant Manager. Server is shutting down.");
			Thread.sleep(3000);
			System.exit(1);
		}
		// started by injection of getInstance() 
		// this.getPersistenceService().start();

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

		startApiServer(null,Integer.getInteger("api.port"));
		startTsdbReporter();
		registerSystemMetrics();

		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck(persistenceService);
		MetricsService.registerHealthCheck(dbCheck);

		ActiveSiteControllerHealthCheck sessionCheck = new ActiveSiteControllerHealthCheck();
		MetricsService.registerHealthCheck(sessionCheck);

		DropboxServiceHealthCheck dbxCheck = new DropboxServiceHealthCheck();
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
		// Recompute path positions
		Collection<Tenant> tenants = TenantManagerService.getInstance().getTenants();
		for(Tenant tenant : tenants) {
			try {
				this.getPersistenceService().beginTenantTransaction(tenant);
				for (Facility facility : Facility.DAO.getAll()) {
					for (Path path : facility.getPaths()) {
						// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
						facility.recomputeLocationPathDistances(path);
					}
				}
				this.getPersistenceService().commitTenantTransaction(tenant);
			} catch(Exception e) {
				this.getPersistenceService().rollbackTenantTransaction(tenant);
				throw e;
			}
		}
	}
}
