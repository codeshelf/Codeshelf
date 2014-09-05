/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkMain.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.OpenTsdb;
import com.gadgetworks.codeshelf.metrics.OpenTsdbReporter;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.ws.websocket.CsWebSocketClient;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketServer;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketSslContextFactory;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketSslContextGenerator;
import com.gadgetworks.codeshelf.ws.websocket.WebSocketSslContextFactory;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.controller.FTDIInterface;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

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

	// --------------------------------------------------------------------------
	/**
	 */
	private CsSiteControllerMain() {
	}
	


	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) {

		// Create and start the application.
		Injector injector = setupInjector();
		ICodeshelfApplication application = injector.getInstance(CsSiteControllerApplication.class);
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
	/**
	 * @return
	 */
	private static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				bind(String.class).annotatedWith(Names.named("WS_SERVER_URI")).toInstance(System.getProperty("websocket.uri"));
				bind(Byte.class).annotatedWith(Names.named(IPacket.NETWORK_NUM_PROPERTY)).toInstance(Byte.valueOf(System.getProperty("codeshelf.networknum")));

				bind(ICodeshelfApplication.class).to(CsSiteControllerApplication.class);
				bind(IRadioController.class).to(RadioController.class);
				bind(IGatewayInterface.class).to(FTDIInterface.class);
				bind(ICsDeviceManager.class).to(CsDeviceManager.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(IWebSocketSslContextFactory.class).to(WebSocketSslContextFactory.class);

				// requestStaticInjection(WirelessDevice.class);
				// bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);

				bind(String.class).annotatedWith(Names.named(CsWebSocketClient.WEBSOCKET_URI_PROPERTY)).toInstance(System.getProperty("websocket.uri"));
				
				bind(Boolean.class).annotatedWith(Names.named(CsWebSocketClient.WEBSOCKET_SUPPRESS_KEEPALIVE_PROPERTY))
					.toInstance(Boolean.valueOf(System.getProperty("websocket.idle.suppresskeepalive")));
				bind(Boolean.class).annotatedWith(Names.named(CsWebSocketClient.WEBSOCKET_KILL_IDLE_PROPERTY))
					.toInstance(Boolean.valueOf(System.getProperty("websocket.idle.kill")));

				// 	TODO: remove below after taking java WS code out
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_PATH_PROPERTY)).toInstance(System.getProperty("keystore.path"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_TYPE_PROPERTY)).toInstance(System.getProperty("keystore.type"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_STORE_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.store.password"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_KEY_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.key.password"));
			}
		});

		return injector;
	}
}
