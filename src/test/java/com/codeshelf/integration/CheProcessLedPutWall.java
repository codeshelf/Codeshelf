package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;

public class CheProcessLedPutWall extends CheProcessPutWallSuper {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessLedPutWall.class);
	private static final int	WAIT_TIME	= 4000;

	private void setUpFacilityWithLedPutWall() throws IOException {
		// The super class sets up a putwall with poscons for slots.
		setUpFacilityWithPutWall();

		// Now modify it. An intentional side effect of setting a bay poscon is to clear the slot poscons.
		// The putwall bays are A4.B1 and A4.B2
		beginTransaction();
		Facility facility = getFacility();
		facility = Facility.staticGetDao().reload(facility);
		Bay bay1 = (Bay) facility.findSubLocationById("A4.B1"); // could have used the alias WALL1
		Bay bay2 = (Bay) facility.findSubLocationById("WALL2"); // use the alias instead of A4.B2
		// Find a slot in each
		Slot slotInBay1 = (Slot) facility.findSubLocationById("A4.B1.T1.S1");
		Slot slotInBay2 = (Slot) facility.findSubLocationById("A4.B2.T1.S1");
		// See that slots have a poscon assignment
		Assert.assertEquals((Integer) 1, slotInBay1.getPosconIndex());
		LedController aController = slotInBay1.getEffectiveLedController(); // actually the tier has the controller
		Assert.assertNotNull(aController);
		String controllerPersistentId = aController.getPersistentId().toString();

		Assert.assertEquals((Integer) 5, slotInBay2.getPosconIndex());
		bay1.setPosconAssignment(controllerPersistentId, "1");
		bay2.setPosconAssignment(controllerPersistentId, "2");
		// this should have cleared the slot poscons
		Assert.assertNull(slotInBay1.getPosconIndex());
		Assert.assertNull(slotInBay2.getPosconIndex());

		// Redo A4, giving it LEDs.  4 slots, use our 32 LED tubes.
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n"
				+ "Aisle,A4,,,,,tierB1S1Side,20,20,X,20\n" //
				+ "Bay,B1,50,,,,,,,,\n"//
				+ "Tier,T1,50,4,32,0,,,,,\n"//
				+ "Bay,B2,CLONE(B1)\n"; //
		importAislesData(facility, aislesCsvString);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Make sure the redo did not lose the bay controller
		LedController bController = bay1.getEffectiveLedController();
		Assert.assertNotNull(bController);
		Assert.assertEquals(aController, bController);
		Assert.assertEquals((Integer) 1, bay1.getPosconIndex());

		// The A4 tiers need the LED controller. Do it as the UX would call it
		Tier tier = (Tier) facility.findSubLocationById("A4.B1.T1");
		Assert.assertNotNull(tier);
		CodeshelfNetwork network = getNetwork();
		LedController controller5 = network.getLedController("LED5");
		Assert.assertNotNull(controller5);
		String controller5PersistentIDStr = controller5.getPersistentId().toString();
		tier.setControllerChannel(controller5PersistentIDStr, "1", "aisle");

		commitTransaction();
	}

	/**
	 * When site controller starts, the webSocket connect has a side effect to call reinitPutWallFeedback(). It is hard to replicate that, but we can
	 * call the function. This is normal poscon put wall, testing workService.reinitPutWallFeedback()
	 */
	@Test
	public final void testReinitPutwall() throws IOException {

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
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		// P14 is position 4. Show dash as an order is there. Should see in console logging about the reinit
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);

		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		beginTransaction();
		assertOrderLocation("11118", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "P16", "WALL2 - P16");
		assertOrderLocation("11111", "", ""); // always good to test the NOT case.

		LOGGER.info("1b: Call the site controller reinit function");
		List<SiteController> siteControllers = SiteController.staticGetDao().getAll();
		Assert.assertEquals((Integer) 1, (Integer) (siteControllers.size()));
		SiteController siteController = siteControllers.get(0);
		workService.reinitPutWallFeedback(siteController);
		commitTransaction();

		// P14 is position 4. Show dash as an order is there. Should see in console logging about the reinit
		// The problem with this is the value was that already before the reinit call. There is no good way to restart and reinit site controller to really test this.
		// But at least it does execute reinitPutWallFeedback.
		posman.waitForControllerDisplayValue((byte) 4, PosControllerInstr.BITENCODED_SEGMENTS_CODE, WAIT_TIME);

	}

	@Test
	public final void ledPutWallPut() throws IOException {
		// This is for DEV-712, 713

		setUpFacilityWithLedPutWall();

		setUpOrders1(getFacility());

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(CONTROLLER_1_ID));
		Assert.assertNotNull(posman);

		LOGGER.info("1: Just set up some orders for the put wall");
		LOGGER.info(" : P14 is in WALL1. P15 and P16 are in WALL2.");
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
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// Verify that orders 11114, 11115 have order locations in put wall, but not 11116
		beginTransaction();
		assertOrderLocation("11118", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "", "");

		LOGGER.info("1b: Call the site controller reinit function");
		List<SiteController> siteControllers = SiteController.staticGetDao().getAll();
		Assert.assertEquals((Integer) 1, (Integer) (siteControllers.size()));
		SiteController siteController = siteControllers.get(0);
		workService.reinitPutWallFeedback(siteController);
		// 11118 has two details in wall 1.  11115 and 11116 have one detail each in wall 2. See it logged in console
		commitTransaction();
		
		LOGGER.info("1c: Wall1 has 2 jobs, and Wall2 has 2 jobs remaining. Should see those as dim feedback");
		posman.waitForControllerDisplayValue((byte) 1, (byte) 2, WAIT_TIME);
		posman.waitForControllerDisplayValue((byte) 2, (byte) 1, WAIT_TIME);

		LOGGER.info("1d: Add in the 11116 order");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);


		// Verify that orders 11114, 11115, 1111 have order locations in put wall
		beginTransaction();
		assertOrderLocation("11118", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "P16", "WALL2 - P16");
		siteController = SiteController.staticGetDao().reload(siteController);
		workService.reinitPutWallFeedback(siteController);
		// 11118 has two details in wall 1.  11115 and 11116 have one detail each in wall 2. See it logged in console
		commitTransaction();
		
		LOGGER.info("1e: See that Wall2 now has 2 jobs remaining. Was one before we added 11116 to the wall");
		posman.waitForControllerDisplayValue((byte) 1, (byte) 2, WAIT_TIME);
		posman.waitForControllerDisplayValue((byte) 2, (byte) 2, WAIT_TIME);

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
		LOGGER.info("3a: Scanning 1515 into Wall1");
		
		// This is scan of the SKU, the ItemMaster's domainId
		picker1.scanSomething("1515");
		picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);

		// P14 is B1.T1.S4. Count should be 3
		// This is the essence of it.  P14 should have lit LEDs. And the poscon in bay 1 (index 1) should have count 3
		/*
				Byte displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1);
				Assert.assertEquals((Byte) (byte) 3, displayValue);

				LOGGER.info("3b: Complete 1515 into P14 via put wall button press");
				posman.buttonPress(1, 3);
				picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);

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

				LOGGER.info("4c: The poscon in bay 2 for P15 shows 4 count. The bay 1 poscon should still show the '--'");
				displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 2);
				Assert.assertEquals(toByte(4), displayValue);
				displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 1);
				Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, displayValue);

				LOGGER.info("4d: Complete this job. Should immediately show the next. No need to scan again.");
				posman.buttonPress(2, 4);
				picker1.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
				picker1.logCheDisplay();

				LOGGER.info("4e: Complete this job. That is all for this SKU in this wall. Therefore, need to scan again.");
				posman.buttonPress(2, 5);
				picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
			*/
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
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		// Verify that orders 11118, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		assertOrderLocation("11118", "P14", "WALL1 - P14");
		assertOrderLocation("11115", "P15", "WALL2 - P15");
		assertOrderLocation("11116", "P16", "WALL2 - P16");
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
		Assert.assertEquals(toByte(21), flashValue); // our flashing value

		// button from the put wall. Let's see how this affected the poscon
		posman.buttonPress(4, 1);
		picker1.waitForCheState(CheStateEnum.SHORT_PUT_CONFIRM, WAIT_TIME);
		picker1.logCheDisplay();
		displayValue = posman.getLastSentPositionControllerDisplayValue((byte) 4);
		flashValue = posman.getLastSentPositionControllerDisplayFreq((byte) 4);
		minValue = posman.getLastSentPositionControllerMinQty((byte) 4);
		Assert.assertEquals(toByte(3), displayValue);
		Assert.assertEquals(toByte(0), minValue);
		Assert.assertEquals(toByte(21), flashValue);

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
		picker1.scanCommand("CLEAR");
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

}