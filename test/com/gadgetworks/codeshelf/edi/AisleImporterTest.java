/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
// domain objects needed
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.flyweight.command.NetGuid;

/**
 * @author ranstrom
 * Also see createAisleTest() in FacilityTest.java
 */
public class AisleImporterTest extends EdiTestABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AisleImporterTest.class);

	@Test
	public final void testTierB1S1Side() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A9,,,,,tierB1S1Side,12.85,43.45,X,120,\r\n" //
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
		organization.setDomainId("O-AISLE9");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE9", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE9");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check the aisle
		Location aisle = facility.findLocationById("A9");
		Assert.assertNotNull(aisle);
		Assert.assertEquals(aisle.getDomainId(), "A9");

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle2);

		// Not sure if they are really the same reference. However, both implement LocationABC. DomainObjectABC has an equals override that checks class and persistentId
		Assert.assertEquals(aisle, aisle2);

		// Check the bays
		Location bay1 = aisle.findLocationById("B1");
		Location bay2 = aisle.findLocationById("B2");
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
		Assert.assertTrue(ledCount == 80);

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
		Assert.assertTrue(lastLed == 6);

		Slot slotB1T1S8 = Slot.DAO.findByDomainId(tierB1T1, "S8");
		firstLed = slotB1T1S8.getFirstLedNumAlongPath();
		lastLed = slotB1T1S8.getLastLedNumAlongPath();
		Assert.assertTrue(firstLed == 76);
		Assert.assertTrue(lastLed == 79);

		// Check aisle and bay pick face values. (aisle came as a sublocation)
		Double pickFaceEndX = ((Aisle) aisle).getPickFaceEndPosX();
		Double pickFaceEndY = ((Aisle) aisle).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndY == 0.0);
		pickFaceEndX = ((Bay) bay1).getPickFaceEndPosX();
		pickFaceEndY = ((Bay) bay1).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 2.44);
		Assert.assertTrue(pickFaceEndY == 0.0);
		pickFaceEndX = ((Bay) bay2).getPickFaceEndPosX();
		Double bay2EndX = pickFaceEndX;
		pickFaceEndY = ((Bay) bay2).getPickFaceEndPosY();
		// bay 2 should also be 2.44 (relative to its own anchor). Its anchor is relative to parent
		Assert.assertTrue(pickFaceEndX == 2.44);
		Double anchorX = ((Bay) bay2).getAnchorPosX();
		Assert.assertTrue(anchorX == 2.44); // exactly equal to bay1 pickFaceEnd

		Assert.assertTrue(pickFaceEndY == 0.0);
		pickFaceEndX = ((Tier) tierB2T1).getPickFaceEndPosX();
		pickFaceEndY = ((Tier) tierB2T1).getPickFaceEndPosY();
		Assert.assertEquals(pickFaceEndX, bay2EndX); // tier pickface end is relative to its own anchor. As is bay, So they match
		// So, they better not be the same except for single bay aisles or first bay in aisle.
		Assert.assertTrue(pickFaceEndY == 0.0); // x-oriented aisle, so y is zero.
		// however, the slot anchors do progress
		Double slotAnchorX = ((Slot) slotB1T2S3).getAnchorPosX();
		Assert.assertTrue(slotAnchorX > 0.54); // value about ..54m: 3rd starts after 2nd of 9 slots across 244 cm.
		slotAnchorX = ((Slot) slotB2T2S3).getAnchorPosX();

		// Check some vertices. The aisle and each bay should have 4 vertices.
		// aisle defined as an LocationABC. Cannot event cast it to call getVerticesInOrder()
		List<Vertex> vList1 = aisle2.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		// Vertex thirdV = (Vertex) vList1.get(2);

		// New side effect: creates some LED controllers. But if no network in our test system, may not happen
		Assert.assertTrue(facility.countLedControllers() > 0);

		// Check that the locations know which side has lower led numbers.
		// as a tierB1S1Side aisle, tier and slot have lower led number on anchor side.

		Assert.assertTrue(tierB2T1.isLowerLedNearAnchor());
		Assert.assertTrue(slotB1T1S8.isLowerLedNearAnchor());
		// Not so meaningful, but check these
		Assert.assertTrue(bayA9B2.isLowerLedNearAnchor());
		Assert.assertTrue(aisle2.isLowerLedNearAnchor());

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testTierNotB1S1Side() {
		this.getPersistenceService().beginTenantTransaction();

		// Beside tierNotB1S1Side, this as two aisles, so it makes sure both get their leds properly set, and both vertices set
		// Not quite realistic; A10 and A20 are on top of each other. Same anchor point
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A10,,,,,tierNotB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,0,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,50,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n" //
				+ "Aisle,A20,,,,,tierNotB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,0,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE10");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE10", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE10");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		/* getLocationIdToParentLevel gives "" for this. You might argue it should give "F1". 
		 * Originally NPE this case, so determinant result is good. 
		 * Normally calles as this, to the aisle level. */
		String id = facility.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.isEmpty());

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A10");
		Assert.assertNotNull(aisle);

		/* getLocationIdToParentLevel */
		id = aisle.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10"));

		Bay bayA10B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA10B2 = Bay.DAO.findByDomainId(aisle, "B2");

		id = bayA10B1.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1"));

		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA10B1, "T2");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA10B2, "T2");
		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA10B1, "T1");
		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA10B2, "T1");

		id = tierB1T2.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2"));

		// Mostly for code coverage. Does a complex iteration. But not aliases, so will be empty.
		String aliasRange = tierB1T2.getSlotAliasRange();
		Assert.assertTrue(aliasRange.isEmpty());

		Slot slotB1T2S3 = Slot.DAO.findByDomainId(tierB1T2, "S3");
		Assert.assertNotNull(slotB1T2S3);

		id = slotB1T2S3.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2.S3"));

		id = slotB1T2S3.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2.S3"));

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
		Assert.assertTrue(slotB1T1S6.getLastLedNumAlongPath() == 66);

		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");
		short slotB2T1S1First = slotB2T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1First == 56);
		short slotB2T1S1Last = slotB2T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1Last == 59);

		// Check that vertices were computed for not-last aisle
		// Bay vertices in a coordinate system with aisle anchor at 0,0.
		List<Vertex> vList1 = bayA10B1.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2); // each bay has the same depth
		Assert.assertTrue(xValue == 2.44);

		// Aisle vertices in coordinate system with facility anchor at 0,0
		List<Vertex> vList2 = aisle.getVerticesInOrder();
		Assert.assertEquals(vList2.size(), 4);
		thirdV = (Vertex) vList2.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(xValue == 4.88); // two bays of 144 cm.
		Assert.assertTrue(yValue == 1.2); // just the depth, relative to anchor as 0,0

		// Check that led computation occurred for last aisle in the file
		Aisle aisle20 = Aisle.DAO.findByDomainId(facility, "A20");
		Assert.assertNotNull(aisle20);
		Bay bayA20B1 = Bay.DAO.findByDomainId(aisle20, "B1");
		Assert.assertNotNull(bayA20B1);
		Tier tierA20B1T1 = Tier.DAO.findByDomainId(bayA20B1, "T1");
		Assert.assertNotNull(tierA20B1T1);
		Assert.assertTrue(tierA20B1T1.getFirstLedNumAlongPath() != 0); // If something happened for last aisle in the file, then assume the right thing happened

		// Check that vertex computation occurred for last aisle in the file
		List<Vertex> vList3 = bayA20B1.getVerticesInOrder();
		Assert.assertEquals(vList3.size(), 4);
		thirdV = (Vertex) vList3.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2);
		Assert.assertTrue(xValue == 2.44); // this bay is 244 cm wide

		List<Vertex> vList4 = aisle20.getVerticesInOrder();
		Assert.assertEquals(vList4.size(), 4);
		thirdV = (Vertex) vList4.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2);
		Assert.assertTrue(xValue == 4.88);

		// Reread. We had a last bay and last aisle vertices bug on re-read
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		// AislesFileCsvImporter importer2 = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		// new reader, because cannot reset the old reader without handling a possible exception. Same stream, though.
		InputStreamReader reader2 = new InputStreamReader(stream);
		importer.importAislesFileFromCsvStream(reader2, facility, ediProcessTime2);

		// just check second aisle. Need to get it again after the reread as our old reference may not be current
		aisle20 = Aisle.DAO.findByDomainId(facility, "A20");
		List<Vertex> vList5 = aisle20.getVerticesInOrder();
		Assert.assertEquals(vList5.size(), 4);
		thirdV = (Vertex) vList5.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2);
		Assert.assertTrue(xValue == 4.88);

		// Check that the locations know which side has lower led numbers.
		// as a tierNotB1S1Side aisle, tier and slot have higher led number on anchor side.		
		Assert.assertFalse(tierA20B1T1.isLowerLedNearAnchor());
		Assert.assertFalse(slotB1T1S6.isLowerLedNearAnchor());
		// Not so meaningful, but check these
		Assert.assertTrue(bayA10B1.isLowerLedNearAnchor());
		Assert.assertTrue(aisle.isLowerLedNearAnchor());

		this.getPersistenceService().commitTenantTransaction();

	}

	@SuppressWarnings("unused")
	@Test
	public final void test32Led5Slot() {
		this.getPersistenceService().beginTenantTransaction();

		// the purpose of bay B1 is to compare this slotting algorithm to Jeff's hand-done goodeggs zigzag slots
		// the purpose of bay B2 is to check the sort and LEDs of more than 10 slots in a tier
		// the purpose of bays 9,10,11 is check the bay sort.
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A11,,,,,tierB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,12,80,0,,\r\n" //
				+ "Bay,B3,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B4,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B5,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B6,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B7,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B8,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B9,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B10,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B11,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n" //
				+ "Bay,B12,24,,,,,\r\n" //
				+ "Tier,T1,,1,6,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE11");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE11", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE11");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

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
		short slotB1T1S1Last = slotB1T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S1First == 1);
		Assert.assertTrue(slotB1T1S1Last == 4);

		short slotB1T1S2First = slotB1T1S2.getFirstLedNumAlongPath();
		short slotB1T1S2Last = slotB1T1S2.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S2First == 8);
		Assert.assertTrue(slotB1T1S2Last == 11);

		short slotB1T1S3First = slotB1T1S3.getFirstLedNumAlongPath();
		short slotB1T1S3Last = slotB1T1S3.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S3First == 16);
		Assert.assertTrue(slotB1T1S3Last == 19);

		short slotB1T1S4First = slotB1T1S4.getFirstLedNumAlongPath();
		short slotB1T1S4Last = slotB1T1S4.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S4First == 22);
		Assert.assertTrue(slotB1T1S4.getLastLedNumAlongPath() == 25);

		short slotB1T1S5First = slotB1T1S5.getFirstLedNumAlongPath();
		short slotB1T1S5Last = slotB1T1S5.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S5First == 28);
		Assert.assertTrue(slotB1T1S5.getLastLedNumAlongPath() == 31);

		// So, we see the difference. 
		// Jeff's slots were lit 1-4, 8-11,  15-18, 22-25, 29-32
		// This algorithm  (with v4 modification) lights 3-6, 10-13, 16-19, 22-25, 28-31 with 2 guard low, and 1 guard high.
		// With v4 modification lights 1-4, 8-11, 16-19, 22-25, 28-31 with almost any guard parameters
		// 1,1 guards would yield 2-5, 9-12, 16-19, 22-25, 28-31
		// 0,0 would get to 5 lit per slot instead of 4

		Bay bayA11B2 = Bay.DAO.findByDomainId(aisle, "B2");
		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA11B2, "T1");

		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");
		Slot slotB2T1S2 = Slot.DAO.findByDomainId(tierB2T1, "S2");
		Slot slotB2T1S9 = Slot.DAO.findByDomainId(tierB2T1, "S9");
		Slot slotB2T1S10 = Slot.DAO.findByDomainId(tierB2T1, "S10");
		Slot slotB2T1S11 = Slot.DAO.findByDomainId(tierB2T1, "S11");
		// We are just checking the slotsort. Alpha sort would go S1,S10,S11,S2,S9
		// subtract 32 because first zigzag used up 32. So this will give the more familiar answer, with S1 starting to light third LED in this tier
		int firstRelativeLed = slotB2T1S1.getFirstLedNumAlongPath() - 32;
		Assert.assertTrue(firstRelativeLed == 3);
		firstRelativeLed = slotB2T1S2.getFirstLedNumAlongPath() - 32;
		Assert.assertTrue(firstRelativeLed == 10);
		firstRelativeLed = slotB2T1S9.getFirstLedNumAlongPath() - 32;
		Assert.assertTrue(firstRelativeLed == 58);
		firstRelativeLed = slotB2T1S10.getFirstLedNumAlongPath() - 32;
		Assert.assertTrue(firstRelativeLed == 64);
		firstRelativeLed = slotB2T1S11.getFirstLedNumAlongPath() - 32;
		Assert.assertTrue(firstRelativeLed == 70);

		// check the bay sort
		Bay bayA11B3 = Bay.DAO.findByDomainId(aisle, "B3");
		Tier tierB3T1 = Tier.DAO.findByDomainId(bayA11B3, "T1");
		// just showing that we do not set bay first led. Could for zigzags, but not for other types
		// B3 starts at 32 + 80 + 1 = 113.
		// short bayFirstLed = bayA11B3.getFirstLedNumAlongPath(); // throws
		// Assert.assertTrue(bayFirstLed == 0);
		short tierFirstLed = tierB3T1.getFirstLedNumAlongPath();
		Assert.assertTrue(tierFirstLed == 113);
		// making sure that bay 10 is after, and not before T2 which would start at 33.
		Bay bayA11B10 = Bay.DAO.findByDomainId(aisle, "B10");
		Tier tierB10T1 = Tier.DAO.findByDomainId(bayA11B10, "T1");
		tierFirstLed = tierB10T1.getFirstLedNumAlongPath();
		Assert.assertTrue(tierFirstLed == 155);

		this.getPersistenceService().commitTenantTransaction();

	}

	@SuppressWarnings("unused")
	@Test
	public final void testSparseLeds() {
		this.getPersistenceService().beginTenantTransaction();

		// Lasers are sparse: one led per slot
		// Paul tested 8 leds for 5 slots and found bad behavior
		// Other bad ones
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A91,,,,,zigzagB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,5,0,,\r\n" //
				+ "Tier,T2,,5,15,0,,\r\n" //
				+ "Tier,T3,,5,10,0,,\r\n" //
				+ "Tier,T4,,5,12,0,,\r\n" //
				+ "Tier,T5,,5,8,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-SPARSE91");
		mOrganizationDao.store(organization);

		organization.createFacility("F-SPARSE91", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-SPARSE91");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A91");
		Assert.assertNotNull(aisle);

		Bay bayA91B1 = Bay.DAO.findByDomainId(aisle, "B1");

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA91B1, "T1");
		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA91B1, "T2");
		Tier tierB1T3 = Tier.DAO.findByDomainId(bayA91B1, "T3");
		Tier tierB1T4 = Tier.DAO.findByDomainId(bayA91B1, "T4");
		Tier tierB1T5 = Tier.DAO.findByDomainId(bayA91B1, "T5");

		// This is a zigzag bay. T1 is last. This is the "laser" shelf, one "LED" per slot)
		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Slot slotB1T1S2 = Slot.DAO.findByDomainId(tierB1T1, "S2");
		Slot slotB1T1S3 = Slot.DAO.findByDomainId(tierB1T1, "S3");
		Slot slotB1T1S4 = Slot.DAO.findByDomainId(tierB1T1, "S4");
		Slot slotB1T1S5 = Slot.DAO.findByDomainId(tierB1T1, "S5");

		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 46);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 50);

		short slotB1T1S1First = slotB1T1S1.getFirstLedNumAlongPath();
		short slotB1T1S1Last = slotB1T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S1First == 46);
		Assert.assertTrue(slotB1T1S1Last == 46);

		short slotB1T1S2First = slotB1T1S2.getFirstLedNumAlongPath();
		short slotB1T1S2Last = slotB1T1S2.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S2First == 47);
		Assert.assertTrue(slotB1T1S2Last == 47);

		short slotB1T1S3First = slotB1T1S3.getFirstLedNumAlongPath();
		short slotB1T1S3Last = slotB1T1S3.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S3First == 48);
		Assert.assertTrue(slotB1T1S3Last == 48);

		short slotB1T1S4First = slotB1T1S4.getFirstLedNumAlongPath();
		short slotB1T1S4Last = slotB1T1S4.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S4First == 49);
		Assert.assertTrue(slotB1T1S4Last == 49);

		short slotB1T1S5First = slotB1T1S5.getFirstLedNumAlongPath();
		short slotB1T1S5Last = slotB1T1S5.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T1S5First == 50);
		Assert.assertTrue(slotB1T1S5Last == 50);

		// T5 is the 8 Leds over 5 slots shelf, leds 1-8)
		Slot slotB1T5S1 = Slot.DAO.findByDomainId(tierB1T5, "S1");
		Slot slotB1T5S2 = Slot.DAO.findByDomainId(tierB1T5, "S2");
		Slot slotB1T5S3 = Slot.DAO.findByDomainId(tierB1T5, "S3");
		Slot slotB1T5S4 = Slot.DAO.findByDomainId(tierB1T5, "S4");
		Slot slotB1T5S5 = Slot.DAO.findByDomainId(tierB1T5, "S5");

		Assert.assertTrue(tierB1T5.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB1T5.getLastLedNumAlongPath() == 8);

		// Ok if algorithm change affects these slot values. Change the test if reasonable.
		short slotB1T5S1First = slotB1T5S1.getFirstLedNumAlongPath();
		short slotB1T5S1Last = slotB1T5S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T5S1First == 1);
		Assert.assertTrue(slotB1T5S1Last == 1);

		short slotB1T5S2First = slotB1T5S2.getFirstLedNumAlongPath();
		short slotB1T5S2Last = slotB1T5S2.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T5S2First == 3);
		Assert.assertTrue(slotB1T5S2Last == 3);

		short slotB1T5S3First = slotB1T5S3.getFirstLedNumAlongPath();
		short slotB1T5S3Last = slotB1T5S3.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T5S3First == 5);
		Assert.assertTrue(slotB1T5S3Last == 5);

		short slotB1T5S4First = slotB1T5S4.getFirstLedNumAlongPath();
		short slotB1T5S4Last = slotB1T5S4.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T5S4First == 6);
		Assert.assertTrue(slotB1T5S4Last == 6);

		short slotB1T5S5First = slotB1T5S5.getFirstLedNumAlongPath();
		short slotB1T5S5Last = slotB1T5S5.getLastLedNumAlongPath();
		Assert.assertTrue(slotB1T5S5First == 7);
		Assert.assertTrue(slotB1T5S5Last == 7);

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testZigzagB1S1Side() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A12,,,,,zigzagB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE12");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE12", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE12");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A12");
		Assert.assertNotNull(aisle);

		Bay bayA12B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA12B2 = Bay.DAO.findByDomainId(aisle, "B2");

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA12B1, "T1");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA12B2, "T2");

		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");

		Slot slotB2T2S5 = Slot.DAO.findByDomainId(tierB2T2, "S5");

		// leds should come from the top left for this zigzag bay. Third tier down from top starts at 65
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 65);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 96);

		short tierB2T2First = tierB2T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T2First == 129); // fifth tier in the led path, and direction right to left for this tier.
		Assert.assertTrue(tierB2T2.getLastLedNumAlongPath() == 160);

		short slotB1T1S1First = slotB1T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S1First == 65);

		short slotB2T2S5First = slotB2T2S5.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB2T2S5First == 129);
		Assert.assertTrue(slotB2T2S5.getLastLedNumAlongPath() == 132);

		// Test the obvious. For 2 bays, 3 tier, zigzagB1S1Side, tierB1T3 should start at led1. tierB2T3 should start at 97
		Tier tierB1T3 = Tier.DAO.findByDomainId(bayA12B1, "T3");
		Tier tierB2T3 = Tier.DAO.findByDomainId(bayA12B2, "T3");
		short tierB1T3First = tierB1T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T3First == 1);
		short tierB2T3First = tierB2T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T3First == 97);

		// Check that the locations know which side has lower led numbers.
		// as a zigzagB1S1Side aisle, top tier (3) will have tier and slot have lower led number on anchor side. (2) opposite, and tier1 same again.	
		Assert.assertTrue(tierB1T3.isLowerLedNearAnchor());
		Assert.assertFalse(tierB2T2.isLowerLedNearAnchor());
		Assert.assertTrue(tierB1T1.isLowerLedNearAnchor());
		Assert.assertTrue(slotB1T1S1.isLowerLedNearAnchor());
		Assert.assertFalse(slotB2T2S5.isLowerLedNearAnchor());
		// Not so meaningful, but check these
		Assert.assertTrue(bayA12B1.isLowerLedNearAnchor());
		Assert.assertTrue(aisle.isLowerLedNearAnchor());

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testZigzagNotB1S1Side() {
		this.getPersistenceService().beginTenantTransaction();

		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A13,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE13");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE13", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE13");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A13");
		Assert.assertNotNull(aisle);

		Bay bayA13B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA13B2 = Bay.DAO.findByDomainId(aisle, "B2");

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA13B1, "T1");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA13B2, "T2");

		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");

		Slot slotB2T2S5 = Slot.DAO.findByDomainId(tierB2T2, "S5");

		// leds should come from the top right for this zigzag bay. Third tier down on second bay starts at 65
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 161);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 192);

		short tierB2T2First = tierB2T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T2First == 33); // second tier in the led path, and direction left to right for this tier.
		Assert.assertTrue(tierB2T2.getLastLedNumAlongPath() == 64);

		short slotB1T1S1First = slotB1T1S1.getFirstLedNumAlongPath(); // last slot in last tier along the led path
		Assert.assertTrue(slotB1T1S1First == 188);

		short slotB2T2S5First = slotB2T2S5.getFirstLedNumAlongPath(); // last slot in second tier along the led path
		Assert.assertTrue(slotB2T2S5First == 60);
		Assert.assertTrue(slotB2T2S5.getLastLedNumAlongPath() == 63);

		// Check pickface and vertex values. This is where the Y orientation comes in
		Double pickFaceEndX = ((Aisle) aisle).getPickFaceEndPosX();
		Double pickFaceEndY = ((Aisle) aisle).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 0.0);
		pickFaceEndX = ((Bay) bayA13B1).getPickFaceEndPosX();
		pickFaceEndY = ((Bay) bayA13B1).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 0.0);
		Assert.assertTrue(pickFaceEndY == 1.15);

		pickFaceEndX = ((Tier) tierB1T1).getPickFaceEndPosX();
		pickFaceEndY = ((Tier) tierB1T1).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 0.0);
		pickFaceEndX = ((Slot) slotB1T1S1).getPickFaceEndPosX();
		pickFaceEndY = ((Slot) slotB1T1S1).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 0.0); // S1 Y value is about 0.23 (1/5 of 1.15
		pickFaceEndX = ((Slot) slotB2T2S5).getPickFaceEndPosX();
		pickFaceEndY = ((Slot) slotB2T2S5).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndY != 1.15); // S5 is last slot of 115 cm tier. But pickFaceEnd is relative to its own anchor

		// Check some vertices. The aisle and each bay should have 4 vertices.
		// Aisle vertices all in the same coordinate system
		List<Vertex> vList1 = aisle.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		Assert.assertTrue(xValue == 1.2); // Just the depth:120 cm. Relative aisle vertex at the anchor: (0,0)

		Assert.assertTrue(yValue == 2.3); // two bays of 115 cm. Again, relative to anchor as 0,0

		List<Vertex> vList2 = bayA13B1.getVerticesInOrder();
		Assert.assertEquals(vList2.size(), 4);
		thirdV = (Vertex) vList2.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(xValue == 1.2); // each bay has the same depth
		Assert.assertTrue(yValue == 1.15); // this bay is 115 cm wide

		// Test the obvious. For 2 bays, 3 tier, zigzagNotB1S1Side, tierB1T3 should start at led 97. tierB2T3 should start at 1
		Tier tierB1T3 = Tier.DAO.findByDomainId(bayA13B1, "T3");
		Tier tierB2T3 = Tier.DAO.findByDomainId(bayA13B2, "T3");
		short tierB1T3First = tierB1T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T3First == 97);
		short tierB2T3First = tierB2T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T3First == 1);

		// Check that the locations know which side has lower led numbers.
		// as a zigzagNotB1S1Side aisle, top tier (3) will have tier and slot have higher led number on anchor side. (2) opposite, and tier1 same again.	
		Assert.assertFalse(tierB1T3.isLowerLedNearAnchor());
		Assert.assertTrue(tierB2T2.isLowerLedNearAnchor());
		Assert.assertFalse(tierB1T1.isLowerLedNearAnchor());
		Assert.assertFalse(slotB1T1S1.isLowerLedNearAnchor());
		Assert.assertTrue(slotB2T2S5.isLowerLedNearAnchor());
		// Not so meaningful, but check these
		Assert.assertTrue(bayA13B1.isLowerLedNearAnchor());
		Assert.assertTrue(aisle.isLowerLedNearAnchor());

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testMultiAisleZig() {
		this.getPersistenceService().beginTenantTransaction();

		// We seemed to have a bug in the parse where when processing A21 beans, we have m values set for A22. That is, A21 might come out as zigzagNotB1S1Side
		// So this tests Bay to bay attributes changing within an aisle, and tier attributes changing within a bay.

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A21,,,,,zigzagB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,141,,,,,\r\n" //
				+ "Tier,T1,,3,32,0,,\r\n" //
				+ "Tier,T2,,6,32,0,,\r\n" //
				+ "Aisle,A22,,,,,zigzagNotB1S1Side,12.85,53.45,Y,110,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE2X");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE2X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE2X");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A21");
		Assert.assertNotNull(aisle);

		Bay bayA21B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA21B2 = Bay.DAO.findByDomainId(aisle, "B2");

		// For 2 bays, 2 tier, zigzagB1S1Side, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA21B1, "T1");
		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA21B1, "T2");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA21B2, "T2");
		double b1T1FaceEnd = tierB1T1.getPickFaceEndPosX();
		Assert.assertTrue(b1T1FaceEnd == 1.15);
		double b2T2FaceEnd = tierB2T2.getPickFaceEndPosX();
		// Remember, tier in second bay pickface is relative to the bay. It will be about 1.41
		Assert.assertTrue(b2T2FaceEnd < 2.0);

		@SuppressWarnings("rawtypes")
		List<Location> theB1T1Slots = tierB1T1.getActiveChildren();
		Assert.assertTrue(theB1T1Slots.size() == 5);
		@SuppressWarnings("rawtypes")
		List<Location> theB1T2Slots = tierB1T2.getActiveChildren();
		Assert.assertTrue(theB1T2Slots.size() == 4);
		short tierB1T2First = tierB1T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T2First == 1);
		short tierB2T2First = tierB2T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T2First == 65);

		// Aisle 22 should have Y orientation
		Aisle aisle22 = Aisle.DAO.findByDomainId(facility, "A22");
		Assert.assertNotNull(aisle);
		Bay bayA22B1 = Bay.DAO.findByDomainId(aisle22, "B1");
		Tier tierA22B1T1 = Tier.DAO.findByDomainId(bayA22B1, "T1");
		double pickX = tierA22B1T1.getPickFaceEndPosX();
		double pickY = tierA22B1T1.getPickFaceEndPosY();
		// 1.15 + 1.41 = 2.56. But real addition is too precise.
		Assert.assertTrue(pickX == 0.0);
		Assert.assertTrue(pickY == 1.15);

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testBadFile1() {
		this.getPersistenceService().beginTenantTransaction();

		// Ideally, we want non-throwing or caught exceptions that give good user feedback about what is wrong.
		// This has tier before bay, and some other blank fields
		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A14,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" // tier before bay invalidates the rest of this aisle
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,A8,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "xTier,T2,,5,32,0,,\r\n" // invalid binType invalidates the rest of this aisle
				+ "Bay,B3,115,,,,,\r\n" //
				+ "Tier,,,5,32,0,,\r\n" //
				+ "Aisle,A7,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n" // should be T2 here. Invalidates rest of this aisle
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,AB7,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" // Invalid AisleName
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Aisle,A9,,,,,zigzagNotB1S1Side,12.85,43.45,Y,120,\r\n" // ok
				+ "Bay,B1,115,,,,,\r\n"; // ok, even with no tiers

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE14");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE14", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE14");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got from this bad file
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A14");
		Assert.assertNotNull(aisle); // the aisle started ok

		Bay bayA14B2 = Bay.DAO.findByDomainId(aisle, "B2");
		Assert.assertNull(bayA14B2); // bay should have failed for the tier coming first.

		Bay bayA14B3 = Bay.DAO.findByDomainId(aisle, "B3");
		Assert.assertNull(bayA14B3); // bay should have failed for nothing read until next aisle.

		Aisle aisle7 = Aisle.DAO.findByDomainId(facility, "A7");
		Assert.assertNotNull(aisle7); // the aisle started ok. Note that we do not enforce name number consistency on aisles

		Bay bayA7B1 = Bay.DAO.findByDomainId(aisle7, "B1");
		Assert.assertNotNull(bayA7B1); // bay should be ok

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA7B1, "T1");
		Assert.assertNotNull(tierB1T1); // should be there

		Bay bayA7B2 = Bay.DAO.findByDomainId(aisle7, "B2");
		Assert.assertNull(bayA7B2); // will not be there because second T1 in B1 made it skip past rest of this aisle

		Aisle aisleB7 = Aisle.DAO.findByDomainId(facility, "AB7");
		Assert.assertNull(aisleB7); // the aisle name not accepted

		Aisle aisle9 = Aisle.DAO.findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle9); // ok

		Bay bayA9B1 = Bay.DAO.findByDomainId(aisle9, "B1");
		Assert.assertNotNull(bayA9B1); // ok, even with no tiers

		this.getPersistenceService().commitTenantTransaction();

	}

	@SuppressWarnings("unused")
	@Test
	public final void testDoubleFileRead() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,3,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,3,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE15");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE15", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE15");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle);
		Bay bayA15B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1);
		Slot slotB1T1S3 = Slot.DAO.findByDomainId(tierB1T1, "S3");
		Assert.assertNotNull(slotB1T1S3);
		Double s1InitialMetersAlongPath = slotB1T1S1.getPosAlongPath();
		Double s3InitialMetersAlongPath = slotB1T1S3.getPosAlongPath();

		// Act like "oops, forgot the second tier". 
		// And change from 3 slots down to 6. 
		// And change to 50 leds across the tier
		// And change leds to tierB1S1Side
		// And change bay length
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,122,,,,,\r\n" //
				+ "Tier,T1,,6,50,0,,\r\n" //
				+ "Tier,T2,,6,50,0.8,,\r\n" //
				+ "Bay,B2,122,,,,,\r\n" //
				+ "Tier,T1,,6,50,0,,\r\n" //
				+ "Tier,T2,,6,50,0.8,,\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer2 = createAisleFileImporter();
		importer2.importAislesFileFromCsvStream(reader2, facility, ediProcessTime2);

		aisle = Aisle.DAO.findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle);

		bayA15B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA15B2 = Bay.DAO.findByDomainId(aisle, "B2");
		Assert.assertNotNull(bayA15B2);
		Double baylength = bayA15B1.getPickFaceEndPosY() - bayA15B1.getAnchorPosY(); // this aisle is Y orientation
		Assert.assertTrue(baylength > 1.20); // Bay 1 values were updated
		// Compiler warning on equality of double. (== 1.22) so lets use > as the old value was 1.15

		tierB1T1 = Tier.DAO.findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1); // should still exist

		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA15B2, "T2");
		Assert.assertNotNull(tierB2T2); // Shows that we reread and this time created T2

		slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1); // should still exist
		Slot slotB1T1S5 = Slot.DAO.findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5); // should still exist
		Slot slotB1T1S6 = Slot.DAO.findByDomainId(tierB1T1, "S6");
		Assert.assertNotNull(slotB1T1S6); // Shows that we reread and this time created S6

		short tierB1T1Last = tierB1T1.getLastLedNumAlongPath(); // did the tier LEDs change?
		Assert.assertTrue(tierB1T1Last == 50); // Show that LEDs were recomputed and updated

		slotB1T1S3 = Slot.DAO.findByDomainId(tierB1T1, "S3");
		Double s1SubsequentMetersAlongPath = slotB1T1S1.getPosAlongPath();
		Double s3SubsequentMetersAlongPath = slotB1T1S3.getPosAlongPath();

		// meters along path should have changed for S3, but not for S1
		// This would work if we had a path in this unit test. But we don't. So the value of these doubles is null.
		//		Assert.assertEquals(s1InitialMetersAlongPath, s1SubsequentMetersAlongPath);
		//		Assert.assertNotEquals(s3InitialMetersAlongPath, s3SubsequentMetersAlongPath);

		// And the third read, that should (but won't yet) delete extras. 
		// Delete one slot in a tier. 
		// Delete one tier in a bay
		// Delete one bay in the aisle
		// Make the bay shorter,so aisle vertices should be less
		String csvString3 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,110,,,,,\r\n" //
				+ "Tier,T1,,4,50,0,,\r\n"; //

		byte[] csvArray3 = csvString3.getBytes();

		ByteArrayInputStream stream3 = new ByteArrayInputStream(csvArray3);
		InputStreamReader reader3 = new InputStreamReader(stream3);

		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer3 = createAisleFileImporter();
		importer3.importAislesFileFromCsvStream(reader3, facility, ediProcessTime3);

		// Check what we got
		Aisle aisle3 = Aisle.DAO.findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle3);

		bayA15B1 = Bay.DAO.findByDomainId(aisle3, "B1");
		bayA15B2 = Bay.DAO.findByDomainId(aisle3, "B2");
		Assert.assertNotNull(bayA15B2); // Incorrect! We want B2 to be null or somehow retired

		tierB1T1 = Tier.DAO.findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1); // should still exist

		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA15B1, "T2");
		Assert.assertNotNull(tierB1T2); // Incorrect! We want T2 to be null or somehow retired

		slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1); // should still exist
		slotB1T1S5 = Slot.DAO.findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5); // Incorrect! We want T2 to be null or somehow retired

		List<Vertex> vList1 = aisle3.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		// Assert.assertTrue(yValue == 1.1); // new bay width 110 cm. But aisle is coming as 2.3 which is the original 2 bay value

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testAfterFileModifications() {
		this.getPersistenceService().beginTenantTransaction();

		// The file read does a lot. But then we rely on the user via the UI to do additional things to complete the configuration. This is
		// a (nearly) end to end test of that. The actual UI will call a websocket command that calls a method on a domain object.
		// This test calls the same methods.

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A16,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE16");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE16", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE16");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the objects we will use
		Aisle aisle16 = Aisle.DAO.findByDomainId(facility, "A16");
		Assert.assertNotNull(aisle16);

		Bay bayA16B1 = Bay.DAO.findByDomainId(aisle16, "B1");
		Bay bayA16B2 = Bay.DAO.findByDomainId(aisle16, "B2");
		Assert.assertNotNull(bayA16B2);

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA16B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA16B2, "T1");
		Assert.assertNotNull(tierB2T1);

		// Get our network so that we may add a network controller
		List<CodeshelfNetwork> networks = facility.getNetworks();
		int howManyNetworks = networks.size();
		Assert.assertTrue(howManyNetworks == 1);

		// organization.createFacility() should have created this network
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		Assert.assertNotNull(network);

		// There are led controllers, but we will make a new one. If it exists already, no harm.
		String cntlrId = "000026";
		String guidId = "0x000026";
		LedController ledController = network.findOrCreateLedController(cntlrId, new NetGuid(guidId));
		Assert.assertNotNull(ledController);
		LedController aController = network.getLedController(cntlrId); // make sure we can get it as we might
		Assert.assertNotNull(aController);
		UUID cntlrPersistID = aController.getPersistentId();
		String cntrlPersistIdStr = cntlrPersistID.toString();

		// just a null test of getEffectiveXXX before any controller and channel are set. Will go up the parent chain
		Slot slotB1T1S1 = Slot.DAO.findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1);
		Assert.assertNull(slotB1T1S1.getLedController());
		Assert.assertNull(slotB1T1S1.getLedChannel());

		// Now the real point. UI will call as follows to set all of T1 in the aisle to this controller.
		// Side effect if channel not set is to set to channel 1 also. This was temporarily important before our current controller plus channel dialog.
		tierB1T1.setControllerChannel(cntrlPersistIdStr, "0", "aisle");
		Short b1T1Channel = tierB1T1.getLedChannel();
		Short b2T1Channel = tierB2T1.getLedChannel();
		Assert.assertTrue(b1T1Channel == (short) 1);
		// Assert.assertTrue(b2T1Channel == (short) 1); bug? Bizarre. This is DEV-165.
		// b2T1Channel is still uninitialized, even though it definitely got set, but not on this reference. Let's re-get the tier.
		tierB2T1 = Tier.DAO.findByDomainId(bayA16B2, "T1");
		b2T1Channel = tierB2T1.getLedChannel(); // need to get this again from the re-hydrated object
		Assert.assertTrue(b2T1Channel == (short) 1);

		LedController b1T1Controller = tierB1T1.getLedController();
		LedController b2T1Controller = tierB2T1.getLedController(); // This needed the re-get also
		Assert.assertEquals(b1T1Controller, b2T1Controller); // different ebeans reference, but same persistent ID should match on equals

		String b1T1ControllerStr = tierB1T1.getLedControllerId();
		String b2T1ControllerStr = tierB2T1.getLedControllerId();
		Assert.assertEquals(b2T1ControllerStr, b1T1ControllerStr); // strings match; both "0x000026"

		// test getEffective controller and channel.  The get will not have anything, but getEffective will go up the parent chain.
		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");
		Assert.assertNotNull(slotB2T1S1);
		Assert.assertNull(slotB2T1S1.getLedController());
		Assert.assertNull(slotB2T1S1.getLedChannel());
		Assert.assertEquals(b2T1Controller, slotB2T1S1.getEffectiveLedController());
		Assert.assertEquals(b2T1Channel, slotB2T1S1.getEffectiveLedChannel());

		// New from v8. Setting controller on aisle should clear the earlier tier set.
		String cntlrId66 = "000066";
		String guidId66 = "0x000066";
		LedController ledController66 = network.findOrCreateLedController(cntlrId66, new NetGuid(guidId66));
		String cntlrId55 = "000055";
		String guidId55 = "0x000055";
		LedController ledController55 = network.findOrCreateLedController(cntlrId55, new NetGuid(guidId55));
		UUID cntlrPersistID55 = ledController55.getPersistentId();
		String cntrlPersistIdStr55 = cntlrPersistID55.toString();

		Assert.assertNotNull(ledController66);
		LedController aController66 = network.getLedController(cntlrId66); // make sure we can get it as we might
		Assert.assertNotNull(aController66);
		UUID cntlrPersistID66 = aController66.getPersistentId();
		String cntrlPersistIdStr2 = cntlrPersistID66.toString();
		// verify we have something on the tier
		Assert.assertNotNull(tierB1T1.getLedController());
		Assert.assertNotNull(tierB1T1.getLedChannel());
		// set the aisle, then make sure tier got cleared and tier getEffectiveXXX() works
		aisle16.setControllerChannel(cntrlPersistIdStr2, "2");

		// DEV-514 investigation tierB1T1 is old reference to tier. Does it know its controller/channel immediately after the aisle set it in the same transaction space?
		// Yes! ebean would have failed the following
		Assert.assertNull(tierB1T1.getLedController());
		Assert.assertNull(tierB1T1.getLedChannel());
		Assert.assertEquals(ledController66, tierB1T1.getEffectiveLedController());
		Assert.assertTrue(tierB1T1.getEffectiveLedChannel() == 2);

		// DEV-514: a different kind of issue with hibernate. findByDomainId does not go to the database.  If you asked the database, 
		// tierB1T1.getLedController()) would not be null.
		tierB1T1 = Tier.DAO.findByDomainId(bayA16B1, "T1");
		Assert.assertNull(tierB1T1.getLedController());
		Assert.assertNull(tierB1T1.getLedChannel());
		Assert.assertEquals(ledController66, tierB1T1.getEffectiveLedController());
		Assert.assertTrue(tierB1T1.getEffectiveLedChannel() == 2);

		// DEV-514 Let's persist now. tierB1T1 reference comes from the previous. As does aisle16 reference.
		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		// set on the old aisle reference. Does the old tier reference know?
		aisle16.setControllerChannel(cntrlPersistIdStr55, "1");
		// These fail!
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		// Assert.assertTrue(tierB1T1.getEffectiveLedChannel() == 1);
		// Get from DB again under this transaction. However, facility is old reference. Ok? No!
		aisle16 = Aisle.DAO.findByDomainId(facility, "A16");
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		// There is no way in this test structure to re-get the facility from the database under a new transaction.
		// aisle16 = Aisle.DAO.findByDomainId(getFacility(), "A16");
		// similar problem: still the old facility reference.
		tierB1T1 = (Tier) facility.findSubLocationById("A16.B1.T1");
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		List<Facility> aList = Facility.DAO.getAll();
		Facility facility2 = aList.get(0);
		tierB1T1 = (Tier) facility2.findSubLocationById("A16.B1.T1");
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails

		// Set it back to pass the rest of the test.
		aisle16.setControllerChannel(cntrlPersistIdStr2, "2");
		// and the new v8 UI fields
		// Aisle has the direct association, so no parenthesis around it. Tier is indirect via getEffective.
		String aisleCntrlUiField = aisle16.getLedControllerIdUi();
		String aisleChannelUiField = aisle16.getLedChannelUi();
		Assert.assertEquals("000066", aisleCntrlUiField);
		Assert.assertEquals("2", aisleChannelUiField);
		String tierCntrlUiField = tierB1T1.getLedControllerIdUi();
		String tierChannelUiField = tierB1T1.getLedChannelUi();
		Assert.assertEquals("(000066)", tierCntrlUiField);
		Assert.assertEquals("(2)", tierChannelUiField);

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testNoLed() {
		this.getPersistenceService().beginTenantTransaction();

		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A21,,,,,tierNotB1S1Side,12.85,23.45,Y,240,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,5,0,0,,\r\n" //
				+ "Tier,T2,,5,0,0,,\r\n" //
				+ "Tier,T3,,5,0,0,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,1,0,0,,\r\n" //
				+ "Tier,T2,,1,0,0,,\r\n" //
				+ "Tier,T3,,1,0,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE21");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE21", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE21");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle21 = Aisle.DAO.findByDomainId(facility, "A21");
		Assert.assertNotNull(aisle21);

		Bay bayA21B1 = Bay.DAO.findByDomainId(aisle21, "B1");
		Bay bayA21B2 = Bay.DAO.findByDomainId(aisle21, "B2");

		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA21B2, "T1");
		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA21B1, "T2");

		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");

		Slot slotB1T2S5 = Slot.DAO.findByDomainId(tierB1T2, "S5");

		// leds should be zero
		Short ledValue1 = tierB2T1.getFirstLedNumAlongPath();
		Assert.assertTrue(ledValue1 == 0);
		Short ledValue2 = slotB1T2S5.getLastLedNumAlongPath();
		Assert.assertTrue(ledValue2 == 0);

		Short ledValue3 = slotB2T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(ledValue3 == 0);

		this.getPersistenceService().commitTenantTransaction();

	}

	private Double helperGetPosAlongSegment(PathSegment inSegment, Double inX, Double inY) {
		Point testPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, inX, inY, 0.0);
		return inSegment.computeNormalizedPositionAlongPath(testPoint);
	}

	@Test
	public final void testPathCreation() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setDomainId("O4X");
		mOrganizationDao.store(organization);

		organization.createFacility("F4X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F4X");

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.0, 12.0, 48.0);
		PathSegment segment1 = addPathSegmentForTest(aPath, 1, 12.0, 48.0, 12.0, 58.0);
		SortedSet<PathSegment> segments = aPath.getSegments();
		int countSegments = segments.size();
		Assert.assertTrue(countSegments == 2);

		// Path aPath2 = Path.DAO.findByDomainId(facility, "F4X.1");  does not work
		Path aPath2 = facility.getPath("F4X.1");
		Assert.assertNotNull(aPath2);

		List<Path> paths = facility.getPaths();
		int countPaths = paths.size();
		Assert.assertTrue(countPaths == 1);

		// Take this chance to unit test the path segment position calculator for X oriented segment.
		Double posAlongPath = segment0.getStartPosAlongPath();
		Assert.assertEquals(posAlongPath, (Double) 0.0);

		// case 1: beyond the start. Give the path segment's starting value. In this case zero.
		Double value = helperGetPosAlongSegment(segment0, 25.0, 43.45);
		Assert.assertEquals(value, (Double) 0.0);
		// case 2: at the start
		value = helperGetPosAlongSegment(segment0, 22.0, 43.45);
		Assert.assertEquals(value, (Double) 0.0);
		// case 3: usual situation. Along the segment. In this case, 4.0 meters along from start
		value = helperGetPosAlongSegment(segment0, 18.0, 43.45);
		Assert.assertEquals(value, (Double) 4.0);
		// case 4: at the end
		value = helperGetPosAlongSegment(segment0, 12.0, 43.45);
		Assert.assertEquals(value, (Double) 10.0);
		// case 5: beyond the end. Give the path segment's ending value, or at least the x component of it.
		value = helperGetPosAlongSegment(segment0, 8.0, 43.45);
		Assert.assertEquals(value, (Double) 10.0);

		// And the Y oriented segment. Note that the Y start with distance 10.00 along path
		posAlongPath = segment1.getStartPosAlongPath();
		Assert.assertEquals(posAlongPath, (Double) 10.0);

		// case 1: beyond the start. Give the path segment's starting value. In this case zero.
		value = helperGetPosAlongSegment(segment1, 25.0, 43.45);
		Assert.assertEquals(value, (Double) 10.0);
		// case 2: at the start
		value = helperGetPosAlongSegment(segment1, 25.0, 48.0);
		Assert.assertEquals(value, (Double) 10.0);
		// case 3: usual situation. Along the segment. In this case, 4.0 meters along from start
		value = helperGetPosAlongSegment(segment1, 25.0, 55.0);
		Assert.assertEquals(value, (Double) 17.0);
		// case 4: at the end
		value = helperGetPosAlongSegment(segment1, 25.0, 58.0);
		Assert.assertEquals(value, (Double) 20.0);
		// case 5: beyond the end. Give the path segment's ending value, or at least the x component of it.
		value = helperGetPosAlongSegment(segment1, 25.0, 62.0);
		Assert.assertEquals(value, (Double) 20.0);

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void simplestPathTest() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE5X");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE5X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE5X");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get A31
		Aisle aisle51 = Aisle.DAO.findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.00, 48.45);

		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();

		String persistStr = segment0.getPersistentId().toString();
		aisle51.associatePathSegment(persistStr);
		// This should have recomputed all positions along path.  Aisle, bay, tier, and slots should ahve position now
		// Although the old reference to aisle before path association would not.

		Bay bayA51B1 = Bay.DAO.findByDomainId(aisle51, "B1");
		Tier tierA51B1T1 = Tier.DAO.findByDomainId(bayA51B1, "T1");
		Slot slotS1 = Slot.DAO.findByDomainId(tierA51B1T1, "S1");
		Assert.assertNotNull(slotS1);
		Slot slotS4 = Slot.DAO.findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotS4);

		Double bayMeters = bayA51B1.getPosAlongPath();
		Double tierMeters = tierA51B1T1.getPosAlongPath();
		Assert.assertNotNull(bayMeters);
		Assert.assertEquals(bayMeters, tierMeters); // tier spans the bay, so should be the same
		Double slot1Meters = slotS1.getPosAlongPath();
		Assert.assertNotNull(slot1Meters);
		Double slot4Meters = slotS4.getPosAlongPath();
		Assert.assertNotNull(slot4Meters);

		Assert.assertNotEquals(slot1Meters, slot4Meters); // one of these should be further along the path
		Assert.assertTrue(slot1Meters > slot4Meters); // path goes right to left, so S4 lowest.

		this.getPersistenceService().commitTenantTransaction();

	}

	@SuppressWarnings({ "unused" })
	@Test
	public final void testPath() {
		this.getPersistenceService().beginTenantTransaction();

		// We seemed to have a bug in the parse where when processing A21 beans, we have m values set for A22. That is, A21 might come out as zigzagNotB1S1Side
		// This also tests Bay to bay attributes changing within A31.

		// The main point is testing path relationship if we run one path between the aisles. (right to left)
		// B1/S1 is always by the anchor. All values go positive from there. 
		// Lowest path values at A31B2T1S5 and A32B2T1S5
		// For the LED, Lowest LED values should be A31B1T1S1 and A32B2T1S5
		//

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A31,,,,,zigzagB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,141,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,A32,,,,,zigzagNotB1S1Side,12.85,53.45,X,110,N\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-3X");
		mOrganizationDao.store(organization);

		organization.createFacility("F3X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F3X");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get A31
		Aisle aisle31 = Aisle.DAO.findByDomainId(facility, "A31");
		Assert.assertNotNull(aisle31);

		Bay bayA31B1 = Bay.DAO.findByDomainId(aisle31, "B1");
		Bay bayA31B2 = Bay.DAO.findByDomainId(aisle31, "B2");

		// For 2 bays, 2 tier, zigzagB1S1Side, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierA31B1T1 = Tier.DAO.findByDomainId(bayA31B1, "T1");
		Tier tierA31B2T1 = Tier.DAO.findByDomainId(bayA31B2, "T1");

		// Get A32
		Aisle aisle32 = Aisle.DAO.findByDomainId(facility, "A32");
		Assert.assertNotNull(aisle32);
		Bay bayA32B1 = Bay.DAO.findByDomainId(aisle32, "B1");
		Bay bayA32B2 = Bay.DAO.findByDomainId(aisle32, "B2");
		Tier tierA32B1T1 = Tier.DAO.findByDomainId(bayA32B1, "T1");
		Tier tierA32B2T1 = Tier.DAO.findByDomainId(bayA32B2, "T1");

		// There is no path yet. Try to find positionAlongPath values.
		Double tierA32B1T1Value = tierA32B1T1.getPosAlongPath();
		Assert.assertNull(tierA32B1T1Value);

		// Now Pathing. Simulate UI doing  path between the aisles, right to left.
		// For A31, B2 will be at the start of the path. And pos along path should be about the same for pairs of slots from A31 and A32
		// For A32, B1 will be at the start of the path

		Path aPath = createPathForTest(facility);

		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		// Mostly check the parent relationship these 4 lines
		TravelDirectionEnum direction1 = aPath.getTravelDir();
		Assert.assertEquals(direction1, TravelDirectionEnum.FORWARD);
		TravelDirectionEnum direction2 = segment0.getParent().getTravelDir();
		Assert.assertEquals(direction2, TravelDirectionEnum.FORWARD);

		Path retrievedPath = facility.getPath("F3X.1");
		Assert.assertEquals(aPath, retrievedPath);

		List<Path> paths = facility.getPaths();
		Assert.assertEquals(1, paths.size());
		Assert.assertEquals(aPath, segment0.getParent());
		Assert.assertEquals(1, paths.size());

		UUID retrievedPathID = retrievedPath.getPersistentId();
		// Then we need to associate the aisles to the path segment. Use the same function as the UI does
		String segmentId = segment0.getPersistentId().toString();
		UUID facilityID = facility.getPersistentId();
		aisle31.associatePathSegment(segmentId);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		checkLocations(facilityID, retrievedPathID, "F3X.1.0", aisle31);

		// If you step into associatePathSegment, you will see that it finds the segment by UUID, and its location count was 1 and goes to 2.
		aisle32.associatePathSegment(segmentId);

		checkLocations(facilityID, retrievedPathID, "F3X.1.0", aisle31, aisle32);

		// Lowest path values at A31B2T1S5 and A32B2T1S5
		// Lowest LED values should be A31B1T1S1 and A32B2T1S5
		Slot firstA31SlotOnPath = Slot.DAO.findByDomainId(tierA31B2T1, "S5");
		Slot firstA32SlotOnPath = Slot.DAO.findByDomainId(tierA32B2T1, "S5");
		Slot lastA31SlotOnPath = Slot.DAO.findByDomainId(tierA31B1T1, "S1");
		Slot lastA32SlotOnPath = Slot.DAO.findByDomainId(tierA32B2T1, "S1");

		Double valueFirst31 = firstA31SlotOnPath.getPosAlongPath();
		Double valueFirst32 = firstA32SlotOnPath.getPosAlongPath();
		Double valueLast31 = lastA31SlotOnPath.getPosAlongPath();
		Double valueLast32 = lastA32SlotOnPath.getPosAlongPath();

		Short lowestLEDforA32 = firstA32SlotOnPath.getFirstLedNumAlongPath();
		Assert.assertTrue(lowestLEDforA32 < 4);
		Slot lowestA31LedSLot = Slot.DAO.findByDomainId(tierA31B1T1, "S1");
		Short lowestLEDforA31 = lowestA31LedSLot.getFirstLedNumAlongPath();
		Assert.assertTrue(lowestLEDforA31 < 4);

		// old bug got the same position for first two bays. Check that.
		Double a31b1Value = tierA31B1T1.getPosAlongPath();
		Double a31b2Value = tierA31B2T1.getPosAlongPath();
		// Values are null. Lets get new tier references after the path was applied, as the posAlongPath is null on old reference
		tierA31B1T1 = Tier.DAO.findByDomainId(bayA31B1, "T1");
		tierA31B2T1 = Tier.DAO.findByDomainId(bayA31B2, "T1");
		tierA32B1T1 = Tier.DAO.findByDomainId(bayA32B1, "T1");
		tierA32B2T1 = Tier.DAO.findByDomainId(bayA32B2, "T1");

		Double valueTierA31B1T1 = tierA31B1T1.getAnchorPosX();
		Double valueTierA31B2T1 = tierA31B2T1.getAnchorPosX();
		Double valueTierA32B1T1 = tierA32B1T1.getAnchorPosX();
		Double valueTierA32B2T1 = tierA32B2T1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueTierA31B1T1);
		Assert.assertEquals((Double) 0.0, valueTierA32B2T1);

		// Slots increase the same way in A32. So S1 anchor will always be 0 and S5 anchor will not
		Slot slotA31B1T1S1 = Slot.DAO.findByDomainId(tierA31B1T1, "S1");
		Slot slotA31B1T1S5 = Slot.DAO.findByDomainId(tierA31B1T1, "S5");
		Slot slotA31B2T1S1 = Slot.DAO.findByDomainId(tierA31B2T1, "S1");
		Slot slotA31B2T1S5 = Slot.DAO.findByDomainId(tierA31B2T1, "S5");
		Slot slotA32B1T1S1 = Slot.DAO.findByDomainId(tierA32B1T1, "S1");
		Slot slotA32B1T1S5 = Slot.DAO.findByDomainId(tierA32B1T1, "S5");
		Slot slotA32B2T1S1 = Slot.DAO.findByDomainId(tierA32B2T1, "S1");
		Slot slotA32B2T1S5 = Slot.DAO.findByDomainId(tierA32B2T1, "S5");

		Double valueSlotA31B1T1S1 = slotA31B1T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA31B1T1S1); // first slot in A31
		Double valueSlotA31B1T1S5 = slotA31B1T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA31B1T1S5);
		Double valueSlotA31B2T1S1 = slotA31B2T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA31B2T1S1);
		Double valueSlotA31B2T1S5 = slotA31B2T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA31B2T1S5);

		Double valueSlotA32B1T1S1 = slotA32B1T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA32B1T1S1); // first slot in A32
		Double valueSlotA32B1T1S5 = slotA32B1T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA32B1T1S5);
		Double valueSlotA32B2T1S1 = slotA32B2T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA32B2T1S1);
		Double valueSlotA32B2T1S5 = slotA32B2T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA32B2T1S5);

		// As the path goes right to left between A31 and A32,
		// lowest values for meters along path are at A31B2T1S5 and A32B1T1S5
		Double slotA31B2T1S5Value = slotA31B2T1S5.getPosAlongPath();
		Double slotA31B2T1S1Value = slotA31B2T1S1.getPosAlongPath();
		Assert.assertTrue(slotA31B2T1S1Value > slotA31B2T1S5Value); // S1 further along path than S5 in any tier. (if path runs right to left there)

		Double slotA31B1T1S5Value = slotA31B1T1S5.getPosAlongPath();
		Double slotA31B1T1S1Value = slotA31B1T1S1.getPosAlongPath();
		Assert.assertTrue(slotA31B1T1S1Value > slotA31B1T1S5Value); // S1 further in B1 also

		Assert.assertTrue(slotA31B1T1S5Value > slotA31B2T1S1Value); // first bay last slot further along path than second bay first slot

		// lowest at A32B1T1S5
		Double slotA32B1T1S5Value = slotA32B1T1S5.getPosAlongPath();
		Double slotA32B1T1S1Value = slotA32B1T1S1.getPosAlongPath();
		Assert.assertTrue(slotA32B1T1S5Value < slotA32B1T1S1Value); // in A32 also,first bay last slot further along path than second bay first slot

		this.getPersistenceService().commitTenantTransaction();

	}

	protected void checkLocations(UUID facilityID, UUID retrievedPathID, String segmentDomainId, Aisle... locations) {
		Path retrievedPath;
		retrievedPath = Path.DAO.findByPersistentId(retrievedPathID);
		// this segment should have one location now. However, the old reference is stale and may know its aisles (used to be). Re-get
		PathSegment retrievedSegment = PathSegment.DAO.findByDomainId(retrievedPath, segmentDomainId);
		Assert.assertEquals(Arrays.asList(locations), retrievedSegment.getLocations());

		// Let's check locations on the path segment, derived different ways
		// original aPath return while created:
		PathSegment memberSegment = retrievedPath.getPathSegment(0);
		Assert.assertEquals(memberSegment, retrievedSegment);
		Assert.assertEquals(Arrays.asList(locations), memberSegment.getLocations());

		//From the facility now (after associating aisle to path segment)
		Facility retrievedFacility = Facility.DAO.findByPersistentId(facilityID);
		Path memberPath = retrievedFacility.getPath("F3X.1");
		Assert.assertNotNull(memberPath);
		PathSegment memberPathFirstSegment = memberPath.getPathSegment(0);
		Assert.assertNotNull(memberPathFirstSegment);
		Assert.assertEquals(memberSegment, memberPathFirstSegment);
		Assert.assertEquals(Arrays.asList(locations), memberPathFirstSegment.getLocations());
	}

	@SuppressWarnings("unused")
	@Test
	public final void nonSlottedTest() {
		this.getPersistenceService().beginTenantTransaction();

		// For tier-wise non-slotted inventory, we will support the same file format, but with zero tiers.

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A61,,,,,tierNotB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				// + "\r\n" // blank line in the middle
				+ "Tier,T1,,0,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,0,32,0,,\r\n" //
				+ "\r\n" //
				+ "\r\n"; // extra blank lines, just to see if they cause trouble

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE6X");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE6X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE6X");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle61 = Aisle.DAO.findByDomainId(facility, "A61");
		Assert.assertNotNull(aisle61);

		Path aPath = createPathForTest(facility);
		// this path goes from right to left, and should easily extend beyond the aisle boundaries.
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 10.85, 48.45);
		// let's check that assumption.
		Double segmentLeftMostX = segment0.getEndPosX();
		Double segmentRightMostX = segment0.getStartPosX();
		Double aisleAnchorX = aisle61.getAnchorPosX();
		Double aislePickEndX = aisle61.getPickFaceEndPosX();
		Double aisleCorrectedEndX = aisleAnchorX + aislePickEndX;
		Assert.assertTrue(segmentLeftMostX < aisleAnchorX);
		Assert.assertTrue(segmentRightMostX > aisleCorrectedEndX);

		String persistStr = segment0.getPersistentId().toString();

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		aisle61.associatePathSegment(persistStr);
		// This should have recomputed all positions along path.  Aisle, bay, tier, and slots should have position now
		// Although the old reference to aisle before path association would not.

		aisle61 = Aisle.DAO.findByDomainId(facility, "A61");
		Bay bayA61B1 = Bay.DAO.findByDomainId(aisle61, "B1");
		Bay bayA61B2 = Bay.DAO.findByDomainId(aisle61, "B2");

		// Check some of the functions called by computePosAlongPath
		Point aisle61AnchorPoint = aisle61.getAbsoluteAnchorPoint();
		Point bay1AnchorPoint = bayA61B1.getAbsoluteAnchorPoint();
		Point bay2AnchorPoint = bayA61B2.getAbsoluteAnchorPoint();
		// replicating the pickEnd logic
		Point bay1PickFaceEndPoint = bayA61B1.getAbsolutePickFaceEndPoint();
		Point bay2PickFaceEndPoint = bayA61B2.getAbsolutePickFaceEndPoint();
		// Manual check here. The bay1 and bay2 points are correct.		

		// pickface end values are critical. This simple test should cover in a non-confusing way.
		// Aisle anchor is 12.85,43.45
		String aislePickEnd = aisle61.getPickFaceEndPosXui();
		Assert.assertEquals(aislePickEnd, "2.3"); // Two bays long, 115 cm each
		String bay1PickEnd = bayA61B1.getPickFaceEndPosXui();
		Assert.assertEquals(bay1PickEnd, "1.15"); // Two bays long, 115 cm each
		String bay2PickEnd = bayA61B2.getPickFaceEndPosXui();
		Assert.assertEquals(bay2PickEnd, "1.15"); // Two bays long, 115 cm each
		// Depending on which way the path is going, the aisle's posAlongPath will match the first or last bay (and tiers therein)
		String aislePosAlongPath = aisle61.getPosAlongPathui();
		String bay1PosAlongPath = bayA61B1.getPosAlongPathui();
		String bay2PosAlongPath = bayA61B2.getPosAlongPathui();
		Assert.assertNotEquals(bay1PosAlongPath, bay2PosAlongPath);
		Assert.assertEquals(aislePosAlongPath, bay2PosAlongPath);

		Tier tierA61B1T1 = Tier.DAO.findByDomainId(bayA61B1, "T1");
		Assert.assertNotNull(tierA61B1T1);
		Tier tierA61B2T1 = Tier.DAO.findByDomainId(bayA61B2, "T1");
		Assert.assertNotNull(tierA61B2T1);
		Slot slotS1 = Slot.DAO.findByDomainId(tierA61B1T1, "S1");
		Assert.assertNull(slotS1); // no slots

		String tierB1Meters = tierA61B1T1.getPosAlongPathui();
		String tierB2Meters = tierA61B2T1.getPosAlongPathui();
		Assert.assertNotEquals(tierB1Meters, tierB2Meters); // tier spans the bay, so should be the same
		// Bay1 and bay2 path position differ by about 1.15 meters;  bay is 115 cm long.

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void testMultipleReferencesForSimpleField() {
		// DAO-correct
		// This was one common ebeans bug. This test gets references to the same slot at different times, and sees that they are all synchronized.

		this.getPersistenceService().beginTenantTransaction();
		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A29,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE29");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE29", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE29");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the objects we will use
		Aisle aisle29 = Aisle.DAO.findByDomainId(facility, "A29");
		Assert.assertNotNull(aisle29);

		Bay bayA16B1 = Bay.DAO.findByDomainId(aisle29, "B1");
		Bay bayA16B2 = Bay.DAO.findByDomainId(aisle29, "B2");
		Assert.assertNotNull(bayA16B2);

		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA16B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Tier tierB2T1 = Tier.DAO.findByDomainId(bayA16B2, "T1");
		Assert.assertNotNull(tierB2T1);

		Slot slotB1T1S5 = Slot.DAO.findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5);

		// The modification is trivial: activation and deactivation of a slot. Verify starting condition.
		Assert.assertTrue(slotB1T1S5.getActive());

		slotB1T1S5.setActive(false); // but not persisted yet.
		Assert.assertFalse(slotB1T1S5.getActive());
		Slot.DAO.store(slotB1T1S5);
		Assert.assertFalse(slotB1T1S5.getActive()); // Just showing that the store did not matter on the local reference

		// Get it again, although from the old facility reference. Does not give the database version.
		slotB1T1S5 = (Slot) facility.findSubLocationById("A29.B1.T1.S5");
		Assert.assertFalse(slotB1T1S5.getActive());
		// true in database. False on our object		

		// See if we can somehow get what the database has. No!
		List<Facility> listB = Facility.DAO.getAll();
		Facility facilityB = listB.get(0);
		Slot slotB1T1S5B = (Slot) facilityB.findSubLocationById("A29.B1.T1.S5");
		Assert.assertFalse(slotB1T1S5B.getActive());
		// true in database. False in this transaction, even though we really tried to get it straight from the DAO 

		// Persist it by closing the transaction
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());
		// Old reference. Now false in database, and false on this reference. Cannot tell if the reference actually got updated by hibernate.

		slotB1T1S5.setActive(true); // but not persisted yet.
		Slot.DAO.store(slotB1T1S5); // object attached to the transaction, but still not persisted
		Assert.assertTrue(slotB1T1S5.getActive());

		// How is our old reference?
		Assert.assertTrue(slotB1T1S5B.getActive()); // Also true, for the old transaction reference after refetch

		// Get it from scratch again.
		List<Facility> listC = Facility.DAO.getAll();
		Facility facilityC = listC.get(0);
		Slot slotB1T1S5C = (Slot) facilityC.findSubLocationById("A29.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5C.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive()); // old reference is still active.

		// close the transaction
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();

		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive());
		Assert.assertTrue(slotB1T1S5C.getActive());
		// We should be able to get it again.
		List<Facility> listD = Facility.DAO.getAll();
		Facility facilityD = listD.get(0);
		Slot slotB1T1S5D = (Slot) facilityD.findSubLocationById("A29.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5D.getActive());
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive());

		this.getPersistenceService().commitTenantTransaction();

	}

	private void setActiveValue(Location inLocation, boolean inValue, boolean inWithTransaction, boolean inThrow) {
		if (inWithTransaction)
			this.getPersistenceService().beginTenantTransaction();

		inLocation.setActive(inValue);
		inLocation.getDao().store(inLocation);

		if (inThrow) {
			throw new EdiFileReadException("Just a throw because test commanded it to. No relevance to EDI.");
		}

		if (inWithTransaction)
			this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testThrowInTransaction() {
		// DAO-correct
		// And nested transactions
		this.getPersistenceService().beginTenantTransaction();

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A30,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE30");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE30", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE30");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();

		List<Facility> listA = Facility.DAO.getAll();
		Facility facilityA = listA.get(0);
		Slot slotB1T1S5 = (Slot) facilityA.findSubLocationById("A30.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5.getActive());

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 1: try to store without a transaction should throw");
		boolean caughtExpected = false;
		try {
			slotB1T1S5.setActive(true);
			LOGGER.info("Modify a detached object was ok."); // Modify and forget to store will be an easy mistake to make.
			Slot.DAO.store(slotB1T1S5);
			LOGGER.error("Should not see this message. Cannot store a detached object");
		} catch (HibernateException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		final boolean throwYes = true;
		final boolean throwNo = false;
		final boolean transactionYes = true;
		final boolean transactionNo = false;

		LOGGER.info("Case 2: simple nested transaction that might work. See errors from PersistenceService");
		this.getPersistenceService().beginTenantTransaction();
		setActiveValue(slotB1T1S5, false, transactionYes, throwNo);
		Assert.assertFalse(slotB1T1S5.getActive());
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 3: simple nested transaction will throw");
		this.getPersistenceService().beginTenantTransaction();
		caughtExpected = false;
		try {
			setActiveValue(slotB1T1S5, true, transactionYes, throwYes);
		} catch (EdiFileReadException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());

		LOGGER.info("Case 4: does nested transaction spoil the outer transaction? See errors from PersistenceService");
		this.getPersistenceService().beginTenantTransaction();
		setActiveValue(slotB1T1S5, true, transactionYes, throwNo);
		slotB1T1S5.setLedChannel((short) 4);
		try {
			Slot.DAO.store(slotB1T1S5);
		} catch (HibernateException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5.getLedChannel() == 4);

		LOGGER.info("Case 5: Not a nested transaction. Will throw, leaving transaction open");
		caughtExpected = false;
		try {
			setActiveValue(slotB1T1S5, false, transactionYes, throwYes);
		} catch (EdiFileReadException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 6: After the throw that left a transaction open, continue with normal transaction.");
		this.getPersistenceService().beginTenantTransaction();
		slotB1T1S5.setActive(true);
		Slot.DAO.store(slotB1T1S5);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
	}

	@Test
	public final void outOfOrderStores() {
		// DAO-correct
		// Attempts to create various detached and reattach object scenarios 
		// Need to add database queries to this later to see if and when change was persisted.

		this.getPersistenceService().beginTenantTransaction();

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A31,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE31");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE31", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE31");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();

		List<Facility> listA = Facility.DAO.getAll();
		Facility facilityA = listA.get(0);
		Slot slotB1T1S5 = (Slot) facilityA.findSubLocationById("A31.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5.getActive());

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 1: Modify outside a transaction. Then store inside a transaction");
		slotB1T1S5.setActive(false);
		this.getPersistenceService().beginTenantTransaction();
		slotB1T1S5.setLedChannel((short) 2); // set another field, making it look more like a mistake may be.
		Slot.DAO.store(slotB1T1S5);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 2: multiple stores in the same transaction");
		this.getPersistenceService().beginTenantTransaction();
		slotB1T1S5.setActive(true);
		Slot.DAO.store(slotB1T1S5);
		slotB1T1S5.setLedChannel((short) 3);
		Slot.DAO.store(slotB1T1S5);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5.getLedChannel() == 3);
	}

}
