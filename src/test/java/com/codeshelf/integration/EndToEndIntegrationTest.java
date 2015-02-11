package com.codeshelf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ApplicationABC;
import com.codeshelf.application.CsSiteControllerApplication;
import com.codeshelf.application.CsSiteControllerMain;
import com.codeshelf.application.WebApiServer;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.edi.EdiTestABC;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.TcpServerInterface;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.WorkService;
import com.codeshelf.util.IConfiguration;
import com.codeshelf.util.JVMSystemConfiguration;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.SessionManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;

@Ignore
public abstract class EndToEndIntegrationTest extends EdiTestABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(EndToEndIntegrationTest.class);

	// id of the organization has to match site controller configuration
	protected static String organizationId = "TestOrg";

	protected static String facilityId = "F1";
	protected static String networkId = CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String cheId1 = "CHE1";
	@Getter
	protected static NetGuid cheGuid1 = new NetGuid("0x00009991");
	protected static String cheId2 = "CHE2";
	@Getter
	protected static NetGuid cheGuid2 = new NetGuid("0x00009992");

	@Getter
	CsSiteControllerApplication siteController;

	@Getter
	CsDeviceManager deviceManager;

	@Getter
	WebApiServer apiServer;

	@Getter
	UUID facilityPersistentId;

	@Getter
	UUID networkPersistentId;

	@Getter
	UUID che1PersistentId;

	@Getter
	UUID che2PersistentId;

	int connectionTimeOut = 30 * 1000;

	public static Injector setupWSSInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IConfiguration.class).to(JVMSystemConfiguration.class);
				bind(SessionManager.class).toInstance(SessionManager.getInstance());
				// jetty websocket
				bind(MessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
			}
		});
		return injector;
	}

	@SuppressWarnings("unused")
	@Override
	public void doBefore() {
		mWorkService = new WorkService().start();
		
		Injector websocketServerInjector = setupWSSInjector();
		try { //Ideally this would be statically initialized once before all of the integration tests
			// Burying the exception allows the normal mode for the design to raise issue,
			//  but in testing assume that it got setup once the first time this is called
			CsServerEndPoint.setSessionManager(websocketServerInjector.getInstance(SessionManager.class));
			CsServerEndPoint.setMessageProcessor(websocketServerInjector.getInstance(ServerMessageProcessor.class));
		}
		catch(RuntimeException e) {
			LOGGER.debug("CsServerEndpoint already setup (NORMAL): " + e.toString());
		}

		IConfiguration configuration = websocketServerInjector.getInstance(IConfiguration.class);
		LOGGER.debug("-------------- Creating environment before running test case");
		//The client WSS needs the self-signed certificate to be trusted
		
		this.getTenantPersistenceService().beginTransaction();
		// ensure facility, network exist in database before booting up site controller
		Facility facility = mFacilityDao.findByDomainId(null, facilityId);
		if (facility==null) {
			// create organization object
			// facility = organization.createFacility(facilityId, "Integration Test Facility", Point.getZeroPoint());
			facility=Facility.createFacility(getDefaultTenant(),facilityId,"",Point.getZeroPoint());
			mFacilityDao.store(facility);

			// facility.recomputeDdcPositions(); remove this call at v10 hibernate. DDc is not compliant with hibernate patterns.
		}
		this.facilityPersistentId=facility.getPersistentId();
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		this.networkPersistentId = network.getPersistentId();

		List<Che> ches = new ArrayList<Che>(network.getChes().values());
		Che che1 = ches.get(0);
		che1.setColor(ColorEnum.MAGENTA);
		this.che1PersistentId = che1.getPersistentId();

		Che che2 = ches.get(1);
		che2.setColor(ColorEnum.WHITE);
		this.che2PersistentId = che2.getPersistentId();

		this.getTenantPersistenceService().commitTransaction();

		apiServer = new WebApiServer();
		apiServer.start(Integer.getInteger("api.port"), null, null, false, "./");
		ThreadUtils.sleep(2000);

		// start site controller
		//Use a different IGateway implementation instead of disableRadio
		Module integrationTestModule = new CsSiteControllerMain.BaseModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(IGatewayInterface.class).to(TcpServerInterface.class);
			}
		};

		siteController = CsSiteControllerMain.createApplication(integrationTestModule);
		try {
			siteController.startApplication();
		} catch (Exception e) {
			LOGGER.error("Failed to start site controller", e);
			throw new RuntimeException("Failed to start site controller");
		}
		ThreadUtils.sleep(2000);

		// wait for site controller/server connection to be established
		deviceManager = (CsDeviceManager) siteController.getDeviceManager();
		JettyWebSocketClient client = deviceManager.getClient();
		long start = System.currentTimeMillis();
		boolean connected = client.isConnected();
		while (!connected) {
			LOGGER.debug("Embedded site controller and server are not connected yet");
			ThreadUtils.sleep(1000);
			connected = client.isConnected();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed>connectionTimeOut) {
				stop();
				throw new RuntimeException("Failed to establish connection between embedded site controller and server");
			}
		}
		long lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
		while (lastNetworkUpdate==0) {
			LOGGER.debug("Embedded site controller has not yet received a network update");
			ThreadUtils.sleep(1000);
			lastNetworkUpdate = deviceManager.getLastNetworkUpdate();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed>connectionTimeOut) {
				stop();
				throw new RuntimeException("Failed to receive network update in allowed time");
			}
		}
		
		// verify that che is in site controller's device list
		CheDeviceLogic cheDeviceLogic1 = (CheDeviceLogic) this.siteController.getDeviceManager().getDeviceByGuid(cheGuid1);
		Assert.assertNotNull("Che-1 device logic not found",cheDeviceLogic1);
		CheDeviceLogic cheDeviceLogic2 = (CheDeviceLogic) this.siteController.getDeviceManager().getDeviceByGuid(cheGuid2);
		Assert.assertNotNull("Che-2 device logic not found",cheDeviceLogic2);
		LOGGER.debug("Embedded site controller and server connected");
		LOGGER.debug("-------------- Environment created");
	}

	@Override
	public void doAfter() {
		// roll back transaction if active
		if (TenantPersistenceService.getInstance().hasAnyActiveTransaction()) {
			LOGGER.error("Active transaction found after executing unit test. Please make sure transactions are terminated on exit.");
			TenantPersistenceService.getInstance().rollbackTenantTransaction();
		}
		// tear down server and site controller
		stop();
		// reset
		siteController = null;
		super.doAfter();
	}

	private void stop() {
		LOGGER.debug("-------------- Cleaning up after running test case");
		try {
			siteController.stopApplication(ApplicationABC.ShutdownCleanupReq.NONE);
		}
		catch (Exception e) {
			LOGGER.error("Failed to stop site controller",e);
		}
		try {
			apiServer.stop();
		} catch (Exception e) {
			LOGGER.error("Failed to stop WebSocket server", e);
		}
		
		LOGGER.debug("-------------- Clean up completed");
	}
	
	Facility getFacility() {
		return Facility.DAO.findByPersistentId(this.facilityPersistentId);
	}
	
	CodeshelfNetwork getNetwork() {
		return CodeshelfNetwork.DAO.findByPersistentId(this.networkPersistentId);
	}
}