/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfApplicationTest.java,v 1.12 2012/11/24 04:23:54 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.Socket;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.java_websocket.WebSocketAdapter;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.server.WebSocketServer;
import org.junit.Test;

import com.gadgetworks.codeshelf.edi.CsvImporter;
import com.gadgetworks.codeshelf.edi.EdiProcessor;
import com.gadgetworks.codeshelf.edi.ICsvImporter;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.dao.MockWirelessDeviceDao;
import com.gadgetworks.codeshelf.model.dao.Result;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.CodeshelfSSLWebSocketServerFactory;
import com.gadgetworks.codeshelf.web.websocket.CodeshelfWebSocketServer;
import com.gadgetworks.codeshelf.web.websocket.ICodeshelfWebSocketServer;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

/**
 * @author jeffw
 *
 */
public class CodeshelfApplicationTest {

	private static final String	DB_INIT_URL	= "jdbc:h2:mem:database;DB_CLOSE_DELAY=-1";
	private static final String	DB_URL		= "jdbc:h2:mem:database;SCHEMA=CODESHELF;DB_CLOSE_DELAY=-1";

	public class MockUtil implements IUtil {
		public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
		}

		public String getVersionString() {
			return "";
		}

		public String getApplicationLogDirPath() {
			return ".";
		}

		public String getApplicationInitDatabaseURL() {
			return DB_INIT_URL;
		}

		public String getApplicationDatabaseURL() {
			return DB_URL;
		}

		public String getApplicationDataDirPath() {
			return ".";
		}

		public void exitSystem() {
			System.exit(-1);
		}
	}

	public class MockInjector implements Injector {

		@Override
		public void injectMembers(Object instance) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<TypeConverterBinding> getTypeConverterBindings() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<Class<? extends Annotation>, Scope> getScopeBindings() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Provider<T> getProvider(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Provider<T> getProvider(Key<T> key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Injector getParent() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T getInstance(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T getInstance(Key<T> key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Binding<T> getExistingBinding(Key<T> key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<Key<?>, Binding<?>> getBindings() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Binding<T> getBinding(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> Binding<T> getBinding(Key<T> key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<Key<?>, Binding<?>> getAllBindings() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Injector createChildInjector(Module... modules) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Injector createChildInjector(Iterable<? extends Module> modules) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * Test method for {@link com.gadgetworks.codeshelf.application.CodeShelfApplication#startApplication()}.
	 */
	@Test
	public void testStartStopApplication() {

		ITypedDao<PersistentProperty> persistentPropertyDao = new MockDao<PersistentProperty>();
		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Facility> facilityDao = new MockDao<Facility>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();
		IWirelessDeviceDao wirelessDeviceDao = new MockWirelessDeviceDao();

		Injector injector = new MockInjector();
		IDaoProvider daoProvider = new DaoProvider(injector);
		IWebSessionReqCmdFactory webSessionReqCmdFactory = new WebSessionReqCmdFactory(organizationDao, daoProvider);
		IWebSessionManager webSessionManager = new WebSessionManager(webSessionReqCmdFactory);
		CodeshelfSSLWebSocketServerFactory webSocketFactory = new CodeshelfSSLWebSocketServerFactory("./conf/codeshelf.keystore", "JKS", "x2HPbC2avltYQR", "x2HPbC2avltYQR");
		
		ICodeshelfWebSocketServer webSocketListener = new CodeshelfWebSocketServer(ICodeshelfWebSocketServer.WEBSOCKET_DEFAULT_HOSTNAME,
			CodeshelfWebSocketServer.WEBSOCKET_DEFAULT_PORTNUM,
			webSessionManager,
			webSocketFactory);
		IHttpServer httpServer = new HttpServer("./", "localhost", 8000, "./", "localhost", 8443, "./conf/codeshelf.keystore", "x2HPbC2avltYQR", "x2HPbC2avltYQR");

		ICsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao, uomMasterDao);
		IEdiProcessor ediProcessor = new EdiProcessor(importer, facilityDao);
		IUtil util = new MockUtil();
		ISchemaManager schemaManager = new H2SchemaManager(util, "codeshelf", "codeshelf", "codeshelf", "codeshelf", "localhost", "");
		IDatabase database = new Database(schemaManager, util);

		final CodeShelfApplication application = new CodeShelfApplication(webSocketListener,
			daoProvider,
			httpServer,
			ediProcessor,
			database,
			util,
			persistentPropertyDao,
			organizationDao,
			facilityDao,
			wirelessDeviceDao,
			userDao);

		final Result checkAppRunning = new Result();

		Thread appThread = new Thread(new Runnable() {
			public void run() {
				application.startApplication();
				Assert.assertTrue(true);
				checkAppRunning.result = true;
				application.handleEvents();
			}
		}, "APP_TEST_THREAD");
		appThread.start();

		while (!checkAppRunning.result) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}

		application.stopApplication();

		Assert.assertTrue(true);
	}
}
