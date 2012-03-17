/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.2 2012/03/17 09:07:03 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.persist.User.IUserDao;
import com.gadgetworks.codeshelf.model.persist.User.UserDao;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.gadgetworks.codeshelf.web.websocket.WebSocketListener;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class Main {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}
	private static final Log	LOGGER	= LogFactory.getLog(Main.class);

	// --------------------------------------------------------------------------
	/**
	 */
	private Main() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) {
		
		String appDataDir = Util.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);

		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(ICodeShelfApplication.class).to(CodeShelfApplication.class);
				bind(IWebSocketListener.class).to(WebSocketListener.class);
				bind(IWebSessionManager.class).to(WebSessionManager.class);
				bind(IUserDao.class).to(UserDao.class);
				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				//bind(IUserDao.class).toProvider(DaoProviderFactory.createProvider(IUserDao.class)); 
				//install(new XmlBeanModule(xmlUrl));
			}
		});

		// Create and start the application.
		ICodeShelfApplication application = injector.getInstance(CodeShelfApplication.class);
		application.startApplication();

		// Handle events until the application exits.
		application.handleEvents();

		LOGGER.info("Exiting Main()");
	}
}