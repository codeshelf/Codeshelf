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

import com.codeshelf.behavior.DummyPropertyBehavior;
import com.codeshelf.edi.EdiImportService;
import com.codeshelf.model.dao.Result;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.testframework.ServerTest;
import com.google.inject.Provider;

/**
 * @author jeffw
 *
 */
public class CodeshelfApplicationTest extends ServerTest {
	// could be MockDaoTest, but if it runs first it would 
	// try to instantiate/own TenantPersistence before test framework
	
	/**
	 * Test method for {@link com.codeshelf.application.ServerCodeshelfApplication#startApplication()}.
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testStartStopApplication() {

		Provider mockProvider= mock(Provider.class);
		EdiImportService ediImportService = new EdiImportService(mockProvider,
			mockProvider,
			mockProvider,
			mockProvider,
			mockProvider,
			mockProvider);

		WebApiServer adminServer = new WebApiServer();
		final ServerCodeshelfApplication application = new ServerCodeshelfApplication(
			ediImportService,
			adminServer,
			this.tenantManagerService,
			this.metricsService,
			this.webSocketManagerService,
			this.ediExporterService,
			new DummyPropertyBehavior(),
			new TokenSessionService(),
			mock(AuthorizingSecurityManager.class),
			null,
			this.emailService,
			this.templateService);

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
