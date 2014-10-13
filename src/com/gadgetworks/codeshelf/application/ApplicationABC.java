/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ApplicationABC.java,v 1.4 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.OpenTsdb;
import com.gadgetworks.codeshelf.metrics.OpenTsdbReporter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public abstract class ApplicationABC implements ICodeshelfApplication {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(ApplicationABC.class);

	private boolean				mIsRunning	= true;
	private Thread				mShutdownHookThread;
	private Runnable			mShutdownRunnable;

	private AdminServer mAdminServer;

	@Inject
	public ApplicationABC(AdminServer inAdminServer) {
		mAdminServer = inAdminServer;
	}

	protected abstract void doStartup() throws Exception;

	protected abstract void doShutdown();

	protected abstract void doLoadLibraries();

	protected abstract void doInitializeApplicationData();

	// --------------------------------------------------------------------------
	/**
	 * Setup the JVM environment.
	 */
	private void setupLibraries() {
		LOGGER.info("Codeshelf version: " + Configuration.getVersionString());
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
	public final void startApplication() throws Exception {

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
					//System.out.println("Shutdown signal handled");
				} catch (Exception e) {
					System.out.println("Shutdown signal exception:" + e);
					e.printStackTrace();
				}
			}
		};
		mShutdownHookThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
		Runtime.getRuntime().addShutdownHook(mShutdownHookThread);
	}
	
	protected void startAdminServer() {

		// start admin server, if enabled
		String useAdminServer = System.getProperty("metrics.adminserver");
		
		if ("true".equalsIgnoreCase(useAdminServer)) {
			Integer port = Integer.getInteger("metrics.adminserver.port");
			if(port != null) {
				LOGGER.info("Starting Admin Server");
				mAdminServer.startServer(port);
			} else {
				LOGGER.error("Could not start admin server, metrics.adminserver.port needs to be specified");
			}
		}
		else {
			LOGGER.info("Admin Server not enabled");
		}		
	}
	
	protected void startTsdbReporter() {
		// publish metrics to opentsdb
		String useMetricsReporter = System.getProperty("metrics.reporter.enabled");
		if ("true".equalsIgnoreCase(useMetricsReporter)) {
			String metricsServerUrl = System.getProperty("metrics.reporter.serverurl");
			String intervalStr = System.getProperty("metrics.reporter.interval");
			int interval = Integer.parseInt(intervalStr);

			LOGGER.info("Starting OpenTSDB Reporter writing to "+metricsServerUrl+" in "+interval+" sec intervals");
			MetricRegistry registry = MetricsService.getRegistry();
			String hostName = MetricsService.getInstance().getHostName();
			OpenTsdbReporter.forRegistry(registry)
			      .prefixedWith("")
			      .withTags(ImmutableMap.of("host", hostName))
			      .build(OpenTsdb.forService(metricsServerUrl)
			      .create())
			      .start(interval, TimeUnit.SECONDS);
		}
		else {
			LOGGER.info("Metrics reporter is not enabled");
		}

	}
	
	protected void registerSystemMetrics() {
		// register JVM metrics
		MemoryUsageGaugeSet memoryUsage = new MemoryUsageGaugeSet();
		Map<String, Metric> metrics  = memoryUsage.getMetrics();
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			MetricsService.registerMetric(MetricsGroup.JVM,"memory."+entry.getKey(), entry.getValue());
		}
		ThreadStatesGaugeSet threadStateMetrics = new ThreadStatesGaugeSet();
		metrics  = threadStateMetrics.getMetrics();
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			MetricsService.registerMetric(MetricsGroup.JVM,"thread."+entry.getKey(), entry.getValue());
		}
		GarbageCollectorMetricSet gcMetrics = new GarbageCollectorMetricSet();
		metrics  = gcMetrics.getMetrics();
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			MetricsService.registerMetric(MetricsGroup.JVM,"gc."+entry.getKey(), entry.getValue());
		}
	}
}
