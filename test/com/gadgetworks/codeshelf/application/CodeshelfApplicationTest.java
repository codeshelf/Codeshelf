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

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.CrossBatchCsvImporter;
import com.gadgetworks.codeshelf.edi.EdiProcessor;
import com.gadgetworks.codeshelf.edi.ICsvAislesFileImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.LocationAliasCsvImporter;
import com.gadgetworks.codeshelf.edi.OrderLocationCsvImporter;
import com.gadgetworks.codeshelf.edi.OutboundOrderCsvImporter;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.dao.Result;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.report.PickDocumentGenerator;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
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
	 * Test method for {@link com.gadgetworks.codeshelf.application.ServerCodeshelfApplication#startApplication()}.
	 */
	@Test
	public void testStartStopApplication() {
		Configuration.loadConfig("test");

		ITypedDao<PersistentProperty> persistentPropertyDao = new MockDao<PersistentProperty>();
		ITypedDao<Organization> organizationDao = Organization.DAO = new MockDao<Organization>();
		ITypedDao<User> userDao = User.DAO = new MockDao<User>();
		ITypedDao<Facility> facilityDao = Facility.DAO = new MockDao<Facility>();
		ITypedDao<Aisle> aisleDao = Aisle.DAO = new MockDao<Aisle>();
		ITypedDao<Bay> bayDao = Bay.DAO = new MockDao<Bay>();
		ITypedDao<Tier> tierDao = Tier.DAO = new MockDao<Tier>();
		ITypedDao<Slot> slotDao = Slot.DAO = new MockDao<Slot>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();
		//ITypedDao<Che> cheDao = new MockDao<Che>();
		//ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		ITypedDao<LocationAlias> locationAliasDao = new MockDao<LocationAlias>();
		ITypedDao<OrderLocation> orderLocationDao = new MockDao<OrderLocation>();

		//Injector injector = new MockInjector();
		//IDaoProvider daoProvider = new DaoProvider(injector);

		IHttpServer httpServer = new HttpServer("./",
			"localhost",
			8443,
			"conf/localhost.jks",
			"1qazse4",
			"1qazse4");

		ICsvOrderImporter orderImporter = new OutboundOrderCsvImporter(orderGroupDao,
			orderHeaderDao,
			orderDetailDao,
			containerDao,
			containerUseDao,
			itemMasterDao,
			uomMasterDao);
		ICsvInventoryImporter inventoryImporter = new InventoryCsvImporter(itemMasterDao, itemDao, uomMasterDao);
		ICsvLocationAliasImporter locationAliasImporter = new LocationAliasCsvImporter(locationAliasDao);
		ICsvOrderLocationImporter orderLocationImporter = new OrderLocationCsvImporter(orderLocationDao);
		ICsvCrossBatchImporter crossBatchImporter = new CrossBatchCsvImporter(orderGroupDao,
			orderHeaderDao,
			orderDetailDao,
			containerDao,
			containerUseDao,
			uomMasterDao);
		ICsvAislesFileImporter aislesFileImporter = new AislesFileCsvImporter(aisleDao,
			bayDao,
			tierDao,
			slotDao);
		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter,
			inventoryImporter,
			locationAliasImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter,
			facilityDao);
		IPickDocumentGenerator pickDocumentGenerator = new PickDocumentGenerator();
		ISchemaManager schemaManager = new H2SchemaManager(
			"codeshelf",
			"codeshelf",
			"codeshelf",
			"codeshelf",
			"localhost",
			"");
		
		AdminServer adminServer = new AdminServer();
		
		JettyWebSocketServer jettyServer = new JettyWebSocketServer();

		final ServerCodeshelfApplication application = new ServerCodeshelfApplication(
			httpServer,
			ediProcessor,
			pickDocumentGenerator,
			persistentPropertyDao,
			organizationDao,
			facilityDao,
			userDao,
			adminServer,
			jettyServer);

		final Result checkAppRunning = new Result();

		Thread appThread = new Thread(new Runnable() {
			public void run() {
				try {
					application.startApplication();
					Assert.assertTrue(true);
					checkAppRunning.result = true;
					application.handleEvents();
				}
				catch(Exception e) {
					Assert.fail("Application should not have thrown an exception on startup: " + e);
				}
			}
		}, "APP_TEST_THREAD");
		appThread.start();

		while (appThread.isAlive() && !checkAppRunning.result) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}

		// This next line causes the whole JUnit system to stop.
		// Yes, I know it's terrible to have dependent unit tests.
		// I don't know how to fix this.  WIll consult with someone.

		application.stopApplication();

		Assert.assertTrue("application failed to start", checkAppRunning.result);
	}
}
