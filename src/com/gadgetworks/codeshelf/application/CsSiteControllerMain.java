/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkMain.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.OpenTsdb;
import com.gadgetworks.codeshelf.metrics.OpenTsdbReporter;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.util.JVMSystemConfiguration;
import com.gadgetworks.flyweight.controller.FTDIInterface;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CsSiteControllerMain {

	// pre-main static load configuration and set up logging (see Configuration.java)
	static {
		Configuration.loadConfig("sitecontroller");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsSiteControllerMain.class);

	private static WebSocketContainer websocketContainer = ContainerProvider.getWebSocketContainer();
	// --------------------------------------------------------------------------
	/**
	 */
	private CsSiteControllerMain() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) throws Exception {

		// Create and start the application.
		ICodeshelfApplication application = createApplication(new DefaultModule());
		application.startApplication();
		
		// public metrics to opentsdb
		String useMetricsReporter = System.getProperty("metrics.reporter.enabled");
		if ("true".equalsIgnoreCase(useMetricsReporter)) {
			String metricsServerUrl = System.getProperty("metrics.reporter.serverurl");
			String intervalStr = System.getProperty("metrics.reporter.interval");
			int interval = Integer.parseInt(intervalStr);
			
			LOGGER.info("Starting OpenTSDB Reporter writing to "+metricsServerUrl+" in "+interval+" sec intervals");
			MetricRegistry registry = MetricsService.getRegistry();
			String hostName = MetricsService.getInstance().getHostName();
			OpenTsdbReporter.forRegistry(registry)
			      .prefixedWith("")
			      .withTags(ImmutableMap.of("host", hostName))
			      .build(OpenTsdb.forService(metricsServerUrl)
			      .create())
			      .start(interval, TimeUnit.SECONDS);
		}
		else {
			LOGGER.info("Metrics reporter is not enabled");
		}		

		// Handle events until the application exits.
		application.handleEvents();

		LOGGER.info("Exiting Main()");
	}

	// --------------------------------------------------------------------------
	
	public static CsSiteControllerApplication createApplication(Module guiceModule) {
		Injector injector = Guice.createInjector(guiceModule);
		return injector.getInstance(CsSiteControllerApplication.class); 
	}
	
	public static class BaseModule extends AbstractModule {
		
		@Override
		protected void configure() {
			bind(WebSocketContainer.class).toInstance(websocketContainer);
			bind(ICodeshelfApplication.class).to(CsSiteControllerApplication.class);
			bind(IRadioController.class).to(RadioController.class);
			bind(IConfiguration.class).to(JVMSystemConfiguration.class);
			bind(ICsDeviceManager.class).to(CsDeviceManager.class);
		}
		
	}

	public static class DefaultModule extends BaseModule {
		
		@Override
		protected void configure() {
			super.configure();
			bind(IGatewayInterface.class).to(FTDIInterface.class);
		}
		
	}

}
