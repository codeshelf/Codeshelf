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
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

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
		UUID detail1_1a = wis.get(0).getOrderDetail().getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Import the orders again. This file has two lines, separate detail IDs for 11111, testItem 1, each. And just a different detail ID line for 22222");
		// Since 22222.1 is not represented in this file, that detail will be inactivated. Later, if it completes, will get a WARN for completing inactive order.
		beginTransaction();
		facility = facility.reload();
		String csvOrders2 = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n11111,11111,11111.1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n11111,11111,11111.2,1,Test Item 1,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n22222,22222,22222.2,2,Description Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders2);
		commitTransaction();

		LOGGER.info("4: Let's get the UUIDs of the work instructions and detail. Are they the same?");
		beginTransaction();
		List<WorkInstruction> wis2 = picker.getServerVersionAllPicksList();
		Assert.assertEquals(2, wis2.size());
		UUID wi1bId = wis2.get(0).getPersistentId();
		UUID detail1_1b = wis2.get(0).getOrderDetail().getPersistentId();
		UUID wi2bId = wis2.get(1).getPersistentId();
		commitTransaction();

		Assert.assertEquals(wi1aId, wi1bId);
		Assert.assertEquals(wi2aId, wi2bId);
		Assert.assertEquals(detail1_1a, detail1_1b); // This is fairly amazing. Different domain ID, but we followed our business rule of same order/item/uom to call it the same one

		LOGGER.info("5: Complete the pick. Should be no server-side errors");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("6: Check displays. Is the order done");
		picker.assertPosconDisplayOc(1); // sort of a bug here. site controller thinks it is done, but we only picked two of three
		picker.assertPosconDisplayOc(2);

		//Wait for pick to propagate through server
		ThreadUtils.sleep(500);
		
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh1 = OrderHeader.staticGetDao().findByDomainId(facility, "11111");
		Assert.assertNotNull(oh1);
		List<OrderDetail> details = oh1.getOrderDetails();
		Assert.assertEquals(2, details.size());
		OrderDetail detail1a = oh1.getOrderDetail("11111.1");
		Assert.assertNotNull(detail1a);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1a.getStatus());
		Assert.assertTrue(detail1a.getActive()); // This still active as it was represented in the latest file

		OrderHeader oh2 = OrderHeader.staticGetDao().findByDomainId(facility, "22222");
		OrderDetail detail2a = oh2.getOrderDetail("22222.1"); // This API still finds inactive orders
		Assert.assertNotNull(detail2a);
		detail2a = OrderDetail.staticGetDao().findByDomainId(oh2, "22222.1");
		Assert.assertNotNull(detail2a);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail2a.getStatus()); // bug? depends on server side handling
		Assert.assertFalse(detail2a.getActive()); // inactive as it was not represented in latest file

		OrderDetail detail2b = oh2.getOrderDetail("22222.2");
		Assert.assertNotNull(detail2b);
		Assert.assertEquals(OrderStatusEnum.RELEASED, detail2b.getStatus());
		Assert.assertTrue(detail2b.getActive());
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

	/**
	 * Trying to replicate what seemed to stop us during Walmart trial. DEV-1415
	 * Unable to. This does separate picks for the duplicate order lines as intended, without needing new scan.
	 */
	@Test
	public final void doubleDetailId() throws IOException {

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "sku1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "sku2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "sku3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "sku4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "sku6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		beginTransaction();
		importInventoryData(facility, csvInventory);
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// Dallas trial used the config. We can do it by order line to set scan needed.
		String csvOrders = "preAssignedContainerId,orderId,orderDetailId,itemId,gtin, description,quantity,uom,needsScan"
				+ "\r\n11111,11111,11111.1,sku1,gtin1,Test Item 1,2,each,Y"
				+ "\r\n11111,11111,11111.2,sku1,gtin1,Test Item 1,3,each,Y"
				+ "\r\n22222,22222,22222.1,sku1,gtin1,Test Item 1,1,each,Y"
				+ "\r\n22222,22222,22222.2,sku2,gtin2,Description Item 2,1,each,Y";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: set up cart with two orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.logLastCheDisplay();

		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 3000);
		picker.logLastCheDisplay();

		LOGGER.info("2: Pick, scanning each. At Walmart, the first pick worked, then stuck on the second.");
		picker.scanSomething("gtin1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.logLastCheDisplay();
		// proceed in exquisite detail to try to find and localize any bug. Instead of pickItemAuto();

		WorkInstruction wi = picker.getFirstActivePick();
		int button = picker.buttonFor(wi);
		int quantity = wi.getPlanQuantity();
		picker.pick(button, quantity);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.logLastCheDisplay();

		LOGGER.info("3: After the first pick, scan location/tape again. Scan START/START would be equivalent. Needs scan again if user does this.");
		// curious!  The scan location variant stays on the 301 pick, but start/start goes to 302. Change to testVariable = true;
		// If testVariable = true, CORRECTLY does all the sku1 picks first, then sku2.
		// If testVariable = false, INCORRECTLY switches back and forth. START should have remembered we were at D301.
		
		boolean testVariable = false;
		if (testVariable) {
			picker.scanLocation("D301");
		} else {
			picker.scanCommand("START");
			picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
			picker.scanCommand("START");
		}
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 3000);
		picker.logLastCheDisplay();

		if (testVariable) {
			picker.scanSomething("gtin1");
		} else {
			picker.scanSomething("gtin2");
		}
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.logLastCheDisplay();

		wi = picker.getFirstActivePick();
		button = picker.buttonFor(wi);
		quantity = wi.getPlanQuantity();
		picker.pick(button, quantity);
		if (testVariable) {
			picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
			picker.logLastCheDisplay();
		} else {
			picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 3000);
			picker.logLastCheDisplay();
			picker.scanSomething("gtin1");
			picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
			picker.logLastCheDisplay();
		}

		wi = picker.getFirstActivePick();
		button = picker.buttonFor(wi);
		quantity = wi.getPlanQuantity();
		picker.pick(button, quantity);
		if (testVariable) {
			picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 3000);
			picker.logLastCheDisplay();
			picker.scanSomething("gtin2");
		}
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.logLastCheDisplay();

		wi = picker.getFirstActivePick();
		button = picker.buttonFor(wi);
		quantity = wi.getPlanQuantity();
		picker.pick(button, quantity);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.logLastCheDisplay();

		picker.logout();
	}

	/**
	 * The "difficult" (for the worker) case of picker walking away ready to pick, then the box is stolen to another cart. 
	 * If this picker does not setup and just completes the job, deal with it. 
	 */
	@Test
	public final void pickingOrderMovedToOtherCart() throws IOException {

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
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,33333,33333,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: set up cart with three orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2: Let's get the UUIDs of the work instructions");
		beginTransaction();
		List<WorkInstruction> wis = picker.getServerVersionAllPicksList();
		Assert.assertEquals(3, wis.size());
		UUID wi1aId = wis.get(0).getPersistentId();
		UUID wi2aId = wis.get(1).getPersistentId();
		commitTransaction();

		LOGGER.info("3: Cart 1 did nothing yet. Set up two of the orders on Cart 2");
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

		// DEV-1347 major change
		// This is the "difficult" case of picker walking away ready to pick, then the box is stolen to another cart. If this picker does not setup and just completes the job
		// deal with it as elegantly as possible.
		LOGGER.info("5: What happens if picker 1 then tries to complete the pick? Picker1's che was notified the cntr was stolen away, so it warns and goes to setup state.");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("5b: But two STARTs needed, to go through the recompute and get the feedback right.");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		List<WorkInstruction> wis3 = picker.getAllPicksList();
		Assert.assertEquals(1, wis3.size());

		LOGGER.info("5c: Able to complete only the one job that was not stolen");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi1a = WorkInstruction.staticGetDao().findByPersistentId(wi1aId);
		Assert.assertNull(wi1a);
		commitTransaction();

		LOGGER.info("6: What happens if picker 2 then tries to complete the picks for the containers stolen away? Works fine.");
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
		picker2.logout();

		// For DEV-1370. At PFSWeb, worker got far ahead of computeWork response, so got out of state. In the end
		// was able to submit get work for an empty cart, which led to a SQL error. Try to replicate.
		LOGGER.info("7: Empty compute work scenarios");
		picker2.loginAndSetup("Picker #2");
		picker2.scanCommand("START");
		// not allowed by normal process		
		picker2.waitForCheState(CheStateEnum.NO_CONTAINERS_SETUP, 3000);
		picker2.scanCommand("CANCEL");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("7b: Force to state, then start. This does not send the request either");
		CheDeviceLogic logic = picker2.getCheDeviceLogic();
		logic.testOnlySetState(CheStateEnum.SETUP_SUMMARY);
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		picker2.logout();

	}

	/**
	 * The more normal case of picker being in the middle of picking to other orders, as an order is stolen to another cart. 
	 * At the end of the SKU pick or multi-pick, forces to setup state to deal with it. 
	 */
	@Test
	public final void nonPickingOrderMovedToOtherCart() throws IOException {

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
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,33333,33333,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: set up cart with three orders and start");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("3: Cart 1 did nothing yet. Set up order 33333 on Cart 2");
		picker2.loginAndSetup("Picker #2");
		picker2.setupOrderIdAsContainer("33333", "3");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker2.scanLocation("D301");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);

		// DEV-1347 major change
		LOGGER.info("5: Picker 1 is allowed to finish that pick. But then back to summary screen to force a recompute. Two STARTs, to get the feedback counts right");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		List<WorkInstruction> wis3 = picker.getAllPicksList();
		Assert.assertEquals(1, wis3.size());

		LOGGER.info("5b: Able to complete the other job that was not stolen");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("6: What happens if picker 2 then tries to complete the pick for the container stolen away? Works fine.");
		picker2.pickItemAuto();
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		picker.logout();
		picker2.logout();
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
	 * Attempt to reproduce same worker completing earlier work instruction due to slow getwork/computework.
	 * Not yet achieved.
	 */
	@Test
	public final void completeAfterSlowComputeWork() throws IOException {

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D301,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		beginTransaction();
		importInventoryData(facility, csvInventory);
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,33333,33333,4,Test Item 4,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		this.startSiteController();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1: set up cart with two orders and start, then back to summary state");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		List<WorkInstruction> wis = picker.getAllPicksList();
		this.logWiList(wis);

		LOGGER.info("1b: Get the site controller job for this first computation");
		WorkInstruction wi = picker.getFirstActivePick();
		UUID uuid1 = wi.getPersistentId();
		int button = picker.buttonFor(wi);
		int quantity = wi.getPlanQuantity();

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("2: START, but force the old UUID onto the work instruction, faking still having the old.");
		// In production it happens like this: start, slow computing. Reset CHE, goes back a state (avoids being stuck if the message would never come.)
		// Then START, waiting for response, the first response finally comes. Completes the first job. In the mean time,
		// the server has redone the work instruction. That response goes to site controller, but sc will not absorb
		// the new plans while in pick state.
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		wis = picker.getAllPicksList();
		this.logWiList(wis);
		WorkInstruction wi2 = picker.getFirstActivePick();
		UUID uuid2 = wi2.getPersistentId();
		Assert.assertNotEquals(uuid1, uuid2);
		// here is the big fake. The button press will get the container ID, then iterate active picks looking for matching container.
		// So attempt change the persistent ID in site controller memory for that. Need to get the actual WI reference in the list.
		wi2.setPersistentId(uuid1); // might not work. Saw it not work once.  A different reference obtained via picker

		// go more directly at the reference we want
		CheDeviceLogic logic = picker.getCheDeviceLogic();
		List<WorkInstruction> wisReference = logic.getActivePickWiList();
		for (WorkInstruction wi3 : wisReference) {
			wi3.setPersistentId(uuid1); // Works!  Yields the error.
		}

		LOGGER.info("3: This pick should send old wi to server");
		picker.pick(button, quantity);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.logCheDisplay(); // No error shown. Just moved onto the next job.
		
		LOGGER.info("3b: And the next pick is refused, sending to summary state instead");
		// Would be nice if it did not wait for the next pick. But we do not know how long before the response comes back.
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.logCheDisplay(); // No error shown. Just moved onto the next job.

		LOGGER.info("4: Verify that server did not complete the first job");
		beginTransaction();
		facility = facility.reload();
		WorkInstruction wi2b = WorkInstruction.staticGetDao().findByPersistentId(uuid2.toString());
		Assert.assertNotNull(wi2b);
		Assert.assertNotEquals(WorkInstructionStatusEnum.COMPLETE, wi2b.getStatus());
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
	
	/**
	 * One item picked on cart. One is ready to be picked. Remove both during re-importing
	 */
	@Test
	public void reimportRemoveAlreadyPickedDetail() throws IOException{
		LOGGER.info("1. Setup facility and import order with 2 details.");
		Facility facility = setUpSimpleNoSlotFacility();
		
		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,2,each,2012-09-26,2012-09-26,Loc1,0"
				+ "\r\n11111,11111,2,Test Item 2,1,each,2012-09-26,2012-09-26,Loc2,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2. Pick first detail, get second detail ready to pick.");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Worker1");
		picker.setupContainer("11111", "1");
		picker.start(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "1 order", "2 jobs", "", "START (or SETUP)");
		picker.start(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "Loc1", "1", "QTY 2", "");
		picker.pickItemAuto();
		verifyCheDisplay(picker, "Loc2", "2", "QTY 1", "");
		
		LOGGER.info("3. Re-import order, removing both details.");
		ThreadUtils.sleep(500);
		beginTransaction();
		csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,locationId,workSequence"
				+ "\r\n11111,11111,3,Test Item 3,3,each,2012-09-26,2012-09-26,Loc3,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();
		
		LOGGER.info("4. Pick second detail. Note the WARN message in the log.");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "1 order", "0 jobs", "2 done", "SETUP");
	}
}