/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsCheApplication.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.IDevice;
import com.google.inject.Inject;

public final class CsCheApplication implements ICodeshelfApplication {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(CsCheApplication.class);

	private boolean				mIsRunning	= true;
	private IDevice				mCheDevice;
	private IUtil				mUtil;
	private Thread				mShutdownHookThread;
	private Runnable			mShutdownRunnable;

	@Inject
	public CsCheApplication(final IDevice inCheDevice, final IUtil inUtil) {
		mCheDevice = inCheDevice;
		mUtil = inUtil;
	}

	// --------------------------------------------------------------------------
	/**
	 * Setup the JVM environment.
	 */
	private void setupLibraries() {
		LOGGER.warn("CodeShelf version: " + mUtil.getVersionString());
		LOGGER.info("user.dir = " + System.getProperty("user.dir"));
		LOGGER.info("java.class.path = " + System.getProperty("java.class.path"));
		LOGGER.info("java.library.path = " + System.getProperty("java.library.path"));

		// Set a class loader that can access the classpath when searching for resources.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		//System.loadLibrary("jd2xx");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startApplication() {

		setupLibraries();

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Process info: " + processName);

		installShutdownHook();

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		initializeApplicationData();

		// Start the device.
		mCheDevice.start();

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stopApplication() {

		LOGGER.info("Stopping application");

		mCheDevice.stop();

		mIsRunning = false;

		LOGGER.info("Application terminated normally");

	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	private void initializeApplicationData() {

	}

	/* --------------------------------------------------------------------------
	 * Handle the SWT application/UI events.
	 */
	public void handleEvents() {

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
