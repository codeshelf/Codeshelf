package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.LightBehavior;
import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.util.ThreadUtils;

public class CheProcessPutWall extends CheProcessPutWallSuper {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPutWall.class);
	private static final int	WAIT_TIME	= 4000;

	@Test
	public final void putWallOrderSetup() throws IOException {
		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: prove ORDER_WALL and clear works from start and finish, but not after setup or during pick");
		picker.loginAndSetup("Picker #1");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1c: cannot ORDER_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1d: pick to completion");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanLocation("F21");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("1e: ORDER_WALL from complete state");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("1g: Do simple actual order setup to put wall");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%P12");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("2: Do valid order setup to put wall, but to slot that is not a put wall. Will get a WARN");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11112");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%F12");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11114");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%F12");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// Besides the warn, the successful placement removed 11112 from P12 as it moved to F12.
		// Then placing 11114 at F12 removes 11112

		LOGGER.info("3: Demonstrate what a put wall picker object can do.");
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1); // will return null if blank, so use the object Byte.
		Assert.assertNull(displayValue); // This slot is not lit.

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = getFacility();
		LightBehavior lightBehavior = new LightBehavior();
		lightBehavior.lightLocation(facility.getPersistentId().toString(), "P11");
		this.getTenantPersistenceService().commitTransaction();

		// lightLocation() led to new message back to update the posman display. There is no state to wait for.
		posman.waitForControllerDisplayValue((byte) 1, PosControllerInstr.BITENCODED_SEGMENTS_CODE, 2000);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_TRIPLE_DASH);
		Assert.assertEquals(posman.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_TRIPLE_DASH);
		// could check flashing and brightness, but seem pointless.
	}

	/**
	 * This test places orders onto a PutWall and observers the Bay's single PosCon counting up the number of OrderDetails
	 */
	@Test
	public final void putWallBayPosconTest() throws IOException {
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_5_ID));

		picker.loginAndSetup("Picker #1");

		LOGGER.info("1: Put Orders into the wall.");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		Assert.assertNull(posman.getLastSentPositionControllerDisplayValue((byte) 1));

		placeOrderOnLocation(picker, "11111", "L%F15");
		assertAsynchPosconValue(posman, 1, 5);

		placeOrderOnLocation(picker, "11112", "L%F16");
		assertAsynchPosconValue(posman, 1, 6);

		placeOrderOnLocation(picker, "11114", "L%F17");
		assertAsynchPosconValue(posman, 1, 7);

		placeOrderOnLocation(picker, "11120", "L%F18");
		assertAsynchPosconValue(posman, 1, 10);

		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: Put an item into the wall.");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanLocation("F15");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanSomething("1122");
		picker.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		//Ensure that Bay's poscon is bright and showing required number of items
		Byte brightness = posman.getLastSentPositionControllerDisplayDutyCycle((byte) 1);
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, brightness);
		assertAsynchPosconValue(posman, 1, 2);

		//Press button
		posman.buttonPress(1, 2);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		//Ensure that Bay's poscon is dim and showing remaininig details
		assertAsynchPosconValue(posman, 1, 9);
		brightness = posman.getLastSentPositionControllerDisplayDutyCycle((byte) 1);
		Assert.assertEquals(PosControllerInstr.DIM_DUTYCYCLE, brightness);

		LOGGER.info("3: Short an item.");
		picker.scanSomething("1555");
		picker.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		posman.buttonPress(1, 1); //Expected quantity - 2
		picker.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		//Ensure that Bay's poscon is dim and showing remaininig details
		assertAsynchPosconValue(posman, 1, 8);
		brightness = posman.getLastSentPositionControllerDisplayDutyCycle((byte) 1);
		Assert.assertEquals(PosControllerInstr.DIM_DUTYCYCLE, brightness);
	}

	private void placeOrderOnLocation(PickSimulator picker, String orerId, String locationId) {
		picker.scanSomething(orerId);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething(locationId);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
	}

	@Test
	public final void putWallSimultaneousPutTestPassFirstPicker() throws IOException {
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_5_ID));
		simulataneousPutTestCommon(picker1, picker2, posman, "1122", "1522");

		LOGGER.info("4: Picker2 scans YES to override Picker1's use of the required Poscon.");
		picker2.scanCommand("YES");
		picker2.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 1); //Expect quantity "1" for item "1522" on picker2
		Assert.assertEquals("S11", picker2.getLastCheDisplayString(1));
		Assert.assertEquals("1522", picker2.getLastCheDisplayString(2));
		Assert.assertEquals("QTY 1", picker2.getLastCheDisplayString(3));
		Assert.assertEquals("", picker2.getLastCheDisplayString(4));

		LOGGER.info("5: Press poscon button to pick for Picker2.");
		posman.buttonPress(1, 1);
		picker2.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 2); //Expect quantity "2" for item "1122" on picker1

		LOGGER.info("6: Press poscon button to pick for Picker1.");
		posman.buttonPress(1, 2);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 3); //Expect poscon to show "3" for the remaining 3 details in the putwall (picking started with 5 details in the single order)
	}

	@Test
	public final void putWallSimultaneousPutTestForFirstPicker() throws IOException {
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_5_ID));
		simulataneousPutTestCommon(picker1, picker2, posman, "1122", "1522");

		LOGGER.info("4: Press poscon button to pick for Picker1.");
		posman.buttonPress(1, 2);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 4); //Expect poscon to show "3" for the remaining 3 details in the putwall (picking started with 5 details in the single order)

		LOGGER.info("5: Picker2 scans YES to exit the POSCON_BUSY screen. While Picker1 already put an item, we don't auto-proapgate it to Picker2");
		picker2.scanCommand("YES");
		picker2.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 1); //Expect quantity "1" for item "1522" on picker2
		Assert.assertEquals("S11", picker2.getLastCheDisplayString(1));
		Assert.assertEquals("1522", picker2.getLastCheDisplayString(2));
		Assert.assertEquals("QTY 1", picker2.getLastCheDisplayString(3));
		Assert.assertEquals("", picker2.getLastCheDisplayString(4));
		assertAsynchPosconValue(posman, 1, 1); //Expect quantity "1" for item "1522" on picker2

		LOGGER.info("6: Press poscon button to pick for Picker2.");
		posman.buttonPress(1, 1);
		picker2.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		assertAsynchPosconValue(posman, 1, 3); //Expect poscon to show "3" for the remaining 3 details in the putwall (picking started with 5 details in the single order)
	}

	private void simulataneousPutTestCommon(PickSimulator picker1,
		PickSimulator picker2,
		PosManagerSimulator posman,
		String item1,
		String item2) throws IOException {
		picker1.loginAndSetup("Picker #1");
		picker2.loginAndSetup("Picker #2");

		LOGGER.info("1: Put Orders into the wall.");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		Assert.assertNull(posman.getLastSentPositionControllerDisplayValue((byte) 1));
		placeOrderOnLocation(picker1, "11111", "L%F15");
		assertAsynchPosconValue(posman, 1, 5);
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: Picker1 gets an instruction to put Item into the wall.");
		picker1.scanCommand("PUT_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanLocation("F15");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker1.scanSomething(item1);
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		LOGGER.info("3: Picker2 tries to put an item into the same wall.");
		picker2.scanCommand("PUT_WALL");
		picker2.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker2.scanLocation("F15");
		picker2.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker2.scanSomething(item2);
		picker2.waitForCheState(CheStateEnum.PUT_WALL_POSCON_BUSY, WAIT_TIME);

		Assert.assertEquals("Poscon for S11 busy", picker2.getLastCheDisplayString(1));
		Assert.assertEquals("By Picker #1 on CHE1", picker2.getLastCheDisplayString(2));
		Assert.assertEquals("Scan YES after they come", picker2.getLastCheDisplayString(3));
		Assert.assertEquals("or if they will never come", picker2.getLastCheDisplayString(4));
	}

	private void assertAsynchPosconValue(PosManagerSimulator posman, int posInd, int expected) {
		Byte actual = null;
		for (int i = 0; i < 9; i++) {
			actual = posman.getLastSentPositionControllerDisplayValue((byte) posInd);
			if (actual != null && expected == actual) {
				return;
			}
			ThreadUtils.sleep(150);
		}
		Assert.fail("Expected to see poscon value " + expected + ". Instead got " + actual + ".");
	}

	@Test
	public final void slowMoverWorkInstructions() throws IOException {
		// This is for DEV-711
		Facility facility = setUpFacilityWithPutWall();

		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		LOGGER.info("1: Just set up some orders to the put wall. Intentionally choose order with inventory location in the slow mover area.");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		beginTransaction();
		facility = facility.reload();
		assertOrderLocation("11114", "P14", "Put Wall: WALL1 - P14");
		assertOrderLocation("11115", "P15", "Put Wall: WALL2 - P15");
		assertOrderLocation("11116", "P16", "Put Wall: WALL2 - P16");
		assertItemMaster(facility, "1514");
		assertItemMaster(facility, "1515");
		// let's see how P16 thinks it should light.
		Location loc = facility.findSubLocationById("P16");
		Assert.assertFalse(loc.isLightableAisleController());
		Assert.assertTrue(loc.isLightablePoscon());
		commitTransaction();

		LOGGER.info("2b: pickers set up WALL1 and WALL2 and do start");
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		picker2.scanCommand("START");

		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);

		LOGGER.info("2c: Check poscon displays on picker2");
		picker2.logCheDisplay();

		// FIXME: assertions below fail
		Assert.assertEquals(toByte(1), posConValue1);
		Assert.assertEquals(toByte(1), posConValue2);

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

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: prove PUT_WALL and clear works from start and finish, but not after setup or during pick");
		picker.loginAndSetup("Picker #1");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1b: progress futher before clearing. Scan the order ID");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanSomething("L%WALL1");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// check on feedback here. Screen will show "FOR WALL1" on line 2
		Assert.assertEquals("FOR WALL1", picker.getLastCheDisplayString(2));
		picker.scanSomething("BadSku");
		picker.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		// check on feedback here. Screen will show "NO WORK FOR BadSku IN WALL1  SCAN ITEM OR CANCEL
		Assert.assertEquals("NO WORK FOR", picker.getLastCheDisplayString(1));
		Assert.assertEquals("BadSku", picker.getLastCheDisplayString(2));
		Assert.assertEquals("IN WALL1", picker.getLastCheDisplayString(3));
		Assert.assertEquals("SCAN ITEM OR CANCEL", picker.getLastCheDisplayString(4));
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1c: progress even futher before clearing. Get onto a real put job");
		// need to put an order on the wall to get there.
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11114");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%P14");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// now back to the main story
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanSomething("L%WALL1");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		// For DEV-937. Assume it is "000gtin1514 that is scanned, but for various reasons we only got gtin1514 into the data base.
		// v20 undid the unwise thing in DEV-937. We do not over match on scan that has longer gtin than database
		picker.scanSomething("000gtin1514");
		picker.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// This left a work instruction that we did not clean up

		LOGGER.info("1d: cannot PUT_WALL after one order is set");
		picker.setupContainer("11112", "4");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("1e: pick to completion");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanLocation("F21");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("1f: PUT_WALL from complete state");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

	}

	@Test
	public final void putWallPut() throws IOException {
		// This is for DEV-712, 713

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11118", "P14", "Put Wall: WALL1 - P14");
		assertOrderLocation("11115", "P15", "Put Wall: WALL2 - P15");
		assertOrderLocation("11116", "P16", "Put Wall: WALL2 - P16");
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

		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		LOGGER.info("3a: Scanning 1515 into Wall1");

		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		// P14 is at poscon index 4. Count should be 3
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals((Byte) (byte) 3, displayValue);

		LOGGER.info("3b: Complete 1515 into P14 via put wall button press");
		posman.buttonPress(4, 3);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("3c: Scan 1515 again. As the order detail is done, no work for Wall1");
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);

		// Item 1515 is still needed for orders 11115, 11116, 11117. As 15 and 16 are for wall2, should get no work for wall 1.
		LOGGER.info("3c: Scan  the GTIN for item 1515. Still no work for Wall1");
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);

		LOGGER.info("3d: Order detail 11118.2 in wall 1 needs item 21. See that it direct scan from NO_PUT_WORK is ok.");
		picker1.scanSomething("gtin1521");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		// complete this job
		posman.buttonPress(4, 3);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		// Let's scan 1515 again. Still in wall1, so will fail
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);

		LOGGER.info("4a:  Worker needs to change to wall 2. Worker might scan the wall directly, rather than clearing back");
		picker1.scanSomething("L%WALL2");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("4b:  Scan GTIN for item 1515. Yields two jobs for wall 2");
		picker1.scanSomething("gtin1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		List<WorkInstruction> wiList = picker1.getAllPicksList();
		Assert.assertEquals(2, wiList.size());
		this.logWiList(wiList);
		picker1.logCheDisplay();
		// Plan to P15 has count 4; P16 count 5. P15 sorts first.
		// Should the screen show the single work instruction count, or the combined count? From v15, both
		Assert.assertEquals("QTY 4 of 9", picker1.getLastCheDisplayString(3));

		LOGGER.info("4c: The poscon at P15 shows 4 count. P16 should still show the '--'");
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		Assert.assertEquals(toByte(4), displayValue);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 6);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		LOGGER.info("4d: Complete this job. Should immediately show the next. No need to scan again.");
		posman.buttonPress(5, 4);
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker1.logCheDisplay();

		// check the displays. The P15 slot has order feedback. P16 slot show the 5 count
		// These can take time. Hence the wait-for
		posman.waitForControllerDisplayValue((byte) 5, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);
		posman.waitForControllerDisplayValue((byte) 6, (byte) 5, WAIT_TIME);

		LOGGER.info("4e: Complete this job. That is all for this SKU in this wall. Therefore, need to scan again.");
		posman.buttonPress(6, 5);
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
	}

	@Test
	public final void putWallButton() throws IOException {
		// This is for DEV-727. This "cheats" by not doing a putwall transaction at all. Just a simple pick from a location with poscons
		// The picker/Che guid is "00009991"
		// The posman guid is "00001881"

		setUpFacilityWithPutWall();

		this.getTenantPersistenceService().beginTransaction();
		// just these two orders set up as picks from P area.
		String orderCsvString = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,preAssignedContainerId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,11117,11117.1,11117,1515,Sku1515,4,each,P12"
				+ "\r\n,USF314,COSTCO,11118,11118.1,11118,1515,Sku1515,5,each,P13";
		importOrdersData(getFacility(), orderCsvString);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		// A diversion. This could be in non-integration unit test. Only one needed. Do not clone if you clone the test.
		CheDeviceLogic theDevice = picker.getCheDeviceLogic();
		theDevice.testOffChePosconWorkInstructions();

		LOGGER.info("1a: set up a one-pick order");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("11117", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
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
		Assert.assertEquals(toByte(4), displayValue);
		// button from the put wall
		posman.buttonPress(2, 4);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// after DEV-713
		// we get two plans. For this test, handle singly. DEV-714 is about lighting two or more put wall locations at time.
		// By that time, we should have implemented something to not allow button press from CHE poscon, especially if more than one WI.

		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertNull(displayValue);

	}

	/**
	 * This test full 105cm light tube per pallet
	 */
	@Test
	public final void palletWallTest() throws IOException {
		setUpFacilityWithPalletWall();
		setUpOrders1(getFacility());

		this.startSiteController();

		beginTransaction();
		Facility theFacility = getFacility();
		theFacility.reload();
		Location slot = theFacility.findSubLocationById("P12");
		Assert.assertEquals(35, ((Short) slot.getFirstLedNumAlongPath()).intValue());
		Assert.assertEquals(63, ((Short) slot.getLastLedNumAlongPath()).intValue());

		LOGGER.info("1: send location lighting message for slot P12 with leds from 35 to 63");
		LightBehavior lightBehavior = new LightBehavior();
		lightBehavior.lightLocation(theFacility.getPersistentId().toString(), "P12");
		commitTransaction();
		// Look in the console to see what got sent.

		PickSimulator picker1 = createPickSim(cheGuid1);

		LOGGER.info("2: Set up some orders for the put wall before doing any picks");
		picker1.loginAndSetup("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11117");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P13");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P12");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

	}

	@Test
	public final void putWallLightingConfusion() throws IOException {
		// This explores the ambiguous situation of ORDER_WALL being set up, but still doing a normal orders pick

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Set up some orders for the put wall before doing any picks");
		LOGGER.info(" : P12 is in WALL1. P15 and P16 are in WALL2.");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
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
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
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
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// wait-for to avoid intermittent failure
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);
		ThreadUtils.sleep(500);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_O);

	}

	@Test
	public final void putWallShort() throws IOException {
		// This is for DEV-715

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2. We will skip the slow mover SKU pick for this.");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// Verify that orders 11118, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11118", "P14", "Put Wall: WALL1 - P14");
		assertOrderLocation("11115", "P15", "Put Wall: WALL2 - P15");
		assertOrderLocation("11116", "P16", "Put Wall: WALL2 - P16");
		assertOrderLocation("11111", "", ""); // always good to test the NOT case.
		// Let's document the poscon indices, and relative position of P15 and P16
		Facility facility = getFacility();
		Location loc = facility.findSubLocationById("P14");
		LOGGER.info("P14 has index:{}", loc.getPosconIndex());
		loc = facility.findSubLocationById("P15");
		Tier tier = loc.getParentAtLevel(Tier.class);
		LOGGER.info("P15 has index:{} and meterAlongPath:{} tier:{}",
			loc.getPosconIndex(),
			loc.getPosAlongPathui(),
			tier.getDomainId());
		loc = facility.findSubLocationById("P16");
		tier = loc.getParentAtLevel(Tier.class);
		LOGGER.info("P16 has index:{} and meterAlongPath:{} tier:{}",
			loc.getPosconIndex(),
			loc.getPosAlongPathui(),
			tier.getDomainId());
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("2: As if aslow movers came out of system, just scan the first SKUs to place into put wall");

		picker1.scanCommand("PUT_WALL");
		// 11118 is in wall 1 with two detail lines for 1515 and 1521
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanSomething("L%WALL1");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		// P14 is at poscon index 4. Count should be 3. No flashing
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Byte flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		Byte minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(3), minValue);
		Assert.assertEquals(toByte(0), flashValue);

		LOGGER.info("2b: scanning short should make the wall button flash on the number");
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		picker1.logCheDisplay();
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(FLASH_VALUE), flashValue); // our flashing value

		// button from the put wall. Let's see how this affected the poscon
		posman.buttonPress(4, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		picker1.logCheDisplay();
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(FLASH_VALUE), flashValue);

		LOGGER.info("2c: Short confirm screen is up. Scan no to see how it recovers. Poscon display is back to original job.");
		picker1.scanCommand("NO");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(3), minValue);
		Assert.assertEquals(toByte(0), flashValue);

		LOGGER.info("2d: Short same job again. And confirm the short");
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		posman.buttonPress(4, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		picker1.scanCommand("YES");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// No active job on the poscon, so it should now show the state of the order.
		// do a wait-for to avoid the interrmittent failure
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);

		LOGGER.info("3a: Scan 1515 into wall2 will give two jobs. Short the first");
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.scanCommand("PUT_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanSomething("L%WALL2");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		List<WorkInstruction> wiList = picker1.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(2, wiList.size());
		// P15 sorts first, with count 4
		picker1.logCheDisplay();
		// Logged above, P14 is index 4. P15 is index 5. P16 is index 6
		// If no active job on the poscon, so it should show the state of the order.
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 6);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		Assert.assertEquals(toByte(4), displayValue);
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		posman.buttonPress(5, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		LOGGER.info("3b: This will short ahead. See in the log. Therefore, did not go to DO_PUT state for the P15 job.");
		picker1.scanCommand("YES");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		LOGGER.info("3c: both order/slots should show needing more. Just the normal dash.");
		// Wait for here. Intermittent can happen
		posman.waitForControllerDisplayValue((byte) 6, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_DASH);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 5);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 5), PosControllerInstr.BITENCODED_LED_DASH);
	}

	@Test
	public final void putWallClearAbandon() throws IOException {
		// This test shows that CANCEL may be used in put wall states DO_PUT, SHORT_PUT, nad SHORT_PUT_CONFIRM

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2. We will skip the slow mover SKU pick for this.");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: As if aslow movers came out of system, just scan the SKU to place into put wall");

		picker1.scanCommand("PUT_WALL");
		// 11118 is in wall 1 with two detail lines for 1515 and 1521
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanSomething("L%WALL1");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		// P14 is at poscon index 4. Count should be 3. No flashing
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);

		LOGGER.info("2b: Scan CANCEL, which cancels the job. Show that the poscon display is back to the order feedback");
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		LOGGER.info("3a: scanning short should make the wall button flash on the number");
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Byte flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		Byte minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(FLASH_VALUE), flashValue); // our flashing value was 21

		LOGGER.info("3b: Scan CANCEL, which cancels the job. Show that the poscon display is back to the order feedback");
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

		LOGGER.info("4a: Short and do the button to get to confirm state");
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(FLASH_VALUE), flashValue); // our flashing value

		// button from the put wall. Let's see how this affected the poscon
		posman.buttonPress(4, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		picker1.logCheDisplay();
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(FLASH_VALUE), flashValue);

		LOGGER.info("4b: Scan CANCEL, which cancels the job. Show that the poscon display is back to the order feedback");
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);
	}

	@Test
	public final void putWallOtherConfigurations() throws IOException {
		// This is for DEV-714
		/* The point is to make sure that our various configuration possibilities, including multi-pick, do not make the put wall unusable.
		 * Housekeeping:
		 *  BAYCHANG - If a put wall is a bay, there would never be the possibility of bay change. If the put wall is an aisle, there could be that possibility. Not tested.
		 *  RPEATPOS - currently, container/order is not being populated onto put wall work instruction. We do test one SKU to several slots in the wall. This does not
		 *  yield repeat container housekeeping.
		 * PICKMULT: we have decided to not do multiple puts at once. This is tested.
		 * WORKSEQR: We have no concept of how the put wall should work if WORKSEQR is not set to BayDistance. Not tested.
		 * SCANPICK: Should not matter. All putwall puts begin with a scan. All but this one test of SCANPICK off. This test is set for UPC scan.
		 * AUTOSHRT: default is on, and we showed elsewhere that short ahead works. It is off in this test.
		 */

		setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.getTenantPersistenceService().beginTransaction();
		LOGGER.info("1: Set up with all possibly interfering configurations set.");
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.PICKMULT, Boolean.toString(true));
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.SCANPICK, "UPC");
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.AUTOSHRT, Boolean.toString(false));
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2. We will skip the slow mover SKU pick for this.");
		picker1.loginAndSetup("Picker #1");
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
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2a: We will do 1515 in wall2 as it yields two plan that might be subject to PICKMULT");
		picker1.scanCommand("PUT_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker1.scanSomething("L%WALL2");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		LOGGER.info("2b: if SCANPICK had an effect we would have had to scan again foolishly.");
		List<WorkInstruction> wiList = picker1.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(2, wiList.size());

		LOGGER.info("2c: if PICKMULT had an effect we would have two in active jobs list instead of 1.");
		// TODO Bizarre: if workSequence given for orders 115 and 116 to same sequence, we do get the 2.
		List<WorkInstruction> activeWiList = picker1.getActivePickList();
		Assert.assertEquals(1, activeWiList.size());

		LOGGER.info("3a: AUTOSHRT is off. Short the first of two jobs for this SKU, and see that the second job does not short ahead.");
		// P15 sorts first, with count 4
		picker1.logCheDisplay();
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PUT, WAIT_TIME);
		posman.buttonPress(5, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);

		LOGGER.info("3b: This will not short ahead. See in the log. Therefore, go to DO_PUT state for the P15 job for the same SKU.");
		picker1.scanCommand("YES");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		picker1.logCheDisplay();
	}

	@Test
	public final void orderWallRemoveOrder() throws IOException {
		// This is for DEV-766. Test strategy:
		// Order 11117 has a single line from F14. So set up cart for it, and start on the path say at F14 or F11.
		// Short the order.
		// Change location, or finish and restart, we will get that work again.
		// Short the order again. Finish, and place 11117 to ORDER_WALL.
		// Restart. Do not get that work again.

		Facility facility = setUpFacilityWithPutWall();

		setUpOrders1(getFacility());

		this.getTenantPersistenceService().beginTransaction();
		// Let's document the poscon index for P14
		facility = facility.reload();
		Location loc = facility.findSubLocationById("P14");
		LOGGER.info("1: P14 has index:{}", loc.getPosconIndex());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1a: set up 11117 as our one-pick order. Also 11119 just to complete one");
		// Re-START on the completed 11119 job tests some subtle things. Server does not send it back in feedback as "oc",
		// but we interpret it as "oc".
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("11117", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("11119", "6");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanLocation("F11");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> wis = picker.getAllPicksList();
		this.logWiList(wis);

		LOGGER.info("1b: Complete 11119 (2 count at poscon 6), then short the 111117 job");
		picker.buttonPress(6, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.buttonPress(4, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("2a: Try to jump to location on same path, without doing START. On same path. so ready to pick");
		picker.scanLocation("F14");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		// TODO
		LOGGER.info("3a: Restart. Get the 11117 job again. LOCATION_SELECT_REVIEW because 11119 is completed");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// see that 11119 shows as oc
		Byte poscon6Value = picker.getLastSentPositionControllerDisplayValue((byte) 6);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, poscon6Value);
		Byte maxValue = picker.getLastSentPositionControllerMaxQty((byte) 6);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_O, maxValue);
		picker.scanCommand("START"); // this could have been location scan on the same path
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("3b: Short it again");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.buttonPress(4, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// show nothing there at position 4.
		Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertNull(displayValue);

		LOGGER.info("4a: Place this order in the put wall");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanSomething("11117");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething("L%P14");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker.scanCommand("CANCEL");
		// The choice of PICK_COMPLETE or CONTAINER_SELECT after CANCEL used to depend on if there is anything in the container map
		// That is why we just completed one order first in the test. But now, it is set by a member variable.
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("4b: Check the put wall display"); // use a waitFor since there is nothing to trigger off of. Avoid intermittent failure
		// P14 is at poscon index 4.
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, 2000);

		LOGGER.info("4c: Restart. Do not get the job again. (did before DEV-766)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("5: Not recommended. Just showing. Set up again even though it is is in the put wall");
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("11117", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanLocation("F11");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		wis = picker.getAllPicksList();
		this.logWiList(wis);
		Assert.assertEquals(1, wis.size());

		LOGGER.info("5b: Put wall display still there");
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

	}

	@Test
	public final void putWallOrderReassign() throws IOException {
		// This shows if we are live updating to clear out earlier order locations

		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());

		beginTransaction();
		// Document where the poscon indices of some of the slots
		Facility facility = getFacility();
		Location loc = facility.findSubLocationById("P13");
		LOGGER.info("P13 has index:{}", loc.getPosconIndex());
		loc = facility.findSubLocationById("P14");
		LOGGER.info("P14 has index:{}", loc.getPosconIndex());
		loc = facility.findSubLocationById("P15");
		LOGGER.info("P15 has index:{}", loc.getPosconIndex());
		loc = facility.findSubLocationById("P16");
		LOGGER.info("P16 has index:{}", loc.getPosconIndex());

		commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("0: Set up three orders in the put wall");
		picker1.loginAndSetup("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.setOrderToPutWall("11117", "P13");
		picker1.setOrderToPutWall("11115", "P15");
		picker1.setOrderToPutWall("11116", "P16");
		picker1.scanCommand("CANCEL");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// show the starting state of the orders we will use
		beginTransaction();
		facility = getFacility();
		Facility.staticGetDao().reload(facility);
		assertOrderLocation("11115", "P15", "Put Wall: WALL2 - P15");
		assertOrderLocation("11116", "P16", "Put Wall: WALL2 - P16");
		assertOrderLocation("11117", "P13", "Put Wall: WALL1 - P13");
		assertOrderLocation("11119", "", "");
		commitTransaction();

		// P13 is at poscon index 3. Should show "--" as there is more work for that order.
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);
		Assert.assertEquals(posman.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);

		/* Use cases are
		 * 0) Covered in setup. New order to unoccupied slot. 
		 * 1) If  we set new order ID to the spot of the assigned one, does the old order lose its orderLocation? Should. Still light the new.
		 * 2) If we take order with a wall location, and assign it to an empty slot, does the old slot lose its light and new get it?
		 * 3) Take order with a wall location, and assign it where another exists. The prior slot light should go off.
		 * 4) Take order with wall location, and reassign to same location again. No change.
		 */

		LOGGER.info("1: Different order takes the spot of 11117 in P13");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.setOrderToPutWall("11119", "P13");
		// Will check result later. Not too much point in calling wait for byte 3 to stay the same.

		LOGGER.info("2: 11115, in P15, assigned to empty slot P14");
		posman.waitForControllerDisplayValue((byte) 5, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11115 was here
		posman.waitForControllerDisplayValue((byte) 4, null, WAIT_TIME); // show it is blank
		picker1.setOrderToPutWall("11115", "P14");
		posman.waitForControllerDisplayValue((byte) 5, null, WAIT_TIME); // blanked this one
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // moved it here

		LOGGER.info("3: 11115, in P14, assigned P13 where 11119 is");
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11115 is here
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11119 is here
		picker1.setOrderToPutWall("11115", "P13");
		posman.waitForControllerDisplayValue((byte) 4, null, WAIT_TIME); // nothing left here
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11115 now is here

		LOGGER.info("4: 11115, in P13, assigned to P13 again");
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11115  is here
		picker1.setOrderToPutWall("11115", "P13");
		// wait for no change makes no sense. Oh, well.
		posman.waitForControllerDisplayValue((byte) 3, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME); // 11115  is still here

		// check that all orders are as we expect them to be
		beginTransaction();
		facility = getFacility();
		Facility.staticGetDao().reload(facility);
		assertOrderLocation("11115", "P13", "Put Wall: WALL1 - P13");
		assertOrderLocation("11119", "", "");
		assertOrderLocation("11117", "", "");
		commitTransaction();
	}

}
