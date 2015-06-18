/*******************************************************************************
 *  Codeshelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file CheProcessAssociate.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
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
public class CheProcessRemoteLink extends ServerTest {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessRemoteLink.class);
	private static final int	WAIT_TIME	= 4000;

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
	 * Utility function. Supplied picker should be in setupSummary state. If not, will fail.
	 * Supplied pick should not be in REMOTE_LINKED state. (parameterize if you need it.)
	 */
	private void setupInventoryForOrders(PickSimulator picker) {
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanSomething("gtin1123");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanSomething("%004290570250");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanSomething("gtin14933");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanSomething("%004290590150");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
	}

	/**
	 * Test of the associate getters, and the WorkService APIs for associate.
	 */
	@Test
	public final void testLinkProgramatically() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();

		startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Che che1 = this.getChe1();
		Che che2 = this.getChe2();
		CodeshelfNetwork network = che1.getParent();

		LOGGER.info("1: see the non-associated state");
		String state0Che1 = che1.getAssociateToUi();
		Assert.assertEquals("", state0Che1);
		String state0Che2 = che2.getAssociateToUi();
		Assert.assertEquals("", state0Che2);
		Assert.assertNull(che1.getLinkedToChe());
		Assert.assertNull(che2.getCheLinkedToThis());

		LOGGER.info("2: associate (mobile) CHE1 to (cart) CHE2");
		this.workService.linkCheToCheName(che1, "CHE2");

		LOGGER.info("2b1: check our associate getters");
		Assert.assertEquals(che2, che1.getLinkedToChe());
		Assert.assertEquals(che1, che2.getCheLinkedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		network = CodeshelfNetwork.staticGetDao().reload(network); // these are necessary or the WorkService functions have staleObjectUpdate exceptions

		LOGGER.info("2b2: check our associate getters in the next transaction");
		byte[] bytes = che1.getAssociateToCheGuid();
		Assert.assertNotNull(bytes);
		byte[] che2bytes = che2.getDeviceGuid();
		LOGGER.info("che1 pointing at:{} che2 is:{}", bytes, che2bytes);
		Assert.assertEquals(che2, che1.getLinkedToChe());
		Assert.assertEquals(che1, che2.getCheLinkedToThis());

		LOGGER.info("2b: check the UI field");
		String state1Che1 = che1.getAssociateToUi();
		Assert.assertEquals("controlling CHE2", state1Che1);
		String state1Che2 = che2.getAssociateToUi();
		Assert.assertEquals("controlled by CHE1", state1Che2);

		LOGGER.info("3: tell che1 to clear associations");
		this.workService.clearCheLink(che1);
		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		network = CodeshelfNetwork.staticGetDao().reload(network); // these are necessary or the WorkService functions have staleObjectUpdate exceptions

		Assert.assertNull(che1.getLinkedToChe());
		Assert.assertNull(che2.getCheLinkedToThis());
		Assert.assertNull(che2.getLinkedToChe());

		LOGGER.info("4: associate CHE2 to CHE1");
		this.workService.linkCheToCheName(che2, "CHE1");
		Assert.assertEquals(che1, che2.getLinkedToChe());
		Assert.assertEquals(che2, che1.getCheLinkedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		network = CodeshelfNetwork.staticGetDao().reload(network); // these are necessary or the WorkService functions have staleObjectUpdate exceptions

		LOGGER.info("4b: associate back the other way. Will give some warns");
		this.workService.linkCheToCheName(che1, "CHE2");
		Assert.assertEquals(che2, che1.getLinkedToChe());
		Assert.assertEquals(che1, che2.getCheLinkedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		network = CodeshelfNetwork.staticGetDao().reload(network);

		LOGGER.info("5: tell che2 to clear associations to it");
		this.workService.clearLinksToChe(che2);

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		network = CodeshelfNetwork.staticGetDao().reload(network);

		Assert.assertNull(che1.getLinkedToChe());
		Assert.assertNull(che2.getCheLinkedToThis());
		Assert.assertNull(che2.getLinkedToChe());

		commitTransaction();
	}

	/**
	 * Test link via CHE scans in a normal production manner.
	 * Tests valid and invalid link cases.
	 */
	@Test
	public final void testLinkChe() throws IOException {
		beginTransaction();
		setUpSmallNoSlotFacility();
		commitTransaction();
		// No orders file yet. Just testing associations

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1: Picker 1 scan REMOTE");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("2: Picker 1 scan the CHE2 name");
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		// Here we would see the che2 display

		LOGGER.info("3a: Picker 1 scan CLEAR from remote screen to clear association");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("Linked to: CHE2", picker1.getLastCheDisplayString(1));

		LOGGER.info("3b: Picker 1 scan CLEAR to clear link, then clear to exit");
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("4: Link to CHE2 again");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);

		LOGGER.info("5: Associate to unknown che. Result is still linked as it was to CHE2");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanSomething("H%CHE99");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		// linked will pick up the CHE2 actual display
		// Assert.assertEquals("Linked to: CHE2", picker1.getLastCheDisplayString(1));
		picker1.logCheDisplay();

		LOGGER.info("5b: While linked, other scans should pass through to the other che");
		picker1.scanSomething("XXXZZZ");
		picker1.scanCommand("INVENTORY");

		LOGGER.info("5c: Clear it. REMOTE while linked says Remote to continue.");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);

		LOGGER.info("5d: Clear it.");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		Assert.assertEquals("Linked to: (none)", picker1.getLastCheDisplayString(1));
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("6: Associate to itself");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanCommand("REMOTE"); // unlink old association, if any
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanSomething("H%CHE1");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("Linked to: (none)", picker1.getLastCheDisplayString(1));
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("7: Unnecessary unlink");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanCommand("REMOTE"); // unlink old association, if any. There is not.
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		Assert.assertEquals("Linked to: (none)", picker1.getLastCheDisplayString(1));
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
	}

	/**
	 * Test that the screens of the linked CHE are correct.
	 * Also a happy-day test of remote worker completing a job by pressing the button on the cart.
	 */
	@Test
	public final void testLinkedCheScreen() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.logCheDisplay();
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		LOGGER.info("2: Picker 1 login, scan REMOTE, and link to CHE2");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		// We see the che2 display

		LOGGER.info("3: Picker 1 scan START. This will advance the CHE2 cart state. CHE1 is in REMOTE_LINKED state");
		picker1.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals(CheStateEnum.REMOTE_LINKED, picker1.getCurrentCheState());
		picker1.logCheDisplay();
		// No jobs. 3 "other". The problem is the orders file does not have location, and the inventory is not set.

		LOGGER.info("4: Picker 1 scan INVENTORY and inventory what we need for order 12345");
		// notice, this is a remote inventory. Did not call setupInventoryForOrders() in this test.
		// notice picker2. waitForCheState or picker1.waitForLinkedCheState
		picker1.scanCommand("INVENTORY");
		picker2.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker1.scanSomething("gtin1123");
		picker2.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker1.scanSomething("%004290570250");
		picker2.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker1.scanSomething("gtin14933");
		picker1.waitForLinkedCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker1.scanSomething("%004290590150");
		picker1.waitForLinkedCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("5: Picker 1 scan a location on path. Get the job(s)");
		// substitute a tape scan here
		picker1.scanLocation("D301");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("6: push the button on the cart. Poscons on the cart CHE2, not the mobile CHE1");
		picker2.pickItemAuto();
		// was only one job on the route, so done
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay();

	}

	/**
	 * One mobile linked to cart, with work underway. Then log out.
	 * The cart poscons, LEDs, etc. should extinguish.
	 * Log in again, resume, although not quite as it was.
	 */
	@Test
	public final void remoteLogout() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.logCheDisplay();
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		LOGGER.info("2: Picker 1 login and inventory what we need");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		setupInventoryForOrders(picker1);

		LOGGER.info("2b: Picker1 scan REMOTE and link to CHE2");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());

		LOGGER.info("3: Picker 1 scan a location on path. Get the 1 job");
		// substitute a tape scan here
		picker1.scanLocation("D301");
		picker1.waitForLinkedCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForLinkedCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("3b: verify the aisle controller lights (if we can), and poscon on the cart");
		Assert.assertEquals(toByte(1), picker2.getLastSentPositionControllerDisplayValue((byte) 1));

		LOGGER.info("4: Picker 1 logout.");
		picker1.logout();

		LOGGER.info("4b: Check the state of the cart screen and poscons");
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		Assert.assertNull(picker2.getLastSentPositionControllerDisplayValue((byte) 1));

		LOGGER.info("5: Picker1 log in again. As nothing unlinked it, it comes to REMOTE screen showing CHE2 still");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.REMOTE);
		picker1.logCheDisplay();
		LOGGER.info("5b: Screen instructions suggest REMOTE to continue CHE2 link");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		// screen showing 1 order with location D301 as it was
		// Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		// Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());
		// Good enough for this test.

	}

	/**
	 * One mobile linked to cart, with work underway. Then link to different cart.
	 */
	@Test
	public final void remoteToDifferentCart() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PickSimulator picker3 = createPickSim(cheGuid3);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.logCheDisplay();
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			picker2.getLastSentPositionControllerDisplayValue((byte) 1));

		picker2.logout();
		Assert.assertNull(picker2.getLastSentPositionControllerDisplayValue((byte) 1));

		// Warning. Not inventory this test. So just seeing order count.		
		LOGGER.info("2: Picker1 scan REMOTE and link to CHE2. The CHE2 poscon shows feedback now.");
		picker1.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			picker2.getLastSentPositionControllerDisplayValue((byte) 1));

		LOGGER.info("3: let's see the CHE3 screen");
		picker3.loginAndCheckState("Picker #3", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("0 orders", picker3.getLastCheDisplayString(1).trim());

		LOGGER.info("4: Picker1 now remote to CHE3");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.scanSomething("H%CHE3");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);

		Assert.assertEquals("0 orders", picker1.getLastCheDisplayString(1).trim());

		LOGGER.info("5: CHE2 should be in idle state now.");
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);

	}

	/**
	 * One mobile linked to cart. Then cart worker scan from cart logout, badge  in.
	 */
	@Test
	public final void remoteVsCart() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.logCheDisplay();
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		LOGGER.info("2: Picker 1 login and inventory what we need");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		setupInventoryForOrders(picker1);

		LOGGER.info("2b: Picker1 scan REMOTE and link to CHE2");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());

		LOGGER.info("3: Picker 1 scan a location on path. Get the 1 job");
		// substitute a tape scan here
		picker1.scanLocation("D301");
		picker1.waitForLinkedCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForLinkedCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("4: Picker 2 (cart) logout");
		picker2.logout();
		// should result in picker 1 seeing it is not linked.
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("5: Picker 2 (cart) login, get work");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker2.logCheDisplay();
		// Done. cart is working independently. mobile is unlinked, on remote screen.
	}

	/**
	 * Test two mobiles "fighting" over control of one cart che
	 */
	@Test
	public final void competingRemotes1() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PickSimulator picker3 = createPickSim(cheGuid3);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.logCheDisplay();
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		LOGGER.info("2: Picker 1 login and inventory what we need");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		setupInventoryForOrders(picker1);

		LOGGER.info("2b: Picker1 scan REMOTE and link to CHE2");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());

		LOGGER.info("3: Picker 1 scan a location on path. Get the 1 job");
		// substitute a tape scan here
		picker1.scanLocation("D301");
		picker1.waitForLinkedCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForLinkedCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("4: Picker3 now log in as remote and steal CHE2");
		picker3.loginAndCheckState("Picker #3", CheStateEnum.SETUP_SUMMARY);
		picker3.scanCommand("REMOTE");
		picker3.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker3.scanSomething("H%CHE2");
		picker3.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker3.logCheDisplay();
		Assert.assertEquals("D301", picker2.getLastCheDisplayString(1).trim());
		Assert.assertEquals("D301", picker3.getLastCheDisplayString(1).trim());

		// picker 1 should be forced back to remote state
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		Assert.assertEquals("Linked to: (none)", picker1.getLastCheDisplayString(1));

		LOGGER.info("5: Check the database linkage via the UI fields");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Che che1 = Che.staticGetDao().findByPersistentId(che1PersistentId);
		Che che2 = Che.staticGetDao().findByPersistentId(che2PersistentId);
		Che che3 = Che.staticGetDao().findByPersistentId(che3PersistentId);
		CodeshelfNetwork.staticGetDao().reload(getNetwork()); // these are necessary or the WorkService functions have staleObjectUpdate exceptions

		String linkChe1 = che1.getAssociateToUi();
		Assert.assertEquals("", linkChe1);
		String linkChe2 = che2.getAssociateToUi();
		Assert.assertEquals("controlled by CHE3", linkChe2);
		String linkChe3 = che3.getAssociateToUi();
		Assert.assertEquals("controlling CHE2", linkChe3);
		commitTransaction();

	}

	/**
	 * Test one mobile trying to remote to other mobile that is already remote to CHE.
	 */
	@Test
	public final void competingRemotes2() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PickSimulator picker3 = createPickSim(cheGuid3);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		LOGGER.info("2: Picker 1 login and inventory what we need");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		setupInventoryForOrders(picker1);

		LOGGER.info("2b: Picker scan REMOTE and link to CHE2");
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());

		LOGGER.info("3: Picker 1 scan a location on path. Get the 1 job");
		// substitute a tape scan here
		picker1.scanLocation("D301");
		picker1.waitForLinkedCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForLinkedCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.logCheDisplay();

		LOGGER.info("4: Picker3 now log in as remote and try to control mobile CHE1");
		picker3.loginAndCheckState("Picker #3", CheStateEnum.SETUP_SUMMARY);
		picker3.scanCommand("REMOTE");
		picker3.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker3.scanSomething("H%CHE1");
		picker3.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		// picker 2 should experience a forced logout
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		picker1.logCheDisplay();
		picker2.logCheDisplay();
		picker3.logCheDisplay();
		// picker 3 state should be REMOTE_LINKED, because that is directly what the user did
		// mobile che 1 was linked, but when CHE 3 linked to it, the state had to change. Go to REMOTE with (none)
		Assert.assertEquals(CheStateEnum.REMOTE, picker1.getCurrentCheState());
		Assert.assertEquals("Linked to: (none)", picker1.getLastCheDisplayString(1));

		LOGGER.info("5: Check the database linkage via the UI fields");
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Che che1 = Che.staticGetDao().findByPersistentId(che1PersistentId);
		Che che2 = Che.staticGetDao().findByPersistentId(che2PersistentId);
		Che che3 = Che.staticGetDao().findByPersistentId(che3PersistentId);
		CodeshelfNetwork network = CodeshelfNetwork.staticGetDao().reload(getNetwork()); // these are necessary or the WorkService functions have staleObjectUpdate exceptions

		String linkChe1 = che1.getAssociateToUi();
		Assert.assertEquals("controlled by CHE3", linkChe1);
		String linkChe2 = che2.getAssociateToUi();
		Assert.assertEquals("", linkChe2);
		String linkChe3 = che3.getAssociateToUi();
		Assert.assertEquals("controlling CHE1", linkChe3);
		commitTransaction();
	}

	/**
	 * Test one mobile remote to other CHE. Then that one remotes to third CHE.
	 */
	@Test
	public final void competingRemotes3() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpOrdersWithCntrAndGtin(facility);
		commitTransaction();

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		PickSimulator picker3 = createPickSim(cheGuid3);

		LOGGER.info("1: Picker 2 sets up some jobs on CHE2, then logs out");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("12345", "1");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		String line1 = picker2.getLastCheDisplayString(1).trim();
		Assert.assertEquals("1 order", line1);
		picker2.logout();

		// no inventory needed for this test. Will not go to DO_PICK state

		LOGGER.info("2: Picker 1 scan REMOTE and link to CHE2");
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker1.scanCommand("REMOTE");
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		picker1.logCheDisplay();
		picker1.scanSomething("H%CHE2");
		picker1.waitForCheState(CheStateEnum.REMOTE_LINKED, WAIT_TIME);
		picker1.logCheDisplay();
		Assert.assertEquals("1 order", picker1.getLastCheDisplayString(1).trim());
		Assert.assertEquals("1 order", picker2.getLastCheDisplayString(1).trim());

		LOGGER.info("3: Picker2 now scan REMOTE. Not respected");
		// Note that picker 2 is active only as a result of picker1's login. So REMOTE would have to act much like log out.
		// It may not reasonably continue. We choose to do nothing, and WARN. Users only choice to use CHE 2 is to logout
		picker2.scanCommand("REMOTE");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// Look in log or console for "00009992: REMOTE scan from controlled CHE not allowed."

		LOGGER.info("4: Picker2 logout");
		picker2.scanCommand("LOGOUT");
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		picker1.waitForCheState(CheStateEnum.REMOTE, WAIT_TIME);
		// The remoteVsCart() test covers this
	}

	/**
	 * Temporary stub to isolate failures
	 * @throws IOException 
	 */
	// @Test
	public final void runManyTimes() throws IOException {
		for (int count = 1; count < 20; count++) {
			testLinkedCheScreen();
			doAfter();
			LOGGER.info("**Finished run #{}. Starting run #{}", count, count + 1);
			doBefore();
		}
	}

}
