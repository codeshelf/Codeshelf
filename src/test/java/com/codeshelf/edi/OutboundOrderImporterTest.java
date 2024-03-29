/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.HeaderCounts;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.validation.BatchResult;

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
public class OutboundOrderImporterTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrderImporterTest.class);

	// the full set of fields known to the bean  (in the order of the bean, just for easier verification) is
	// orderGroupId,orderId,orderDetailID,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,destinationId,pickStrategy,preAssignedContainerId,shipmentId,customerId,workSequence
	// of these: orderId,itemId,description,quantity,uom are not nullable

	private UUID				facilityId;

	@Before
	public void doBefore() {
		super.doBefore();

		beginTransaction();

		facilityId = getTestFacility("O-" + getTestName(), "F-" + getTestName()).getPersistentId();

		commitTransaction();
	}

	@Test
	public void persistDataReceipt() {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		ICsvOrderImporter subject = createOrderImporter();
		BatchResult<?> batchResult = new BatchResult<Object>();
		batchResult.setCompleted(new Date());
		batchResult.setReceived(new Date());
		subject.persistDataReceipt(facility,
			"testUser",
			"testfilename",
			System.currentTimeMillis(),
			EdiTransportType.OTHER,
			batchResult);
		commitTransaction();
	}

	@Test
	public void testIntegerConversion() {
		ICsvOrderImporter importer3 = createOrderImporter();
		Assert.assertEquals(0, importer3.toInteger("0"));
		Assert.assertEquals(0, importer3.toInteger("000"));
		Assert.assertEquals(0, importer3.toInteger(" 0"));
		Assert.assertEquals(0, importer3.toInteger(" 00 "));
		Assert.assertEquals(3, importer3.toInteger(" 3 "));
		Assert.assertEquals(3, importer3.toInteger(" 003 "));
	}

	@Test
	public final void testOrderImporterFromCsvStream() throws IOException, ParseException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,120,931,10706962,Sun Ripened Dried Tomato Pesto 24oz,1,each,2012-09-26 11:31:01,,0";
		importOrdersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		OrderGroup orderGroup = facility.getOrderGroup("1");
		Assert.assertNotNull(orderGroup);
		Assert.assertEquals(OrderStatusEnum.RELEASED, orderGroup.getStatus());

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount); // 4 details for order header 123. They would get the default name

		OrderHeader order931 = OrderHeader.staticGetDao().findByDomainId(facility, "931");
		Assert.assertNotNull(order931);
		Integer detail931Count = order931.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detail931Count); // 4 details for order header 123. They would get the default name

		OrderDetail detail931 = order931.getOrderDetail("931");
		Assert.assertNull(detail931); // not this. Do not find by order.
		detail931 = order931.getOrderDetail("10706962-each");
		Assert.assertNotNull(detail931); // this works, find by itemId within an order.
		String detail931DomainID = detail931.getOrderDetailId(); // this calls through to domainID
		OrderDetail detail931b = order931.getOrderDetail(detail931DomainID);
		Assert.assertNotNull(detail931b); // this works, find by itemId within an order.
		Assert.assertEquals(detail931b, detail931);
		Assert.assertEquals(detail931DomainID, "10706962-each"); // This is the itemID from file above.

		String expectedDateStr = OutboundOrderCsvBean.getDefaultDueDate();
		SimpleDateFormat parserSDF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date expectedDate = parserSDF.parse(expectedDateStr);
		Timestamp dueDate931 = order931.getDueDate();
		Assert.assertEquals(expectedDate.getTime(), dueDate931.getTime()); //Verify the auto-filled due date

		commitTransaction();
	}

	@Test
	public final void testOrderImporterWithLocationsFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderId,preassignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin,type,locationId,cmFromLeft"
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
		importOrdersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "1");

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
		Assert.assertEquals("D34", prefLoc); // D34 alias does not exist, but still preferred location should come through (from V12)

		// just looking. How many locations?
		int aisleCount = facility.getChildren().size();
		Assert.assertEquals(0, aisleCount);
		List<LocationAlias> las = LocationAlias.staticGetDao().findByParent(facility);
		int aliasCount = las.size();
		Assert.assertEquals(0, aliasCount);

		commitTransaction();
	}

	@Test
	public final void testOrderImporterWithPickStrategyFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

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
		importOrdersData(facility, csvString);

		// If not specified, default to serial
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategy(), PickStrategyEnum.SERIAL);

		order = OrderHeader.staticGetDao().findByDomainId(facility, "789");

		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategy(), PickStrategyEnum.PARALLEL);

		commitTransaction();

	}

	@Test
	public final void testOrderImporterWithShipperIdFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipperId,orderId,itemId,description,quantity,uom,orderDate, dueDate\r\n" //
				+ "1,TRUCKA,123,3001,Widget,100,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,FEDEX,456,4550,Gadget,450,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,789,3007,Dealybob,300,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n";
		importOrdersData(facility, csvString);

		OrderHeader o123 = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderHeader o456 = OrderHeader.staticGetDao().findByDomainId(facility, "456");
		OrderHeader o789 = OrderHeader.staticGetDao().findByDomainId(facility, "789");
		Assert.assertEquals("TRUCKA", o123.getShipperId());
		Assert.assertEquals("FEDEX", o456.getShipperId());
		//not required
		Assert.assertEquals("", o789.getShipperId());

		commitTransaction();

	}

	@Test
	public final void testOrderImporterWithPreassignedContainerIdFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

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
		importOrdersData(facility, csvString);

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "789");

		Assert.assertNotNull(order);

		Container container = Container.staticGetDao().findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 1);

		commitTransaction();

	}

	@Test
	public void testFailOrderImporterFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		// orderId,itemId,description,quantity,uom are not nullable according to the bean
		// There's no due date on first order line. nullable, ok
		// There's no itemID on third to last line. (now matters)
		// There's no uom on second to last line. (now matters)
		// There's no order date on last order line. nullable, ok
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,,2012-09-26 11:31:02,0";
		importOrdersData(facility, csvString);

		// We should find order 123
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);

		// Also find order detail item 1. Used to not because there is no due date, but now that field is nullable in the schema.
		OrderDetail orderDetail = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(orderDetail);

		// But should find order detail item 2
		orderDetail = order.getOrderDetail("10706952-each");
		Assert.assertNotNull(orderDetail);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertEquals(3, theCounts.mTotalHeaders);
		Assert.assertEquals(3, theCounts.mActiveHeaders);
		Assert.assertEquals(9, theCounts.mActiveDetails);
		Assert.assertEquals(3, theCounts.mActiveCntrUses);

		commitTransaction();

	}

	@Test
	public void testManyOrderArchive() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeno Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalapeno Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, firstOrderBatchCsv);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);
		commitTransaction();

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeno Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalapeno Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, secondOrderBatchCsv);

		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 7);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 2);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);

		// Order 789 should exist and not be inactive.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "789");

		Assert.assertNotNull(order);
		Assert.assertEquals(false, order.getActive());

		// Line item 10722222 from order 456 should be inactive.
		order = OrderHeader.staticGetDao().findByDomainId(facility, "456");
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222-each")) {
				Assert.assertEquals(false, detail.getActive());
				//Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());
			}
		}
		commitTransaction();
	}

	@Test
	public void testOneOrderArchive() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String firstOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10711111,Napa Valley Bistro Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, firstOrderBatchCsv);
		commitTransaction();

		beginTransaction();
		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertEquals(3, theCounts.mTotalHeaders);
		Assert.assertEquals(3, theCounts.mActiveHeaders);
		Assert.assertEquals(3, theCounts.mActiveDetails);
		Assert.assertEquals(3, theCounts.mActiveCntrUses);

		// Now import a smaller list of orders, but more than one.
		String secondOrderBatchCsv = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, secondOrderBatchCsv);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertEquals(theCounts2.mTotalHeaders, 3);
		Assert.assertEquals(theCounts2.mActiveHeaders, 1);
		Assert.assertEquals(theCounts2.mActiveDetails, 2);
		Assert.assertEquals(theCounts2.mActiveCntrUses, 1);
		Assert.assertEquals(theCounts2.mInactiveDetailsOnActiveOrders, 0);
		Assert.assertEquals(theCounts2.mInactiveCntrUsesOnActiveOrders, 0);

		// Order 789 should exist and be active.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "789");

		Assert.assertNotNull(order);
		Assert.assertEquals(false, order.getActive());

		// Line item 10722222 from order 456 should be inactive.
		order = OrderHeader.staticGetDao().findByDomainId(facility, "456");

		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getOrderDetailId().equals("10722222")) {
				Assert.assertEquals(false, detail.getActive());
				//Assert.assertEquals(Integer.valueOf(0), detail.getQuantity());
			} else {
				Assert.assertEquals(true, detail.getActive());
				Assert.assertNotEquals(Integer.valueOf(0), detail.getQuantity());
			}
		}
		commitTransaction();
	}

	@Test
	public final void testMinMaxOrderImporterFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,	1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,				1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,							1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,			1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,					1,0,5,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, csvString);

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		Assert.assertNotNull(order);

		OrderDetail orderDetail = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(Integer.valueOf(0), orderDetail.getMinQuantity());
		Assert.assertEquals(Integer.valueOf(5), orderDetail.getMaxQuantity());

		commitTransaction();

	}

	@Test
	public final void testMinMaxDefaultOrderImporterFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, csvString);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("10700589-each");
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

		importOrdersData(facility, csvString2);

		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "222");

		// normal case with min and max supplied
		OrderDetail orderDetail700001 = order2.getOrderDetail("10700001-each");
		Assert.assertEquals((Integer) 1, orderDetail700001.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700001.getMaxQuantity());

		// Edi had min > quantity. Set the min to reasonable value
		OrderDetail orderDetail700002 = order2.getOrderDetail("10700002-each");
		Assert.assertEquals((Integer) 2, orderDetail700002.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700002.getMaxQuantity());

		// Edi had max > quantity. Set the max to reasonable value
		OrderDetail orderDetail700003 = order2.getOrderDetail("10700003-each");
		Assert.assertEquals((Integer) 2, orderDetail700003.getMinQuantity());
		Assert.assertEquals((Integer) 3, orderDetail700003.getMaxQuantity());

		// Edi declared the min/max columns, but not supplied. Min and max should equal quantity.
		OrderDetail orderDetail700004 = order2.getOrderDetail("10700004-each");
		Assert.assertEquals((Integer) 4, orderDetail700004.getMinQuantity());
		Assert.assertEquals((Integer) 4, orderDetail700004.getMaxQuantity());

		// Edi populated the min/max fields with blank. Min and max should equal quantity.
		OrderDetail orderDetail700005 = order2.getOrderDetail("10700005-each");
		Assert.assertEquals((Integer) 5, orderDetail700005.getMinQuantity());
		Assert.assertEquals((Integer) 5, orderDetail700005.getMaxQuantity());

		commitTransaction();
	}

	@Test
	public final void testMinMaxBoundariesCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, csvString);

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMinQuantity());
		Assert.assertEquals(orderDetail.getQuantity(), orderDetail.getMaxQuantity());

		commitTransaction();

	}

	@Test
	public final void testDetailIdOrderImporterFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("123.1");
		Assert.assertNotNull(orderDetail);

		order = OrderHeader.staticGetDao().findByDomainId(facility, "456");

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
		orderDetail = order.getOrderDetail("10706962-each"); // item ID
		Assert.assertNull(orderDetail);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);
		commitTransaction();

		// This is a very odd test. Above had one set of headers, and this a different set. Could happen for different customers maybe, but here same customer and same orders.
		// So, what happens to the details? 
		// The answer through v24 was the old details all updated with the new ids. In this case, the new IDs are the "item-uom" pairs
		// But DEV-1323 gives precedence to the order detail ID. No detail ID field in this file, so we get the default ID as "10700589-each"
		// It will not match the previous "item-uom" pair because the detail ID is different.
		String secondCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape�����������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape�����������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		beginTransaction();
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		// check data
		beginTransaction();
		facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		//123.1 should no longer exist. Instead, there should be 10700589-each
		order = OrderHeader.staticGetDao().findByDomainId(facility, "123");

		Assert.assertNotNull(order);
		orderDetail = order.getOrderDetail("123.1");
		// prior to DEV-1323, the detail was not found because it got renamed.
		Assert.assertNotNull(orderDetail);
		Assert.assertFalse(orderDetail.getActive()); // but not active because not represented in this file with this order ID.
		orderDetail = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(orderDetail);
		Assert.assertTrue(orderDetail.getActive());

		// Find inactive 456.1 and new active 10711111 and 10722222
		order = OrderHeader.staticGetDao().findByDomainId(facility, "456");

		Assert.assertNotNull(order);
		orderDetail = order.getOrderDetail("456.1");
		Assert.assertNotNull(orderDetail);
		Assert.assertFalse(orderDetail.getActive()); // but not active because not represented in this file with this order ID.
		orderDetail = order.getOrderDetail("10711111-each");
		Assert.assertNotNull(orderDetail);
		orderDetail = order.getOrderDetail("10722222-each");
		Assert.assertNotNull(orderDetail);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 2);
		Assert.assertTrue(theCounts2.mActiveDetails == 8);
		Assert.assertEquals(9, theCounts2.mInactiveDetailsOnActiveOrders); // Used to be 1 before the DEV-1323 changes
		Assert.assertTrue(theCounts2.mActiveCntrUses == 2);

		commitTransaction();
	}

	@Test
	public final void testReimportDetailIdOrderImporterFromCsvStream() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String firstCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, firstCsvString);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);

		// Import exact same file again
		importOrdersData(facility, firstCsvString);

		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveDetails == 11);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 3);

		commitTransaction();

	}

	@Test
	public void testNonsequentialOrderIds() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String nonSequentialOrders = "orderId,orderDetailId, orderDate, dueDate,itemId,description,quantity,uom,preAssignedContainerId"
				+ "\r\n243511,243511.2.01,2014-11-06 12:00:00,2014-11-06 12:00:00,CTL-SC-U3(a),Lids fro 8.88 x6.8 Fiber Boxes cs/400,77,CS,243511"
				+ "\r\n243534,243534.10.01,2014-11-06 12:00:00,2014-11-06 12:00:00,TR-SC-U10T(a),9.8X7.5 Three Compartment Trays cs/400,12,CS,243534"
				+ "\r\n243511,\"243,511.2\",2014-11-06 12:00:00,2014-11-06 12:00:00,CTL-SC-U3(b),Lids fro 8.88 x6.8 Fiber Boxes cs/400,23,CS,243511"
				+ "\r\n243534,\"243,534.1\",2014-11-06 12:00:00,2014-11-06 12:00:00,TR-SC-U10T(b),9.8X7.5 Three Compartment Trays cs/400,8,CS,243534";
		importOrdersData(facility, nonSequentialOrders);

		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertEquals(2, theCounts.mTotalHeaders);
		Assert.assertEquals(2, theCounts.mActiveHeaders);
		Assert.assertEquals(4, theCounts.mActiveDetails);
		Assert.assertEquals(2, theCounts.mActiveCntrUses);

		commitTransaction();
	}

	/**
	 * Simulates the edi process for order importing
	 */
	@Test
	public void testMultipleImportOfLargeSet() throws IOException, InterruptedException {
		beginTransaction();
		//The edi mechanism finds the facility from DAO before entering the importers
		Facility foundFacility = null;
		foundFacility = Facility.staticGetDao().findByPersistentId(facilityId);

		//The large set creates the initial sets of orders
		BatchResult<?> result = importOrdersResource(foundFacility, "largeset/superset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		foundFacility = Facility.staticGetDao().findByPersistentId(facilityId);

		//The subset triggers all but one of the details to be active = false
		result = importOrdersResource(foundFacility, "largeset/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());

		//Simulate a cache trim between the uploads
		// Ebean.getServer("codeshelf").getServerCacheManager().getCollectionIdsCache(OrderHeader.class, "orderDetails").clear();

		foundFacility = Facility.staticGetDao().findByPersistentId(facilityId);

		//Reimporting the subset again would cause class cast exception or the details would be empty and DAOException would occur because we would attempt to create an already existing detail
		result = importOrdersResource(foundFacility, "largeset/subset.orders.csv");
		Assert.assertTrue(result.toString(), result.isSuccessful());
		List<OrderHeader> orders = OrderHeader.staticGetDao().findByParent(foundFacility);
		for (OrderHeader orderHeader : orders) {
			Assert.assertNotNull(orderHeader.getOrderDetails());
		}
		commitTransaction();

	}

	//******************** TESTS without Group ID ***********************

	@Test
	public final void testImportEmptyQuantityAsZero() throws IOException {
		// create facility
		getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		commitTransaction();

		// import files
		getTenantPersistenceService().beginTransaction();
		String withOneEmptyQuantity = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,\"\",each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		BatchResult<Object> results = importOrdersData(facility, withOneEmptyQuantity);
		commitTransaction();

		// verify data
		getTenantPersistenceService().beginTransaction();
		List<OrderDetail> orderDetails = OrderDetail.staticGetDao().getAll();
		Assert.assertEquals(1, orderDetails.size());
		Assert.assertTrue(results.isSuccessful());
		Assert.assertEquals(0, orderDetails.get(0).getQuantity().intValue());
		commitTransaction();
	}

	@Test
	public final void testImportAlphaQuantityAsZero() throws IOException {
		getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		getTenantPersistenceService().commitTransaction();

		beginTransaction();
		String withOneEmptyQuantity = "preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,\"A\",each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		BatchResult<Object> results = importOrdersData(facility, withOneEmptyQuantity);
		getTenantPersistenceService().commitTransaction();

		getTenantPersistenceService().beginTransaction();
		List<OrderDetail> orderDetails = OrderDetail.staticGetDao().getAll();
		Assert.assertEquals(1, orderDetails.size());
		Assert.assertTrue(results.isSuccessful());
		Assert.assertEquals(0, orderDetails.get(0).getQuantity().intValue());
		getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testReimportOutboundOrderNoGroup() throws IOException {
		// set up facility
		beginTransaction();
		Facility facility = Facility.createFacility("F-ORD2.1", "TEST", Point.getZeroPoint());
		commitTransaction();

		// import order files
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		String firstCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check order stats
		beginTransaction();
		HeaderCounts theCounts = facility.countOutboundOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 3);
		Assert.assertTrue(theCounts.mActiveHeaders == 3);
		Assert.assertTrue(theCounts.mActiveDetails == 11);
		Assert.assertTrue(theCounts.mActiveCntrUses == 3);
		// and import exact same file again
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check order stats again
		beginTransaction();
		HeaderCounts theCounts2 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts2.mTotalHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveHeaders == 3);
		Assert.assertTrue(theCounts2.mActiveDetails == 11);
		Assert.assertTrue(theCounts2.mInactiveDetailsOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mInactiveCntrUsesOnActiveOrders == 0);
		Assert.assertTrue(theCounts2.mActiveCntrUses == 3);
		// Try the kinds of things we did before with group ID. Remove detail 123.4  And remove 789 altogether. (Dates are the same.)
		String secondCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, secondCsvString);
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// See, works same as if matching the group
		HeaderCounts theCounts3 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts3.mTotalHeaders == 3);
		Assert.assertEquals(theCounts3.mActiveHeaders, 3);
		Assert.assertEquals(theCounts3.mActiveDetails, 10);
		Assert.assertEquals(theCounts3.mInactiveDetailsOnActiveOrders, 1);
		Assert.assertEquals(theCounts3.mInactiveCntrUsesOnActiveOrders, 0);
		Assert.assertEquals(theCounts3.mActiveCntrUses, 3);

		// Can a customer update a single order or detail by setting the count to zero
		String fourthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,0,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, fourthCsvString);

		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		HeaderCounts theCounts4 = facility.countOutboundOrders();
		Assert.assertEquals(theCounts4.mTotalHeaders, 3);
		Assert.assertEquals(theCounts4.mActiveHeaders, 2);
		Assert.assertEquals(theCounts4.mActiveDetails, 7);
		Assert.assertEquals(theCounts4.mInactiveDetailsOnActiveOrders, 0);
		Assert.assertEquals(theCounts4.mInactiveCntrUsesOnActiveOrders, 0);
		Assert.assertEquals(theCounts4.mActiveCntrUses, 2);

		// So, can a customer update the count on a single item? 123.2 going to 3.
		String fifthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, fifthCsvString);

		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Well, yes. Looks like that detail was cleanly updated without bothering anything else.
		HeaderCounts theCounts5 = facility.countOutboundOrders();
		Assert.assertEquals(theCounts5.mTotalHeaders, 3);
		Assert.assertEquals(theCounts5.mActiveHeaders, 3);
		Assert.assertEquals(theCounts5.mActiveDetails, 8);
		Assert.assertEquals(theCounts5.mInactiveDetailsOnActiveOrders, 3);
		Assert.assertEquals(theCounts5.mInactiveCntrUsesOnActiveOrders, 0);
		Assert.assertEquals(theCounts5.mActiveCntrUses, 3);

		// re-submit order with three items instead of five
		String sixthCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,4,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		importOrdersData(facility, sixthCsvString);

		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// inactive detail count should increase by 2 to 5 and active detail count go from 6 to 6
		// other counts remain the same
		HeaderCounts theCounts6 = facility.countOutboundOrders();
		Assert.assertTrue(theCounts6.mTotalHeaders == 3);
		Assert.assertTrue(theCounts6.mActiveHeaders == 3);
		Assert.assertTrue(theCounts6.mActiveDetails == 6);
		Assert.assertTrue(theCounts6.mInactiveDetailsOnActiveOrders == 5);
		Assert.assertTrue(theCounts6.mInactiveCntrUsesOnActiveOrders == 0);
		Assert.assertTrue(theCounts6.mActiveCntrUses == 3);
		commitTransaction();
	}

	@Test
	public final void showNoItemCreationfromOrders() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);
		commitTransaction();

		LOGGER.info("1: Read a very small aisles and locations file. Aliases exist only for D34, D35, D13");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		loadSmallAislesAndLocations(facility);
		commitTransaction();

		LOGGER.info("3: Read the orders file, which has some preferred locations");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,gtin,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D35,61"
				+ "\r\n10,10,10.2,SKU0002,16 oz Clear Cup,2,CS,,pick,D34,43"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D13,";

		importOrdersData(facility, csvString);

		OrderHeader order10 = OrderHeader.staticGetDao().findByDomainId(facility, "10");

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

		OrderHeader order11 = OrderHeader.staticGetDao().findByDomainId(facility, "11");

		OrderDetail detail111 = order11.getOrderDetail("11.1");
		String prefLoc111 = detail111.getPreferredLocation();
		// from v12, even unresolved preferredLocation should be on the order detail
		Assert.assertEquals("D21", prefLoc111);

		OrderDetail detail112 = order11.getOrderDetail("11.2");
		String prefLoc112 = detail112.getPreferredLocation();
		Assert.assertEquals("D13", prefLoc112);

		ItemMaster theMaster = ItemMaster.staticGetDao().findByDomainId(facility, "SKU0001");
		Assert.assertNotNull("ItemMaster should be created", theMaster);
		List<Item> items = theMaster.getItems();
		Assert.assertEquals(0, items.size()); // No inventory created

		commitTransaction();
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
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1"
				+ "\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1"
				+ "\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1"
				+ "\r\n4,4,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1";
		importOrdersData(facility, secondCsvString);

		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", false);
		headerExpectations.put("2", false);
		headerExpectations.put("3", true);
		headerExpectations.put("4", true);
		assertArchiveStatuses(groupExpectations, headerExpectations);

		commitTransaction();
	}

	/**
	 * Same as testArchiveNoGroup, but without specifying a group
	 */
	@Test
	public final void testArchiveNoGroup() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom"
				+ "\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a"
				+ "\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a"
				+ "\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom"
				+ "\r\n3,3,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a"
				+ "\r\n4,4,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a";
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", true);
		headerExpectations.put("2", true);
		headerExpectations.put("3", true);
		headerExpectations.put("4", true);
		assertArchiveStatuses(groupExpectations, headerExpectations);
		commitTransaction();
	}

	@Test
	public final void testArchiveTwoGroups() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1"
				+ "\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1"
				+ "\r\n3,3,347,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group2";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n4,4,349,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2"
				+ "\r\n5,5,350,12/03/14 12:00,12/31/14 12:00,Item8,,50,a,Group2";
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

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

		commitTransaction();
	}

	@Test
	public final void testArchiveAbandonedGroup() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		LOGGER.info("1: Read tiny orders for for group 1, 2 orders with one detail each.");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1"
				+ "\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		OrderHeader header1a = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderGroup group1a = header1a.getOrderGroup();
		Assert.assertNotNull(group1a);
		Assert.assertEquals("Group1", group1a.getDomainId());

		LOGGER.info("2: As if the orders were pushed to later group, same orders except for group2.");
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group2"
				+ "\r\n2,2,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2";
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		group1a = OrderGroup.staticGetDao().reload(group1a);
		OrderHeader header1b = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderGroup group1b = header1b.getOrderGroup();
		OrderGroup group2 = facility.getOrderGroup("Group2");
		// Is it the same header?
		Assert.assertEquals(header1a, header1b);
		// did it change owner?
		Assert.assertNotEquals(group1a, group1b);
		Assert.assertEquals(group1b, group2); // just proving what we expect

		/*  this is no longer a valid assumption, since groups are deactivated via a background process
		LOGGER.info("3: We expect to find that Group1 is inactive. Group2 is active with its orders.");
		HashMap<String, Boolean> groupExpectations = new HashMap<String, Boolean>();
		groupExpectations.put("Group1", false);
		groupExpectations.put("Group2", true);
		HashMap<String, Boolean> headerExpectations = new HashMap<String, Boolean>();
		headerExpectations.put("1", true);
		headerExpectations.put("2", true);
		headerExpectations.put("99", true); // just showing  the limit of the test function. Order 99 does not exist. No error reported.
		assertArchiveStatuses(groupExpectations, headerExpectations);
		*/

		LOGGER.info("  We showed that the orders are the same objects. They changed owner groups.");
		commitTransaction();
	}

	@Test
	public final void demonstrateArchiveQuirk() throws IOException {
		/* This simulates doing some of the work in the morning wave, then completing some of the orders in the afternoon wave.
		 * The corresponding real test would be to drop orders file similar to the first. Set up the cart for the orders. Run, shorting the jobs that are not completed,
		 * (or don't set up the cart at all for orders that have no lines that need to be completed.)
		 * and completing work instructions for the ones that are completed.
		 * Then drop the second orders file.
		 */
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		LOGGER.info("1: Read orders for for group 1, as if it is morning wave");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,1.1,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1"
				+ "\r\n1,1,1.2,12/03/14 12:00,12/31/14 12:00,Item16,,90,a,Group1"
				+ "\r\n2,2,2.1,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1"
				+ "\r\n3,3,3.1,12/03/14 12:00,12/31/14 12:00,Item9,,100,a,Group1";
		importOrdersData(facility, firstCsvString);

		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		OrderHeader header1a = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail detail1_1a = header1a.getOrderDetail("1.1");
		Assert.assertNotNull(detail1_1a);

		OrderDetail detail1_2a = header1a.getOrderDetail("1.2");
		Assert.assertNotNull(detail1_2a);
		OrderHeader header2a = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		Assert.assertNotNull(header2a);
		OrderHeader header3a = OrderHeader.staticGetDao().findByDomainId(facility, "3");
		OrderDetail detail3_1a = header3a.getOrderDetail("3.1");

		LOGGER.info("2: Partially complete order 1. Fully complete order 3. Leave order 2 uncompleted.");
		detail1_1a.setStatus(OrderStatusEnum.COMPLETE);
		detail3_1a.setStatus(OrderStatusEnum.COMPLETE);
		header3a.setStatus(OrderStatusEnum.COMPLETE);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		LOGGER.info("3: As if remaining work is reassigned to a later wave, push what is left to group2.");
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,1.2,12/03/14 12:00,12/31/14 12:00,Item16,,90,a,Group2"
				+ "\r\n2,2,2.1,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group2";
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		OrderGroup orderGroup1b = facility.getOrderGroup("Group1");
		OrderGroup orderGroup2b = facility.getOrderGroup("Group2");
		OrderHeader header1b = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail detail1_1b = header1b.getOrderDetail("1.1");
		Assert.assertNotNull(detail1_1b);
		OrderDetail detail1_2b = header1b.getOrderDetail("1.2");
		Assert.assertNotNull(detail1_2b);
		OrderHeader header2b = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		Assert.assertNotNull(header2b);
		OrderHeader header3b = OrderHeader.staticGetDao().findByDomainId(facility, "3");
		OrderDetail detail3_1b = header3b.getOrderDetail("3.1");
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

		commitTransaction();
	}

	@Test
	public final void testOrderDetailIdNormal() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		//Test that if orderDetailId is provided, it becomes detail's id. If not, then use the item-uom combination.
		//Also confirm that detail ids can be reused across orders
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,90,each,Group1"
				+ "\r\n1,1,,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1"
				+ "\r\n2,2,101,12/03/14 12:00,12/31/14 12:00,Item3,,90,each,Group1"
				+ "\r\n2,2,,12/03/14 12:00,12/31/14 12:00,Item2,,90,each,Group1";
		importOrdersData(facility, firstCsvString);

		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail d1_1 = h1.getOrderDetail("101");
		assertActiveOrderDetail(d1_1);
		OrderDetail d1_2 = h1.getOrderDetail("Item2-each");
		assertActiveOrderDetail(d1_2);
		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		OrderDetail d2_1 = h2.getOrderDetail("101");
		assertActiveOrderDetail(d2_1);
		OrderDetail d2_2 = h2.getOrderDetail("Item2-each");
		assertActiveOrderDetail(d2_2);

		commitTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void testOrderDetailIdDuplicates() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId"
				+
				//Repeating orderDetailId - leave last one
				"\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,9,each,Group1"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item2,,10,each,Group1"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item3,,11,each,Group1"
				+
				//Repeating item-uom - leave last one. In this case, also loose orderDetailId.
				"\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item4,,9,each,Group1"
				+ "\r\n1,1,,12/03/14 12:00,12/31/14 12:00,Item4,,10,each,Group1"
				+
				//Same item, but different uom - OK
				"\r\n1,1,,12/03/14 12:00,12/31/14 12:00,Item4,,10,cs,Group1"
				+
				//Other order - OK
				"\r\n2,2,101,12/03/14 12:00,12/31/14 12:00,Item1,,9,each,Group1"
				+ "\r\n2,2,,12/03/14 12:00,12/31/14 12:00,Item4,,10,each,Group1";
		importOrdersData(facility, firstCsvString);

		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");

		OrderDetail d1_1 = h1.getOrderDetail("101");
		assertActiveOrderDetail(d1_1);
		Assert.assertEquals("Incorrect item saved", "Item3", d1_1.getItemMaster().getDomainId());
		Assert.assertTrue("Incorrect quantity saved", d1_1.getQuantity() == 11);

		OrderDetail d1_2 = h1.getOrderDetail("Item4-each");
		assertActiveOrderDetail(d1_2);

		OrderDetail d1_3 = h1.getOrderDetail("Item4-cs");
		assertActiveOrderDetail(d1_2);

		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "2");

		OrderDetail d2_1 = h2.getOrderDetail("101");
		assertActiveOrderDetail(d2_1);
		OrderDetail d2_2 = h2.getOrderDetail("Item4-each");
		assertActiveOrderDetail(d2_2);

		commitTransaction();
	}

	@Test
	public final void testPreferedSequence() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,workSequence"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,90,each,Group1,1"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1,2"
				+ "\r\n2,2,201,12/03/14 12:00,12/31/14 12:00,Item3,,90,each,Group1,"
				+ "\r\n2,2,202,12/03/14 12:00,12/31/14 12:00,Item2,,90,each,Group1,2";
		importOrdersData(facility, firstCsvString);

		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Assert.assertEquals(d1_1.getWorkSequence(), (Integer) 1);
		OrderDetail d1_2 = h1.getOrderDetail("102");
		Assert.assertEquals(d1_2.getWorkSequence(), (Integer) 2);

		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		OrderDetail d2_1 = h2.getOrderDetail("201");
		Assert.assertNull(d2_1.getWorkSequence());
		OrderDetail d2_2 = h2.getOrderDetail("202");
		Assert.assertEquals(d2_2.getWorkSequence(), (Integer) 2);

		commitTransaction();
	}

	/*
	 * If multiple gtins are created for an item we may return the incorrect gtin. We do not check if a gtin exists
	 * for an item before creating a new one. Probably good to do in the future if this becomes an issue, however,
	 * multiple gtins for identical items does not make sense.
	 */
	@Test
	public final void testGtin() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,90,each,Group1,1"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1,2"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1,3"
				+ // Create new gtin for existing item. Not well supported
				"\r\n2,2,201,12/03/14 12:00,12/31/14 12:00,Item3,,90,each,Group1,4"
				+ "\r\n2,2,202,12/03/14 12:00,12/31/14 12:00,Item3,,90,cs,Group1,4"
				+ // Repeat gtin for diff UOM
				"\r\n2,2,203,12/03/14 12:00,12/31/14 12:00,Item4,,90,each,Group1,5"
				+ "\r\n2,2,204,12/03/14 12:00,12/31/14 12:00,Item5,,90,each,Group1,5" + // Repeat gtin for different item
				"\r\n2,2,205,12/03/14 12:00,12/31/14 12:00,Item5,,90,cs,Group1,6"; // Create new gtin for new UOM
		importOrdersData(facility, firstCsvString);

		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Gtin d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("1", d1_1_gtin.getDomainId());

		/*
		 * This tests unsupported functionality of multiple GTINs for the same item
		 * 2 or 3 would be "correct" answers
		 */
		/*
		OrderDetail d1_2 = h1.getOrderDetail("103");
		Gtin d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("2", d1_2_gtin.getDomainId());
		*/

		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		OrderDetail d2_1 = h2.getOrderDetail("201");
		Gtin d2_1_gtin = d2_1.getItemMaster().getGtinForUom(d2_1.getUomMaster());
		// there was a d2_1_gtin for detail 102. But it got converted on the line commented // Repeat gtin for diff UOM
		// So, now we will not find the gtin for the previous UOM
		Assert.assertNull(d2_1_gtin);

		OrderDetail d2_2 = h2.getOrderDetail("202");
		Gtin d2_2_gtin = d2_2.getItemMaster().getGtinForUom(d2_2.getUomMaster());
		Assert.assertNotNull(d2_2_gtin); // this was the converted one

		OrderDetail d2_3 = h2.getOrderDetail("203");
		Gtin d2_3_gtin = d2_3.getItemMaster().getGtinForUom(d2_3.getUomMaster());
		Assert.assertEquals("5", d2_3_gtin.getDomainId());

		OrderDetail d2_4 = h2.getOrderDetail("204");
		Gtin d2_4_gtin = d2_4.getItemMaster().getGtinForUom(d2_4.getUomMaster());
		Assert.assertNull(d2_4_gtin);

		OrderDetail d2_5 = h2.getOrderDetail("205");
		Gtin d2_5_gtin = d2_5.getItemMaster().getGtinForUom(d2_5.getUomMaster());
		Assert.assertEquals("6", d2_5_gtin.getDomainId());

		commitTransaction();
	}

	@Test
	public final void testGtinReimport() throws IOException {
		// initial order import
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin-1-each"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,each,Group1,gtin-2";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check gtin
		beginTransaction();
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Gtin d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("gtin-1-each", d1_1_gtin.getDomainId());
		OrderDetail d1_2 = h1.getOrderDetail("102");
		Gtin d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("gtin-1-case", d1_2_gtin.getDomainId());
		commitTransaction();

		// modify gtin
		beginTransaction();
		facility = Facility.staticGetDao().findByPersistentId(facilityId);
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin-1-each-mod"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,each,Group1,gtin-2";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check gtin again
		beginTransaction();
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		d1_1 = h1.getOrderDetail("101");
		d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("gtin-1-each-mod", d1_1_gtin.getDomainId());
		d1_2 = h1.getOrderDetail("102");
		d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("gtin-1-case", d1_2_gtin.getDomainId());
		commitTransaction();
	}

	@Test
	public final void testFailedLinesCounter() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		LOGGER.info("1: No errors");
		String ordersStringNormal = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId\n"
				+ "order1,detail1,item1,description1,5,each,LocX24,cont1\n"
				+ "order1,detail2,item2,description2,5,each,LocX25,cont1\n"
				+ "order2,detail3,item3,description3,5,each,LocX26,cont2\n"
				+ "order2,detail4,item4,description4,5,each,LocX27,cont2\n";
		BatchResult<Object> result = importOrdersData(facility, ordersStringNormal);
		Assert.assertEquals(4, result.getLinesProcessed());
		Assert.assertEquals(2, result.getOrdersProcessed());
		Assert.assertEquals(0, result.getViolations().size());

		LOGGER.info("2: Bad header causes all lines to fail");
		String ordersStringBadHeader = "orderId,orderDetailId,itemXId,description,quantity,uom,locationId,preAssignedContainerId\n"
				+ "order1,detail1,item1,description1,5,each,LocX24,cont1\n"
				+ "order1,detail2,item2,description2,5,each,LocX25,cont1\n"
				+ "order2,detail3,item3,description3,5,each,LocX26,cont2\n"
				+ "order2,detail4,item4,description4,5,each,LocX27,cont2\n";
		result = importOrdersData(facility, ordersStringBadHeader);
		Assert.assertEquals(4, result.getLinesProcessed());
		Assert.assertEquals(2, result.getOrdersProcessed());
		Assert.assertEquals(4, result.getViolations().size());
		Assert.assertTrue(result.getViolations().get(0).getMessage().contains("Required field 'itemId' is null"));
		Assert.assertTrue(result.getViolations().get(1).getMessage().contains("Required field 'itemId' is null"));
		Assert.assertTrue(result.getViolations().get(2).getMessage().contains("Required field 'itemId' is null"));
		Assert.assertTrue(result.getViolations().get(3).getMessage().contains("Required field 'itemId' is null"));

		LOGGER.info("3: Missing order id");
		String ordersStringMissingOrderId = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId\n"
				+ "order1,detail1,item1,description1,5,each,LocX24,cont1\n"
				+ ",detail2,item2,description2,5,each,LocX25,cont1\n"
				+ "order2,detail3,item3,description3,5,each,LocX26,cont2\n"
				+ "order2,detail4,item4,description4,5,each,LocX27,cont2\n";
		result = importOrdersData(facility, ordersStringMissingOrderId);
		Assert.assertEquals(1, result.getViolations().size());
		Assert.assertTrue(result.getViolations()
			.get(0)
			.getMessage()
			.contains("Errors on line 3: \nRequired field 'orderId' is empty"));

		LOGGER.info("3: Missing item id");
		String ordersStringMissingDetailId = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId\n"
				+ "order1,detail1,item1,description1,5,each,LocX24,cont1\n"
				+ "order1,detail2,item2,description2,5,each,LocX25,cont1\n"
				+ "order2,detail3,,description3,5,each,LocX26,cont2\n"
				+ "order2,detail4,item4,description4,5,each,LocX27,cont2\n";
		result = importOrdersData(facility, ordersStringMissingDetailId);
		Assert.assertEquals(1, result.getViolations().size());
		Assert.assertTrue(result.getViolations()
			.get(0)
			.getMessage()
			.contains("Errors on line 4: \nRequired field 'itemId' is empty"));

		commitTransaction();
	}

	@Test
	public final void testLorealOrders() throws Exception {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);

		String createOrderHeader = "def OrderImportCreateHeader(orderHeader) { \n"
				+ "	orderHeader= \"orderId, orderDetailId, itemId, description, quantity, uom, preAssignedContainerId, locationId, workSequence, gtin\"\n"
				+ "}\n";

		/*
		 * Loreal pilot: requirements become known.
		 * F aisle has 5 character names. All positions need scan.
		 * T aisle has 6 character names like T108D7. For those, no scan if bay is even (like 108), and need scan if odd (like T111C3)
		 * Virtual position is currently named T999B1. That is odd, so correctly needs scan.		
		 */

		String transformOrderBean = "def OrderImportBeanTransformation(bean) {\n"
				+ "	needsScan = determineNeedsScan(bean.locationId);\n" + "	bean.needsScan = needsScan.toString();\n"
				+ "	return bean;\n" + "}\n" + "\n" + "def determineNeedsScan(locationId){\n" + "	if (locationId.length() < 5 ){\n"
				+ "		return true;}\n" + "	if (locationId[0] == \"F\"){\n" + "		return true;}\n" + "	if (locationId[0] == \"T\"){\n"
				+ "		bayLastChar = locationId[3];\n"
				+ "		return [1:\"1\",2:\"3\",3:\"5\",4:\"7\",9:\"C\"].containsValue(bayLastChar)}\n" +

				"	return false;\n" + "}";

		ExtensionPointEngine engine = ExtensionPointEngine.getInstance(facility);
		ExtensionPoint extensionHeader = new ExtensionPoint(facility, ExtensionPointType.OrderImportCreateHeader);
		extensionHeader.setScript(createOrderHeader);
		extensionHeader.setActive(true);
		engine.create(extensionHeader);
		ExtensionPoint extensionBean = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		extensionBean.setScript(transformOrderBean);
		extensionBean.setActive(true);
		engine.create(extensionBean);

		String ordersStringNormal = "2105334827,2105334827.1,10043585,KL SUNFLOWER SHAMPOO COLOR 500ML,1,EA,731781354,T116C5,1150,3605975054118\n"
				+ "2105334827,2105334827.2,10059617,KL CLEARLY CORRECTIVE 30ML,1,EA,731781354,F24D3,120,3605970202637\n"
				+ "2105334827,2105334827.3,10012841,KL ULTRA FACIAL MOISTURIZER 75ML,1,EA,731781354,T121C3,4500,3700194712068";
		BatchResult<Object> result = importOrdersData(facility, ordersStringNormal);
		Assert.assertTrue(result.isSuccessful());
		Assert.assertEquals(1, result.getOrdersProcessed());
		Assert.assertEquals(3, result.getLinesProcessed());

		OrderHeader header = OrderHeader.staticGetDao().findByDomainId(facility, "2105334827");
		Assert.assertNotNull(header);
		OrderDetail detail1 = header.getOrderDetail("2105334827.1");
		OrderDetail detail2 = header.getOrderDetail("2105334827.2");
		OrderDetail detail3 = header.getOrderDetail("2105334827.3");
		Assert.assertNotNull(detail1);
		Assert.assertNotNull(detail2);
		Assert.assertNotNull(detail3);
		Assert.assertFalse(detail1.getNeedsScan());
		Assert.assertTrue(detail2.getNeedsScan());
		Assert.assertTrue(detail3.getNeedsScan());
		commitTransaction();
	}

	/**
	 * Although not done as a Loreal test with their extensions, these are Loreal cases.  Oddness:
	 * 1) Loreal will change the detailId on us.
	 * 2) Loreal may send two or more details with the same SKU/UOM for an order, expecting us to pick enough for all these lines.
	 */
	@Test
	public final void testRepeatedDetails() throws IOException {
		// initial order import.  This has repeated lines for same SKU/UOM.  One sort of host error, repeating an order detail ID.
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom,gtin"
				+ "\r\n1001,1001,1001.1,ITEM01,Item 1,1,each,gtin-ITEM01"//
				+ "\r\n1001,1001,1001.2,ITEM01,Item 1,2,each,gtin-ITEM01"//
				+ "\r\n1002,1002,1002.1,ITEM02,Item 2,1,each,gtin-ITEM02"//
				+ "\r\n1002,1002,1002.2,ITEM02,Item 2,2,each,gtin-ITEM02"//
				+ "\r\n1002,1002,1002.3,ITEM02,Item 2,3,each,gtin-ITEM02"//
				+ "\r\n1002,1002,1002.3,ITEM02,Item 2,4,each,gtin-ITEM02"; // this is a repeat line, which will update to 4 count
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// Covers the DEV-1323 changes. Previously, 1001.1, 1002.1, and 1002.2 would be null
		beginTransaction();
		facility = facility.reload();
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1001");
		OrderDetail d1_1 = h1.getOrderDetail("1001.1");
		Assert.assertNotNull(d1_1);
		OrderDetail d1_2 = h1.getOrderDetail("1001.2");
		Assert.assertNotNull(d1_2);
		Assert.assertNotEquals(d1_1, d1_2);

		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "1002");
		OrderDetail d2_1 = h2.getOrderDetail("1002.1");
		Assert.assertNotNull(d1_1);
		OrderDetail d2_2 = h2.getOrderDetail("1002.2");
		Assert.assertNotNull(d2_2);
		OrderDetail d2_3 = h2.getOrderDetail("1002.3");
		Assert.assertNotNull(d2_3);
		Assert.assertEquals((Integer) 4, d2_3.getQuantity());
		commitTransaction();

		// re-import with changes.  This shows how Loreal will totally change the order line (SKU) for a detail.
		// And delete a line, which changes the ID on everything. (Note: this will wreck completed work instructions.)
		beginTransaction();
		facility = facility.reload();
		String secondCsvString = "orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom,gtin"
				+ "\r\n1001,1001,1001.1,ITEM01,Item 1,1,each,gtin-ITEM01"//
				+ "\r\n1001,1001,1001.2,ITEM02,Item 2,2,each,gtin-ITEM02"//
				+ "\r\n1002,1002,1002.1,ITEM02,Item 2,3,each,gtin-ITEM02";//
		importOrdersData(facility, secondCsvString);
		commitTransaction();

		// Covers the DEV-1323 changes. Previously, 1001.1, 1002.1, and 1002.2 would be null
		beginTransaction();
		facility = facility.reload();
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1001");
		d1_1 = h1.getOrderDetail("1001.1");
		Assert.assertNotNull(d1_1);
		Assert.assertTrue(d1_1.getActive());
		d1_2 = h1.getOrderDetail("1001.2");
		Assert.assertNotNull(d1_2);
		Assert.assertNotEquals(d1_1, d1_2);
		Assert.assertTrue(d1_2.getActive());

		h2 = OrderHeader.staticGetDao().findByDomainId(facility, "1002");
		d2_1 = h2.getOrderDetail("1002.1");
		Assert.assertNotNull(d1_1);
		Assert.assertTrue(d2_1.getActive());
		d2_2 = h2.getOrderDetail("1002.2");
		Assert.assertNotNull(d2_2);
		Assert.assertFalse(d2_2.getActive());
		d2_3 = h2.getOrderDetail("1002.3");
		Assert.assertNotNull(d2_3);
		Assert.assertEquals((Integer) 4, d2_3.getQuantity());
		Assert.assertFalse(d2_3.getActive());
		commitTransaction();

	}

	/**
	 * This is not generally useful. It gets absolutely all orders and groups, not even limiting to the facility.
	 * Then assumes that all are represented in the groupExpectations and headerExpectations and complains if one is found not in expectations. You cannot chain imports and only check the results for
	 * some of the headers and groups. This can only be used for extremely small tests. It would be generally useful if it iterated through the expectations,
	 * then found the groups and headers in the facility to check.
	 */
	private void assertArchiveStatuses(HashMap<String, Boolean> groupExpectations, HashMap<String, Boolean> headerExpectations) {
		List<OrderGroup> groups = OrderGroup.staticGetDao().getAll();
		for (OrderGroup group : groups) {
			assertArchiveStatusesHelper(groupExpectations, group.getDomainId(), group.getActive(), "group");
		}
		List<OrderHeader> headers = OrderHeader.staticGetDao().getAll();
		for (OrderHeader header : headers) {
			assertArchiveStatusesHelper(headerExpectations, header.getDomainId(), header.getActive(), "order header");
		}
	}

	private void assertArchiveStatusesHelper(HashMap<String, Boolean> expectations,
		String domainId,
		Boolean active,
		String examinedLevel) {
		System.out.println(examinedLevel + " " + domainId + " " + active);
		Boolean expected = expectations.get(domainId);
		Assert.assertNotNull("Encountered an unexpected " + examinedLevel + " " + domainId, expected);
		String message = String.format("Expected %s '%s' to be %s; instead was %s.", examinedLevel, domainId, expected ? "active"
				: "inactive", active ? "active" : "inactive");
		Assert.assertEquals(message, expected, active);
	}

	//******************** private helpers ***********************

	private Facility getTestFacility(String orgId, String facilityId) {
		Facility facility = Facility.createFacility(facilityId, "TEST", Point.getZeroPoint());
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
		importAislesData(inFacility, csvString1);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D34\r\n" //
				+ "A1.B2, D35\r\n" //
				+ "A2.B1, D13\r\n"; //
		importLocationAliasesData(inFacility, csvString2);
	}

	private BatchResult<?> importOrdersResource(Facility facility, String csvResource) throws IOException, InterruptedException {
		InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(csvResource);
		try {
			InputStreamReader reader = new InputStreamReader(stream);
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			return createOrderImporter().importOrdersFromCsvStream(reader, facility, ediProcessTime);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	private void assertActiveOrderDetail(OrderDetail detail) {
		Assert.assertNotNull(detail);
		Assert.assertTrue("Expected an active Order Detail. Got inactive instead", detail.getActive());
	}
}
