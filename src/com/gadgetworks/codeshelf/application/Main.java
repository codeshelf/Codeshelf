/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.8 2012/03/22 06:58:44 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.DaoRegistry;
import com.gadgetworks.codeshelf.model.dao.DbFacade;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.dao.domain.AisleDao;
import com.gadgetworks.codeshelf.model.dao.domain.CodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.dao.domain.ControlGroupDao;
import com.gadgetworks.codeshelf.model.dao.domain.DBPropertyDao;
import com.gadgetworks.codeshelf.model.dao.domain.FacilityDao;
import com.gadgetworks.codeshelf.model.dao.domain.OrganizationDao;
import com.gadgetworks.codeshelf.model.dao.domain.PersistentPropertyDao;
import com.gadgetworks.codeshelf.model.dao.domain.UserDao;
import com.gadgetworks.codeshelf.model.dao.domain.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.Facility;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.model.persist.UserSession;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.gadgetworks.codeshelf.web.websocket.WebSocketListener;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

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

		// Guice (injector) will invoke log4j, so we need to set some log dir parameters before we call it.
		String appDataDir = Util.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);

		//		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		//		Handler[] handlers = rootLogger.getHandlers();
		//		for (int i = 0; i < handlers.length; i++) {
		//			rootLogger.removeHandler(handlers[i]);
		//		}
		//		SLF4JBridgeHandler.install();

		// Create and start the application.
		Injector injector = setupInjector();
		ICodeShelfApplication application = injector.getInstance(CodeShelfApplication.class);
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
				bind(ICodeShelfApplication.class).to(CodeShelfApplication.class);
				bind(IWebSocketListener.class).to(WebSocketListener.class);
				bind(IWebSessionManager.class).to(WebSessionManager.class);
				bind(new TypeLiteral<IGenericDao<Organization>>() {
				}).to(OrganizationDao.class);
				bind(new TypeLiteral<IGenericDao<Facility>>() {
				}).to(FacilityDao.class);
				bind(new TypeLiteral<IGenericDao<Aisle>>() {
				}).to(AisleDao.class);
				bind(new TypeLiteral<IGenericDao<PersistentProperty>>() {
				}).to(PersistentPropertyDao.class);
				bind(new TypeLiteral<IGenericDao<DBProperty>>() {
				}).to(DBPropertyDao.class);
				bind(new TypeLiteral<IGenericDao<User>>() {
				}).to(UserDao.class);
				bind(new TypeLiteral<IGenericDao<CodeShelfNetwork>>() {
				}).to(CodeShelfNetworkDao.class);
				bind(new TypeLiteral<IGenericDao<ControlGroup>>() {
				}).to(ControlGroupDao.class);
				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				bind(IDaoRegistry.class).to(DaoRegistry.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(new TypeLiteral<IDbFacade<PersistABC>>() {
				}).to(new TypeLiteral<DbFacade<PersistABC>>() {
				});
				bind(new TypeLiteral<IDbFacade<Aisle>>() {
				}).to(new TypeLiteral<DbFacade<Aisle>>() {
				});
				bind(new TypeLiteral<IDbFacade<CodeShelfNetwork>>() {
				}).to(new TypeLiteral<DbFacade<CodeShelfNetwork>>() {
				});
				bind(new TypeLiteral<IDbFacade<ControlGroup>>() {
				}).to(new TypeLiteral<DbFacade<ControlGroup>>() {
				});
				bind(new TypeLiteral<IDbFacade<DBProperty>>() {
				}).to(new TypeLiteral<DbFacade<DBProperty>>() {
				});
				bind(new TypeLiteral<IDbFacade<Facility>>() {
				}).to(new TypeLiteral<DbFacade<Facility>>() {
				});
				bind(new TypeLiteral<IDbFacade<Organization>>() {
				}).to(new TypeLiteral<DbFacade<Organization>>() {
				});
				bind(new TypeLiteral<IDbFacade<PersistentProperty>>() {
				}).to(new TypeLiteral<DbFacade<PersistentProperty>>() {
				});
				bind(new TypeLiteral<IDbFacade<User>>() {
				}).to(new TypeLiteral<DbFacade<User>>() {
				});
				bind(new TypeLiteral<IDbFacade<UserSession>>() {
				}).to(new TypeLiteral<DbFacade<UserSession>>() {
				});
				bind(new TypeLiteral<IDbFacade<WirelessDevice>>() {
				}).to(new TypeLiteral<DbFacade<WirelessDevice>>() {
				});
			}
		});

		return injector;
	}
}
