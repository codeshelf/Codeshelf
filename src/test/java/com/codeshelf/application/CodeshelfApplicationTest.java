/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfApplicationTest.java,v 1.23 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.application;

import static org.mockito.Mockito.mock;

import org.apache.shiro.mgt.AuthorizingSecurityManager;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.edi.EdiProcessorService;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.model.dao.Result;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.report.PickDocumentGenerator;
import com.codeshelf.security.AuthProviderService;
import com.codeshelf.service.DummyPropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.testframework.MockDaoTest;

/**
 * @author jeffw
 *
 */
public class CodeshelfApplicationTest extends MockDaoTest { 
	/**
	 * Test method for {@link com.codeshelf.application.ServerCodeshelfApplication#startApplication()}.
	 */
	
	@Test
	public void testStartStopApplication() {

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
			this.metricsService,
			this.sessionManagerService,
			new DummyPropertyService(),
			mock(AuthProviderService.class),
			mock(AuthorizingSecurityManager.class));

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

		application.stopApplication(); // this doesn't actually exit
		try {
			appThread.join(60000, 0);
		} catch (InterruptedException e) {
			Assert.fail("interrupted waiting for main app thread to terminate");
		}
		Assert.assertFalse("app thread hung at shutdown",appThread.isAlive());

		Assert.assertTrue("application failed to start", checkAppRunning.result);
	}

}
