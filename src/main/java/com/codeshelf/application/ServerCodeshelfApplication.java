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

import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.EdiImportService;
import com.codeshelf.email.EmailService;
import com.codeshelf.email.TemplateService;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.ManagerPersistenceService;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.codeshelf.metrics.DropboxGatewayHealthCheck;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.IsProductionServerHealthCheck;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.scheduler.ApplicationSchedulerService;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends CodeshelfApplication {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private EdiImportService			ediImportService;
    @SuppressWarnings("unused")
	private EdiExportService			ediExportService;
	
	private WebSocketManagerService sessionManager;
	private IMetricsService metricsService;
	
	@Inject
	public ServerCodeshelfApplication(final EdiImportService inEdiProcessService,
			final WebApiServer inWebApiServer,
			final ITenantManagerService tenantManagerService,
			final IMetricsService metricsService,
			final WebSocketManagerService webSocketManagerService,
			final EdiExportService ediExportService,
			final TokenSessionService authService,
			final SecurityManager securityManager,
			final EmailService emailService,
			final TemplateService templateService,
			final ApplicationSchedulerService schedulerService) {
			
		super(inWebApiServer);
	
		ediImportService = inEdiProcessService;
		this.ediExportService = ediExportService;
		sessionManager = webSocketManagerService;
		this.metricsService = metricsService;
		
		SecurityUtils.setSecurityManager(securityManager);
		
		// if services already running e.g. in test, these will log an error and continue
		this.registerService(tenantManagerService);
		this.registerService(TenantPersistenceService.getMaybeRunningInstance()); 
		this.registerService(ManagerPersistenceService.getMaybeRunningInstance());
		this.registerService(metricsService);
		this.registerService(webSocketManagerService);
		this.registerService(authService);
		this.registerService(ediExportService);
		this.registerService(ediImportService);
		this.registerService(emailService);
		this.registerService(templateService);
		this.registerService(schedulerService);
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

		startApiServer(null,Integer.getInteger("api.port"));
		startTsdbReporter();
		registerSystemMetrics();

		// create server-specific health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck();
		metricsService.registerHealthCheck(dbCheck);

		ActiveSiteControllerHealthCheck sessionCheck = new ActiveSiteControllerHealthCheck(this.sessionManager);
		metricsService.registerHealthCheck(sessionCheck);

		DropboxGatewayHealthCheck dbxCheck = new DropboxGatewayHealthCheck();
		metricsService.registerHealthCheck(dbxCheck);
		
		IsProductionServerHealthCheck productionCheck = new IsProductionServerHealthCheck();
		metricsService.registerHealthCheck(productionCheck);
		
		/*
		PicksActivityHealthCheck picksActivityCheck = new PicksActivityHealthCheck();
		metricsService.registerHealthCheck(picksActivityCheck);

		EdiHealthCheck ediCheck = new EdiHealthCheck(this.ediImportService, ediExportService);
		metricsService.registerHealthCheck(ediCheck);
		
		DataQuantityHealthCheck dataQuantityCheck = new DataQuantityHealthCheck();
		metricsService.registerHealthCheck(dataQuantityCheck);
		*/

}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		LOGGER.info("Stopping application");
		this.stopApiServer();

	}

	@Override
	protected void doInitializeApplicationData() {
		// TODO Auto-generated method stub
		
	}


}
