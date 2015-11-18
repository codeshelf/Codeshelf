/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014-2015, Codeshelf, All rights reserved
 *  file CheProcessTestPick.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.device.SetupOrdersDeviceLogic;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.PropertyService;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

/**
 * This test class focuses primarily on poscon feedback issues
 *
 */
public class CheProcessTestPickFeedback extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestPickFeedback.class);

	public CheProcessTestPickFeedback() {

	}

	@Test
	public final void testCartSetupFeedback() throws IOException {
		// Test cases:
		// 1. Two good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. There is only a case pick order for position 5. Currently will give a work instruction if the case is in inventory.
		// set up data for pick scenario

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		beginTransaction();
		importInventoryData(facility, csvInventory);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 11111 has two items in stock (Item 1 and Item 2)
		// Order 22222 has 1 item in stock (Item 1) and 1 immediate short (Item 5 which is out of stock)
		// Order 44444 has an immediate short (Item 5 which is out of stock)
		// Order 55555 has a each pick for an item that only has a case (Item 6)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,9,5,6,Test Item 6,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a6,a6,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);

		propertyService.turnOffHK(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		// Start setting up cart etc
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		//Make sure we have 4 orders/containers
		Assert.assertEquals(5, containers.size());

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("a6", "6");

		//Check that container show last 2 digits of container id. But container a6 must show as "a". (Note, this does not come from the a in a6; a means "assigned"
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_A);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		commitTransaction();

		// note no transaction active in test thread here - transactions will be opened by server during simulation

		picker.scanCommand("START");

		//Check State Make sure we do not hit REVIEW
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("Case 1: 1 good pick no flashing");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);
		//Make sure other position is null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		//Make this order complete
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pick(6, 1);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		//Reset Picker
		picker.logout();
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Continue setting up containers with bad counts");
		picker.setupOrderIdAsContainer("a1111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("9", "5");
		picker.setupOrderIdAsContainer("a6", "6");

		//Quickly check assigment feedback
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_A);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), Byte.valueOf("22"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), Byte.valueOf("33"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), Byte.valueOf("44"));
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_A);

		//Pos 5 should have "09"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[9]);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[0]);

		LOGGER.info("Starting pick");

		picker.scanCommand("START");

		//Check State
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		//Check Screens
		//Case 1: 2 good picks - solid , bright
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 2);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//Case 2: 1 good pick flashing, bright due to immediate short
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.BLINK_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 5: Each pick on a case pick which is an immediate short display solid, bright 1
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 5), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 5), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid, "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		//Scan location to make sure position controller does not show counts anymore
		picker.scanLocation("D301");

		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//Make sure all position controllers are cleared - except for case 3,4,6 since they are zero and 2 since that is the first task
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid oc
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		beginTransaction();
		propertyService.restoreHKDefaults(facility);
		commitTransaction();
	}

	//DEV-603 test case
	@Test
	public final void testCartSetupFeedbackWithPreviouslyShortedWI() throws IOException {

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		beginTransaction();
		importInventoryData(facility, csvInventory);
		propertyService.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,1,1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		//SETUP
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SHORT FIRST ITEM
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(1, 1);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Make sure we have a bright blinking 1 on the poscon
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.BLINK_FREQ);

		//COMPLETE FIRST ITEM
		picker.pick(1, 1);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
	}

	//DEV-1222 test case of double scan of position like "P%6P%6"
	@Test
	public final void testDoublePositionScan() throws IOException {

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		beginTransaction();
		importInventoryData(facility, csvInventory);
		propertyService.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: simulate a double scan coming in");
		picker.loginAndSetup("Picker #1");
		picker.scanOrderId("11111");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);

		picker.scanSomething("P%1P%1");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		LOGGER.info("2: clear and continue with second order");
		picker.scanSomething("X%CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("3: check poscons"); // before DEV-1222, the bad value would be in the position map. And then the system would throw before displaying the good poscon 2.
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.BLINK_FREQ);

		picker.logout();
	}

	@Test
	public final void noInventoryCartRunFeedback() throws IOException {
		// One good result for this, so the cart has something to run. And one no inventory.
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		propertyService.turnOffHK(facility);
		commitTransaction();

		beginTransaction();
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "4");
		picker.startAndSkipReview("D301", 3000, 3000);

		// No item for 44444, therefore nothing good happened. If it autoshorts, it would show double dash. If no short made, then it shows
		// single dash. As of April 2015, doAutoShortInstructions() is hard coded to false in WorkService. It does not use AUTOSHRT parameter.
		// Therefore, we expect poscon 4 to get the single dash, instead of the double short dash.

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

	}

	@Test
	public final void testCartRunFeedback() throws IOException {
		// Test cases:
		// 1. One good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. One good plans for position 5.
		// set up data for pick scenario

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		propertyService.turnOffHK(facility);
		commitTransaction();

		// Start setting up cart etc
		beginTransaction();
		facility = facility.reload();
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		//Make sure we have 4 orders/containers
		Assert.assertEquals(4, containers.size());

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("55555", "5");
		picker.startAndSkipReview("D301", 3000, 3000);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Check Screens -- Everything should be clear except the one we are picked #1, #4 immediate short and #3 unknown order id
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Make sure position 1 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);

		// Look at the screen
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("D301", line1);
		Assert.assertEquals("1", line2);
		Assert.assertEquals("QTY 1", line3); // This may change soon. Just update this line.
		Assert.assertEquals("", line4);

		picker.pick(1, 1);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);
		//5 should stay null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		// Look at the screen
		line1 = picker.getLastCheDisplayString(1);
		line3 = picker.getLastCheDisplayString(3);
		Assert.assertEquals("D302", line1);
		// Important: this next line shows DEV-691 result. Two picks in a row from same spot, so total for that SKU is 2, not 1.
		Assert.assertEquals("QTY 2", line3); // This may change soon. Just update this line.

		/**
		 * Now we will do a short pick and cancel it and make sure we never lose feedback.
		 * Then will redo the short and confirm it and make sure we keep the feedback
		 */
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should now also be shorted ahead so display dim, solid, double dash
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//2 is now shorted so display dim, solid, double dash
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		propertyService.restoreHKDefaults(facility);
		commitTransaction();
	}

	@Test
	public void testCheSetupErrors() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		propertyService.turnOffHK(facility);
		commitTransaction();

		// Start setting up cart etc
		beginTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");

		//CASE 1: Scan 2 containers in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);
		picker.scanOrderId("44444");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 1000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CANCEL gets us out
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Reset
		picker.logout();
		picker.loginAndSetup("Picker #1");

		//CASE 2: Scan 2 positions in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.scanPosition("3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECTION_INVALID, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CANCEL gets us out
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == Byte.valueOf("22"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 3: SCAN A TAKEN POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("1");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_IN_USE, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CANCEL gets us out
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 4: SCAN START WORK AFTER CONTAINER W/ NO POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure scanning something random doesn;t change the state
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we still got an error codes
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CANCEL gets us out
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Make sure CANCEL again does nothing
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		propertyService.restoreHKDefaults(facility);

		commitTransaction();
	}

	private void assertPositionHasErrorCode(PickSimulator picker, byte position) {
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue(position) == PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayFreq(position) == PosControllerInstr.SOLID_FREQ);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayDutyCycle(position) == PosControllerInstr.MED_DUTYCYCLE);
		Assert.assertTrue(picker.getLastSentPositionControllerMaxQty(position) == PosControllerInstr.BITENCODED_LED_BLANK);
		Assert.assertTrue(picker.getLastSentPositionControllerMinQty(position) == PosControllerInstr.BITENCODED_LED_E);

	}

	@Test
	public final void basicSimulPick() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku1,Test Item 1,1,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku3,Test Item 3,1,each,LocB,2"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku2,Test Item 2,4,each,LocC,4"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku1,Test Item 1,2,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku4,Test Item 4,1,each,LocD,3"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku2,Test Item 2,5,each,LocC,4";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set WORKSEQR, turn off housekeeping, set PICKMULT");
		beginTransaction();
		facility = facility.reload();
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		propertyService.turnOffHK(facility);
		commitTransaction();
		this.startSiteController(); // after all the parameter changes

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1b: setup two orders, that will have 3 work instructions. The first two are same SKU/Location so should be done as simultaneous WI ");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "2");
		picker.setupOrderIdAsContainer("22222", "3");

		/*
		 * 6 order lines above will yield 6 work instructions
		 * Simultaneous sequence 1, Sku1 in LocA. Count 1 on poscon 1. Count 2 on poscon 2.
		 * Sequence 2 is Sku3 in LocB. Count 1 on poscon 1.
		 * Sequence 3 is Sku4 in LocD. Count 1 on poscon 3.
		 * Simultaneous sequence 4, Sku2 in LocC. Count 4 on poscon 3. Count 5 on poscon 1.
		 */

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("1c: Screen shows the location, SKU, and total to pick");
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 3", line3);
		Assert.assertEquals("", line4);

		LOGGER.info("1d: see that both poscons show their pick count: 1 and 2");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());

		// bug was here
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("2a: Complete the second poscon first");
		picker.pick(2, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("2b: see that poscons 1 count remains, and 2 is now oc. 3 still null/blank");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("2c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("2d: poscon message dropped. User is confused. Press button 2 two times more");
		// DEV-1318 case. We definitely sent out "oc" to position 2, but let's assume the CHE did not get it to the poscon, so the poscon is
		// is still showing the value 2. The user might just push that button again.
		// In v24, we did nothing at all. We should either send out the correct value for that poscon again, or do the entire CHE/poscon display again.
		picker.pick(2, 2);
		picker.waitInSameState(CheStateEnum.DO_PICK, 1500); // give a chance to see in the console what messages go out.
		picker.pick(2, 2);
		picker.waitInSameState(CheStateEnum.DO_PICK, 1500); // give a chance to see in the console what messages go out.

		LOGGER.info("2e: Some other button press comes in. Should not happen, but unit test can.");
		// This provides test coverage for the other WARN case in SetupOrdersDeviceLogic.processButtonPress()
		picker.pick(4, 1);
		picker.waitInSameState(CheStateEnum.DO_PICK, 1500); // give a chance to see in the console what messages go out.
		// cannot currently assert on anything to know new messages went out. Only see in the console.

		LOGGER.info("3a: Complete the first poscon");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("3b: Poscon 1 gets the next job, poscon 2 remains oc. 3 still null/blank");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("3c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocB", line1);
		Assert.assertEquals("Sku3", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("4a: Complete the job on first poscon");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("4b: 3 gets the next jobs, poscon 2 remains oc. 1 is blank");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);

		LOGGER.info("4c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocD", line1);
		Assert.assertEquals("Sku4", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("5a: Complete the job on first poscon. Now 1 is blank. Next job on 3");
		picker.pick(1, 5);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());

		LOGGER.info("5b: Complete the job on poscon3. Last jobs are simultaneous 1,3");
		picker.pick(3, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("6a: Poscon 1 and 3 gets the next jobs, poscon 2 remains oc.");
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		// same bug was here
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);

		LOGGER.info("6b: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocC", line1);
		Assert.assertEquals("Sku2", line2);
		Assert.assertEquals("QTY 9", line3);
	}

	@Test
	public final void simulPickShort() throws IOException {
		// Bug?  should be BLINK_FREQ
		final Byte kBLINK_FREQ = PosControllerInstr.BLINK_FREQ;
		final Byte kSOLID_FREQ = PosControllerInstr.SOLID_FREQ;

		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku1,Test Item 1,1,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku2,Test Item 2,4,each,LocC,2"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku1,Test Item 1,2,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku2,Test Item 2,3,each,LocC,2"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku1,Test Item 1,5,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku2,Test Item 2,6,each,LocC,2";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set WORKSEQR, turn off housekeeping, set PICKMULT");
		beginTransaction();
		facility = facility.reload();
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		propertyService.turnOffHK(facility);
		commitTransaction();
		this.startSiteController(); // after all the parameter changes

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1b: setup two orders, that will have 3 work instructions. The first two are same SKU/Location so should be done as simultaneous WI ");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "2");
		picker.setupOrderIdAsContainer("22222", "3");

		/*
		 * 6 order lines above will yield 6 work instructions in 2 simulpick groups of 3
		 * Simultaneous sequence 1, Sku1 in LocA. Count 1 on poscon 1. Count 2 on poscon 2. Count 5 on poscon 3.
		 * Simultaneous sequence 2, Sku2 in LocC. Count 4 on poscon 1. Count 3 on poscon 2. Count 6 on poscon 3.
		 */

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("1c: Screen shows the location, SKU, and total to pick");
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 8", line3);
		Assert.assertEquals("", line4);

		LOGGER.info("1d: see that both poscons show their pick count: 1 and 2");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());

		LOGGER.info("2a: SHORT now. This should make all 3 poscons showing solid numbers");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);

		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1));
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2));
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 1));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("2c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 8", line3);

		LOGGER.info("3a: Complete the first poscon with the full count. State should still be short pick");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);

		LOGGER.info("3b: Poscon 1 goes out. 2 and 3 still solid.");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("3c: Screen shows same location, SKU. Total counts down.");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 7", line3);

		LOGGER.info("4a: Short poscon 3. What do poscons show then? Counts there? Still flashing?");
		picker.pick(3, 3);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		// 3 went out. 2 also went out. Otherwise misleading to user.
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		LOGGER.info("4b: Say no to confirm. Should be back at DO_PICK. Flashing");
		line2 = picker.getLastCheDisplayString(2);
		Assert.assertEquals(line2, CheDeviceLogic.YES_NO_MSG);
		picker.scanCommand("NO");
		// Comes to DO_PICK. One could argue it should remain at SHORT_PICK. but behavior is consistent with single short case.
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("5a: This time short and confirm");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(3, 3);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("5b: Poscon 3 job should have shorted. Poscon 2 should have shorted ahead. We should be on to the next simultaneous jobs");
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(3, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(6, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);
		Assert.assertEquals("LocC", line1);
		Assert.assertEquals("Sku2", line2);
		Assert.assertEquals("QTY 13", line3);

		LOGGER.info("6a: Just work these off");
		picker.pick(2, 3);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		// brief diversion to see poscon states. shorted 2 should be double dashed now. 1 and 3 with active picks
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(6, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		picker.pick(1, 4);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pick(3, 6);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);

		LOGGER.info("6b: See if the shorted and shorted-ahead poscons show double dash, and the one good one is oc");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
	}

	/**
	 * The purpose is to have a relatively high number of orders set up on the cart, to examine messages sent out to the poscons.
	 * Mostly done by looking at log console, and not by asserting anything.
	 * Also review for "efficiency" of the logging: relatively few logged messages, but with all the information one would want.
	 */
	@Test
	public final void checkManyPosconsMessages() throws IOException {
	LOGGER.info("1: Set up facility. Add the export extensions");
	// somewhat cloned from FacilityAccumulatingExportTest
	Facility facility = setUpSimpleNoSlotFacility();

	beginTransaction();
	facility = facility.reload();
	propertyService.turnOffHK(facility);
	DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.WORKSEQR);
	if (theProperty != null) {
		theProperty.setValue("WorkSequence");
		PropertyDao.getInstance().store(theProperty);
	}
	commitTransaction();

	LOGGER.info("2: Load orders. No inventory, so uses locationA, etc. as the location-based pick");
	beginTransaction();
	facility = facility.reload();

	String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
			+ "\r\n1,1,1,Test Item 1,1,each,locationA,1" //
			+ "\r\n2,2,2,Test Item 2,3,each,locationB,2" //
			+ "\r\n3,3,3,Test Item 3,1,each,locationC,3" //
			+ "\r\n4,4,4,Test Item 4,1,each,locationD,4" //
			+ "\r\n5,5,5,Test Item 5,1,each,locationE,5" //
			+ "\r\n6,6,6,Test Item 6,1,each,locationF,6" //
			+ "\r\n7,7,7,Test Item 7,3,each,locationG,7" //
			+ "\r\n8,8,8,Test Item 8,1,each,locationH,8" //
			+ "\r\n9,9,9,Test Item 9,1,each,locationI,9" //
			+ "\r\n10,10,10,Test Item 10,1,each,locationJ,10" //
			+ "\r\n11,11,11,Test Item 11,1,each,locationK,11" //
			+ "\r\n12,12,12,Test Item 12,3,each,locationL,12" //
			+ "\r\n13,13,13,Test Item 13,1,each,locationM,13" //
			+ "\r\n14,14,14,Test Item 14,1,each,locationN,14" //
			+ "\r\n15,15,15,Test Item 15,1,each,locationO,15" //
			+ "\r\n16,16,16,Test Item 16,1,each,locationP,16" //
			+ "\r\n17,17,17,Test Item 17,3,each,locationQ,17" //
			+ "\r\n18,18,18,Test Item 18,1,each,locationR,18" //
			+ "\r\n19,19,19,Test Item 19,1,each,locationS,19" //
			+ "\r\n20,20,20,Test Item 20,1,each,locationT,20"; //

	importOrdersData(facility, csvOrders);
	commitTransaction();
	
	startSiteController();
	PickSimulator picker = createPickSim(cheGuid1);

	LOGGER.info("2: load 2 orders on the CHE");
	picker.loginAndSetup("Picker #1");
	picker.setupOrderIdAsContainer("1", "1");
	picker.setupOrderIdAsContainer("2", "2");
	picker.setupOrderIdAsContainer("3", "3");
	picker.setupOrderIdAsContainer("4", "4");
	picker.setupOrderIdAsContainer("5", "5");
	picker.setupOrderIdAsContainer("6", "6");
	picker.setupOrderIdAsContainer("7", "7");
	picker.setupOrderIdAsContainer("8", "8");
	picker.setupOrderIdAsContainer("9", "9");
	picker.setupOrderIdAsContainer("10", "10");
	picker.setupOrderIdAsContainer("11", "11");
	picker.setupOrderIdAsContainer("12", "12");
	picker.setupOrderIdAsContainer("13", "13");
	picker.setupOrderIdAsContainer("14", "14");
	picker.setupOrderIdAsContainer("15", "15");
	picker.setupOrderIdAsContainer("16", "16");
	picker.setupOrderIdAsContainer("17", "17");
	picker.setupOrderIdAsContainer("18", "18");
	LOGGER.info("2b: finished 18, add 19");
	picker.setupOrderIdAsContainer("19", "19");
	LOGGER.info("2c: finished 19, add 20");
	picker.setupOrderIdAsContainer("20", "20");
	LOGGER.info("2d: finished 20");
	
	LOGGER.info("3a: Move order 20 to position 21");
	picker.setupOrderIdAsContainer("20", "21");

	// 1-9 yields "digits" which is just the number shifted to left display. Seems bad, but not very important. Only happens if someone is 
	// setting up order number or preassigned container that is only one digit. Aside from tests, almost always there are more digits.
	// (Stupid feature. Thankfully, we do not have to do this for pick counts. We should undo this feature.)
	Assert.assertEquals(11, (int) picker.getLastSentPositionControllerDisplayValue(11));

	LOGGER.info("4: Scan start twice, getting to picking state");
	
	picker.scanCommand("START");
	picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
	picker.logCheDisplay();
	Assert.assertEquals(1, (int) picker.getLastSentPositionControllerDisplayValue(11)); // only one job
	picker.scanCommand("START");
	picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
	picker.logCheDisplay();
	Assert.assertEquals(1, (int) picker.getLastSentPositionControllerDisplayValue(1));
	
	LOGGER.info("5a: Complete a few, just to see some oc values come. Complete the first.");
	picker.pickItemAuto();
	LOGGER.info("5b: Complete the secone");
	picker.pickItemAuto();
	LOGGER.info("5a: Complete the third");
	picker.pickItemAuto();
	
	LOGGER.info("6: Back to Setup summary screen");
	picker.scanCommand("START");
	picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
	// This shows a sort of bug in the log/console. See the that the feedback message coming back from the server does not have completes
	// for positions 1,2, and 3. But site controller still remembers. If the site controller had to restart, probably would not get the "oc" that we get.

}
	
	@Test
	public final void simulPickShortOrderCountIssue() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();

		facility = facility.reload();

		propertyService.changePropertyValue(facility, DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "SKU");
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		LOGGER.info("1: upload 2 identical orders");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate\n"
				+ "1111,1,ItemS1,,11,each,LocX24,1111,1,,1,10,20,\n"
				+ "1111,2,ItemS2,,12,each,LocX25,1111,1,,1,10,20,\n"
				+ "2222,3,ItemS1,,22,each,LocX24,2222,1,,1,10,20,\n" + "2222,4,ItemS2,,23,each,LocX25,2222,1,,1,10,20,\n";
		importOrdersData(facility, csvOrders);

		commitTransaction();

		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: load 2 orders on the CHE");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1111", "1");
		picker.setupOrderIdAsContainer("2222", "2");

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		Assert.assertEquals(11, (int) picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertEquals(22, (int) picker.getLastSentPositionControllerDisplayValue(2));

		LOGGER.info("3: Short the first pair of items");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("4: Assert that order counts are correct");
		SetupOrdersDeviceLogic device = (SetupOrdersDeviceLogic) picker.getCheDeviceLogic();
		Map<String, WorkInstructionCount> countMap = device.getMContainerToWorkInstructionCountMap();
		WorkInstructionCount countOrder1 = countMap.get("1111");
		WorkInstructionCount countOrder2 = countMap.get("2222");
		Assert.assertNotNull(countOrder1);
		Assert.assertNotNull(countOrder2);
		Assert.assertEquals(1, countOrder1.getGoodCount());
		Assert.assertEquals(1, countOrder1.getShortCount());
		Assert.assertEquals(1, countOrder2.getGoodCount());
		Assert.assertEquals(1, countOrder2.getShortCount());
		return;
	}

	/**
	 * This test verifies that the "bay change" displays disappears after the button is pressed,
	 * and a different CHE poscon displays the quantity for the next pick
	 */
	@Test
	public final void testBayChangeDisappearTest() throws IOException {
		LOGGER.info("1: setup facility");
		setUpOneAisleFourBaysFlatFacilityWithOrders();

		this.startSiteController();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: assign two identical two-item orders to containers on the CHE");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("7", "1");
		picker.setupOrderIdAsContainer("8", "2");

		LOGGER.info("3: verify 'Location Select' and work on containers");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Byte posConValue1 = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("2"), posConValue1);
		Byte posConValue2 = picker.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("2"), posConValue2);

		LOGGER.info("4: verify generated instructions");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item2", "Item2", "Housekeeping", "Item6", "Item6" };
		compareInstructionsList(wiList, expectations);

		LOGGER.info("5: pick two items before bay change");
		picker.pick(2, 40);
		picker.pick(1, 40);

		// JR_BUG
		LOGGER.info("6: verify 'bc' code on position 1; then - press button to advance");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_B, picker.getLastSentPositionControllerMaxQty((byte) 1));
		picker.buttonPress(1, 0);

		// The main purpose of the test.
		LOGGER.info("7: verify the correct quantity on position 2, and nothing on position 1");
		Assert.assertEquals(new Byte("30"), picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));

		LOGGER.info("8: wrap up the test by finishing both orders");
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
	}

	@Test
	public final void testPressWrongButton() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		LOGGER.info("1: Upload 2 orders");
		beginTransaction();
		facility = facility.reload();
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate\n"
				+ "1111,1,ItemS1,,11,each,LocX24,1111,1,,1,10,20,\n"
				+ "2222,2,ItemS2,,22,each,LocX25,2222,2,,1,10,20,\n"
				+ "2222,3,ItemS3,,33,each,LocX26,2222,3,,1,10,20,\n";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: Load 2 orders on cart");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1111", "1");
		picker.setupOrderIdAsContainer("2222", "2");

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("3: Complete order 1, and ensure its poscon shows 'OC'");
		Assert.assertEquals(11, (int) picker.getLastSentPositionControllerDisplayValue(1));
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_O, picker.getLastSentPositionControllerMaxQty((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_C, picker.getLastSentPositionControllerMinQty((byte) 1));

		LOGGER.info("4: When picking for order 2, press button for order 1, ensure that nothing changes");
		Assert.assertEquals(22, (int) picker.getLastSentPositionControllerDisplayValue(2));
		picker.buttonPress(1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(22, (int) picker.getLastSentPositionControllerDisplayValue(2));

		LOGGER.info("5: Pick first item in order 1, ensure that Housekeeping comes up on poscon 2");
		picker.buttonPress(2);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_R, picker.getLastSentPositionControllerMinQty((byte) 2));

		LOGGER.info("6: Press poscon 1, ensure that nothing changes");
		picker.buttonPress(1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_R, picker.getLastSentPositionControllerMinQty((byte) 2));

		LOGGER.info("7: Press poscon 2, ensure that the second item in order 2 is displayed");
		picker.buttonPress(2);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(33, (int) picker.getLastSentPositionControllerDisplayValue(2));

		LOGGER.info("8: Scan SHORT, wait for poscon 2 to be solid");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		Assert.assertEquals(33, (int) picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals(PosControllerInstr.SOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));

		LOGGER.info("9: Press poscon 1, ensure that nothing changes");
		picker.buttonPress(1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		Assert.assertEquals(33, (int) picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals(PosControllerInstr.SOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));

		LOGGER.info("10: Press poscon 2, ensure that picking is done and poscon 2 now shows '=='");
		picker.buttonPress(2, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TOP_BOTTOM, picker.getLastSentPositionControllerMaxQty((byte) 2));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TOP_BOTTOM, picker.getLastSentPositionControllerMinQty((byte) 2));

	}

}