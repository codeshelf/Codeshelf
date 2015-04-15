package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.LightService;
import com.codeshelf.util.ThreadUtils;

public class CheProcessPutWall extends CheProcessPutWallSuper {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPutWall.class);
	private static final int	WAIT_TIME	= 4000;

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
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1c: cannot ORDER_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1d: pick to completion");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("F21");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

		LOGGER.info("1e: ORDER_WALL from complete state");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

		LOGGER.info("1g: Do simple actual order setup to put wall");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%P12");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

		LOGGER.info("2: Demonstrate what a put wall picker object can do.");
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1); // will return null if blank, so use the object Byte.
		Assert.assertNull(displayValue); // This slot is not lit.

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = getFacility();
		LightService lightService = workService.getLightService();
		lightService.lightLocation(facility.getPersistentId().toString(), "P11");
		this.getTenantPersistenceService().commitTransaction();

		// will this have intermittent failure? lightLocation() led to new message back to update the posman display. There is no state to wait for.
		ThreadUtils.sleep(2000);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertNotNull(displayValue); // Poscon is lit. Just to have it in one test case, let's sanctify all the values.
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_TRIPLE_DASH);
		Assert.assertEquals(posman.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_TRIPLE_DASH);
		// could check flashing and brightness, but seem pointless.
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
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11114");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P14");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P15");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().reload(getFacility());
		assertOrderLocation("11114", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "P16", "WALL2 - P16");
		assertItemMaster(facility, "1514");
		assertItemMaster(facility, "1515");
		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker2 = new PickSimulator(this, cheGuid2);
		picker2.login("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		// picker2.startAndSkipReview("S11", WAIT_TIME, WAIT_TIME);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("1"), posConValue1);
		Assert.assertEquals(new Byte("1"), posConValue2);

		LOGGER.info("3: The result should be only two work instructions, as orders 11115 and 11116 are for the same SKU on the same wall.");
		picker2.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		// DEV-711 ComputeWorkInstructions will achieve this.
		List<WorkInstruction> wiList = picker2.getAllPicksList();
		Assert.assertEquals(2, wiList.size());
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi2 = wiList.get(1);
		Assert.assertEquals("Item mismatch", "1514", wi1.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(3), wi1.getPlanQuantity());
		Assert.assertEquals("Item mismatch", "1515", wi2.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(9), wi2.getPlanQuantity());
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
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanSomething("L%WALL1");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanSomething("BadSku");
		picker.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1c: progress even futher before clearing. Get onto a real put job");
		// need to put an order on the wall to get there.
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11114");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%P14");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// now back to the main story
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanSomething("L%WALL1");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanSomething("gtin1514");
		picker.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// This left a work instruction that we did not clean up

		LOGGER.info("1d: cannot PUT_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1e: pick to completion");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("F21");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

		LOGGER.info("1f: PUT_WALL from complete state");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

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
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11118");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P14");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P15");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11118", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "P16", "WALL2 - P16");
		assertOrderLocation("11111", "", ""); // always good to test the NOT case. 
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("2: As if the slow movers came out of system, just scan those SKUs to place into put wall");

		picker1.scanCommand("PUT_WALL");
		// 11118 is in wall 1 with two detail lines for 1515 and 1521
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanSomething("L%WALL1");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker1.scanSomething("BadItemId");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);

		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		// P14 is at poscon index 4. Count should be 3
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals((Byte) (byte) 3, displayValue);

		// button from the put wall
		posman.buttonPress(4, 3);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		// Scan 1515 again. As the order detail is done, no work
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);

		// Item 1515 is still needed for orders 11115, 11116, 11117. As 15 and 16 are for wall2, should get no work for wall 1.
		// Let's scan the gtin/upc instead. Should get the same result.
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		// Order detail 11118.2 in wall 1 needs item 21. See that it direct scan from NO_PUT_WORK is ok.
		picker1.scanSomething("gtin1521");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		// complete this job
		posman.buttonPress(4, 3);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		// Let's scan 1515 again. Still in wall1, so will fail
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		// Worker needs to change to wall 2. Worker might scan the wall directly, rather than clearing back
		picker1.scanSomething("L%WALL2");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		// we should have two plans now
		List<WorkInstruction> wiList = picker1.getAllPicksList();
		Assert.assertEquals(2, wiList.size());
		this.logWiList(wiList);
		picker1.logCheDisplay();
		// Plan to P15 has count 4; P16 count 5.
		// Should the screen show the single work instruction count, or the combined count? Currently, the single.
		Assert.assertEquals("QTY 4", picker1.getLastCheDisplayString(3));

		// the poscon at P15 shows 4 count. P16 should still show the "--"
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		Assert.assertEquals((Byte) (byte) 4, displayValue);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 6);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		// complete this job. Should immediately show the next. No need to scan again.
		posman.buttonPress(5, 4);
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker1.logCheDisplay();
		// check the displays. The P15 slot has order feedback. P16 slot show the 5 count
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 6);
		Assert.assertEquals((Byte) (byte) 5, displayValue);

		// complete this job. That is all for this SKU in this wall. Therefore, need to scan again.
		posman.buttonPress(6, 5);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

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
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("P11");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

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
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);

		// after DEV-713 
		// we get two plans. For this test, handle singly. DEV-714 is about lighting two or more put wall locations at time.
		// By that time, we should have implemented something to not allow button press from CHE poscon, especially if more than one WI.

		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertNull(displayValue);

	}

	@Test
	public final void putWallLightingConfusion() throws IOException {
		// This explores the ambiguous situation of ORDER_WALL being set up, but still doing a normal orders pick

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = new PickSimulator(this, cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Set up some orders for the put wall before doing any picks");
		LOGGER.info(" : P12 is in WALL1. P15 and P16 are in WALL2.");
		picker1.login("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11117");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P13");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P15");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// P13 is at poscon index 3. Should show "--" as there is more work for that order.
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 3);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		LOGGER.info("2: Set up order 11117 for pick");
		picker1.scanSomething("11117");
		picker1.waitForCheState(CheStateEnum.CONTAINER_POSITION, WAIT_TIME);
		picker1.scanSomething("P%1");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> activeWis = picker1.getActivePickList();
		this.logWiList(activeWis);

		// Here is the point of this test. On this pick, what location do we tell the worker to pick from?
		// And does the put wall slot still show "--", or does it show the count and allow button press.
		// Did the LEDs light?
		Assert.assertEquals("F14", picker1.getLastCheDisplayString(1));
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 3);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		// Apparent in the log: the source LED did light. Proven just above, the put wall poscon did not light. And the source shows on CHE display.

		// If we complete this pick, do we get a recalculation for the put wall position? We should. It is "oc" now.
		WorkInstruction wi = picker1.nextActiveWi();
		int button = picker1.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker1.pick(button, quant);
		picker1.waitForCheState(CheStateEnum.PICK_COMPLETE, WAIT_TIME);
		// will this have intermittent failure? Server complete led to new message back to update the posman display. There is no state to wait for.
		ThreadUtils.sleep(2000);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_O);

	}
}