package com.codeshelf.testframework;

import java.sql.Connection;
import java.sql.SQLException;
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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.hibernate.tool.hbm2ddl.SchemaExport;
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
import com.codeshelf.application.WebApiServer;
import com.codeshelf.behavior.IPropertyBehavior;
import com.codeshelf.behavior.InventoryBehavior;
import com.codeshelf.behavior.LightBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.device.ClientConnectionManagerService;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.SiteControllerMessageProcessor;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.email.EmailService;
import com.codeshelf.email.TemplateService;
import com.codeshelf.event.EventProducer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.TcpServerInterface;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserRole;
import com.codeshelf.manager.service.DefaultRolesPermissions;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.ManagerPersistenceService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Point;
import com.codeshelf.persistence.AbstractPersistenceService;
import com.codeshelf.persistence.DatabaseUtils;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfRealm;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.ServiceUtility;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.client.CsClientEndpoint;
import com.codeshelf.ws.client.MessageCoordinator;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.server.CsServerEndPoint;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Sets;
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

	private static Logger							LOGGER; // = need to set up logging before creating the logger
	@Getter
	@Rule
	public TestName									testName					= new TestName();

	// service managers for the various test types. for complete server test all of these will be used.
	private static ServiceManager					serverServiceManager		= null;
	private static ServiceManager					persistenceServiceManager	= null;
	private static ServiceManager					siteconServiceManager		= null;

	// app server static (reused) services
	private static WebSocketManagerService			staticWebSocketManagerService;
	private static IMetricsService					staticMetricsService;
	private static IPropertyBehavior					staticPropertyService;
	private static ServerMessageProcessor			staticServerMessageProcessor;
	private static TokenSessionService				staticTokenSessionService;
	private static EmailService						staticEmailService;
	private static TemplateService					staticTemplateService;
	private static EdiExportService	            staticEdiExporterService;

	// real non-mock instances
	private static TenantPersistenceService			realTenantPersistenceService;
	private static ITenantManagerService			realTenantManagerService;
	private boolean									resetRealPersistenceDaos;

	// default IDs to use when generating facility
	protected static String							facilityId					= "F1";
	protected static String							networkId					= CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String							cheId1						= "CHE1";
	@Getter
	protected static NetGuid						cheGuid1					= new NetGuid("0x00009991");
	protected static String							cheId2						= "CHE2";
	@Getter
	protected static NetGuid						cheGuid2					= new NetGuid("0x00009992");
	protected static String							cheId3						= "CHE3";
	@Getter
	protected static NetGuid						cheGuid3					= new NetGuid("0x00009999");

	// site controller services
	private static CsClientEndpoint					staticClientEndpoint;
	private static ClientConnectionManagerService	staticClientConnectionManagerService;
	private static WebSocketContainer				staticWebSocketContainer;

	// instance services
	protected ServiceManager						ephemeralServiceManager;
	protected WorkBehavior							workService;
	protected InventoryBehavior						inventoryService;
	protected EventProducer							eventProducer				= new EventProducer();
	protected WebApiServer							apiServer;

	@Getter
	protected TenantPersistenceService				tenantPersistenceService;
	protected ITenantManagerService					tenantManagerService;

	@Getter
	protected CsDeviceManager						deviceManager;
	protected WebSocketManagerService				webSocketManagerService;
	protected IPropertyBehavior						propertyService;
	protected IMetricsService						metricsService;
	protected TokenSessionService					tokenSessionService;
	protected EmailService							emailService;
	protected TemplateService						templateService;
	protected EdiExportService					ediExporterService;

	protected IRadioController						radioController;

	// auto created facility details
	int												facilitiesGenerated; // automatic serial naming
	private Facility								facility;
	protected UUID									networkPersistentId;
	protected UUID									che1PersistentId;
	protected UUID									che2PersistentId;
	protected UUID									che3PersistentId;

	@Getter
	private String									defaultTenantId;
	@Getter
	private WebSocketConnection						mockWsConnection;

	private Integer									port;
	private Tenant									defaultMockTenant			= Mockito.mock(Tenant.class);
	private User									defaultMockUser				= Mockito.mock(User.class);

	public static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				requestStaticInjection(CsClientEndpoint.class);

				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);

				requestStaticInjection(TenantPersistenceService.class);
				bind(TenantPersistenceService.class).in(Singleton.class);

				requestStaticInjection(WebSocketManagerService.class);
				bind(WebSocketManagerService.class).in(Singleton.class);

				requestStaticInjection(TenantManagerService.class);
				bind(ITenantManagerService.class).to(TenantManagerService.class).in(Singleton.class);

				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(DummyMetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyBehavior.class).to(PropertyService.class).in(Singleton.class);

				bind(WebSocketContainer.class).toInstance(ContainerProvider.getWebSocketContainer());

				requestStaticInjection(TokenSessionService.class);
				bind(TokenSessionService.class).in(Singleton.class);

				// Shiro modules
				bind(SecurityManager.class).to(CodeshelfSecurityManager.class);
				bind(Realm.class).to(CodeshelfRealm.class);
				
				requestStaticInjection(EmailService.class);
				bind(EmailService.class).in(Singleton.class);
				
				requestStaticInjection(TemplateService.class);
				bind(TemplateService.class).in(Singleton.class);
				
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

		//staticSecurityManagerService = injector.getInstance(SecurityManager.class);
		//SecurityUtils.setSecurityManager(staticSecurityManagerService);

		realTenantPersistenceService = TenantPersistenceService.getMaybeRunningInstance();
		realTenantManagerService = TenantManagerService.getMaybeRunningInstance();

		staticMetricsService = injector.getInstance(IMetricsService.class);
		staticMetricsService.startAsync(); // always running, outside of service manager
		ServiceUtility.awaitRunningOrThrow(staticMetricsService);
		staticTokenSessionService = injector.getInstance(TokenSessionService.class);
		staticTokenSessionService.startAsync();
		ServiceUtility.awaitRunningOrThrow(staticTokenSessionService);
		staticEmailService = injector.getInstance(EmailService.class);
		staticEmailService.startAsync();
		ServiceUtility.awaitRunningOrThrow(staticEmailService);
		staticTemplateService = injector.getInstance(TemplateService.class);
		staticTemplateService.startAsync();
		ServiceUtility.awaitRunningOrThrow(staticTemplateService);

		staticEdiExporterService = injector.getInstance(EdiExportService.class);
		staticPropertyService = injector.getInstance(IPropertyBehavior.class);

		staticWebSocketManagerService = injector.getInstance(WebSocketManagerService.class);
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
		webSocketManagerService = staticWebSocketManagerService;
		WebSocketManagerService.setInstance(webSocketManagerService);
		propertyService = staticPropertyService;
		PropertyService.setInstance(propertyService);
		metricsService = staticMetricsService;
		MetricsService.setInstance(metricsService);
		tokenSessionService = staticTokenSessionService;
		TokenSessionService.setInstance(tokenSessionService);
		emailService = staticEmailService;
		EmailService.setInstance(emailService);
		templateService = staticTemplateService;
		TemplateService.setInstance(templateService);

		SecurityUtils.setSecurityManager(new CodeshelfSecurityManager(new CodeshelfRealm()));

		// remove user/subject from main threadcontext 
		// we cannot access other threads' contexts so we hope they cleaned up!
		CodeshelfSecurityManager.removeContextIfPresent();

		workService = new WorkBehavior(new LightBehavior(), staticEdiExporterService);
		radioController = null;
		deviceManager = null;
		apiServer = null;
		mockWsConnection = null;

		// reset default facility
		facilitiesGenerated = 0;
		facility = null;
		networkPersistentId = null;
		che1PersistentId = null;
		che2PersistentId = null;
		che3PersistentId = null;

		if (staticWebSocketManagerService.hasAnySessions())
			staticWebSocketManagerService.reset();

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

		createMockWsConnection();

		LOGGER.info("------------------- Running test: " + this.testName.getMethodName() + " -------------------");
	}

	@After
	public void doAfter() {
		if (this.getFrameworkType().equals(Type.COMPLETE_SERVER)) // complete server setup has more logs during teardown
			LOGGER.info("------------------- Cleanup after test: " + this.testName.getMethodName() + " -------------------");

		if (this.getFrameworkType().equals(Type.HIBERNATE) || this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			if (CodeshelfSecurityManager.getCurrentTenant() == null) {
				LOGGER.warn("Tenant context was not present when cleaning up test, using default context to search for open transactions");
				setDefaultUserAndTenant();
			}
			Assert.assertFalse(realTenantPersistenceService.rollbackAnyActiveTransactions());
		}

		if (staticClientConnectionManagerService != null) {
			staticClientConnectionManagerService.setDisconnected();
			for (int i = 0; i < 500 && staticWebSocketManagerService.hasAnySessions(); i++) {
				ThreadUtils.sleep(2);
				;
			}
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

		if (staticWebSocketManagerService.isRunning()) {
			staticWebSocketManagerService.reset();
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

		webSocketManagerService = null;
		propertyService = null;
		metricsService = null;
		tokenSessionService = null;
		emailService = null;
		templateService = null;

		if (resetRealPersistenceDaos) {
			realTenantPersistenceService.resetDaosForTest();
			resetRealPersistenceDaos = false;
		}

		LOGGER.info("******************* Completed test: " + this.testName.getMethodName() + " *******************");
	}

	public <T extends IDomainObject> void useCustomDao(Class<T> domainType, ITypedDao<T> testDao) {
		this.tenantPersistenceService.setDaoForTest(domainType, testDao);
		if (this.getFrameworkType().equals(Type.HIBERNATE) || this.getFrameworkType().equals(Type.COMPLETE_SERVER)) {
			resetRealPersistenceDaos = true;
		}
	}

	private void disablePersistence() {
		this.tenantPersistenceService = null;
		this.tenantManagerService = null;
		TenantPersistenceService.setInstance(null);
		TenantManagerService.setInstance(null);
	}

	private Map<Class<? extends IDomainObject>, ITypedDao<?>> createMockDaos() {
		Map<Class<? extends IDomainObject>, Class<? extends ITypedDao<? extends IDomainObject>>> daoClasses = DomainObjectABC.getDaoClasses();

		Map<Class<? extends IDomainObject>, ITypedDao<?>> daos = new HashMap<Class<? extends IDomainObject>, ITypedDao<?>>();
		for (Class<? extends IDomainObject> clazz : daoClasses.keySet()) {
			daos.put(clazz, new MockDao<>(clazz));
		}
		return daos;
	}

	private void setupRealPersistenceObjects() {
		this.tenantPersistenceService = realTenantPersistenceService;
		TenantPersistenceService.setInstance(tenantPersistenceService);

		this.tenantManagerService = realTenantManagerService;
		TenantManagerService.setInstance(tenantManagerService);
	}

	private void setDummyPersistence() {
		Map<Class<? extends IDomainObject>, ITypedDao<?>> mockDaos = this.createMockDaos();

		tenantPersistenceService = new MockTenantPersistenceService(mockDaos);
		TenantPersistenceService.setInstance(tenantPersistenceService);

		tenantManagerService = new MockTenantManagerService();
		TenantManagerService.setInstance(tenantManagerService);
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
			// start h2 web interface for debugging
			try {
				org.h2.tools.Server.createWebServer("-webPort", "8082").start();
			} catch (Exception e) {
				// it's probably fine
			}

			// initialize server for the first time
			List<Service> services = new ArrayList<Service>();
			AbstractPersistenceService mps = ManagerPersistenceService.getMaybeRunningInstance();

			if (!mps.isRunning())
				services.add(mps);
			if (!realTenantManagerService.isRunning())
				services.add(realTenantManagerService);
			if (!realTenantPersistenceService.isRunning())
				services.add(realTenantPersistenceService);

			persistenceServiceManager = new ServiceManager(services);
			try {
				persistenceServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (persistence)", e1);
			}

			// create default tenant schema
			// TODO: use Liquibase instead?
			Tenant tenant = getDefaultTenant();
			Connection conn;
			try {
				conn = DatabaseUtils.getConnection(tenant);
			} catch (SQLException e2) {
				LOGGER.error("Failed to get connection to default tenant schema, cannot continue.", e2);
				throw new RuntimeException(e2);
			}
			SchemaExport se = new SchemaExport(this.tenantPersistenceService.getHibernateConfiguration(), conn);
			se.create(false, true);

		} else {
			// not 1st persistence run. need to reset
			Tenant realDefaultTenant = realTenantManagerService.getInitialTenant();
			// destroy any non-default tenants we created
			List<Tenant> tenants = realTenantManagerService.getTenants();
			for (Tenant tenant : tenants) {
				if (!tenant.equals(realDefaultTenant)) {
					realTenantManagerService.deleteTenant(tenant);
				}
				realTenantPersistenceService.forgetInitialActions(tenant.getTenantIdentifier());
			}
			realTenantManagerService.resetTenant(realDefaultTenant);
			List<UserRole> roles = realTenantManagerService.getRoles(true);
			for (UserRole role : roles) {
				if (!DefaultRolesPermissions.isDefaultRole(role.getName())) {
					realTenantManagerService.deleteRole(role);
				}
			}
			List<UserPermission> permissions = realTenantManagerService.getPermissions();
			for (UserPermission perm : permissions) {
				if (!DefaultRolesPermissions.isDefaultPermission(perm.getDescriptor())) {
					realTenantManagerService.deletePermission(perm);
				}
			}
		}

		defaultTenantId = getDefaultTenant().getTenantIdentifier();
	}

	private void startServer() {
		if (serverServiceManager == null) {
			// initialize server for the first time
			List<Service> services = new ArrayList<Service>();
			services.add(staticWebSocketManagerService);
			services.add(staticPropertyService);
			services.add(staticEdiExporterService);
			serverServiceManager = new ServiceManager(services);
			try {
				serverServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
			} catch (TimeoutException e1) {
				throw new RuntimeException("Could not start test services (server)", e1);
			}
		}

		// [re]set up server endpoint 
		try {
			CsServerEndPoint.setWebSocketManagerService(staticWebSocketManagerService);
			CsServerEndPoint.setMessageProcessor(staticServerMessageProcessor);
		} catch (Exception e) {
			LOGGER.debug("Exception setting session manager / message processor: " + e.toString());
		}

		apiServer = new WebApiServer();
		apiServer.start(port, null, null);
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
				Assert.fail("timeout starting ephemeralServiceManager: " + e.getMessage());
			}
		}
	}

	protected List<Service> generateEphemeralServices() {
		List<Service> services = new ArrayList<Service>();

		if (services.isEmpty()) {
			services.add(new DummyService()); // suppress warning on empty service list
		}
		return services;
	}

	protected final Facility generateTestFacility() {
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		Tenant defaultTenant = this.getDefaultTenant();
		if (!tenant.equals(defaultTenant))
			Assert.fail("tried to call generateTestFacility out of default context");
		// maybe support this in the future?

		String useFacilityId;
		if (this.facilitiesGenerated > 0) {
			useFacilityId = facilityId + Integer.toString(facilitiesGenerated);
		} else {
			useFacilityId = facilityId;
		}
		facilitiesGenerated++;

		boolean inTransaction = this.tenantPersistenceService.hasAnyActiveTransactions();
		if (!inTransaction)
			this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.staticGetDao().findByDomainId(null, useFacilityId);
		if (facility == null) {
			facility = Facility.createFacility(useFacilityId, "", Point.getZeroPoint());
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

		// CreateFacility makes 2 CHE by default, which is fine for smaller sites. Need 3 for some tests.
		Che che3 = network.createChe(cheId3, cheGuid3);
		che3.setColor(ColorEnum.WHITE);
		this.che3PersistentId = che3.getPersistentId();

		if (!inTransaction)
			this.getTenantPersistenceService().commitTransaction();

		return facility;
	}

	// this method should be removed. state should be managed in unit test method.
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

	public UserContext setDefaultUserAndTenant() {
		UserContext user = getMockDefaultUserContext();
		CodeshelfSecurityManager.setContext(user, this.getDefaultTenant());
		return user;
	}

	public Tenant getDefaultTenant() {
		if (realTenantManagerService.isRunning()) {
			return realTenantManagerService.getInitialTenant();
		} // else 
		return defaultMockTenant;
	}

	public UserContext getMockDefaultUserContext() {
		UserContext user = defaultMockUser;
		Mockito.when(user.getPermissionStrings()).thenReturn(Sets.newHashSet("*"));
		return user;
	}
	
	// this method should be removed. state should be managed in unit test method.
	public Che getChe1() {
		return Che.staticGetDao().findByPersistentId(this.che1PersistentId);
	}

	// this method should be removed. state should be managed in unit test method.
	public Che getChe2() {
		return Che.staticGetDao().findByPersistentId(this.che2PersistentId);
	}

	private void createMockWsConnection() {
		UserContext user = setDefaultUserAndTenant();

		this.mockWsConnection = Mockito.mock(WebSocketConnection.class);
		Mockito.when(mockWsConnection.getCurrentUserContext()).thenReturn(user);
		Mockito.when(mockWsConnection.getCurrentTenant()).thenReturn(this.getDefaultTenant());
		Mockito.when(mockWsConnection.getCurrentTenantIdentifier()).thenReturn(this.getDefaultTenantId());

		// TODO: more advanced user connection setups for tests (real/mock, roles etc)                     
	}

}
