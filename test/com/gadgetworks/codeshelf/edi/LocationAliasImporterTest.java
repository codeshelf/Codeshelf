/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 *
 */
public class LocationAliasImporterTest extends EdiTestABC {

	@Test
	public final void testLocationAliasImporterFromCsvStream() {

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D139\r\n" //
				+ "A2.B2, B34\r\n" //
				+ "A3, AisleC\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-LOCS.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-LOCS.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-LOCS.1");

		Aisle aisleA1 = new Aisle(facility, "A1", 0.0, 0.0);
		mSubLocationDao.store(aisleA1);

		Bay bay1 = new Bay(aisleA1, "B1", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay1);

		Aisle aisleA2 = new Aisle(facility, "A2", 0.0, 0.0);
		mSubLocationDao.store(aisleA2);

		Bay bay2 = new Bay(aisleA2, "B2", 0.0, 0.0, 0.0);
		mSubLocationDao.store(bay2);

		ICsvLocationImporter importer = new CsvLocationImporter(mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(reader, facility);

		// Make sure we can still look up an aisle by it's FQN.
		ILocation location = facility.getLocationById("A1");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "A1");

		// Make sure we can look it up by an alias, and get the mapped location.
		location = facility.getLocationById("AisleA");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "A1");
		
		// Make sure we can still look up a bay by it's FQN.
		location = facility.getSubLocationById("A2.B2");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "B2");
		
		// Make sure we can look it up by an alias, and get the mapped location.
		location = facility.getLocationById("B34");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "B2");
		
		// Make sure we cannot lookup the bad entry (there is no A3).
		location = facility.getLocationById("AisleC");
		Assert.assertNull(location);
		
		

	}
}
