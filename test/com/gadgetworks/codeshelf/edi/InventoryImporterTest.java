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
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest extends EdiTestABC {

	@Test
	public final void testInventoryImporterFromCsvStream() {

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,4550,Gadget,450,case,A1.B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,3007,Dealybob,300,case,A1.B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,2150,Thingamajig,220,case,A1.B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,2170,Doodad,125,each,A1.B5,555,2012-09-26 11:31:03";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV1.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-INV1.1");

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		Item item = facility.getStoredItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

	}

	// --------------------------------------------------------------------------
	/**
	 * Created when we discovered that multiple inventory items in the same facility failed to import due to key collisions.
	 */
	@Test
	public final void testMultipleItemInstancesInventoryImporterFromCsvStream() {

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,100,each,A1.B2,111,2012-09-26 11:31:01\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV2.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV2.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-INV2.1");
		mFacilityDao.store(facility);

		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA1);

		Bay bay1 = new Bay(aisleA1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bay1);

		Bay bay2 = new Bay(aisleA1, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bay2);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		bay1 = (Bay) facility.findSubLocationById("A1.B1");
		bay2 = (Bay) facility.findSubLocationById("A1.B2");

		Item item = bay1.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		item = bay2.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		// Run the import again - it should not trip up on the same items at the same place(s) - it should instead update them.

		csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,200,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,200,each,A1.B2,111,2012-09-26 11:31:01\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		bay1 = (Bay) facility.findSubLocationById("A1.B1");
		bay2 = (Bay) facility.findSubLocationById("A1.B2");

		item = bay1.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(200.0, item.getQuantity().doubleValue(), 0.0);

		itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

	}
	
	private Facility setUpSimpleNoSlotFacility(String inOrganizationName) {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle. 
		//   With location alias for first baytier only, not second. 
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);
		
		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);
		
		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T1.S1, D301\r\n" //
				+ "A1.B1.T1.S2, D302\r\n" //
				+ "A1.B1.T1.S3, D303\r\n" //
				+ "A1.B1.T1.S4, D304\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);
		
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);


		
		return facility;
	
	}

	@Test
	public final void checkBayanchors() {
		// This is critical for path values for non-slotted inventory. Otherwise, this belongs in aisle file test, and not in inventory test.
		Facility facility = setUpSimpleNoSlotFacility("XX01");	
		SubLocationABC locationB1 =  (SubLocationABC) facility.findSubLocationById("A1.B1");
		Assert.assertNotNull(locationB1);
		SubLocationABC locationB2 =  (SubLocationABC)facility.findSubLocationById("A1.B2");
		Assert.assertNotNull(locationB2);
		SubLocationABC locationB3 =  (SubLocationABC) facility.findSubLocationById("A1.B3");
		Assert.assertNotNull(locationB3);
		
		// By our model, each bay's anchor is relative to the owner aisle, so will differ.
		// Each bay's pickFaceEnd is relative to its own anchor. As the bays are uniformly wide, the pickFace end values will be the same.
		String bay1Anchor = locationB1.getAnchorPosXui(); // bay 1 anchor will be zero.
		String bay2Anchor = locationB2.getAnchorPosXui();
		String bay3Anchor = locationB3.getAnchorPosXui();
		Assert.assertNotEquals(bay2Anchor, bay3Anchor);
		String bay1PickEnd = locationB1.getPickFaceEndPosXui();
		String bay2PickEnd = locationB2.getPickFaceEndPosXui();
		String bay3PickEnd = locationB3.getPickFaceEndPosXui();
		Assert.assertEquals(bay1PickEnd, bay2PickEnd);
		
		SubLocationABC locationB3T1S1 =  (SubLocationABC) facility.findSubLocationById("A1.B3.T1.S1");
		Assert.assertNotNull(locationB3T1S1);
		SubLocationABC locationB3T1S2 =  (SubLocationABC) facility.findSubLocationById("A1.B3.T1.S2");
		Assert.assertNotNull(locationB3T1S2);
		SubLocationABC locationB3T1S3 =  (SubLocationABC) facility.findSubLocationById("A1.B3.T1.S3");
		Assert.assertNotNull(locationB3T1S3);
		SubLocationABC locationB3T1S4 =  (SubLocationABC) facility.findSubLocationById("A1.B3.T1.S4");
		Assert.assertNotNull(locationB3T1S4);

		// By our model, each slot's anchor is relative to the owner tier, so will differ.
		// Each slot's pickFaceEnd is relative to its own anchor. As the slots are uniformly wide, the pickFace end values will be the same.
		String slot1Anchor = locationB3T1S1.getAnchorPosXui(); // slot 1 anchor will be zero.
		String slot2Anchor = locationB3T1S2.getAnchorPosXui();
		String slot4Anchor = locationB3T1S4.getAnchorPosXui();
		Assert.assertNotEquals(slot2Anchor, slot4Anchor);
		String slot1PickEnd = locationB3T1S1.getPickFaceEndPosXui();
		String slot4PickEnd = locationB3T1S4.getPickFaceEndPosXui();
		Assert.assertEquals(slot1PickEnd, slot4PickEnd);
		
		String slot1Pos = locationB3T1S1.getPosAlongPathui();
		String slot2Pos = locationB3T1S2.getPosAlongPathui();
		String slot4Pos = locationB3T1S4.getPosAlongPathui();

		String bay1Pos = locationB1.getPosAlongPathui();
		String bay2Pos = locationB2.getPosAlongPathui();
		String bay3Pos = locationB3.getPosAlongPathui();
		// The last slot in bay3 should have same path value as the bay
		Assert.assertEquals(slot4PickEnd, bay3Pos);


	}
		

	@Test
	public final void testNonSlottedInventory() {
			
		Facility facility = setUpSimpleNoSlotFacility("XX02");

		// leave out the optional lot, and organize the file as we are telling Accu. Note: there is no such thing as itemDetailId
		// D102 (item 1522) will not resolve
		// Item 1546 had cm, but no location.
		// BOL-CS-8 in D100 is a bay. with cm value. Meaning?
		// BOL-CS-8 in A1.B1.T1 has no cm value.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D101,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,6\r\n" //
				+ "BOL-CS-8,A1.B1.T1,8 oz Paper Bowl Lids - Comp Case of 1000,3,CS,6/25/14 12:00,\r\n" //
				+ "BOL-CS-8,D100,8 oz Paper Bowl Lids - Comp Case of 1000,22,CS,6/25/14 12:00,56\r\n" //
				+ "1493,D101,PARK RANGER Doll,40,EA,6/25/14 12:00,66\r\n" //
				+ "1522,D102,SJJ BPP,10,EA,6/25/14 12:00,3\r\n" //
				+ "1546,,BAD KITTY BBP,10,EA,6/25/14 12:00,89"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		// How do you find the inventory items made?
		// other unit tests do it like this:
		Item item1123 = facility.getStoredItem("1123");
		Assert.assertNull(item1123);
		// This would only work if the location did not resolve, so item went to the facility.
		// Let's find the D101 location
		LocationABC locationD101 = (LocationABC) facility.findSubLocationById("D101");
		Assert.assertNotNull(locationD101);
		item1123 = locationD101.getStoredItem("1123");
		Assert.assertNotNull(item1123);

		ItemMaster itemMaster = item1123.getParent();
		Assert.assertNotNull(itemMaster);	
		
		LocationABC locationB1T1 = (LocationABC) facility.findSubLocationById("A1.B1.T1");
		Assert.assertNotNull(locationB1T1);
		Item itemBOL1 = locationB1T1.getStoredItem("BOL-CS-8");
		Assert.assertNotNull(itemBOL1);

		// D102 will not resolve. Let's see if it went to the facility
		LocationABC locationD102 = (LocationABC) facility.findSubLocationById("D102");
		Assert.assertNull(locationD102);
		Item item1522 = facility.getStoredItem("1522");
		Assert.assertNotNull(item1522);

		// Now check the good stuff. We have a path, items with position, with cm offset. So, we should get posAlongPath values, 
		// as well as getting back any valid cm values. (They converted to Double meters from anchor, and then convert back.)
		
		// zero cm value. Same posAlongPath as the location
		Integer cmValue = itemBOL1.getCmFromLeft(); // this did not have a value.
		Assert.assertEquals(cmValue, (Integer) 0);
		// for zero cmfromLeft, either 0 meters,or the full pickFaceEnd value
		Double correspondingMeters = itemBOL1.getMetersFromAnchor();
		
		String itemPosValue = itemBOL1.getPosAlongPathui();
		String locPosValue = ((SubLocationABC) locationB1T1).getPosAlongPathui();
		// Assert.assertEquals(itemPosValue, locPosValue);
		// Bug here

		// Has a cm value pf 6. Different posAlongPath than the location
		Integer cmValue2 = item1123.getCmFromLeft(); // this did not have a value.
		Assert.assertEquals(cmValue2, (Integer) 6);
		String itemPosValue2 = item1123.getPosAlongPathui();
		String locPosValue2 = ((SubLocationABC) locationD101).getPosAlongPathui();
		Assert.assertNotEquals(itemPosValue2, locPosValue2);

	}

}
