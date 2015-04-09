/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.HeaderCounts;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jeffw
 * 
 */
public class CrossBatchImporterTest extends ServerTest {
	
	private UUID facilityId;
	
	@Override
	public void doBefore() {
		super.doBefore();

		this.getTenantPersistenceService().beginTransaction();

		String facilityName = "F-" + testName.getMethodName();
		Facility facility = Facility.createFacility(facilityName, "TEST", Point.getZeroPoint());
		
		facilityId = facility.getPersistentId();
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private ItemMaster createItemMaster(final String inItemMasterId, final String inUom, final Facility inFacility) {
		ItemMaster result = null;

		UomMaster uomMaster = inFacility.getUomMaster(inUom);
		if (uomMaster == null) {
			uomMaster = new UomMaster();
			uomMaster.setUomMasterId(inUom);
			uomMaster.setParent(inFacility);
			UomMaster.staticGetDao().store(uomMaster);
			inFacility.addUomMaster(uomMaster);
		}

		result = new ItemMaster();
		result.setItemId(inItemMasterId);
		result.setStandardUom(uomMaster);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		inFacility.addItemMaster(result);
		ItemMaster.staticGetDao().store(result);

		return result;
	}

	@Test
	public final void testMissingItemMaster() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.valid,100,ea\r\n" //
				+ ",C111,I111.missing,200,ea\r\n"; //
		
		createItemMaster("I111.valid", "ea", facility);
		
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(1,  count);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testEmptyItemMaster() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.valid,100,ea\r\n" //
				+ ",C111,,200,ea\r\n"; //
		
		createItemMaster("I111.valid", "ea", facility);
		
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(1,  count);
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testInvalidQuantity() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String[] invalidQuantities = new String[]{"0", "-1", "NaN", "1.1"};

		String csvStringTemplate = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,{0},ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n"; //

		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		
		for (String invalidQuantity: invalidQuantities) {
			String csvString = MessageFormat.format(csvStringTemplate, invalidQuantity);
			int count = importBatchData(facility, csvString);
			Assert.assertEquals(1,  count);
			Assert.assertTrue("Did not contain quantity: " + csvString, csvString.contains(invalidQuantity));// sanity check
		}

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCrossBatchImporter() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		Assert.assertEquals(0, facility.countCrossOrders().mTotalHeaders);
		
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);

