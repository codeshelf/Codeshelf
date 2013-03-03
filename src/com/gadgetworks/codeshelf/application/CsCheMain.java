/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsCheMain.java,v 1.3 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.CheDeviceEmbedded;
import com.gadgetworks.codeshelf.device.IEmbeddedDevice;
import com.gadgetworks.flyweight.command.IPacket;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CsCheMain {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsCheMain.class);

	// --------------------------------------------------------------------------
	/**
	 */
	private CsCheMain() {
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
		ICodeshelfApplication application = injector.getInstance(CsCheApplication.class);
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
				bind(Byte.class).annotatedWith(Names.named(IPacket.NETWORK_NUM_PROPERTY)).toInstance(Byte.valueOf(System.getProperty("codeshelf.networknum")));

				bind(IUtil.class).to(Util.class);
				bind(IEmbeddedDevice.class).to(CheDeviceEmbedded.class);
			}
		});

		return injector;
	}
}
