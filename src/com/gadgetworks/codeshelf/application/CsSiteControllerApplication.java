/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkApplication.java,v 1.5 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import lombok.Getter;

import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.RadioOnHealthCheck;
import com.google.inject.Inject;

public final class CsSiteControllerApplication extends ApplicationABC {

	@Getter
	private ICsDeviceManager	deviceManager;

	@Inject
	public CsSiteControllerApplication(final ICsDeviceManager inDeviceManager,final AdminServer inAdminServer) {
		super(inAdminServer);
		deviceManager = inDeviceManager;
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
		startAdminServer();
		startTsdbReporter();
		registerMemoryUsageMetrics();
		
		// Start the device manager.
		deviceManager.start();
		
		// create site controller specific health checks
		RadioOnHealthCheck radioCheck = new RadioOnHealthCheck(this.deviceManager);
		MetricsService.registerHealthCheck(radioCheck);
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
