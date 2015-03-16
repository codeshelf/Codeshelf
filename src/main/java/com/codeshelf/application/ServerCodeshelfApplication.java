/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiProcessorService;
import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.ManagerPersistenceService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.codeshelf.metrics.DropboxServiceHealthCheck;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Path;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.security.AuthProviderService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.server.SessionManagerService;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends CodeshelfApplication {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private EdiProcessorService			ediProcessorService;
	private IPickDocumentGenerator	mPickDocumentGenerator;
	
	private SessionManagerService sessionManager;
	private IMetricsService metricsService;
	
	@Inject
	public ServerCodeshelfApplication(final EdiProcessorService inEdiProcessorService,
			final IPickDocumentGenerator inPickDocumentGenerator,
			final WebApiServer inWebApiServer,
			final ITenantManagerService tenantManagerService,
			final WorkService workService,
			final IMetricsService metricsService,
			final SessionManagerService sessionManagerService,
			final IPropertyService propertyService,
			final AuthProviderService authService,
			final SecurityManager securityManager) {
			
		super(inWebApiServer);
	
		ediProcessorService = inEdiProcessorService;
		mPickDocumentGenerator = inPickDocumentGenerator;
		sessionManager = sessionManagerService;
		this.metricsService = metricsService;
		
		SecurityUtils.setSecurityManager(securityManager);
		
		// if services already running e.g. in test, these will log an error and continue
		this.registerService(tenantManagerService);
		this.registerService(TenantPersistenceService.getMaybeRunningInstance()); 
		this.registerService(ManagerPersistenceService.getMaybeRunningInstance());
		this.registerService(workService);
		this.registerService(metricsService);
		this.registerService(sessionManagerService);
		this.registerService(propertyService);
		this.registerService(authService);

		this.registerService(ediProcessorService);

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

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {
		// Recompute path positions
		
		//Collection<Tenant> tenants = TenantManagerService.getInstance().getTenants();
		Collection<Tenant> tenants = new ArrayList<Tenant>(1);
		tenants.add(TenantManagerService.getInstance().getDefaultTenant());

		for(Tenant tenant : tenants) {
			try {
				TenantPersistenceService.getInstance().beginTransaction(tenant);
				for (Facility facility : Facility.staticGetDao().getAll()) {
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
