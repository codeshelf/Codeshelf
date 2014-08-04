/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
// domain objects needed
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.flyweight.command.NetGuid;

/**
 * @author ranstrom
 * Also see createAisleTest() in FacilityTest.java
 */
public class AisleImporterTest extends DomainTestABC {

	private PathSegment addPathSegmentForTest(final String inSegmentId,
		final Path inPath,
		final Integer inSegmentOrder,
		Double inStartX,
		Double inStartY,
		Double inEndX,
		Double inEndY) {

		Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, inStartX, inStartY, 0.0);
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndX, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentId, inSegmentOrder, head, tail);
		return returnSeg;
	}

	private Path createPathForTest(String inDomainId, Facility inFacility) {
		return inFacility.createPath(inDomainId);
	}

	@Test
	public final void testTierLeft() {
		if (false)
			return;

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
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
		organization.setDomainId("O-AISLE9");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE9", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE9");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

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
		// bay 2 should be 2.88 (relative to parent). Its anchor is also relative to parent
		Assert.assertTrue(pickFaceEndX == 4.88);
		Double anchorX = ((Bay) bay2).getAnchorPosX();
		Assert.assertTrue(anchorX == 2.44); // exactly equal to bay1 pickFaceEnd

		Assert.assertTrue(pickFaceEndY == 0.0);
		pickFaceEndX = ((Tier) tierB2T1).getPickFaceEndPosX();
		pickFaceEndY = ((Tier) tierB2T1).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX.equals(bay2EndX)); // tier will match the bay. Cannot use == for two different Double objects.
		Assert.assertTrue(pickFaceEndY == 0.0);
		pickFaceEndX = ((Slot) slotB1T2S3).getPickFaceEndPosX();
		pickFaceEndY = ((Slot) slotB1T2S3).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX > 0.813); // value about .813m: 3rd of 9 slots across 244 cm.
		pickFaceEndX = ((Slot) slotB2T2S3).getPickFaceEndPosX();
		pickFaceEndY = ((Slot) slotB2T2S3).getPickFaceEndPosY();
		Assert.assertTrue(pickFaceEndX == 1.22); // Bay 2 Tier 2 has 6 slots across 244 cm, so 3rd ends at 1.22

		// Check some vertices. The aisle and each bay should have 4 vertices.
		// aisle defined as an ISublocation. Cannot event cast it to call getVerticesInOrder()
		List<Vertex> vList1 = aisle2.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		// Vertex thirdV = (Vertex) vList1.get(2);

		// New side effect: creates some LED controllers. But if no network in our test system, may not happen
		// Assert.assertTrue(facility.countLedControllers() > 0);

	}

	@Test
	public final void testTierRight() {

		if (false)
			return;

		// Beside TierRight, this as two aisles, so it makes sure both get their leds properly set, and both vertices set
		// Not quite realistic; A10 and A20 are on top of each other. Same anchor point
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A10,,,,,TierRight,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,0,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,6,60,50,,\r\n" //
				+ "Tier,T2,,6,60,100,,\r\n" //
				+ "Aisle,A20,,,,,TierRight,12.85,43.45,X,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		Assert.assertTrue(slotB1T1S6.getLastLedNumAlongPath() == 69);

		Slot slotB2T1S1 = Slot.DAO.findByDomainId(tierB2T1, "S1");
		short slotB2T1S1First = slotB2T1S1.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1First == 53);
		short slotB2T1S1Last = slotB2T1S1.getLastLedNumAlongPath();
		Assert.assertTrue(slotB2T1S1Last == 59);

		// Check that vertices were computed for not-last aisle
		List<Vertex> vList1 = bayA10B1.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2); // each bay has the same depth
		Assert.assertTrue(xValue == 2.44);

		List<Vertex> vList2 = aisle.getVerticesInOrder();
		Assert.assertEquals(vList2.size(), 4);
		thirdV = (Vertex) vList2.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(yValue == 1.2);
		Assert.assertTrue(xValue == 17.73);

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
		Assert.assertTrue(xValue == 17.73);

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
		Assert.assertTrue(xValue == 17.73);

	}

	@Test
	public final void test32Led5Slot() {
		if (false)
			return;
		// the purpose of bay B1 is to compare this slotting algorithm to Jeff's hand-done goodeggs zigzag slots
		// the purpose of bay B2 is to check the sort and LEDs of more than 10 slots in a tier
		// the purpose of bays 9,10,11 is check the bay sort.
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A11,,,,,TierLeft,12.85,43.45,X,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		// This algorithm lights 3-6, 10-13, 16-19, 22-25, 28-31 with 2 guard low, and 1 guard high.
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

	}

	@Test
	public final void testZigzagLeft() {
		if (false)
			return;

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A12,,,,,zigzagLeft,12.85,43.45,X,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		Assert.assertTrue(slotB1T1S1First == 67);

		short slotB2T2S5First = slotB2T2S5.getFirstLedNumAlongPath();
		Assert.assertTrue(slotB2T2S5First == 131);
		Assert.assertTrue(slotB2T2S5.getLastLedNumAlongPath() == 134);

		// Test the obvious. For 2 bays, 3 tier, zigzagleft, tierB1T3 should start at led1. tierB2T3 should start at 97
		Tier tierB1T3 = Tier.DAO.findByDomainId(bayA12B1, "T3");
		Tier tierB2T3 = Tier.DAO.findByDomainId(bayA12B2, "T3");
		short tierB1T3First = tierB1T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T3First == 1);
		short tierB2T3First = tierB2T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T3First == 97);

	}

	@Test
	public final void testZigzagRightY() {
		if (false)
			return;

		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A13,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		Assert.assertTrue(pickFaceEndY == 1.15); // S5 is last slot of 115 cm tier

		// Check some vertices. The aisle and each bay should have 4 vertices.
		List<Vertex> vList1 = aisle.getVerticesInOrder();
		Assert.assertEquals(vList1.size(), 4);
		// the third point is the interesting one. Note index 0,1,2,3
		Vertex thirdV = (Vertex) vList1.get(2);
		Double xValue = thirdV.getPosX();
		Double yValue = thirdV.getPosY();
		Assert.assertTrue(xValue == 1.2); // depth was 120 cm, so 1.2 meters in the x direction
		Assert.assertTrue(yValue == 45.75);

		List<Vertex> vList2 = bayA13B1.getVerticesInOrder();
		Assert.assertEquals(vList2.size(), 4);
		thirdV = (Vertex) vList2.get(2);
		xValue = thirdV.getPosX();
		yValue = thirdV.getPosY();
		Assert.assertTrue(xValue == 1.2); // each bay has the same depth
		Assert.assertTrue(yValue == 1.15); // this bay is 115 cm wide

		// Test the obvious. For 2 bays, 3 tier, zigzagright, tierB1T3 should start at led 97. tierB2T3 should start at 1
		Tier tierB1T3 = Tier.DAO.findByDomainId(bayA13B1, "T3");
		Tier tierB2T3 = Tier.DAO.findByDomainId(bayA13B2, "T3");
		short tierB1T3First = tierB1T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB1T3First == 97);
		short tierB2T3First = tierB2T3.getFirstLedNumAlongPath();
		Assert.assertTrue(tierB2T3First == 1);
	}

	@Test
	public final void testMultiAisleZig() {

		// We seemed to have a bug in the parse where when processing A21 beans, we have m values set for A22. That is, A21 might come out as zigzagRight
		// So this tests Bay to bay attributes changing within an aisle, and tier attributes changing within a bay.

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A21,,,,,zigzagLeft,12.85,43.45,X,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,4,32,0,,\r\n" //
				+ "Bay,B2,141,,,,,\r\n" //
				+ "Tier,T1,,3,32,0,,\r\n" //
				+ "Tier,T2,,6,32,0,,\r\n" //
				+ "Aisle,A22,,,,,zigzagRight,12.85,53.45,Y,110,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A21");
		Assert.assertNotNull(aisle);

		Bay bayA21B1 = Bay.DAO.findByDomainId(aisle, "B1");
		Bay bayA21B2 = Bay.DAO.findByDomainId(aisle, "B2");

		// For 2 bays, 2 tier, zigzagleft, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierB1T1 = Tier.DAO.findByDomainId(bayA21B1, "T1");
		Tier tierB1T2 = Tier.DAO.findByDomainId(bayA21B1, "T2");
		Tier tierB2T2 = Tier.DAO.findByDomainId(bayA21B2, "T2");
		double b1T1FaceEnd = tierB1T1.getPickFaceEndPosX();
		Assert.assertTrue(b1T1FaceEnd == 1.15);
		double b2T2FaceEnd = tierB2T2.getPickFaceEndPosX();
		// 1.15 + 1.41 = 2.56. But real addition is too precise.
		Assert.assertTrue(b2T2FaceEnd > 2.55);

		List<ISubLocation> theB1T1Slots = tierB1T1.getChildren();
		Assert.assertTrue(theB1T1Slots.size() == 5);
		List<ISubLocation> theB1T2Slots = tierB1T2.getChildren();
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

	}

	@Test
	public final void testBadFile1() {

		if (false)
			return;

		// Ideally, we want non-throwing or caught exceptions that give good user feedback about what is wrong.
		// This has tier before bay, and some other blank fields
		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A14,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" // tier before bay invalidates the rest of this aisle
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,A8,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" //
				+ "xTier,T2,,5,32,0,,\r\n" // invalid binType invalidates the rest of this aisle
				+ "Bay,B3,115,,,,,\r\n" //
				+ "Tier,,,5,32,0,,\r\n" //
				+ "Aisle,A7,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T3,,5,32,0,,\r\n" // should be T2 here. Invalidates rest of this aisle
				+ "Tier,T2,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,AB7,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" // Invalid AisleName
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Aisle,A9,,,,,zigzagRight,12.85,43.45,Y,120,\r\n" // ok
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Check what we got from this bad file
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A14");
		Assert.assertNotNull(aisle); // the aisle started ok

		Bay bayA14B2 = Bay.DAO.findByDomainId(aisle, "B2");
		Assert.assertNull(bayA14B2); // bay should have failed for the tier coming first.

		Bay bayA14B3 = Bay.DAO.findByDomainId(aisle, "B3");
		Assert.assertNull(bayA14B2); // bay should have failed for nothing read until next aisle.

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

	}

	@Test
	public final void testDoubleFileRead() {
		if (false)
			return;

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierRight,12.85,43.45,Y,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		// And change leds to tierLeft
		// And change bay length
		String csvString2 = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A15,,,,,tierLeft,12.85,43.45,Y,120,\r\n" //
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
		AislesFileCsvImporter importer2 = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
				+ "Aisle,A15,,,,,tierLeft,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,110,,,,,\r\n" //
				+ "Tier,T1,,4,50,0,,\r\n"; //

		byte[] csvArray3 = csvString3.getBytes();

		ByteArrayInputStream stream3 = new ByteArrayInputStream(csvArray3);
		InputStreamReader reader3 = new InputStreamReader(stream3);

		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer3 = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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

	}

	@Test
	public final void testAfterFileModifications() {
		if (false)
			return;

		// The file read does a lot. But then we rely on the user via the UI to do additional things to complete the configuration. This is
		// a (nearly) end to end test of that. The actual UI will call a websocket command that calls a method on a domain object.
		// This test calls the same methods.

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A16,,,,,tierRight,12.85,43.45,Y,120,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_ID);
		Assert.assertNotNull(network);

		// There are led controllers, but we will make a new one. If it exists already, no harm.
		String cntlrId = "0x000026";
		LedController ledController = network.findOrCreateLedController(cntlrId, new NetGuid(cntlrId));
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
	}

	@Test
	public final void testNoLed() {
		if (false)
			return;

		// do a Y orientation on this as well
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A21,,,,,tierRight,12.85,23.45,Y,240,\r\n" //
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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
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

	}

	@Test
	public final void testPathCreation() {
		Organization organization = new Organization();
		organization.setDomainId("O-AISLE4X");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE4X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE4X");

		Path aPath = createPathForTest("F4X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F4X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);
		PathSegment segment1 = addPathSegmentForTest("F4X.1.1", aPath, 1, 12.85, 48.45, 12.85, 58.45);
		SortedSet<PathSegment> segments = aPath.getSegments();
		int countSegments = segments.size();
		Assert.assertTrue(countSegments == 2);

		// Path aPath2 = Path.DAO.findByDomainId(facility, "F4X.1");  does not work
		Path aPath2 = facility.getPath("F4X.1");
		Assert.assertNotNull(aPath2);

		List<Path> paths = facility.getPaths();
		int countPaths = paths.size();
		Assert.assertTrue(countPaths == 1);
	}

	@Test
	public final void testPath() {

		// We seemed to have a bug in the parse where when processing A21 beans, we have m values set for A22. That is, A21 might come out as zigzagRight
		// So this tests Bay to bay attributes changing within an aisle, and tier attributes changing within a bay.

		// This test is also good for testing path relationship if we run one path between the aisles.
		// Notice  that A31 has B1/S1 by the anchor
		// A32 has B1/S1 away from anchor point of aisle.

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm, b1S1NearAnchor\r\n" //
				+ "Aisle,A31,,,,,zigzagLeft,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,141,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Aisle,A32,,,,,zigzagRight,12.85,53.45,X,110,N\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-AISLE3X");
		mOrganizationDao.store(organization);

		organization.createFacility("F-AISLE3X", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-AISLE3X");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get A31
		Aisle aisle31 = Aisle.DAO.findByDomainId(facility, "A31");
		Assert.assertNotNull(aisle31);

		Bay bayA31B1 = Bay.DAO.findByDomainId(aisle31, "B1");
		Bay bayA31B2 = Bay.DAO.findByDomainId(aisle31, "B2");

		// For 2 bays, 2 tier, zigzagleft, tierB1T2 should start at led 1. tierB2T2 should start at 65
		Tier tierA31B1T1 = Tier.DAO.findByDomainId(bayA31B1, "T1");
		Tier tierA31B2T1 = Tier.DAO.findByDomainId(bayA31B2, "T1");

		// Get A32
		Aisle aisle32 = Aisle.DAO.findByDomainId(facility, "A32");
		Assert.assertNotNull(aisle32);
		Bay bayA32B1 = Bay.DAO.findByDomainId(aisle32, "B1");
		Bay bayA32B2 = Bay.DAO.findByDomainId(aisle32, "B2");
		Tier tierA32B1T1 = Tier.DAO.findByDomainId(bayA32B1, "T1");
		Tier tierA32B2T1 = Tier.DAO.findByDomainId(bayA32B2, "T1");	
		
		
		// Now Pathing. Simulate UI doing  path between the aisles, right to left.
		// For A31, B2 will be at the start of the path. And pos along path should be about the same for pairs of slots from A31 and A32
		// For A32, B1 will be at the start of the path

		Path aPath = createPathForTest("F3X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F3X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);
		
		// Mostly check the parent relationship these 4 lines
		TravelDirectionEnum direction1 = aPath.getTravelDirEnum();
		Assert.assertEquals(direction1, TravelDirectionEnum.FORWARD);
		TravelDirectionEnum direction2 = segment0.getParent().getTravelDirEnum();
		Assert.assertEquals(direction2, TravelDirectionEnum.FORWARD);


		Path bPath = facility.getPath("F3X.1");
		Assert.assertEquals(aPath, bPath);

		List<Path> paths = facility.getPaths();
		int countPaths = paths.size();
		Assert.assertEquals(1, countPaths);
		Path aPath2 = segment0.getParent();
		Assert.assertEquals(aPath, aPath2);
		int countPaths2 = paths.size();
		Assert.assertEquals(1, countPaths2);

		// Then we need to associate the aisles to the path segment. Use the same function as the UI does
		String persistStr = segment0.getPersistentId().toString();
		aisle31.associatePathSegment(persistStr);
		// Interesting. This calls facility.recomputeLocationPathDistances(the path segment's parent path); But does not find any locations on the path segment

		// this segment should have one location now. However, the old reference is stale and may know its aisles (used to be). Re-get
		PathSegment segment00 = PathSegment.DAO.findByDomainId(aPath, "F3X.1.0");
		int countLocations1 = segment00.getLocations().size();
		Assert.assertEquals(1, countLocations1);

		// Let's check locations on the path segment, derived different ways
		// original aPath return while created:
		PathSegment aPathSegment = aPath.getPathSegment(0);
		int countLocationsA = aPathSegment.getLocations().size(); 
		Assert.assertEquals(aPathSegment, segment00);
		Assert.assertEquals(1, countLocationsA); // if this fails, may be irrelevant; just a stale reference.

		// bPath from the facility before associating aisle to path segment
		PathSegment bPathSegment = bPath.getPathSegment(0);
		int countLocationsB = bPathSegment.getLocations().size();
		Assert.assertEquals(bPathSegment, segment00);
		Assert.assertEquals(1, countLocationsB); // if this fails, may be irrelevant; just a stale reference.


		// cPath from the facility now (after associating aisle to path segment)
		Path cPath = facility.getPath("F3X.1");
		PathSegment cPathSegment = cPath.getPathSegment(0);
		int countLocationsC = cPathSegment.getLocations().size(); 
		Assert.assertEquals(cPathSegment, segment00);
		Assert.assertEquals(1, countLocationsC); // if this fails, may be irrelevant; just a stale reference.

		// If you step into associatePathSegment, you will see that it finds the segment by UUID, and its location count was 1 and goes to 2.
		aisle32.associatePathSegment(persistStr);
		// Check in the same manner
		UUID persistentId = UUID.fromString(persistStr);
		PathSegment dPathSegment = PathSegment.DAO.findByPersistentId(persistentId);
		int countLocationsD = dPathSegment.getLocations().size(); 
		Assert.assertEquals(dPathSegment, segment00);
		Assert.assertEquals(2, countLocationsD); 

		// this segment should have two locations now
		//However, the old reference is stale and would only have one aisle. Need to re-get.
		PathSegment segment000 = PathSegment.DAO.findByDomainId(aPath, "F3X.1.0");
		List<LocationABC> locations2 = segment000.getLocations();
		int countLocations2 = locations2.size();
		Assert.assertEquals(2, countLocations2);  

		// Justs checking if the getParent() returns fully hydrated path. Used to NPE from this.
		Path dPath = dPathSegment.getParent();
		Assert.assertNotNull(dPath);
		TravelDirectionEnum theDirection = dPath.getTravelDirEnum();
		Assert.assertEquals(theDirection, TravelDirectionEnum.FORWARD);

		// This should not be necessary as associatePathSegment() called it
		facility.recomputeLocationPathDistances(aPath);
		
		Slot firstA31SlotOnPath = Slot.DAO.findByDomainId(tierA31B2T1, "S5");
		Slot firstA32SlotOnPath = Slot.DAO.findByDomainId(tierA32B1T1, "S1");
		
		Slot lastA31SlotOnPath = Slot.DAO.findByDomainId(tierA31B1T1, "S1");
		Slot lastA32SlotOnPath = Slot.DAO.findByDomainId(tierA32B2T1, "S5");

		Double valueFirst31 = firstA31SlotOnPath.getPosAlongPath();
		Double valueFirst32 = firstA32SlotOnPath.getPosAlongPath();
		Double valueLast31 = firstA31SlotOnPath.getPosAlongPath();
		Double valueLast32 = firstA32SlotOnPath.getPosAlongPath();
		
		// old bug got the same position for first two bays. Check that.
		Double a31b1Value = tierA31B1T1.getPosAlongPath();
		Double a31b2Value = tierA31B2T1.getPosAlongPath();	
		// Values are null. Lets get new references tier references after the path was applied, as the posAlongPath is null on old reference
		tierA31B1T1 = Tier.DAO.findByDomainId(bayA31B1, "T1");
		tierA31B2T1 = Tier.DAO.findByDomainId(bayA31B2, "T1");
		tierA32B1T1 = Tier.DAO.findByDomainId(bayA32B1, "T1");
		tierA32B2T1 = Tier.DAO.findByDomainId(bayA32B2, "T1");
		
		// Here is a major point of this test. b1S1NearAnchor is Y for A31, and N for A32
		// Therefore, A31B1 has anchor 0, but A32B2 has anchor zero as B1 is further away from the anchor point.
		Double valueTierA31B1T1  = tierA31B1T1.getAnchorPosX();
		Double valueTierA31B2T1  = tierA31B2T1.getAnchorPosX();
		Double valueTierA32B1T1  = tierA32B1T1.getAnchorPosX();
		Double valueTierA32B2T1  = tierA32B2T1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueTierA31B1T1);
		Assert.assertEquals((Double) 0.0, valueTierA32B2T1);
		
		// Slots should also be appropriate reversed in A32.
		Slot slotA31B1T1S1 = Slot.DAO.findByDomainId(tierA31B1T1, "S1");
		Slot slotA31B1T1S5 = Slot.DAO.findByDomainId(tierA31B1T1, "S5");
		Slot slotA31B2T1S1 = Slot.DAO.findByDomainId(tierA31B2T1, "S1");
		Slot slotA31B2T1S5 = Slot.DAO.findByDomainId(tierA31B2T1, "S5");
		Slot slotA32B1T1S1 = Slot.DAO.findByDomainId(tierA32B1T1, "S1");
		Slot slotA32B1T1S5 = Slot.DAO.findByDomainId(tierA32B1T1, "S5");
		Slot slotA32B2T1S1 = Slot.DAO.findByDomainId(tierA32B2T1, "S1");
		Slot slotA32B2T1S5 = Slot.DAO.findByDomainId(tierA32B2T1, "S5");

		Double valueSlotA31B1T1S1  = slotA31B1T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA31B1T1S1); // first slot in A31
		Double valueSlotA31B1T1S5  = slotA31B1T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA31B1T1S5); 
		Double valueSlotA31B2T1S1  = slotA31B2T1S1.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA31B2T1S1);
		Double valueSlotA31B2T1S5  = slotA31B2T1S5.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA31B2T1S5); 
