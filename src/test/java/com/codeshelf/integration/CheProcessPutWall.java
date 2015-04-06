package com.codeshelf.integration;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPutWall extends ServerTest {
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CheProcessPutWall.class);
	private String				CONTROLLER_1_ID	= "00001881";
	private String				CONTROLLER_2_ID	= "00001882";
	private String				CONTROLLER_3_ID	= "00001883";
	private String				CONTROLLER_4_ID	= "00001884";

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
		picker.scanSomething("P12");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 4000);

		LOGGER.info("2: Demonstrate what a put wall picker object can do.");
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		this.getTenantPersistenceService().beginTransaction();
		// Facility facility = getFacility();

		/* LightService theService = ServiceFactory.getServiceInstance(LightService.class);
		theService.lightLocation(facility.getPersistentId().toString(), "P11");
		*/

		@SuppressWarnings("unused")
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1); // will return null if blank, so use the object Byte.
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void slowMoverWorkInstructions() throws IOException {
		// This is for DEV-711

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = new PickSimulator(this, cheGuid1);

		LOGGER.info("1: Just set up some orders to the put wall. Intentionally choose order with inventory location in the slow mover area.");
		picker1.login("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanSomething("11114");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P14");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P15");
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("2: P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11114", "P14");
		assertOrderLocation("11115", "P15");
		assertOrderLocation("11116", "P16");
		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker2 = new PickSimulator(this, cheGuid2);
		picker2.login("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		// picker2.startAndSkipReview("S11", 3000, 3000);
		picker2.scanCommand("START");
		LOGGER.info("3: The result should be only two work instructions, as orders 11115 and 11116 are for the same SKU on the same wall.");
		List<WorkInstruction> theWiList = picker2.getAllPicksList();
		logWiList(theWiList);
		// DEV-711 ComputeWorkInstructions will achieve this.

	}

	@Test
	public final void putWallFlowState() throws IOException {
		// This is for DEV-712, just doing the Che state transitions

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker = new PickSimulator(this, cheGuid1);

		LOGGER.info("1: prove PUT_WALL and clear works from start and finish, but not after setup or during pick");
		picker.login("Picker #1");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker.scanSomething("Sku1514");
		picker.waitForCheState(CheStateEnum.DO_PUT, 4000); // getting work, then DO_PUT DEV-713 will do this right.
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);

		LOGGER.info("1c: cannot PUT_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("PUT_WALL");
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

		LOGGER.info("1e: PUT_WALL from complete state");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 4000);

	}

	@Test
	public final void putWallPut() throws IOException {
		// This is for DEV-712, 713

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = new PickSimulator(this, cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		picker1.login("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanSomething("11114");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P14");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P15");
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, 4000);
		picker1.scanSomething("P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, 4000);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		
		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11114", "P14");
		assertOrderLocation("11115", "P15");
		assertOrderLocation("11116", "P16");
		this.getTenantPersistenceService().commitTransaction();
		

		LOGGER.info("2: As if the slow movers came out of system, just scan those SKUs to place into put wall");

		picker1.scanCommand("PUT_WALL");
		// TODO
		// Work flow wrong here. Should need to scan the container as another state-step. Otherwise, scan "Sku1514" may lead to work instructions in multiple walls.
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker1.scanSomething("Sku1514");
		picker1.waitForCheState(CheStateEnum.DO_PUT, 4000);
		// after DEV-713 we will get a plan, display to the put wall, etc.
		// P14 is at poscon index 4. Count should be 3
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		//Assert.assertEquals((Byte) (byte) 3, displayValue);
		//Assert.assertNull(displayValue);
		//We have not yet implemented displaying needed quantities on PosCons. So, by this point in the process, they are still blinking from Order Location setup
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		// button from the put wall
		// posman.buttonPress(4, 3);

		// this should complete the plan, and return to PUT_WALL_SCAN_ITEM.  More DEV-713 work
		picker1.scanCommand("CLEAR");

		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, 4000);
		picker1.scanSomething("Sku1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, 4000);
		// after DEV-713 
		// we get two plans. For this test, handle singly. DEV-714 is about lighting two or more put wall locations at time.
		// By that time, we should have implemented something to not all button press from CHE poscon, especially if more than one WI.

		// Counts are 4 and 5
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		//Assert.assertEquals((Byte) (byte) 4, displayValue);
		//Assert.assertNull(displayValue);
		//We have not yet implemented displaying needed quantities on PosCons. So, by this point in the process, they are still blinking from Order Location setup
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
	}

	@Test
	public final void putWallButton() throws IOException {
		// This is for DEV-727. This "cheats" by not doing a putwall transaction at all. Just a simple pick from a location with poscons
		// The picker/Che guid is "00009991"
		// The posman guid is "00001881"

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();

		// just these two orders set up as picks from P area.
		String orderCsvString = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,preAssignedContainerId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,11117,11117.1,11117,1515,Sku1515,4,each,P12"
				+ "\r\n,USF314,COSTCO,11118,11118.1,11118,1515,Sku1515,5,each,P13";

		importOrdersData(getFacility(), orderCsvString);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker = new PickSimulator(this, cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		CheDeviceLogic theDevice = picker.getCheDeviceLogic();

		// A diversion. This could be in non-integration unit test.
		theDevice.testOffChePosconWorkInstructions();

		LOGGER.info("1a: set up a one-pick order");
		picker.login("Picker #1");
		picker.setupContainer("11117", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("P11");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		// A diversion. Check the last scanned location behavior
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		String lastScan = che.getLastScannedLocation();
		Assert.assertEquals("P11", lastScan);
		String pathOfLastScan = che.getActivePathUi();
		Assert.assertEquals("F1.4", pathOfLastScan); // put wall on the 4th path made in setUpFacilityWithPutWall
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1b: This should result in the poscon lighting");
		// P12 is at poscon index 2. Count should be 4
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals((Byte) (byte) 4, displayValue);
		// button from the put wall
		posman.buttonPress(2, 4);
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 3000);

		// after DEV-713 
		// we get two plans. For this test, handle singly. DEV-714 is about lighting two or more put wall locations at time.
		// By that time, we should have implemented something to not allow button press from CHE poscon, especially if more than one WI.

		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertNull(displayValue);

	}
	
	private void assertOrderLocation(String orderId, String locationId) {
		Facility facility = getFacility();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order);
		Location location = facility.findSubLocationById(locationId);
		Assert.assertNotNull(location);
		List<OrderLocation> locations = order.getOrderLocations();
		Assert.assertEquals(1, locations.size());
		OrderLocation savedOrderLocation = locations.get(0);
		Location savedLocation = savedOrderLocation.getLocation();
		Assert.assertEquals(location, savedLocation);
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

		aisle4.togglePutWallLocation();
		Assert.assertTrue(aisle4.isPutWallLocation());

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
				+ "\r\n,USF314,COSTCO,11113,11113.1,11113,1555,Sku1555,2,each,F23"
				+ "\r\n,USF314,COSTCO,11114,11114.1,11114,1514,Sku1514,3,each,S12"
				+ "\r\n,USF314,COSTCO,11115,11115.1,11115,1515,Sku1515,4,each,S13"
				+ "\r\n,USF314,COSTCO,11116,11116.1,11116,1515,Sku1515,5,each,S13";

		importOrdersData(getFacility(), orderCsvString);
	}

}