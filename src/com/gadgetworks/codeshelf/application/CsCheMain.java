/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsCheMain.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.device.CheDevice;
import com.gadgetworks.codeshelf.device.IDevice;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CsCheMain {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}

	private static final Log	LOGGER	= LogFactory.getLog(CsCheMain.class);

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
				bind(IUtil.class).to(Util.class);
				bind(IDevice.class).to(CheDevice.class);
			}
		});

		return injector;
	}
}
