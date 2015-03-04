package com.codeshelf.testframework;

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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.Mockito;
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
import com.codeshelf.model.dao.MockDao;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DropboxService;
import com.codeshelf.model.domain.EdiDocumentLocator;
import com.codeshelf.model.domain.EdiServiceABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IronMqService;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemDdcGroup;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.UnspecifiedLocation;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.Vertex;
import com.codeshelf.model.domain.WorkArea;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.multitenancy.ITenantManager;
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
		SERVER // server services, h2 persistence, can start Site Controller services
	};
	abstract Type getFrameworkType();

	private static Logger LOGGER; // = need to set up logging before creating the logger
	@Rule public TestName testName = new TestName();
	
	protected static ServiceManager serverServiceManager = null;
	protected static ServiceManager siteconServiceManager = null;
	protected static Map<Class<? extends IDomainObject>, ITypedDao<?>> mockDaos = new HashMap<Class<? extends IDomainObject>,ITypedDao<?>>();
	
	protected static SessionManagerService staticSessionManagerService; 
	protected static IMetricsService staticMetricsService; 
	protected static IPropertyService staticPropertyService; 
	protected static ServerMessageProcessor	staticServerMessageProcessor;
	protected static MessageCoordinator staticServerMessageCoordinator;

	private static ITenantPersistenceService realTenantPersistenceService;
	
	// default IDs to use when generating facility
	protected static String		facilityId			= "F1";
	protected static String		networkId			= CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String		cheId1				= "CHE1";
	@Getter
	protected static NetGuid	cheGuid1			= new NetGuid("0x00009991");
	protected static String		cheId2				= "CHE2";
	@Getter
	protected static NetGuid	cheGuid2			= new NetGuid("0x00009992");
	
	// site controller
	private static CsClientEndpoint staticClientEndpoint;
	private static ClientConnectionManagerService	staticClientConnectionManagerService;
	private static WebSocketContainer staticWebSocketContainer;
	
	private ITenantPersistenceService	savedPersistenceServiceInstance = null; // restore after test
	private IMetricsService	savedMetricsService;

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
	private Facility facility = null;
	protected UUID	networkPersistentId = null;
	protected UUID	che1PersistentId = null;
	protected UUID	che2PersistentId = null;

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
		LOGGER.info("******************* Setting up test: "+this.testName.getMethodName()+" *******************");
		// reset all services to defaults in case changed by a test
		sessionManagerService = staticSessionManagerService;
		propertyService = staticPropertyService;
		PropertyService.setInstance(propertyService);
		metricsService = staticMetricsService;
		MetricsService.setInstance(metricsService);

		if(MetricsService.exists()) {
			this.savedMetricsService = MetricsService.getMaybeRunningInstance();
		} else {
			this.savedMetricsService = null;
		}
		
		if(TenantPersistenceService.exists()) {
			this.savedPersistenceServiceInstance = TenantPersistenceService.getMaybeRunningInstance(); // restore after
		} else {
			this.savedPersistenceServiceInstance = null;
		}
		
		if(this.getFrameworkType().equals(Type.MOCK_DAO)) {
			setDummyPersistence();
		} else if(this.getFrameworkType().equals(Type.SERVER)) {
			setRealPersistence();

			if(serverServiceManager == null) {
				// initialize server for the first time
				startServer();
			}

			// make sure default properties are in the database
			TenantPersistenceService.getInstance().beginTransaction();
	        PropertyDao.getInstance().syncPropertyDefaults();
	        TenantPersistenceService.getInstance().commitTransaction();
			
			apiServer = new WebApiServer();
			apiServer.start(port, null, null, false, "./");
		} else {
			disablePersistence();
		}
        
		if(ephemeralServicesShouldStartAutomatically())
        	initializeEphemeralServiceManager();
		LOGGER.info("------------------- Running test: "+this.testName.getMethodName()+" -------------------");
	}

	@After
	public void doAfter() {
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
		
		boolean hadActiveTransactions = false;
		if(this.getFrameworkType().equals(Type.SERVER)) {
			hadActiveTransactions = this.tenantPersistenceService.rollbackAnyActiveTransactions();

			TenantManagerService.getInstance().resetTenant(getDefaultTenant());
			
			this.tenantPersistenceService.forgetInitialActions(getDefaultTenant());
		}

		if(this.ephemeralServiceManager != null) {
			try {
				this.ephemeralServiceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout stopping ephemeralServiceManager",e);
			}
			ImmutableCollection<Service> failedServices = ephemeralServiceManager.servicesByState().get(State.FAILED);
			Assert.assertTrue(failedServices == null || failedServices.isEmpty());
		}
		
		if(staticSessionManagerService.isRunning())
			staticSessionManagerService.reset();

		sessionManagerService = null;
		propertyService = null;
		metricsService = null;
		if(TenantPersistenceService.exists()) {
			disablePersistence();
		}
		TenantPersistenceService.setInstance(this.savedPersistenceServiceInstance);
		MetricsService.setInstance(savedMetricsService);

		Assert.assertFalse(hadActiveTransactions);
		LOGGER.info("******************* Completed test: "+this.testName.getMethodName()+" *******************");
	}

	private void disablePersistence() {
		TenantPersistenceService.setInstance(null);
		
		Aisle.DAO = null;
		LedController.DAO = null;
		Bay.DAO = null;
		Che.DAO = null;
		SiteController.DAO = null;
		CodeshelfNetwork.DAO = null;
		Container.DAO = null;
		ContainerKind.DAO = null;
		ContainerUse.DAO = null;
		DropboxService.DAO = null;
		EdiServiceABC.DAO = null;
		EdiDocumentLocator.DAO = null;
		IronMqService.DAO = null;
		Facility.DAO = null;
		Item.DAO = null;
		ItemMaster.DAO = null;
		ItemDdcGroup.DAO = null;
		LocationAlias.DAO = null;
		OrderDetail.DAO = null;
		OrderHeader.DAO = null;
		OrderGroup.DAO = null;
		OrderLocation.DAO = null;
		Path.DAO = null;
		PathSegment.DAO = null;
		Slot.DAO = null;
		Tier.DAO = null;
		UnspecifiedLocation.DAO = null;
		UomMaster.DAO = null;
		Vertex.DAO = null;
		WorkArea.DAO = null;
		WorkInstruction.DAO = null;
	}

	private void setRealPersistence() {
		this.tenantPersistenceService = realTenantPersistenceService;
		TenantPersistenceService.setInstance(tenantPersistenceService);
		
		@SuppressWarnings("unused")
		Injector injector = Guice.createInjector(ServerMain.createDaoBindingModule());
	}

	private ITypedDao<? extends IDomainObject> getMockDao(Class<? extends IDomainObject> clazz) {
		@SuppressWarnings("unchecked")
		ITypedDao<? extends IDomainObject> result = (ITypedDao<? extends IDomainObject>) mockDaos.get(clazz);
		if(result == null) {
			result = new MockDao<>();
			mockDaos.put(clazz,result);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private void setDummyPersistence() {
		tenantPersistenceService = Mockito.mock(ITenantPersistenceService.class);
		TenantPersistenceService.setInstance(tenantPersistenceService);
		
		Aisle.DAO = (ITypedDao<Aisle>) getMockDao(Aisle.class);
		LedController.DAO = (ITypedDao<LedController>) getMockDao(LedController.class);
		Bay.DAO = (ITypedDao<Bay>) getMockDao(Bay.class);
		Che.DAO = (ITypedDao<Che>) getMockDao(Che.class);
		SiteController.DAO = (ITypedDao<SiteController>) getMockDao(SiteController.class);
		CodeshelfNetwork.DAO = (ITypedDao<CodeshelfNetwork>) getMockDao(CodeshelfNetwork.class);
		Container.DAO = (ITypedDao<Container>) getMockDao(Container.class);
		Container.DAO = (ITypedDao<Container>) getMockDao(Container.class);
		ContainerKind.DAO = (ITypedDao<ContainerKind>) getMockDao(ContainerKind.class);
		ContainerUse.DAO = (ITypedDao<ContainerUse>) getMockDao(ContainerUse.class);
		DropboxService.DAO = (ITypedDao<DropboxService>) getMockDao(DropboxService.class);
		EdiServiceABC.DAO = (ITypedDao<EdiServiceABC>) getMockDao(EdiServiceABC.class);
		EdiDocumentLocator.DAO = (ITypedDao<EdiDocumentLocator>) getMockDao(EdiDocumentLocator.class);
		IronMqService.DAO = (ITypedDao<IronMqService>) getMockDao(IronMqService.class);
		Facility.DAO = (ITypedDao<Facility>) getMockDao(Facility.class);
		Item.DAO = (ITypedDao<Item>) getMockDao(Item.class);
		ItemMaster.DAO = (ITypedDao<ItemMaster>) getMockDao(ItemMaster.class);
		ItemDdcGroup.DAO = (ITypedDao<ItemDdcGroup>) getMockDao(ItemDdcGroup.class);
		LocationAlias.DAO = (ITypedDao<LocationAlias>) getMockDao(LocationAlias.class);
		OrderDetail.DAO = (ITypedDao<OrderDetail>) getMockDao(OrderDetail.class);
		OrderHeader.DAO = (ITypedDao<OrderHeader>) getMockDao(OrderHeader.class);
		OrderGroup.DAO = (ITypedDao<OrderGroup>) getMockDao(OrderGroup.class);
		OrderLocation.DAO = (ITypedDao<OrderLocation>) getMockDao(OrderLocation.class);
		Path.DAO = (ITypedDao<Path>) getMockDao(Path.class);
		PathSegment.DAO = (ITypedDao<PathSegment>) getMockDao(PathSegment.class);
		Slot.DAO = (ITypedDao<Slot>) getMockDao(Slot.class);
		Tier.DAO = (ITypedDao<Tier>) getMockDao(Tier.class);
		UnspecifiedLocation.DAO = (ITypedDao<UnspecifiedLocation>) getMockDao(UnspecifiedLocation.class);
		UomMaster.DAO = (ITypedDao<UomMaster>) getMockDao(UomMaster.class);
		Vertex.DAO = (ITypedDao<Vertex>) getMockDao(Vertex.class);
		WorkArea.DAO = (ITypedDao<WorkArea>) getMockDao(WorkArea.class);
		WorkInstruction.DAO = (ITypedDao<WorkInstruction>) getMockDao(WorkInstruction.class);
	}

	protected final void startSitecon() {		
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
				throw new RuntimeException("Could not start unit test services (site controller)",e1);
			}
		} 	
		this.getFacility(); // ensure we have created a facility
		
		staticClientConnectionManagerService.setConnected();
		this.awaitConnection();
	}

	private void startServer() {
		// this is only called once - first time server is required for a test
		
		List<Service> services = new ArrayList<Service>();
		ITenantManager tms = TenantManagerService.getMaybeRunningInstance();
		PersistenceService<ManagerSchema> mps = ManagerPersistenceService.getMaybeRunningInstance();

		if(!tms.isRunning())
			services.add(tms); 
		if(!mps.isRunning())
			services.add(mps); 
		if(!realTenantPersistenceService.isRunning())
			services.add(realTenantPersistenceService);
		
		services.add(staticSessionManagerService); 
		services.add(staticPropertyService); 
		
		serverServiceManager = new ServiceManager(services);
		try {
			serverServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
		} catch (TimeoutException e1) {
			throw new RuntimeException("Could not start unit test services (server)",e1);
		}

		// set up server endpoint 
		try {
			CsServerEndPoint.setSessionManagerService(staticSessionManagerService);
			CsServerEndPoint.setMessageProcessor(staticServerMessageProcessor);
		}
		catch(Exception e) {
			LOGGER.debug("Exception setting session manager / message processor: " + e.toString());
		}
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

	protected boolean ephemeralServicesShouldStartAutomatically() {
		return true;
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
		
		if(this.getFrameworkType().equals(Type.SERVER)) {
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
		boolean inTransaction = this.tenantPersistenceService.hasActiveTransaction(this.getDefaultTenant());
		if(!inTransaction) this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.DAO.findByDomainId(null, facilityId);
		if (facility == null) {
			facility = Facility.createFacility(getDefaultTenant(), facilityId, "", Point.getZeroPoint());
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
	protected final Tenant getDefaultTenant() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

}
