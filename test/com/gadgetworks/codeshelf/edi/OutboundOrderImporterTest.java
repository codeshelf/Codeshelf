/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;

/**
 * @author jeffw
 * 
 * Yes, these aren't exactly unit tests, but when they were unit tested they missed a lot of important business behaviors.
 * Sure, the coupling shouldn't be so tight, but Ebean doesn't make it easy to test it's granular behaviors.
 * 
 * While not ideal, we are testing, known, expected business behaiors against the full machinery in a memory-mapped DB
 * that runs at the speed of a unit test (and runs with the units tests).
 * 
 * There are other unit tests of EDI behaviors.
 *
 */
public class OutboundOrderImporterTest extends EdiTestABC {

	@Test
	public final void testOrderImporterFromCsvStream() {

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
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

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.1");

		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

	}

	@Test
	public final void testOrderImporterWithPickStrategyFromCsvStream() {

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

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.2", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.2");

		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.SERIAL);

		order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.PARALLEL);

	}

	@Test
	public final void testOrderImporterWithPreassignedContainerIdFromCsvStream() {

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

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.3");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.3", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.3");

		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);

		Container container = mContainerDao.findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);
	}

	@Test
	public void testFailOrderImporterFromCsvStream() {

		// There's no order due date on 123.1, so it should assert/fail to import.
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,,0"
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

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.4");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.4", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.4");

		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// We should find order 123
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

		// But not order detail item 1
		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNull(orderDetail);

		// But should find order detail item 2
		orderDetail = order.getOrderDetail("10706952");
		Assert.assertNotNull(orderDetail);

	}

	@Test
	public void testManyOrderArchive() {

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
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

		byte csvArray[] = firstOrderBatchCsv.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.5");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.5", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.5");

		// First import a big list of orders.
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte csv2Array[] = secondOrderBatchCsv.getBytes();

		stream = new ByteArrayInputStream(csv2Array);
		reader = new InputStreamReader(stream);

		// First import a big list of orders.
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// Order 789 should exist and be inactive.
		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(false, order.getActive());

		// Line item 10722222 from order 456 should be quantity 0 and inactive.
		order = facility.getOrderHeader("456");
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222")) {
				Assert.assertEquals(false, detail.getActive());
				Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());	
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());	
			}
		}
	}

	@Test
	public void testOneOrderArchive() {

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
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

		byte csvArray[] = firstOrderBatchCsv.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORD1.6");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD1.6", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD1.6");

		// First import a big list of orders.
		ICsvOrderImporter importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte csv2Array[] = secondOrderBatchCsv.getBytes();

		stream = new ByteArrayInputStream(csv2Array);
		reader = new InputStreamReader(stream);

		// First import a big list of orders.
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// Order 789 should exist and be active.
		OrderHeader order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(true, order.getActive());

		// Line item 10722222 from order 456 should be quantity 0 and inactive.
		order = facility.getOrderHeader("456");
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222")) {
				Assert.assertEquals(false, detail.getActive());
				Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());	
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());	
			}
		}
	}

}
