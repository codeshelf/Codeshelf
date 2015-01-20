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
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.service.PropertyService;
import com.gadgetworks.codeshelf.validation.BatchResult;

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
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrderImporterTest.class);

	// the full set of fields known to the bean  (in the order of the bean, just for easier verification) is
	// orderGroupId,orderId,orderDetailID,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,destinationId,pickStrategy,preAssignedContainerId,shipmentId,customerId,workSequence
	// of these: orderId,itemId,description,quantity,uom are not nullable

	private ICsvOrderImporter	importer;
	private UUID				facilityId;

	@Before
	public void initTest() {
		this.getPersistenceService().beginTenantTransaction();

		importer = createOrderImporter();
		facilityId = getTestFacility("O-" + getTestName(), "F-" + getTestName()).getPersistentId();

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
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

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderGroup orderGroup = facility.getOrderGroup("1");
		Assert.assertNotNull(orderGroup);
		Assert.assertEquals(OrderStatusEnum.RELEASED, orderGroup.getStatus());

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

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testOrderImporterWithLocationsFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String csvString = "orderId,preassignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n1,1,1.1,,,BO-PA-16,16 OZ. PAPER BOWLS,3,CS,,pick,D35,61"
				+ "\r\n1,1,1.5,,,CP-CS-16,16 oz Clear Cup,2,CS,,pick,D34,43"
				+ "\r\n1,1,1.3,,,DCL-CS-12,8-32OZ Round Deli Lid- Comp -Case 0f 499,5,CS,,pick,D34,84"
				+ "\r\n1,1,1.2,,,SP-PS-6,Spoon 6in.,4,CS,,pick,D21,"
				+ "\r\n1,1,1.4,,,ST-CS-8W,7.9in. indiv. wrapped .25in. dia.,1,CS,,pick,D28,"
				+ "\r\n2,2,2.3,,,DC-CS-16,16 oz Clear Round Deli Container,1,EA,,pick,D34,20"
				+ "\r\n2,2,2.1,,,CP-CS-2S,2 oz Souffle Cup,2,EA,,pick,D35,8"
				+ "\r\n2,2,2.2,,,KL-CS-6,6x6x3 PLA Clamshell,4,CS,,pick,D15,"
				+ "\r\n2,2,2.4,,,PLRT-SC-U9,9 Plates - Unbleached Sugar Cane Retail,3,CS,,pick,D22,"
				+ "\r\n2,2,2.5,,,TO-SC-U85-3,Unbleached 8.5 container w/ locking hin,2,CS,,pick,D9,"
				+ "\r\n3,3,3.2,,,BO-SC-U6,6 oz Unbleached Bowl,3,CS,,pick,D35,28"
				+ "\r\n3,3,3.5,,,CP-CS-7,7 oz Clear Cup,3,CS,,pick,D36,32"
				+ "\r\n3,3,3.3,,,DC-CS-16,16 oz Clear Round Deli Container,4,CS,,pick,D34,20"
				+ "\r\n3,3,3.4,,,DCL-CS-12,8-32OZ Round Deli Lid- Comp -Case 0f 499,4,CS,,pick,D34,84"
				+ "\r\n3,3,3.1,,,RK-PS-B,Corn Starch Spork, 190F,4,CS,,pick,D23,"
				+ "\r\n4,4,4.2,,,CP-CS-10,10 oz Clear Cup,1,CS,,pick,D35,92"
				+ "\r\n4,4,4.1,,,CP-CS-7,7 oz Clear Cup,2,CS,,pick,D36,32"
				+ "\r\n4,4,4.4,,,FD-MT-RT,FLOOR DISPLAY-METAL,6,CS,,pick,D3,"
				+ "\r\n4,4,4.3,,,NP-SC-DN,UNBLEACHED DINNER NAPKINS-CASE OF 2999,6,CS,,pick,D20,"
				+ "\r\n4,4,4.5,,,SB-CS-24,24 oz Salad Bowls-Ingeo-Compostable,1,CS,,pick,D6,"
				+ "\r\n5,5,5.1,,,CP-CS-16,16 oz Clear Cup,1,CS,,pick,D34,43"
				+ "\r\n5,5,5.2,,,FO-PS-R-M,Forks - Retail -Master,3,CS,,pick,D10,"
				+ "\r\n5,5,5.4,,,BO-SC-U6,6 oz Unbleached Bowl,1,CS,,pick,D35,28"
				+ "\r\n5,5,5.5,,,ST-CS-8W,7.9in. indiv. wrapped .25in. dia.,4,CS,,pick,D28,"
				+ "\r\n5,5,5.3,,,TO-SC-U9T,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D13,"
				+ "\r\n6,6,6.3,,,CP-CS-2S,2 oz Souffle Cup,3,CS,,pick,D35,8"
				+ "\r\n6,6,6.1,,,CPL-CS-12,Clear Flat Lid 10/12/16/20/24oz Cup,3,CS,,pick,D36,88"
				+ "\r\n6,6,6.2,,,FO-PS-R-M,Forks - Retail -Master,2,CS,,pick,D10,"
				+ "\r\n6,6,6.5,,,KL-CS-6,6x6x3 PLA Clamshell,1,EA,,pick,D15,"
				+ "\r\n6,6,6.4,,,SL-PA-LG,100% Post Consumer Recycled Cup Sleeves,1,CS,,pick,D30,"
				+ "\r\n7,7,7.1,,,CP-CS-10,10 oz Clear Cup,3,CS,,pick,D35,92"
				+ "\r\n7,7,7.3,,,SL-PA-LG,100% Post Consumer Recycled Cup Sleeves,2,CS,,pick,D30,"
				+ "\r\n7,7,7.3,,,KL-CS-6,6x6x3 PLA Clamshell,4,EA,,pick,D15,"
				+ "\r\n7,7,7.4,,,SP-PS-6,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n7,7,7.5,,,TO-SC-U9T,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D13,";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("1");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 5, detailCount); // 5 details for order header 1.

		//		+ "\r\n1,1,1.3,,,DCL-CS-12,8-32OZ Round Deli Lid- Comp -Case 0f 499,5,CS,,pick,D34,84"

		OrderDetail detail = order.getOrderDetail("999");
		Assert.assertNull(detail); // not this. Do not find by order.
		detail = order.getOrderDetail("1.3");
		Assert.assertNotNull(detail); // this works, find by itemId within an order.
		String detailDomainID = detail.getOrderDetailId(); // this calls through to domainID
		OrderDetail detail2 = order.getOrderDetail(detailDomainID);
		Assert.assertNotNull(detail2); // this works, find by itemId within an order.
		Assert.assertEquals(detail2, detail);
		Assert.assertEquals(detailDomainID, "1.3"); // This is the itemID from file above.

		String prefLoc = detail.getPreferredLocation();
		Assert.assertNotNull("Preferred location is undefined", prefLoc);
		Assert.assertEquals("", prefLoc); // D34 alias does not exist, so set to blank on the orderDetail

		// just looking. How many locations?
		int aisleCount = facility.getChildren().size();
		Assert.assertEquals(0, aisleCount);
		int aliasCount = facility.getLocationAliases().size();
		Assert.assertEquals(0, aliasCount);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testOrderImporterWithPickStrategyFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

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

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		// If not specified, default to serial
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategy(), PickStrategyEnum.SERIAL);

		order = facility.getOrderHeader("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategy(), PickStrategyEnum.PARALLEL);

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testOrderImporterWithPreassignedContainerIdFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

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

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
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

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public void testFailOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		// orderId,itemId,description,quantity,uom are not nullable according to the bean
		// There's no due date on first order line. nullable, ok
		// There's no itemID on third to last line. (did not matter)
		// There's no uom on second to last line. (did not matter)
		// There's no order date on last order line. nullable, ok
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
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

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public void testManyOrderArchive() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = firstOrderBatchCsv.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		// First import a big list of orders.
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csv2Array = secondOrderBatchCsv.getBytes();

		stream = new ByteArrayInputStream(csv2Array);
		reader = new InputStreamReader(stream);

		// First import a big list of orders.
		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = createOrderImporter();
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
		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public void testOneOrderArchive() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importCsvString(facility, firstOrderBatchCsv);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertEquals(3, theCounts.mTotalHeaders);
		Assert.assertEquals(3, theCounts.mActiveHeaders);
		Assert.assertEquals(3, theCounts.mActiveDetails);
		Assert.assertEquals(3, theCounts.mActiveCntrUses);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importCsvString(facility, secondOrderBatchCsv);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertEquals(3, theCounts2.mTotalHeaders);
		Assert.assertEquals(3, theCounts2.mActiveHeaders);
		Assert.assertEquals(4, theCounts2.mActiveDetails);
		Assert.assertEquals(1, theCounts2.mActiveCntrUses);
		Assert.assertEquals(0, theCounts2.mInactiveDetailsOnActiveOrders);
		Assert.assertEquals(2, theCounts2.mInactiveCntrUsesOnActiveOrders);

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
		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testMinMaxOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);

		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(Integer.valueOf(0), orderDetail.getMinQuantity());
		Assert.assertEquals(Integer.valueOf(5), orderDetail.getMaxQuantity());

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testMinMaxDefaultOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMinQuantity());
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMaxQuantity());

		// Min/Max Boundaries checking
		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,minQuantity,maxQuantity"
				+ "\r\n2,USF314,COSTCO,222,222,10700001,Item1 Description,2,each,1,3"
				+ "\r\n2,USF314,COSTCO,222,222,10700002,Item2 Description,2,each,3,3"
				+ "\r\n2,USF314,COSTCO,222,222,10700003,Item3 Description,3,each,2,2"
				+ "\r\n2,USF314,COSTCO,222,222,10700004,Item4 Description,4,each"
				+ "\r\n2,USF314,COSTCO,222,222,10700005,Item5 Description,5,each, , ";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		OrderHeader order2 = facility.getOrderHeader("222");
		// normal case with min and max supplied
		OrderDetail orderDetail700001 = order2.getOrderDetail("10700001");
		Assert.assertEquals((Integer) 1, orderDetail700001.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700001.getMaxQuantity());

		// Edi had min > quantity. Set the min to reasonable value
		OrderDetail orderDetail700002 = order2.getOrderDetail("10700002");
		Assert.assertEquals((Integer) 2, orderDetail700002.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700002.getMaxQuantity());

		// Edi had max > quantity. Set the max to reasonable value
		OrderDetail orderDetail700003 = order2.getOrderDetail("10700003");
		Assert.assertEquals((Integer) 2, orderDetail700003.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700003.getMaxQuantity());

		// Edi declared the min/max columns, but not supplied. Min and max should equal quantity.
		OrderDetail orderDetail700004 = order2.getOrderDetail("10700004");
		Assert.assertEquals((Integer) 4, orderDetail700004.getMinQuantity());
		Assert.assertEquals((Integer) 4, orderDetail700004.getMaxQuantity());

		// Edi populated the min/max fields with blank. Min and max should equal quantity.
		OrderDetail orderDetail700005 = order2.getOrderDetail("10700005");
		Assert.assertEquals((Integer) 5, orderDetail700005.getMinQuantity());
		Assert.assertEquals((Integer) 5, orderDetail700005.getMaxQuantity());

	}

	@Test
	public final void testMinMaxBoundariesCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("10700589");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMinQuantity());
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMaxQuantity());

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testDetailIdOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] firstCsvArray = firstCsvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("123.1");
		Assert.assertNotNull(orderDetail);

		order = facility.getOrderHeader("456");
		Assert.assertNotNull(order);
		Assert.assertEquals(OrderStatusEnum.RELEASED, order.getStatus());
		orderDetail = order.getOrderDetail("456.1");
		Assert.assertNotNull(orderDetail);
		orderDetail = order.getOrderDetail("456.5");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(OrderStatusEnum.RELEASED, orderDetail.getStatus());
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
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte[] secondCsvArray = secondCsvString.getBytes();
		stream = new ByteArrayInputStream(secondCsvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
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

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testReimportDetailIdOrderImporterFromCsvStream() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
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

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public void testNonsequentialOrderIds() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String nonSequentialOrders = "orderId,orderDetailId, orderDate, dueDate,itemId,description,quantity,uom,preAssignedContainerId"
				+ "\r\n243511,243511.2.01,2014-11-06 12:00:00,2014-11-06 12:00:00,CTL-SC-U3,Lids fro 8.88 x6.8 Fiber Boxes cs/400,77,CS,243511"
				+ "\r\n243534,243534.10.01,2014-11-06 12:00:00,2014-11-06 12:00:00,TR-SC-U10T,9.8X7.5 Three Compartment Trays cs/400,12,CS,243534"
				+ "\r\n243511,\"243,511.2\",2014-11-06 12:00:00,2014-11-06 12:00:00,CTL-SC-U3,Lids fro 8.88 x6.8 Fiber Boxes cs/400,23,CS,243511"
				+ "\r\n243534,\"243,534.1\",2014-11-06 12:00:00,2014-11-06 12:00:00,TR-SC-U10T,9.8X7.5 Three Compartment Trays cs/400,8,CS,243534";

		importCsvString(facility, nonSequentialOrders);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertEquals(2, theCounts.mTotalHeaders);
		Assert.assertEquals(2, theCounts.mActiveHeaders);
		Assert.assertEquals(4, theCounts.mActiveDetails);
		Assert.assertEquals(2, theCounts.mActiveCntrUses);

		this.getPersistenceService().commitTenantTransaction();
	}

	/**
	 * Simulates the edi process for order importing
	 */
	@Test
	public void testMultipleImportOfLargeSet() throws IOException, InterruptedException {
		this.getPersistenceService().beginTenantTransaction();
		//The edi mechanism finds the facility from DAO before entering the importers
		Facility foundFacility = null;
		foundFacility = mFacilityDao.findByPersistentId(facilityId);

		//The large set creates the initial sets of orders
		BatchResult<?> result = importOrdersResource(foundFacility, "./resource/superset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		foundFacility = mFacilityDao.findByPersistentId(facilityId);

		//The subset triggers all but one of the details to be active = false
		result = importOrdersResource(foundFacility, "./resource/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		//Simulate a cache trim between the uploads
		// Ebean.getServer("codeshelf").getServerCacheManager().getCollectionIdsCache(OrderHeader.class, "orderDetails").clear();

		foundFacility = mFacilityDao.findByPersistentId(facilityId);

		//Reimporting the subset again would cause class cast exception or the details would be empty and DAOException would occur because we would attempt to create an already existing detail
		result = importOrdersResource(foundFacility, "./resource/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());
		for (OrderHeader orderHeader : foundFacility.getOrderHeaders()) {
			Assert.assertNotNull(orderHeader.getOrderDetails());
		}
		this.getPersistenceService().commitTenantTransaction();

	}

	//******************** TESTS without Group ID ***********************

	@Test
	public final void testImportEmptyQuantityAsZero() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacility();
		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis() - 30000);

		String withOneEmptyQuantity = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,\"\",each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		BatchResult<Object> results = importCsvString(facility, withOneEmptyQuantity, firstEdiProcessTime);
		List<OrderDetail> orderDetails = OrderDetail.DAO.getAll();
		Assert.assertEquals(1, orderDetails.size());
		Assert.assertTrue(results.isSuccessful());
		Assert.assertEquals(0, orderDetails.get(0).getQuantity().intValue());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testImportAlphaQuantityAsZero() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacility();
		Timestamp firstEdiProcessTime = new Timestamp(System.currentTimeMillis() - 30000);

		String withOneEmptyQuantity = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,\"A\",each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		BatchResult<Object> results = importCsvString(facility, withOneEmptyQuantity, firstEdiProcessTime);
		List<OrderDetail> orderDetails = OrderDetail.DAO.getAll();
		Assert.assertEquals(1, orderDetails.size());
		Assert.assertTrue(results.isSuccessful());
		Assert.assertEquals(0, orderDetails.get(0).getQuantity().intValue());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testReimportOutboundOrderNoGroup() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setDomainId("O-ORD2.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORD2.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORD2.1");

		String firstCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
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
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
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
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������o Stuffed Olives,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
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

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testItemCreationfromOrders() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("1: Read a very small aisles and locations file. Aliases exist only for D34, D35, D13");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		loadSmallAislesAndLocations(facility);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("3: Read the orders file, which has some preferred locations");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D35,61"
				+ "\r\n10,10,10.2,SKU0002,16 oz Clear Cup,2,CS,,pick,D34,43"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D13,";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);

		OrderHeader order10 = facility.getOrderHeader("10");
		Assert.assertNotNull(order10);
		Integer detailCount = order10.getOrderDetails().size();
		Assert.assertEquals((Integer) 2, detailCount);

		OrderDetail detail101 = order10.getOrderDetail("10.1");
		Assert.assertNotNull(detail101);
		OrderDetail detail102 = order10.getOrderDetail("10.2");
		Assert.assertNotNull(detail102);

		String prefLoc101 = detail101.getPreferredLocation();
		Assert.assertNotNull("Preferred location is undefined", prefLoc101);
		Assert.assertEquals("D35", prefLoc101);

		String prefLoc102 = detail102.getPreferredLocation();
		Assert.assertNotNull("Preferred location is undefined", prefLoc102);
		Assert.assertEquals("D34", prefLoc102);

		OrderHeader order11 = facility.getOrderHeader("11");
		OrderDetail detail111 = order11.getOrderDetail("11.1");
		String prefLoc111 = detail111.getPreferredLocation();
		Assert.assertTrue("unresolved preferredLocation should be blank", prefLoc111.isEmpty());

		OrderDetail detail112 = order11.getOrderDetail("11.2");
		String prefLoc112 = detail112.getPreferredLocation();
		Assert.assertEquals("D13", prefLoc112);

		ItemMaster theMaster = facility.getItemMaster("SKU0001");
		Assert.assertNotNull("ItemMaster should be created", theMaster);
		List<Item> items = theMaster.getItems();
		Assert.assertEquals(0, items.size()); // No inventory created

		// Now set up for next case. 
		DomainObjectProperty theProperty = PropertyService.getPropertyObject(facility, DomainObjectProperty.LOCAPICK);
		if (theProperty != null) {
			theProperty.setValue(true);
			PropertyDao.getInstance().store(theProperty);
		}
		this.getPersistenceService().commitTenantTransaction();

		// Read the same file again, but this time LOCAPICK is true
		this.getPersistenceService().beginTenantTransaction();

		// getting a lazy initialization error during the read on a tier addStoredItem.
		// Interesting that relaod the facility seems to set the tier straight.
		facility = Facility.DAO.reload(facility);

		ByteArrayInputStream stream4 = new ByteArrayInputStream(csvArray);
		InputStreamReader reader4 = new InputStreamReader(stream4);
		Timestamp ediProcessTime4 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader4, facility, ediProcessTime4);

		ItemMaster theMaster2 = facility.getItemMaster("SKU0001");
		Assert.assertNotNull("ItemMaster should be found", theMaster2);
		List<Item> items2 = theMaster2.getItems();
		Assert.assertEquals(1, items2.size()); // Inventory should be created since LOCAPICK is true

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void itemCreationChoicesfromOrders() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("1: Read a very small aisles and locations file");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		loadSmallAislesAndLocations(facility);

		LOGGER.info("2: Set LOCAPICK = true");
		DomainObjectProperty theProperty = PropertyService.getPropertyObject(facility, DomainObjectProperty.LOCAPICK);
		if (theProperty != null) {
			theProperty.setValue(true);
			PropertyDao.getInstance().store(theProperty);
		}
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("3: Read the orders file, which has some preferred locations");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		String csvString1 = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D35,61"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D13,";

		Timestamp ediProcessTime1 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(new StringReader(csvString1), facility, ediProcessTime1);

		LOGGER.info("4: Check that we got item locations for SKU0001, and SKU0004, but not SKU0003 which had unknown alias location");
		ItemMaster master1 = facility.getItemMaster("SKU0001");
		Assert.assertNotNull(master1);
		List<Item> items1 = master1.getItems();
		Assert.assertEquals(1, items1.size()); // Inventory should be created since LOCAPICK is true

		ItemMaster master3 = facility.getItemMaster("SKU0003");
		List<Item> items3 = master3.getItems();
		Assert.assertEquals(0, items3.size());

		ItemMaster master4 = facility.getItemMaster("SKU0004");
		List<Item> items4 = master4.getItems();
		Assert.assertEquals(1, items4.size());
		
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("5: Read a revised orders file. This is testing two things");
		LOGGER.info("   Change preferredLocation for a case order detail. Should create new inventory for SKU0001, but delete old.");
		LOGGER.info("   Change preferredLocation for an each order detail. Should move and not create new inventory for SKU0004");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		// Checking precondition, and remembering the persistentId of the items.
		master1 = facility.getItemMaster("SKU0001");
		List<Item> itemsList1a = master1.getItemsOfUom("CS");
		Assert.assertEquals(1, itemsList1a.size());
		Assert.assertEquals(1, master1.getItems().size());	
		Item items1a = itemsList1a.get(0);
		UUID persist1a = items1a.getPersistentId();
		master4 = facility.getItemMaster("SKU0004");
		List<Item> itemsList4a = master4.getItemsOfUom("EA");
		Assert.assertEquals(1, itemsList4a.size());
		Item items4a = itemsList4a.get(0);
		UUID persist4a = items4a.getPersistentId();

		// Now import again, but SKU0001 and SKU0004 orders both got different locations
		String csvString2 = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(new StringReader(csvString2), facility, ediProcessTime2);

		LOGGER.info("6: Check that we got new item locations for SKU0001, and moved the old one for SKU0004, ");
		master1 = facility.getItemMaster("SKU0001");
		items1 = master1.getItems();
		
		Assert.assertEquals(1, items1.size()); // Should have created new, and deleted the old
		Item items1b = items1.get(0);
		UUID persist1b = items1b.getPersistentId();

		master4 = facility.getItemMaster("SKU0004");
		items4 = master4.getItems();
		Assert.assertEquals(1, items4.size());
		Item items4b = items4.get(0);
		UUID persist4b = items4b.getPersistentId();
		
		Assert.assertNotEquals(persist1a, persist1b); // made new. Deleted old. So different persistent Id
		Assert.assertEquals(persist4a, persist4b);  // just moved the EA item since EACHMULT is false.		
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("7: Move the 10.1 SKU0001 order at D34. But add another SKU0001 order at D34. SKU0001 hould have two itemLocations after that.");
		// Just for thoroughness, changed the cm Offset of the D34 item. Does not matter.
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		
		String csvString3 = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D35,70"
				+ "\r\n12,12,12.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,50"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";

		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(new StringReader(csvString3), facility, ediProcessTime3);

		master1 = facility.getItemMaster("SKU0001");
		items1 = master1.getItems();
		Assert.assertEquals(2, items1.size());

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("8: Showing the limits of current implementation. Next day, orders for the same SKUs in different locations. Does not clean up old inventory.");
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		
		String csvString4 = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n14,14,14.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D13,"
				+ "\r\n14,14,14.2,SKU0003,Spoon 6in.,1,CS,,pick,D21,";

		Timestamp ediProcessTime4 = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(new StringReader(csvString4), facility, ediProcessTime4);

		master1 = facility.getItemMaster("SKU0001");
		items1 = master1.getItems();
		Assert.assertEquals(3, items1.size());

		this.getPersistenceService().commitTenantTransaction();
		
		// Not shown. But now if we moved the 14.1 SKU0001, that one would delete, after the make of the new CS itemLocation, but the old two would remain.
		// At some point we will need an archive mechanism.

	}

	// ORDER ARCHIVE TESTS?
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
	
	/**
	 * This test verifies that when importing orders for an existing group, all orders previously in that group get archived unless they've been updated
	 */
	@Test
	public final void testArchiveOneGroup() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		
		
		String firstCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1" + 
				"\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1" +
				"\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1";
		importCsvString(facility, firstCsvString);

		String secondCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1" + 
				"\r\n4,4,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1";
		importCsvString(facility, secondCsvString);
		
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", false);
		headerExpectations.put("2", false);
		headerExpectations.put("3", true);
		headerExpectations.put("4", true);
		assertArchiveStatuses(groupExpectations, headerExpectations);

		this.getPersistenceService().commitTenantTransaction();
	}

	/**
	 * Same as testAcriveTestOneGroup, but without specifying a group
	 */
	@Test
	public final void testArchiveNoGroup() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		
		
		String firstCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom" + 
				"\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a" + 
				"\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a" +
				"\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a";
		importCsvString(facility, firstCsvString);

		String secondCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom" + 
				"\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a" + 
				"\r\n4,4,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a";
		importCsvString(facility, secondCsvString);
		
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", false);
		headerExpectations.put("2", false);
		headerExpectations.put("3", true);
		headerExpectations.put("4", true);
		assertArchiveStatuses(groupExpectations, headerExpectations);

		this.getPersistenceService().commitTenantTransaction();
	}
	
	/**
	 * This is a test for my modification of the Archiving behavior.
	 * Currently, importing orders will archive all older orders, even in other groups.
	 * In the new behavior, only old orders from the newly imported groups will be archived
	 */
	@Test
	public final void testArchiveTwoGroups() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		
		
		String firstCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1" + 
				"\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1" +
				"\r\n3,3,347,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group2";
		importCsvString(facility, firstCsvString);

		String secondCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" +  
				"\r\n4,4,349,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2" +
				"\r\n5,5,350,12/03/14 12:00,12/31/14 12:00,Item8,,50,a,Group2";
		importCsvString(facility, secondCsvString);
		
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", true);
		groupExpectations.put("Group2", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", true);
		headerExpectations.put("2", true);
		headerExpectations.put("3", false);
		headerExpectations.put("4", true);
		headerExpectations.put("5", true);
		assertArchiveStatuses(groupExpectations, headerExpectations);
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void testArchiveAbandonedGroup() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		
		LOGGER.info("1: Read tiny orders for for group 1, 2 orders with one detail each.");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1" + 
				"\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1";
		importCsvString(facility, firstCsvString);
		
		OrderHeader header1a = facility.getOrderHeader("1");
		OrderGroup group1a = header1a.getOrderGroup();
		
		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		Facility.DAO.reload(facility);

		LOGGER.info("2: As if the orders were pushed to later group, same orders except for group2.");
		String secondCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group2" + 
				"\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2";
		importCsvString(facility, secondCsvString);
		
		OrderHeader header1b = facility.getOrderHeader("1");
		OrderGroup group1b = header1b.getOrderGroup();
		OrderGroup group2 = facility.getOrderGroup("Group2");
		// Is it the same header?
		Assert.assertEquals(header1a, header1b);
		// did it change owner?
		Assert.assertNotEquals(group1a, group1b);
		Assert.assertEquals(group1b, group2); // just proving what we expect

		LOGGER.info("3: We expect to find that Group1 is inactive. Group2 is active with its orders.");
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", false);
		groupExpectations.put("Group2", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", true);
		headerExpectations.put("2", true);
		headerExpectations.put("99", true); // just showing  the limit of the test function. Order 99 does not exist. No error reported.
		assertArchiveStatuses(groupExpectations, headerExpectations);
		
		LOGGER.info("  We showed that the orders are the same objects. They changed owner groups.");
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void demonstrateArchiveQuirk() throws IOException {
		/* This simulates doing some of the work in the morning wave, then completing some of the orders in the afternoon wave.
		 * The corresponding real test would be to drop orders file similar to the first. Set up the cart for the orders. Run, shorting the jobs that are not completed, 
		 * (or don't set up the cart at all for orders that have no lines that need to be completed.)
		 * and completing work instructions for the ones that are completed.
		 * Then drop the second orders file.
		 */
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		
		LOGGER.info("1: Read orders for for group 1, as if it is morning wave");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,1.1,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1" + 
				"\r\n1,1,1.2,12/03/14 12:00,12/31/14 12:00,Item16,,90,a,Group1" + 
				"\r\n2,2,2.1,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1" +
				"\r\n3,3,3.1,12/03/14 12:00,12/31/14 12:00,Item9,,100,a,Group1";
		importCsvString(facility, firstCsvString);
		
		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		Facility.DAO.reload(facility);

		OrderHeader header1a = facility.getOrderHeader("1");
		OrderDetail detail1_1a = header1a.getOrderDetail("Item15");
		Assert.assertNotNull(detail1_1a); // Notice: orderDetail name is thrown away! Using the SKU name
		
		OrderDetail detail1_2a = header1a.getOrderDetail("Item16");
		Assert.assertNotNull(detail1_2a);
		OrderHeader header2a = facility.getOrderHeader("2");
		Assert.assertNotNull(header2a);
		OrderHeader header3a = facility.getOrderHeader("3");
		OrderDetail detail3_1a = header3a.getOrderDetail("Item9");
		
		LOGGER.info("2: Partially complete order 1. Fully complete order 3. Leave order 2 uncompleted.");
		detail1_1a.setStatus(OrderStatusEnum.COMPLETE);
		detail3_1a.setStatus(OrderStatusEnum.COMPLETE);
		header3a.setStatus(OrderStatusEnum.COMPLETE);

		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		Facility.DAO.reload(facility);

		LOGGER.info("3: As if remaining work is reassigned to a later wave, push what is left to group2.");
		String secondCsvString = "orderId,preAssignedContainerId,orderDetail,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId" + 
				"\r\n1,1,1.2,12/03/14 12:00,12/31/14 12:00,Item16,,90,a,Group2" + 
				"\r\n2,2,2.1,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2";
		importCsvString(facility, secondCsvString);
		
		OrderGroup orderGroup1b = facility.getOrderGroup("Group1");
		OrderGroup orderGroup2b = facility.getOrderGroup("Group2");
		OrderHeader header1b = facility.getOrderHeader("1");
		OrderDetail detail1_1b = header1b.getOrderDetail("Item15");
		Assert.assertNotNull(detail1_1b);
		OrderDetail detail1_2b = header1b.getOrderDetail("Item16");
		Assert.assertNotNull(detail1_2b);
		OrderHeader header2b = facility.getOrderHeader("2");
		Assert.assertNotNull(header2b);
		OrderHeader header3b = facility.getOrderHeader("3");
		OrderDetail detail3_1b = header3b.getOrderDetail("Item9");
		Assert.assertNotNull(detail3_1b);

		LOGGER.info("3: Let's see what archived, and see if anything seems wrong.");
		LOGGER.info("   Group 1 still active.");
		Assert.assertTrue(orderGroup1b.getActive());
		Assert.assertTrue(orderGroup2b.getActive());
		LOGGER.info("   Order1  and 2 still active.");
		Assert.assertTrue(header1b.getActive());
		Assert.assertTrue(header2b.getActive());
				
		LOGGER.info("   Quirk: Order3 still active even though it is fully completed and was not represented in last file drop.");
		// Not sure we should "fix". Just understand. Order3 is still completed.
		Assert.assertTrue(header3b.getActive());
		OrderGroup header1Owner = header1b.getOrderGroup();
		Assert.assertEquals(orderGroup2b, header1Owner);
		LOGGER.info("   Quirk: Order1 changed owner to group2. Correct obviously for uncomplete 1.2 detail, but misleading for the 1.1 detail");
		// Productivity reporting may have to look out for this. Or write a separate record at the time of detail completion, recording
		// the order group at the time of completion.
		LOGGER.info("   As expected, completed order 3 remains on group 1.");
		Assert.assertEquals(orderGroup1b, header3b.getOrderGroup());
			
		this.getPersistenceService().commitTenantTransaction();
	}


	/**
	 * This is not generally useful. It gets absolutely all orders and groups, not even limiting to the facility.
	 * Then assumes that all are represented in the groupExpectations and headerExpectations and complains if one is found not in expectations. You cannot chain imports and only check the results for
	 * some of the headers and groups. This can only be used for extremely small tests. It would be generally useful if it iterated through the expectations,
	 * then found the groups and headers in the facility to check.
	 */
	private void assertArchiveStatuses(HashMap<String, Boolean> groupExpectations, HashMap<String, Boolean> headerExpectations) {
		List<OrderGroup> groups = OrderGroup.DAO.getAll();
		for (OrderGroup group : groups) {
			assertArchiveStatusesHelper(groupExpectations, group.getDomainId(), group.getActive(), "group");
		}
		List<OrderHeader> headers = OrderHeader.DAO.getAll();
		for (OrderHeader header : headers) {
			assertArchiveStatusesHelper(headerExpectations, header.getDomainId(), header.getActive(), "order header");
		}		
	}
	
	private void assertArchiveStatusesHelper(HashMap<String, Boolean> expectations, String domainId, Boolean active, String examinedLevel) {
		System.out.println(examinedLevel + " " + domainId + " " + active);
		Boolean expected = expectations.get(domainId);
		Assert.assertNotNull("Encountered an unexpected " + examinedLevel + " " + domainId, expected);
		String message = String.format("Expected %s '%s' to be %s; instead was %s.", examinedLevel, domainId, expected?"active":"inactive", active?"active":"inactive");
		Assert.assertEquals(message, expected, active);
	}
	//******************** private helpers ***********************

	private Facility getTestFacility(String orgId, String facilityId) {
		Organization organization = new Organization();
		organization.setDomainId(orgId);
		mOrganizationDao.store(organization);

		organization.createFacility(facilityId, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(facilityId);
		return facility;
	}

	private void loadSmallAislesAndLocations(Facility inFacility) {
		String csvString1 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierNotB1S1Side,12.0,37.0,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,1,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,1,1,0,,\r\n"//
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.0,43.0,X,120,\r\n" + "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,1,0,,\r\n"; //

		Timestamp ediProcessTime1 = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer1 = createAisleFileImporter();
		importer1.importAislesFileFromCsvStream(new StringReader(csvString1), inFacility, ediProcessTime1);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D34\r\n" //
				+ "A1.B2, D35\r\n" //
				+ "A2.B1, D13\r\n"; //
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(new StringReader(csvString2), inFacility, ediProcessTime2);
	}

	private BatchResult<Object> importCsvString(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		return importCsvString(facility, csvString, ediProcessTime);
	}

	private BatchResult<Object> importCsvString(Facility facility, String csvString, Timestamp ediProcessTime) throws IOException {
		BatchResult<Object> results = importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return results;
	}

	private BatchResult<?> importOrdersResource(Facility facility, String csvResource) throws IOException, InterruptedException {
		try (InputStream stream = this.getClass().getResourceAsStream(csvResource);) {
			InputStreamReader reader = new InputStreamReader(stream);
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			return importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		}
	}
}
