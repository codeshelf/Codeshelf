/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkMain.java,v 1.3 2013/02/24 22:54:25 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.client.WebSocketClient;

import com.gadgetworks.codeshelf.device.WebSocketController;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.web.websocket.CsWebSocketClient;
import com.gadgetworks.codeshelf.web.websocket.ICsWebSocketClient;
import com.gadgetworks.codeshelf.web.websocket.ICsWebsocketClientMsgHandler;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketSslContextGenerator;
import com.gadgetworks.codeshelf.web.websocket.SSLWebSocketClientFactory;
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

	private static final Log	LOGGER	= LogFactory.getLog(CsNetworkMain.class);

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
			properties.load(new FileInputStream(System.getProperty("config.properties")));
			for (String name : properties.stringPropertyNames()) {
				String value = properties.getProperty(name);
				System.setProperty(name, value);
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
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_NAME_PROPERTY)).toInstance(System.getProperty("db.name"));
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_SCHEMANAME_PROPERTY)).toInstance(System.getProperty("db.schemaname"));
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_USERID_PROPERTY)).toInstance(System.getProperty("db.userid"));
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_PASSWORD_PROPERTY)).toInstance(System.getProperty("db.password"));
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_ADDRESS_PROPERTY)).toInstance(System.getProperty("db.address"));
				bind(String.class).annotatedWith(Names.named(ISchemaManager.DATABASE_PORTNUM_PROPERTY)).toInstance(System.getProperty("db.portnum"));

				bind(String.class).annotatedWith(Names.named(CsWebSocketClient.WEBSOCKET_URI_PROPERTY)).toInstance(System.getProperty("websocket.uri"));

				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_PATH_PROPERTY)).toInstance(System.getProperty("keystore.path"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_TYPE_PROPERTY)).toInstance(System.getProperty("keystore.type"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_STORE_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.store.password"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_KEY_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.key.password"));

				bind(IUtil.class).to(Util.class);
				bind(ICodeshelfApplication.class).to(CsNetworkApplication.class);
				bind(ICsWebSocketClient.class).to(CsWebSocketClient.class);
				bind(ICsWebsocketClientMsgHandler.class).to(WebSocketController.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(WebSocketClient.WebSocketClientFactory.class).to(SSLWebSocketClientFactory.class);

				//				requestStaticInjection(WirelessDevice.class);
				//				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
			}
		});

		return injector;
	}
}