		Assert.assertEquals(6, count);
		// With cross batches, we get one header per unique container, and one detail per unique item in container
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 6);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);


		// Make sure we created an order with the container's ID.
		//OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", ediProcessTime));		
		String orderId = OrderHeader.computeCrossOrderId("C111", ediProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);

		// Make sure there's a contianer use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCrossBatchOrderGroups() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C333,I333.1,100,ea\r\n" //
				+ "G1,C333,I333.2,200,ea\r\n" //
				+ "G1,C333,I333.3,300,ea\r\n" //
				+ "G1,C333,I333.4,400,ea\r\n" //
				+ "G1,C444,I444.1,100,ea\r\n" //
				+ "G1,C444,I444.2,200,ea\r\n";


		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I333.1", "ea", facility);
		createItemMaster("I333.2", "ea", facility);
		createItemMaster("I333.3", "ea", facility);
		createItemMaster("I333.4", "ea", facility);
		createItemMaster("I444.1", "ea", facility);
		createItemMaster("I444.2", "ea", facility);
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);
		OrderGroup group = facility.getOrderGroup("G1");
		Assert.assertNotNull(group);
		OrderHeader order = group.getOrderHeader(OrderHeader.computeCrossOrderId("C333", ediProcessTime));
		Assert.assertNotNull(order);
		
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testResendCrossBatchRemoveItem() {
		TenantPersistenceService tenantPersistenceService=this.getTenantPersistenceService();
		tenantPersistenceService.beginTransaction();

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.3,300,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";


		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I555.1", "ea", facility);
		createItemMaster("I555.2", "ea", facility);
		createItemMaster("I555.3", "ea", facility);
		createItemMaster("I555.4", "ea", facility);
		createItemMaster("I666.1", "ea", facility);
		createItemMaster("I666.2", "ea", facility);

		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);
		
		// With cross batches, we get one header per unique container, and one detail per unique item in container
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertEquals(2, theCounts.mTotalHeaders);
		Assert.assertEquals(2, theCounts.mActiveHeaders);
		Assert.assertEquals(6, theCounts.mActiveDetails);
		Assert.assertEquals(2, theCounts.mActiveCntrUses);

		// from 3/15/2015 new transaction before doing the reimport.
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		
		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";

		Timestamp secondEdiProcessTime = new Timestamp(System.currentTimeMillis());
		int secondCount = importBatchData(facility, csvString);
		Assert.assertEquals(5, secondCount);
		
		// The reimport resulted in inactivation of previous order headers for those containers
		// Then we get new stuff.
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertEquals(4, theCounts2.mTotalHeaders);
		Assert.assertEquals(2, theCounts2.mActiveHeaders);
		Assert.assertEquals(5, theCounts2.mActiveDetails);
		Assert.assertEquals(2, theCounts2.mActiveCntrUses);

		// Make sure that first cross batch order is inactive and contains order detail I555.3 but it's inactive
		String orderId = OrderHeader.computeCrossOrderId("C555", firstEdiProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		//OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C555", firstEdiProcessTime));
		// JR got assertion fail here on 3/25/15 and 3/31/15 but usually succeeded.
		// From 3/31/15, changed test to do the second import in a separate transaction, which more closely mimics production.
		Assert.assertNotNull(order); 
		Assert.assertEquals(false, order.getActive());
		OrderDetail orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(false, orderDetail.getActive());

		// Make sure the second cross batch order is active and doesn't contain I555.3
		orderId = OrderHeader.computeCrossOrderId("C555", secondEdiProcessTime);
		order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		// order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C555", secondEdiProcessTime));
		Assert.assertNotNull(order); // got assertion fail here on 3/26/15 but not reproducible
		Assert.assertEquals(true, order.getActive());
		orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNull(orderDetail);
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testResendCrossBatchAddItem() {
		this.getTenantPersistenceService().beginTransaction();
		
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C777,I777.1,100,ea\r\n" //
				+ "G1,C777,I777.2,200,ea\r\n" //
				+ "G1,C777,I777.3,300,ea\r\n" //
				+ "G1,C777,I777.4,400,ea\r\n" //
				+ "G1,C888,I888.1,100,ea\r\n" //
				+ "G1,C888,I888.2,200,ea\r\n";

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I777.1", "ea", facility);
		createItemMaster("I777.2", "ea", facility);
		createItemMaster("I777.3", "ea", facility);
		createItemMaster("I777.4", "ea", facility);
		createItemMaster("I777.5", "ea", facility);
		createItemMaster("I888.1", "ea", facility);
		createItemMaster("I888.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);
		
		// With cross batches, we get one header per unique container, and one detail per unique item in container
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 2);
		Assert.assertTrue(theCounts.mActiveHeaders == 2);
		Assert.assertTrue(theCounts.mActiveDetails == 6);
		Assert.assertTrue(theCounts.mActiveCntrUses == 2);


		// Now re-import the interchange with one order missing a single item.
		// add item I777.5
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C777,I777.1,100,ea\r\n" //
				+ "G1,C777,I777.2,200,ea\r\n" //
				+ "G1,C777,I777.3,300,ea\r\n" //
				+ "G1,C777,I777.4,400,ea\r\n" //
				+ "G1,C777,I777.5,500,ea\r\n" //
				+ "G1,C888,I888.1,100,ea\r\n" //
				+ "G1,C888,I888.2,200,ea\r\n";

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		count = importBatchData(facility, csvString);
		Assert.assertEquals(7, count);
		
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 7);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);


		// check the new order detail.
		// OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C777", ediProcessTime));
		String orderId = OrderHeader.computeCrossOrderId("C777", ediProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order); // got one unrepeated error here on 3/26/15
		OrderDetail orderDetail = order.getOrderDetail("I777.5");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 500);
		
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testResendCrossBatchAlterItems() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,300,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I999.1", "ea", facility);
		createItemMaster("I999.2", "ea", facility);
		createItemMaster("I999.3", "ea", facility);
		createItemMaster("I999.4", "ea", facility);
		createItemMaster("IAAA.1", "ea", facility);
		createItemMaster("IAAA.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);

		// Now re-import the interchange changing the count on item 3.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,999,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);

		// Make sure that order detail item I999.3 still exists, but has quantity 0.
		// OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C999", ediProcessTime));
		String orderId = OrderHeader.computeCrossOrderId("C999", ediProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I999.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 999);
		
		this.getTenantPersistenceService().commitTransaction();

	}

	/*
	 * TODO this might be better pulled into a higher level EdiProcessor test that goes across crossbatch and outbound
	 */
	@Test
	public final void testSendOrdersAfterCrossBatch() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp crossBatchEdiProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6, count);
		
		String orderCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, orderCsvString);

		// Make sure we imported the outbound order.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");		
		Assert.assertNotNull(order);

		// Now make sure that all of the cross batch orders are still valid.
		String orderId = OrderHeader.computeCrossOrderId("C111", crossBatchEdiProcessTime);
		order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a container use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");
		Assert.assertEquals(true, use.getActive());

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);
		
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testCrossBatchGroupArchives() {
		this.getTenantPersistenceService().beginTransaction();

		// Good eggs has group IDs. Make sure the behavior on reread is similar to above
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "xx,C111,I111.1,100,ea\r\n" //
				+ "xx,C111,I111.2,200,ea\r\n" //
				+ "xx,C111,I111.3,300,ea\r\n" //
				+ "xx,C111,I111.4,400,ea\r\n" //
				+ "xx,C222,I222.1,100,ea\r\n" //
				+ "xx,C222,I222.2,200,ea\r\n";

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, csvString);
		Assert.assertEquals(6,  count);
		
		// Make sure we created an order with the container's ID.
		String orderId = OrderHeader.computeCrossOrderId("C111", ediProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);

		Assert.assertNotNull(order); // got a random failure here on 3/26/15, did not repeat
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);

		// Make sure there's a container use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);
		
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 2);
		Assert.assertTrue(theCounts.mActiveHeaders == 2);
		Assert.assertTrue(theCounts.mActiveDetails == 6);
		Assert.assertTrue(theCounts.mActiveCntrUses == 2);
		
		// Now re-import the interchange removing item 4.
		String csvStringRemoval = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "xx,C111,I111.1,100,ea\r\n" //
				+ "xx,C111,I111.2,200,ea\r\n" //
				+ "xx,C111,I111.3,300,ea\r\n" //
				+ "xx,C222,I222.1,100,ea\r\n" //
				+ "xx,C222,I222.2,200,ea\r\n";

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		count = importBatchData(facility, csvStringRemoval);
		Assert.assertEquals(5,  count);
		
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 5);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);
		
		// The next bit is a minor test for DEV-278, adding uom to the default domain name.
		// Re-import, with two difference. Duplicate one detail with different uom. And change a uom.
		String csvStringUomChange = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "xx,C111,I111.1,100,ea\r\n" //
				+ "xx,C111,I111.2,200,ea\r\n" //
				+ "xx,C111,I111.3,300,ea\r\n" //
				+ "xx,C222,I222.1,100,ea\r\n" //
				+ "xx,C222,I222.1,5,cs\r\n" //
				+ "xx,C222,I222.2,200,cs\r\n";

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		count = importBatchData(facility, csvStringUomChange);
		Assert.assertEquals(6,  count);

		// JR for DEV-278. Just making it pass now.
		// 5 details. After change, we might get 6 there are both ea and cs orders for I222.1 in C2222
		HeaderCounts theCounts3 = facility.countCrossOrders();
		Assert.assertTrue(theCounts3.mTotalHeaders == 6);
		Assert.assertTrue(theCounts3.mActiveHeaders == 2);
		Assert.assertTrue(theCounts3.mActiveDetails == 5);
		Assert.assertTrue(theCounts3.mActiveCntrUses == 2);
		
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testCrossBatchDoubleImporter() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		String firstCsvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis());
		int count = importBatchData(facility, firstCsvString);
		Assert.assertEquals(6,  count);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		// Make sure we created an order with the container's ID.
		String orderId = OrderHeader.computeCrossOrderId("C111", firstEdiProcessTime);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
	
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a container use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);
		
		// Verify what we got
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 2);
		Assert.assertTrue(theCounts.mActiveHeaders == 2);
		Assert.assertTrue(theCounts.mActiveDetails == 6);
		Assert.assertTrue(theCounts.mActiveCntrUses == 2);


		String secondCsvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C333,I111.1,100,ea\r\n" //
				+ ",C333,I111.2,200,ea\r\n" //
				+ ",C333,I111.3,300,ea\r\n" //
				+ ",C333,I111.4,400,ea\r\n" //
				+ ",C444,I222.1,100,ea\r\n" //
				+ ",C444,I222.2,200,ea\r\n";


		Timestamp secondEdiProcessTime = new Timestamp(System.currentTimeMillis());
		int secondCount = importBatchData(facility, secondCsvString);
		Assert.assertEquals(6,  secondCount);

		// Make sure we created an order with the container's ID.
		orderId = OrderHeader.computeCrossOrderId("C111", firstEdiProcessTime);
		order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a contianer use and that its ID matches the order.
		use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

		// Make sure we created an order with the container's ID.
		orderId = OrderHeader.computeCrossOrderId("C333", secondEdiProcessTime);
		order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderType(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a contianer use and that its ID matches the order.
		use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C333");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);
		
		// second EDI did had different containers than the first. Should just add on orders.
		// Same items. But get new detail per container-item combination therefore new details
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveDetails == 12);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 4);

		this.getTenantPersistenceService().commitTransaction();
	}
}
