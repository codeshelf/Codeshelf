/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.avaje.ebean.Ebean;
import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.joestelmach.natty.Parser;

/**
 * @author jeffw
 * 
 * Yes, these aren't exactly unit tests, but when they were unit tested they missed a lot of important business behaviors.
 * Sure, the coupling shouldn't be so tight, but Ebean doesn't make it easy to test it's granular behaviors.
 * 
 * While not ideal, we are testing, known, expected business behaviors against the full machinery in a memory-mapped DB
 * that runs at the speed of a unit test (and runs with the units tests).
 * 
 * There are other unit tests of EDI behaviors.
 *
 */
public class OutboundOrderImporterTest extends EdiTestABC {

	// the full set of fields known to the bean  (in the order of the bean, just for easier verification) is
	// orderGroupId,orderId,orderDetailID,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,destinationId,pickStrategy,preAssignedContainerId,shipmentId,customerId,workSequence
	// of these: orderId,itemId,description,quantity,uom are not nullable

	private ICsvOrderImporter	importer;

	@Before
	public void initTest() {
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
	}

	@Test
	public final void testOrderImporterFromCsvStream() throws IOException {

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,120,931,10706962,Sun Ripened Dried Tomato Pesto 24oz,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.1");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount); // 4 details for order header 123. They would get the default name

		OrderHeader order931 = facility.getOrderHeader("931");
		Assert.assertNotNull(order931);
		Integer detail931Count = order931.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detail931Count); // 4 details for order header 123. They would get the default name

