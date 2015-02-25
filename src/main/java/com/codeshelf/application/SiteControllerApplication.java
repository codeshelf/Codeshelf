/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkApplication.java,v 1.5 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import lombok.Getter;

import com.codeshelf.device.ClientConnectionManagerService;
import com.codeshelf.device.ICsDeviceManager;
import com.codeshelf.metrics.AssociatedRadioHealthCheck;
import com.codeshelf.metrics.ConnectedToServerHealthCheck;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.RadioOnHealthCheck;
import com.google.inject.Inject;

public final class SiteControllerApplication extends CodeshelfApplication {

	@Getter
	private ICsDeviceManager	deviceManager;

	
	@Inject
	public SiteControllerApplication(final ICsDeviceManager inDeviceManager,final WebApiServer inAdminServer,
			IMetricsService metricsService) {
		super(inAdminServer);
		deviceManager = inDeviceManager;
		
		this.registerService(new ClientConnectionManagerService(deviceManager.getClient()));
		this.registerService(metricsService);
		
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.application.ApplicationABC#doLoadLibraries()
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
		Integer port = Integer.getInteger("sitecontroller.port");

		startApiServer(this.deviceManager,port);
		startTsdbReporter();
		registerSystemMetrics();
		
		// Start the device manager.
		deviceManager.start();

		// create and register site controller specific health checks
		RadioOnHealthCheck radioCheck = new RadioOnHealthCheck(this.deviceManager);
		MetricsService.getInstance().registerHealthCheck(radioCheck);
		
		ConnectedToServerHealthCheck serverConnectionCheck = new ConnectedToServerHealthCheck(this.deviceManager);
		MetricsService.getInstance().registerHealthCheck(serverConnectionCheck);
		
		AssociatedRadioHealthCheck associateCheck = new AssociatedRadioHealthCheck(this.deviceManager);
		MetricsService.getInstance().registerHealthCheck(associateCheck);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {

		// Stop the web socket.
		deviceManager.stop();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

	}
}
