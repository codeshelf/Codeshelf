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
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.LightService;
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
		Bay bay1 = (Bay) facility.findSubLocationById("A4.B1"); // could have used the alias WALL1
		Bay bay2 = (Bay) facility.findSubLocationById("WALL2"); // use the alias instead of A4.B2
		// Find a slot in each
		Slot slotInBay1 = (Slot) facility.findSubLocationById("A4.B1.T1.S1");
		Slot slotInBay2 = (Slot) facility.findSubLocationById("A4.B2.T1.S1");
		// See that slots have a poscon assignment
		// Assert.assertEquals((Integer) 1, slotInBay1.getPosconIndex());
		
		// importAislesData(facility, aislesCsvString);
		commitTransaction();
		
		
		
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
		propertyService.changePropertyValue(getFacility(), DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(getFacility(), DomainObjectProperty.SCANPICK, "UPC");
		propertyService.changePropertyValue(getFacility(), DomainObjectProperty.AUTOSHRT, Boolean.toString(false));
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
		picker1.scanCommand("CLEAR");
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



}
