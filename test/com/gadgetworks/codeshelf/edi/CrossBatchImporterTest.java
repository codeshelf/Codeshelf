/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

/**
 * @author jeffw
 * 
 */
public class CrossBatchImporterTest extends EdiTestABC {

	private ItemMaster createItemMaster(final String inItemMasterId, final String inUom, final Facility inFacility) {
		ItemMaster result = null;

		UomMaster uomMaster = inFacility.getUomMaster(inUom);
		if (uomMaster == null) {
			uomMaster = new UomMaster();
			uomMaster.setUomMasterId(inUom);
			uomMaster.setParent(inFacility);
			mUomMasterDao.store(uomMaster);
			inFacility.addUomMaster(uomMaster);
		}

		result = new ItemMaster();
		result.setItemId(inItemMasterId);
		result.setParent(inFacility);
		result.setStandardUom(uomMaster);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mItemMasterDao.store(result);
		inFacility.addItemMaster(result);

		return result;
	}

	@Test
	public final void testCrossBatchImporter() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS1");
		
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 0);

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		// With cross batches, we get one header per unique container, and one detail per unique item in container
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 6);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);


		// Make sure we created an order with the container's ID.
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", ediProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);

		// Make sure there's a contianer use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

	}

	@Test
	public final void testCrossBatchOrderGroups() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C333,I333.1,100,ea\r\n" //
				+ "G1,C333,I333.2,200,ea\r\n" //
				+ "G1,C333,I333.3,300,ea\r\n" //
				+ "G1,C333,I333.4,400,ea\r\n" //
				+ "G1,C444,I444.1,100,ea\r\n" //
				+ "G1,C444,I444.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS2", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS2");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I333.1", "ea", facility);
		createItemMaster("I333.2", "ea", facility);
		createItemMaster("I333.3", "ea", facility);
		createItemMaster("I333.4", "ea", facility);
		createItemMaster("I444.1", "ea", facility);
		createItemMaster("I444.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		OrderGroup group = facility.getOrderGroup("G1");
		Assert.assertNotNull(group);

		OrderHeader order = group.getOrderHeader(OrderHeader.computeCrossOrderId("C333", ediProcessTime));
		Assert.assertNotNull(order);

	}

	@Test
	public final void testResendCrossBatchRemoveItem() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.3,300,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS3");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS3", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS3");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I555.1", "ea", facility);
		createItemMaster("I555.2", "ea", facility);
		createItemMaster("I555.3", "ea", facility);
		createItemMaster("I555.4", "ea", facility);
		createItemMaster("I666.1", "ea", facility);
		createItemMaster("I666.2", "ea", facility);

		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, firstEdiProcessTime);
		
		// With cross batches, we get one header per unique container, and one detail per unique item in container
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 2);
		Assert.assertTrue(theCounts.mActiveHeaders == 2);
		Assert.assertTrue(theCounts.mActiveDetails == 6);
		Assert.assertTrue(theCounts.mActiveCntrUses == 2);


		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		Timestamp secondEdiProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, secondEdiProcessTime);
		
		// The reimport resulted in inactivation of previous order headers for those containers
		// Then we get new stuff.
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 5);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);


		// Make sure that first cross batch order is inactive and contains order detail I555.3 but it's inactive
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C555", firstEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(false, order.getActive());
		OrderDetail orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(false, orderDetail.getActive());

		// Make sure the second cross batch order is active and doesn't contain I555.3
		order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C555", secondEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(true, order.getActive());
		orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNull(orderDetail);

	}

	@Test
	public final void testResendCrossBatchAddItem() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C777,I777.1,100,ea\r\n" //
				+ "G1,C777,I777.2,200,ea\r\n" //
				+ "G1,C777,I777.3,300,ea\r\n" //
				+ "G1,C777,I777.4,400,ea\r\n" //
				+ "G1,C888,I888.1,100,ea\r\n" //
				+ "G1,C888,I888.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS4");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS4", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS4");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I777.1", "ea", facility);
		createItemMaster("I777.2", "ea", facility);
		createItemMaster("I777.3", "ea", facility);
		createItemMaster("I777.4", "ea", facility);
		createItemMaster("I777.5", "ea", facility);
		createItemMaster("I888.1", "ea", facility);
		createItemMaster("I888.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);
		
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

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);
		
		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 7);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);


		// check the new order detail.
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C777", ediProcessTime));
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I777.5");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 500);

	}

	@Test
	public final void testResendCrossBatchAlterItems() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,300,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS5");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS5", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS5");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I999.1", "ea", facility);
		createItemMaster("I999.2", "ea", facility);
		createItemMaster("I999.3", "ea", facility);
		createItemMaster("I999.4", "ea", facility);
		createItemMaster("IAAA.1", "ea", facility);
		createItemMaster("IAAA.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		// Now re-import the interchange changing the count on item 3.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,999,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		// Make sure that order detail item I999.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C999", ediProcessTime));
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I999.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 999);
	}

	/*
	 * TODO this might be better pulled into a higher level EdiProcessor test that goes across crossbatch and outbound
	 */
	@Test
	public final void testSendOrdersAfterCrossBatch() throws IOException {
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS6");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS6", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS6");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp crossBatchEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, crossBatchEdiProcessTime);

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

		byte orderCsvArray[] = orderCsvString.getBytes();

		stream = new ByteArrayInputStream(orderCsvArray);
		reader = new InputStreamReader(stream);

		Timestamp ordersEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter orderImporter = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		orderImporter.importOrdersFromCsvStream(reader, facility, ordersEdiProcessTime);

		// Make sure we imported the outbound order.
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

		// Now make sure that all of the cross batch orders are still valid.
		order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", crossBatchEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a container use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");
		Assert.assertEquals(true, use.getActive());

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

	}

	@Test
	public final void testCrossBatchGroupArchives() {
		// Good eggs has group IDs. Make sure the behavior on reread is similar to above
		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "xx,C111,I111.1,100,ea\r\n" //
				+ "xx,C111,I111.2,200,ea\r\n" //
				+ "xx,C111,I111.3,300,ea\r\n" //
				+ "xx,C111,I111.4,400,ea\r\n" //
				+ "xx,C222,I222.1,100,ea\r\n" //
				+ "xx,C222,I222.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS7");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS7", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS7");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		// Make sure we created an order with the container's ID.
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", ediProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);

		// Make sure there's a contianer use and that its ID matches the order.
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
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "xx,C111,I111.1,100,ea\r\n" //
				+ "xx,C111,I111.2,200,ea\r\n" //
				+ "xx,C111,I111.3,300,ea\r\n" //
				+ "xx,C222,I222.1,100,ea\r\n" //
				+ "xx,C222,I222.2,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts2 = facility.countCrossOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 4);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 5);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);

	}

	@Test
	public final void testCrossBatchDoubleImporter() {

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS8");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS8", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS8");

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

		byte[] firstCsvArray = firstCsvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, firstEdiProcessTime);

		// Make sure we created an order with the container's ID.
		OrderHeader order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", firstEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a contianer use and that its ID matches the order.
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

		byte[] secondCsvArray = secondCsvString.getBytes();

		stream = new ByteArrayInputStream(secondCsvArray);
		reader = new InputStreamReader(stream);

		Timestamp secondEdiProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility, secondEdiProcessTime);

		// Make sure we created an order with the container's ID.
		order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C111", firstEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);
		Assert.assertEquals(true, order.getActive());

		// Make sure there's a contianer use and that its ID matches the order.
		use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

		// Make sure we created an order with the container's ID.
		order = facility.getOrderHeader(OrderHeader.computeCrossOrderId("C333", secondEdiProcessTime));
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);
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

	}
}
