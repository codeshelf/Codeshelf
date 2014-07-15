/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;

/**
 * @author jeffw
 *
 */
public class OrderLocationImporterTest extends EdiTestABC {

	@Test
	public final void testOutOfOrderSlotting1() {

		// **************
		// First a trivial aisle
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A9,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,8,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-SLOTTING9");
		mOrganizationDao.store(organization);

		organization.createFacility("F-SLOTTING9", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-SLOTTING9");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);
		
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle);
		
		// **************
		// We need the location aliases
		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A9, D\r\n" //
				+ "A9.B1, DB\r\n" //
				+ "A2.B1.T1, DT\r\n" //
				+ "A2.B1.T1.S1, D-21\r\n" //
				+ "A2.B1.T1.S2, D-22\r\n" //
				+ "A2.B1.T1.S3, D-23\r\n" //
				+ "A2.B1.T1.S4, D-24\r\n" //
				+ "A2.B1.T1.S5, D-25\r\n" //
				+ "A2.B1.T1.S6, D-26\r\n"; //
		// Leaving S7 and S8 unknown

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);	
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		// **************
		// Now a slotting file.  No orders yet. This is the out of order situation.	 Normally we want orders before slotting.
		String csvString3 = "orderId,locationId\r\n" //
				+ "O1111, D-21\r\n" //
				+ "O1111, D-22\r\n" //
				+ "O2222, D-26\r\n" //
				+ "O3333, D-27\r\n" // Notice that D-27 does not resolve to a slot
				+ "O4444, D-23\r\n"; // This will not come in the orders file

		byte[] csvArray3 = csvString.getBytes();

		ByteArrayInputStream stream3 = new ByteArrayInputStream(csvArray3);
		InputStreamReader reader3 = new InputStreamReader(stream3);
		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer3 = new OrderLocationCsvImporter(mOrderLocationDao);
		importer3.importOrderLocationsFromCsvStream(reader3, facility, ediProcessTime3);
		
		// At this point (after the fix) we would like order number 01111 and 02222 to exist as dummy outbound orders.
		// Not sure about 03333
		OrderHeader order1111 = facility.getOrderHeader("01111");
		Assert.assertNull(order1111); // after fix, will have a header

		// **************
		// Now the orders file. The 01111 line has detailId 01111.1. The other two leave the detail blank, so will get a default name.
		String csvString4 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,,01111,01111.1,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,02222,,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,03333,,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray4 = csvString4.getBytes();

		ByteArrayInputStream stream4 = new ByteArrayInputStream(csvArray4);
		InputStreamReader reader4 = new InputStreamReader(stream4);
		Timestamp ediProcessTime4 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer4 = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer4.importOrdersFromCsvStream(reader4, facility, ediProcessTime4);

		order1111 = facility.getOrderHeader("01111");
		Assert.assertNotNull(order1111);
		// make sure order details got updated
		String cust = order1111.getCustomerId();
		Assert.assertEquals(cust, "COSTCO");

		OrderDetail orderDetail = order1111.getOrderDetail("01111.1");
		Assert.assertNotNull(orderDetail);
		
		// Make sure we can lookup all of the locations for order O1111. This pretty much proves it.
		Assert.assertEquals(0, order1111.getOrderLocations().size()); // after fix
		// Assert.assertEquals(2, order1111.getOrderLocations().size());


