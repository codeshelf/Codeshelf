package com.gadgetworks.codeshelf.integration;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.CsSiteControllerApplication;
import com.gadgetworks.codeshelf.application.CsSiteControllerMain;
import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Ignore
public abstract class EndToEndIntegrationTest extends DomainTestABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(EndToEndIntegrationTest.class);

	// id of the organization has to match site controller configuration
	protected static String organizationId = "TestOrg";

	protected static String facilityId = "F1";
	protected static String networkId = CodeshelfNetwork.DEFAULT_NETWORK_NAME;
	protected static String cheId1 = "CHE1";
	@Getter
	protected static NetGuid cheGuid1 = new NetGuid("0x23");
	protected static String cheId2 = "CHE2";
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
	Facility facility;
	
	@Getter
	CodeshelfNetwork network;
	
	@Getter
	Che che1;
	
	@Getter
	Che che2;
	
	int connectionTimeOut = 30 * 1000;

	@Override
	protected void init() { 
	}

	public static Injector setupWSSInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IConfiguration.class).to(JVMSystemConfiguration.class);
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(SessionManager.class).toInstance(SessionManager.getInstance());
				// jetty websocket
				bind(MessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);

			}
		});
		return injector;
	}	
	
	@SuppressWarnings("unused")
	@Override
	public void doBefore() throws Exception {
		Injector websocketServerInjector = setupWSSInjector();
		Injector siteControllerInjector = CsSiteControllerMain.setupInjector();
		try { //Ideally this would be statically initialized once before all of the integration tests
			// Burying the exception allows the normal mode for the design to raise issue,
			//  but in testing assume that it got setup once the first time this is called
			CsServerEndPoint.setSessionManager(websocketServerInjector.getInstance(SessionManager.class));
			CsServerEndPoint.setMessageProcessor(websocketServerInjector.getInstance(ServerMessageProcessor.class));
		}
		catch(RuntimeException e) {
			LOGGER.debug("CsServerEndpoint already setup: " + e.toString());
		}
		IConfiguration configuration = websocketServerInjector.getInstance(IConfiguration.class);
		LOGGER.debug("-------------- Creating environment before running test case");
		//The client WSS needs the self-signed certificate to be trusted
		System.setProperty("javax.net.ssl.keyStore", configuration.getString("keystore.path"));
		System.setProperty("javax.net.ssl.keyStorePassword", configuration.getString("keystore.store.password"));
		System.setProperty("javax.net.ssl.trustStore", configuration.getString("keystore.path"));
		System.setProperty("javax.net.ssl.trustStorePassword", configuration.getString("keystore.store.password"));
		
		// ensure facility, organization, network exist in database before booting up site controller
		this.organization = mOrganizationDao.findByDomainId(null, organizationId);
		if (organization==null) {
			// create organization object
			organization = new Organization();
			organization.setDomainId(organizationId);
			mOrganizationDao.store(organization);
		}
		facility = mFacilityDao.findByDomainId(organization, facilityId);
		if (facility==null) {
			// create organization object
			// facility = organization.createFacility(facilityId, "Integration Test Facility", Point.getZeroPoint());
			facility = new Facility();
			facility.setDomainId(facilityId);
			facility.setParent(organization);
			mFacilityDao.store(facility);
			facility.createDefaultContainerKind();
			facility.recomputeDdcPositions();
		}
		network = facility.getNetwork(networkId);
		if (network==null) {
			network = new CodeshelfNetwork(facility, networkId, "The Network");
			facility.addNetwork(network);
			mCodeshelfNetworkDao.store(network);
		}
		
		User scUser = network.createDefaultSiteControllerUser();
		che1 = network.getChe(cheId1);
		if (che1==null) {
			che1 = new Che();
			che1.setParent(network);
			che1.setDomainId(cheId1);
			che1.setDeviceGuidStr(cheGuid1.getHexStringWithPrefix());
			mCheDao.store(che1);
		}
		che2 = network.getChe(cheId2);
		if (che2==null) {
			che2 = new Che();
			che2.setParent(network);
			che2.setDomainId(cheId2);
			che2.setDeviceGuidStr(cheGuid2.getHexStringWithPrefix());
			mCheDao.store(che2);
		}
		
		// start web socket server
		webSocketServer = websocketServerInjector.getInstance(JettyWebSocketServer.class);
		webSocketServer.start();
		ThreadUtils.sleep(2000);
		
		// start site controller
		siteController = siteControllerInjector.getInstance(CsSiteControllerApplication.class);
		siteController.startApplication();
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
}
