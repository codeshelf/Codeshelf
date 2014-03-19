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

		organization.createFacility("F-ORDLOC.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
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

		Aisle aisleA1 = new Aisle(facility, "A1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA1);

		Bay bayA1B1 = new Bay(aisleA1, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B1);

		Bay bayA1B2 = new Bay(aisleA1, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B2);

		Bay bayA1B3 = new Bay(aisleA1, "B3", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B3);

		Aisle aisleA2 = new Aisle(facility, "A2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA2);

		Bay bayA2B1 = new Bay(aisleA2, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA2B1);

		Bay bayA2B2 = new Bay(aisleA2, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA2B2);

		Aisle aisleA3 = new Aisle(facility, "A3", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA3);

		Bay bayA3B1 = new Bay(aisleA3, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA3B1);

		Bay bayA3B2 = new Bay(aisleA3, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
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

		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mOrderLocationDao);
		importer.importOrderLocationsFromCsvStream(reader, facility);

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

		organization.createFacility("F-ORDLOC.2", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
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

		Aisle aisleA1 = new Aisle(facility, "A1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA1);

		Bay bayA1B1 = new Bay(aisleA1, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B1);

		Bay bayA1B2 = new Bay(aisleA1, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B2);

		Bay bayA1B3 = new Bay(aisleA1, "B3", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA1B3);

		Aisle aisleA2 = new Aisle(facility, "A2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA2);

		Bay bayA2B1 = new Bay(aisleA2, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA2B1);

		Bay bayA2B2 = new Bay(aisleA2, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA2B2);

		Aisle aisleA3 = new Aisle(facility, "A3", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA3);

		Bay bayA3B1 = new Bay(aisleA3, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bayA3B1);

		Bay bayA3B2 = new Bay(aisleA3, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
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
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mOrderLocationDao);
		importer.importOrderLocationsFromCsvStream(reader, facility);

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
		importer.importOrderLocationsFromCsvStream(reader, facility);

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
