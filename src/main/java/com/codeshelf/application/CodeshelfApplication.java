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
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.OpenTsdb;
import com.codeshelf.metrics.OpenTsdbReporter;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;

public abstract class CodeshelfApplication implements ICodeshelfApplication {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CodeshelfApplication.class);

	private boolean				mIsRunning	= true;

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
		if(service == null) {
			LOGGER.warn("not registering null Service");
		} else {
			State state = service.state();
			if(state == null) {
				LOGGER.warn("not registering apparent mock service {}",service.getClass().getSimpleName());
			} else {
				if(state.equals(Service.State.NEW)) {
					this.services.add(service);
				} else {
					LOGGER.warn("not registering service that has already been started ({})",service.getClass().getSimpleName());
				}	
			}
		}
	}
	// --------------------------------------------------------------------------
	/**
	 */
	public final void startApplication() throws Exception {
		if(serviceManager == null) {
			throw new RuntimeException("application cannot start, need to startServices first");
		}
		LOGGER.debug("startApplication() beginning");

		setupLibraries();

		doStartup();

		// Some persistent objects need some of their fields set to a base/start state when the system restarts.
		doInitializeApplicationData();
		
		//Need the following line to know at a glance when startup is complete
		LOGGER.info("------------------------------------------------------------");
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("Started app version {} - process info {} ",JvmProperties.getVersionString(),processName);
	}
	
	public final void startServices() {
		installShutdownHook();
		if(services.isEmpty())
			services.add(new DummyService());
		
		serviceManager = new ServiceManager(services);
		LOGGER.info("About to start application services: {}",serviceManager.servicesByState().toString());
		serviceManager.startAsync();
		try {
			serviceManager.awaitHealthy(60,TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			LOGGER.error("timeout starting services", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void stopApplication() {

		LOGGER.info("Stopping application. Services are: {}",this.serviceManager.servicesByState().toString());
		
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
		Thread mShutdownHookThread = new Thread() {
			public void run() {
				try {
					LOGGER.info("Shutdown signal received");
					stopApplication();
				} catch (Exception e) {
					System.out.println("Shutdown signal exception:" + e);
					e.printStackTrace();
				}
			}
		};
		mShutdownHookThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
		Runtime.getRuntime().addShutdownHook(mShutdownHookThread);
	}
	
	protected void startApiServer(CsDeviceManager deviceManager, Integer port) {
		if(port != null) {
			apiServer.start(port,deviceManager,this);
			LOGGER.info("Starting Admin Server on port "+port);
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
			MetricRegistry registry = MetricsService.getInstance().getMetricsRegistry();
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
			MetricsService.getInstance().registerMetric(MetricsGroup.JVM,"memory."+entry.getKey(), entry.getValue());
		}
		ThreadStatesGaugeSet threadStateMetrics = new ThreadStatesGaugeSet();
		metrics  = threadStateMetrics.getMetrics();
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			MetricsService.getInstance().registerMetric(MetricsGroup.JVM,"thread."+entry.getKey(), entry.getValue());
		}
		GarbageCollectorMetricSet gcMetrics = new GarbageCollectorMetricSet();
		metrics  = gcMetrics.getMetrics();
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			MetricsService.getInstance().registerMetric(MetricsGroup.JVM,"gc."+entry.getKey(), entry.getValue());
		}
	}
}
