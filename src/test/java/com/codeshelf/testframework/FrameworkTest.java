package com.codeshelf.testframework;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import lombok.Getter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.DummyService;
import com.codeshelf.application.JvmProperties;
import com.codeshelf.application.WebApiServer;
import com.codeshelf.device.ClientConnectionManagerService;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.SiteControllerMessageProcessor;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.event.EventProducer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.TcpServerInterface;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.multitenancy.ITenantManagerService;
import com.codeshelf.platform.multitenancy.ManagerPersistenceService;
import com.codeshelf.platform.multitenancy.ManagerSchema;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.PersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.InventoryService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.ServiceUtility;
import com.codeshelf.service.WorkService;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.jetty.client.CsClientEndpoint;
import com.codeshelf.ws.jetty.client.MessageCoordinator;
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

public abstract class FrameworkTest implements IntegrationTest {
	public enum Type {
		MINIMAL, // no persistence, no services, null DAOs 
		MOCK_DAO, // no services, mock/stub persistence
		HIBERNATE, // minimal services, hibernate+h2 persistence
		COMPLETE_SERVER // server services, can start Site Controller 
	};

	abstract Type getFrameworkType();

	public abstract boolean ephemeralServicesShouldStartAutomatically(); // return true for WorkService

	private static Logger										LOGGER; // = need to set up logging before creating the logger
	@Getter
	@Rule
	public TestName												testName					= new TestName();

	// service managers for the various test types. for complete server test all of these will be used.
	private static ServiceManager								serverServiceManager		= null;
	private static ServiceManager								persistenceServiceManager	= null;
	private static ServiceManager								siteconServiceManager		= null;

	// app server static (reused) services
	private static SessionManagerService						staticSessionManagerService;
	private static IMetricsService								staticMetricsService;
	private static IPropertyService								staticPropertyService;
	private static ServerMessageProcessor						staticServerMessageProcessor;

	// real non-mock instances
	private static ITenantPersistenceService					realTenantPersistenceService;
	private static ITenantManagerService						realTenantManagerService;
	private boolean												resetRealPersistenceDaos;

	// default IDs to use when generating facility
	protected static String										facilityId					= "F1";
	protected static String										networkId					= CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String										cheId1						= "CHE1";
	@Getter
	protected static NetGuid									cheGuid1					= new NetGuid("0x00009991");
	protected static String										cheId2						= "CHE2";
	@Getter
	protected static NetGuid									cheGuid2					= new NetGuid("0x00009992");

	// site controller services
	private static CsClientEndpoint								staticClientEndpoint;
	private static ClientConnectionManagerService				staticClientConnectionManagerService;
	private static WebSocketContainer							staticWebSocketContainer;

	// instance services
	protected ServiceManager									ephemeralServiceManager;
	protected WorkService										workService;
	protected InventoryService									inventoryService;
	protected EventProducer										eventProducer				= new EventProducer();
	protected WebApiServer										apiServer;

	@Getter
	protected ITenantPersistenceService							tenantPersistenceService;
	@Getter
	protected CsDeviceManager									deviceManager;
	protected SessionManagerService								sessionManagerService;
	protected IPropertyService									propertyService;
	protected IMetricsService									metricsService;

	protected IRadioController									radioController;

	// auto created facility details
	int															facilitiesGenerated;	// automatic serial naming
	private Facility											facility;
	protected UUID												networkPersistentId;
	protected UUID												che1PersistentId;
	protected UUID												che2PersistentId;

	private Integer												port;

