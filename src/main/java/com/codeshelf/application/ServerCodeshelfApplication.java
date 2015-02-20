/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.IEdiProcessor;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.codeshelf.metrics.DropboxServiceHealthCheck;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Path;
import com.codeshelf.platform.multitenancy.ITenantManager;
import com.codeshelf.platform.multitenancy.ManagerPersistenceService;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.ws.jetty.server.ServerWatchdogThread;
import com.codeshelf.ws.jetty.server.SessionManager;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends CodeshelfApplication {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor			mEdiProcessor;
	private IPickDocumentGenerator	mPickDocumentGenerator;
	
	private BlockingQueue<String>	mEdiProcessSignalQueue;

	private final ServerWatchdogThread watchdog;
	
	@Inject
	public ServerCodeshelfApplication(final IEdiProcessor inEdiProcessor,
			final IPickDocumentGenerator inPickDocumentGenerator,
			final WebApiServer inWebApiServer,
			final ITenantManager inTenantManager) {
			
		super(inWebApiServer);
		
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
		
		// if services already running e.g. in test, these will log an error and continue
		this.registerService(inTenantManager);
		this.registerService(TenantPersistenceService.getMaybeRunningInstance()); 
		this.registerService(ManagerPersistenceService.getMaybeRunningInstance());

		// create and configure watch dog
		this.watchdog = new ServerWatchdogThread(SessionManager.getInstance());
		boolean suppressKeepAlive = Boolean.getBoolean("websocket.idle.suppresskeepalive");
		boolean killIdle = Boolean.getBoolean("websocket.idle.kill");
		this.watchdog.setSuppressKeepAlive(suppressKeepAlive);
		this.watchdog.setKillIdle(killIdle);

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.application.ApplicationABC#doLoadLibraries()
	 */
	@Override
	protected void doLoadLibraries() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() throws Exception {

		// Start the EDI process.
		mEdiProcessSignalQueue = new ArrayBlockingQueue<>(100);
		mEdiProcessor.startProcessor(mEdiProcessSignalQueue);

		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(mEdiProcessSignalQueue);

		startApiServer(null,Integer.getInteger("api.port"));
		startTsdbReporter();
		registerSystemMetrics();

		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck();
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
				TenantPersistenceService.getInstance().beginTransaction(tenant);
				for (Facility facility : Facility.DAO.getAll()) {
					for (Path path : facility.getPaths()) {
						// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
						facility.recomputeLocationPathDistances(path);
					}
				}
				TenantPersistenceService.getInstance().commitTransaction(tenant);
			} catch(Exception e) {
				TenantPersistenceService.getInstance().rollbackTransaction(tenant);
				throw e;
			}
		}
	}

}
