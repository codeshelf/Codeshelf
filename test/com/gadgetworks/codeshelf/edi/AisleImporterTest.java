/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.DomainTestABC;

import com.gadgetworks.codeshelf.model.domain.ILocation;
// domain objects needed
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Facility;

import com.gadgetworks.codeshelf.model.domain.Point;


/**
 * @author ranstrom
 * Also see createAisleTest() in FacilityTest.java
 */
public class AisleImporterTest extends DomainTestABC {

	@Test
	public final void testAisleImporter() {

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,controllerLED,anchorX,anchorY,tubeLightKind, pickFaceEndX, pickFaceEndY\r\n" //
				+ "Aisle,A9,,,TierRight,12.85,43.45,,5.0,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,8,,,,\r\n" //
				+ "Tier,T2,,9,,,,\r\n" //
				+ "Tier,T3,,5,,,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,5,,,,\r\n" //
				+ "Tier,T2,,6,,,,\r\n" //
				+ "Tier,T3,,4,,,,\r\n"; //
	
		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE1");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFromCsvStream(reader, facility, ediProcessTime);

		// Make sure we can still look up an aisle by it's FQN.
		ILocation location = facility.findLocationById("A9");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "A9");


	}

	

}