		OrderDetail detail931 = order931.getOrderDetail("931");
		Assert.assertNull(detail931); // not this. Do not find by order.
		detail931 = order931.getOrderDetail("10706962");
		Assert.assertNotNull(detail931); // this works, find by itemId within an order.
		String detail931DomainID = detail931.getOrderDetailId(); // this calls through to domainID
		OrderDetail detail931b = order931.getOrderDetail(detail931DomainID);
		Assert.assertNotNull(detail931b); // this works, find by itemId within an order.
		Assert.assertEquals(detail931b, detail931);
		Assert.assertEquals(detail931DomainID, "10706962"); // This is the itemID from file above.

	}

	@Test
	public final void testOrderImporterWithPickStrategyFromCsvStream() throws IOException {

		String csvString = "orderGroupId,pickStrategy,orderId,itemId,description,quantity,uom,orderDate, dueDate\r\n" //
				+ "1,,123,3001,Widget,100,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,4550,Gadget,450,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3007,Dealybob,300,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2150,Thingamajig,220,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3001,Widget,230,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,4550,Gadget,70,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3007,Dealybob,90,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2150,Thingamajig,140,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,PARALLEL,789,2150,Thingamajig,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,PARALLEL,789,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.2", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.2");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		// If not specified, default to serial
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.SERIAL);

		order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.PARALLEL);

	}

	@Test
	public final void testOrderImporterWithPreassignedContainerIdFromCsvStream() throws IOException {

		String csvString = "orderGroupId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate, dueDate\r\n" //
				+ "1,,123,3001,Widget,100,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,4550,Gadget,450,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3007,Dealybob,300,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2150,Thingamajig,220,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3001,Widget,230,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,4550,Gadget,70,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3007,Dealybob,90,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2150,Thingamajig,140,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,CONTAINER1,789,2150,Thingamajig,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,CONTAINER1,789,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.3");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.3", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.3");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);

		Container container = mContainerDao.findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 1);

	}

	@Test
	public void testFailOrderImporterFromCsvStream() throws IOException {
		// orderId,itemId,description,quantity,uom are not nullable according to the bean
		// There's no due date on first order line. nullable, ok
		// There's no itemID on third to last line. (did not matter)
		// There's no uom on second to last line. (did not matter)
		// There's no order date on last order line. nullable, ok
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.4");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.4", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.4");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		// We should find order 123
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

		// Also find order detail item 1. Used to not because there is no due date, but now that field is nullable in the schema.
		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);

		// But should find order detail item 2
		orderDetail = order.getOrderDetail("10706952");
		Assert.assertNotNull(orderDetail);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);
		// Seems possibly wrong! Got a detail for missing itemID.

	}

	@Test
	public void testManyOrderArchive() throws IOException {

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = firstOrderBatchCsv.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.5");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.5", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.5");

		// First import a big list of orders.
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csv2Array = secondOrderBatchCsv.getBytes();

		stream = new ByteArrayInputStream(csv2Array);
		reader = new InputStreamReader(stream);

		// First import a big list of orders.
		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 8);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);
		// Unused details are actually deleted
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		// But container 789 was not deleted. Just went inactive.
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 1);

		// Order 789 should exist and be inactive.
		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(false, order.getActive());

		// Line item 10722222 from order 456 should be inactive.
		order = facility.getOrderHeader("456");
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222")) {
				Assert.assertEquals(false, detail.getActive());
				//Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());
			}
		}
	}

	//@Test
	public void testOneOrderArchive() throws IOException {

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = firstOrderBatchCsv.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.6");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.6", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.6");

		// First import a big list of orders.
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csv2Array = secondOrderBatchCsv.getBytes();

		stream = new ByteArrayInputStream(csv2Array);
		reader = new InputStreamReader(stream);

		// First import a big list of orders.
		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 1);
		Assert.assertTrue(theCounts2.mActiveDetails == 4);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 1);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);

		// Order 789 should exist and be active.
		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(true, order.getActive());

		// Line item 10722222 from order 456 should be inactive.
		order = facility.getOrderHeader("456");
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222")) {
				Assert.assertEquals(false, detail.getActive());
				//Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());
			}
		}
	}

	@Test
	public final void testMinMaxOrderImporterFromCsvStream() throws IOException {

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.7");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.7", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.7");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(Integer.valueOf(0), orderDetail.getMinQuantity());
		Assert.assertEquals(Integer.valueOf(5), orderDetail.getMaxQuantity());
	}

	@Test
	public final void testMinMaxDefaultOrderImporterFromCsvStream() throws IOException {

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.8");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.8", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.8");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMinQuantity());
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMaxQuantity());

	}

	@Test
	public final void testDetailIdOrderImporterFromCsvStream() throws IOException {

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] firstCsvArray = firstCsvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.10");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.10", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.10");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("123.1");
		Assert.assertNotNull(orderDetail);

		order = facility.getOrderHeader("456");
		Assert.assertNotNull(order);
		orderDetail = order.getOrderDetail("456.1");
		Assert.assertNotNull(orderDetail);
		orderDetail = order.getOrderDetail("456.5");
		Assert.assertNotNull(orderDetail);
		// try what worked in the absence of the order detail ID
		orderDetail = order.getOrderDetail("456"); // order ID
		Assert.assertNull(orderDetail);
		orderDetail = order.getOrderDetail("10706962"); // item ID
		Assert.assertNull(orderDetail);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// This is a very odd test. Above had one set of headers, and this a different set. Could happen for different customers maybe, but here same customer and same orders.
		// So, what happens to the details? The answer is the old details all are inactivated for each represented order ID, and new details made.
		String secondCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] secondCsvArray = secondCsvString.getBytes();
		stream = new ByteArrayInputStream(secondCsvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		// Find inactive 123.1 and new active 10700589
		order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		orderDetail = order.getOrderDetail("123.1");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(false, orderDetail.getActive());
		orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);

		// Find inactive 456.1 and new active 10711111 and 10722222
		order = facility.getOrderHeader("456");
		Assert.assertNotNull(order);
		orderDetail = order.getOrderDetail("456.1");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(false, orderDetail.getActive());
		orderDetail = order.getOrderDetail("10711111");
		Assert.assertNotNull(orderDetail);
		orderDetail = order.getOrderDetail("10722222");
		Assert.assertNotNull(orderDetail);

		// And what about order 789, missing altogether in second file? Answer is the header went inactive, along with its 2 details and 1 cntrUse

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 9);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);

	}

	@Test
	public final void testReimportDetailIdOrderImporterFromCsvStream() throws IOException {
		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.9");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.9", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.9");

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importCsvString(facility, firstCsvString);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Import exact same file again
		importCsvString(facility, firstCsvString);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveDetails == 11);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 3);

	}

	/**
	 * Simulates the edi process for order importing
	 */
	@Test
	public void testMultipleImportOfLargeSet() throws IOException, InterruptedException {

		Facility testFacility = getTestFacility("O-testMultipleImportOfLargeSet", "F1");

		//The edi mechanism finds the facility from DAO before entering the importers
		Facility foundFacility = null;
		foundFacility = mFacilityDao.findByPersistentId(testFacility.getPersistentId());

		//The large set creates the initial sets of orders
		ImportResult result = importOrdersResource(foundFacility, "./resource/superset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		foundFacility = mFacilityDao.findByPersistentId(testFacility.getPersistentId());

		//The subset triggers all but one of the details to be active = false
		result = importOrdersResource(foundFacility, "./resource/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		//Simulate a cache trim between the uploads
		Ebean.getServer("codeshelf").getServerCacheManager().getCollectionIdsCache(OrderHeader.class, "orderDetails").clear();

		foundFacility = mFacilityDao.findByPersistentId(testFacility.getPersistentId());

		//Reimporting the subset again would cause class cast exception or the details would be empty and DAOException would occur because we would attempt to create an already existing detail
		result = importOrdersResource(foundFacility, "./resource/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());
		for (OrderHeader orderHeader : foundFacility.getOrderHeaders()) {
			Assert.assertNotNull(orderHeader.getOrderDetails());
		}
	}

	//******************** TESTS without Group ID ***********************

	@Test
	public final void testOutboundOrderNoGroup() throws IOException {
		Organization organization = new Organization();
		organization.setDomainId("O-ORD2.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD2.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD2.1");

		String firstCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importCsvString(facility, firstCsvString);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Import exact same file again
		importCsvString(facility, firstCsvString);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveDetails == 11);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 3);

		// Try the kinds of things we did before with group ID. Remove detail 123.4  And remove 789 altogether. (Dates are the same.)

		String secondCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importCsvString(facility, secondCsvString);
		// See, works same as if matching the group
		HeaderCounts theCounts3 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts3.mTotalHeaders == 3);
		Assert.assertTrue(theCounts3.mActiveHeaders == 2);
		Assert.assertTrue(theCounts3.mActiveDetails == 8);
		Assert.assertTrue(theCounts3.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts3.mInactiveCntrUsesOnActiveOrders == 1);
		Assert.assertTrue(theCounts3.mActiveCntrUses == 2);

		// Can a customer update a single order or detail by setting the count to zero
		String fourthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,0,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importCsvString(facility, fourthCsvString);
		// This looks buggy. Orders and 6 details still present. What two orderDetails went away. Looks like all other cntrUses got inactivated, and new one made.
		HeaderCounts theCounts4 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts4.mTotalHeaders == 3);
		Assert.assertTrue(theCounts4.mActiveHeaders == 2);
		Assert.assertTrue(theCounts4.mActiveDetails == 6);
		Assert.assertTrue(theCounts4.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts4.mInactiveCntrUsesOnActiveOrders == 4);
		Assert.assertTrue(theCounts4.mActiveCntrUses == 1);

		// So, can a customer update the count on a single item? 123.2 going to 3.
		String fifthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		importCsvString(facility, fifthCsvString);
		// Well, yes. Looks like that detail was cleanly updated without bothering anything else.
		HeaderCounts theCounts5 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts5.mTotalHeaders == 3);
		Assert.assertTrue(theCounts5.mActiveHeaders == 2);
		Assert.assertTrue(theCounts5.mActiveDetails == 6);
		Assert.assertTrue(theCounts5.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts5.mInactiveCntrUsesOnActiveOrders == 4);
		Assert.assertTrue(theCounts5.mActiveCntrUses == 1);

		// So, here is a count update for three items. Will this doesthe updates, or will this be interpreted as full new file, needing to delete others?
		String sixthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importCsvString(facility, sixthCsvString);
		// Buggy? Not sure. Hard to understand anyway. All other details except 1 were made inactive.  That led to two more inactive container uses (ok, if the detail inactive is correct).
		HeaderCounts theCounts6 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts6.mTotalHeaders == 3);
		Assert.assertTrue(theCounts6.mActiveHeaders == 2);
		Assert.assertTrue(theCounts6.mActiveDetails == 4);
		Assert.assertTrue(theCounts6.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts6.mInactiveCntrUsesOnActiveOrders == 6);
		Assert.assertTrue(theCounts6.mActiveCntrUses == 1);

	}

	// ARCHIVE TESTS?
	// The code in OutboundOrderCsvImporter.java does a lot of archiving as it reads a file.
	// By code review, the behavior is this for reading outbound orders file

	// archiveCheckAllContainers(). After a outbound order file read, iterate all containers.
	// get each containerUse.
	// for each, get associated order header if any. If it is an outbound order
	// Only if this containerUse process time is the same as this EDI process time (that is, created earlier inside of this same file read)
	// Then inactivate the containerUse. That is why inactiveCntrUses accumulate so much
	// And then for the owning container, if all its uses are inactive, then inactivate the master container. (Not sure this is wise.)

	// archiveCheckAllOrders().
	// Iterate all of the order groups. If not updated or created this order time (during this file read), then inactivate. WARNING. Outbound file read will inactivate different group from batch file group.
	// Iterate all outbound order headers. Get details. If there was a detail with this process time (created or updated during this file read), then keep the order header active. Otherwise inactivate.
	// WARNING different file for different waves may kill a lot of orders.
	// And similarly, if the detail does not match this process time, inactivate it. (Same warning).

	// archiveCheckOneOrder() If only one order was in the file (even if two or more details for the same order header) then do the archive checking only for this order.
	// That is, inactivate older details for this order. Do not do anything with containerUse.

	//******************** private helpers ***********************

	private Facility getTestFacility(String orgId, String facilityId) {
		Organization organization = new Organization();
		organization.setDomainId(orgId);
		mOrganizationDao.store(organization);

		organization.createFacility(facilityId, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(facilityId);
		return facility;
	}

	private void importCsvString(Facility facility, String csvString) throws IOException {
		byte[] firstCsvArray = csvString.getBytes();

		try (ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);) {
			InputStreamReader reader = new InputStreamReader(stream);

			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		}
	}

	private ImportResult importOrdersResource(Facility facility, String csvResource) throws IOException, InterruptedException {
		try (InputStream stream = this.getClass().getResourceAsStream(csvResource);) {
			InputStreamReader reader = new InputStreamReader(stream);
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			return importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		}
	}
}
