/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.15 2012/07/12 08:18:06 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.domain.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork.CodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.ControlGroup.ControlGroupDao;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.DBProperty.DBPropertyDao;
import com.gadgetworks.codeshelf.model.persist.Facility;
import com.gadgetworks.codeshelf.model.persist.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty.PersistentPropertyDao;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.model.persist.User.UserDao;
import com.gadgetworks.codeshelf.model.persist.Vertex;
import com.gadgetworks.codeshelf.model.persist.Vertex.VertexDao;
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
				bind(new TypeLiteral<ITypedDao<Organization>>() {
				}).to(OrganizationDao.class);
				bind(new TypeLiteral<ITypedDao<Vertex>>() {
				}).to(VertexDao.class);
				bind(new TypeLiteral<ITypedDao<Facility>>() {
				}).to(FacilityDao.class);
				bind(new TypeLiteral<ITypedDao<Aisle>>() {
				}).to(AisleDao.class);
				bind(new TypeLiteral<ITypedDao<PersistentProperty>>() {
				}).to(PersistentPropertyDao.class);
				bind(new TypeLiteral<ITypedDao<DBProperty>>() {
				}).to(DBPropertyDao.class);
				bind(new TypeLiteral<ITypedDao<User>>() {
				}).to(UserDao.class);
				bind(new TypeLiteral<ITypedDao<CodeShelfNetwork>>() {
				}).to(CodeShelfNetworkDao.class);
				bind(new TypeLiteral<ITypedDao<ControlGroup>>() {
				}).to(ControlGroupDao.class);
				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				requestStaticInjection(Organization.class);
				requestStaticInjection(Facility.class);
				requestStaticInjection(WirelessDevice.class);
				requestStaticInjection(PersistentProperty.class);
				requestStaticInjection(DBProperty.class);
			}
		});

		return injector;
	}
}
