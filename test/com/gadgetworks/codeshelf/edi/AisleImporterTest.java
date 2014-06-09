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

import com.gadgetworks.codeshelf.model.domain.ISubLocation;
// domain objects needed
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Point;


/**
 * @author ranstrom
 * Also see createAisleTest() in FacilityTest.java
 */
public class AisleImporterTest extends DomainTestABC {

	@Test
	public final void testTierLeft() {

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm,pickFaceEndX,pickFaceEndY\r\n" //
				+ "Aisle,A9,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,8,80,0,,\r\n" //
				+ "Tier,T2,,9,80,50,,\r\n" //
				+ "Tier,T3,,5,80,100,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,5,80,50,,\r\n" //
				+ "Tier,T2,,6,80,100,,\r\n" //
				+ "Tier,T3,,4,80,150,,\r\n"; //
	
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

		// Check the aisle
		ISubLocation<?> aisle = facility.findLocationById("A9");
		Assert.assertNotNull(aisle);
		Assert.assertEquals(aisle.getDomainId(), "A9");
		
		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle2);
		
		// Not sure if they are really the same reference. However, both implement ISubLocation. DomainObjectABC has an equals override that checks class and persistentId
		Assert.assertEquals(aisle, aisle2);

		// Check the bays
		ISubLocation<?> bay1 = aisle.findLocationById("B1");
		ISubLocation<?> bay2 = aisle.findLocationById("B2");
		Assert.assertNotNull(bay1);
		Assert.assertNotNull(bay2);
		Assert.assertEquals(bay2.getDomainId(), "B2");
		
		// Can the sublocation find mechanism's aisle be used in this manner?
		Bay bayA9B2 = Bay.DAO.findByDomainId(aisle2, "B2");
		Assert.assertNotNull(bayA9B2);

		Bay bayA9B2x = Bay.DAO.findByDomainId(aisle, "B2");
		Assert.assertNotNull(bayA9B2x);

		// Testing how well findByDomainId works. Both bay1 and bay2 have T2 tiers.
		Tier tierB1T2 = Tier.DAO.findByDomainId(bay1, "T2");
		Assert.assertNotNull(tierB1T2);

		Tier tierB2T2 = Tier.DAO.findByDomainId(bay2, "T2");
		Assert.assertNotNull(tierB2T2);

		// These should not be equal because the persistentIds are different
		Assert.assertNotEquals(tierB1T2, tierB2T2);
		
		Slot slotB1T2S3 = Slot.DAO.findByDomainId(tierB1T2, "S3");
		Assert.assertNotNull(slotB1T2S3);

		Slot slotB2T2S3 = Slot.DAO.findByDomainId(tierB2T2, "S3");
		Assert.assertNotNull(slotB2T2S3);
		Assert.assertNotEquals(slotB1T2S3, slotB2T2S3);
		
		// Demonstrate the tier transient field behaviors. As we are refetching tiers via the DAO, the transients are uninitialized
		short ledCount = tierB1T2.getMTransientLedsThisTier();
		// Assert.assertTrue(ledCount == 80); // what we really want to check
		Assert.assertTrue(ledCount == 0);
		
