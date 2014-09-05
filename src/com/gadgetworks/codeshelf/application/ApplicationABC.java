/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ApplicationABC.java,v 1.4 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public abstract class ApplicationABC implements ICodeshelfApplication {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(ApplicationABC.class);

	private boolean				mIsRunning	= true;
	private Thread				mShutdownHookThread;
	private Runnable			mShutdownRunnable;

	@Inject
	public ApplicationABC() {
	}

	protected abstract void doStartup();

	protected abstract void doShutdown();

	protected abstract void doLoadLibraries();

	protected abstract void doInitializeApplicationData();

	// --------------------------------------------------------------------------
	/**
	 * Setup the JVM environment.
	 */
	private void setupLibraries() {
		LOGGER.warn("Codeshelf version: " + Util.getVersionString());
		LOGGER.info("user.dir = " + System.getProperty("user.dir"));
		LOGGER.info("java.class.path = " + System.getProperty("java.class.path"));
		LOGGER.info("java.library.path = " + System.getProperty("java.library.path"));

		// Set a class loader that can access the classpath when searching for resources.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		// The applications load the native libraries they need from the classpath.
		doLoadLibraries();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void startApplication() {

		setupLibraries();

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Process info: " + processName);

		installShutdownHook();

		doStartup();

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		doInitializeApplicationData();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void stopApplication() {

		LOGGER.info("Stopping application");

		doShutdown();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

	}

	/* --------------------------------------------------------------------------
	 * Handle the SWT application/UI events.
	 */
	public final void handleEvents() {

		while (mIsRunning) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			} catch (RuntimeException inRuntimeException) {
				// We have to catch RuntimeExceptions, because SWT natives do throw them sometime and then don't handle them.
				LOGGER.error("Caught runtime exception", inRuntimeException);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void installShutdownHook() {
		// Prepare the shutdown hook.
		mShutdownRunnable = new Runnable() {
			public void run() {
				// Only execute this hook if the application is still running at (external) shutdown.
				// (This is to help where the shutdown is done externally and not through our own means.)
				if (mIsRunning) {
					stopApplication();
				}
			}
		};
		mShutdownHookThread = new Thread() {
			public void run() {
				try {
					LOGGER.info("Shutdown signal received");
					// Start the shutdown thread to cleanup and shutdown everything in an orderly manner.
					Thread shutdownThread = new Thread(mShutdownRunnable);
					// Set the class loader for this thread, so we can get stuff out of our own JARs.
					//shutdownThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
					shutdownThread.start();
					long time = System.currentTimeMillis();
					// Wait until the shutdown thread succeeds, but not more than 20 sec.
					while ((mIsRunning) && ((System.currentTimeMillis() - time) < 20000)) {
						Thread.sleep(1000);
					}
					System.out.println("Shutdown signal handled");
				} catch (Exception e) {
					System.out.println("Shutdown signal exception:" + e);
					e.printStackTrace();
				}
			}
		};
		mShutdownHookThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
		Runtime.getRuntime().addShutdownHook(mShutdownHookThread);
	}
}