	public static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				requestStaticInjection(CsClientEndpoint.class);

				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);

				requestStaticInjection(TenantPersistenceService.class);
				bind(ITenantPersistenceService.class).to(TenantPersistenceService.class).in(Singleton.class);

				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(DummyMetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyService.class).to(PropertyService.class).in(Singleton.class);

				bind(WebSocketContainer.class).toInstance(ContainerProvider.getWebSocketContainer());
			}

			@Provides
			@Singleton
			public SessionManagerService createSessionManagerService() {
				SessionManagerService sessionManagerService = new SessionManagerService();
				return sessionManagerService;
			}

			@Provides
			@Singleton
			public MessageCoordinator createMessageCoordinator() {
				return new MessageCoordinator();
			}

		});
		return injector;
	}

	static {
		JvmProperties.load("test"); // set up logging & environment
		LOGGER = LoggerFactory.getLogger(FrameworkTest.class);

		Injector injector = setupInjector();

		realTenantPersistenceService = TenantPersistenceService.getMaybeRunningInstance();
		realTenantManagerService = TenantManagerService.getMaybeRunningInstance();

		staticMetricsService = injector.getInstance(IMetricsService.class);
		staticMetricsService.startAsync(); // always running, outside of service manager
		ServiceUtility.awaitRunningOrThrow(staticMetricsService); 

		staticPropertyService = injector.getInstance(IPropertyService.class);

		staticSessionManagerService = injector.getInstance(SessionManagerService.class);
		staticServerMessageProcessor = injector.getInstance(ServerMessageProcessor.class);

		// site controller services
		staticClientEndpoint = new CsClientEndpoint();
		staticClientConnectionManagerService = new ClientConnectionManagerService(staticClientEndpoint);
		staticWebSocketContainer = injector.getInstance(WebSocketContainer.class);
	}

	public FrameworkTest() {
		this.port = Integer.getInteger("api.port");
	}

	@Before
	public void doBefore() {
		if (this.getFrameworkType().equals(Type.COMPLETE_SERVER)) // complete server setup has more logs during setup
			LOGGER.info("******************* Setting up test: " + this.testName.getMethodName() + " *******************");

		// reset all services to defaults 
		sessionManagerService = staticSessionManagerService;
		propertyService = staticPropertyService;
		PropertyService.setInstance(propertyService);
		metricsService = staticMetricsService;
		MetricsService.setInstance(metricsService);

		radioController = null;
		deviceManager = null;
		apiServer = null;

		// reset default facility
		facilitiesGenerated = 0;
		facility = null;
		networkPersistentId = null;
		che1PersistentId = null;
		che2PersistentId = null;

		if (this.getFrameworkType().equals(Type.MINIMAL)) {
			disablePersistence();
		} else if (this.getFrameworkType().equals(Type.MOCK_DAO)) {
			setDummyPersistence();
		} else if (this.getFrameworkType().equals(Type.HIBERNATE)) {
			startPersistence();
		} else if (this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			startPersistence();
			startServer();
		}

		if (ephemeralServicesShouldStartAutomatically())
			initializeEphemeralServiceManager();

		LOGGER.info("------------------- Running test: " + this.testName.getMethodName() + " -------------------");
	}

	@After
	public void doAfter() {
		if (this.getFrameworkType().equals(Type.COMPLETE_SERVER)) // complete server setup has more logs during teardown
			LOGGER.info("------------------- Cleanup after test: " + this.testName.getMethodName() + " -------------------");

		if (staticClientConnectionManagerService != null) {
			staticClientConnectionManagerService.setDisconnected();
		}
		if (radioController != null) {
			radioController.stopController();
			radioController = null;
		}

		if (deviceManager != null) {
			deviceManager.unattached();
			deviceManager = null;
		}

		if (apiServer != null) {
			apiServer.stop();
			apiServer = null;
		}

		CsClientEndpoint.setEventListener(null);
		CsClientEndpoint.setMessageProcessor(null);
		CsClientEndpoint.setWebSocketContainer(null);

		if (this.ephemeralServiceManager != null) {
			try {
				this.ephemeralServiceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout stopping ephemeralServiceManager", e);
			}
			ImmutableCollection<Service> failedServices = ephemeralServiceManager.servicesByState().get(State.FAILED);
			Assert.assertTrue(failedServices == null || failedServices.isEmpty());
			ephemeralServiceManager = null;
		}

		sessionManagerService = null;
		propertyService = null;
		metricsService = null;

		if (this.getFrameworkType().equals(Type.HIBERNATE) || this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			Assert.assertFalse(realTenantPersistenceService.rollbackAnyActiveTransactions());
		}
		
		if(resetRealPersistenceDaos) {
			realTenantPersistenceService.resetDaosForTest();
			resetRealPersistenceDaos = false;
		}

		LOGGER.info("******************* Completed test: " + this.testName.getMethodName() + " *******************");
	}

	public <T extends IDomainObject> void useCustomDao(Class<T> domainType, ITypedDao<T> testDao) {
		this.tenantPersistenceService.setDaoForTest(domainType,testDao);
		if(this.getFrameworkType().equals(Type.HIBERNATE) || this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			resetRealPersistenceDaos = true;
		}
	}

	private void disablePersistence() {
		this.tenantPersistenceService = null;
		TenantPersistenceService.setInstance(null);
	}

	private Map<Class<? extends IDomainObject>, ITypedDao<?>> createMockDaos() {
		Map<Class<? extends IDomainObject>, Class<? extends ITypedDao<? extends IDomainObject>>> daoClasses
			= DomainObjectABC.getDaoClasses();
				
		Map<Class<? extends IDomainObject>, ITypedDao<?>> daos 
			= new HashMap<Class<? extends IDomainObject>, ITypedDao<?>>();
		for (Class<? extends IDomainObject> clazz : daoClasses.keySet()) {
			daos.put(clazz, new MockDao<>(clazz));
		}
		return daos;
	}

	private void setupRealPersistenceObjects() {
		TenantManagerService.setInstance(realTenantManagerService);
		
		this.tenantPersistenceService = realTenantPersistenceService;
		TenantPersistenceService.setInstance(tenantPersistenceService);
	}

	private void setDummyPersistence() {
		Map<Class<? extends IDomainObject>, ITypedDao<?>> mockDaos = this.createMockDaos();

		tenantPersistenceService = new MockTenantPersistenceService(mockDaos);

		TenantManagerService.setInstance(new MockTenantManagerService(tenantPersistenceService.getDefaultSchema()));
		TenantPersistenceService.setInstance(tenantPersistenceService);
	}

	protected final void startSiteController() {
		// subclasses call this method to initialize the site controller and connect to the server API
		this.getFacility(); // ensure we have created a facility

		CsClientEndpoint.setWebSocketContainer(staticWebSocketContainer);

		radioController = new RadioController(new TcpServerInterface());
		deviceManager = new CsDeviceManager(radioController, staticClientEndpoint);
		new SiteControllerMessageProcessor(deviceManager, staticClientEndpoint);

		if (siteconServiceManager == null) {
			List<Service> services = new ArrayList<Service>();
			services.add(FrameworkTest.staticClientConnectionManagerService);

			siteconServiceManager = new ServiceManager(services);
			try {
				siteconServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (site controller)", e1);
			}
		}

		staticClientConnectionManagerService.setConnected();
		this.awaitConnection();
	}

	private void startPersistence() {
		setupRealPersistenceObjects();

		if (persistenceServiceManager == null) {
			// initialize server for the first time
			List<Service> services = new ArrayList<Service>();
			ITenantManagerService tms = TenantManagerService.getMaybeRunningInstance();
			PersistenceService<ManagerSchema> mps = ManagerPersistenceService.getMaybeRunningInstance();

			if (!tms.isRunning())
				services.add(tms);
			if (!mps.isRunning())
				services.add(mps);
			if (!realTenantPersistenceService.isRunning())
				services.add(realTenantPersistenceService);

			persistenceServiceManager = new ServiceManager(services);
			try {
				persistenceServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (persistence)", e1);
			}

			// start h2 web interface for debugging
			try {
				org.h2.tools.Server.createWebServer("-webPort", "8082").start();
			} catch (Exception e) {
				// it's probably fine
			}

		} else {
			// not 1st persistence run. need to reset
			Tenant realDefaultTenant = realTenantManagerService.getDefaultTenant();
			realTenantPersistenceService.forgetInitialActions(realDefaultTenant);
			realTenantManagerService.resetTenant(realDefaultTenant);
		}

		// make sure default properties are in the database
		TenantPersistenceService.getInstance().beginTransaction();
		PropertyDao.getInstance().syncPropertyDefaults();
		TenantPersistenceService.getInstance().commitTransaction();
	}

	private void startServer() {
		if (staticSessionManagerService.isRunning())
			staticSessionManagerService.reset();

		if (serverServiceManager == null) {
			// initialize server for the first time
			List<Service> services = new ArrayList<Service>();
			services.add(staticSessionManagerService);
			services.add(staticPropertyService);

			serverServiceManager = new ServiceManager(services);
			try {
				serverServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (server)", e1);
			}
		}

		// [re]set up server endpoint 
		try {
			CsServerEndPoint.setSessionManagerService(staticSessionManagerService);
			CsServerEndPoint.setMessageProcessor(staticServerMessageProcessor);
		} catch (Exception e) {
			LOGGER.debug("Exception setting session manager / message processor: " + e.toString());
		}

		apiServer = new WebApiServer();
		apiServer.start(port, null, null, false, "./");
	}

	private void awaitConnection() {
		// wait for site controller/server connection to be established
		long start = System.currentTimeMillis();
		while (!staticClientEndpoint.isConnected()) {
			LOGGER.debug("Embedded site controller and server are not connected yet");
			ThreadUtils.sleep(50);
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 10000) {
				throw new RuntimeException("Failed to establish connection between embedded site controller and server");
			}
		}
		long lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
		while (lastNetworkUpdate == 0) {
			LOGGER.debug("Embedded site controller has not yet received a network update");
			ThreadUtils.sleep(50);
			lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 10000) {
				throw new RuntimeException("Failed to receive network update in allowed time");
			}
		}
		LOGGER.debug("Embedded site controller and server connected");
	}

	protected final void initializeEphemeralServiceManager() {
		if (ephemeralServiceManager != null) {
			throw new RuntimeException("could not initialize ephemeralServiceManager (already started)");
		} else {
			// start ephemeral services. these will be stopped in @After
			this.ephemeralServiceManager = new ServiceManager(generateEphemeralServices());
			LOGGER.info("starting ephemeral service manager: {}", ephemeralServiceManager.servicesByState().toString());

			try {
				this.ephemeralServiceManager.startAsync().awaitHealthy(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				Assert.fail("timeout starting ephemeralServiceManager: "+ e.getMessage());
			}
		}
	}

	protected List<Service> generateEphemeralServices() {
		List<Service> services = new ArrayList<Service>();

		if (this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			this.workService = this.generateWorkService();
			if (this.workService != null)
				services.add(this.workService);
		}

		if (services.isEmpty()) {
			services.add(new DummyService()); // suppress warning on empty service list
		}
		return services;
	}

	protected WorkService generateWorkService() {
		return new WorkService();
	}

	protected final Facility generateTestFacility() {
		String useFacilityId;
		if (this.facilitiesGenerated > 0) {
			useFacilityId = facilityId + Integer.toString(facilitiesGenerated);
		} else {
			useFacilityId = facilityId;
		}
		facilitiesGenerated++;

		boolean inTransaction = this.tenantPersistenceService.hasActiveTransaction(this.getDefaultTenant());
		if (!inTransaction)
			this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.staticGetDao().findByDomainId(null, useFacilityId);
		if (facility == null) {
			facility = Facility.createFacility( useFacilityId, "", Point.getZeroPoint());
			Facility.staticGetDao().store(facility);
		}

		CodeshelfNetwork network = facility.getNetworks().get(0);
		this.networkPersistentId = network.getPersistentId();

		List<Che> ches = new ArrayList<Che>(network.getChes().values());
		Che che1 = ches.get(0);
		che1.setColor(ColorEnum.MAGENTA);
		che1.setDeviceNetGuid(cheGuid1);
		che1.setDomainId(cheId1);
		this.che1PersistentId = che1.getPersistentId();

		Che che2 = ches.get(1);
		che2.setColor(ColorEnum.WHITE);
		che2.setDeviceNetGuid(cheGuid2);
		che2.setDomainId(cheId2);
		this.che2PersistentId = che2.getPersistentId();

		if (!inTransaction)
			this.getTenantPersistenceService().commitTransaction();

		return facility;
	}

	protected final Facility getFacility() {
		if (facility == null) {
			facility = generateTestFacility();
		}
		return facility;
	}

	public Facility createFacility() {
		if (facility == null)
			return getFacility();
		//else actually create another one
		return generateTestFacility();
	}

	protected final Tenant getDefaultTenant() {
		return TenantPersistenceService.getInstance().getDefaultSchema();
	}

}
