package com.gadgetworks.codeshelf.integration;

import java.util.UUID;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.CsSiteControllerApplication;
import com.gadgetworks.codeshelf.application.CsSiteControllerMain;
import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.util.JVMSystemConfiguration;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.TcpServerInterface;
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
	protected static String cheId1 = "CHE-E2E-1";
	@Getter
	protected static NetGuid cheGuid1 = new NetGuid("0x23");
	protected static String cheId2 = "CHE-E2E-2";
	@Getter
	protected static NetGuid cheGuid2 = new NetGuid("0x24");

	@Getter
	JettyWebSocketServer webSocketServer;

	@Getter
	CsSiteControllerApplication siteController;

	@Getter
	CsDeviceManager deviceManager;

	@Getter
	Organization organization;

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
		System.setProperty("javax.net.ssl.keyStore", configuration.getString("keystore.path"));
		System.setProperty("javax.net.ssl.keyStorePassword", configuration.getString("keystore.store.password"));
		System.setProperty("javax.net.ssl.trustStore", configuration.getString("keystore.path"));
		System.setProperty("javax.net.ssl.trustStorePassword", configuration.getString("keystore.store.password"));
		
		this.getPersistenceService().beginTenantTransaction();
		// ensure facility, organization, network exist in database before booting up site controller
		this.organization = mOrganizationDao.findByDomainId(null, organizationId);
		if (organization==null) {
			// create organization object
			organization = new Organization();
			organization.setDomainId(organizationId);
			mOrganizationDao.store(organization);
		}
		Facility facility = mFacilityDao.findByDomainId(organization, facilityId);
		if (facility==null) {
			// create organization object
			// facility = organization.createFacility(facilityId, "Integration Test Facility", Point.getZeroPoint());
			facility=organization.createFacility(facilityId,"",0.0,0.0);
			mFacilityDao.store(facility);
			facility.createDefaultContainerKind();
			facility.recomputeDdcPositions();
		}
		this.facilityPersistentId=facility.getPersistentId();
		
		CodeshelfNetwork network = facility.getNetwork(networkId);
		if (network==null) {
			network = facility.createNetwork(networkId);
			facility.addNetwork(network);
			mCodeshelfNetworkDao.store(network);
		}
		this.networkPersistentId = network.getPersistentId();

		User scUser = network.createDefaultSiteControllerUser();
		Che che1 = network.getChe(cheId1);
		if (che1==null) {
			che1=network.createChe(cheId1, cheGuid1);
		}
		this.che1PersistentId = che1.getPersistentId();

		Che che2 = network.getChe(cheId2);
		if (che2==null) {
			che2=network.createChe(cheId2, cheGuid2);
		}
		this.che2PersistentId = che2.getPersistentId();

		this.getPersistenceService().endTenantTransaction();
		
		// start web socket server
		webSocketServer = websocketServerInjector.getInstance(JettyWebSocketServer.class);
		try {
			webSocketServer.start();
		} catch (Exception e) {
			LOGGER.error("Failed to start WebSocket server", e);
			throw new RuntimeException("Failed to start WebSocket server");
		}
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
		stop();
		webSocketServer = null;
		siteController = null;
		System.clearProperty("javax.net.ssl.keyStore");
		System.clearProperty("javax.net.ssl.keyStorePassword");
		System.clearProperty("javax.net.ssl.trustStore");
		System.clearProperty("javax.net.ssl.trustStorePassword");
	}

	private void stop() {
		LOGGER.debug("-------------- Cleaning up after running test case");
		try {
			siteController.stopApplication();
		}
		catch (Exception e) {
			LOGGER.error("Failed to stop site controller",e);
		}
		try {
			webSocketServer.stop();
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
