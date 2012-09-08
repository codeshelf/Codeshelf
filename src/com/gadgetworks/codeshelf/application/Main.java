/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.20 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.domain.CodeShelfNetwork.CodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.domain.ControlGroup;
import com.gadgetworks.codeshelf.model.domain.ControlGroup.ControlGroupDao;
import com.gadgetworks.codeshelf.model.domain.DBProperty;
import com.gadgetworks.codeshelf.model.domain.DBProperty.DBPropertyDao;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC.EdiServiceDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty.PersistentPropertyDao;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.User.UserDao;
import com.gadgetworks.codeshelf.model.domain.UserSession;
import com.gadgetworks.codeshelf.model.domain.UserSession.UserSessionDao;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.gadgetworks.codeshelf.web.websocket.WebSocketListener;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
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
				
				requestStaticInjection(Aisle.class);
				bind(new TypeLiteral<ITypedDao<Aisle>>() {
				}).to(AisleDao.class);

				requestStaticInjection(Bay.class);
				bind(new TypeLiteral<ITypedDao<Bay>>() {
				}).to(BayDao.class);

				requestStaticInjection(CodeShelfNetwork.class);
				bind(new TypeLiteral<ITypedDao<CodeShelfNetwork>>() {
				}).to(CodeShelfNetworkDao.class);

				requestStaticInjection(ControlGroup.class);
				bind(new TypeLiteral<ITypedDao<ControlGroup>>() {
				}).to(ControlGroupDao.class);

				requestStaticInjection(DBProperty.class);
				bind(new TypeLiteral<ITypedDao<DBProperty>>() {
				}).to(DBPropertyDao.class);
				
				requestStaticInjection(EdiServiceABC.class);
				bind(new TypeLiteral<ITypedDao<EdiServiceABC>>() {
				}).to(EdiServiceDao.class);
				
				requestStaticInjection(Facility.class);
				bind(new TypeLiteral<ITypedDao<Facility>>() {
				}).to(FacilityDao.class);

				requestStaticInjection(Organization.class);
				bind(new TypeLiteral<ITypedDao<Organization>>() {
				}).to(OrganizationDao.class);

				requestStaticInjection(Path.class);
				bind(new TypeLiteral<ITypedDao<Path>>() {
				}).to(PathDao.class);

				requestStaticInjection(PathSegment.class);
				bind(new TypeLiteral<ITypedDao<PathSegment>>() {
				}).to(PathSegmentDao.class);

				requestStaticInjection(PersistentProperty.class);
				bind(new TypeLiteral<ITypedDao<PersistentProperty>>() {
				}).to(PersistentPropertyDao.class);

				requestStaticInjection(User.class);
				bind(new TypeLiteral<ITypedDao<User>>() {
				}).to(UserDao.class);

				requestStaticInjection(UserSession.class);
				bind(new TypeLiteral<ITypedDao<UserSession>>() {
				}).to(UserSessionDao.class);

				requestStaticInjection(Vertex.class);
				bind(new TypeLiteral<ITypedDao<Vertex>>() {
				}).to(VertexDao.class);
				
				requestStaticInjection(WirelessDevice.class);
				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);

				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				
			}
		});

		return injector;
	}
}
