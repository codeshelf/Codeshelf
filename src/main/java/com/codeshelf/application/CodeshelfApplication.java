/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ApplicationABC.java,v 1.4 2013/04/01 23:42:40 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codeshelf.device.ICsDeviceManager;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.OpenTsdb;
import com.codeshelf.metrics.OpenTsdbReporter;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;

public abstract class CodeshelfApplication implements ICodeshelfApplication {
	public enum ShutdownCleanupReq {
		NONE , 
		DROP_SCHEMA , 
		DELETE_ORDERS_WIS ,
		DELETE_ORDERS_WIS_INVENTORY
		};
		
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CodeshelfApplication.class);

	private boolean				mIsRunning	= true;
	private Thread				mShutdownHookThread;
	private Runnable			mShutdownRunnable;

	private List<Service> services = new ArrayList<Service>(); // subclass must register services before starting app 
	private ServiceManager	serviceManager = null;

	private WebApiServer apiServer;

	@Inject
	public CodeshelfApplication(WebApiServer apiServer) {
		this.apiServer = apiServer;
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
		LOGGER.trace("user.dir = " + System.getProperty("user.dir"));
		LOGGER.trace("java.class.path = " + System.getProperty("java.class.path"));
		LOGGER.trace("java.library.path = " + System.getProperty("java.library.path"));

		// Set a class loader that can access the classpath when searching for resources.
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		// The applications load the native libraries they need from the classpath.
		doLoadLibraries();
	}

	protected final void registerService(Service service) {
		if(serviceManager != null)
			throw new IllegalArgumentException("cannot register service, serviceManager is already up");
		if(service == null)
			throw new NullPointerException("null Service");
		if(service.state().equals(Service.State.NEW)) {
			this.services.add(service);
		} else {
			LOGGER.error("cannot register service that has already been started");
		}
	}
	// --------------------------------------------------------------------------
	/**
	 */
	public final void startApplication() throws Exception {
		LOGGER.debug("startApplication() beginning");

		setupLibraries();

		String processName = ManagementFactory.getRuntimeMXBean().getName();

		installShutdownHook();
		if(services.isEmpty())
			services.add(new DummyService());
		
		serviceManager = new ServiceManager(services);
		serviceManager.startAsync();
		try {
			serviceManager.awaitHealthy(30,TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			LOGGER.error("timeout starting services", e);
		}

		// TODO: other services like this

		doStartup();

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		doInitializeApplicationData();
		
		//Need the following line to know at a glance when startup is complete
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Started app version {} - process info {} ",JvmProperties.getVersionString(),processName);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void stopApplication(ShutdownCleanupReq cleanup) {
		switch (cleanup) {
			case NONE:
				break;
			case DROP_SCHEMA:
				TenantManagerService.getInstance().dropDefaultSchema();
				break;
			case DELETE_ORDERS_WIS:
				TenantManagerService.getInstance().deleteDefaultOrdersWis();
				break;
			case DELETE_ORDERS_WIS_INVENTORY:
				TenantManagerService.getInstance().deleteDefaultOrdersWisInventory();
				break;
			default:
				break;

		}

		LOGGER.info("Stopping application");
		
		doShutdown();
		
		serviceManager.stopAsync();
		try {
			serviceManager.awaitStopped(60, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException("failure stopping services");
		}


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
					stopApplication(CodeshelfApplication.ShutdownCleanupReq.NONE);
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
	
	protected void startApiServer(ICsDeviceManager deviceManager, Integer port) {
		if(port != null) {
			boolean enableSchemaManagement = "true".equalsIgnoreCase(System.getProperty("adminserver.schemamanagement"));

			LOGGER.info("Starting Admin Server on port "+port+", schema management "+(enableSchemaManagement?"enabled":"disabled"));
			apiServer.start(port,deviceManager,this,enableSchemaManagement,System.getProperty("webapp.content.path"));
		}
		// else do not start
	}
	
	protected void stopApiServer() {
		apiServer.stop();
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
			LOGGER.debug("Metrics reporter is not enabled");
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
