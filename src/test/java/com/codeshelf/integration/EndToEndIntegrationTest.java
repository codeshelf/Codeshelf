package com.codeshelf.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.CodeshelfApplication;
import com.codeshelf.application.CsSiteControllerMain;
import com.codeshelf.application.SiteControllerApplication;
import com.codeshelf.application.WebApiServer;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.EdiTestABC;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.TcpServerInterface;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.persistence.TenantPersistenceService;
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
	SiteControllerApplication siteController;

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
				bind(SessionManager.class).toInstance(SessionManager.getInstance());
				// jetty websocket
				bind(MessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
			}
		});
		return injector;
	}

	@Override
	public void doBefore() throws Exception {
		super.doBefore();
		
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
		if (TenantPersistenceService.getInstance().hasAnyActiveTransactions()) {
			LOGGER.error("Active transaction found after executing unit test. Please make sure transactions are terminated on exit.");
			TenantPersistenceService.getInstance().rollbackTransaction();
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
			siteController.stopApplication(CodeshelfApplication.ShutdownCleanupReq.NONE);
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
	
	protected Facility setUpSimpleNoSlotFacility() {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle.
		//   With location alias for first baytier only, not second.
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		// Aisle 2 associated to same path segment. But with aisle controller on the other side
		// Aisle 3 will be on a separate path.
		// All tiers have controllers associated.
		// There are two CHE called CHE1 and CHE2

		/*
		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);
		*/

		/*
		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);
		*/

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,160,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, getFacility(), ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.DAO.findByDomainId(getFacility(), "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D300\r\n" //
				+ "A1.B2, D400\r\n" //
				+ "A1.B3, D500\r\n" //
				+ "A1.B1.T1, D301\r\n" //
				+ "A1.B2.T1, D302\r\n" //
				+ "A1.B3.T1, D303\r\n" //
				+ "A2.B1.T1, D401\r\n" //
				+ "A2.B2.T1, D402\r\n" //
				+ "A2.B3.T1, D403\r\n"//
				+ "A3.B1.T1, D501\r\n" //
				+ "A3.B2.T1, D502\r\n" //
				+ "A3.B3.T1, D503\r\n";//

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(reader2, getFacility(), ediProcessTime2);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("2", new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController("3", new NetGuid("0x00000013"));

		Short channel1 = 1;
		Location tier = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		// Make sure we also got the alias
		String tierName = tier.getPrimaryAliasId();
		if (!tierName.equals("D301"))
			LOGGER.error("D301 vs. A1.B1.T1 alias not set up in setUpSimpleNoSlotFacility");

		tier = getFacility().findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A1.B3.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B2.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B1.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B2.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);

		return getFacility();
	}
	
	protected void importInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}
	
	protected void importOrdersData(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = createOrderImporter();
		importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}
}
