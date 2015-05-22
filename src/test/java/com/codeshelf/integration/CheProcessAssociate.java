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
public class CheProcessAssociate extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessAssociate.class);

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
	 * Test of the associate getters, and the WorkService APIs for associate.
	 */
	@Test
	public final void testAssociateProgramatically() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Che che1 = this.getChe1();
		Che che2 = this.getChe2();

		LOGGER.info("1: see the non-associated state");
		String state0Che1 = che1.getAssociateToUi();
		Assert.assertEquals("", state0Che1);
		String state0Che2 = che2.getAssociateToUi();
		Assert.assertEquals("", state0Che2);
		Assert.assertNull(che1.getAssociateToChe());
		Assert.assertNull(che2.getCheAssociatedToThis());

		LOGGER.info("2: associate (mobile) CHE1 to (cart) CHE2");
		this.workService.associateCheToCheName(che1, "CHE2");

		LOGGER.info("2b1: check our associate getters");
		Assert.assertEquals(che2, che1.getAssociateToChe());
		Assert.assertEquals(che1, che2.getCheAssociatedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);

		LOGGER.info("2b2: check our associate getters in the next transaction");
		byte[] bytes = che1.getAssociateToCheGuid();
		Assert.assertNotNull(bytes);
		byte[] che2bytes = che2.getDeviceGuid();
		LOGGER.info("che1 pointing at:{} che2 is:{}", bytes, che2bytes);
		Assert.assertEquals(che2, che1.getAssociateToChe());
		Assert.assertEquals(che1, che2.getCheAssociatedToThis());

		LOGGER.info("2b: check the UI field");
		String state1Che1 = che1.getAssociateToUi();
		Assert.assertEquals("this-->CHE2-00009992", state1Che1);
		String state1Che2 = che2.getAssociateToUi();
		Assert.assertEquals("CHE1-00009991-->this", state1Che2);

		LOGGER.info("3: tell che1 to clear associations");
		this.workService.clearCheAssociation(che1);
		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);

		Assert.assertNull(che1.getAssociateToChe());
		Assert.assertNull(che2.getCheAssociatedToThis());
		Assert.assertNull(che2.getAssociateToChe());

		LOGGER.info("4: associate CHE2 to CHE1");
		this.workService.associateCheToCheName(che2, "CHE1");
		Assert.assertEquals(che1, che2.getAssociateToChe());
		Assert.assertEquals(che2, che1.getCheAssociatedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);

		LOGGER.info("4b: associate back the other way. Will give some warns");
		this.workService.associateCheToCheName(che1, "CHE2");
		Assert.assertEquals(che2, che1.getAssociateToChe());
		Assert.assertEquals(che1, che2.getCheAssociatedToThis());

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);
		facility = Facility.staticGetDao().reload(facility);

		LOGGER.info("5: tell che2 to clear associations to it");
		this.workService.clearAssociationsToChe(che2);

		// Commit, primarily to have the network update go to site controller. Should see that in the log.
		commitTransaction();
		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		che2 = Che.staticGetDao().reload(che2);

		Assert.assertNull(che1.getAssociateToChe());
		Assert.assertNull(che2.getCheAssociatedToThis());
		Assert.assertNull(che2.getAssociateToChe());

		commitTransaction();

	}

	/**
	 * Test using association via CHE scans
	 */
	@Test
	public final void testAssociateChe() throws IOException {
		beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		commitTransaction();
		// No orders file yet, so no GTINs or OrderMasters in the system

		startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
	}

}
