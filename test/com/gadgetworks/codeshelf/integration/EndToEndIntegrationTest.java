package com.gadgetworks.codeshelf.integration;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.CsSiteControllerApplication;
import com.gadgetworks.codeshelf.application.CsSiteControllerMain;
import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.device.CsDeviceManager;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.gadgetworks.codeshelf.ws.jetty.server.MessageProcessorFactory;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class EndToEndIntegrationTest extends DomainTestABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(EndToEndIntegrationTest.class);

	// id of the organization has to match site controller configuration
	protected static String organizationId = "TestOrg";

	protected static String facilityId = "F1";
	protected static String networkId = "DEFAULT";
	protected static String networkCredential = "0.6910096026612129";
	protected static String cheId1 = "CHE1";
	protected static NetGuid cheGuid1 = new NetGuid("0x23");
	protected static String cheId2 = "CHE2";
	protected static NetGuid cheGuid2 = new NetGuid("0x24");

	JettyWebSocketServer webSocketServer;
	CsSiteControllerApplication siteController;
	CsDeviceManager deviceManager;
	
	int connectionTimeOut = 30 * 1000;

	@Override
	protected void init() { 
	}

	public static Injector setupWSSInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {				
				bind(IDaoProvider.class).to(DaoProvider.class);
				bind(MessageProcessor.class).to(ServerMessageProcessor.class);
				requestStaticInjection(MessageProcessorFactory.class);
			}
		});
		return injector;
	}	
	
	@Override
	public void doBefore() {
		// ensure facility, organization, network exist in database before booting up site controller
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);
		if (organization==null) {
			// create organization object
			organization = new Organization();
			organization.setDomainId(organizationId);
			mOrganizationDao.store(organization);
		}
		Facility fac = mFacilityDao.findByDomainId(organization, facilityId);
		if (fac==null) {
			// create organization object
			fac = new Facility();
			fac.setDomainId(facilityId);
			fac.setParent(organization);
			mFacilityDao.store(fac);
		}
		CodeshelfNetwork network = fac.getNetwork(networkId);
		if (network==null) {
			network = new CodeshelfNetwork(fac, networkId, "The Network", networkCredential);
			mCodeshelfNetworkDao.store(network);
		}
		Che che1 = network.getChe(cheId1);
		if (che1==null) {
			che1 = new Che();
			che1.setParent(network);
			che1.setDomainId(cheId1);
			che1.setDeviceGuidStr(cheGuid1.getHexStringWithPrefix());
			mCheDao.store(che1);
		}
		Che che2 = network.getChe(cheId2);
		if (che2==null) {
			che2 = new Che();
			che2.setParent(network);
			che2.setDomainId(cheId2);
			che2.setDeviceGuidStr(cheGuid2.getHexStringWithPrefix());
			mCheDao.store(che2);
		}
		
		// start web socket server
		setupWSSInjector();
		webSocketServer = new JettyWebSocketServer();
		webSocketServer.start();
		ThreadUtils.sleep(2000);
		
		// start site controller
		Injector injector = CsSiteControllerMain.setupInjector();
		siteController = injector.getInstance(CsSiteControllerApplication.class);
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
			connected = client.isConnected();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed>connectionTimeOut) {
				stop();
				throw new RuntimeException("Failed to receive network update in allowed time");
			}
		}
		LOGGER.debug("Embedded site controller and server connected");
	}
	
	@Override 
	public void doAfter() {
		stop();
		webSocketServer = null;
	}
	
	private void stop() {
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
	}
	
	protected void waitForCheState(CheDeviceLogic cheDeviceLogic, CheStateEnum state, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start<timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			if (cheDeviceLogic.getCheStateEnum()==state) {
				// expected state found - all good
				return;
			}
		}
		Assert.fail("Che state "+state+" not encountered in "+timeoutInMillis+"ms");
	}	

}