		// Other use cases?
		// If you redrop the orders file, do the locations go away?
		// Redrop slotting with a change should work.
		// If redrop of slotting has 01111 with only one location, is the other one cleared?
		// If redrop of slotting has 01111 to unknown location alias, are existing locations cleared?
	}

		
	@Test
	public final void testLocationAliasImporterFromCsvStream() {

		String csvString = "orderId,locationId\r\n" //
				+ "O1111, A1.B1\r\n" //
				+ "O1111, A1.B2\r\n" //
				+ "O1111, A1.B3\r\n" //
				+ "O2222, A1.B3\r\n" //
				+ "O2222, A2.B1\r\n" //
				+ ", A2.B2\r\n" // O3333's location
				+ "O4444, "; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORDLOC.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORDLOC.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORDLOC.1");

		OrderHeader order1111 = new OrderHeader();
		order1111.setOrderId("O1111");
		order1111.setParent(facility);
		order1111.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1111.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1111.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1111.setActive(true);
		order1111.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order1111);
		mOrderHeaderDao.store(order1111);

		OrderHeader order2222 = new OrderHeader();
		order2222.setOrderId("O2222");
		order2222.setParent(facility);
		order2222.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order2222.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2222.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2222.setActive(true);
		order2222.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order2222);
		mOrderHeaderDao.store(order2222);

		OrderHeader order3333 = new OrderHeader();
		order3333.setOrderId("O3333");
		order3333.setParent(facility);
		order3333.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order3333.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order3333.setDueDate(new Timestamp(System.currentTimeMillis()));
		order3333.setActive(true);
		order3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order3333);
		mOrderHeaderDao.store(order3333);

		OrderHeader order4444 = new OrderHeader();
		order4444.setOrderId("O4444");
		order4444.setParent(facility);
		order4444.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order4444.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order4444.setDueDate(new Timestamp(System.currentTimeMillis()));
		order4444.setActive(true);
		order4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order4444);
		mOrderHeaderDao.store(order4444);

		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA1);

		Bay bayA1B1 = new Bay(aisleA1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B1);

		Bay bayA1B2 = new Bay(aisleA1, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B2);

		Bay bayA1B3 = new Bay(aisleA1, "B3", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B3);

		Aisle aisleA2 = new Aisle(facility, "A2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA2);

		Bay bayA2B1 = new Bay(aisleA2, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA2B1);

		Bay bayA2B2 = new Bay(aisleA2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA2B2);

		Aisle aisleA3 = new Aisle(facility, "A3", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA3);

		Bay bayA3B1 = new Bay(aisleA3, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA3B1);

		Bay bayA3B2 = new Bay(aisleA3, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA3B2);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation3333 = new OrderLocation();
		orderLocation3333.setDomainId(OrderLocation.makeDomainId(order3333, bayA2B2));
		orderLocation3333.setLocation(bayA2B2);
		orderLocation3333.setActive(true);
		orderLocation3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation3333.setParent(order3333);
		mOrderLocationDao.store(orderLocation3333);
		order3333.addOrderLocation(orderLocation3333);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation4444 = new OrderLocation();
		orderLocation4444.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B1));
		orderLocation4444.setLocation(bayA3B1);
		orderLocation4444.setActive(true);
		orderLocation4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation4444.setParent(order4444);
		mOrderLocationDao.store(orderLocation4444);
		order4444.addOrderLocation(orderLocation4444);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation5555 = new OrderLocation();
		orderLocation5555.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B2));
		orderLocation5555.setLocation(bayA3B2);
		orderLocation5555.setActive(true);
		orderLocation5555.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation5555.setParent(order4444);
		mOrderLocationDao.store(orderLocation5555);
		order4444.addOrderLocation(orderLocation5555);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mOrderLocationDao);
		importer.importOrderLocationsFromCsvStream(reader, facility, ediProcessTime);

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			ILocation<?> location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}

		// Make sure we blanked out the order location for O3333.
		Assert.assertEquals(0, order3333.getOrderLocations().size());

		// Make sure we blanked out the order location for O4444.
		Assert.assertEquals(0, order4444.getOrderLocations().size());

	}

	@Test
	// There was a bug when you tried to import the same interchange twice.
	public final void testReimportSameData() {

		String csvString = "orderId,locationId\r\n" //
				+ "O1111, A1.B1\r\n" //
				+ "O1111, A1.B2\r\n" //
				+ "O1111, A1.B3\r\n" //
				+ "O2222, A1.B3\r\n" //
				+ "O2222, A2.B1\r\n" //
				+ ", A2.B2\r\n" // O3333's location
				+ "O4444, "; //

		byte[] csvArray = csvString.getBytes();

		Organization organization = new Organization();
		organization.setDomainId("O-ORDLOC.2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORDLOC.2", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-ORDLOC.2");

		OrderHeader order1111 = new OrderHeader();
		order1111.setOrderId("O1111");
		order1111.setParent(facility);
		order1111.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1111.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1111.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1111.setActive(true);
		order1111.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order1111);
		mOrderHeaderDao.store(order1111);

		OrderHeader order2222 = new OrderHeader();
		order2222.setOrderId("O2222");
		order2222.setParent(facility);
		order2222.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order2222.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2222.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2222.setActive(true);
		order2222.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order2222);
		mOrderHeaderDao.store(order2222);

		OrderHeader order3333 = new OrderHeader();
		order3333.setOrderId("O3333");
		order3333.setParent(facility);
		order3333.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order3333.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order3333.setDueDate(new Timestamp(System.currentTimeMillis()));
		order3333.setActive(true);
		order3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order3333);
		mOrderHeaderDao.store(order3333);

		OrderHeader order4444 = new OrderHeader();
		order4444.setOrderId("O4444");
		order4444.setParent(facility);
		order4444.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order4444.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order4444.setDueDate(new Timestamp(System.currentTimeMillis()));
		order4444.setActive(true);
		order4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order4444);
		mOrderHeaderDao.store(order4444);

		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA1);

		Bay bayA1B1 = new Bay(aisleA1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B1);

		Bay bayA1B2 = new Bay(aisleA1, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B2);

		Bay bayA1B3 = new Bay(aisleA1, "B3", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA1B3);

		Aisle aisleA2 = new Aisle(facility, "A2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA2);

		Bay bayA2B1 = new Bay(aisleA2, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA2B1);

		Bay bayA2B2 = new Bay(aisleA2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA2B2);

		Aisle aisleA3 = new Aisle(facility, "A3", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA3);

		Bay bayA3B1 = new Bay(aisleA3, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA3B1);

		Bay bayA3B2 = new Bay(aisleA3, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bayA3B2);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation3333 = new OrderLocation();
		orderLocation3333.setDomainId(OrderLocation.makeDomainId(order3333, bayA2B2));
		orderLocation3333.setLocation(bayA2B2);
		orderLocation3333.setActive(true);
		orderLocation3333.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation3333.setParent(order3333);
		mOrderLocationDao.store(orderLocation3333);
		order3333.addOrderLocation(orderLocation3333);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation4444 = new OrderLocation();
		orderLocation4444.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B1));
		orderLocation4444.setLocation(bayA3B1);
		orderLocation4444.setActive(true);
		orderLocation4444.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation4444.setParent(order4444);
		mOrderLocationDao.store(orderLocation4444);
		order4444.addOrderLocation(orderLocation4444);

		// This order location should get blanked out by the import.
		OrderLocation orderLocation5555 = new OrderLocation();
		orderLocation5555.setDomainId(OrderLocation.makeDomainId(order4444, bayA3B2));
		orderLocation5555.setLocation(bayA3B2);
		orderLocation5555.setActive(true);
		orderLocation5555.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderLocation5555.setParent(order4444);
		mOrderLocationDao.store(orderLocation5555);
		order4444.addOrderLocation(orderLocation5555);

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mOrderLocationDao);
		importer.importOrderLocationsFromCsvStream(reader, facility, ediProcessTime);

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			ILocation<?> location = orderLocation.getLocation();
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
		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrderLocationsFromCsvStream(reader, facility, ediProcessTime);

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(3, order1111.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		Assert.assertEquals(2, order2222.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		for (OrderLocation orderLocation : order1111.getOrderLocations()) {
			ILocation<?> location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}

		// Make sure we blanked out the order location for O3333.
		Assert.assertEquals(0, order3333.getOrderLocations().size());

		// Make sure we blanked out the order location for O4444.
		Assert.assertEquals(0, order4444.getOrderLocations().size());
	}

}