		// Get two more tiers. Then check the tier led values
		Tier tierB1T1 = Tier.DAO.findByDomainId(bay1, "T1");
		Assert.assertNotNull(tierB1T2);
		Tier tierB2T1 = Tier.DAO.findByDomainId(bay2, "T1");
		Assert.assertNotNull(tierB2T1);
		// finalizeTiersInThisAisle() used the transientLeds field in order to figure out the first and last led values
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 80);
		Assert.assertTrue(tierB2T1.getFirstLedNumAlongPath() == 81);
		Assert.assertTrue(tierB2T1.getLastLedNumAlongPath() == 160);
		// should get same values for tier 2
		Assert.assertTrue(tierB1T2.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB1T2.getLastLedNumAlongPath() == 80);
		Assert.assertTrue(tierB2T2.getFirstLedNumAlongPath() == 81);
		Assert.assertTrue(tierB2T2.getLastLedNumAlongPath() == 160);
		
		// Check some slot led values
		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		short firstLed = slotB1T1S1.getFirstLedNumAlongPath();
		short lastLed = slotB1T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(firstLed == 3);
		Assert.assertTrue(lastLed == 9);

		Slot slotB1T1S8 = Slot.DAO.findByDomainId(tierB1T1, "S8");
		firstLed = slotB1T1S8.getFirstLedNumAlongPath();
		lastLed = slotB1T1S8.getLastLedNumAlongPath();
		Assert.assertTrue(firstLed == 73);
		Assert.assertTrue(lastLed == 79);


	}

	@Test
	public final void testTierRight() {

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm,pickFaceEndX,pickFaceEndY\r\n" //
				+ "Aisle,A10,,,,,TierRight,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,0,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,50,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n"; //
	
		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE10");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE10", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE10");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A10");
		Assert.assertNotNull(aisle);
		
		Bay bayA10B1 = Bay.DAO.findByDomainId(aisle, "B1");

		Bay bayA10B2 = Bay.DAO.findByDomainId(aisle, "B2");

		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA10B1, "T2");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA10B2, "T2");
		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA10B1, "T1");
		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA10B2, "T1");

		Slot slotB1T2S3 = Slot.DAO.findByDomainId(tierB1T2, "S3");
		Assert.assertNotNull(slotB1T2S3);

		Slot slotB2T2S3 = Slot.DAO.findByDomainId(tierB2T2, "S3");
		Assert.assertNotNull(slotB2T2S3);
		Assert.assertNotEquals(slotB1T2S3, slotB2T2S3);
		
		// leds should come from the right
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 61);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 120);
		Assert.assertTrue(tierB2T1.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB2T1.getLastLedNumAlongPath() == 60);
		// should get same values for tier 2
		Assert.assertTrue(tierB1T2.getFirstLedNumAlongPath() == 61);
		Assert.assertTrue(tierB1T2.getLastLedNumAlongPath() == 120);
		Assert.assertTrue(tierB2T2.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB2T2.getLastLedNumAlongPath() == 60);

		Slot slotB1T1S6 = Slot.DAO.findByDomainId(tierB1T1, "S6");
		short slotB1T1S6First = slotB1T1S6.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S6First == 63);
		Assert.assertTrue(slotB1T1S6.getLastLedNumAlongPath() == 69);

		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");
		short slotB2T1S1First = slotB2T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1First == 53);
		short slotB2T1S1Last = slotB2T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1Last == 59);

	}

	@Test
	public final void test32Led5Slot() {
		// the purpose is to compare this slotting algorithm to Jeff's hand-done goodeggs zigzag slots
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm,pickFaceEndX,pickFaceEndY\r\n" //
				+ "Aisle,A11,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n"; //
	
		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE11");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE11", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE11");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A11");
		Assert.assertNotNull(aisle);
		
		Bay bayA11B1 = Bay.DAO.findByDomainId(aisle, "B1");

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA11B1, "T1");

		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Slot slotB1T1S2 = Slot.DAO.findByDomainId(tierB1T1, "S2");
		Slot slotB1T1S3 = Slot.DAO.findByDomainId(tierB1T1, "S3");
		Slot slotB1T1S4 = Slot.DAO.findByDomainId(tierB1T1, "S4");
		Slot slotB1T1S5 = Slot.DAO.findByDomainId(tierB1T1, "S5");
		
		// leds should come from the left. (This is not a zigzag bay)
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 32);

		short slotB1T1S1First = slotB1T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S1First == 3);

		short slotB1T1S2First = slotB1T1S2.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S2First == 10);

		short slotB1T1S3First = slotB1T1S3.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S3First == 16);
		Assert.assertTrue(slotB1T1S3.getLastLedNumAlongPath() == 19);

		short slotB1T1S4First = slotB1T1S4.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S4First == 22);
		Assert.assertTrue(slotB1T1S4.getLastLedNumAlongPath() == 25);

		short slotB1T1S5First = slotB1T1S5.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S5First == 28);
		Assert.assertTrue(slotB1T1S5.getLastLedNumAlongPath() == 31);
		
		// So, we see the difference. 
		// Jeff's slots were lit 1-4, 8-11,  15-18, 22-25, 29-32
		// This algorithm lights 3-6, 10-13, 16-19, 22-25, 28-31

	}
	

}
