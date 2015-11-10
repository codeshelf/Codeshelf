package com.codeshelf.edi;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
import com.codeshelf.util.ThreadUtils;

public class OrderReimportTest extends ServerTest {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderReimportTest.class);

	@Test
	public final void orderNotPickedYet() throws IOException {
		// set up facility and turn off housekeeping
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		// import order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// verify imported order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		assertEquals(true, order.getActive());

		OrderStatusEnum status = order.getStatus();
		assertEquals(OrderStatusEnum.RELEASED, status);

		List<OrderDetail> details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));

		commitTransaction();
		
		// re-import order with different order lines
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
 				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
//				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// check order state after re-import
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		
		assertEquals(OrderStatusEnum.RELEASED, status);
		assertEquals(true, order.getActive());
		
		details = order.getOrderDetails();
		assertEquals(3, details.size());
		assertEquals(2, countActive(details));
		
		@SuppressWarnings("unchecked")
		List<OrderDetail> orderDetails = (List<OrderDetail>) order.getChildren();
		for (OrderDetail detail : orderDetails) {
			String domainId = detail.getDomainId();
			if (domainId.equals("1522-each")) {
				assertEquals(false, detail.getActive());
			}
			else {
				assertEquals(true, detail.getActive());				
			}
		}
		commitTransaction();		
	}

	@Test
	public final void orderCompletedNoChange() throws IOException {
		// set up facility and turn off housekeeping
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		// import order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// verify imported order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		assertEquals(true, order.getActive());

		OrderStatusEnum status = order.getStatus();
		assertEquals(OrderStatusEnum.RELEASED, status);

		List<OrderDetail> details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));

		commitTransaction();
		
		// pick items
		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());
		
		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);
		picker.logout();	
		
		// verify order status
		ThreadUtils.sleep(2000);
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		assertEquals(OrderStatusEnum.COMPLETE, status);
		assertEquals(true, order.getActive());
		commitTransaction();
		
		// re-import order with different order lines
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// check order state after re-import
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		
		// status should still be complete
		assertEquals(OrderStatusEnum.COMPLETE, status);
		assertEquals(true, order.getActive());
		
		details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));
		
		commitTransaction();		
	}
	
	@Test
	public final void orderCompletedItemAdded() throws IOException {
		// set up facility and turn off housekeeping
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		// import order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// verify imported order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		assertEquals(true, order.getActive());

		OrderStatusEnum status = order.getStatus();
		assertEquals(OrderStatusEnum.RELEASED, status);

		List<OrderDetail> details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));

		commitTransaction();
		
		// pick items
		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());
		
		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);
		picker.logout();	
		
		// verify order status
		ThreadUtils.sleep(2000);
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		assertEquals(OrderStatusEnum.COMPLETE, status);
		assertEquals(true, order.getActive());
		commitTransaction();
		
		// re-import order with one order line added
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// check order state after re-import
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		
		// status should be released after adding a line to a complete order
		assertEquals(OrderStatusEnum.RELEASED, status);
		assertEquals(true, order.getActive());
		
		details = order.getOrderDetails();
		assertEquals(3, details.size());
		assertEquals(3, countActive(details));
		
		commitTransaction();		
	}
	
	@Test
	public final void orderCompletedItemRemoved() throws IOException {
		// set up facility and turn off housekeeping
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,each,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		// import order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// verify imported order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		assertEquals(true, order.getActive());

		OrderStatusEnum status = order.getStatus();
		assertEquals(OrderStatusEnum.RELEASED, status);

		List<OrderDetail> details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));

		commitTransaction();
		
		// pick items
		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());
		
		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);
		picker.logout();	
		
		// verify order status
		ThreadUtils.sleep(2000);
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		assertEquals(OrderStatusEnum.COMPLETE, status);
		assertEquals(true, order.getActive());
		commitTransaction();
		
		// re-import order with one order line added
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// check order state after re-import
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		
		// status should be released after adding a line to a complete order
		assertEquals(OrderStatusEnum.RELEASED, status);
		assertEquals(true, order.getActive());
		
		details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(1, countActive(details));
		
		commitTransaction();		
	}		

	@Test
	public final void orderCompletedQuantityChanged() throws IOException {
		// set up facility and turn off housekeeping
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,each,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		// import order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// verify imported order
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		assertEquals(true, order.getActive());

		OrderStatusEnum status = order.getStatus();
		assertEquals(OrderStatusEnum.RELEASED, status);

		List<OrderDetail> details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));

		commitTransaction();
		
		// pick items
		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());
		
		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);
		picker.logout();	
		
		// verify order status
		ThreadUtils.sleep(2000);
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		assertEquals(OrderStatusEnum.COMPLETE, status);
		assertEquals(true, order.getActive());
		commitTransaction();
		
		// re-import order with one order line added
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// check order state after re-import
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		status = order.getStatus();
		
		// status should be released after adding a line to a complete order
		assertEquals(OrderStatusEnum.RELEASED, status);
		assertEquals(true, order.getActive());
		
		details = order.getOrderDetails();
		assertEquals(2, details.size());
		assertEquals(2, countActive(details));
		
		commitTransaction();		
	}
	
	private static int countActive(List<OrderDetail> details) {
		int num = 0;
		if (details!=null) {
			for (OrderDetail detail : details) {
				if (detail.getActive()) num++;
			}
		}
		return num;
	}	

}