// A32 will be a bit backwards. S5 should have the zero value
		Double valueSlotA32B1T1S1  = slotA32B1T1S1.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA32B1T1S1);
		Double valueSlotA32B1T1S5  = slotA32B1T1S5.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA32B1T1S5); 
		Double valueSlotA32B2T1S1  = slotA32B2T1S1.getAnchorPosX();
		Assert.assertNotEquals((Double) 0.0, valueSlotA32B2T1S1);
		Double valueSlotA32B2T1S5  = slotA32B2T1S5.getAnchorPosX();
		Assert.assertEquals((Double) 0.0, valueSlotA32B2T1S5); // last slot in A32, close to anchor

		// Path values should derive from location anchor and pickface end, relative to the path.
		// PATH VALUES NOT CORRECT YET
		// As the path goes right to left between A31 and A32,
		// Lowest values for meters along path are at A31B2T1S5 and A32B1T1S1
		Double slotA31B2T1S5Value = slotA31B2T1S5.getPosAlongPath();
		Double slotA31B2T1S1Value = slotA31B2T1S1.getPosAlongPath();
		// Assert.assertTrue(slotA31B2T1S1Value > slotA31B2T1S5Value);
	
		Double slotA31B1T1S5Value = slotA31B1T1S5.getPosAlongPath();
		// Assert.assertTrue(slotA31B1T1S5Value > slotA31B2T1S1Value); // first bay last slot further along path than second bay first slot
		
		Double slotA31B1T1S1Value = slotA31B1T1S1.getPosAlongPath();
		// Assert.assertTrue(slotA31B1T1S1Value > slotA31B1T1S5Value);

		// lowest at A32B1T1S1
		Double slotA32B1T1S5Value = slotA32B1T1S5.getPosAlongPath();
		Double slotA32B1T1S1Value = slotA32B1T1S1.getPosAlongPath();
		// Assert.assertTrue(slotA32B1T1S5Value > slotA32B1T1S1Value);
		
		Double slotA32B2T1S5Value = slotA32B2T1S5.getPosAlongPath();
		// Assert.assertTrue(slotA32B1T1S5Value < slotA32B2T1S1Value); // first bay last slot not as far along path than second bay first slot
		
		Double slotA32B2T1S1Value = slotA32B2T1S1.getPosAlongPath();
		// Assert.assertTrue(slotA31B1T1S1Value > slotA31B1T1S5Value);


	}
}
