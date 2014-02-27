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

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.model.domain.Organization;

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
				+ ", A2.B2\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-ORDLOC.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-ORDLOC.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-ORDLOC.1");

		OrderHeader order = new OrderHeader();
		order.setOrderId("O1111");
		order.setParent(facility);
		order.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order.setDueDate(new Timestamp(System.currentTimeMillis()));
		order.setActive(true);
		order.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order);
		mOrderHeaderDao.store(order);

		order = new OrderHeader();
		order.setOrderId("O2222");
		order.setParent(facility);
		order.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order.setDueDate(new Timestamp(System.currentTimeMillis()));
		order.setActive(true);
		order.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order);
		mOrderHeaderDao.store(order);

		order = new OrderHeader();
		order.setOrderId("O3333");
		order.setParent(facility);
		order.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order.setDueDate(new Timestamp(System.currentTimeMillis()));
		order.setActive(true);
		order.setUpdated(new Timestamp(System.currentTimeMillis()));
		facility.addOrderHeader(order);
		mOrderHeaderDao.store(order);

		Aisle aisle = new Aisle(facility, "A1", 0.0, 0.0);
		mSubLocationDao.store(aisle);

		Bay bay = new Bay(aisle, "B1", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay);

		bay = new Bay(aisle, "B2", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay);

		bay = new Bay(aisle, "B3", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay);

		aisle = new Aisle(facility, "A2", 0.0, 0.0);
		mSubLocationDao.store(aisle);

		bay = new Bay(aisle, "B1", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay);

		bay = new Bay(aisle, "B2", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay);
		
		// This order location should get blanked out by the import.
		order = facility.findOrder("O3333");
		bay = (Bay) facility.findSubLocationById("A2.B2");
		OrderLocation orderLocation1 = new OrderLocation();
		orderLocation1.setDomainId(order.getOrderId() + "-" + bay.getLocationId());
		orderLocation1.setLocation(bay);

		ICsvOrderLocationImporter importer = new CsvOrderLocationImporter(mOrderLocationDao);
		importer.importOrderLocationsFromCsvStream(reader, facility);

		// Make sure we can lookup all of the locations for order O1111.
		order = facility.findOrder("O1111");
		Assert.assertEquals(3, order.getOrderLocations().size());

		// Make sure we can lookup all of the locations for order O1111.
		order = facility.findOrder("O2222");
		Assert.assertEquals(2, order.getOrderLocations().size());

		// Make sure all of the order locations map to real locations.
		order = facility.findOrder("O1111");
		for (OrderLocation orderLocation : order.getOrderLocations()) {
			ILocation<?> location = orderLocation.getLocation();
			String locationId = location.getLocationIdToParentLevel(Aisle.class);
			location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
		}
		
		// Make sure we blanked out the order location for O3333.
		order = facility.findOrder("O3333");
		Assert.assertEquals(0, order.getOrderLocations().size());

	}
}
