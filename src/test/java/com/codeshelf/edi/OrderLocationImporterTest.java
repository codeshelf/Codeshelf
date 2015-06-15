/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jeffw
 *
 */
public class OrderLocationImporterTest extends ServerTest {

	/**
	 * Given two orders and a single slot
	 * When both orders are slotted to the same location
	 * Then both orders will contain that location
	 *
	 */
	@Test
	public final void testMultipleOrdersToOneSlot() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getTestFacility("O-testMultipleOrdersToOneSlot", "F-testMultipleOrdersToOneSlot");
		doLocationSetup(facility);
		String singleSlot = "D-21";
		doSingleSlotOrder(facility, "01111", singleSlot);
		doSingleSlotOrder(facility, "02222", singleSlot);
		
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "01111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "02222");

		assertOrderHasLocation(facility, order1, singleSlot);
		assertOrderHasLocation(facility, order2, singleSlot);

		this.getTenantPersistenceService().commitTransaction();
	}
	
	/*
	 * TODO probably better to move to a higher level test across slotting and orders
	 */
	@Test
	public final void testSlottingBeforeOrders() throws IOException {
		beginTransaction();
		Facility facility = getTestFacility("O-SLOTTING9", "F-SLOTTING9");
		setupTestLocations(facility);
		
		// **************
		// Now a slotting file.  No orders yet. This is the out of order situation.	 Normally we want orders before slotting.
		String slottingCsv = "orderId,locationId\r\n" //
				+ "01111, D-21\r\n" //
				+ "01111, D-22\r\n" //
				+ "02222, D-27\r\n" // Notice that D-27 does not resolve to a slot
				+ "03333, D-26\r\n" // Should still be processed
				+ "04444, D-23\r\n"; // This will not come in the orders file

		Assert.assertTrue("Should have been 'successful' if something was imported", importSlotting(facility, slottingCsv)); //One of the order slots could be updated
		// At this point we would like order number 01111 and 03333 to exist as dummy outbound orders.
		// Not sure about 02222
		OrderHeader order1111 = OrderHeader.staticGetDao().findByDomainId(facility, "01111");

		Assert.assertNotNull(order1111); // after fix, will have a header
		Assert.assertEquals(2, order1111.getOrderLocations().size());

		OrderHeader order3333 = OrderHeader.staticGetDao().findByDomainId(facility, "03333");
		Assert.assertNotNull("Should have still processed the other lines after an error", order3333); // after fix, will have a header
		Assert.assertEquals("Should have still processed the other lines after an error",1, order3333.getOrderLocations().size());
		commitTransaction();
		
		beginTransaction();
		// **************
		// Now the orders file. The 01111 line has detailId 01111.1. The other two leave the detail blank, so will get a default name.
		String csvString4 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,,01111,01111.1,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,02222,,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,03333,,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvString4);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		order1111 = OrderHeader.staticGetDao().findByDomainId(facility, "01111");

		Assert.assertNotNull(order1111);
		// make sure order details got updated
		String cust = order1111.getCustomerId();
		Assert.assertEquals(cust, "COSTCO");

		OrderDetail orderDetail = order1111.getOrderDetail("01111.1");
		Assert.assertNotNull(orderDetail);
		
		// Make sure we can lookup all of the locations for order O1111. This pretty much proves it.
		Assert.assertEquals(2, order1111.getOrderLocations().size());

		this.getTenantPersistenceService().commitTransaction();

		// Other use cases?
		// If you redrop the orders file, do the locations go away?
		// If redrop of slotting has 01111 to unknown location alias, are existing locations cleared?
	}

	/**
	 * Given an initial order with a location
	 * When the slotting file contains a new location (without a null "reset" location line)
	 * Then the order location is updated
	 * 
	 */
	@Test
	public final void testSlotUpdate() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getTestFacility("ORG-testSlotUpdate", "F-testSlotUpdate");

		doSingleSlotOrder(facility, "01111", "D-21");
		
		String singleSlotUpdateCsv = "orderId,locationId\r\n" //
				+ "01111, D-22\r\n"; //
		Assert.assertTrue(importSlotting(facility, singleSlotUpdateCsv));
		
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "01111");

		Assert.assertEquals(1, order.getOrderLocations().size());
		assertOrderHasLocation(facility, order, "D-22");

		this.getTenantPersistenceService().commitTransaction();
	}
	
	/**
	 * Given an initial slotting
	 * When the order is moved 
	 * Then the final slot is the one returned
	 */
	@Test
	public final void testOnlyActiveSlotsReturned() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getTestFacility("ORG-testOnlyActiveSlotsReturned", "F-testOnlyActiveSlotsReturned");
		setupTestLocations(facility);

		String initialSlotFile = new SlotFileBuilder()
			.slot("01111", "D-21")
			.slot("01112", "D-22")
			.slot("01113", "D-23")
			.slot("01114", "D-24")
			.build();
		
		Assert.assertTrue("Failed to import slotting file", importSlotting(facility, initialSlotFile));

		String orderId = "01111";
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull("OrderHeader: " + orderId + " not found", order);
		assertOrderHasLocation(facility, order, "D-21");
		
		String modifySlots = new SlotFileBuilder()
		.slot("01111", "D-23")
		.slot("01112", "D-21")
		.slot("01113", "D-22")
			.slot("01114", "D-24")
		.build();
	
		Assert.assertTrue("Failed to import slotting file", importSlotting(facility, modifySlots));

		String rotateAgain = new SlotFileBuilder()
		.slot("01111", "D-22")
		.slot("01112", "D-23")
		.slot("01113", "D-21")
			.slot("01114", "D-24")
		.build();

		Assert.assertTrue(importSlotting(facility, rotateAgain));

		facility = Facility.staticGetDao().findByPersistentId(facility.getPersistentId());
		OrderHeader orderAfterReduction = OrderHeader.staticGetDao().findByDomainId(facility, "01111");

		Assert.assertEquals(1, orderAfterReduction.getOrderLocations().size());
		assertOrderHasLocation(facility, orderAfterReduction, "D-22");

		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void testSlotsResetWhenOrdersUnsorted() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getTestFacility("ORG-testSlotsResetWhenOrdersUnsorted", "F-testSlotsResetWhenOrdersUnsorted");

		doSingleSlotOrder(facility, "01111", "D-21");
		doSingleSlotOrder(facility, "02222", "D-22");
		
		String multiOrderSlotUpdateCsv = "orderId,locationId\r\n" //
				+ "01111, D-22\r\n" //
				+ "02222, D-23\r\n" //
				+ "01111, D-21\r\n"; //
		Assert.assertTrue(importSlotting(facility, multiOrderSlotUpdateCsv));
		
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "01111");

		Assert.assertEquals(2, order.getOrderLocations().size());
		assertOrderHasLocation(facility, order, "D-21");
		assertOrderHasLocation(facility, order, "D-22");
		
		OrderHeader order02222 = OrderHeader.staticGetDao().findByDomainId(facility, "02222");

		Assert.assertEquals(1, order02222.getOrderLocations().size());
		assertOrderHasLocation(facility, order02222, "D-23");

		this.getTenantPersistenceService().commitTransaction();
	}

	/**
	 * Given an initial order with 2 locations
	 * 	When a slotting file contains 1 null location and 1 real location for that order
	 *  Then the order has 1 location
	 * 
     */	
	@Test
	public final void testReduceOrderLocationsWithResetLine() {
		this.getTenantPersistenceService().beginTransaction();
		
		Facility facility = getTestFacility("ORG-testReduceOrderLocations", "F-testReduceOrderLocations");
		
		doMultiSlotOrder(facility, "01111", "D-21", "D-22");

		String singleSlotCsv = "orderId,locationId\r\n" //
				+ "01111, \r\n" // reset
				+ "01111, D-21\r\n"; //
		Assert.assertTrue(importSlotting(facility, singleSlotCsv));
		
		OrderHeader o1 = OrderHeader.staticGetDao().findByDomainId(facility, "01111");
		Assert.assertEquals(1, o1.getOrderLocations().size());

		this.getTenantPersistenceService().commitTransaction();
	}

	
	/**
	 * Given an initial order with 2 locations
	 * 	When a slotting file contains 1 null location and 1 real location for that order
	 *  Then the order has 1 location
	 * 
     */	
	@Test
	public final void testReduceOrderLocationsWithSingleLineUpdate() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getTestFacility("ORG-testReduceOrderLocationsWithSingleLineUpdate", "F-testReduceOrderLocationsWithSingleLineUpdate");
		
		doMultiSlotOrder(facility, "01111", "D-21", "D-22");

		String singleSlotCsv = "orderId,locationId\r\n" //
				+ "01111, D-23\r\n"; //
		Assert.assertTrue(importSlotting(facility, singleSlotCsv));
		
		OrderHeader o1 = OrderHeader.staticGetDao().findByDomainId(facility, "01111");
		Assert.assertEquals(1, o1.getOrderLocations().size());
		
		this.getTenantPersistenceService().commitTransaction();
	}
		
	@Test
	public final void testLocationAliasImporterFromCsvStream() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "orderId,locationId\r\n" //
				+ "O1111, A1.B1\r\n" //
				+ "O1111, A1.B2\r\n" //
				+ "O1111, A1.B3\r\n" //
				+ "O2222, A1.B3\r\n" //
				+ "O2222, A2.B1\r\n" //
				+ ", A2.B2\r\n" // O3333's location
				+ "O4444, "; //

		Facility facility = Facility.createFacility("F-ORDLOC.1", "TEST", Point.getZeroPoint());

		OrderHeader order1111 = new OrderHeader();
		order1111.setOrderId("O1111");
		order1111.setParent(facility);
		order1111.setOrderType(OrderTypeEnum.OUTBOUND);
		order1111.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1111.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1111.setActive(true);
		order1111.setUpdated(new Timestamp(System.currentTimeMillis()));
		order1111.setParent(facility);
		OrderHeader.staticGetDao().store(order1111);

		OrderHeader order2222 = new OrderHeader();
		order2222.setOrderId("O2222");
		order2222.setParent(facility);
		order2222.setOrderType(OrderTypeEnum.OUTBOUND);
		order2222.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2222.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2222.setActive(true);
		order2222.setUpdated(new Timestamp(System.currentTimeMillis()));
		order2222.setParent(facility);
		OrderHeader.staticGetDao().store(order2222);

		OrderHeader order3333 = new OrderHeader();
		order3333.setOrderId("O3333");
		order3333.setParent(facility);
		order3333.setOrderType(OrderTypeEnum.OUTBOUND);
		order3333.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order3333.setDueDate(new Timestamp(System.currentTimeMillis()));
		order3333.setActive(true);
		order3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		order3333.setParent(facility);
		OrderHeader.staticGetDao().store(order3333);

		OrderHeader order4444 = new OrderHeader();
		order4444.setOrderId("O4444");
		order4444.setParent(facility);
		order4444.setOrderType(OrderTypeEnum.OUTBOUND);
		order4444.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order4444.setDueDate(new Timestamp(System.currentTimeMillis()));
		order4444.setActive(true);
		order4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		order4444.setParent(facility);
		OrderHeader.staticGetDao().store(order4444);

		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA1);

		Bay bayA1B1 = aisleA1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B1);

		Bay bayA1B2 = aisleA1.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B2);

		Bay bayA1B3 = aisleA1.createBay("B3", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B3);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA2);

		Bay bayA2B1 = aisleA2.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA2B1);

		Bay bayA2B2 = aisleA2.createBay( "B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA2B2);

		Aisle aisleA3 = facility.createAisle("A3", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA3);

		Bay bayA3B1 = aisleA3.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA3B1);

		Bay bayA3B2 = aisleA3.createBay( "B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA3B2);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation3333 = new OrderLocation();
		orderLocation3333.setDomainId(OrderLocation.makeDomainId(order3333, bayA2B2));
		orderLocation3333.setLocation(bayA2B2);
		orderLocation3333.setActive(true);
		orderLocation3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation3333.setParent(order3333);
		OrderLocation.staticGetDao().store(orderLocation3333);
		order3333.addOrderLocation(orderLocation3333);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation4444 = new OrderLocation();
		orderLocation4444.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B1));
		orderLocation4444.setLocation(bayA3B1);
		orderLocation4444.setActive(true);
		orderLocation4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation4444.setParent(order4444);
		OrderLocation.staticGetDao().store(orderLocation4444);
		order4444.addOrderLocation(orderLocation4444);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation5555 = new OrderLocation();
		orderLocation5555.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B2));
		orderLocation5555.setLocation(bayA3B2);
		orderLocation5555.setActive(true);
		orderLocation5555.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation5555.setParent(order4444);
		OrderLocation.staticGetDao().store(orderLocation5555);
		order4444.addOrderLocation(orderLocation5555);

		boolean result = importSlotting(facility, csvString);
		Assert.assertTrue(result);
		
		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			Location location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}

		// Make sure we blanked out the order location for O3333.
		Assert.assertEquals(0, order3333.getOrderLocations().size());

		// Make sure we blanked out the order location for O4444.
		Assert.assertEquals(0, order4444.getOrderLocations().size());

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	// There was a bug when you tried to import the same interchange twice.
	public final void testReimportSameData() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "orderId,locationId\r\n" //
				+ "O1111, A1.B1\r\n" //
				+ "O1111, A1.B2\r\n" //
				+ "O1111, A1.B3\r\n" //
				+ "O2222, A1.B3\r\n" //
				+ "O2222, A2.B1\r\n" //
				+ ", A2.B2\r\n" // O3333's location
				+ "O4444, "; //

		Facility facility = Facility.createFacility("F-ORDLOC.2", "TEST", Point.getZeroPoint());

		OrderHeader order1111 = new OrderHeader();
		order1111.setOrderId("O1111");
		order1111.setParent(facility);
		order1111.setOrderType(OrderTypeEnum.OUTBOUND);
		order1111.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1111.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1111.setActive(true);
		order1111.setUpdated(new Timestamp(System.currentTimeMillis()));
		order1111.setParent(facility);
		OrderHeader.staticGetDao().store(order1111);

		OrderHeader order2222 = new OrderHeader();
		order2222.setOrderId("O2222");
		order2222.setParent(facility);
		order2222.setOrderType(OrderTypeEnum.OUTBOUND);
		order2222.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2222.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2222.setActive(true);
		order2222.setUpdated(new Timestamp(System.currentTimeMillis()));
		order2222.setParent(facility);
		OrderHeader.staticGetDao().store(order2222);

		OrderHeader order3333 = new OrderHeader();
		order3333.setOrderId("O3333");
		order3333.setParent(facility);
		order3333.setOrderType(OrderTypeEnum.OUTBOUND);
		order3333.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order3333.setDueDate(new Timestamp(System.currentTimeMillis()));
		order3333.setActive(true);
		order3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		order3333.setParent(facility);
		OrderHeader.staticGetDao().store(order3333);

		OrderHeader order4444 = new OrderHeader();
		order4444.setOrderId("O4444");
		order4444.setParent(facility);
		order4444.setOrderType(OrderTypeEnum.OUTBOUND);
		order4444.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order4444.setDueDate(new Timestamp(System.currentTimeMillis()));
		order4444.setActive(true);
		order4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		order4444.setParent(facility);
		OrderHeader.staticGetDao().store(order4444);

		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA1);

		Bay bayA1B1 = aisleA1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B1);

		Bay bayA1B2 = aisleA1.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B2);

		Bay bayA1B3 = aisleA1.createBay( "B3", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA1B3);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA2);

		Bay bayA2B1 = aisleA2.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA2B1);

		Bay bayA2B2 = aisleA2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA2B2);

		Aisle aisleA3 = facility.createAisle("A3", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisleA3);

		Bay bayA3B1 = aisleA3.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA3B1);

		Bay bayA3B2 = aisleA3.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.staticGetDao().store(bayA3B2);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation3333 = new OrderLocation();
		orderLocation3333.setDomainId(OrderLocation.makeDomainId(order3333, bayA2B2));
		orderLocation3333.setLocation(bayA2B2);
		orderLocation3333.setActive(true);
		orderLocation3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation3333.setParent(order3333);
		OrderLocation.staticGetDao().store(orderLocation3333);
		order3333.addOrderLocation(orderLocation3333);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation4444 = new OrderLocation();
		orderLocation4444.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B1));
		orderLocation4444.setLocation(bayA3B1);
		orderLocation4444.setActive(true);
		orderLocation4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation4444.setParent(order4444);
		OrderLocation.staticGetDao().store(orderLocation4444);
		order4444.addOrderLocation(orderLocation4444);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation5555 = new OrderLocation();
		orderLocation5555.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B2));
		orderLocation5555.setLocation(bayA3B2);
		orderLocation5555.setActive(true);
		orderLocation5555.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation5555.setParent(order4444);
		OrderLocation.staticGetDao().store(orderLocation5555);
		order4444.addOrderLocation(orderLocation5555);

		importSlotting(facility, csvString);

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			Location location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}

		// Make sure we blanked out the order location for O3333.
		Assert.assertEquals(0, order3333.getOrderLocations().size());

		// Make sure we blanked out the order location for O4444.
		Assert.assertEquals(0, order4444.getOrderLocations().size());

		// --------
		// Import it again - and rerun all the same tests.
		importSlotting(facility, csvString);
		
		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			Location location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}

		// Make sure we blanked out the order location for O3333.
		Assert.assertEquals(0, order3333.getOrderLocations().size());

		// Make sure we blanked out the order location for O4444.
		Assert.assertEquals(0, order4444.getOrderLocations().size());

		this.getTenantPersistenceService().commitTransaction();
	}
	
	private void setupTestLocations(Facility facility) {
		// **************
		// First a trivial aisle
		String aisleCsv = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A9,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,8,80,0,,\r\n"; //
		Assert.assertTrue(importAislesData(facility, aisleCsv));;
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle);
		Location location = facility.findSubLocationById("A9.B1.T1.S1");
		Assert.assertNotNull(location);

		// **************
		// We need the location aliases
		String locationAliasCsv = "mappedLocationId,locationAlias\r\n" //
				+ "A9, D\r\n" //
				+ "A9.B1, DB\r\n" //
				+ "A9.B1.T1, DT\r\n" //
				+ "A9.B1.T1.S1, D-21\r\n" //
				+ "A9.B1.T1.S2, D-22\r\n" //
				+ "A9.B1.T1.S3, D-23\r\n" //
				+ "A9.B1.T1.S4, D-24\r\n" //
				+ "A9.B1.T1.S5, D-25\r\n" //
				+ "A9.B1.T1.S6, D-26\r\n"; //
		// Leaving S7 and S8 unknown
		
		Assert.assertTrue(importLocationAliasesData(facility, locationAliasCsv));
		Location locationByAlias = facility.findSubLocationById("D-21");
		Assert.assertNotNull(locationByAlias);
	}

	private void doMultiSlotOrder(Facility facility, String orderId, String... locations) {
		doLocationSetup(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();		
		String multiSlotCsv = "orderId,locationId\r\n"; //
		for (int i = 0; i < locations.length; i++) {
			String locationId = locations[i];
			multiSlotCsv += orderId + ", " + locationId + "\r\n"; 
		}
		Assert.assertTrue(importSlotting(facility, multiSlotCsv));
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();		
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull("OrderHeader: " + orderId + "not found", order);
		Assert.assertEquals(locations.length, order.getOrderLocations().size());
	}
	
	private void doSingleSlotOrder(Facility facility, String orderId, String locationId) {
		doLocationSetup(facility);
		
		String doubleSlotCsv = "orderId,locationId\r\n" //
				+ orderId + ", " + locationId + "\r\n"; //
		Assert.assertTrue(importSlotting(facility, doubleSlotCsv));
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull("OrderHeader: " + orderId + "not found", order);
		Assert.assertEquals(1, order.getOrderLocations().size());
	}

	private void doLocationSetup(Facility facility) {
		// First a trivial aisle
		String aisleCsv = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A9,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,8,80,0,,\r\n"; //
		
		Assert.assertTrue(importAislesData(facility, aisleCsv));
		
		String locationsCsv = "mappedLocationId,locationAlias\r\n" //
				+ "A9, D\r\n" //
				+ "A9.B1, DB\r\n" //
				+ "A9.B1.T1, DT\r\n" //
				+ "A9.B1.T1.S1, D-21\r\n" //
				+ "A9.B1.T1.S2, D-22\r\n" //
				+ "A9.B1.T1.S3, D-23\r\n"; //
		Assert.assertTrue(importLocationAliasesData(facility, locationsCsv));
	}
	
	private Facility getTestFacility(String orgId, String facilityId) {
		Facility facility = Facility.createFacility(facilityId, "TEST", Point.getZeroPoint());
		return facility;
	}
	
	private void assertOrderHasLocation(Facility facility, OrderHeader order, String locationAlias) {
		Location mappedLocation = facility.findSubLocationById(locationAlias);
		//String orderLocationId = OrderLocation.makeDomainId(order, mappedLocation);
		Assert.assertTrue(order.getOrderLocations().size() > 0);
		boolean found = false;
		for (OrderLocation orderLocation : order.getOrderLocations()) {
			found = orderLocation.getLocation().getPersistentId().equals(mappedLocation.getPersistentId());
			if (found) break;
		}
		Assert.assertTrue("Unable to find order location " + locationAlias + " for order: " + order, found);
	}
	

	private static class SlotFileBuilder {
		StringBuilder file = new StringBuilder("orderId,locationId\r\n");
		
		public SlotFileBuilder slot(String orderHeaderId, String slotLocationId) {
			file.append(orderHeaderId + ", " + slotLocationId + "\r\n");
			return this;
		}
		
		public String build() {
			return file.toString();
		}
	}

}
