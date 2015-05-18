/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
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

		return getFacility();
	}

	private void setTapeForTierNamed(String tierName, String tapeGuidString) {
		Location loc = getFacility().findSubLocationById(tierName);
		if (loc instanceof Tier) {
			Tier t = (Tier) loc;
			t.setTapeIdUi(tapeGuidString);
			Tier.staticGetDao().store(t);
			LOGGER.info("Tier:{} has tape id:{}", tierName, t.getTapeIdUi());
		}
	}

	private void setUpOrdersWithCntrAndGtin(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. With preassigned container number.
		// The result of importing this will be gtin made as we have the SKU and UOM. We can then create items via inventory actions.
		String csvOrders = "gtin,shipmentId,customerId,orderId,preassignedContainerId,orderDetailId,itemId,gtin,description,quantity,uom"
				+ "\r\n100,USF314,COSTCO,12345,12345,12345.1,1123,gtin1123,12/16 oz Bowl Lids -PLA Compostable,1,each"
				+ "\r\n101,USF314,COSTCO,12345,12345,12345.2,1493,gtin1493,PARK RANGER Doll,1,each"
				+ "\r\n102,USF314,COSTCO,12345,12345,12345.3,1522,gtin1522,Butterfly Yoyo,3,each"
				+ "\r\n103,USF314,COSTCO,11111,11111,11111.1,1122,gtin1122,8 oz Bowl Lids -PLA Compostable,2,each"
				+ "\r\n104,USF314,COSTCO,11111,11111,11111.2,1522,gtin1522,Butterfly Yoyo,1,each"
				+ "\r\n105,USF314,COSTCO,11111,11111,11111.3,1523,gtin1523,SJJ BPP,1,each"
				+ "\r\n106,USF314,COSTCO,11111,11111,11111.4,1124,gtin1124,8 oz Bowls -PLA Compostable,1,each"
				+ "\r\n107,USF314,COSTCO,11111,11111,11111.5,1555,gtin1555,paper towel,2,each";
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

		LOGGER.info("1g: scan Codeshelf tape corresponding to a different tier and offset. ");
		picker.scanSomething("%000D3030250");
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
		Item item1493locD402 = locationD402.getStoredItemFromMasterIdAndUom("1493", "ea");
// 		Assert.assertNotNull(item1493locD402);
// TODO fix
		
		LOGGER.info("2b: check that item 1123 moved via the tape scan to D303");
		Location locationD303 = facility.findSubLocationById("D303");
		Assert.assertNotNull(locationD303);
		Item item1123locD303 = locationD303.getStoredItemFromMasterIdAndUom("1123", "ea");
// 		Assert.assertNotNull(item1123locD303);
		this.getTenantPersistenceService().commitTransaction();
	}

	/**
	 * Login, scan a valid order detail ID, scan INVENTORY command.
	 * LOCAPICK is true, so create inventory on order import.
	 */
	public final void testInventory2() throws IOException {
// TODO add as test
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();

		setUpOrdersWithCntrAndGtin(facility);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.staticGetDao().store(che1);
		this.getTenantPersistenceService().commitTransaction();

		startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");
		Assert.assertEquals(CheStateEnum.IDLE, picker.getCurrentCheState());

		LOGGER.info("0a: scan INVENTORY and make sure we stay idle");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.IDLE, 1000);

		LOGGER.info("1a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("1b: scan X%INVENTORY, should go to SCAN_GTIN state");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1c: scan GTIN that does not exist - 200");
		picker.scanSomething("200");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1d: scan location. Should create item with GTIN at location D302");
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		LOGGER.info("1e: check that the item with GTIN 200 exists at D302");
		Location D302 = facility.findSubLocationById("D302");
		Assert.assertNotNull(D302);
		Item item200 = D302.getStoredItemFromMasterIdAndUom("200", "ea");
		Assert.assertNotNull(item200);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("2a: scan invalid commands");
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		picker.scanSomething("U%USER1");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("2b: scan GTIN that exists - 100");
		picker.scanSomething("100");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("2c: clear");
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.READY, 1000);

		LOGGER.info("3a: scan inventory command");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("3b: scan location before scanning GTIN");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
	}

}