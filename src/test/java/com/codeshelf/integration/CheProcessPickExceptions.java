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
		
		Assert.assertEquals(wi1aId,wi1bId);
		Assert.assertEquals(wi2aId,wi2bId);

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
		propertyService.turnOffHK(facility);
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
		
		Assert.assertEquals(wi1aId,wi1bId);
		Assert.assertEquals(wi2aId,wi2bId);

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
		propertyService.turnOffHK(facility);
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
		
		Assert.assertEquals(wi1aId,wi1bId);
		Assert.assertEquals(wi2aId,wi2bId);
		Assert.assertEquals(detail1a,detail1b); // This is fairly amazing. Different domain ID, but we followed our business rule of same order/item/uom to call it the same one

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
		Assert.assertEquals("11111.2", detail.getDomainId()); // See that we actually got the detail updated.
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
	
}