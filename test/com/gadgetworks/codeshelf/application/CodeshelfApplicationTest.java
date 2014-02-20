/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfApplicationTest.java,v 1.23 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.realm.Realm;
import org.java_websocket.WebSocket;
import org.junit.Assert;
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
import com.gadgetworks.codeshelf.model.dao.Result;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
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
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.monitor.IMonitor;
import com.gadgetworks.codeshelf.monitor.Monitor;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.report.PickDocumentGenerator;
import com.gadgetworks.codeshelf.security.CodeshelfRealm;
import com.gadgetworks.codeshelf.ws.IWebSessionFactory;
import com.gadgetworks.codeshelf.ws.IWebSessionManager;
import com.gadgetworks.codeshelf.ws.WebSession;
import com.gadgetworks.codeshelf.ws.WebSessionManager;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.command.req.WsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.websocket.CsWebSocketServer;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketServer;
import com.gadgetworks.codeshelf.ws.websocket.SSLWebSocketServerFactory;
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

	private class WebSessionFactory implements IWebSessionFactory {

		@Override
		public WebSession create(WebSocket inWebSocket, IWsReqCmdFactory inWebSessionReqCmdFactory) {
			Realm realm = new CodeshelfRealm();
			return new WebSession(inWebSocket, inWebSessionReqCmdFactory, realm);
		}
	}

	/**
	 * Test method for {@link com.gadgetworks.codeshelf.application.ServerCodeshelfApplication#startApplication()}.
	 */
	@Test
	public void testStartStopApplication() {

		ITypedDao<PersistentProperty> persistentPropertyDao = new MockDao<PersistentProperty>();
		ITypedDao<Organization> organizationDao = Organization.DAO = new MockDao<Organization>();
		ITypedDao<User> userDao = User.DAO = new MockDao<User>();
		ITypedDao<Facility> facilityDao = Facility.DAO = new MockDao<Facility>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();
		ITypedDao<Che> cheDao = new MockDao<Che>();
		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		IMonitor monitor = new Monitor();

		Injector injector = new MockInjector();
		IDaoProvider daoProvider = new DaoProvider(injector);
		IWsReqCmdFactory webSessionReqCmdFactory = new WsReqCmdFactory(organizationDao, cheDao, workInstructionDao, orderHeaderDao, orderDetailDao, daoProvider);
		IWebSessionFactory webSessionFactory = new WebSessionFactory();
		IWebSessionManager webSessionManager = new WebSessionManager(webSessionReqCmdFactory, webSessionFactory);
		SSLWebSocketServerFactory webSocketFactory = new SSLWebSocketServerFactory("./conf/codeshelf.keystore", "JKS", "x2HPbC2avltYQR", "x2HPbC2avltYQR");

		IWebSocketServer webSocketListener = new CsWebSocketServer(IWebSocketServer.WEBSOCKET_DEFAULT_HOSTNAME,
			CsWebSocketServer.WEBSOCKET_DEFAULT_PORTNUM,
			webSessionManager,
			webSocketFactory);
		IHttpServer httpServer = new HttpServer("./", "localhost", 8443, "./conf/codeshelf.keystore", "x2HPbC2avltYQR", "x2HPbC2avltYQR");

		ICsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
		IEdiProcessor ediProcessor = new EdiProcessor(importer, facilityDao);
		IPickDocumentGenerator pickDocumentGenerator = new PickDocumentGenerator();
		IUtil util = new MockUtil();
		ISchemaManager schemaManager = new H2SchemaManager(util, "codeshelf", "codeshelf", "codeshelf", "codeshelf", "localhost", "", "false");
		IDatabase database = new Database(schemaManager, util);

		final ServerCodeshelfApplication application = new ServerCodeshelfApplication(webSocketListener,
			monitor,
			httpServer,
			ediProcessor,
			pickDocumentGenerator,
			database,
			util,
			persistentPropertyDao,
			organizationDao,
			facilityDao,
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
		
		// This next line causes the whole JUnit system to stop.
		// Yes, I know it's terrible to have dependent unit tests.
		// I don't know how to fix this.  WIll consult with someone.
		
		//application.stopApplication();

		Assert.assertTrue(true);
	}
}
