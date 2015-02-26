/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.LazyInitializationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.application.ServerMain;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.multitenancy.ITenantManager;
import com.codeshelf.platform.multitenancy.ManagerPersistenceService;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.SessionManagerService;
import com.google.common.collect.ImmutableCollection;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
//import com.codeshelf.model.domain.LocationABC.LocationABCDao;
//import com.codeshelf.model.domain.SubLocationABC.SubLocationDao;

public abstract class DAOTestABC {
	private static Logger LOGGER;

	@Rule
	public TestName testName = new TestName();

	static ServiceManager jvmServiceManager;
	private static SessionManagerService staticSessionManagerService; // one per jvm..
	private static IPropertyService staticPropertyService;
	private static IMetricsService	staticMetricsService;
	private static ITenantPersistenceService staticTenantPersistenceService;
	
	public static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				// jetty websocket
				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
				
				requestStaticInjection(TenantPersistenceService.class);
				bind(ITenantPersistenceService.class).to(TenantPersistenceService.class).in(Singleton.class);
				
				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(DummyMetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyService.class).to(PropertyService.class).in(Singleton.class);
			}
			
			
			@Provides
			@Singleton
			public SessionManagerService createSessionManagerService() {
				SessionManagerService sessionManagerService = new SessionManagerService();
				return sessionManagerService;				
			}
			
		});
		return injector;
	}

	static {
		JvmProperties.load("test");
		LOGGER	= LoggerFactory.getLogger(DAOTestABC.class);
		
		Injector injector = setupInjector();

		// always keep a handle on these per-jvm services
		DAOTestABC.staticSessionManagerService = injector.getInstance(SessionManagerService.class);
		DAOTestABC.staticMetricsService = injector.getInstance(IMetricsService.class);
		DAOTestABC.staticPropertyService = injector.getInstance(IPropertyService.class);
		DAOTestABC.staticTenantPersistenceService = injector.getInstance(ITenantPersistenceService.class);
		
		// start singleton services here, per jvm, not per test
		List<Service> services = new ArrayList<Service>();
		
		services.add(TenantManagerService.getNonRunningInstance()); // self-creating
		services.add(ManagerPersistenceService.getNonRunningInstance()); // self-creating
		services.add(TenantPersistenceService.getNonRunningInstance()); // self-creating
		
		services.add(staticSessionManagerService); // provider injected
		services.add(staticMetricsService); // static injected singleton
		services.add(staticPropertyService); // static injected singleton
		// see doBefore() for ephemeral service manager
		
		jvmServiceManager = new ServiceManager(services);
		try {
			jvmServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
		} catch (TimeoutException e1) {
			throw new RuntimeException("Could not start unit test services",e1);
		}

		// start h2 web service
		try {
			org.h2.tools.Server.createWebServer("-webPort", "8082").start();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try { //Ideally this would be statically initialized once before all of the integration tests
			// Burying the exception allows the normal mode for the design to raise issue,
			//  but in testing assume that it got setup once the first time this is called

			CsServerEndPoint.setSessionManagerService(staticSessionManagerService);
			CsServerEndPoint.setMessageProcessor(injector.getInstance(ServerMessageProcessor.class));
		}
		catch(Exception e) {
			LOGGER.debug("Exception setting session manager / message processor: " + e.toString());
		}
	}
	
	protected ServiceManager ephemeralServiceManager;
	ITenantManager tenantManager;

	
	// ephemeral services that might be generated by subclasses
	protected WorkService workService;
	
	// long-lived services, as they were originally injected, for resetting
	public ITenantPersistenceService tenantPersistenceService = DAOTestABC.staticTenantPersistenceService; 
	protected final SessionManagerService sessionManagerService = DAOTestABC.staticSessionManagerService;
	protected final IPropertyService propertyService = DAOTestABC.staticPropertyService;
	protected final IMetricsService metricsService = DAOTestABC.staticMetricsService;
	
	Facility defaultFacility = null;


	public DAOTestABC() {
		super();
		//this.tenantManager = tenantManager;
		this.tenantManager = TenantManagerService.getMaybeRunningInstance();
	}
	
	public Tenant getDefaultTenant() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	public ITenantPersistenceService getTenantPersistenceService() {
		return TenantPersistenceService.getInstance();
	}
	
	public Facility createFacility() {
		return Facility.createFacility(getDefaultTenant(), this.getTestName(), "Test Facility", Point.getZeroPoint());
	}
	
	public Facility getDefaultFacility() {
		if(defaultFacility == null) {
			defaultFacility = createFacility();
		}
		return defaultFacility;
	}
	
	@Before
	public void doBefore() throws Exception {	
		this.tenantPersistenceService = TenantPersistenceService.getInstance();

		// this will cause DAOs to get statically reinjected, in case they were messed with
		@SuppressWarnings("unused")
		Injector injector = Guice.createInjector(ServerMain.createDaoBindingModule());

		// reset long-lived singleton instances, in case they were messed with
		MetricsService.setInstance(staticMetricsService);
		PropertyService.setInstance(staticPropertyService);
		TenantPersistenceService.setInstance(staticTenantPersistenceService);
		// sessionManager is not statically accessible and basically not mockable, so no worries
		
		// make sure default properties are in the database
		TenantPersistenceService.getInstance().beginTransaction();
        PropertyDao.getInstance().syncPropertyDefaults();
        TenantPersistenceService.getInstance().commitTransaction();
			
        // subclasses may override ephemeralServicesShouldStartAutomatically, if service start needs to be delayed
        if(ephemeralServicesShouldStartAutomatically())
        	initializeEphemeralServiceManager();
	}
	
	protected boolean ephemeralServicesShouldStartAutomatically() {
		return true;
	}

	protected void initializeEphemeralServiceManager() {
		if(ephemeralServiceManager != null) {
			//throw new RuntimeException("could not initialize ephemeralServiceManager (already started)");
		} else {
			// start ephemeral services. these will be stopped in @After
			// must use new service objects (services cannot be restarted)
			List<Service> services = new ArrayList<Service>();
			// services.add(new Service()); e.g.
			this.workService = this.generateWorkService();
			if(this.workService != null)
				services.add(this.workService);
			
			this.ephemeralServiceManager = new ServiceManager(services);
			LOGGER.info("starting ephemeral service manager: {}",ephemeralServiceManager.servicesByState().toString());
		
			try {
				this.ephemeralServiceManager.startAsync().awaitHealthy(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout starting ephemeralServiceManager",e);
			}
		}
	}

	protected WorkService generateWorkService() {
		return new WorkService();
	}

	protected IMetricsService generateMetricsService() {
		return new DummyMetricsService();
	}
	
	@After
	public void doAfter() {
		boolean hadActiveTransactions = this.tenantPersistenceService.rollbackAnyActiveTransactions();
		
		TenantManagerService.getInstance().resetTenant(getDefaultTenant());
		this.tenantPersistenceService.forgetInitialActions(getDefaultTenant());

		if(this.ephemeralServiceManager != null) {
			try {
				this.ephemeralServiceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout stopping ephemeralServiceManager",e);
			}
			ImmutableCollection<Service> failedServices = ephemeralServiceManager.servicesByState().get(State.FAILED);
			Assert.assertTrue(failedServices == null || failedServices.isEmpty());
		}
		
		this.sessionManagerService.reset();

		Assert.assertFalse(hadActiveTransactions);
		
		inspectThreads();
	
	}
	
	protected void inspectThreads() {
		Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
		Set<Thread> threads = traces.keySet();
		int wsThreads=0;
		for(Thread thread : threads) {
			if(thread.getName().startsWith("WebSocketClient")) {
				wsThreads++;
			}
		}
		if(wsThreads>10) {
			Assert.fail(""+wsThreads+" websocketclients"); 
		}
	}

	protected String getTestName() {
		return testName.getMethodName();
	}
	
}
