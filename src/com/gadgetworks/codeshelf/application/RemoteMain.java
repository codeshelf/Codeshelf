/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: RemoteMain.java,v 1.3 2013/02/12 19:19:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.client.WebSocketClient;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork.CodeshelfNetworkDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Container.ContainerDao;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerKind.ContainerKindDao;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.ContainerUse.ContainerUseDao;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.DropboxService.DropboxServiceDao;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator.EdiDocumentLocatorDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.Item.ItemDao;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderGroup.OrderGroupDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty.PersistentPropertyDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.User.UserDao;
import com.gadgetworks.codeshelf.model.domain.UserSession;
import com.gadgetworks.codeshelf.model.domain.UserSession.UserSessionDao;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;
import com.gadgetworks.codeshelf.web.websocket.CsWebSocketClient;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketClient;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketSslContextGenerator;
import com.gadgetworks.codeshelf.web.websocket.SSLWebSocketClientFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class RemoteMain {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}

	private static final Log	LOGGER	= LogFactory.getLog(RemoteMain.class);

	// --------------------------------------------------------------------------
	/**
	 */
	private RemoteMain() {
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
		ICodeshelfApplication application = injector.getInstance(RemoteCodeshelfApplication.class);
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

				bind(String.class).annotatedWith(Names.named(IWebSocketClient.WEBSOCKET_URI_PROPERTY)).toInstance(System.getProperty("websocket.uri"));

				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_PATH_PROPERTY)).toInstance(System.getProperty("keystore.path"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_TYPE_PROPERTY)).toInstance(System.getProperty("keystore.type"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_STORE_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.store.password"));
				bind(String.class).annotatedWith(Names.named(IWebSocketSslContextGenerator.KEYSTORE_KEY_PASSWORD_PROPERTY)).toInstance(System.getProperty("keystore.key.password"));

				bind(IUtil.class).to(Util.class);
				bind(ISchemaManager.class).to(H2SchemaManager.class);
				bind(IDatabase.class).to(Database.class);
				bind(ICodeshelfApplication.class).to(RemoteCodeshelfApplication.class);
				bind(IWebSocketClient.class).to(CsWebSocketClient.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(WebSocketClient.WebSocketClientFactory.class).to(SSLWebSocketClientFactory.class);
		
				requestStaticInjection(Aisle.class);
				bind(new TypeLiteral<ITypedDao<Aisle>>() {
				}).to(AisleDao.class);

				requestStaticInjection(Bay.class);
				bind(new TypeLiteral<ITypedDao<Bay>>() {
				}).to(BayDao.class);

				requestStaticInjection(CodeshelfNetwork.class);
				bind(new TypeLiteral<ITypedDao<CodeshelfNetwork>>() {
				}).to(CodeshelfNetworkDao.class);

				requestStaticInjection(Container.class);
				bind(new TypeLiteral<ITypedDao<Container>>() {
				}).to(ContainerDao.class);

				requestStaticInjection(ContainerKind.class);
				bind(new TypeLiteral<ITypedDao<ContainerKind>>() {
				}).to(ContainerKindDao.class);

				requestStaticInjection(ContainerUse.class);
				bind(new TypeLiteral<ITypedDao<ContainerUse>>() {
				}).to(ContainerUseDao.class);

				requestStaticInjection(EdiDocumentLocator.class);
				bind(new TypeLiteral<ITypedDao<EdiDocumentLocator>>() {
				}).to(EdiDocumentLocatorDao.class);

				requestStaticInjection(DropboxService.class);
				bind(new TypeLiteral<ITypedDao<DropboxService>>() {
				}).to(DropboxServiceDao.class);

				requestStaticInjection(Facility.class);
				bind(new TypeLiteral<ITypedDao<Facility>>() {
				}).to(FacilityDao.class);

				requestStaticInjection(Item.class);
				bind(new TypeLiteral<ITypedDao<Item>>() {
				}).to(ItemDao.class);

				requestStaticInjection(ItemMaster.class);
				bind(new TypeLiteral<ITypedDao<ItemMaster>>() {
				}).to(ItemMasterDao.class);

				requestStaticInjection(LocationABC.class);
				bind(new TypeLiteral<ITypedDao<LocationABC>>() {
				}).to(LocationDao.class);

				requestStaticInjection(OrderDetail.class);
				bind(new TypeLiteral<ITypedDao<OrderDetail>>() {
				}).to(OrderDetailDao.class);

				requestStaticInjection(OrderHeader.class);
				bind(new TypeLiteral<ITypedDao<OrderHeader>>() {
				}).to(OrderHeaderDao.class);

				requestStaticInjection(OrderGroup.class);
				bind(new TypeLiteral<ITypedDao<OrderGroup>>() {
				}).to(OrderGroupDao.class);

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

				requestStaticInjection(UomMaster.class);
				bind(new TypeLiteral<ITypedDao<UomMaster>>() {
				}).to(UomMasterDao.class);

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
				bind(new TypeLiteral<IWirelessDeviceDao>() {
				}).to(WirelessDeviceDao.class);

				requestStaticInjection(WorkArea.class);
				bind(new TypeLiteral<ITypedDao<WorkArea>>() {
				}).to(WorkAreaDao.class);

				requestStaticInjection(WorkInstruction.class);
				bind(new TypeLiteral<ITypedDao<WorkInstruction>>() {
				}).to(WorkInstructionDao.class);

				//				requestStaticInjection(WirelessDevice.class);
				//				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
			}
		});

		return injector;
	}
}
