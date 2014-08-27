/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerCodeshelfApplication.java,v 1.17 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.OpenTsdb;
import com.gadgetworks.codeshelf.metrics.OpenTsdbReporter;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.monitor.IMonitor;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.gadgetworks.codeshelf.ws.websocket.IWebSocketServer;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor					mEdiProcessor;
	private IWebSocketServer				mWebSocketServer;
	private IHttpServer						mHttpServer;
	private IPickDocumentGenerator			mPickDocumentGenerator;
	private IDatabase						mDatabase;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;
	private ITypedDao<Facility>				mFacilityDao;
	private ITypedDao<User>					mUserDao;

	private IMonitor						mMonitor;

	private BlockingQueue<String>			mEdiProcessSignalQueue;
	
	JettyWebSocketServer mAlternativeWebSocketServer;
	
	private AdminServer mAdminServer;
	
	private MemoryUsageGaugeSet memoryUsage;
	
	@Inject
	public ServerCodeshelfApplication(final IWebSocketServer inWebSocketServer,
		final IMonitor inMonitor,
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IPickDocumentGenerator inPickDocumentGenerator,
		final IDatabase inDatabase,
		final IUtil inUtil,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final ITypedDao<User> inUserDao,
		final AdminServer inAdminServer,
		final JettyWebSocketServer inAlternativeWebSocketServer) {
		super(inUtil);
		mMonitor = inMonitor;
		mWebSocketServer = inWebSocketServer;
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mDatabase = inDatabase;
		mPickDocumentGenerator = inPickDocumentGenerator;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mUserDao = inUserDao;
		mAdminServer = inAdminServer;
		mAlternativeWebSocketServer = inAlternativeWebSocketServer;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.ApplicationABC#doLoadLibraries()
	 */
	@Override
	protected void doLoadLibraries() {
		//System.loadLibrary("jd2xx");
		//System.loadLibrary("libjSSC-0.9_x86_64");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("------------------------------------------------------------");
		LOGGER.info("Process info: " + processName);

		// mMonitor.logToCentralAdmin("Startup: codeshelf server " + processName);
		
		// register JVM metrics
		memoryUsage = new MemoryUsageGaugeSet();
		Map<String, Metric> memoryMetrics  = memoryUsage.getMetrics();
		for (Entry<String, Metric> entry : memoryMetrics.entrySet()) {
			MetricsService.registerMetric(MetricsGroup.JVM,"memory."+entry.getKey(), entry.getValue());
		}
		mDatabase.start();

		// Start the WebSocket server 
		mAlternativeWebSocketServer.start();

		// Start the EDI process.
		mEdiProcessSignalQueue = new ArrayBlockingQueue<>(100);
		mEdiProcessor.startProcessor(mEdiProcessSignalQueue);
		
		// Start the pick document generator process;
		mPickDocumentGenerator.startProcessor(mEdiProcessSignalQueue);

		mHttpServer.startServer();
		
		// start admin server, if enabled
		String useAdminServer = System.getProperty("metrics.adminserver");
		if ("true".equalsIgnoreCase(useAdminServer)) {
			LOGGER.info("Starting Admin Server");
			mAdminServer.startServer();
		}
		else {
			LOGGER.info("Admin Server not enabled");
		}
		
		// public metrics to opentsdb
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

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		mMonitor.logToCentralAdmin("Shutodwn: codeshelf server " + processName);

		LOGGER.info("Stopping application");

		mHttpServer.stopServer();

		mEdiProcessor.stopProcessor();
		mPickDocumentGenerator.stopProcessor();

		// Stop the web socket manager.
		try {
			mWebSocketServer.stop();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}

		mDatabase.stop();

		LOGGER.info("Application terminated normally");

	}

	private void initPreferencesStore(Organization inOrganization) {
		initPreference(inOrganization,
			PersistentProperty.FORCE_CHANNEL,
			"Preferred wireless channel",
			RadioController.NO_PREFERRED_CHANNEL_TEXT);
		initPreference(inOrganization,
			PersistentProperty.GENERAL_INTF_LOG_LEVEL,
			"Preferred general log level",
			Level.INFO.toString());
		initPreference(inOrganization,
			PersistentProperty.GATEWAY_INTF_LOG_LEVEL,
			"Preferred gateway log level",
			Level.INFO.toString());
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inPropertyID
	 *  @param inDescription
	 *  @param inDefaultValue
	 */
	private void initPreference(Organization inOrganization, String inPropertyID, String inDescription, String inDefaultValue) {
		boolean shouldUpdate = false;

		// Find the property in the DB.
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inOrganization, inPropertyID);

		// If the property doesn't exist then create it.
		if (property == null) {
			property = new PersistentProperty();
			property.setParent(inOrganization);
			property.setDomainId(inPropertyID);
			property.setCurrentValueAsStr(inDefaultValue);
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the stored default value doesn't match then change it.
		if (!property.getDefaultValueAsStr().equals(inDefaultValue)) {
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the property changed then we need to persist the change.
		if (shouldUpdate) {
			try {
				mPersistentPropertyDao.store(property);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

		// Create some demo organizations.
		createOrganizationUser("DEMO1", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO1", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO1", "che@example.com", "testme"); //view + simulate
		createOrganizationUser("DEMO1", "work@example.com", "testme"); //view + simulate
		
		createOrganizationUser("DEMO1", "view@goodeggs.com", "goodeggs"); //view
		createOrganizationUser("DEMO1", "view@accu-logistics.com", "accu-logistics"); //view
		
		createOrganizationUser("DEMO2", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO2", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO2", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO2", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO2", "che@example.com", "testme"); //view + simulate

		// Recompute path positions.
		// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
		for (Organization organization : mOrganizationDao.getAll()) {
			for (Facility facility : organization.getFacilities()) {
				for (Path path : facility.getPaths()) {
					facility.recomputeLocationPathDistances(path);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private void createOrganizationUser(String inOrganizationId, String inDefaultUserId, String inDefaultUserPw) {
		Organization organization = mOrganizationDao.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization();
			organization.setDomainId(inOrganizationId);
			try {
				mOrganizationDao.store(organization);

			} catch (DaoException e) {
				e.printStackTrace();
			}

		}
		if (organization.getUser(inDefaultUserId) == null) {
			organization.createUser(inDefaultUserId, inDefaultUserPw);
		}
	}
}
