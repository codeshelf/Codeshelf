/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

/**
 * This replicates the essence of a few situation seen in production.
 * @author jon ranstrom
 *
 */
public class CheProcessPickExceptions extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessPickExceptions.class);

	public CheProcessPickExceptions() {

	}

	@Test
	public final void reimportSameOrders() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
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

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Import the same exact orders again");
		beginTransaction();
		facility = facility.reload();
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("4: Let's get the UUIDs of the work instructions. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1aId, wi1bId);
		Assert.assertEquals(wi2aId, wi2bId);

		LOGGER.info("5: Complete the pick. Should be no server-side errors");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("6: Check displays, try to START again. Should get nothing");
		picker.assertPosconDisplayOc(1);
		picker.assertPosconDisplayOc(2);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.assertPosconDisplayOc(1);
		picker.assertPosconDisplayOc(2);

		picker.logout();
	}

	/**
	 * Shows that reimport order with different count after cart is set up does not change the existing WI persistent ID. In fact, the WI is not updated
	 */
	@Test
	public final void reimportDifferentOrders() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		String csvOrders = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Import the orders again. Count changed on item 1. Description change on item 2");
		beginTransaction();
		facility = facility.reload();
		String csvOrders2 = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.1,1,Test Item 1,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.1,2,Description Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders2);
		commitTransaction();

		LOGGER.info("4: Let's get the UUIDs of the work instructions. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1aId, wi1bId);
		Assert.assertEquals(wi2aId, wi2bId);

		LOGGER.info("5: Complete the pick. Should be no server-side errors");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("6: Check displays. Is the order done");
		picker.assertPosconDisplayOc(1); // sort of a bug here. site controller thinks it is done, but we only picked two of three
		picker.assertPosconDisplayOc(2);
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh = OrderHeader.staticGetDao().findByDomainId(facility, "11111");
		Assert.assertNotNull(oh);
		List<OrderDetail> details = oh.getOrderDetails();
		Assert.assertEquals(1, details.size());
		OrderDetail detail = details.get(0);
		Assert.assertEquals(OrderStatusEnum.SHORT, detail.getStatus());
		commitTransaction();

		LOGGER.info("7: START again");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		// back to revised summary screen. What does 1 show?
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == 1);
		picker.assertPosconDisplayOc(2); //aren't we clever!. Still says oc as that order was never removed from the cart.
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		picker.logout();
	}

	/**
	 * Order has item. Setup cart. Order does not have item. Then order has item
	 */
	@Test
	public final void reimportDeleteAndPutBack() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// Item 9x not in inventory so will not get a plan made.
		String csvOrders = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n11111,11111,11111.2,9x,Test Item 9x,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.2,9x,Test Item 9x,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Import. But the orders have removed those items");
		beginTransaction();
		facility = facility.reload();
		String csvOrders2 = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.2,9x,Test Item 9x,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.2,9x,Test Item 9x,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders2);
		commitTransaction();

		LOGGER.info("4: Let's get the UUIDs of the work instructions. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1aId, wi1bId);
		Assert.assertEquals(wi2aId, wi2bId);

		LOGGER.info("5: Complete one pick. This yields a WARN about completing for an inactive order detail");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("6: Import the original file again, reactivating the details.");
		beginTransaction();
		facility = facility.reload();
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("6: Let's get the UUIDs of the work instructions. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis3 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis3.size());
		UUID wi1cId = wis3.get(0).getPersistentId();
		UUID wi2cId = wis3.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1cId, wi1bId);
		Assert.assertEquals(wi2cId, wi2bId);

		LOGGER.info("5: Complete one pick. Works");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		picker.logout();
	}

	/**
	 * Should not happen, but this test checks when the order detail ID changes even though order, item, uom is all the same
	 */
	@Test
	public final void reimportDifferentDetailId() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		String csvOrders = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID detail1a = wis.get(0).getOrderDetail().getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Import the orders again. Count changed on item 1. Description change on item 2. But the order details IDs are different.");
		beginTransaction();
		facility = facility.reload();
		String csvOrders2 = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.2,1,Test Item 1,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.2,2,Description Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders2);
		commitTransaction();

		LOGGER.info("4: Let's get the UUIDs of the work instructions and detail. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID detail1b = wis2.get(0).getOrderDetail().getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1aId, wi1bId);
		Assert.assertEquals(wi2aId, wi2bId);
		Assert.assertEquals(detail1a, detail1b); // This is fairly amazing. Different domain ID, but we followed our business rule of same order/item/uom to call it the same one

		LOGGER.info("5: Complete the pick. Should be no server-side errors");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("6: Check displays. Is the order done");
		picker.assertPosconDisplayOc(1); // sort of a bug here. site controller thinks it is done, but we only picked two of three
		picker.assertPosconDisplayOc(2);
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh = OrderHeader.staticGetDao().findByDomainId(facility, "11111");
		Assert.assertNotNull(oh);
		List<OrderDetail> details = oh.getOrderDetails();
		// TODO
		// DEV-1323 change. Used to just change the detailID. Now gets a new one, so size is 2 instead of 1 as before.
		// The first detail is still COMPLETE. Active? No! Due to the fact that this detail is not represented for the order in this file. Odd case. 
		Assert.assertEquals(2, details.size());
		OrderDetail detail1 = oh.getOrderDetail("11111.1");
		Assert.assertNotNull(detail1);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1.getStatus());
		Assert.assertFalse(detail1.getActive()); // maybe a bug.

		OrderDetail detail2 = oh.getOrderDetail("11111.2");
		Assert.assertNotNull(detail2);
		Assert.assertEquals(OrderStatusEnum.RELEASED, detail2.getStatus());
		Assert.assertTrue(detail2.getActive());
		commitTransaction();

		LOGGER.info("7: START again");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		// back to revised summary screen. What does 1 show?
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == 1);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		picker.logout();
	}

	@Test
	public final void finishOrderAfterMovedToOtherCart() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
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
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Cart 1 did nothing yet. Set up those orders on Cart 2");
		picker2.loginAndSetup("Picker #2");
		picker2.setupOrderIdAsContainer("11111", "1");
		picker2.setupOrderIdAsContainer("22222", "2");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker2.scanLocation("D301");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("4: Let's get the UUIDs of the work instructions. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker2.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertNotEquals(wi1aId, wi1bId);
		Assert.assertNotEquals(wi2aId, wi2bId);

		LOGGER.info("5: What happens if picker 1 then tries to complete the pick? Nothing much. Get some server errors");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi1a = WorkInstruction.staticGetDao().findByPersistentId(wi1aId);
		Assert.assertNull(wi1a);
		commitTransaction();

		LOGGER.info("6: What happens if picker 2 then tries to complete the pick? Works fine.");
		picker2.pickItemAuto();
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker2.pickItemAuto();
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi1b = WorkInstruction.staticGetDao().findByPersistentId(wi1bId);
		Assert.assertNotNull(wi1b);
		OrderDetail detail = wi1b.getOrderDetail();
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail.getStatus());
		commitTransaction();

		picker.logout();
	}

	/**
	 * Shows that we get new persistent ID for work instruction when setting up cart again.
	 */
	@Test
	public final void doubleSetupSameCart() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
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

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Cart 1 did nothing yet. Set up those orders again on Cart 1. Done via logout, but could just do a setup");
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("4: Let's get the UUIDs of the work instructions. Are they the same? No! They could be.");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertNotEquals(wi1aId, wi1bId);
		Assert.assertNotEquals(wi2aId, wi2bId);

		LOGGER.info("5: What happens if picker 1 then tries to complete the pick? Works fine.");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi1a = WorkInstruction.staticGetDao().findByPersistentId(wi1aId);
		Assert.assertNull(wi1a);
		WorkInstruction wi1b = WorkInstruction.staticGetDao().findByPersistentId(wi1bId);
		Assert.assertNotNull(wi1b);
		OrderDetail detail = wi1b.getOrderDetail();
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail.getStatus());
		commitTransaction();

		picker.logout();
	}

	/**
	 * Shows that we get new persistent ID each START or REVERSE
	 */
	@Test
	public final void repeatedStartReverse() throws IOException {

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
		PropertyBehavior.turnOffHK(facility);
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

		LOGGER.info("1a: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("1b: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("2a: Cart 1 did nothing yet. Logout and start again, but don't set up again");
		picker.logout();
		picker.scanSomething("U%Picker #1");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2b: Let's get the UUIDs of the work instructions. Are they the same? No! They could be.");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertNotEquals(wi1aId, wi1bId);
		Assert.assertNotEquals(wi2aId, wi2bId);

		LOGGER.info("3a: Cart 1 did nothing yet. REVERSE");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("REVERSE");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("3b: Let's get the UUIDs of the work instructions. Are they the same? No! They could be.");
		beginTransaction();
		List<WorkInstruction> wis3 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis3.size());
		UUID wi1cId = wis3.get(0).getPersistentId();
		UUID wi2cId = wis3.get(1).getPersistentId();
		commitTransaction();

		Assert.assertNotEquals(wi1cId, wi1bId);
		Assert.assertNotEquals(wi2cId, wi2bId);

		LOGGER.info("5: What happens if picker 1 then tries to complete the pick? Works fine.");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi1b = WorkInstruction.staticGetDao().findByPersistentId(wi1bId);
		Assert.assertNull(wi1b);
		WorkInstruction wi1c = WorkInstruction.staticGetDao().findByPersistentId(wi1cId);
		Assert.assertNotNull(wi1c);
		OrderDetail detail = wi1c.getOrderDetail();
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail.getStatus());
		commitTransaction();

		picker.logout();
	}
}