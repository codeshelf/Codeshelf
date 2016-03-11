/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkApplication.java,v 1.5 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.ClientConnectionManagerService;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.SiteControllerMessageProcessor;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.metrics.AssociatedRadioHealthCheck;
import com.codeshelf.metrics.ConnectedToServerHealthCheck;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.IsProductionSiteControllerHealthCheck;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.RadioOnHealthCheck;
import com.codeshelf.ws.client.CsClientEndpoint;
import com.google.inject.Inject;

public final class SiteControllerApplication extends CodeshelfApplication {
	@SuppressWarnings("unused")
	private static final Logger							LOGGER						= LoggerFactory.getLogger(SiteControllerApplication.class);

	@Getter
	private CsDeviceManager	deviceManager;
	@Getter
	private CsClientEndpoint	clientEndpoint;
	private IRadioController	radioController;

	
	@Inject
	public SiteControllerApplication(
			final WebApiServer inAdminServer,
			IMetricsService metricsService,
			IRadioController inRadioController,
			CsClientEndpoint clientEndpoint) {
		super(inAdminServer);

		this.registerService(new ClientConnectionManagerService(clientEndpoint));
		this.registerService(metricsService);
		
		this.clientEndpoint = clientEndpoint;
		this.radioController = inRadioController;
		deviceManager = new CsDeviceManager(inRadioController,clientEndpoint);
		
		/*
		try {
			startClojureEngine(deviceManager);
		} catch (ClassNotFoundException e) {
			LOGGER.error("unable to start clojure repl", e);
		}
		*/
		new SiteControllerMessageProcessor(deviceManager,clientEndpoint);
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
		
		// create and register site controller specific health checks
		MetricsService.getInstance().registerHealthCheck(new RadioOnHealthCheck(this.deviceManager));
		MetricsService.getInstance().registerHealthCheck(new ConnectedToServerHealthCheck(this.clientEndpoint));
		MetricsService.getInstance().registerHealthCheck(new AssociatedRadioHealthCheck(this.deviceManager));
		MetricsService.getInstance().registerHealthCheck(new IsProductionSiteControllerHealthCheck(this.deviceManager));
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		// STOP RADIO
		radioController.stopController();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

	}
	
	/*
	private static void startClojureEngine(CsDeviceManager deviceManager) throws ClassNotFoundException {
		 Class.forName("clojure.lang.RT");

		 String startScript = "(ns my-app (:require [clojure.tools.nrepl.server :as nrepl-server] [cider.nrepl :refer (cider-nrepl-handler)])) (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)";
		 
		    // Start the nREPL server.

		    Compiler.load(new StringReader(startScript));

		    // Make the Spring context available in the "lw" namespace.

		    RT.var("lw", "*manager*", deviceManager);
	}
*/	
}
