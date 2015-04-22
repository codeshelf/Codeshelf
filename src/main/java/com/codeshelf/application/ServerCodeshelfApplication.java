/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiProcessorService;
import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.ManagerPersistenceService;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.codeshelf.metrics.DropboxServiceHealthCheck;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends CodeshelfApplication {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private EdiProcessorService			ediProcessorService;
	private IPickDocumentGenerator	mPickDocumentGenerator;
	
	private WebSocketManagerService sessionManager;
	private IMetricsService metricsService;
	
	@Inject
	public ServerCodeshelfApplication(final EdiProcessorService inEdiProcessorService,
			final IPickDocumentGenerator inPickDocumentGenerator,
			final WebApiServer inWebApiServer,
			final ITenantManagerService tenantManagerService,
			final WorkService workService,
			final IMetricsService metricsService,
			final WebSocketManagerService webSocketManagerService,
			final IPropertyService propertyService,
			final TokenSessionService authService,
			final SecurityManager securityManager,
			final SchedulingService schedulingService) {
			
		super(inWebApiServer);
	
		ediProcessorService = inEdiProcessorService;
		mPickDocumentGenerator = inPickDocumentGenerator;
		sessionManager = webSocketManagerService;
		this.metricsService = metricsService;
		
		SecurityUtils.setSecurityManager(securityManager);
		
		// if services already running e.g. in test, these will log an error and continue
		this.registerService(tenantManagerService);
		this.registerService(TenantPersistenceService.getMaybeRunningInstance()); 
		this.registerService(ManagerPersistenceService.getMaybeRunningInstance());
		this.registerService(workService);
		this.registerService(metricsService);
		this.registerService(webSocketManagerService);
		this.registerService(propertyService);
		this.registerService(authService);
		this.registerService(ediProcessorService);
		if (schedulingService!=null) this.registerService(schedulingService);
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

		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(this.ediProcessorService.getEdiSignalQueue());

		startApiServer(null,Integer.getInteger("api.port"));
		startTsdbReporter();
		registerSystemMetrics();

		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck();
		metricsService.registerHealthCheck(dbCheck);

		ActiveSiteControllerHealthCheck sessionCheck = new ActiveSiteControllerHealthCheck(this.sessionManager);
		metricsService.registerHealthCheck(sessionCheck);

		DropboxServiceHealthCheck dbxCheck = new DropboxServiceHealthCheck();
		metricsService.registerHealthCheck(dbxCheck);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		LOGGER.info("Stopping application");
		mPickDocumentGenerator.stopProcessor();
		this.stopApiServer();

	}

	@Override
	protected void doInitializeApplicationData() {
		// TODO Auto-generated method stub
		
	}


}
