/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkMain.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.ws.websocket.CsWebSocketClient;
import com.gadgetworks.codeshelf.ws.websocket.ICsWebsocketClientMsgHandler;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketSslContextFactory;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketSslContextGenerator;
import com.gadgetworks.codeshelf.ws.websocket.WebSocketSslContextFactory;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.controller.FTDIInterface;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CsNetworkMain {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsNetworkMain.class);

	// --------------------------------------------------------------------------
	/**
	 */
	private CsNetworkMain() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) {

		Properties properties = new Properties();
		try {
			String configFileName = System.getProperty("config.properties");
			if (configFileName != null) {
				FileInputStream configFileStream = new FileInputStream(configFileName);
				if (configFileStream != null) {
					properties.load(configFileStream);
					for (String name : properties.stringPropertyNames()) {
						String value = properties.getProperty(name);
						System.setProperty(name, value);
					}
				}
			}
		} catch (IOException e) {
			System.err.println();
		}

		// Guice (injector) will invoke log4j, so we need to set some log dir parameters before we call it.
		Util util = new Util();
		String appDataDir = util.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);

		//		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		//		Handler[] handlers = rootLogger.getHandlers();
		//		for (int i = 0; i < handlers.length; i++) {
		//			rootLogger.removeHandler(handlers[i]);
		//		}
		//		SLF4JBridgeHandler.install();

		// Create and start the application.
		Injector injector = setupInjector();
		ICodeshelfApplication application = injector.getInstance(CsNetworkApplication.class);
		application.startApplication();

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
				bind(String.class).annotatedWith(Names.named(CsWebSocketClient.WEBSOCKET_URI_PROPERTY)).toInstance(System.getProperty("websocket.uri"));

				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_PATH_PROPERTY)).toInstance(System.getProperty("keystore.path"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_TYPE_PROPERTY)).toInstance(System.getProperty("keystore.type"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_STORE_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.store.password"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_KEY_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.key.password"));

				bind(Byte.class).annotatedWith(Names.named(IPacket.NETWORK_NUM_PROPERTY)).toInstance(Byte.valueOf(System.getProperty("codeshelf.networknum")));

				bind(IUtil.class).to(Util.class);
				bind(ICodeshelfApplication.class).to(CsNetworkApplication.class);
				// Can't inject Java_Websocket classes.  See CsWebSocketClient.java for explanation.
				//bind(ICsWebSocketClient.class).to(CsWebSocketClient.class);
				bind(IRadioController.class).to(RadioController.class);
				bind(IGatewayInterface.class).to(FTDIInterface.class);
				bind(ICsDeviceManager.class).to(CsDeviceManager.class);
				bind(ICsWebsocketClientMsgHandler.class).to(CsDeviceManager.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(IWebSocketSslContextFactory.class).to(WebSocketSslContextFactory.class);

				//				requestStaticInjection(WirelessDevice.class);
				//				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
			}
		});

		return injector;
	}
}
