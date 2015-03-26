package com.codeshelf.integration;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.AisleDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.testframework.IntegrationTest;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPutWall extends ServerTest {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CheProcessPutWall.class);
	private String				CONTROLLER_1_ID	= "00001991";
	private String				CONTROLLER_2_ID	= "00001992";
	private String				CONTROLLER_3_ID	= "00001993";
	private String				CONTROLLER_4_ID	= "00001994";

	@Test
	public final void putWallOrderSetup() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker = new PickSimulator(this, cheGuid1);

		LOGGER.info("1: prove ORDER_WALL and clear works from start and finish, but not after setup or during pick");
		picker.login("Picker #1");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("1c: cannot ORDER_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("1d: pick to completion");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("F21");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 4000);

		LOGGER.info("1e: ORDER_WALL from complete state");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 4000);

		LOGGER.info("1g: Do simple actual order setup to put wall");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker.scanSomething("P15");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 4000);

	}

	protected PosManagerSimulator waitAndGetPosConController(final IntegrationTest test, final NetGuid deviceGuid) {
		Callable<PosManagerSimulator> createPosManagerSimulator = new Callable<PosManagerSimulator>() {
			@Override
			public PosManagerSimulator call() throws Exception {
				PosManagerSimulator managerSimulator = new PosManagerSimulator(test, deviceGuid);
				return (managerSimulator.getControllerLogic() != null) ? managerSimulator : null;
			}
		};

		PosManagerSimulator managerSimulator = new WaitForResult<PosManagerSimulator>(createPosManagerSimulator).waitForResult();
		return managerSimulator;
	}

	protected AisleDeviceLogic waitAndGetAisleDeviceLogic(final IntegrationTest test, final NetGuid deviceGuid) {
		Callable<AisleDeviceLogic> getAisleLogic = new Callable<AisleDeviceLogic>() {
			@Override
			public AisleDeviceLogic call() throws Exception {
				INetworkDevice deviceLogic = test.getDeviceManager().getDeviceByGuid(deviceGuid);
				return (deviceLogic instanceof AisleDeviceLogic) ? (AisleDeviceLogic) deviceLogic : null;
			}
		};

		AisleDeviceLogic aisleLogic = new WaitForResult<AisleDeviceLogic>(getAisleLogic).waitForResult();
		return aisleLogic;
	}

	/**
	 * The goal is a small version of our model put wall facility. Two fast mover areas on different paths. A slow mover area on different path.
	 * And a put wall on separate path.
	 */
	private Facility setUpFacilityWithPutWall() throws IOException {
		//Import aisles. A4 is the put wall. No LEDs
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n"
				+ "Aisle,A1,,,,,tierB1S1Side,2.85,10,X,20\n"
				+ "Bay,B1,50,,,,,,,,\n"
				+ "Tier,T1,50,4,20,0,,,,,\n"
				+ "Bay,B2,CLONE(B1)\n" //
				+ "Aisle,A2,CLONE(A1),,,,tierB1S1Side,2.85,20,X,20\n"
				+ "Aisle,A3,CLONE(A1),,,,tierB1S1Side,2.85,60,X,20\n"
				+ "Aisle,A4,,,,,tierB1S1Side,20,20,X,20\n" + "Bay,B1,50,,,,,,,,\n"//
				+ "Tier,T1,50,4,0,0,,,,,\n"//
				+ "Bay,B2,CLONE(B1)\n"; //

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(aislesCsvString), getFacility(), ediProcessTime);

		// Get the aisles
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(getFacility(), "A3");
		Aisle aisle4 = Aisle.staticGetDao().findByDomainId(getFacility(), "A4");
		Assert.assertNotNull(aisle1);

		//Make separate paths and asssign to aisle
		Path path1 = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(path1, 0, 3d, 6d, 5d, 6d);
		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path2, 0, 3d, 16d, 5d, 16d);
		persistStr = segment0.getPersistentId().toString();
		aisle2.associatePathSegment(persistStr);

		Path path3 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path3, 0, 3d, 36d, 5d, 36d);
		persistStr = segment0.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr);

		Path path4 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path4, 0, 15d, 6d, 20d, 6d);
		persistStr = segment0.getPersistentId().toString();
		aisle4.associatePathSegment(persistStr);

		//Import location aliases
		// A1 and A2 are fast mover blocks. F11-F18 and F21-F28
		// A3 is slow mover. S11-S18
		// A4 is put wall. P11-P18, but with bays having alias names WALL1 and WALL2
		String csvLocationAliases = "mappedLocationId,locationAlias\n" //
				+ "A1.B1.T1.S1,F11\n"//
				+ "A1.B1.T1.S2,F12\n"//
				+ "A1.B1.T1.S3,F13\n"// 
				+ "A1.B1.T1.S4,F14\n"//
				+ "A1.B2.T1.S1,F15\n"//
				+ "A1.B2.T1.S2,F16\n"//
				+ "A1.B2.T1.S3,F17\n"//
				+ "A1.B2.T1.S4,F18\n"//
				+ "A2.B1.T1.S1,F21\n"//
				+ "A2.B1.T1.S2,F22\n"//
				+ "A2.B1.T1.S3,F23\n"// 
				+ "A2.B1.T1.S4,F24\n"//
				+ "A2.B2.T1.S1,F25\n"//
				+ "A2.B2.T1.S2,F26\n"//
				+ "A2.B2.T1.S3,F27\n"//
				+ "A2.B2.T1.S4,F28\n"//
				+ "A3.B1.T1.S1,S11\n"//
				+ "A3.B1.T1.S2,S12\n"//
				+ "A3.B1.T1.S3,S13\n"// 
				+ "A3.B1.T1.S4,S14\n"//
				+ "A3.B2.T1.S1,S15\n"//
				+ "A3.B2.T1.S2,S16\n"//
				+ "A3.B2.T1.S3,S17\n"//
				+ "A3.B2.T1.S4,S18\n"//
				+ "A4.B1,WALL1\n"//
				+ "A4.B1.T1.S1,P11\n"//
				+ "A4.B1.T1.S2,P12\n"//
				+ "A4.B1.T1.S3,P13\n"// 
				+ "A4.B1.T1.S4,P14\n"//
				+ "A4.B2,WALL2\n"//
				+ "A4.B2.T1.S1,P15\n"//
				+ "A4.B2.T1.S2,P16\n"//
				+ "A4.B2.T1.S3,P17\n"//
				+ "A4.B2.T1.S4,P18\n";//

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter locationAliasImporter = createLocationAliasImporter();
		locationAliasImporter.importLocationAliasesFromCsvStream(new StringReader(csvLocationAliases),
			getFacility(),
			ediProcessTime2);

		CodeshelfNetwork network = getNetwork();

		//Set up a PosManager
		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid(CONTROLLER_1_ID));
		controller1.updateFromUI(CONTROLLER_1_ID, "Poscons");
		Assert.assertEquals(DeviceType.Poscons, controller1.getDeviceType());

		//Assign PosCon controller and indices to slots
		Location wall1Tier = getFacility().findSubLocationById("A4.B1.T1");
		controller1.addLocation(wall1Tier);
		wall1Tier.setLedChannel((short) 1);
		wall1Tier.getDao().store(wall1Tier);
		Location wall2Tier = getFacility().findSubLocationById("A4.B2.T1");
		controller1.addLocation(wall2Tier);
		wall2Tier.setLedChannel((short) 1);
		wall2Tier.getDao().store(wall2Tier);

		String[] slotNames = { "P11", "P12", "P13", "P14", "P15", "P16", "P17", "P18" };
		int posconIndex = 1;
		for (String slotName : slotNames) {
			Location slot = getFacility().findSubLocationById(slotName);
			slot.setPosconIndex(posconIndex);
			slot.getDao().store(slot);
			posconIndex += 1;
		}

		//Set up a LED controllers for  the fast and slow movers
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_2_ID));
		LedController controller3 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_3_ID));
		LedController controller4 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_4_ID));
		Location aisle = getFacility().findSubLocationById("A1");
		controller2.addLocation(aisle);
		aisle.setLedChannel((short) 1);
		aisle = getFacility().findSubLocationById("A2");
		controller3.addLocation(aisle);
		aisle.setLedChannel((short) 1);
		aisle = getFacility().findSubLocationById("A3");
		controller4.addLocation(aisle);
		aisle.setLedChannel((short) 1);

		// Check our lighting configuration
		Location slot = getFacility().findSubLocationById("P12");
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertFalse(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("P17");
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertFalse(slot.isLightableAisleController());

		slot = getFacility().findSubLocationById("F11");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("F23");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("S17");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());

		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.BayDistance.toString());

		return getFacility();
	}

	private void setUpOrders1(Facility inFacility) throws IOException {
		// Outbound orders. No group. Using 5 digit order number and .N detail ID. No preassigned container number.
		// With preassigned container number and preferredLocation. No inventory. All preferredLocations resolve
		// Order 12345 has two items in fast, and one is slow
		// Order 11111 has four items in other fast area, and one is slow
		// Some extra singleton orders just to get to completion states.

		String orderCsvString = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,preAssignedContainerId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345.1,12345,1123,Sku1123,1,each,F11"
				+ "\r\n,USF314,COSTCO,12345,12345.2,12345,1493,Sku1493,1,each,F12"
				+ "\r\n,USF314,COSTCO,12345,12345.3,12345,1522,Sku1522,3,each,S11"
				+ "\r\n,USF314,COSTCO,11111,11111.1,11111,1122,Sku1122,2,each,F24"
				+ "\r\n,USF314,COSTCO,11111,11111.2,11111,1522,Sku1522,1,each,S11"
				+ "\r\n,USF314,COSTCO,11111,11111.3,11111,1523,Sku1523,1,each,F21"
				+ "\r\n,USF314,COSTCO,11111,11111.4,11111,1124,Sku1124,1,each,F22"
				+ "\r\n,USF314,COSTCO,11111,11111.5,11111,1555,Sku1555,2,each,F23"
				+ "\r\n,USF314,COSTCO,11112,11112.1,11112,1555,Sku1555,2,each,F23"
				+ "\r\n,USF314,COSTCO,11113,11113.1,11113,1555,Sku1555,2,each,F23";

		importOrdersData(getFacility(), orderCsvString);
	}

}