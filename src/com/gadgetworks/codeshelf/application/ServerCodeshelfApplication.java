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
import com.gadgetworks.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.metrics.OpenTsdb;
import com.gadgetworks.codeshelf.metrics.OpenTsdbReporter;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public final class ServerCodeshelfApplication extends ApplicationABC {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ServerCodeshelfApplication.class);

	private IEdiProcessor					mEdiProcessor;
	private IHttpServer						mHttpServer;
	private IPickDocumentGenerator			mPickDocumentGenerator;
	private PersistenceService				persistencyService;

	private ITypedDao<PersistentProperty>	mPersistentPropertyDao;
	private ITypedDao<Organization>			mOrganizationDao;

	private BlockingQueue<String>			mEdiProcessSignalQueue;

	JettyWebSocketServer webSocketServer;

	private AdminServer mAdminServer;

	private MemoryUsageGaugeSet memoryUsage;

	@Inject
	public ServerCodeshelfApplication(
		final IHttpServer inHttpServer,
		final IEdiProcessor inEdiProcessor,
		final IPickDocumentGenerator inPickDocumentGenerator,
		final ITypedDao<PersistentProperty> inPersistentPropertyDao,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final ITypedDao<User> inUserDao,
		final AdminServer inAdminServer,
		final JettyWebSocketServer inAlternativeWebSocketServer) {
		super();
		mHttpServer = inHttpServer;
		mEdiProcessor = inEdiProcessor;
		mPickDocumentGenerator = inPickDocumentGenerator;
		mPersistentPropertyDao = inPersistentPropertyDao;
		mOrganizationDao = inOrganizationDao;
		mAdminServer = inAdminServer;
		webSocketServer = inAlternativeWebSocketServer;
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
	protected void doStartup() throws Exception {

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		LOGGER.info("Process info: " + processName);

		// register JVM metrics
		memoryUsage = new MemoryUsageGaugeSet();
		Map<String, Metric> memoryMetrics  = memoryUsage.getMetrics();
		for (Entry<String, Metric> entry : memoryMetrics.entrySet()) {
			MetricsService.registerMetric(MetricsGroup.JVM,"memory."+entry.getKey(), entry.getValue());
		}

		// Start the WebSocket server
		webSocketServer.start();

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

		// create health checks
		DatabaseConnectionHealthCheck dbCheck = new DatabaseConnectionHealthCheck(persistencyService);
		MetricsService.registerHealthCheck(MetricsGroup.Database, dbCheck.getName(), dbCheck);

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
		LOGGER.info("Stopping application");
		mHttpServer.stopServer();
		mEdiProcessor.stopProcessor();
		mPickDocumentGenerator.stopProcessor();
		// Stop the web socket server
		try {
			webSocketServer.stop();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to stop WebSocket server", e);
		}
		LOGGER.info("Application terminated normally");
	}

	@SuppressWarnings("unused")
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

		// Create a demo organization
		createOrganizationUser("DEMO1", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO1", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO1", "che@example.com", "testme"); //view + simulate
		createOrganizationUser("DEMO1", "work@example.com", "testme"); //view + simulate

		createOrganizationUser("DEMO1", "view@goodeggs.com", "goodeggs"); //view
		createOrganizationUser("DEMO1", "view@accu-logistics.com", "accu-logistics"); //view

		// Recompute path positions,
		//   and ensure IronMq configuration
		//   and create a default site controller user if doesn't already exist
		for (Organization organization : mOrganizationDao.getAll()) {
			for (Facility facility : organization.getFacilities()) {
				for (Path path : facility.getPaths()) {
					// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
					facility.recomputeLocationPathDistances(path);
				}

				// create a default site controller and user for the first facility you see
				// this should go away
				for(CodeshelfNetwork network : facility.getNetworks()) {
					network.createDefaultSiteControllerUser(); // does nothing if user already exists
				}

				facility.ensureEdiExportService(); // This is weak, but the only place we know that runs once after most data is present
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private User createOrganizationUser(String inOrganizationId, String inDefaultUserId, String inDefaultUserPw) {
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
		User user = organization.getUser(inDefaultUserId);
		if (user == null) {
			user = organization.createUser(inDefaultUserId, inDefaultUserPw, null);
		}
		return user;
	}
}
