package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;

public class CheProcessPutWallSlowPick extends CheProcessPutWallSuper{
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CheProcessPutWallSlowPick.class);
	private static final int	WAIT_TIME		= 4000;

	@Before
	public void setupPutWall() throws IOException{
		Facility facility = setUpFacilityWithPutWall();

		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		LOGGER.info("1: Just set up some orders to the put wall. Intentionally choose order with inventory location in the slow mover area.");
		picker1.loginAndSetup("Picker #1");
		String ordersAndPositions[][] = {{"11114", "L%P14"},{"11115", "L%P15"},{"11116", "L%P16"}};
		assignOrdersToPutWall(picker1, ordersAndPositions);

		LOGGER.info("2: P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");

		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		beginTransaction();
		facility = facility.reload();
		assertOrderLocation("11114", "P14", "Put Wall: WALL1 - P14");
		assertOrderLocation("11115", "P15", "Put Wall: WALL2 - P15");
		assertOrderLocation("11116", "P16", "Put Wall: WALL2 - P16");
		assertItemMaster(facility, "1514");
		assertItemMaster(facility, "1515");
		commitTransaction();
	}

	private void assignOrdersToPutWall(PickSimulator picker, String ordersAndPositions[][]){
		if (ordersAndPositions == null) {
			return;
		}
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		for (String assignment[] : ordersAndPositions){
			picker.scanSomething(assignment[0]);
			picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
			picker.scanSomething(assignment[1]);
			picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		}
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

	}

	@Test
	public final void slowMoverCombineOrders() {
		// This is for DEV-711
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		// TODO fix
		// picker2.startAndSkipReview("S11", WAIT_TIME, WAIT_TIME);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
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
	public final void slowMoverScanWallLocation() {
		// For DEV-762 allow L%WALL1 scan, instead of WALL1.  For PUT_WALL process we require the L% form.
		LOGGER.info("1: Set up identically to slowMoverCombineOrders, but use L%WALL1 and L%WALL2");
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// spell it out in detail once so we see what is going on.
		picker2.scanSomething("L%WALL1");
		picker2.waitForCheState(CheStateEnum.CONTAINER_POSITION, WAIT_TIME);
		picker2.scanSomething("P%1");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		/*picker2.setupOrderIdAsContainer("WALL1", "1"); */
		picker2.setupOrderIdAsContainer("L%WALL2", "2");

		// Although some of this seems out of scope, it explores the validation/logging in WorkService.computeWorkInstructions()
		LOGGER.info("2: Set up poscons 3 and beyond with assorted invalid location and orders. Should not affect 1 and 2 at all aside from invoke a review state.");
		LOGGER.info("2a: A bad location name. See warn in server logs during computeWorkInstructinos");
		picker2.setupOrderIdAsContainer("L%WALL99", "3");

		LOGGER.info("2b: A good location name, but not a put wall. See warn in server logs during computeWorkInstructions");
		picker2.setupOrderIdAsContainer("L%S15", "4");

		LOGGER.info("2c: A good location name, in a put wall, but a slot name. Almost certainly a user error, but no useful warning.");
		picker2.setupOrderIdAsContainer("L%P17", "5");
		// a bizarre quirk. Not tested. If we set up both P16 and WALL2, the one order belongs to both. Do we get two work instructions? Yes, incorrectly.

		LOGGER.info("2d: A bad order name. ");
		// picker2.setupOrderIdAsContainer("1119119119", "6"); cannot do this since it waits for the non error state
		picker2.scanSomething("1119119119");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECTION_INVALID, WAIT_TIME);
		picker2.scanCommand("CLEAR");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// TODO fix
		LOGGER.info("3: After start, will go to summary");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("1"), posConValue1);
		Assert.assertEquals(new Byte("1"), posConValue2);

		LOGGER.info("3b: The result should be only two work instructions, as orders 11115 and 11116 are for the same SKU on the same wall.");
		picker2.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> wiList = picker2.getAllPicksList();
		Assert.assertEquals(2, wiList.size());
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi2 = wiList.get(1);
		Assert.assertEquals("Item mismatch", "1514", wi1.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(3), wi1.getPlanQuantity());
		Assert.assertEquals("Item mismatch", "1515", wi2.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(9), wi2.getPlanQuantity());

		// Checks for the two are exactly as for the other test. But look at the logged WARNs and "Position 6 got no WIs. Causes:..."
	}

	@Test
	public final void slowMoverDontCombineOrders() throws IOException {
		PickSimulator picker1 = createPickSim(cheGuid1);
		//Assign 2 orders (one unique item each) to a wall
		String ordersAndPositions[][] = {{"11114", "L%P15"},{"11115", "L%P16"}};
		assignOrdersToPutWall(picker1, ordersAndPositions);

		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("WALL2", "1");

		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// TODO fix
		//Make sure different items do not combine
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("2"), posConValue1);
	}

	@Test
	public final void slowMoverBadWallName() {
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("Bad Wall Name", "1");

		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		//Confirm nothing on PosCon1
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValueMin1 = picker2.getLastSentPositionControllerMinQty((byte)1);
		Byte posConValueMax1 = picker2.getLastSentPositionControllerMaxQty((byte)1);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, posConValue1);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMin1);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMax1);
	}

	@Test
	public final void slowMoverBadAndGoodWallNames() {
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");
		picker2.setupOrderIdAsContainer("Bad Wall Name", "3");

		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// TODO fix
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("1"), posConValue1);
		Assert.assertEquals(new Byte("1"), posConValue2);

		//Confirm nothing on PosCon1
		Byte posConValue3 = picker2.getLastSentPositionControllerDisplayValue((byte) 3);
		Byte posConValueMin3 = picker2.getLastSentPositionControllerMinQty((byte)3);
		Byte posConValueMax3 = picker2.getLastSentPositionControllerMaxQty((byte)3);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, posConValue3);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMin3);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMax3);
	}


	@Test
	public final void slowMoverOrderAndWallMix() throws IOException {
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// TODO fix
		//Verify 2 items on PosCon1. Order "12345" has 3 items, but only 2 of them are on the path che picks.
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("2"), posConValue1);

		//Confirm nothing on PosCon2
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Byte posConValueMin2 = picker2.getLastSentPositionControllerMinQty((byte)2);
		Byte posConValueMax2 = picker2.getLastSentPositionControllerMaxQty((byte)2);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, posConValue2);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMin2);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, posConValueMax2);
	}

	@Test
	public final void slowMoverFreeOrders() throws IOException {
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("11111", "1");
		picker2.setupOrderIdAsContainer("11112", "2");

		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		// TODO fix
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("4"), posConValue1);

		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("1"), posConValue2);
	}
}
