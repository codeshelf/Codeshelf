package com.gadgetworks.codeshelf.integration;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
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
import com.gadgetworks.codeshelf.model.domain.Point;
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
	protected static String organizationId = "E2EOrg";

	protected static String facilityId = "F1";
	protected static String networkId = "DEFAULT";
	protected static String networkCredential = "0.6910096026612129";
	protected static String cheId = "CHE1";
	// protected static String cheGuid = "0x23";
	protected static NetGuid cheGuid = new NetGuid("0x23");


	JettyWebSocketServer webSocketServer;
	CsSiteControllerApplication siteController;
	CsDeviceManager deviceManager;
	
	int connectionTimeOut = 30 * 1000;

	@Override
	protected void init() { 
		Configuration.loadConfig("e2etest");
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
		Che che = network.getChe(cheId);
		if (che==null) {
			che = new Che();
			che.setParent(network);
			che.setDomainId(cheId);
			che.setDeviceGuidStr(cheGuid.getHexStringWithPrefix());
			mCheDao.store(che);
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
		boolean connected = client.isConnected();
		long start = System.currentTimeMillis();
		while (!connected) {
			LOGGER.debug("Embedded site controller and server are not connected yet");
			ThreadUtils.sleep(1000);
			connected = client.isConnected();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed>connectionTimeOut) {
				throw new RuntimeException("Failed to establish connection between embedded site controller and server");
			}
		}
		LOGGER.debug("Embedded site controller and server connected");
	}
	
	@Override 
	public void doAfter() {
		try {
			webSocketServer.stop();
		} catch (Exception e) {
			LOGGER.error("Failed to stop WebSocket server", e);
		}
		webSocketServer = null;
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
