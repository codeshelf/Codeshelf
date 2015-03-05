package com.codeshelf.testframework;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import lombok.Getter;

import org.atteo.classindex.ClassIndex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.DummyService;
import com.codeshelf.application.JvmProperties;
import com.codeshelf.application.ServerMain;
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
import com.codeshelf.service.PropertyService;
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

	
	private static Logger LOGGER; // = need to set up logging before creating the logger
	@Getter
	@Rule 
	public TestName testName = new TestName();

	// service managers for the various test types. for complete server test all of these will be used.
	private static ServiceManager serverServiceManager = null;
	private static ServiceManager persistenceServiceManager = null;
	private static ServiceManager siteconServiceManager = null;

	// app server static (reused) services
	private static SessionManagerService staticSessionManagerService; 
	private static IMetricsService staticMetricsService; 
	private static IPropertyService staticPropertyService; 
	private static ServerMessageProcessor	staticServerMessageProcessor;
	//private static MessageCoordinator staticServerMessageCoordinator;

	// real non-mock instance
	private static ITenantPersistenceService realTenantPersistenceService;
	private static ITenantManagerService realTenantManagerService;
	
	// default IDs to use when generating facility
	protected static String		facilityId			= "F1";
	protected static String		networkId			= CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String		cheId1				= "CHE1";
	@Getter
	protected static NetGuid	cheGuid1			= new NetGuid("0x00009991");
	protected static String		cheId2				= "CHE2";
	@Getter
	protected static NetGuid	cheGuid2			= new NetGuid("0x00009992");
	
	// site controller services
	private static CsClientEndpoint staticClientEndpoint;
	private static ClientConnectionManagerService	staticClientConnectionManagerService;
	private static WebSocketContainer staticWebSocketContainer;

	// these managed statics are saved/restored to avoid breaking tests that don't subclass this
	private ITenantManagerService savedTenantManagerServiceService = null;
	private ITenantPersistenceService	savedPersistenceServiceInstance = null; 
	private IMetricsService	savedMetricsService = null;
	private IPropertyService savedPropertyService;
	private Map<Class<? extends IDomainObject>, ITypedDao<?>> savedDaos = null;

	// instance services
	protected ServiceManager ephemeralServiceManager;
	protected WorkService workService;
	protected EventProducer eventProducer = new EventProducer();
	protected WebApiServer	apiServer;

	@Getter
	protected ITenantPersistenceService tenantPersistenceService;
	@Getter
	protected CsDeviceManager deviceManager;
	protected SessionManagerService sessionManagerService;
	protected IPropertyService propertyService;
	protected IMetricsService metricsService;

	protected IRadioController	radioController;

	// auto created facility details
	int facilitiesGenerated; // automatic serial naming
	private Facility facility;
	protected UUID	networkPersistentId;
	protected UUID	che1PersistentId;
	protected UUID	che2PersistentId;

	private Integer	port;
		
	public static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				requestStaticInjection(CsClientEndpoint.class);

				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);

				if(!TenantPersistenceService.exists()) {
					// in case legacy DAOTestABC framework already initialized this
					requestStaticInjection(TenantPersistenceService.class);
					bind(ITenantPersistenceService.class).to(TenantPersistenceService.class).in(Singleton.class);
				}
				
				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(DummyMetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyService.class).to(PropertyService.class).in(Singleton.class);

				// site controller bindings
				bind(WebSocketContainer.class).toInstance(ContainerProvider.getWebSocketContainer());

				// we will create these manually per-test:
				//bind(IRadioController.class).to(RadioController.class);
				//bind(IGatewayInterface.class).to(TcpServerInterface.class);
			
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
		LOGGER	= LoggerFactory.getLogger(FrameworkTest.class);
		
		Injector injector = setupInjector();

		realTenantPersistenceService = TenantPersistenceService.getMaybeRunningInstance();
		realTenantManagerService = TenantManagerService.getMaybeRunningInstance();

		staticMetricsService = injector.getInstance(IMetricsService.class);
		staticMetricsService.startAsync().awaitRunning(); // always running, outside of service manager
		
		staticPropertyService = injector.getInstance(IPropertyService.class);

		staticSessionManagerService = injector.getInstance(SessionManagerService.class);
		staticServerMessageProcessor = injector.getInstance(ServerMessageProcessor.class);

		// site controller services
		staticClientEndpoint = new CsClientEndpoint();
		staticClientConnectionManagerService = new ClientConnectionManagerService(staticClientEndpoint);
		staticWebSocketContainer = injector.getInstance(WebSocketContainer.class);
		try {
			org.h2.tools.Server.createWebServer("-webPort", "8082").start();
		} catch (Exception e) {
			// it's probably fine
		} 
	}
	
	public FrameworkTest() {
		this.port = Integer.getInteger("api.port");
	}
	
	@Before
	public void doBefore() {
		if(this.getFrameworkType().equals(Type.COMPLETE_SERVER)) // complete server setup has more logs during setup
			LOGGER.info("******************* Setting up test: "+this.testName.getMethodName()+" *******************");

		// save statics to restore after test
		// TODO: make this list shorter ;)
		if(MetricsService.exists()) {
			this.savedMetricsService = MetricsService.getMaybeRunningInstance();
		} else {
			this.savedMetricsService = null;
		}
		if(PropertyService.exists()) {
			this.savedPropertyService = PropertyService.getMaybeRunningInstance();
		} else {
			this.savedPropertyService = null;
		}
		if(TenantPersistenceService.exists()) {
			this.savedPersistenceServiceInstance = TenantPersistenceService.getMaybeRunningInstance(); // restore after
		} else {
			this.savedPersistenceServiceInstance = null;
		}
		if(TenantManagerService.exists()) {
			this.savedTenantManagerServiceService = TenantManagerService.getMaybeRunningInstance();
		} else {
			this.savedTenantManagerServiceService = null;
		}
		this.savedDaos = this.getStaticDaos();
		
		// reset all services to defaults in case changed by a test
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

		if(this.getFrameworkType().equals(Type.MOCK_DAO)) {
			setDummyPersistence();
		} else if(this.getFrameworkType().equals(Type.COMPLETE_SERVER) || this.getFrameworkType().equals(Type.HIBERNATE)) {
			startPersistence();
			
			if(staticSessionManagerService.isRunning())
				staticSessionManagerService.reset();

	        if(this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
	        	startServer();
	        }
		} else {
			disablePersistence();
		}
        
		if(ephemeralServicesShouldStartAutomatically())
        	initializeEphemeralServiceManager();
		
		LOGGER.info("------------------- Running test: "+this.testName.getMethodName()+" -------------------");
	}

	@After
	public void doAfter() {
		if(this.getFrameworkType().equals(Type.COMPLETE_SERVER)) // complete server setup has more logs during teardown
			LOGGER.info("------------------- Cleanup after test: "+this.testName.getMethodName()+" -------------------");
		
		if(staticClientConnectionManagerService != null) {
			staticClientConnectionManagerService.setDisconnected();
		}
		if(radioController != null) {
			this.radioController.stopController();
			radioController = null;
		}
		
		if(deviceManager != null) {
			deviceManager.unattached();
			deviceManager = null;
		}
		
		if(apiServer != null) {
			apiServer.stop();
			apiServer = null;
		}

		CsClientEndpoint.setEventListener(null);
		CsClientEndpoint.setMessageProcessor(null);
		CsClientEndpoint.setWebSocketContainer(null);
		
		if(this.ephemeralServiceManager != null) {
			try {
				this.ephemeralServiceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout stopping ephemeralServiceManager",e);
			}
			ImmutableCollection<Service> failedServices = ephemeralServiceManager.servicesByState().get(State.FAILED);
			Assert.assertTrue(failedServices == null || failedServices.isEmpty());
		}

		sessionManagerService = null;
		propertyService = null;
		metricsService = null;

		ITenantPersistenceService duringTestPersistenceInstance = TenantPersistenceService.getMaybeRunningInstance();
		TenantPersistenceService.setInstance(this.savedPersistenceServiceInstance);
		TenantManagerService.setInstance(savedTenantManagerServiceService);
		MetricsService.setInstance(savedMetricsService);
		PropertyService.setInstance(savedPropertyService);
		this.setStaticDaos(this.savedDaos);

		if(this.getFrameworkType().equals(Type.HIBERNATE) 
				|| this.getFrameworkType().equals(Type.COMPLETE_SERVER)
				|| duringTestPersistenceInstance == realTenantPersistenceService) {

			// for persistence tests, or any test that may have accessed persistence, reset H2 stuff
			Tenant realDefaultTenant = realTenantManagerService.getDefaultTenant();
			realTenantPersistenceService.forgetInitialActions(realDefaultTenant);
			realTenantManagerService.resetTenant(realDefaultTenant);
			
			Assert.assertFalse(realTenantPersistenceService.rollbackAnyActiveTransactions());
		} 

		LOGGER.info("******************* Completed test: "+this.testName.getMethodName()+" *******************");
	}

	private void disablePersistence() {
		Map<Class<? extends IDomainObject>, ITypedDao<?>> nullDaos = createDaos(false); // no mock
		this.tenantPersistenceService = null;
		TenantPersistenceService.setInstance(null);		
		setStaticDaos(nullDaos);
	}

	private void setStaticDaos(Map<Class<? extends IDomainObject>, ITypedDao<?>> daos) {
		Iterable<Class<? extends IDomainObject>> domainTypes = ClassIndex.getSubclasses(IDomainObject.class);
		for (Class<? extends IDomainObject> domainType : domainTypes) {
			Field field = null;
			try {
				field = domainType.getField("DAO");
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
				LOGGER.error("unexpected SecurityException setting up test", e);
			}
			if(field != null) {
				try {
					field.set(null, daos.get(domainType));
				} catch (IllegalArgumentException e) {
					LOGGER.error("unexpected IllegalArgumentException setting up test", e);
				} catch (IllegalAccessException e) {
					LOGGER.error("unexpected IllegalAccessException setting up test", e);
				} // ObjectClass.DAO = null;
			}
		}
	}
	private Map<Class<? extends IDomainObject>, ITypedDao<?>> getStaticDaos() {
		Map<Class<? extends IDomainObject>,ITypedDao<?>> daos = new HashMap<Class<? extends IDomainObject>,ITypedDao<?>>();
		
		Iterable<Class<? extends IDomainObject>> domainTypes = ClassIndex.getSubclasses(IDomainObject.class);
		for (Class<? extends IDomainObject> domainType : domainTypes) {
			Field field = null;
			try {
				field = domainType.getField("DAO");
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
				LOGGER.error("unexpected SecurityException setting up test", e);
			}
			if(field != null) {
				try {
					daos.put(domainType, (ITypedDao<?>) field.get(null));
				} catch (IllegalArgumentException e) {
					LOGGER.error("unexpected IllegalArgumentException setting up test", e);
				} catch (IllegalAccessException e) {
					LOGGER.error("unexpected IllegalAccessException setting up test", e);
				}
			}
		}
		return daos;
	}
	private Map<Class<? extends IDomainObject>, ITypedDao<?>> createDaos(boolean createMock) {
		Map<Class<? extends IDomainObject>,ITypedDao<?>> daos = new HashMap<Class<? extends IDomainObject>,ITypedDao<?>>();
		
		Iterable<Class<? extends IDomainObject>> domainTypes = ClassIndex.getSubclasses(IDomainObject.class);
		for (Class<? extends IDomainObject> domainType : domainTypes) {
			Field field = null;
			try {
				field = domainType.getField("DAO");
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
				LOGGER.error("unexpected SecurityException setting up test", e);
			}
			if(field != null) {
				if(createMock) {
					daos.put(domainType, new MockDao<>());
				} else {
					daos.put(domainType, null);
				}
			}
		}
		return daos;
	}
	
	private void setupRealPersistenceObjects() {
		TenantManagerService.setInstance(realTenantManagerService);
		this.tenantPersistenceService = realTenantPersistenceService;
		TenantPersistenceService.setInstance(tenantPersistenceService);
		
		@SuppressWarnings("unused")
		Injector injector = Guice.createInjector(ServerMain.createDaoBindingModule());
	}

	private void setDummyPersistence() {
		Map<Class<? extends IDomainObject>,ITypedDao<?>> mockDaos = this.createDaos(true);

		tenantPersistenceService = new MockTenantPersistenceService(mockDaos);
		setStaticDaos(mockDaos);

		TenantManagerService.setInstance(new MockTenantManagerService(tenantPersistenceService.getDefaultSchema()));
		TenantPersistenceService.setInstance(tenantPersistenceService);
	}

	protected final void startSiteController() {		
		// subclasses call this method to initialize the site controller and connect to the server API

		CsClientEndpoint.setWebSocketContainer(staticWebSocketContainer);

		radioController = new RadioController(new TcpServerInterface());
		deviceManager = new CsDeviceManager(radioController,staticClientEndpoint);
		new SiteControllerMessageProcessor(deviceManager,staticClientEndpoint);
		
		if(siteconServiceManager == null) {
			List<Service> services = new ArrayList<Service>();
			services.add(FrameworkTest.staticClientConnectionManagerService);
						
			siteconServiceManager = new ServiceManager(services);
			try {
				siteconServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (site controller)",e1);
			}
		} 	
		this.getFacility(); // ensure we have created a facility
		
		staticClientConnectionManagerService.setConnected();
		this.awaitConnection();
	}

	private void startPersistence() {
		setupRealPersistenceObjects();

		if(persistenceServiceManager == null) {
			// initialize server for the first time
			List<Service> services = new ArrayList<Service>();
			ITenantManagerService tms = TenantManagerService.getMaybeRunningInstance();
			PersistenceService<ManagerSchema> mps = ManagerPersistenceService.getMaybeRunningInstance();

			if(!tms.isRunning())
				services.add(tms); 
			if(!mps.isRunning())
				services.add(mps); 
			if(!realTenantPersistenceService.isRunning())
				services.add(realTenantPersistenceService);
			
			persistenceServiceManager = new ServiceManager(services);
			try {
				persistenceServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (persistence)",e1);
			}
		}
		
		// make sure default properties are in the database
		TenantPersistenceService.getInstance().beginTransaction();
        PropertyDao.getInstance().syncPropertyDefaults();
        TenantPersistenceService.getInstance().commitTransaction();
	}
	
	private void startServer() {
        if(serverServiceManager == null) {
			// initialize server for the first time
    		List<Service> services = new ArrayList<Service>();
    		services.add(staticSessionManagerService); 
    		services.add(staticPropertyService); 
    		
    		serverServiceManager = new ServiceManager(services);
    		try {
    			serverServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
    		} catch (TimeoutException e1) {
    			throw new RuntimeException("Could not start test services (server)",e1);
    		}
        }

        // [re]set up server endpoint 
		try {
			CsServerEndPoint.setSessionManagerService(staticSessionManagerService);
			CsServerEndPoint.setMessageProcessor(staticServerMessageProcessor);
		}
		catch(Exception e) {
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
			ThreadUtils.sleep(100);
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 10000) {
				throw new RuntimeException("Failed to establish connection between embedded site controller and server");
			}
		}
		long lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
		while (lastNetworkUpdate == 0) {
			LOGGER.debug("Embedded site controller has not yet received a network update");
			ThreadUtils.sleep(100);
			lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 10000) {
				throw new RuntimeException("Failed to receive network update in allowed time");
			}
		}
		LOGGER.debug("Embedded site controller and server connected");	
	}

	protected final void initializeEphemeralServiceManager() {
		if(ephemeralServiceManager != null) {
			throw new RuntimeException("could not initialize ephemeralServiceManager (already started)");
		} else {
			// start ephemeral services. these will be stopped in @After
			// must use new service objects (services cannot be restarted)
			this.ephemeralServiceManager = new ServiceManager(generateEphemeralServices());
			LOGGER.info("starting ephemeral service manager: {}",ephemeralServiceManager.servicesByState().toString());
		
			try {
				this.ephemeralServiceManager.startAsync().awaitHealthy(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout starting ephemeralServiceManager",e);
			}
		}
	}
	
	protected List<Service> generateEphemeralServices() {
		List<Service> services = new ArrayList<Service>();
		// services.add(new Service()); e.g.
		
		if(this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			this.workService = this.generateWorkService();
			if(this.workService != null)
				services.add(this.workService);
		}
		
		if(services.isEmpty()) {
			services.add(new DummyService()); // no warning on empty service list
		}
		return services;
	}

	protected WorkService generateWorkService() {
		return new WorkService();
	}

	protected final Facility generateTestFacility() {
		String useFacilityId;
		if(this.facilitiesGenerated > 0) {
			useFacilityId = facilityId + Integer.toString(facilitiesGenerated);
		} else {
			useFacilityId = facilityId;
		}
		facilitiesGenerated++;
		
		boolean inTransaction = this.tenantPersistenceService.hasActiveTransaction(this.getDefaultTenant());
		if(!inTransaction) this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.DAO.findByDomainId(null, useFacilityId);
		if (facility == null) {
			facility = Facility.createFacility(getDefaultTenant(), useFacilityId, "", Point.getZeroPoint());
			Facility.DAO.store(facility);
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

		if(!inTransaction) this.getTenantPersistenceService().commitTransaction();
		
		return facility;
	}
	
	protected final Facility getFacility() {
		if(facility == null) {
			facility = generateTestFacility();
		}
		return facility;
	}
	public Facility createFacility() {
		if(facility == null)
			return getFacility();
		//else actually create another one
		return generateTestFacility();
	}
	protected final Tenant getDefaultTenant() {
		return TenantPersistenceService.getInstance().getDefaultSchema();
	}

}
