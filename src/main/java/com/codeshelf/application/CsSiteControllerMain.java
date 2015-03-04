/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkMain.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.SiteControllerMessageProcessor;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.flyweight.controller.FTDIInterface;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.OpenTsdb;
import com.codeshelf.metrics.OpenTsdbReporter;
import com.codeshelf.ws.jetty.client.CsClientEndpoint;
import com.codeshelf.ws.jetty.client.MessageCoordinator;
import com.codeshelf.ws.jetty.client.WebSocketEventListener;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CsSiteControllerMain {

	// pre-main static load configuration and set up logging (see Configuration.java)
	static {
		JvmProperties.load("sitecontroller");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsSiteControllerMain.class);

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
		application.startServices();
		application.startApplication();
		
		// public metrics to opentsdb
		String useMetricsReporter = System.getProperty("metrics.reporter.enabled");
		if ("true".equalsIgnoreCase(useMetricsReporter)) {
			String metricsServerUrl = System.getProperty("metrics.reporter.serverurl");
			String intervalStr = System.getProperty("metrics.reporter.interval");
			int interval = Integer.parseInt(intervalStr);
			
			LOGGER.info("Starting OpenTSDB Reporter writing to "+metricsServerUrl+" in "+interval+" sec intervals");
			MetricRegistry registry = MetricsService.getInstance().getMetricsRegistry();
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
	
	public static SiteControllerApplication createApplication(Module guiceModule) {
		Injector injector = Guice.createInjector(guiceModule);
		return injector.getInstance(SiteControllerApplication.class); 
	}
	
	public static class BaseModule extends AbstractModule {
		
		@Override
		protected void configure() {
			requestStaticInjection(MetricsService.class);			
			requestStaticInjection(CsClientEndpoint.class);

			bind(IMetricsService.class).to(MetricsService.class).in(Singleton.class);

			bind(ICodeshelfApplication.class).to(SiteControllerApplication.class).in(Singleton.class);
			bind(IRadioController.class).to(RadioController.class).in(Singleton.class);
			
			bind(IMessageProcessor.class).to(SiteControllerMessageProcessor.class).in(Singleton.class);
			bind(WebSocketEventListener.class).to(CsDeviceManager.class).in(Singleton.class);
			
		}
		
		@Provides
		@Singleton
		protected MessageCoordinator createMessageCoordinator() {
			return new MessageCoordinator();
		}
		
		@Provides
		@Singleton
		protected WebSocketContainer createWebSocketContainer() {
			return ContainerProvider.getWebSocketContainer();
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
