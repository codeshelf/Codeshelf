/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
// domain objects needed
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.TravelDirectionEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.Vertex;
import com.codeshelf.testframework.MockDaoTest;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author ranstrom
 * Also see createAisleTest() in FacilityTest.java
 */
public class AisleImporterTest extends MockDaoTest {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(AisleImporterTest.class);

	private static double		CM_PER_M	= 100D;

	@Test
	public final void testWalmartPalletLighting() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A49,,,,,tierB1S1Side,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,318,,,,,\r\n" //
				+ "Tier,T1,,3,96,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE99", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check the aisle
		Location aisle = facility.findLocationById("A49");

		// Check the bays
		Location bay1 = aisle.findLocationById("B1");
		Assert.assertNotNull(bay1);

		// Get the tier. Then check the tier led values
		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bay1, "T1");
		Assert.assertNotNull(tierB1T1);

		// Did not create path, but will default to increasing direction
		Assert.assertTrue(tierB1T1.getFirstLedNumAlongPath() == 1);
		Assert.assertTrue(tierB1T1.getLastLedNumAlongPath() == 96);

		// Check some slot led values
		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		int firstLed = slotB1T1S1.getFirstLedNumAlongPath();
		int lastLed = slotB1T1S1.getLastLedNumAlongPath();
		Assert.assertEquals(3, firstLed);
		Assert.assertEquals(31, lastLed);

		Slot slotB1T1S2 = Slot.staticGetDao().findByDomainId(tierB1T1, "S2");
		firstLed = slotB1T1S2.getFirstLedNumAlongPath();
		lastLed = slotB1T1S2.getLastLedNumAlongPath();
		Assert.assertEquals(35, firstLed);
		Assert.assertEquals(63, lastLed);

		Slot slotB1T1S3 = Slot.staticGetDao().findByDomainId(tierB1T1, "S3");
		firstLed = slotB1T1S3.getFirstLedNumAlongPath();
		lastLed = slotB1T1S3.getLastLedNumAlongPath();
		Assert.assertEquals(67, firstLed);
		Assert.assertEquals(95, lastLed);

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testTierB1S1Side() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE9", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check the aisle
		Location aisle = facility.findLocationById("A9");
		Assert.assertNotNull(aisle);
		Assert.assertEquals(aisle.getDomainId(), "A9");

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(facility, "A9");
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
		Bay bayA9B2 = Bay.staticGetDao().findByDomainId(aisle2, "B2");
		Assert.assertNotNull(bayA9B2);

		Bay bayA9B2x = Bay.staticGetDao().findByDomainId(aisle, "B2");
		Assert.assertNotNull(bayA9B2x);

		// Testing how well findByDomainId works. Both bay1 and bay2 have T2 tiers.
		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bay1, "T2");
		Assert.assertNotNull(tierB1T2);

		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bay2, "T2");
		Assert.assertNotNull(tierB2T2);

		// These should not be equal because the persistentIds are different
		Assert.assertNotEquals(tierB1T2, tierB2T2);

		Slot slotB1T2S3 = Slot.staticGetDao().findByDomainId(tierB1T2, "S3");
		Assert.assertNotNull(slotB1T2S3);

		Slot slotB2T2S3 = Slot.staticGetDao().findByDomainId(tierB2T2, "S3");
		Assert.assertNotNull(slotB2T2S3);
		Assert.assertNotEquals(slotB1T2S3, slotB2T2S3);

		// Demonstrate the tier transient field behaviors. As we are refetching tiers via the DAO, the transients are uninitialized
		short ledCount = tierB1T2.getMTransientLedsThisTier();
		Assert.assertTrue(ledCount == 80);

		// Get two more tiers. Then check the tier led values
		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bay1, "T1");
		Assert.assertNotNull(tierB1T2);
		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bay2, "T1");
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
		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		short firstLed = slotB1T1S1.getFirstLedNumAlongPath();
		short lastLed = slotB1T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(firstLed == 3);
		Assert.assertTrue(lastLed == 6);

		Slot slotB1T1S8 = Slot.staticGetDao().findByDomainId(tierB1T1, "S8");
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

		// No path. Therefore, cannot know left side yet. Unconfigured shows as blank, not zero. 
		Assert.assertEquals("", aisle2.getMetersFromLeft());
		Assert.assertEquals("", bayA9B2.getMetersFromLeft());

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testTierNotB1S1Side() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE10", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		/* getLocationIdToParentLevel gives "" for this. You might argue it should give "F1". 
		 * Originally NPE this case, so determinant result is good. 
		 * Normally calles as this, to the aisle level. */
		String id = facility.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.isEmpty());

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A10");
		Assert.assertNotNull(aisle);

		/* getLocationIdToParentLevel */
		id = aisle.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10"));

		Bay bayA10B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Bay bayA10B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");

		id = bayA10B1.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1"));

		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bayA10B1, "T2");
		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bayA10B2, "T2");
		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA10B1, "T1");
		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bayA10B2, "T1");

		id = tierB1T2.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2"));

		// Mostly for code coverage. Does a complex iteration. But not aliases, so will be empty.
		String aliasRange = tierB1T2.getSlotAliasRange();
		Assert.assertTrue(aliasRange.isEmpty());

		Slot slotB1T2S3 = Slot.staticGetDao().findByDomainId(tierB1T2, "S3");
		Assert.assertNotNull(slotB1T2S3);

		id = slotB1T2S3.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2.S3"));

		id = slotB1T2S3.getLocationIdToParentLevel(Aisle.class);
		Assert.assertTrue(id.equals("A10.B1.T2.S3"));

		Slot slotB2T2S3 = Slot.staticGetDao().findByDomainId(tierB2T2, "S3");
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

		Slot slotB1T1S6 = Slot.staticGetDao().findByDomainId(tierB1T1, "S6");
		short slotB1T1S6First = slotB1T1S6.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB1T1S6First == 63);
		Assert.assertTrue(slotB1T1S6.getLastLedNumAlongPath() == 66);

		Slot slotB2T1S1 = Slot.staticGetDao().findByDomainId(tierB2T1, "S1");
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
		Aisle aisle20 = Aisle.staticGetDao().findByDomainId(facility, "A20");
		Assert.assertNotNull(aisle20);
		Bay bayA20B1 = Bay.staticGetDao().findByDomainId(aisle20, "B1");
		Assert.assertNotNull(bayA20B1);
		Tier tierA20B1T1 = Tier.staticGetDao().findByDomainId(bayA20B1, "T1");
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
		// new reader, because cannot reset the old reader without handling a possible exception. Same stream, though.
		importAislesData(facility, csvString);

		// just check second aisle. Need to get it again after the reread as our old reference may not be current
		aisle20 = Aisle.staticGetDao().findByDomainId(facility, "A20");
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@SuppressWarnings("unused")
	@Test
	public final void test32Led5Slot() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE11", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A11");
		Assert.assertNotNull(aisle);

		Bay bayA11B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA11B1, "T1");

		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		Slot slotB1T1S2 = Slot.staticGetDao().findByDomainId(tierB1T1, "S2");
		Slot slotB1T1S3 = Slot.staticGetDao().findByDomainId(tierB1T1, "S3");
		Slot slotB1T1S4 = Slot.staticGetDao().findByDomainId(tierB1T1, "S4");
		Slot slotB1T1S5 = Slot.staticGetDao().findByDomainId(tierB1T1, "S5");

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

		Bay bayA11B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");
		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bayA11B2, "T1");

		Slot slotB2T1S1 = Slot.staticGetDao().findByDomainId(tierB2T1, "S1");
		Slot slotB2T1S2 = Slot.staticGetDao().findByDomainId(tierB2T1, "S2");
		Slot slotB2T1S9 = Slot.staticGetDao().findByDomainId(tierB2T1, "S9");
		Slot slotB2T1S10 = Slot.staticGetDao().findByDomainId(tierB2T1, "S10");
		Slot slotB2T1S11 = Slot.staticGetDao().findByDomainId(tierB2T1, "S11");
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
		Bay bayA11B3 = Bay.staticGetDao().findByDomainId(aisle, "B3");
		Tier tierB3T1 = Tier.staticGetDao().findByDomainId(bayA11B3, "T1");
		// just showing that we do not set bay first led. Could for zigzags, but not for other types
		// B3 starts at 32 + 80 + 1 = 113.
		// short bayFirstLed = bayA11B3.getFirstLedNumAlongPath(); // throws
		// Assert.assertTrue(bayFirstLed == 0);
		short tierFirstLed = tierB3T1.getFirstLedNumAlongPath();
		Assert.assertTrue(tierFirstLed == 113);
		// making sure that bay 10 is after, and not before T2 which would start at 33.
		Bay bayA11B10 = Bay.staticGetDao().findByDomainId(aisle, "B10");
		Tier tierB10T1 = Tier.staticGetDao().findByDomainId(bayA11B10, "T1");
		tierFirstLed = tierB10T1.getFirstLedNumAlongPath();
		Assert.assertTrue(tierFirstLed == 155);

		this.getTenantPersistenceService().commitTransaction();

	}

	@SuppressWarnings("unused")
	@Test
	public final void testSparseLeds() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-SPARSE91", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A91");
		Assert.assertNotNull(aisle);

		Bay bayA91B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA91B1, "T1");
		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bayA91B1, "T2");
		Tier tierB1T3 = Tier.staticGetDao().findByDomainId(bayA91B1, "T3");
		Tier tierB1T4 = Tier.staticGetDao().findByDomainId(bayA91B1, "T4");
		Tier tierB1T5 = Tier.staticGetDao().findByDomainId(bayA91B1, "T5");

		// This is a zigzag bay. T1 is last. This is the "laser" shelf, one "LED" per slot)
		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		Slot slotB1T1S2 = Slot.staticGetDao().findByDomainId(tierB1T1, "S2");
		Slot slotB1T1S3 = Slot.staticGetDao().findByDomainId(tierB1T1, "S3");
		Slot slotB1T1S4 = Slot.staticGetDao().findByDomainId(tierB1T1, "S4");
		Slot slotB1T1S5 = Slot.staticGetDao().findByDomainId(tierB1T1, "S5");

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
		Slot slotB1T5S1 = Slot.staticGetDao().findByDomainId(tierB1T5, "S1");
		Slot slotB1T5S2 = Slot.staticGetDao().findByDomainId(tierB1T5, "S2");
		Slot slotB1T5S3 = Slot.staticGetDao().findByDomainId(tierB1T5, "S3");
		Slot slotB1T5S4 = Slot.staticGetDao().findByDomainId(tierB1T5, "S4");
		Slot slotB1T5S5 = Slot.staticGetDao().findByDomainId(tierB1T5, "S5");

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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testZigzagB1S1Side() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE12", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A12");
		Assert.assertNotNull(aisle);

		Bay bayA12B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Bay bayA12B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA12B1, "T1");
		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bayA12B2, "T2");

		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");

		Slot slotB2T2S5 = Slot.staticGetDao().findByDomainId(tierB2T2, "S5");

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
		Tier tierB1T3 = Tier.staticGetDao().findByDomainId(bayA12B1, "T3");
		Tier tierB2T3 = Tier.staticGetDao().findByDomainId(bayA12B2, "T3");
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testZigzagNotB1S1Side() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE13", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A13");
		Assert.assertNotNull(aisle);

		Bay bayA13B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Bay bayA13B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA13B1, "T1");
		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bayA13B2, "T2");

		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");

		Slot slotB2T2S5 = Slot.staticGetDao().findByDomainId(tierB2T2, "S5");

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
		Tier tierB1T3 = Tier.staticGetDao().findByDomainId(bayA13B1, "T3");
		Tier tierB2T3 = Tier.staticGetDao().findByDomainId(bayA13B2, "T3");
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testMultiAisleZig() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE2X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A21");
		Assert.assertNotNull(aisle);

		Bay bayA21B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Bay bayA21B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");

		// For 2 bays, 2 tier, zigzagB1S1Side, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA21B1, "T1");
		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bayA21B1, "T2");
		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bayA21B2, "T2");
		double b1T1FaceEnd = tierB1T1.getPickFaceEndPosX();
		Assert.assertTrue(b1T1FaceEnd == 1.15);
		double b2T2FaceEnd = tierB2T2.getPickFaceEndPosX();
		// Remember, tier in second bay pickface is relative to the bay. It will be about 1.41
		Assert.assertTrue(b2T2FaceEnd < 2.0);

		List<Location> theB1T1Slots = tierB1T1.getActiveChildren();
		Assert.assertTrue(theB1T1Slots.size() == 5);
		List<Location> theB1T2Slots = tierB1T2.getActiveChildren();
		Assert.assertTrue(theB1T2Slots.size() == 4);
		short tierB1T2First = tierB1T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T2First == 1);
		short tierB2T2First = tierB2T2.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T2First == 65);

		// Aisle 22 should have Y orientation
		Aisle aisle22 = Aisle.staticGetDao().findByDomainId(facility, "A22");
		Assert.assertNotNull(aisle);
		Bay bayA22B1 = Bay.staticGetDao().findByDomainId(aisle22, "B1");
		Tier tierA22B1T1 = Tier.staticGetDao().findByDomainId(bayA22B1, "T1");
		double pickX = tierA22B1T1.getPickFaceEndPosX();
		double pickY = tierA22B1T1.getPickFaceEndPosY();
		// 1.15 + 1.41 = 2.56. But real addition is too precise.
		Assert.assertTrue(pickX == 0.0);
		Assert.assertTrue(pickY == 1.15);

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testBadFile1() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE14", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got from this bad file
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A14");
		Assert.assertNotNull(aisle); // the aisle started ok

		Bay bayA14B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");
		Assert.assertNull(bayA14B2); // bay should have failed for the tier coming first.

		Bay bayA14B3 = Bay.staticGetDao().findByDomainId(aisle, "B3");
		Assert.assertNull(bayA14B3); // bay should have failed for nothing read until next aisle.

		Aisle aisle7 = Aisle.staticGetDao().findByDomainId(facility, "A7");
		Assert.assertNotNull(aisle7); // the aisle started ok. Note that we do not enforce name number consistency on aisles

		Bay bayA7B1 = Bay.staticGetDao().findByDomainId(aisle7, "B1");
		Assert.assertNotNull(bayA7B1); // bay should be ok

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA7B1, "T1");
		Assert.assertNotNull(tierB1T1); // should be there

		Bay bayA7B2 = Bay.staticGetDao().findByDomainId(aisle7, "B2");
		Assert.assertNull(bayA7B2); // will not be there because second T1 in B1 made it skip past rest of this aisle

		Aisle aisleB7 = Aisle.staticGetDao().findByDomainId(facility, "AB7");
		Assert.assertNull(aisleB7); // the aisle name not accepted

		Aisle aisle9 = Aisle.staticGetDao().findByDomainId(facility, "A9");
		Assert.assertNotNull(aisle9); // ok

		Bay bayA9B1 = Bay.staticGetDao().findByDomainId(aisle9, "B1");
		Assert.assertNotNull(bayA9B1); // ok, even with no tiers

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		// Check for missing data
		// The bays and aisles should be created
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,,,,,\r\n" //
				+ "Bay,B1,,,,,,\r\n" //
				+ "Tier,T1,,,,,,\r\n" //
				+ "Aisle,A52,CLONE(A51),,,,,,,,\r\n"; //
		importAislesData(facility, csvString2);

		Aisle A512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(A512);

		Bay A51B12 = Bay.staticGetDao().findByDomainId(A512, "B1");
		Assert.assertNotNull(A51B12);

		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(A51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);

		Aisle A522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(A522);

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void testDoubleFileRead() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,3,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,3,40,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE15", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle);
		Bay bayA15B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1);
		Slot slotB1T1S3 = Slot.staticGetDao().findByDomainId(tierB1T1, "S3");
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
		importAislesData(facility, csvString2);

		aisle = Aisle.staticGetDao().findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle);

		bayA15B1 = Bay.staticGetDao().findByDomainId(aisle, "B1");
		Bay bayA15B2 = Bay.staticGetDao().findByDomainId(aisle, "B2");
		Assert.assertNotNull(bayA15B2);
		Double baylength = bayA15B1.getPickFaceEndPosY() - bayA15B1.getAnchorPosY(); // this aisle is Y orientation
		Assert.assertTrue(baylength > 1.20); // Bay 1 values were updated
		// Compiler warning on equality of double. (== 1.22) so lets use > as the old value was 1.15

		tierB1T1 = Tier.staticGetDao().findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1); // should still exist

		Tier tierB2T2 = Tier.staticGetDao().findByDomainId(bayA15B2, "T2");
		Assert.assertNotNull(tierB2T2); // Shows that we reread and this time created T2

		slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1); // should still exist
		Slot slotB1T1S5 = Slot.staticGetDao().findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5); // should still exist
		Slot slotB1T1S6 = Slot.staticGetDao().findByDomainId(tierB1T1, "S6");
		Assert.assertNotNull(slotB1T1S6); // Shows that we reread and this time created S6

		short tierB1T1Last = tierB1T1.getLastLedNumAlongPath(); // did the tier LEDs change?
		Assert.assertTrue(tierB1T1Last == 50); // Show that LEDs were recomputed and updated

		slotB1T1S3 = Slot.staticGetDao().findByDomainId(tierB1T1, "S3");
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
		importAislesData(facility, csvString3);

		// Check what we got
		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(facility, "A15");
		Assert.assertNotNull(aisle3);

		bayA15B1 = Bay.staticGetDao().findByDomainId(aisle3, "B1");
		bayA15B2 = Bay.staticGetDao().findByDomainId(aisle3, "B2");
		Assert.assertNotNull(bayA15B2); // Incorrect! We want B2 to be null or somehow retired

		tierB1T1 = Tier.staticGetDao().findByDomainId(bayA15B1, "T1");
		Assert.assertNotNull(tierB1T1); // should still exist

		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bayA15B1, "T2");
		Assert.assertNotNull(tierB1T2); // Incorrect! We want T2 to be null or somehow retired

		slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
		Assert.assertNotNull(slotB1T1S1); // should still exist
		slotB1T1S5 = Slot.staticGetDao().findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5); // Incorrect! We want T2 to be null or somehow retired

		List<Vertex> vList1 = aisle3.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		// Assert.assertTrue(yValue == 1.1); // new bay width 110 cm. But aisle is coming as 2.3 which is the original 2 bay value

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testAfterFileModifications() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE16", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get the objects we will use
		Aisle aisle16 = Aisle.staticGetDao().findByDomainId(facility, "A16");
		Assert.assertNotNull(aisle16);

		Bay bayA16B1 = Bay.staticGetDao().findByDomainId(aisle16, "B1");
		Bay bayA16B2 = Bay.staticGetDao().findByDomainId(aisle16, "B2");
		Assert.assertNotNull(bayA16B2);

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA16B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bayA16B2, "T1");
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
		Slot slotB1T1S1 = Slot.staticGetDao().findByDomainId(tierB1T1, "S1");
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
		tierB2T1 = Tier.staticGetDao().findByDomainId(bayA16B2, "T1");
		b2T1Channel = tierB2T1.getLedChannel(); // need to get this again from the re-hydrated object
		Assert.assertTrue(b2T1Channel == (short) 1);

		LedController b1T1Controller = tierB1T1.getLedController();
		LedController b2T1Controller = tierB2T1.getLedController(); // This needed the re-get also
		Assert.assertEquals(b1T1Controller, b2T1Controller); // different ebeans reference, but same persistent ID should match on equals

		String b1T1ControllerStr = tierB1T1.getLedControllerId();
		String b2T1ControllerStr = tierB2T1.getLedControllerId();
		Assert.assertEquals(b2T1ControllerStr, b1T1ControllerStr); // strings match; both "0x000026"

		// test getEffective controller and channel.  The get will not have anything, but getEffective will go up the parent chain.
		Slot slotB2T1S1 = Slot.staticGetDao().findByDomainId(tierB2T1, "S1");
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
		tierB1T1 = Tier.staticGetDao().findByDomainId(bayA16B1, "T1");
		Assert.assertNull(tierB1T1.getLedController());
		Assert.assertNull(tierB1T1.getLedChannel());
		Assert.assertEquals(ledController66, tierB1T1.getEffectiveLedController());
		Assert.assertTrue(tierB1T1.getEffectiveLedChannel() == 2);

		// DEV-514 Let's persist now. tierB1T1 reference comes from the previous. As does aisle16 reference.
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		// set on the old aisle reference. Does the old tier reference know?
		aisle16.setControllerChannel(cntrlPersistIdStr55, "1");
		// These fail!
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		// Assert.assertTrue(tierB1T1.getEffectiveLedChannel() == 1);
		// Get from DB again under this transaction. However, facility is old reference. Ok? No!
		aisle16 = Aisle.staticGetDao().findByDomainId(facility, "A16");
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		// There is no way in this test structure to re-get the facility from the database under a new transaction.
		// aisle16 = Aisle.staticGetDao().findByDomainId(getFacility(), "A16");
		// similar problem: still the old facility reference.
		tierB1T1 = (Tier) facility.findSubLocationById("A16.B1.T1");
		// Assert.assertEquals(ledController55, tierB1T1.getEffectiveLedController()); // Fails
		List<Facility> aList = Facility.staticGetDao().getAll();
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testNoLed() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE21", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check what we got
		Aisle aisle21 = Aisle.staticGetDao().findByDomainId(facility, "A21");
		Assert.assertNotNull(aisle21);

		Bay bayA21B1 = Bay.staticGetDao().findByDomainId(aisle21, "B1");
		Bay bayA21B2 = Bay.staticGetDao().findByDomainId(aisle21, "B2");

		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bayA21B2, "T1");
		Tier tierB1T2 = Tier.staticGetDao().findByDomainId(bayA21B1, "T2");

		Slot slotB2T1S1 = Slot.staticGetDao().findByDomainId(tierB2T1, "S1");

		Slot slotB1T2S5 = Slot.staticGetDao().findByDomainId(tierB1T2, "S5");

		// leds should be zero
		Short ledValue1 = tierB2T1.getFirstLedNumAlongPath();
		Assert.assertTrue(ledValue1 == 0);
		Short ledValue2 = slotB1T2S5.getLastLedNumAlongPath();
		Assert.assertTrue(ledValue2 == 0);

		Short ledValue3 = slotB2T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(ledValue3 == 0);

		this.getTenantPersistenceService().commitTransaction();

	}

	private Double helperGetPosAlongSegment(PathSegment inSegment, Double inX, Double inY) {
		Point testPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, inX, inY, 0.0);
		return inSegment.computeNormalizedPositionAlongPath(testPoint);
	}

	@Test
	public final void testPathCreation() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.createFacility("F4X", "TEST", Point.getZeroPoint());

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.0, 12.0, 48.0);
		PathSegment segment1 = addPathSegmentForTest(aPath, 1, 12.0, 48.0, 12.0, 58.0);
		SortedSet<PathSegment> segments = aPath.getSegments();
		int countSegments = segments.size();
		Assert.assertTrue(countSegments == 2);

		// Path aPath2 = Path.staticGetDao().findByDomainId(facility, "F4X.1");  does not work
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void simplestPathTest() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get A31
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.00, 48.45);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		String persistStr = segment0.getPersistentId().toString();
		aisle51.associatePathSegment(persistStr);
		// This should have recomputed all positions along path.  Aisle, bay, tier, and slots should ahve position now
		// Although the old reference to aisle before path association would not.

		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Slot slotS1 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S1");
		Assert.assertNotNull(slotS1);
		Slot slotS4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
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

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testCloneTierB1S1Aisle() {

		// Test tierB1S1Side
		this.getTenantPersistenceService().beginTransaction();

		// tierB1S1Side Test 1
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,tierB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,2,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" + "Aisle,A54,,,,,tierB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check aisles exist
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);
		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);
		Aisle aisle53 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle53);
		Aisle aisle54 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle54);

		// Check LED values
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Assert.assertNotNull(bayA51B1);
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Assert.assertNotNull(tierA51B1T1);
		Slot slotA51B1T1S4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotA51B1T1S4);
		Short ledA51B1T1S4value = slotA51B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value);

		Bay bayA53B1 = Bay.staticGetDao().findByDomainId(aisle53, "B1");
		Assert.assertNotNull(bayA53B1);
		Tier tierA53B1T1 = Tier.staticGetDao().findByDomainId(bayA53B1, "T1");
		Assert.assertNotNull(tierA53B1T1);
		Slot slotA53B1T1S4 = Slot.staticGetDao().findByDomainId(tierA53B1T1, "S4");
		Assert.assertNotNull(slotA53B1T1S4);
		Short LedA53B2T1S4value = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value);

		Assert.assertEquals(ledA51B1T1S4value, LedA53B2T1S4value);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B2 = Bay.staticGetDao().findByDomainId(aisle53, "B2");
		Assert.assertNotNull(bayA53B2);
		Tier tierA53B2T3 = Tier.staticGetDao().findByDomainId(bayA53B2, "T3");
		Assert.assertNotNull(tierA53B2T3);
		Assert.assertEquals(tierA53B2T3.getActiveChildren().size(), 2);

		Short firstLed = tierA53B2T3.getFirstLedNumAlongPath();
		Short lastLed = tierA53B2T3.getLastLedNumAlongPath();
		Assert.assertEquals((lastLed - firstLed) + 1, 32);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// TierB1S1Side Test 2 - (no slots on B2T3)
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,tierB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,0,40,0,,\r\n" // NO SLOTS && NUMBER OF LEDS CHANGED
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" + "Aisle,A54,,,,,tierB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; //
		importAislesData(facility, csvString2);

		// Check aisles exist
		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);
		Aisle aisle522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle522);
		Aisle aisle532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle532);
		Aisle aisle542 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle542);

		// Check LED values
		Bay bayA51B12 = Bay.staticGetDao().findByDomainId(aisle512, "B1");
		Assert.assertNotNull(bayA51B12);
		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(bayA51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);
		Slot slotA51B1T1S42 = Slot.staticGetDao().findByDomainId(tierA51B1T12, "S4");
		Assert.assertNotNull(slotA51B1T1S42);
		Short ledA51B1T1S4value2 = slotA51B1T1S42.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value2);

		Bay bayA53B12 = Bay.staticGetDao().findByDomainId(aisle532, "B1");
		Assert.assertNotNull(bayA53B12);
		Tier tierA53B1T12 = Tier.staticGetDao().findByDomainId(bayA53B12, "T1");
		Assert.assertNotNull(tierA53B1T12);
		Slot slotA53B1T1S42 = Slot.staticGetDao().findByDomainId(tierA53B1T12, "S4");
		Assert.assertNotNull(slotA53B1T1S42);
		Short LedA53B2T1S4value2 = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value2);

		Assert.assertEquals(ledA51B1T1S4value2, LedA53B2T1S4value2);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B22 = Bay.staticGetDao().findByDomainId(aisle532, "B2");
		Assert.assertNotNull(bayA53B22);
		Tier tierA53B2T32 = Tier.staticGetDao().findByDomainId(bayA53B22, "T3");
		Assert.assertNotNull(tierA53B2T32);
		Assert.assertEquals(0, tierA53B2T32.getActiveChildren().size());

		Short firstLed2 = tierA53B2T32.getFirstLedNumAlongPath();
		Short lastLed2 = tierA53B2T32.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLed2 - firstLed2) + 1);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneTierNotB1S1Aisle() {

		// Test tierNotB1S1Side
		this.getTenantPersistenceService().beginTransaction();

		// tierNotB1S1Side Test 1
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,tierNotB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,2,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n"//
				+ "Aisle,A54,,,,,tierNotB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check aisles exist
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);
		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);
		Aisle aisle53 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle53);
		Aisle aisle54 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle54);

		// Check LED values
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Assert.assertNotNull(bayA51B1);
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Assert.assertNotNull(tierA51B1T1);
		Slot slotA51B1T1S4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotA51B1T1S4);
		Short ledA51B1T1S4value = slotA51B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value);

		Bay bayA53B1 = Bay.staticGetDao().findByDomainId(aisle53, "B1");
		Assert.assertNotNull(bayA53B1);
		Tier tierA53B1T1 = Tier.staticGetDao().findByDomainId(bayA53B1, "T1");
		Assert.assertNotNull(tierA53B1T1);
		Slot slotA53B1T1S4 = Slot.staticGetDao().findByDomainId(tierA53B1T1, "S4");
		Assert.assertNotNull(slotA53B1T1S4);
		Short LedA53B2T1S4value = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value);

		Assert.assertEquals(ledA51B1T1S4value, LedA53B2T1S4value);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B2 = Bay.staticGetDao().findByDomainId(aisle53, "B2");
		Assert.assertNotNull(bayA53B2);
		Tier tierA53B2T3 = Tier.staticGetDao().findByDomainId(bayA53B2, "T3");
		Assert.assertNotNull(tierA53B2T3);
		Assert.assertEquals(tierA53B2T3.getActiveChildren().size(), 2);

		Short firstLed = tierA53B2T3.getFirstLedNumAlongPath();
		Short lastLed = tierA53B2T3.getLastLedNumAlongPath();
		Assert.assertEquals((lastLed - firstLed) + 1, 32);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// TierNotB1S1Side Test 2 - (no slots on B2T3)
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,tierNotB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,0,40,0,,\r\n" // NO SLOTS && NUMBER OF LEDS CHANGED
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" //
				+ "Aisle,A54,,,,,,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; //
		importAislesData(facility, csvString2);

		// Check aisles exist
		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);
		Aisle aisle522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle522);
		Aisle aisle532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle532);
		Aisle aisle542 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle542);

		// Check LED values
		Bay bayA51B12 = Bay.staticGetDao().findByDomainId(aisle512, "B1");
		Assert.assertNotNull(bayA51B12);
		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(bayA51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);
		Slot slotA51B1T1S42 = Slot.staticGetDao().findByDomainId(tierA51B1T12, "S4");
		Assert.assertNotNull(slotA51B1T1S42);
		Short ledA51B1T1S4value2 = slotA51B1T1S42.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value2);

		Bay bayA53B12 = Bay.staticGetDao().findByDomainId(aisle532, "B1");
		Assert.assertNotNull(bayA53B12);
		Tier tierA53B1T12 = Tier.staticGetDao().findByDomainId(bayA53B12, "T1");
		Assert.assertNotNull(tierA53B1T12);
		Slot slotA53B1T1S42 = Slot.staticGetDao().findByDomainId(tierA53B1T12, "S4");
		Assert.assertNotNull(slotA53B1T1S42);
		Short LedA53B2T1S4value2 = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value2);

		Assert.assertEquals(ledA51B1T1S4value2, LedA53B2T1S4value2);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B22 = Bay.staticGetDao().findByDomainId(aisle532, "B2");
		Assert.assertNotNull(bayA53B22);
		Tier tierA53B2T32 = Tier.staticGetDao().findByDomainId(bayA53B22, "T3");
		Assert.assertNotNull(tierA53B2T32);
		Assert.assertEquals(0, tierA53B2T32.getActiveChildren().size());

		Short firstLed2 = tierA53B2T32.getFirstLedNumAlongPath();
		Short lastLed2 = tierA53B2T32.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLed2 - firstLed2) + 1);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneZigzagB1S1Aisle() {

		// Test zigzagB1S1Side
		this.getTenantPersistenceService().beginTransaction();

		// zigzagB1S1Side Test 1
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,2,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" //
				+ "Aisle,A54,,,,,zigzagB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check aisles exist
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);
		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);
		Aisle aisle53 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle53);
		Aisle aisle54 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle54);

		// Check LED values
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Assert.assertNotNull(bayA51B1);
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Assert.assertNotNull(tierA51B1T1);
		Slot slotA51B1T1S4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotA51B1T1S4);
		Short ledA51B1T1S4value = slotA51B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value);

		Bay bayA53B1 = Bay.staticGetDao().findByDomainId(aisle53, "B1");
		Assert.assertNotNull(bayA53B1);
		Tier tierA53B1T1 = Tier.staticGetDao().findByDomainId(bayA53B1, "T1");
		Assert.assertNotNull(tierA53B1T1);
		Slot slotA53B1T1S4 = Slot.staticGetDao().findByDomainId(tierA53B1T1, "S4");
		Assert.assertNotNull(slotA53B1T1S4);
		Short LedA53B2T1S4value = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value);

		Assert.assertEquals(ledA51B1T1S4value, LedA53B2T1S4value);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B2 = Bay.staticGetDao().findByDomainId(aisle53, "B2");
		Assert.assertNotNull(bayA53B2);
		Tier tierA53B2T3 = Tier.staticGetDao().findByDomainId(bayA53B2, "T3");
		Assert.assertNotNull(tierA53B2T3);
		Assert.assertEquals(tierA53B2T3.getActiveChildren().size(), 2);

		Short firstLed = tierA53B2T3.getFirstLedNumAlongPath();
		Short lastLed = tierA53B2T3.getLastLedNumAlongPath();
		Assert.assertEquals((lastLed - firstLed) + 1, 32);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// zigzagB1S1Side Test 2 - (no slots on B2T3)
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,0,40,0,,\r\n" // NO SLOTS && NUMBER OF LEDS CHANGED
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" //
				+ "Aisle,A54,,,,,zigzagB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; // 
		importAislesData(facility, csvString2);

		// Check aisles exist
		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);
		Aisle aisle522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle522);
		Aisle aisle532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle532);
		Aisle aisle542 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle542);

		// Check LED values
		Bay bayA51B12 = Bay.staticGetDao().findByDomainId(aisle512, "B1");
		Assert.assertNotNull(bayA51B12);
		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(bayA51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);
		Slot slotA51B1T1S42 = Slot.staticGetDao().findByDomainId(tierA51B1T12, "S4");
		Assert.assertNotNull(slotA51B1T1S42);
		Short ledA51B1T1S4value2 = slotA51B1T1S42.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value2);

		Bay bayA53B12 = Bay.staticGetDao().findByDomainId(aisle532, "B1");
		Assert.assertNotNull(bayA53B12);
		Tier tierA53B1T12 = Tier.staticGetDao().findByDomainId(bayA53B12, "T1");
		Assert.assertNotNull(tierA53B1T12);
		Slot slotA53B1T1S42 = Slot.staticGetDao().findByDomainId(tierA53B1T12, "S4");
		Assert.assertNotNull(slotA53B1T1S42);
		Short LedA53B2T1S4value2 = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value2);

		Assert.assertEquals(ledA51B1T1S4value2, LedA53B2T1S4value2);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B22 = Bay.staticGetDao().findByDomainId(aisle532, "B2");
		Assert.assertNotNull(bayA53B22);
		Tier tierA53B2T32 = Tier.staticGetDao().findByDomainId(bayA53B22, "T3");
		Assert.assertNotNull(tierA53B2T32);
		Assert.assertEquals(0, tierA53B2T32.getActiveChildren().size());

		Short firstLed2 = tierA53B2T32.getFirstLedNumAlongPath();
		Short lastLed2 = tierA53B2T32.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLed2 - firstLed2) + 1);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneZigzagNotB1S1Aisle() {

		this.getTenantPersistenceService().beginTransaction();

		// zigzagNotB1S1Side Test 1
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagNotB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,2,32,0,,\r\n"// 
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"//
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" //
				+ "Aisle,A54,,,,,zigzagNotB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; // */
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check aisles exist
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);
		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);
		Aisle aisle53 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle53);
		Aisle aisle54 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle54);

		// Check LED values
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Assert.assertNotNull(bayA51B1);
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Assert.assertNotNull(tierA51B1T1);
		Slot slotA51B1T1S4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotA51B1T1S4);
		Short ledA51B1T1S4value = slotA51B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value);

		Bay bayA53B1 = Bay.staticGetDao().findByDomainId(aisle53, "B1");
		Assert.assertNotNull(bayA53B1);
		Tier tierA53B1T1 = Tier.staticGetDao().findByDomainId(bayA53B1, "T1");
		Assert.assertNotNull(tierA53B1T1);
		Slot slotA53B1T1S4 = Slot.staticGetDao().findByDomainId(tierA53B1T1, "S4");
		Assert.assertNotNull(slotA53B1T1S4);
		Short LedA53B2T1S4value = slotA53B1T1S4.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S4value);

		Assert.assertEquals(ledA51B1T1S4value, LedA53B2T1S4value);

		// Check number slots and LEDs on B2 T3

		Bay bayA53B2 = Bay.staticGetDao().findByDomainId(aisle53, "B2");
		Assert.assertNotNull(bayA53B2);
		Tier tierA53B2T3 = Tier.staticGetDao().findByDomainId(bayA53B2, "T3");
		Assert.assertNotNull(tierA53B2T3);
		Assert.assertEquals(tierA53B2T3.getActiveChildren().size(), 2);

		Short firstLed = tierA53B2T3.getFirstLedNumAlongPath();
		Short lastLed = tierA53B2T3.getLastLedNumAlongPath();
		Assert.assertEquals((lastLed - firstLed) + 1, 32);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// zigzagNotB1S1Side Test 2 - (no slots on B2T3)
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagNotB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,0,40,0,,\r\n" // NO SLOTS && LED COUNT CHANGE!
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n" //
				+ "Aisle,A54,,,,,zigzagNotB1S1Side,12.85,58.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n"; // */
		importAislesData(facility, csvString2);

		// Check aisles exist
		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);
		Aisle aisle522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle522);
		Aisle aisle532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle532);
		Aisle aisle542 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle542);

		// Check LED values
		Bay bayA51B12 = Bay.staticGetDao().findByDomainId(aisle512, "B1");
		Assert.assertNotNull(bayA51B12);
		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(bayA51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);
		Short ledA51B1T1S4value2 = tierA51B1T12.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S4value2);

		// Check the number of LEDs in A51B2T3 changed
		Bay bayA51B22 = Bay.staticGetDao().findByDomainId(aisle512, "B2");
		Assert.assertNotNull(bayA51B22);
		Tier tierA51B2T32 = Tier.staticGetDao().findByDomainId(bayA51B22, "T3");
		Assert.assertNotNull(tierA51B2T32);
		Short firstLed21 = tierA51B2T32.getFirstLedNumAlongPath();
		Short lastLed21 = tierA51B2T32.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLed21 - firstLed21) + 1);

		Bay bayA53B12 = Bay.staticGetDao().findByDomainId(aisle532, "B1");
		Assert.assertNotNull(bayA53B12);
		Tier tierA53B1T12 = Tier.staticGetDao().findByDomainId(bayA53B12, "T1");
		Assert.assertNotNull(tierA53B1T12);
		Short ledA53B2T1S4value2 = tierA53B1T12.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA53B2T1S4value2);

		Assert.assertEquals(ledA51B1T1S4value2, ledA53B2T1S4value2);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B22 = Bay.staticGetDao().findByDomainId(aisle532, "B2");
		Assert.assertNotNull(bayA53B22);
		Tier tierA53B2T32 = Tier.staticGetDao().findByDomainId(bayA53B22, "T3");
		Assert.assertNotNull(tierA53B2T32);
		Assert.assertEquals(0, tierA53B2T32.getActiveChildren().size());

		Short firstLed2 = tierA53B2T32.getFirstLedNumAlongPath();
		Short lastLed2 = tierA53B2T32.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLed2 - firstLed2) + 1);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneChangeAttributes() {
		this.getTenantPersistenceService().beginTransaction();
		// Test if we can change the X,Y orientation in a clone
		// A clone should not be able to change the X,Y orientation

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,Y,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);
		Assert.assertEquals(aisle51.isLocationXOriented(), true);

		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);
		Assert.assertEquals(aisle52.isLocationXOriented(), true);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Test if we can change the depth
		// A clone should not be able to change the depth

		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,200\r\n"; //
		importAislesData(facility, csvString2);

		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);

		Vertex V3 = Vertex.staticGetDao().findByDomainId(aisle512, "V03");
		Assert.assertEquals(120, (int) (V3.getPosY() * CM_PER_M));

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		// Test if we can change the LED configuration
		// A clone should not be able to change the LED configuration
		// Will print out a warning to the user

		String csvString3 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,zigzagNotB1S1Side,12.85,48.45,X,200\r\n"; //
		importAislesData(facility, csvString3);

		Aisle aisle513 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle513);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneAisle() {
		// For DEV-618
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get A51
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// See the A51 and A52 have some of the same locations, and same Led numbers
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Slot slotS1 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S1");
		Assert.assertNotNull(slotS1);
		Slot slotS4 = Slot.staticGetDao().findByDomainId(tierA51B1T1, "S4");
		Assert.assertNotNull(slotS4);
		Short led51Value = slotS4.getFirstLedNumAlongPath();

		Bay bayA52B1 = Bay.staticGetDao().findByDomainId(aisle52, "B1");
		Assert.assertNotNull(bayA52B1); // change to notNull with DEV-618

		// curious. Tier.staticGetDao().findByDomainId(null, "T1"); will find the A51 T1
		Tier tierA52B1T1 = Tier.staticGetDao().findByDomainId(bayA52B1, "T1");
		Assert.assertNotNull(tierA52B1T1);
		Slot slot52S1 = Slot.staticGetDao().findByDomainId(tierA52B1T1, "S1");
		Assert.assertNotNull(slot52S1);
		Slot slot52S4 = Slot.staticGetDao().findByDomainId(tierA52B1T1, "S4");
		Assert.assertNotNull(slot52S4);
		Short led52Value = slot52S4.getFirstLedNumAlongPath();
		Assert.assertEquals(led51Value, led52Value);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Test Define -> clone defined -> clone defined
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Tier,T3,,2,40,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n"; //
		importAislesData(facility, csvString2);

		// Get A51
		Aisle aisle512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle512);

		// Get A52
		Aisle aisle522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle522);

		// Get A53
		Aisle aisle532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle532);

		// Check slot LED numbers
		Bay bayA51B12 = Bay.staticGetDao().findByDomainId(aisle512, "B1");
		Assert.assertNotNull(bayA51B12);
		Tier tierA51B1T12 = Tier.staticGetDao().findByDomainId(bayA51B12, "T1");
		Assert.assertNotNull(tierA51B1T12);
		Slot slotA51B1T1S42 = Slot.staticGetDao().findByDomainId(tierA51B1T12, "S4");
		Assert.assertNotNull(slotA51B1T1S42);
		Short ledA51B1T1S42value = slotA51B1T1S42.getFirstLedNumAlongPath();
		Assert.assertNotNull(ledA51B1T1S42value);

		Bay bayA53B12 = Bay.staticGetDao().findByDomainId(aisle532, "B1");
		Assert.assertNotNull(bayA53B12);
		Tier tierA53B1T12 = Tier.staticGetDao().findByDomainId(bayA53B12, "T1");
		Assert.assertNotNull(tierA53B1T12);
		Slot slotA53B1T1S42 = Slot.staticGetDao().findByDomainId(tierA53B1T12, "S4");
		Assert.assertNotNull(slotA53B1T1S42);
		Short LedA53B2T1S42value = slotA53B1T1S42.getFirstLedNumAlongPath();
		Assert.assertNotNull(LedA53B2T1S42value);

		Assert.assertEquals(ledA51B1T1S42value, LedA53B2T1S42value);

		// Check number slots and LEDs on B2 T3
		Bay bayA53B22 = Bay.staticGetDao().findByDomainId(aisle532, "B2");
		Assert.assertNotNull(bayA53B22);
		Tier tierA53B2T32 = Tier.staticGetDao().findByDomainId(bayA53B22, "T3");
		Assert.assertNotNull(tierA53B2T32);

		Assert.assertEquals(tierA53B2T32.getActiveChildren().size(), 2);

		Short firstLed2 = tierA53B2T32.getFirstLedNumAlongPath();
		Short lastLed2 = tierA53B2T32.getLastLedNumAlongPath();

		Assert.assertEquals((lastLed2 - firstLed2) + 1, 40);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Test define -> clone defined -> clone cloned

		String csvString3 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,53.45,X,120\r\n"; //
		importAislesData(facility, csvString3);

		// Get A51
		Aisle aisle513 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle513);

		// Get A52
		Aisle aisle523 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle523);

		// Get A53
		Aisle aisle533 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle533);

		//note 1

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Test define -> clone defined -> define -> clone first defined
		String csvString4 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A53,,,,,zigzagB1S1Side,12.85,53.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A54,Clone(A51),,,,,12.85,58.45,X,120\r\n"; //
		importAislesData(facility, csvString4);

		// Get A51
		Aisle aisle514 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle514);

		// Get A52
		Aisle aisle524 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle524);

		// Get A53
		Aisle aisle534 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNotNull(aisle534);

		// Get A53
		Aisle aisle544 = Aisle.staticGetDao().findByDomainId(facility, "A54");
		Assert.assertNotNull(aisle544);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testBadCloneAisle() {
		// For DEV-618
		this.getTenantPersistenceService().beginTransaction();

		// Should not be able to clone A51 because its definition is incorrect
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		Aisle A52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNull(A52);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Should not be able to clone A51 because a bay definition inside is wrong
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		importAislesData(facility, csvString2);

		Aisle A512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(A512);

		Aisle A522 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNull(A522);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Should not be able to define and clone an aisle in the same line.
		// There should be a warning for doing this. Check logs.
		String csvString3 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A51,Clone(A51),,,,,12.85,48.45,X,120\r\n";
		importAislesData(facility, csvString3);

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testBadCloneAisle2() {
		// For DEV-618
		this.getTenantPersistenceService().beginTransaction();

		// - Check if we can clone a bay after a bay creation has failed (should not be able to)
		// - Check if we can clone an aisle that had definition errors (should not be able to)
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Bay,B3,115,,,,,\r\n" // Everything else in aisle should be discarded
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Bay,B2,Clone(B1),,,,,12.85,48.45,X,120\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; // Should not be able to clone.
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		Aisle A51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(A51);

		Aisle A52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNull(A52);

		// Check what bays were created in A51
		Bay A51B1 = Bay.staticGetDao().findByDomainId(A51, "B1");
		Assert.assertNotNull(A51B1);

		Bay A51B2 = Bay.staticGetDao().findByDomainId(A51, "B2");
		Assert.assertNull(A51B2);

		Bay A51B3 = Bay.staticGetDao().findByDomainId(A51, "B3");
		Assert.assertNull(A51B3);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		// Check if we can clone an aisle that doesn't exist.
		// This should produce useful error messages.
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Aisle,A53,Clone(A52),,,,,12.85,48.45,X,120\r\n"; //
		importAislesData(facility, csvString2);

		Aisle A512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(A512);

		Aisle A532 = Aisle.staticGetDao().findByDomainId(facility, "A53");
		Assert.assertNull(A532);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testCloneAisleSlotCount() {
		// For DEV-618
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,32,0,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get A51
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);

		// Check slot counts on A51
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisle51, "B1");
		Assert.assertNotNull(bayA51B1);

		// Aisle 51 - Bay 1 - Tier 1
		Tier tierA51B1T1 = Tier.staticGetDao().findByDomainId(bayA51B1, "T1");
		Assert.assertNotNull(tierA51B1T1);

		List<Location> slotsA51B1T1 = tierA51B1T1.getActiveChildren();
		Assert.assertEquals(1, slotsA51B1T1.size());

		// Aisle 51 - Bay 1 - Tier 2
		Tier tierA51B1T2 = Tier.staticGetDao().findByDomainId(bayA51B1, "T2");
		Assert.assertNotNull(tierA51B1T2);

		List<Location> slotsA51B1T2 = tierA51B1T2.getActiveChildren();
		Assert.assertEquals(2, slotsA51B1T2.size());

		// Check slot counts on A51
		Bay bayA52B1 = Bay.staticGetDao().findByDomainId(aisle52, "B1");
		Assert.assertNotNull(bayA52B1);

		// Aisle 51 - Bay 1 - Tier 1
		Tier tierA52B1T1 = Tier.staticGetDao().findByDomainId(bayA52B1, "T1");
		Assert.assertNotNull(tierA52B1T1);

		List<Location> slotsA52B1T1 = tierA52B1T1.getActiveChildren();
		Assert.assertEquals(1, slotsA52B1T1.size());

		// Aisle 51 - Bay 1 - Tier 2
		Tier tierA52B1T2 = Tier.staticGetDao().findByDomainId(bayA52B1, "T2");
		Assert.assertNotNull(tierA52B1T2);

		List<Location> slotsA52B1T2 = tierA52B1T2.getActiveChildren();
		Assert.assertEquals(2, slotsA52B1T2.size());

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testCloneBay() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B2,CLONE(B1),,,,,\r\n" //
				+ "Bay,B3,CLONE(B1),,,,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get aisle A51 and check
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		// Check if the second bay exists and its tiers
		Bay bayA51B2 = Bay.staticGetDao().findByDomainId(aisle51, "B2");
		Assert.assertNotNull(bayA51B2);

		Tier tierA51B2T1 = Tier.staticGetDao().findByDomainId(bayA51B2, "T1");
		Assert.assertNotNull(tierA51B2T1);

		Tier tierA51B2T2 = Tier.staticGetDao().findByDomainId(bayA51B2, "T2");
		Assert.assertNotNull(tierA51B2T2);

		// Check if the third bay exists and its tiers
		Bay bayA51B3 = Bay.staticGetDao().findByDomainId(aisle51, "B3");
		Assert.assertNotNull(bayA51B3);

		Tier tierA51B3T1 = Tier.staticGetDao().findByDomainId(bayA51B3, "T1");
		Assert.assertNotNull(tierA51B3T1);

		Tier tierA51B3T2 = Tier.staticGetDao().findByDomainId(bayA51B3, "T2");
		Assert.assertNotNull(tierA51B3T2);

		// Check that the number of slots in the tiers is correct
		List<Location> slotsA51B3T1 = tierA51B3T1.getActiveChildren();
		Assert.assertEquals(1, slotsA51B3T1.size());

		List<Location> slotsA51B3T2 = tierA51B3T2.getActiveChildren();
		Assert.assertEquals(2, slotsA51B3T2.size());

		// Get aisle A52 and check
		Aisle aisle52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisle52);

		// Check if the second bay exists and its tiers
		Bay bayA52B2 = Bay.staticGetDao().findByDomainId(aisle52, "B2");
		Assert.assertNotNull(bayA52B2);

		Tier tierA52B2T1 = Tier.staticGetDao().findByDomainId(bayA52B2, "T1");
		Assert.assertNotNull(tierA52B2T1);

		Tier tierA52B2T2 = Tier.staticGetDao().findByDomainId(bayA52B2, "T2");
		Assert.assertNotNull(tierA52B2T2);

		// Check if the third bay exists and its tiers
		Bay bayA52B3 = Bay.staticGetDao().findByDomainId(aisle52, "B3");
		Assert.assertNotNull(bayA52B3);

		Tier tierA52B3T1 = Tier.staticGetDao().findByDomainId(bayA52B3, "T1");
		Assert.assertNotNull(tierA52B3T1);

		Tier tierA52B3T2 = Tier.staticGetDao().findByDomainId(bayA52B3, "T2");
		Assert.assertNotNull(tierA52B3T2);

		// Check that the number of slots in the tiers is correct
		List<Location> slotsA52B3T1 = tierA52B3T1.getActiveChildren();
		Assert.assertEquals(1, slotsA52B3T1.size());

		List<Location> slotsA52B3T2 = tierA52B3T2.getActiveChildren();
		Assert.assertEquals(2, slotsA52B3T2.size());

		// Check the number of LEDS on two tiers
		short firstLedT1 = tierA52B3T1.getFirstLedNumAlongPath();
		short lastLedT1 = tierA52B3T1.getLastLedNumAlongPath();
		Assert.assertEquals(32, (lastLedT1 - firstLedT1) + 1);

		short firstLedT2 = tierA52B3T2.getFirstLedNumAlongPath();
		short lastLedT2 = tierA52B3T2.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLedT2 - firstLedT2) + 1);

		this.getTenantPersistenceService().commitTransaction();
	}

	// FIXME
	@Test
	public final void testBadCloneBay() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B4,CLONE(B1),,,,,\r\n" // Everything after here should be dropped
				+ "Bay,B2,CLONE(B4),,,,,\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get aisle A51 and check
		Aisle aisle51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisle51);

		Bay bayA51B4 = Bay.staticGetDao().findByDomainId(aisle51, "B4");
		Assert.assertNull(bayA51B4);

		Bay bayA51B2 = Bay.staticGetDao().findByDomainId(aisle51, "B2");
		Assert.assertNull(bayA51B2);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Check that we cannot clone nonexistent bays
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B2,CLONE(B1),,,,,\r\n" //
				+ "Bay,B3,CLONE(B4),,,,,\r\n"; //
		importAislesData(facility, csvString2);

		Aisle aisleA512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisleA512);

		Bay A512B3 = Bay.staticGetDao().findByDomainId(aisleA512, "B3");
		Assert.assertNull(A512B3);

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Check that we cannot define and clone the same bay in the same line.
		// Check error logs for a warning about this. Nothing should be done.
		String csvString3 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B1,CLONE(B1),,,,,\r\n" //
				+ "Bay,B2,CLONE(B1),,,,,\r\n"; //
		importAislesData(facility, csvString3);

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testCloneBayOrderings() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B2,CLONE(B1),,,,,\r\n" //
				+ "Bay,B3,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Bay,B4,CLONE(B1),,,,,\r\n" //
				+ "Aisle,A52,Clone(A51),,,,,12.85,48.45,X,120\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE5X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Check all the aisles exist
		Aisle aisleA51 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisleA51);

		Aisle aisleA52 = Aisle.staticGetDao().findByDomainId(facility, "A52");
		Assert.assertNotNull(aisleA52);

		// Check the bays exist
		Bay bayA51B1 = Bay.staticGetDao().findByDomainId(aisleA51, "B1");
		Assert.assertNotNull(bayA51B1);

		Bay bayA51B2 = Bay.staticGetDao().findByDomainId(aisleA51, "B2");
		Assert.assertNotNull(bayA51B2);

		Bay bayA51B3 = Bay.staticGetDao().findByDomainId(aisleA51, "B3");
		Assert.assertNotNull(bayA51B3);

		Bay bayA51B4 = Bay.staticGetDao().findByDomainId(aisleA51, "B4");
		Assert.assertNotNull(bayA51B4);

		Bay bayA52B1 = Bay.staticGetDao().findByDomainId(aisleA52, "B1");
		Assert.assertNotNull(bayA52B1);

		Bay bayA52B2 = Bay.staticGetDao().findByDomainId(aisleA52, "B2");
		Assert.assertNotNull(bayA52B2);

		Bay bayA52B3 = Bay.staticGetDao().findByDomainId(aisleA52, "B3");
		Assert.assertNotNull(bayA52B3);

		Bay bayA52B4 = Bay.staticGetDao().findByDomainId(aisleA52, "B4");
		Assert.assertNotNull(bayA52B4);

		// Check some of the tiers exist - also slots and leds
		Tier tierA51B2T1 = Tier.staticGetDao().findByDomainId(bayA51B2, "T1");
		Assert.assertNotNull(tierA51B2T1);

		List<Location> slotsA51B2T1 = tierA51B2T1.getActiveChildren();
		Assert.assertEquals(1, slotsA51B2T1.size());

		short firstLedT1 = tierA51B2T1.getFirstLedNumAlongPath();
		short lastLedT1 = tierA51B2T1.getLastLedNumAlongPath();
		Assert.assertEquals(32, (lastLedT1 - firstLedT1) + 1);

		Tier tierA51B2T2 = Tier.staticGetDao().findByDomainId(bayA51B2, "T2");
		Assert.assertNotNull(tierA51B2T2);

		List<Location> slotsA51B2T2 = tierA51B2T2.getActiveChildren();
		Assert.assertEquals(2, slotsA51B2T2.size());

		short firstLedT2 = tierA51B2T2.getFirstLedNumAlongPath();
		short lastLedT2 = tierA51B2T2.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLedT2 - firstLedT2) + 1);

		// Check the number of tiers in A52 B3
		List<Location> tiersA51B3 = bayA52B3.getActiveChildren();
		Assert.assertEquals(1, tiersA51B3.size());

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A51,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,1,32,0,,\r\n" //
				+ "Tier,T2,,2,40,0,,\r\n" //
				+ "Bay,B2,CLONE(B1),,,,,\r\n" //
				+ "Bay,B3,CLONE(B2),,,,,\r\n"; //
		importAislesData(facility, csvString2);

		// Check if cloning a cloned bay works
		Aisle aisleA512 = Aisle.staticGetDao().findByDomainId(facility, "A51");
		Assert.assertNotNull(aisleA512);

		Bay bayA51B22 = Bay.staticGetDao().findByDomainId(aisleA512, "B2");
		Assert.assertNotNull(bayA51B22);

		Bay bayA51B32 = Bay.staticGetDao().findByDomainId(aisleA512, "B3");
		Assert.assertNotNull(bayA51B32);

		// Check tiers of aisles
		Tier tierA51B2T12 = Tier.staticGetDao().findByDomainId(bayA51B22, "T1");
		Assert.assertNotNull(tierA51B2T12);

		Tier tierA51B2T22 = Tier.staticGetDao().findByDomainId(bayA51B22, "T2");
		Assert.assertNotNull(tierA51B2T22);

		Tier tierA51B3T12 = Tier.staticGetDao().findByDomainId(bayA51B32, "T1");
		Assert.assertNotNull(tierA51B3T12);

		Tier tierA51B3T22 = Tier.staticGetDao().findByDomainId(bayA51B32, "T2");
		Assert.assertNotNull(tierA51B3T22);

		// Check the slot counts of B3
		List<Location> slotsA51B3T12 = tierA51B3T12.getActiveChildren();
		Assert.assertEquals(1, slotsA51B3T12.size());

		List<Location> slotsA51B3T22 = tierA51B3T22.getActiveChildren();
		Assert.assertEquals(2, slotsA51B3T22.size());

		// Check the LED count of the B3 tiers
		short firstLedB3T1 = tierA51B3T12.getFirstLedNumAlongPath();
		short lastLedB3T1 = tierA51B3T12.getLastLedNumAlongPath();
		Assert.assertEquals(32, (lastLedB3T1 - firstLedB3T1) + 1);

		short firstLedB3T2 = tierA51B3T22.getFirstLedNumAlongPath();
		short lastLedB3T2 = tierA51B3T22.getLastLedNumAlongPath();
		Assert.assertEquals(40, (lastLedB3T2 - firstLedB3T2) + 1);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testPath() {
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F3X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get A31
		Aisle aisle31 = Aisle.staticGetDao().findByDomainId(facility, "A31");
		Assert.assertNotNull(aisle31);

		Bay bayA31B1 = Bay.staticGetDao().findByDomainId(aisle31, "B1");
		Bay bayA31B2 = Bay.staticGetDao().findByDomainId(aisle31, "B2");

		// For 2 bays, 2 tier, zigzagB1S1Side, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierA31B1T1 = Tier.staticGetDao().findByDomainId(bayA31B1, "T1");
		Tier tierA31B2T1 = Tier.staticGetDao().findByDomainId(bayA31B2, "T1");

		// Get A32
		Aisle aisle32 = Aisle.staticGetDao().findByDomainId(facility, "A32");
		Assert.assertNotNull(aisle32);
		Bay bayA32B1 = Bay.staticGetDao().findByDomainId(aisle32, "B1");
		Bay bayA32B2 = Bay.staticGetDao().findByDomainId(aisle32, "B2");
		Tier tierA32B1T1 = Tier.staticGetDao().findByDomainId(bayA32B1, "T1");
		Tier tierA32B2T1 = Tier.staticGetDao().findByDomainId(bayA32B2, "T1");

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
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		checkLocations(facilityID, retrievedPathID, "F3X.1.0", aisle31);

		// If you step into associatePathSegment, you will see that it finds the segment by UUID, and its location count was 1 and goes to 2.
		aisle32.associatePathSegment(segmentId);

		checkLocations(facilityID, retrievedPathID, "F3X.1.0", aisle31, aisle32);

		// Lowest path values at A31B2T1S5 and A32B2T1S5
		// Lowest LED values should be A31B1T1S1 and A32B2T1S5
		Slot firstA32SlotOnPath = Slot.staticGetDao().findByDomainId(tierA32B2T1, "S5");

		Short lowestLEDforA32 = firstA32SlotOnPath.getFirstLedNumAlongPath();
		Assert.assertTrue(lowestLEDforA32 < 4);
		Slot lowestA31LedSLot = Slot.staticGetDao().findByDomainId(tierA31B1T1, "S1");
		Short lowestLEDforA31 = lowestA31LedSLot.getFirstLedNumAlongPath();
		Assert.assertTrue(lowestLEDforA31 < 4);

		// old bug got the same position for first two bays. Check that.

		// Values are null. Lets get new tier references after the path was applied, as the posAlongPath is null on old reference
		tierA31B1T1 = Tier.staticGetDao().findByDomainId(bayA31B1, "T1");
		tierA31B2T1 = Tier.staticGetDao().findByDomainId(bayA31B2, "T1");
		tierA32B1T1 = Tier.staticGetDao().findByDomainId(bayA32B1, "T1");
		tierA32B2T1 = Tier.staticGetDao().findByDomainId(bayA32B2, "T1");

		Double valueTierA31B1T1 = tierA31B1T1.getAnchorPosX();
		Double valueTierA32B2T1 = tierA32B2T1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueTierA31B1T1);
		Assert.assertEquals((Double) 0.0, valueTierA32B2T1);

		// Slots increase the same way in A32. So S1 anchor will always be 0 and S5 anchor will not
		Slot slotA31B1T1S1 = Slot.staticGetDao().findByDomainId(tierA31B1T1, "S1");
		Slot slotA31B1T1S5 = Slot.staticGetDao().findByDomainId(tierA31B1T1, "S5");
		Slot slotA31B2T1S1 = Slot.staticGetDao().findByDomainId(tierA31B2T1, "S1");
		Slot slotA31B2T1S5 = Slot.staticGetDao().findByDomainId(tierA31B2T1, "S5");
		Slot slotA32B1T1S1 = Slot.staticGetDao().findByDomainId(tierA32B1T1, "S1");
		Slot slotA32B1T1S5 = Slot.staticGetDao().findByDomainId(tierA32B1T1, "S5");
		Slot slotA32B2T1S1 = Slot.staticGetDao().findByDomainId(tierA32B2T1, "S1");
		Slot slotA32B2T1S5 = Slot.staticGetDao().findByDomainId(tierA32B2T1, "S5");

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

		// Path defined. Therefore, can know left side yet. These values wrong, or test needs improvement
		// TODO
		Assert.assertEquals("", aisle31.getMetersFromLeft());
		Assert.assertEquals("", aisle32.getMetersFromLeft());
		Assert.assertEquals("1.15", bayA32B1.getMetersFromLeft());
		Assert.assertEquals("0", bayA31B1.getMetersFromLeft());
		Assert.assertEquals("0", tierA31B1T1.getMetersFromLeft());
		Assert.assertEquals("1.15", tierA32B1T1.getMetersFromLeft());
		Assert.assertEquals("0.92", slotA32B1T1S1.getMetersFromLeft());
		Assert.assertEquals("0", slotA32B1T1S5.getMetersFromLeft());

		this.getTenantPersistenceService().commitTransaction();

	}

	protected void checkLocations(UUID facilityID, UUID retrievedPathID, String segmentDomainId, Aisle... locations) {
		Path retrievedPath;
		retrievedPath = Path.staticGetDao().findByPersistentId(retrievedPathID);
		// this segment should have one location now. However, the old reference is stale and may know its aisles (used to be). Re-get
		PathSegment retrievedSegment = PathSegment.staticGetDao().findByDomainId(retrievedPath, segmentDomainId);
		Assert.assertEquals(Arrays.asList(locations), retrievedSegment.getLocations());

		// Let's check locations on the path segment, derived different ways
		// original aPath return while created:
		PathSegment memberSegment = retrievedPath.getPathSegment(0);
		Assert.assertEquals(memberSegment, retrievedSegment);
		Assert.assertEquals(Arrays.asList(locations), memberSegment.getLocations());

		//From the facility now (after associating aisle to path segment)
		Facility retrievedFacility = Facility.staticGetDao().findByPersistentId(facilityID);
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
		this.getTenantPersistenceService().beginTransaction();

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
		Facility facility = Facility.createFacility("F-AISLE6X", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get the aisle
		Aisle aisle61 = Aisle.staticGetDao().findByDomainId(facility, "A61");
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

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		aisle61.associatePathSegment(persistStr);
		// This should have recomputed all positions along path.  Aisle, bay, tier, and slots should have position now
		// Although the old reference to aisle before path association would not.

		aisle61 = Aisle.staticGetDao().findByDomainId(facility, "A61");
		Bay bayA61B1 = Bay.staticGetDao().findByDomainId(aisle61, "B1");
		Bay bayA61B2 = Bay.staticGetDao().findByDomainId(aisle61, "B2");

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

		Tier tierA61B1T1 = Tier.staticGetDao().findByDomainId(bayA61B1, "T1");
		Assert.assertNotNull(tierA61B1T1);
		Tier tierA61B2T1 = Tier.staticGetDao().findByDomainId(bayA61B2, "T1");
		Assert.assertNotNull(tierA61B2T1);
		Slot slotS1 = Slot.staticGetDao().findByDomainId(tierA61B1T1, "S1");
		Assert.assertNull(slotS1); // no slots

		String tierB1Meters = tierA61B1T1.getPosAlongPathui();
		String tierB2Meters = tierA61B2T1.getPosAlongPathui();
		Assert.assertNotEquals(tierB1Meters, tierB2Meters); // tier spans the bay, so should be the same
		// Bay1 and bay2 path position differ by about 1.15 meters;  bay is 115 cm long.

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testMultipleReferencesForSimpleField() {
		// DAO-correct
		// This was one common ebeans bug. This test gets references to the same slot at different times, and sees that they are all synchronized.

		this.getTenantPersistenceService().beginTransaction();
		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A29,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE29", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		// Get the objects we will use
		Aisle aisle29 = Aisle.staticGetDao().findByDomainId(facility, "A29");
		Assert.assertNotNull(aisle29);

		Bay bayA16B1 = Bay.staticGetDao().findByDomainId(aisle29, "B1");
		Bay bayA16B2 = Bay.staticGetDao().findByDomainId(aisle29, "B2");
		Assert.assertNotNull(bayA16B2);

		Tier tierB1T1 = Tier.staticGetDao().findByDomainId(bayA16B1, "T1");
		Assert.assertNotNull(tierB1T1);
		Tier tierB2T1 = Tier.staticGetDao().findByDomainId(bayA16B2, "T1");
		Assert.assertNotNull(tierB2T1);

		Slot slotB1T1S5 = Slot.staticGetDao().findByDomainId(tierB1T1, "S5");
		Assert.assertNotNull(slotB1T1S5);

		// The modification is trivial: activation and deactivation of a slot. Verify starting condition.
		Assert.assertTrue(slotB1T1S5.getActive());

		slotB1T1S5.setActive(false); // but not persisted yet.
		Assert.assertFalse(slotB1T1S5.getActive());
		Slot.staticGetDao().store(slotB1T1S5);
		Assert.assertFalse(slotB1T1S5.getActive()); // Just showing that the store did not matter on the local reference

		// Get it again, although from the old facility reference. Does not give the database version.
		slotB1T1S5 = (Slot) facility.findSubLocationById("A29.B1.T1.S5");
		Assert.assertFalse(slotB1T1S5.getActive());
		// true in database. False on our object		

		// See if we can somehow get what the database has. No!
		List<Facility> listB = Facility.staticGetDao().getAll();
		Facility facilityB = listB.get(0);
		Slot slotB1T1S5B = (Slot) facilityB.findSubLocationById("A29.B1.T1.S5");
		Assert.assertFalse(slotB1T1S5B.getActive());
		// true in database. False in this transaction, even though we really tried to get it straight from the DAO 

		// Persist it by closing the transaction
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());
		// Old reference. Now false in database, and false on this reference. Cannot tell if the reference actually got updated by hibernate.

		slotB1T1S5.setActive(true); // but not persisted yet.
		Slot.staticGetDao().store(slotB1T1S5); // object attached to the transaction, but still not persisted
		Assert.assertTrue(slotB1T1S5.getActive());

		// How is our old reference?
		Assert.assertTrue(slotB1T1S5B.getActive()); // Also true, for the old transaction reference after refetch

		// Get it from scratch again.
		List<Facility> listC = Facility.staticGetDao().getAll();
		Facility facilityC = listC.get(0);
		Slot slotB1T1S5C = (Slot) facilityC.findSubLocationById("A29.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5C.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive()); // old reference is still active.

		// close the transaction
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();

		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive());
		Assert.assertTrue(slotB1T1S5C.getActive());
		// We should be able to get it again.
		List<Facility> listD = Facility.staticGetDao().getAll();
		Facility facilityD = listD.get(0);
		Slot slotB1T1S5D = (Slot) facilityD.findSubLocationById("A29.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5D.getActive());
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5B.getActive());

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void outOfOrderStores() {
		// DAO-correct
		// Attempts to create various detached and reattach object scenarios 
		// Need to add database queries to this later to see if and when change was persisted.

		this.getTenantPersistenceService().beginTransaction();

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A31,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //
		Facility facility = Facility.createFacility("F-AISLE31", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();

		List<Facility> listA = Facility.staticGetDao().getAll();
		Facility facilityA = listA.get(0);
		Slot slotB1T1S5 = (Slot) facilityA.findSubLocationById("A31.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5.getActive());

		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("Case 1: Modify outside a transaction. Then store inside a transaction");
		slotB1T1S5.setActive(false);
		this.getTenantPersistenceService().beginTransaction();
		slotB1T1S5.setLedChannel((short) 2); // set another field, making it look more like a mistake may be.
		Slot.staticGetDao().store(slotB1T1S5);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 2: multiple stores in the same transaction");
		this.getTenantPersistenceService().beginTransaction();
		slotB1T1S5.setActive(true);
		Slot.staticGetDao().store(slotB1T1S5);
		slotB1T1S5.setLedChannel((short) 3);
		Slot.staticGetDao().store(slotB1T1S5);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5.getLedChannel() == 3);
	}

	/**
	 * Per CD_0129. LED offsets account for gaps between bays, primarily used for tier-wise layout although it could be applied to zigzag (if you can understand how to apply it--dioubtful)
	 * Also check that cloning bays works as specified, without taking the offset as the offset is likely to need changing.
	 * Also check that aisle clone does take the bay offsets.
	 * Offset in tier or bay lines goes in controllerLED field.
	 * 
	 * Environmental lights are 12.5cm per addressable element. Divided evenly, that is 19.5 elements per 8 feet.
	 * 8-foot rack is 243.75 cm.  Our best light tube have 80 LEDs within that, with a small gap per end.
	 */
	@Test
	public final void testBasicEnvironmentalConfig() {
		beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A71,,,,,tierB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,244,,,,\r\n" //
				+ "Tier,T1,,10,20,0,,\r\n" //
				+ "Bay,B2,Clone(B1),,,,\r\n"; //
		Facility facility = Facility.createFacility("F-CLONE71", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);
		commitTransaction();

		LOGGER.info("1: Check the tier first/last");
		beginTransaction();
		facility = facility.reload();
		assertLeds(facility, "A71.B1.T1", 1, 20);

		LOGGER.info("2: Current algorithm is giving one element each for a 10/20 configuration. Check B1");
		assertSingleLedElement(facility, "A71.B1.T1.S1", 1);
		assertSingleLedElement(facility, "A71.B1.T1.S2", 3);
		assertSingleLedElement(facility, "A71.B1.T1.S3", 5);
		assertSingleLedElement(facility, "A71.B1.T1.S4", 7);
		assertSingleLedElement(facility, "A71.B1.T1.S5", 9);
		assertSingleLedElement(facility, "A71.B1.T1.S6", 11);
		assertSingleLedElement(facility, "A71.B1.T1.S7", 13);
		assertSingleLedElement(facility, "A71.B1.T1.S8", 15);
		assertSingleLedElement(facility, "A71.B1.T1.S9", 17);
		assertSingleLedElement(facility, "A71.B1.T1.S10", 19);

		LOGGER.info("3: Check the end slots of B2. Should be symetrical");
		assertSingleLedElement(facility, "A71.B2.T1.S1", 21);
		assertSingleLedElement(facility, "A71.B2.T1.S10", 39);

		commitTransaction();

	}

	private Short toShort(int value) {
		return (short) value;
	}

	private void assertSingleLedElement(Facility inFacility, String locId, int expectedLed) {
		assertLeds(inFacility, locId, expectedLed, expectedLed);
	}

	/**
	 * This allows 0 to match null if the Led values are not defined.
	 */
	private void assertLeds(Facility inFacility, String locId, int expectedFirst, int expectedLast) {
		Location loc = inFacility.findSubLocationById(locId);
		Assert.assertNotNull(loc);
		Short firstValue = loc.getFirstLedNumAlongPath() == null ? 0 : loc.getFirstLedNumAlongPath();
		Short lasstValue = loc.getFirstLedNumAlongPath() == null ? 0 : loc.getLastLedNumAlongPath();
		Assert.assertEquals(toShort(expectedFirst), firstValue);
		Assert.assertEquals(toShort(expectedLast), lasstValue);
	}

	@Test
	public final void testBayLedOffsets() {
		beginTransaction();

		// A71 definition has value 3 in controllerLED field for the tier line. Show that it does nothing
		// A72 definition has value 3 in controllerLED field for the bay line. Show that offsets
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A71,,,,,tierB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,244,,,,\r\n" //
				+ "Tier,T1,,10,20,0,3,\r\n" //
				+ "Bay,B2,Clone(B1),,,,\r\n" //
				+ "Bay,B3,Clone(B1),,,,\r\n" //
				+ "Aisle,A72,,,,,tierB1S1Side,12.85,63.45,X,120\r\n" //
				+ "Bay,B1,244,,,,\r\n" //
				+ "Tier,T1,,10,20,0,,\r\n" //
				+ "Bay,B2,Clone(B1),,,,3\r\n" //
				+ "Bay,B3,Clone(B2),,,,a\r\n" //
				+ "Bay,B4,Clone(B1),,,,-1\r\n"//
				+ "Bay,B5,244,,,,-1\r\n" //
				+ "Tier,T1,,10,20,0,5,\r\n";//

		Facility facility = Facility.createFacility("F-CLONE71", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);
		commitTransaction();

		LOGGER.info("1: Check the end slots of Aisle 71 B1 and B2.");
		beginTransaction();
		facility = facility.reload();
		assertLeds(facility, "A71.B1.T1", 1, 20);
		assertLeds(facility, "A71.B2.T1", 21, 40);
		assertSingleLedElement(facility, "A71.B1.T1.S1", 1);
		assertSingleLedElement(facility, "A71.B1.T1.S10", 19);
		assertSingleLedElement(facility, "A71.B2.T1.S1", 21);
		assertSingleLedElement(facility, "A71.B2.T1.S10", 39);
		assertSingleLedElement(facility, "A71.B3.T1.S1", 41);
		assertSingleLedElement(facility, "A71.B3.T1.S10", 59);

		LOGGER.info("2: Check the end slots of Aisle 72 B1 and B2. B2 is offset +3");
		assertLeds(facility, "A72.B1.T1", 1, 20);
		assertLeds(facility, "A72.B2.T1", 21, 40);
		assertSingleLedElement(facility, "A72.B1.T1.S1", 1);
		assertSingleLedElement(facility, "A72.B1.T1.S10", 19);
		assertSingleLedElement(facility, "A72.B2.T1.S1", 24);
		assertSingleLedElement(facility, "A72.B2.T1.S10", 42);

		LOGGER.info("3: Check the clone cases. the main point is the offsets do not accumulate");
		LOGGER.info("3a: B3 cloned B2 with offset a which should yield warn and zero");
		LOGGER.info("3b: Check the clone cases. B4 cloned B1 with offset 1");
		assertSingleLedElement(facility, "A72.B3.T1.S1", 41);
		assertSingleLedElement(facility, "A72.B3.T1.S10", 59);
		assertSingleLedElement(facility, "A72.B4.T1.S1", 60);
		assertSingleLedElement(facility, "A72.B4.T1.S10", 78);

		LOGGER.info("4: See the bay offset apply even if not the clone case");
		// Here we have an error value for value in a tier line
		assertSingleLedElement(facility, "A72.B5.T1.S1", 80);
		assertSingleLedElement(facility, "A72.B5.T1.S10", 98);

		commitTransaction();

	}

	/**
	 * Van's endcaps are described in https://codeshelf.atlassian.net/wiki/display/TD/CD_0165+Tier+share+LEDs+enhancement
	 * The goal is to clone the light from one tier to the next.
	 */
	@Test
	public final void testVansEndcap() {
		beginTransaction();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A78,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,144,,,,\r\n" //
				+ "Tier,T1,,5,42,10,,\r\n" //
				+ "Tier,T2,,5,42,30,,\r\n" //
				+ "Tier,T3,,5,42,60,,\r\n" //
				+ "Tier,T4,,5,42,80,,\r\n" //
				+ "Tier,T5,,5,42,110,,\r\n" //
				+ "Tier,T6,,5,42,130,,\r\n" //
				+ "Tier,T7,,5,42,160,,\r\n" //
				+ "Tier,T8,,5,42,180,,\r\n" //
				+ "Tier,T9,,5,42,210,,\r\n";//

		Facility facility = Facility.createFacility("F-78", "TEST", Point.getZeroPoint());
		importAislesData(facility, csvString);
		commitTransaction();

		LOGGER.info("1: Check the end slots LEDs without any funny business.");
		beginTransaction();
		facility = facility.reload();
		assertLeds(facility, "A78.B1.T9", 1, 42);
		assertLeds(facility, "A78.B1.T9.S1", 3, 6);
		assertLeds(facility, "A78.B1.T9.S5", 38, 41);
		assertLeds(facility, "A78.B1.T8.S1", 80, 83);
		assertLeds(facility, "A78.B1.T8.S5", 45, 48);
		assertLeds(facility, "A78.B1.T7.S1", 87, 90);
		assertLeds(facility, "A78.B1.T7.S5", 122, 125);
		assertLeds(facility, "A78.B1.T1.S1", 339, 342);
		assertLeds(facility, "A78.B1.T1.S5", 374, 377);
		
		LOGGER.info("1b: Show that no indicator lights were set by the basic file.");
		assertLeds(facility, "A78", 0, 0);
		assertLeds(facility, "A78.B1", 0, 0);

		commitTransaction();

		LOGGER.info("2: Directly call the API to set tiers as we want it.");
		beginTransaction();
		facility = facility.reload();
		Tier tier8 = (Tier) facility.findSubLocationById("A78.B1.T8");
		Tier tier7 = (Tier) facility.findSubLocationById("A78.B1.T7");
		Tier tier6 = (Tier) facility.findSubLocationById("A78.B1.T6");
		Tier tier5 = (Tier) facility.findSubLocationById("A78.B1.T5");
		logTierLeds(tier8);
		logTierLeds(tier7);
		logTierLeds(tier6);
		logTierLeds(tier5);
		// tier8 starts are 45,54,64,72,80. so lets set the same for T7
		logSlotLedParameters(tier8);
		// see 43,42,false,4,"45/54/64/72/80" in the consol
		tier7.setSlotTierLEDs(43, 42, false, 4, "45/54/64/72/80");
		logSlotLedParameters(tier7);
		logTierLeds(tier7);
		// show that T8 and T7 are the same
		assertLeds(facility, "A78.B1.T7.S1", 80, 83);
		assertLeds(facility, "A78.B1.T7.S5", 45, 48);
		assertLeds(facility, "A78.B1.T8.S1", 80, 83);
		assertLeds(facility, "A78.B1.T8.S5", 45, 48);

		assertLeds(facility, "A78.B1", 0, 0);
		Bay bay1 = (Bay) facility.findSubLocationById("A78.B1");
		Aisle aisle78 = (Aisle) facility.findSubLocationById("A78");
		bay1.setIndicatorLedValuesInteger(4, 6);
		aisle78.setIndicatorLedValuesInteger(14, 16);
		tier5.setIndicatorLedValuesInteger(24, 26);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		assertLeds(facility, "A78.B1", 4, 6);
		assertLeds(facility, "A78", 14, 16);
		assertLeds(facility, "A78.B1.T5", 24, 26);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		// Does reload clear out what was set before?
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A78,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,144,,,,\r\n" //
				+ "Tier,T1,,5,42,10,,\r\n" //
				+ "Tier,T2,,5,42,30,,\r\n" //
				+ "Tier,T3,,5,42,60,,\r\n" //
				+ "Tier,T4,,5,42,80,,\r\n" //
				+ "Tier,T5,,5,42,110,,\r\n" //
				+ "Tier,T6,,5,42,130,,\r\n" //
				+ "Tier,T7,,5,42,160,,\r\n" //
				+ "Tier,T8,,5,42,180,,\r\n" //
				+ "Tier,T9,,5,42,210,,\r\n";//
				// + "Function,SetIndicators,A78.B1,4,6,\r\n";//

		importAislesData(facility, csvString2);
		assertLeds(facility, "A78.B1", 0, 0);
		commitTransaction();

	}

	private void logTierLeds(Tier inTier) {
		String tierId = inTier.getDomainId();
		LOGGER.info("{} first:{} last:{}", tierId, inTier.getFirstLedNumAlongPath(), inTier.getLastLedNumAlongPath());
		for (Slot slot : inTier.getSlotsInDomainIdOrder()) {
			LOGGER.info("{}.{}: {}-{}", tierId, slot.getDomainId(), slot.getFirstLedNumAlongPath(), slot.getLastLedNumAlongPath());

		}
	}

	/**
	 * Main purpose is to produce the parameters for a call to setSlotTierLEDs(). See it in the console
	 */
	private void logSlotLedParameters(Tier inTier) {
		int firstLed = inTier.getFirstLedNumAlongPath();
		int totalLed = inTier.getLastLedNumAlongPath() - firstLed + 1;
		boolean increaseFromAnchor = inTier.isLowerLedNearAnchor();
		String slotStarts = "";
		int ledsPerSlot = 0;
		List<Slot> slots = inTier.getSlotsInDomainIdOrder();
		if (!increaseFromAnchor) {
			Collections.reverse(slots);
		}
		for (Slot slot : slots) {
			slotStarts += "/" + slot.getFirstLedNumAlongPath();
			ledsPerSlot = slot.getLastLedNumAlongPath() - slot.getFirstLedNumAlongPath() + 1;
		}
		// strip off the first 
		slotStarts = slotStarts.substring(1);
		slotStarts = "\"" + slotStarts + "\"";

		LOGGER.info("{} setSlotTierLEDs parameters", inTier);
		LOGGER.info("{},{},{},{},{}", firstLed, totalLed, increaseFromAnchor, ledsPerSlot, slotStarts);
		/*setSlotTierLEDs(int inTierStartLed,
			int inLedCountTier,
			boolean inLowerLedNearAnchor,
			int inLedsPerSlot,
			String inSlotStartingLeds) */
	}

}
