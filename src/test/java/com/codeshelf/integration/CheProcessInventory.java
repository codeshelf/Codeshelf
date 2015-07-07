/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessInventory extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessInventory.class);

	// This is based on CheProcessLineScan which had a convenient no-slot facility.

	private Facility setUpSmallNoSlotFacility() {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle.
		//   With location alias for first baytier only, not second.
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		// Aisle 2 associated to same path segment. But with aisle controller on the other side
		// Aisle 3 will be on a separate path.
		// All tiers have controllers associated.
		// There are two CHE called CHE1 and CHE2

		String csvAisles = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,160,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n"; //
		importAislesData(getFacility(), csvAisles);

		// Get the aisle
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(getFacility(), "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvAliases = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D300\r\n" //
				+ "A1.B2, D400\r\n" //
				+ "A1.B3, D500\r\n" //
				+ "A1.B1.T1, D301\r\n" // will have codeshelf tape "d310" which translates to a large integer
				+ "A1.B2.T1, D302\r\n" // "d302"
				+ "A1.B3.T1, D303\r\n" // etc.
				+ "A1.B3.T1.S1, D3031\r\n" // etc.
				+ "A2.B1.T1, D401\r\n" //
				+ "A2.B2.T1, D402\r\n" //
				+ "A2.B3.T1, D403\r\n"//
				+ "A3.B1.T1, D501\r\n" //
				+ "A3.B2.T1, D502\r\n" //
				+ "A3.B3.T1, D503\r\n";//
		importLocationAliasesData(getFacility(), csvAliases);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController("LED3", new NetGuid("0x00000013"));

		Short channel1 = 1;
		Location tier = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		// Make sure we also got the alias
		String tierName = tier.getPrimaryAliasId();
		if (!tierName.equals("D301"))
			LOGGER.error("D301 vs. A1.B1.T1 alias not set up in setUpSimpleNoSlotFacility");

		tier = getFacility().findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A1.B3.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B2.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B1.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B2.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);

		// Note: the tape guids would vary. This just helps the test a bit.
		setTapeForTierNamed("D301", "d301");
		setTapeForTierNamed("D302", "d302");
		setTapeForTierNamed("D303", "d303");
		setTapeForTierNamed("D401", "d401");
		setTapeForTierNamed("D402", "d402");
		setTapeForTierNamed("D403", "d403");
		setTapeForTierNamed("D501", "d501");
		setTapeForTierNamed("D502", "d502");
		setTapeForTierNamed("D503", "d503");

		// Check one convert and extract
		Location loc301 = getFacility().findSubLocationById("D301");
		int locTapeId = loc301.getTapeId();
		String base32Id = CodeshelfTape.intToBase32(locTapeId);
		LOGGER.info("Location D301 has tapeId:{}, which converts to  base32:{}", locTapeId, base32Id);
		// Codeshelf tape scans to the numerals, because more numerals are actually more compact than fewer alpha characters.
		// The following is 429057 with offset 250 cm
		int extractTapeId = CodeshelfTape.extractGuid("%004290570250");
		Assert.assertEquals(locTapeId, extractTapeId);

		// Check one convert and extract
		Location loc303 = getFacility().findSubLocationById("D303");
		int loc303TapeId = loc303.getTapeId();
		String base32IdLoc303 = CodeshelfTape.intToBase32(loc303TapeId);
		LOGGER.info("Location D301 has tapeId:{}, which converts to  base32:{}", loc303TapeId, base32IdLoc303);
		// Codeshelf tape scans to the numerals, because more numerals are actually more compact than fewer alpha characters.
		// The following is 429057 with offset 250 cm
		int extractTapeIdLoc303 = CodeshelfTape.extractGuid("%004290590250");
		Assert.assertEquals(loc303TapeId, extractTapeIdLoc303);

		return getFacility();
	}

	private void setTapeForTierNamed(String tierName, String tapeGuidString) {
		Location loc = getFacility().findSubLocationById(tierName);
		if (loc instanceof Tier) {
			Tier t = (Tier) loc;
			t.setTapeIdUi(tapeGuidString);
			Tier.staticGetDao().store(t);
			LOGGER.info("Tier:{} has tape name:{} and tape id:{}", tierName, t.getTapeIdUi(), t.getTapeId());
		}
	}

	private void setUpOrdersWithCntrAndGtin(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. With preassigned container number.
		// The result of importing this will be gtin made as we have the SKU and UOM. We can then create items via inventory actions.
		String csvOrders = "shipmentId,customerId,orderId,preassignedContainerId,orderDetailId,itemId,gtin,description,quantity,uom"
				+ "\r\nUSF314,COSTCO,12345,12345,12345.1,1123,gtin1123,12/16 oz Bowl Lids -PLA Compostable,1,each"
				+ "\r\nUSF314,COSTCO,12345,12345,12345.2,1493,gtin1493,PARK RANGER Doll,1,case"
				+ "\r\nUSF314,COSTCO,12345,12345,12345.3,1522,gtin1522,Butterfly Yoyo,3,each"
				+ "\r\nUSF314,COSTCO,11111,11111,11111.1,1122,gtin1122,8 oz Bowl Lids -PLA Compostable,2,case"
				+ "\r\nUSF314,COSTCO,11111,11111,11111.2,1522,gtin1522,Butterfly Yoyo,1,each"
				+ "\r\nUSF314,COSTCO,11111,11111,11111.3,1523,gtin1523,SJJ BPP,1,case"
				+ "\r\nUSF314,COSTCO,11111,11111,11111.4,1124,gtin1124,8 oz Bowls -PLA Compostable,1,each"
				+ "\r\nUSF314,COSTCO,11111,11111,11111.5,1555,gtin1555,paper towel,2,case";
		importOrdersData(inFacility, csvOrders);
		
		// We expect these gtins are made.
		ItemMaster master = ItemMaster.staticGetDao().findByDomainId(inFacility, "1123");
		Assert.assertNotNull(master);
		Gtin gtin = Gtin.staticGetDao().findByDomainId(master, "gtin1123");
		Assert.assertNotNull(gtin);
	}

	/**
	 * Login, scan a valid order detail ID, scan INVENTORY command.
	 * LOCAPICK is true, so create inventory on order import.
	 */
	@Test
	public final void testInventory() throws IOException {
		/*
		Tier:D301 has tape name:D301 and tape id:429057
		Tier:D302 has tape name:D302 and tape id:429058
		Tier:D303 has tape name:D303 and tape id:429059
		Tier:D401 has tape name:D401 and tape id:430081
		Tier:D402 has tape name:D402 and tape id:430082
		Tier:D403 has tape name:D403 and tape id:430083
		Tier:D501 has tape name:D501 and tape id:431105
		Tier:D502 has tape name:D502 and tape id:431106
		Tier:D503 has tape name:D503 and tape id:431107
		 */

		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		// LOCAPICK off
		commitTransaction();

		beginTransaction();
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1b: scan X%INVENTORY, should go to SCAN_GTIN state");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1c: scan a GTIN that exists. We do not have this inventory yet.");
		picker.scanSomething("gtin1123");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1d: scan location. This is unlikely. Few sites would have barcodes up for location in our L% format");
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1e: scan another GTIN  and also place it by location.");
		picker.scanSomething("gtin1493");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();
		picker.scanLocation("D402");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1f: scan the first GTIN again. Now it would light where it is at (center of D302)");
		picker.scanSomething("gtin1123");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1g: scan Codeshelf tape corresponding to a different tier and offset. D303 tape Id is 429059 ");
		picker.scanSomething("%004290590250");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		picker.logCheDisplay();

		LOGGER.info("1h: scan X%CLEAR and get back to READY state");
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);

		LOGGER.info("1i: logout");
		picker.scanCommand("LOGOUT");
		picker.waitForCheState(CheStateEnum.IDLE, 1000);

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		LOGGER.info("2a: check that item 1493 is in D402");
		Location locationD402 = facility.findSubLocationById("D402");
		Assert.assertNotNull(locationD402);
		Item item1493locD402 = locationD402.getStoredItemFromMasterIdAndUom("1493", "CS");
		Assert.assertNotNull(item1493locD402);

		LOGGER.info("2b: check that item 1123 moved via the tape scan to a slot D3031 under tier D303");
		Location locationD3031 = facility.findSubLocationById("D3031");
		Assert.assertNotNull(locationD3031);
		Item item1123locD3031 = locationD3031.getStoredItemFromMasterIdAndUom("1123", "ea");
		Assert.assertNotNull(item1123locD3031);
		this.getTenantPersistenceService().commitTransaction();
	}

	/**
	 * Mimicking what may happen at new site:
	 * Do the inventory, before Codeshelf has the GTINs, etc.
	 * Only later, the orders  file has some, but not all of the GTINS.
	 */
	@Test
	public final void testInventoryOnboarding() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		// No orders file yet, so no GTINs or OrderMasters in the system

		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1a: inventory five gtins that will come in orders file to good locations");
		picker.inventoryViaTape("gtin1123", "%004290570250"); // D301
		picker.inventoryViaTape("gtin1493", "%004290580250"); // D302
		picker.inventoryViaTape("gtin1522", "%004290590250"); // D303
		picker.inventoryViaTape("gtin1122", "%004290590100"); // D303
		picker.inventoryViaTape("gtin1523", "%004290590050"); // D303

		LOGGER.info("1a2: one repeat inventory to different location");
		picker.inventoryViaTape("gtin1123", "%004290590050"); // was in D301. This is D303

		LOGGER.info("1b: inventory two gtins that will come in orders file to unknown tape locations");
		picker.inventoryViaTape("gtin1124", "%004299960250");
		picker.inventoryViaTape("gtin1555", "%004299970250");

		LOGGER.info("1c: inventory two gtins that will not come in orders file to good locations");
		picker.inventoryViaTape("gtin9996", "%004311050250"); // D501
		picker.inventoryViaTape("gtin9997", "%004311060250"); // D502

		LOGGER.info("1d: inventory two gtins that will not come in orders file to unknown tape locations");
		picker.inventoryViaTape("gtin9998", "%004299980250");
		picker.inventoryViaTape("gtin9999", "%004299990250");

		// probably need to wait here to allow the transactions to complete.
		ThreadUtils.sleep(4000);

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		LOGGER.info("1e: Verify that with the 12 inventory actions with 11 gtins, 11 gtins and 11 masters made");
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		List<ItemMaster> masters = ItemMaster.staticGetDao().getAll();
		List<Item> items = Item.staticGetDao().getAll();
		Assert.assertEquals(11, gtins.size());
		Assert.assertEquals(11, masters.size());
		Assert.assertEquals(11, items.size());

		log(gtins, masters, items);
		HashMap<String, Gtin> gtinHash = getGtinHash(gtins);
		assertItemsUom(gtinHash.get("gtin1493"), "EA");
		assertItemsUom(gtinHash.get("gtin1522"), "EA");
		assertItemsUom(gtinHash.get("gtin1122"), "EA");
		assertItemsUom(gtinHash.get("gtin1523"), "EA");
		assertItemsUom(gtinHash.get("gtin1123"), "EA");
		assertItemsUom(gtinHash.get("gtin1124"), "EA");
		assertItemsUom(gtinHash.get("gtin1555"), "EA");
		assertItemsUom(gtinHash.get("gtin9996"), "EA");
		assertItemsUom(gtinHash.get("gtin9997"), "EA");
		assertItemsUom(gtinHash.get("gtin9998"), "EA");
		assertItemsUom(gtinHash.get("gtin9999"), "EA");

		commitTransaction();

		// Following fails! "gtin1123" was made, guessing at master "gtin1123". When the orders file comes, the master SKU is "1123". The import does not
		// deal with this. After the import, our desired goal is no master with name gtin1123; there is a master SKU 1123; and gtin1123 belongs to the master.
		// Perhaps just changing the domainId of the master will work.
		// This is DEV-840
		
		LOGGER.info("2: Load the orders file with 7 gtins and SKUs");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		LOGGER.info("3: See what we have now");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		List<Gtin> gtins2 = Gtin.staticGetDao().getAll();
		List<ItemMaster> masters2 = ItemMaster.staticGetDao().getAll();
		List<Item> items2 = Item.staticGetDao().getAll();
		Assert.assertEquals(11, gtins2.size());
		Assert.assertEquals(11, masters2.size());
		Assert.assertEquals(11, items2.size());
		
		log(gtins2, masters2, items2);
		gtinHash = getGtinHash(gtins2);
		assertItemsUom(gtinHash.get("gtin1493"), "case");
		assertItemsUom(gtinHash.get("gtin1522"), "each");
		assertItemsUom(gtinHash.get("gtin1122"), "case");
		assertItemsUom(gtinHash.get("gtin1523"), "case");
		assertItemsUom(gtinHash.get("gtin1123"), "each");
		assertItemsUom(gtinHash.get("gtin1124"), "each");
		assertItemsUom(gtinHash.get("gtin1555"), "case");
		assertItemsUom(gtinHash.get("gtin9996"), "EA");
		assertItemsUom(gtinHash.get("gtin9997"), "EA");
		assertItemsUom(gtinHash.get("gtin9998"), "EA");
		assertItemsUom(gtinHash.get("gtin9999"), "EA");

		commitTransaction();
	}
	
	private void log(List<Gtin> gtins, List<ItemMaster> masters, List<Item> items){
		LOGGER.info("1f: Log gtins");
		for (Gtin gtin : gtins) {
			LOGGER.info("Gtin:{} for master:{}, uom: {}", gtin.getGtin(), gtin.getItemMasterId(), gtin.getUomMaster());
		}

		LOGGER.info("1g: Log item masters");
		for (ItemMaster master : masters) {
			LOGGER.info("Master:{} with gtins:{} and item locations:{}", master.getDomainId(), master.getItemGtins(), master.getItemLocations());
		}

		LOGGER.info("1h: Log items");
		for (Item item : items) {
			LOGGER.info("Item:{} for master:{}, uom: {}, and gtin:{}", item.getDomainId(), item.getItemMasterId(), item.getUomMaster(), item.getGtinId());
		}
	}

	private HashMap<String, Gtin> getGtinHash(List<Gtin> gtins) {
		HashMap<String, Gtin> gtinHash = new HashMap<>();
		for (Gtin gtin : gtins) {
			gtinHash.put(gtin.getDomainId(), gtin);
		}
		return gtinHash;
	}
	
	private void assertItemsUom(Gtin gtin, String expectedUom) {
		Assert.assertNotNull(gtin);
		Assert.assertNotNull(gtin.getParent());
		List<Item> items = gtin.getParent().getItems();
		Assert.assertTrue("Could not find items for Gtin " + gtin, !items.isEmpty());
		for (Item item : items) {
			Assert.assertEquals(expectedUom, item.getUomMasterId());
		}
	}
}
