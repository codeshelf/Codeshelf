/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfApplicationTest.java,v 1.23 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.application;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.edi.EdiProcessorService;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.MockDao;
import com.codeshelf.model.dao.Result;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.report.PickDocumentGenerator;
import com.codeshelf.service.DummyPropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.server.SessionManagerService;

/**
 * @author jeffw
 *
 */
public class CodeshelfApplicationTest {
/*
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
*/
	/**
	 * Test method for {@link com.codeshelf.application.ServerCodeshelfApplication#startApplication()}.
	 */
	@Test
	public void testStartStopApplication() {
		JvmProperties.load("test");

		MetricsService.setInstance(new DummyMetricsService());
		TenantPersistenceService.setInstance(mock(ITenantPersistenceService.class));
		Facility.DAO = new MockDao<Facility>();
		Aisle.DAO = new MockDao<Aisle>();
		Bay.DAO = new MockDao<Bay>();
		Tier.DAO = new MockDao<Tier>();
		Slot.DAO = new MockDao<Slot>();

		ICsvOrderImporter orderImporter = mock(ICsvOrderImporter.class);
		ICsvInventoryImporter inventoryImporter = mock(ICsvInventoryImporter.class);
		ICsvLocationAliasImporter locationAliasImporter = mock(ICsvLocationAliasImporter.class);
		ICsvOrderLocationImporter orderLocationImporter = mock(ICsvOrderLocationImporter.class);
		ICsvCrossBatchImporter crossBatchImporter = mock(ICsvCrossBatchImporter.class);
		ICsvAislesFileImporter aislesFileImporter = mock(ICsvAislesFileImporter.class);
		EdiProcessorService ediProcessorService = new EdiProcessorService(orderImporter,
			inventoryImporter,
			locationAliasImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter);
		IPickDocumentGenerator pickDocumentGenerator = new PickDocumentGenerator();

		
		WebApiServer adminServer = new WebApiServer();
		final ServerCodeshelfApplication application = new ServerCodeshelfApplication(
			ediProcessorService,
			pickDocumentGenerator,
			adminServer,
			TenantManagerService.getMaybeRunningInstance(),
			new WorkService(),
			MetricsService.getMaybeRunningInstance(),
			new SessionManagerService(),
			new DummyPropertyService());

		final Result checkAppRunning = new Result();

		Thread appThread = new Thread(new Runnable() {
			public void run() {
				try {
					application.startServices();
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
		try {
			appThread.join(60000, 0);
		} catch (InterruptedException e) {
			Assert.fail("interrupted waiting for main app thread to terminate");
		}
		Assert.assertFalse("app thread hung at shutdown",appThread.isAlive());

		Assert.assertTrue("application failed to start", checkAppRunning.result);
	}
}
