/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.websocket.DecodeException;
import javax.websocket.EncodeException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WiFactory;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest extends EdiTestABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(InventoryImporterTest.class);

	UUID facilityForVirtualSlottingId;
	
	static {
		Configuration.loadConfig("test");
	}

	@Override
	public void doBefore() {
		this.getPersistenceService().beginTenantTransaction();

		VirtualSlottedFacilityGenerator facilityGenerator =
					new VirtualSlottedFacilityGenerator(createAisleFileImporter(),
														createLocationAliasImporter(),
														createOrderImporter());
		
		Facility facilityForVirtualSlotting = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		
		this.facilityForVirtualSlottingId = facilityForVirtualSlotting.getPersistentId();
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void testInventoryImporterFromCsvStream() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,A1.B1,111,2012-09-26 11:31:01\r\n";
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);

		Aisle aisle = Aisle.as(facility.findSubLocationById("A1"));
		Assert.assertNotNull("Aisle is undefined", aisle);

		// retrieve via aisle
		Bay bay = Bay.as(aisle.findSubLocationById("B1"));
		Assert.assertNotNull("Bay is undefined", bay);
		
		// retrieve via facility		
		bay = Bay.as(facility.findSubLocationById("A1.B1"));
		Assert.assertNotNull("Bay is undefined", bay);
		
		Item item = bay.getStoredItemFromMasterIdAndUom("3001", "each");
		Assert.assertNotNull(item);
		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testEmptyUom() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,A,,A1.B1,111,2012-09-26 11:31:01\r\n";
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);

		Item item = facility.getStoredItemFromMasterIdAndUom("3001", "");
		Assert.assertNull(item);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testAlphaQuantity() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,A,each,A1.B1,111,2012-09-26 11:31:01\r\n";
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		

		Item item = facility.findSubLocationById("A1.B1").getStoredItemFromMasterIdAndUom("3001", "each");
		Assert.assertNotNull(item);
		Assert.assertEquals(0.0d, item.getQuantity(), 0.0d);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testNegativeQuantity() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,-2,each,A1.B1,111,2012-09-26 11:31:01\r\n";
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		Item item = facility.findSubLocationById("A1.B1").getStoredItemFromMasterIdAndUom("3001", "each");
		Assert.assertNotNull(item);
		Assert.assertEquals(0.0d, item.getQuantity(), 0.0d);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		this.getPersistenceService().commitTenantTransaction();
	}

	// --------------------------------------------------------------------------
	/**
	 * Created when we discovered that multiple inventory items in the same facility failed to import due to key collisions.
	 */
	@Test
	public final void testMultipleNonEachItemInstancesInventoryImporterFromCsvStream() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);
		
		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,case,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,100,case,A1.B2,111,2012-09-26 11:31:01\r\n";

		setupInventoryData(facility, csvString);

		Bay bay1 = (Bay) facility.findSubLocationById("A1.B1");
		Bay bay2 = (Bay) facility.findSubLocationById("A1.B2");

		Item item = bay1.getStoredItemFromMasterIdAndUom("3001", "case");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		item = bay2.getStoredItemFromMasterIdAndUom("3001", "case");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		// Run the import again - it should not trip up on the same items at the same place(s) - it should instead update them.

		csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,200,case,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,200,case,A1.B2,111,2012-09-26 11:31:01\r\n";

		setupInventoryData(facility, csvString);

		bay1 = (Bay) facility.findSubLocationById("A1.B1");
		bay2 = (Bay) facility.findSubLocationById("A1.B2");

		// test our flexibility of "CS" vs. "case"
		item = bay1.getStoredItemFromMasterIdAndUom("3001", "CS");
		Assert.assertNotNull(item);
		Assert.assertEquals(200.0, item.getQuantity().doubleValue(), 0.0);

		itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		this.getPersistenceService().commitTenantTransaction();
	}

	
	@SuppressWarnings({"unused" })
	@Test
	public final void testBayAnchors() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		// This is critical for path values for non-slotted inventory. Otherwise, this belongs in aisle file test, and not in inventory test.
		Facility facility = facilityForVirtualSlotting;
		Location locationB1 = facility.findSubLocationById("A1.B1");
		Assert.assertNotNull(locationB1);
		Location locationB2 = facility.findSubLocationById("A1.B2");
		Assert.assertNotNull(locationB2);
		Location locationB3 = facility.findSubLocationById("A1.B3");
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

		Location locationB3T1S1 = facility.findSubLocationById("A1.B3.T1.S1");
		Assert.assertNotNull(locationB3T1S1);
		Location locationB3T1S2 = facility.findSubLocationById("A1.B3.T1.S2");
		Assert.assertNotNull(locationB3T1S2);
		Location locationB3T1S3 = facility.findSubLocationById("A1.B3.T1.S3");
		Assert.assertNotNull(locationB3T1S3);
		Location locationB3T1S4 = facility.findSubLocationById("A1.B3.T1.S4");
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
		Assert.assertEquals(slot4Pos, bay3Pos);

		this.getPersistenceService().commitTenantTransaction();

	}

	@SuppressWarnings({ "unused" })
	@Test
	public final void testNonSlottedInventory() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		Facility facility = facilityForVirtualSlotting;

		// leave out the optional lot, and organize the file as we are telling Accu. Note: there is no such thing as itemDetailId
		// D102 (item 1522) will not resolve
		// Item 1546 had cm, but no location.
		// BOL-CS-8 in D100 is a bay. with cm value. Meaning?
		// BOL-CS-8 in A1.B1.T1 has no cm value.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D101,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,16\r\n" //
				+ "BOL-CS-8,A1.B1.T1,8 oz Paper Bowl Lids - Comp Case of 1000,3,CS,6/25/14 12:00,\r\n" //
				+ "BOL-CS-8,D100,8 oz Paper Bowl Lids - Comp Case of 1000,22,CS,6/25/14 12:00,56\r\n" //
				+ "1493,D101,PARK RANGER Doll,40,EA,6/25/14 12:00,66\r\n" //
				+ "1522,D109,SJJ BPP,10,EA,6/25/14 12:00,3\r\n" //
				+ "1546,,BAD KITTY BBP,10,EA,6/25/14 12:00,89\r\n"//
				+ "1123,D403,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n"; //

		setupInventoryData(facility, csvString);


		// How do you find the inventory items made?
		// other unit tests do it like this:
		Item item1123 = facility.getStoredItemFromMasterIdAndUom("1123", "CS");
		Assert.assertNull(item1123);
		// This would only work if the location did not resolve, so item went to the facility.
		// Let's find the D101 location
		Location locationD101 = facility.findSubLocationById("D101");
		Assert.assertNotNull(locationD101);
		item1123 = locationD101.getStoredItemFromMasterIdAndUom("1123", "CS");
		Assert.assertNotNull(item1123);

		ItemMaster itemMaster = item1123.getParent();
		Assert.assertNotNull(itemMaster);

		Location locationB1T1 = facility.findSubLocationById("A1.B1.T1");
		Assert.assertNotNull(locationB1T1);
		// test our flexibility of "CS" vs. "case"
		Item itemBOL1 = locationB1T1.getStoredItemFromMasterIdAndUom("BOL-CS-8", "case");
		Assert.assertNotNull(itemBOL1);

		// D102 will not resolve. Let's see that item 1522 went to the facility
		Location locationD109 = facility.findSubLocationById("D109");
		Assert.assertNull(locationD109);
		Item item1522 = facility.getStoredItemFromMasterIdAndUom("1522", "EA");
		Assert.assertNotNull(item1522);

		// Now check the good stuff. We have a path, items with position, with cm offset. So, we should get posAlongPath values,
		// as well as getting back any valid cm values. (They converted to Double meters from anchor, and then convert back.)

		// zero cm value. Same posAlongPath as the location
		Integer cmValue = itemBOL1.getCmFromLeft(); // this did not have a value.
		Assert.assertEquals(cmValue, (Integer) 0);
		// for zero cmfromLeft, either 0 meters,or the full pickFaceEnd value
		Double correspondingMeters = itemBOL1.getMetersFromAnchor();

		String itemPosValue = itemBOL1.getPosAlongPathui();
		String locPosValue = locationB1T1.getPosAlongPathui();
		// Assert.assertEquals(itemPosValue, locPosValue);
		// Bug here

		// Has a cm value pf 6. Different posAlongPath than the location
		Integer cmValue2 = item1123.getCmFromLeft(); // this did not have a value.
		Assert.assertEquals(cmValue2, (Integer) 16);
		String itemPosValue2 = item1123.getPosAlongPathui();
		String locPosValue2 = locationD101.getPosAlongPathui();
		Assert.assertNotEquals(itemPosValue2, locPosValue2);

		// We can now see how the inventory would light.
		// BOL 1 is case item in A1.B1.T1, with 80 LEDs. No cmFromLeftValue, so it will take the central 4 LEDs.
		Location theLoc = itemBOL1.getStoredLocation();
		// verify the conditions.
		int firstLocLed = theLoc.getFirstLedNumAlongPath();
		int lastLocLed = theLoc.getLastLedNumAlongPath();
		Assert.assertEquals(1, firstLocLed);
		Assert.assertEquals(80, lastLocLed);
		// now the tricky stuff
		LedRange theLedRange = itemBOL1.getFirstLastLedsForItem();
		Assert.assertEquals(39, theLedRange.getFirstLedToLight());
		Assert.assertEquals(42, theLedRange.getLastLedToLight(), 1);

		// item1123 for CS in D101 which is the A1.B1.T1. 16 cm from left of 230 cm aisle
		LedRange theLedRange2 = item1123.getFirstLastLedsForItem();
		Assert.assertEquals(73, theLedRange2.getFirstLedToLight());
		Assert.assertEquals(76, theLedRange2.getLastLedToLight());

		// item1123 for EA in D403 which is the A2.B2.T1. 135 cm from left of 230 cm aisle
		Location locationA2B2T1 = facility.findSubLocationById("A2.B2.T1");
		Assert.assertNotNull(locationA2B2T1);
		Location locationD403 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD403);
		Assert.assertEquals(locationD403, locationA2B2T1);
		// Item item1123EA = locationA2B2T1.getStoredItemFromMasterIdAndUom("1123", "EA");
		Item item1123EA = locationD403.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123EA);
		Assert.assertFalse(locationD403.isLowerLedNearAnchor());
		// This is A2 bay 2; LEDs come from that side in A2. ((230-135)/230)*80 == 33 is the central LED.
		LedRange theLedRange3 = item1123EA.getFirstLastLedsForItem();
		Assert.assertEquals(32, theLedRange3.getFirstLedToLight());
		Assert.assertEquals(35, theLedRange3.getLastLedToLight());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testNonSlottedInventory2() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		// Very small test checking leds for this one inventory item
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n"; //

		setupInventoryData(facility, csvString);


		// item1123 for EA in D402 which is the A2.B1.T1. 135 cm from left of 230 cm aisle
		Location locationA2B2T1 = facility.findSubLocationById("A2.B2.T1");
		Assert.assertNotNull(locationA2B2T1);
		Location locationD403 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD403);
		Assert.assertEquals(locationD403, locationA2B2T1);
		Location locationD402 = facility.findSubLocationById("D402");

		// Let's check the LEDs. A2 is tierNotB1S1 side, so B2 is 1 to 80. A1 is tierB1S1 .
		// Just check our led range on the tiers
		Location locationA2B1T1 = facility.findSubLocationById("A2.B1.T1");
		Assert.assertNotNull(locationA2B1T1);
		Short firstLED1 = locationA2B1T1.getFirstLedNumAlongPath();
		Short lastLED1 = locationA2B1T1.getLastLedNumAlongPath();
		Assert.assertTrue(firstLED1 == 81);
		Assert.assertTrue(lastLED1 == 160);

		Short firstLED2 = locationA2B2T1.getFirstLedNumAlongPath();
		Short lastLED2 = locationA2B2T1.getLastLedNumAlongPath();
		Assert.assertTrue(firstLED2 == 1);
		Assert.assertTrue(lastLED2 == 80);

		// Item item1123EA = locationA2B2T1.getStoredItemFromMasterIdAndUom("1123", "EA");
		Item item1123EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123EA);
		LedRange theLedRange = item1123EA.getFirstLastLedsForItem();
		// central led at about (((230-135)/230) * 80) + 80
		Assert.assertEquals(112, theLedRange.getFirstLedToLight());
		Assert.assertEquals(115, theLedRange.getLastLedToLight());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testNonSlottedInventory3() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		// Very small test checking multiple inventory items for same SKU
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D403,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n"; //

		setupInventoryData(facility, csvString);


		Location locationD403 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD403);
		Location locationD402 = facility.findSubLocationById("D402");
		Assert.assertNotNull(locationD403);

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);
		Item item1123Loc402CS = locationD402.getStoredItemFromMasterIdAndUom("1123", "CS"); // notice each and case are separate items in the same locations
		Assert.assertNotNull(item1123Loc402CS);
		Item item1123Loc403CS = locationD403.getStoredItemFromMasterIdAndUom("1123", "EA"); // a case was here in D403, not EA
		Assert.assertNull(item1123Loc403CS);
		item1123Loc403CS = locationD403.getStoredItemFromMasterIdAndUom("1123", "CS");
		Assert.assertNotNull(item1123Loc403CS);
		// Not tested here. Later, we will enforce only one each location per item in a facility (or perhaps work area) even as we allow multiple case locations.

		this.getPersistenceService().commitTenantTransaction();
	}

	private Facility setupInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return facility.getDao().findByPersistentId(facility.getPersistentId());
	}

	@SuppressWarnings({ "unused" })
	@Test
	public final void testNonSlottedPick() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facilityForVirtualSlotting = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");
		Location locationD502 = facility.findSubLocationById("D502");
		Location locationD503 = facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		// A brief side trip to check the list we use for lighting inventory in a tier
		List<Item>  invList = locationD502.getInventoryInWorkingOrder();
		Assert.assertTrue(invList.size() == 2);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.
		// And extra lines  with variant endings just for fun

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n"
				+ "\r"
				+ "\r\n" + "\n";

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(new StringReader(csvOrders), facility, ediProcessTime2);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Assert.assertEquals(3, order.getOrderDetails().size());

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			}
		}
		Assert.assertEquals(2, itemLocations.size());
		// this.getPersistenceService().commitTenantTransaction();
		
		// Let's find our CHE
		// this.getPersistenceService().beginTenantTransaction();
		Assert.assertTrue(this.getPersistenceService().hasActiveTransaction());
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// Turn off housekeeping work instructions so as to not confuse the counts
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		List<WorkInstruction> wiListBeginningOfPath = mWorkService.getWorkInstructions(theChe, "");
		Assert.assertEquals("The WIs: " + wiListBeginningOfPath, 0, wiListBeginningOfPath.size()); // 3, but one should be short. Only 1123 and 1522 find each inventory
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		// Set up a cart for order 12345, which will generate work instructions
		mWorkService.setUpCheContainerFromString(theChe, "12345");
		
		// Just checking variant case hard on ebeans. What if we immediately set up again? Answer optimistic lock exception and assorted bad behavior.
		// facility.setUpCheContainerFromString(theChe, "12345");

		List<WorkInstruction> wiListBeginningOfPathAfterSetup = mWorkService.getWorkInstructions(theChe, "");

		Assert.assertEquals("The WIs: " + wiListBeginningOfPathAfterSetup, 2, wiListBeginningOfPathAfterSetup.size()); // 3, but one should be short. Only 1123 and 1522 find each inventory
		//Auto-shorting functionality disabled 02/03/2015
		//assertAutoShort(theChe, wiListBeginningOfPathAfterSetup.get(0).getAssigned()); //get any for assigned time)

		List<WorkInstruction> wiListAfterScan = mWorkService.getWorkInstructions(theChe, "D403");

		mPropertyService.restoreHKDefaults(facility);

		Integer wiCountAfterScan = wiListAfterScan.size();
		// Now getting 2. Something is wrong!
		// Assert.assertEquals((Integer) 1, wiCountAfterScan); // only the one each item in 403 should be there. The item in 402 is earlier on the path.

		WorkInstruction wi1 = wiListAfterScan.get(0);
		Assert.assertNotNull(wi1);
		String groupSortStr1 = wi1.getGroupAndSortCode();
		Assert.assertEquals("0001", groupSortStr1);
		Double wi1Pos = wi1.getPosAlongPath();
		String wi1Item = wi1.getItemId();

		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertNotNull(wi2);
		String groupSortStr2 = wi2.getGroupAndSortCode();
		Assert.assertEquals("0002", groupSortStr2);
		Double wi2Pos = wi2.getPosAlongPath();
		String wi2Item = wi2.getItemId();

		Double pos403 = locationD403.getPosAlongPath();
		Double pos402 = locationD402.getPosAlongPath();

		// just checking the relationships of the work instruction
		OrderDetail wiDetail = wi1.getOrderDetail();
		Assert.assertNotNull(wiDetail);
		OrderHeader wiOrderHeader = wiDetail.getParent();
		Assert.assertNotNull(wiOrderHeader);
		Assert.assertEquals(facility, PersistenceService.<Facility>deproxify(wiOrderHeader.getParent()));

		// New from v4. Test our work instruction summarizer
		List<WiSetSummary> summaries = new WorkService().start().workSummary(theChe.getPersistentId(),
			facility.getPersistentId());

		// as this test, this facility only set up this one che, there should be only one wi set. But we have 3. How?
		Assert.assertEquals(1, summaries.size());

		// getAny should get the one. Call it somewhat as the UI would. Get a time, then query again with that time.
		WiSetSummary theSummary = summaries.get(0);
		// So, how many shorts, how many active? None complete yet.
		int actives = theSummary.getActiveCount();
		int shorts = theSummary.getShortCount();
		int completes = theSummary.getCompleteCount();
		Assert.assertEquals(0, completes);
		//Auto shorting disabled on 02/03/2015
		//Assert.assertEquals(1, shorts);
		Assert.assertEquals(2, actives);

		this.getPersistenceService().commitTenantTransaction();
	}

	private void assertAutoShort(Che theChe, Timestamp assignedTimestamp) {
		List<WorkInstruction> wiPlusAutoShort = WorkInstruction.DAO.findByFilterAndClass("workInstructionByCheAndAssignedTime",
			 ImmutableMap.<String, Object>of(
				 "cheId", theChe.getPersistentId(),
				 "assignedTimestamp", assignedTimestamp),
			 WorkInstruction.class);
		boolean foundOne = false;
		for (WorkInstruction workInstruction : wiPlusAutoShort) {
			if (workInstruction.getStatus().equals(WorkInstructionStatusEnum.SHORT)) {
				UUID orderDetailPersistentId = workInstruction.getOrderDetail().getPersistentId();
				OrderDetail persistedOrderDetail = OrderDetail.DAO.findByPersistentId(orderDetailPersistentId);
				Assert.assertEquals(OrderStatusEnum.SHORT, persistedOrderDetail.getStatus());
				foundOne = true;
			}
		}
		Assert.assertTrue("Should have had at least one short wi", foundOne);

	}

	// --------------------------------------------------------------------------
	/**
	 * Attempting to demonstrate when we might get several work instructions that would work simultaneously
	 */
	@SuppressWarnings("unused")
	@Test
	public final void testSameProductPick() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		setupInventoryData(facility, csvString);

		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");
		Location locationD502 = facility.findSubLocationById("D502");
		Location locationD503 = facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// SKU 1123 needed for 12000
		// SKU 1123 needed for 12010
		// Each product needed for 12345

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,TARGET,12000,12000,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,PENNYS,12010,12010,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\n";

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(new StringReader(csvString2), facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);
		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);

		// Housekeeping left on. Expect 4 normal WIs and one housekeep
		// Set up a cart for the three orders, which will generate work instructions
		mWorkService.setUpCheContainerFromString(theChe, "12000,12010,12345");
		//Che.DAO.store(theChe);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(this.facilityForVirtualSlottingId);
		theChe = Che.DAO.findByPersistentId(theChe.getPersistentId());
		List<WorkInstruction> wiListAfterScan = mWorkService.getWorkInstructions(theChe, ""); // get all in working order
		
		for (WorkInstruction wi : wiListAfterScan) {
			LOGGER.info("WI LIST CONTAINS: " + wi.toString());
		}
		int wiCountAfterScan = wiListAfterScan.size();
		Assert.assertEquals(wiCountAfterScan, 5);
		// The only interesting thing here is probably the group and sort code. (Lack of group code)
		WorkInstruction wi1 = wiListAfterScan.get(0);
		String groupSortStr1 = wi1.getGroupAndSortCode();
		Assert.assertEquals("0001", groupSortStr1);

		WorkInstruction wi4 = wiListAfterScan.get(3);
		Assert.assertNotNull(wi4);
		String groupSortStr4 = wi4.getGroupAndSortCode();
		Assert.assertEquals("0004", groupSortStr4);

		// We would really like to see in integration test if all three position controllers light at once for SKU 1123
		// Just some quick log output to see it
		for (WorkInstruction wi : wiListAfterScan)
			LOGGER.debug("WiSort: " + wi.getGroupAndSortCode() + " cntr: " + wi.getContainerId() + " loc: "
					+ wi.getPickInstruction() + " desc.: " + wi.getDescription());

		// Try setting up the cart again in different order. DOES NOT WORK! Hits this optimistic commit case, then fails
		// 		at com.avaje.ebeaninternal.server.core.DefaultServer.refresh(DefaultServer.java:545)
		// facility.setUpCheContainerFromString(theChe, "12345,12010,12000");
		// wiListAfterScan = facility.getWorkInstructions(theChe, ""); // get all in working order

		// A small add on. Test the LightLedsMessage as we are able.
		Item theItem = wi1.getWiItem();
		if (theItem == null) {
			LOGGER.error("fix testSameProductPick"); // use a wi with an item, a proper pick
			return;
		}
		
		// Just a check. The wi led stream is presumably serialized correctly. Let's look at it and verify.
		String reference1 = wi1.getLedCmdStream();
		LOGGER.info("  wi cmd stream: " + reference1);
		List<LedCmdGroup> wi1LedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(reference1);
		Assert.assertTrue(LedCmdGroupSerializer.verifyLedCmdGroupList(wi1LedCmdGroups));

		
		// Now we have an item. Mimic how the code works for lightOneItem
		Location location = theItem.getStoredLocation();
		List<LedCmdGroup> ledCmdGroupList = WiFactory.getLedCmdGroupListForItemOrLocation(theItem, ColorEnum.RED, location);
		if (ledCmdGroupList.size() == 0) {
			LOGGER.error("location with incomplete LED configuration in testSameProductPick");
			return;
		}
		// Test serializer and deserializer and verify that all LED commands have a position.
		Assert.assertTrue(LedCmdGroupSerializer.verifyLedCmdGroupList(ledCmdGroupList));		
		String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList);		
		LOGGER.info("item cmd stream: " + theLedCommands);
	
		List<LedCmdGroup> dsLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(theLedCommands);
		Assert.assertTrue(LedCmdGroupSerializer.verifyLedCmdGroupList(dsLedCmdGroups));


		LedController theController = location.getEffectiveLedController();
		if (theController != null) {
			String theGuidStr = theController.getDeviceGuidStr();
			LightLedsMessage theMessage = new LightLedsMessage(theGuidStr, (short)1, 5, ledCmdGroupList);
			// check encode and decode of the message similar to how the jetty socket does it.
			JsonEncoder encoder = new JsonEncoder();
			String messageString = "";
			try {
				messageString = encoder.encode(theMessage);
			} catch (EncodeException e) {
				LOGGER.error("testSameProductPick Json encode", e);
			}
			
			JsonDecoder decoder = new JsonDecoder();
			MessageABC decodedMessage = null;
			try {
				decodedMessage = decoder.decode(messageString);
			} catch (DecodeException e) {
				LOGGER.error("testSameProductPick Json decode", e);
			}
			Assert.assertTrue(decodedMessage instanceof LightLedsMessage);
			String djsonLedCmdGroupsString = ((LightLedsMessage)decodedMessage).getLedCommands();			
			Assert.assertTrue(LightLedsMessage.verifyCommandString(djsonLedCmdGroupsString));
		}
		
		this.getPersistenceService().commitTenantTransaction();
	}

}
