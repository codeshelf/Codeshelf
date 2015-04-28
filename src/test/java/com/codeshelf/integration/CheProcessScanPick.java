/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessScanPick extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessScanPick.class);

	public CheProcessScanPick() {

	}

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

		/*
		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);
		*/

		/*
		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);
		*/

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
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
		importAislesData(getFacility(), csvString);

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

		String csvLocationAliases = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D300\r\n" //
				+ "A1.B2, D400\r\n" //
				+ "A1.B3, D500\r\n" //
				+ "A1.B1.T1, D301\r\n" //
				+ "A1.B2.T1, D302\r\n" //
				+ "A1.B3.T1, D303\r\n" //
				+ "A2.B1.T1, D401\r\n" //
				+ "A2.B2.T1, D402\r\n" //
				+ "A2.B3.T1, D403\r\n"//
				+ "A3.B1.T1, D501\r\n" //
				+ "A3.B2.T1, D502\r\n" //
				+ "A3.B3.T1, D503\r\n";//
		importLocationAliasesData(getFacility(), csvLocationAliases);

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

		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.BayDistance.toString());

		return getFacility();
	}

	private void setUpLineScanOrdersNoCntr(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. No preassigned container number.
		// Using preferredLocation. No inventory.
		// Locations D301-303, 401-403, 501-503 are modeled. 600s and 700s are not.
		// Order 12345 has 2 modeled locations and one not.
		// Order 11111 has 4 unmodeled locations and one modeled.

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n,USF314,COSTCO,12345,12345.3,1522,Butterfly Yoyo,3,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401"
				+ "\r\n,USF314,COSTCO,11111,11111.2,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.3,1523,SJJ BPP,1,each, D602"
				+ "\r\n,USF314,COSTCO,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603"
				+ "\r\n,USF314,COSTCO,11111,11111.5,1555,paper towel,2,each, D604";
		importOrdersData(inFacility, csvOrders);
	}

	private void setUpOrdersItemsOnSamePath(Facility inFacility) throws IOException {
		// Exactly the same as above, but with preAssignedContainerId set equal to the orderId

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,123,123,123.1,1122,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,456,456,456.1,1122,12/16 oz Bowl Lids -PLA Compostable,1,each, D302"
				+ "\r\n,USF314,COSTCO,789,789,789.1,1122,12/16 oz Bowl Lids -PLA Compostable,1,each, D303";
		importOrdersData(inFacility, csvOrders);
	}

	private void setUpLineScanOrdersWithCntr(Facility inFacility) throws IOException {
		// Exactly the same as above, but with preAssignedContainerId set equal to the orderId

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.3,1522,Butterfly Yoyo,3,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.2,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.3,1523,SJJ BPP,1,each, D602"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.5,1555,paper towel,2,each, D604";
		importOrdersData(inFacility, csvOrders);
	}

	private void setUpOrdersWithCntrAndSequence(Facility inFacility) throws IOException {
		// Exactly the same as above, but with preAssignedContainerId set equal to the orderId

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom, locationId, workSequence"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301, 4000"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.2,1493,PARK RANGER Doll,1,each, D302, 4001"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.3,1522,Butterfly Yoyo,3,each, D601, 2000"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401, 3000"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.2,1522,Butterfly Yoyo,1,each, D601, 2000"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.3,1523,SJJ BPP,1,each, D602, 2001"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603, 2002"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.5,1555,paper towel,2,each, D604, 2003";
		importOrdersData(inFacility, csvOrders);
	}

	private void setUpLineScanOrdersNoCntrWithGtin(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. No preassigned container number.
		// Locations D301-303, 401-403, 501-503 are modeled. 600s and 700s are not.

		String csvOrders = "gtin,shipmentId,customerId,orderId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n100,USF314,COSTCO,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n101,USF314,COSTCO,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n102,USF314,COSTCO,12345,12345.3,1522,Butterfly Yoyo,3,each, D303"
				+ "\r\n103,USF314,COSTCO,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401"
				+ "\r\n104,USF314,COSTCO,11111,11111.2,1522,Butterfly Yoyo,1,each, D402"
				+ "\r\n105,USF314,COSTCO,11111,11111.3,1523,SJJ BPP,1,each, D403"
				+ "\r\n106,USF314,COSTCO,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D501"
				+ "\r\n107,USF314,COSTCO,11111,11111.5,1555,paper towel,2,each, D502";
		importOrdersData(inFacility, csvOrders);
	}
	
	/**
	 * Simple test of INVENTORY command
	 */
	@Test
	public final void testInventoryCommand() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: Set LOCAPICK, then import the orders file, with containerId.");

		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));

		setUpLineScanOrdersNoCntrWithGtin(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
	
		picker.loginAndSetup("Picker #1");
		
		LOGGER.info("1b: Scan INVENTORY command");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1c: scan a GTIN and check item location is correct");
		picker.scanSomething("100");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1d: scan another GTIN");
		picker.scanSomething("103");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1e: scan location");
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1f: scan a new GTIN");
		picker.scanSomething("101");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1g: scan another GTIN");
		picker.scanSomething("102");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1h: scan location");
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1i: scan another location. should move here.");
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1j: scan X%CLEAR and get back to READY state");
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		
		LOGGER.info("1k: scan X%INVENTORY");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("1l: logout");
		picker.scanCommand("LOGOUT");
		picker.waitForCheState(CheStateEnum.IDLE, 1000);
		
		this.getTenantPersistenceService().beginTransaction();
		facility  = Facility.staticGetDao().reload(facility);
		
		LOGGER.info("2a: check that item 100 stayed in it's original location");
		Location locationD301 = facility.findSubLocationById("D301");
		Assert.assertNotNull(locationD301);
		Item item1123locD301 = locationD301.getStoredItemFromMasterIdAndUom("1123", "ea");
		Assert.assertNotNull(item1123locD301);
		
		LOGGER.info("2b: check that item 1123 moved from D301 to D302");
		Location locationD302 = facility.findSubLocationById("D302");
		Assert.assertNotNull(locationD302);
		locationD301 = facility.findSubLocationById("D301");
		Assert.assertNotNull(locationD301);
		
		Item item1122locD302 = locationD302.getStoredItemFromMasterIdAndUom("1122", "ea");
		Assert.assertNotNull("Item 1122 (GTIN 103) should have moved to this location", item1122locD302);
		Item item1122locD301 = locationD301.getStoredItemFromMasterIdAndUom("1122", "ea");
		Assert.assertNull("Item 1122 should no longer be at this location", item1122locD301);
		
		LOGGER.info("2c: check that item 1522 moved to D302 and not D301");
		Item gtin102itemLocD302 = locationD302.getStoredItemFromMasterIdAndUom("1522", "ea");
		Assert.assertNotNull(gtin102itemLocD302);
		Item item1522LocD301 = locationD301.getStoredItemFromMasterIdAndUom("1522", "ea");
		Assert.assertNull(item1522LocD301);
		
		this.getTenantPersistenceService().commitTransaction();
		
	}
	
	/**
	 * Simple test of INVENTORY command
	 */
	@Test
	public final void testInventoryCommand2() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: Set LOCAPICK, then import the orders file, with containerId.");

		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));

		setUpLineScanOrdersNoCntrWithGtin(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
	
		LOGGER.info("0a: scan INVENTORY and make sure we stay idle");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.IDLE, 1000);
		
		LOGGER.info("1a: login, should go to READY state");
		picker.loginAndSetup("Picker #1");
		
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
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		
		LOGGER.info("3a: scan inventory command");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
		LOGGER.info("3b: scan location before scanning GTIN");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);
		
	}
	
	/**
	 * A trivial reference test of Setup_Orders
	 */
	@Test
	public final void testNotScanPick() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);

		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: Set LOCAPICK, then import the orders file again, with containerId");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));

		setUpLineScanOrdersWithCntr(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		picker.loginAndSetup("Picker #1");

		LOGGER.info("1b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1c: START. Now we get some work. 3 jobs, since only 3 details had modeled locations");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);

		LOGGER.info("1d: scan a valid location. Log out the work instructions that we got.");
		picker.scanLocation("D303");
		// DEV-653 go to SCAN_SOMETHING state instead of DO_PICK
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		// verify position index
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Container c1 = Container.staticGetDao().findByDomainId(facility, "12345");
		ContainerUse cu1 = ContainerUse.staticGetDao().findByDomainId(c1, "12345");
		Assert.assertTrue(cu1.getPosconIndex()==1);
		Container c2 = Container.staticGetDao().findByDomainId(facility, "11111");
		ContainerUse cu2 = ContainerUse.staticGetDao().findByDomainId(c2, "11111");
		Assert.assertTrue(cu2.getPosconIndex()==2);
		this.getTenantPersistenceService().commitTransaction();

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		Assert.assertEquals(3, scWiList.size());
		logWiList(scWiList);

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

	}

	/**
	 * Simple test of Setup_Orders with SCANPICK. DEV-653 is the SCANPICK enhancement
	 */
	@Test
	public final void testScanPick() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: Set LOCAPICK, then import the orders file, with containerId. Also set SCANPICK");

		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "SKU");

		setUpLineScanOrdersWithCntr(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);

		String scanPickValue = manager.getScanTypeValue();
		LOGGER.info("Default SCANPICK value for test is " + scanPickValue);
		Assert.assertNotEquals("SKU", manager.getScanTypeValue());
		// We would rather have the device manager know from the SCANPICK parameter update, but that does not happen yet in the integration test.
		// kludgy! Somewhat simulates restarting site controller
		manager.setScanTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getScanTypeValue());
		picker.forceDeviceToMatchManagerConfiguration();

		// A small side trip. The enumeration for scan verification values is private. The only way to unit test odd values is here.
		// see these logged in the console. The picker has the ancestor CheDeviceLogic. No interface to get this private field from SetupOrderDeviceLogic
		// Will see 4 in a row to NO_SCAN_TO_VERIFY
		manager.setScanTypeValue("UPC");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("UPC", picker.getCheDeviceLogic().getScanVerificationType());
		manager.setScanTypeValue("Disabled");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("disabled", picker.getCheDeviceLogic().getScanVerificationType());
		manager.setScanTypeValue("");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("disabled", picker.getCheDeviceLogic().getScanVerificationType());
		manager.setScanTypeValue(null);
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("disabled", picker.getCheDeviceLogic().getScanVerificationType());
		manager.setScanTypeValue("xxxx");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("disabled", picker.getCheDeviceLogic().getScanVerificationType());
		manager.setScanTypeValue("LPN");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("LPN", picker.getCheDeviceLogic().getScanVerificationType());
		// Now set as we want it for this test
		manager.setScanTypeValue("SKU");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("SKU", picker.getCheDeviceLogic().getScanVerificationType());

		picker.loginAndSetup("Picker #1");

		LOGGER.info("1b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1c: START. Now we get some work. 3 jobs, since only 3 details had modeled locations");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);

		LOGGER.info("1d: scan a valid location. This does the usual, but with SCANPICK, it goes to SCAN_SOMETHING state. The brightness is different");
		picker.scanLocation("D303");

		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		Assert.assertEquals(3, scWiList.size());
		logWiList(scWiList);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.MIDDIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		LOGGER.info("1e: although the poscon shows the count, prove that the button press is not handled");
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		scWiList = picker.getAllPicksList();
		Assert.assertEquals(3, scWiList.size());
		logWiList(scWiList);
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2);

		LOGGER.info("1f: scan the SKU. This data has 1493. After the scan, the brightness increases again.");
		picker.scanSomething("1493");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		LOGGER.info("1g: now the button press works");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals(2, picker.countRemainingJobs());

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("2a: setup same two orders on the cart. Start. Location. Brings to SCAN_SOMETHING state");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("2b: scan incorrect SKU. This pick should be 1123");
		picker.scanSomething("1555");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		// This had the side effect of setting E on the poscons

		LOGGER.info("2c: See if you can logout from SCAN_SOMETHING state");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("3a: setup same two orders again. Start. Location. Brings to SCAN_SOMETHING state");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("3b: scan correct SKU. This pick should be 1123");
		picker.scanSomething("1123");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("3c: see that normal short process works from this point");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		// Leaves us at the DO_PICK stage, as we already scanned to confirm the job.

		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("4a: setup same two orders again. Start. Location. Brings to SCAN_SOMETHING state");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("4b: No product present. Worker's only choice is to scan short");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000); // like SHORT_PICK_CONFIRM

		LOGGER.info("4c: Scan NO on the confirm message");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals(2, picker.countRemainingJobs()); // still 2 jobs

		LOGGER.info("4d: Worker decides to complete the short.");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000); // like SHORT_PICK_CONFIRM
		LOGGER.info("4c: Scan YES on the confirm message");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals(1, picker.countRemainingJobs()); // that job shorted. Only one left
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("5a: setup again. Just to see that we can logout from SCAN_SOMETHING_SHORT state");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("5b: No product present. Worker's only choice is to scan short");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000); // like SHORT_PICK_CONFIRM

		LOGGER.info("5c: logout from this confirm screen");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("6a: setup again. Checking if scanskip works");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		picker.scanSomething("SCANSKIP");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("6b: logout from this confirm screen");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

	}

	/**
	 * Exploring what happens when user scans the wrong thing
	 */
	@Test
	public final void testScanPickError() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: Set LOCAPICK, then import the orders file, with containerId. Also set SCANPICK");

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "SKU");

		setUpLineScanOrdersWithCntr(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		CsDeviceManager manager = this.getDeviceManager();
		// We would rather have the device manager know from the SCANPICK parameter update,
		manager.setScanTypeValue("SKU");
		picker.forceDeviceToMatchManagerConfiguration();
		Assert.assertEquals("SKU", picker.getCheDeviceLogic().getScanVerificationType());

		LOGGER.info("1b: setup two orders on the cart. Start. Location. Brings to SCAN_SOMETHING state");
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("1c: scan incorrect SKU. The SKU for this pick should be 1493");
		picker.scanSomething("1555");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		// This had the side effect of setting E on the poscons
		Assert.assertTrue(picker.getLastSentPositionControllerMinQty((byte) 1) == PosControllerInstr.BITENCODED_LED_E);
		Assert.assertTrue(picker.getLastSentPositionControllerMinQty((byte) 2) == PosControllerInstr.BITENCODED_LED_E);

		LOGGER.info("1d: scan correct SKU.");
		picker.scanSomething("1493");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		// This should clear the E
		Assert.assertFalse(picker.getLastSentPositionControllerMinQty((byte) 1) == PosControllerInstr.BITENCODED_LED_E);
		// Assert.assertFalse(picker.getLastSentPositionControllerMinQty((byte) 2) == PosControllerInstr.BITENCODED_LED_E);

		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

	}

	@Test
	public void missingLocationIdShouldHaveNoWork() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom, locationId, workSequence"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, , 4000";
		importOrdersData(facility, csvOrders);

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);

		// We would rather have the device manager know from parameter updates, but that does not happen yet in the integration test.
		manager.setSequenceKind(WorkInstructionSequencerType.WorkSequence.toString());
		Assert.assertEquals(WorkInstructionSequencerType.WorkSequence.toString(), manager.getSequenceKind());
		picker.forceDeviceToMatchManagerConfiguration();

		picker.loginAndSetup("Picker #1");

		LOGGER.info("1b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		LOGGER.info("1c: START. Now we get some work. 3 jobs, since only 3 details had modeled locations");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK, 4000);

	}

	@Test
	public void preferredLocationGetsSecondItemInPath() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1a: Set LOCAPICK, then import the orders file again, with containerId");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));
		this.setUpOrdersItemsOnSamePath(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());
		picker.loginAndSetup("Picker #1");

		picker.setupContainer("456", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("2c: START. Should get some work");
		picker.scanCommand("START");

		// Probably important. The case above yields some problems so that we hit LOCATION_SELECT_REVIEW state.
		// Really should replicate this test case that is all clean so it goes to LOCATION_SELECT state.  The second START should work there also.
		//picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 4000);

		LOGGER.info("2d: in WorkSequence mode, we scan start again, instead of a location");

		picker.scanLocation("");

		picker.waitForCheState(CheStateEnum.DO_PICK, 4000); //scan sku is off

		List<WorkInstruction> scWiList = picker.getAllPicksList();

		logWiList(scWiList);
		Assert.assertEquals(1, scWiList.size());
		Assert.assertEquals("D302", scWiList.get(0).getPickInstruction());

	}

	final boolean	DoNotScanUPC	= false;
	final boolean	DoScanUPC		= true;

	/**
	 * Simple test of Setup_Orders with SCANPICK. DEV-653 is the SCANPICK enhancement
	 * These values are critically dependent on setUpOrdersWithCntrAndSequence
	 * Notice that the first two are 1522 from D601, so are subject to simultaneous work instruction.
	 */
	@Test
	public void workSequencePickForward() throws IOException {
		String[][] sortedItemLocs = { //the forward direction. Comments protect the pretty printer.
		{ "1522", "D601" }, // 
				{ "1522", "D601" }, //
				{ "1523", "D602" }, //
				{ "1124", "D603" }, //
				{ "1555", "D604" }, //
				{ "1122", "D401" }, //
				{ "1123", "D301" }, // 
				{ "1493", "D302" } //
		};
		testPfswebWorkSequencePicks("START", sortedItemLocs);
	}

	/**
	 * Simple test of Setup_Orders with SCANPICK. DEV-653 is the SCANPICK enhancement but in reverse
	 * These values are critically dependent on setUpOrdersWithCntrAndSequence
	 */
	@Test
	public void workSequencePickReverse() throws IOException {
		String[][] sortedItemLocs = { //the forward direction. Comments protect the pretty printer.
		{ "1522", "D601" }, // 
				{ "1522", "D601" }, //
				{ "1523", "D602" }, //
				{ "1124", "D603" }, //
				{ "1555", "D604" }, //
				{ "1122", "D401" }, //
				{ "1123", "D301" }, // 
				{ "1493", "D302" } //
		};
		//reverse it here
		ArrayUtils.reverse(sortedItemLocs);
		testPfswebWorkSequencePicks("REVERSE", sortedItemLocs);
	}

	/**
	 * Important: this is used primarily for sequence testing, but it assumes a lot about CHE process. It may break due to any process change, as it did
	 * for DEV-692.  Full simultaneous work instruction support will break it again.
	 * 
	 * For now, have needScan = false for most sequence testing. As this the main test for scanPick, added the needScan parameter.
	 * jobCountToCheck parameter is new. If zero, does all. If set, will abort and logout only working partway through the list.
	 */
	private final void testPfswebWorkSequencePicks(String scanDirection, String[][] sortedItemLocs) throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		this.setUpOrdersWithCntrAndSequence(facility);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set SCANPICK, set WORKSEQR");

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		// we are not setting SCANPICK for this test. Only about sequencing
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController(); // after all the parameter changes

		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);

		Assert.assertEquals(WorkInstructionSequencerType.WorkSequence.toString(), manager.getSequenceKind());
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		picker.loginAndSetup("Picker #1");

		LOGGER.info("1b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1c: START. Now we get some work. 3 jobs, since only 3 details had modeled locations");
		picker.scanCommand("START");

		// DEV-637 note. After that is implemented, we would get plans here even though LOCAPICK is off and we do not get any inventory.		
		// Shouldn't we get work? We have supplied location, and sequence. 
		//picker.waitForCheState(CheStateEnum.NO_WORK, 4000);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 6000);

		// logout back to idle state.
		picker.logout();

		LOGGER.info("2a: Redo, but with LOCAPICK on. SCANPICK, WORKSEQR as in case 1");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(true));
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		this.setUpOrdersWithCntrAndSequence(facility);
		this.getTenantPersistenceService().commitTransaction();

		picker.loginAndSetup("Picker #1");

		LOGGER.info("2b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("2c: START. Should get some work");
		picker.scanCommand("START");

		// Probably important. The case above yields some problems so that we hit LOCATION_SELECT_REVIEW state.
		// Really should replicate this test case that is all clean so it goes to LOCATION_SELECT state.  The second START should work there also.
		//picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 4000);

		LOGGER.info("2d: in WorkSequence mode, we scan start again, instead of a location");

		picker.scanCommand(scanDirection);

		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		List<WorkInstruction> scWiList = picker.getAllPicksList();

		logWiList(scWiList);
		Assert.assertEquals(sortedItemLocs.length, scWiList.size());

		LOGGER.info("2e:work through it, making sure it matches the work sequence order.");

		for (int i = 0; i < sortedItemLocs.length; i++) {
			String item = sortedItemLocs[i][0];
			String loc = sortedItemLocs[i][1];
			boolean last = (i == sortedItemLocs.length - 1);
			tryPick(picker, item, loc, (!last) ? CheStateEnum.DO_PICK : picker.getCompleteState());
		}
		picker.logout();
	}

	/**
	 * The needScan bit is confusing. The problem is that with simultaneous work instructions, not all work instructions need a scan.
	 */
	private void tryPick(PickSimulator picker, String itemId, String expectedLocation, CheStateEnum nextExpectedState) {
		Assert.assertEquals(expectedLocation, picker.getLastCheDisplayString(1));
		Assert.assertEquals(itemId, picker.getLastCheDisplayString(2));
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(nextExpectedState, 4000);
	}

	/**
	 * Test for DEV-692, which skips scanning if we already scanned that SKU at that location.
	 * LOCAPICK = false; SCANPICK = SKU; WORKSEQR = WorkSequence
	 */
	@Test
	public final void testPfswebScanPicks() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		this.setUpOrdersWithCntrAndSequence(facility);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set SCANPICK, set WORKSEQR");

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "SKU");
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController(); // after all the parameter changes

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		picker.loginAndSetup("Picker #1");

		LOGGER.info("1b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1c: START. Now we get some work. 8 jobs, only 3 with modeled locations");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 4000);
		LOGGER.info("1d: from v13, we can scan start again, instead of a location");
		picker.scanCommand("START");

		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		logWiList(scWiList);
		/*
				{ "1522", "D601" }, // 
				{ "1522", "D601" }, //
				{ "1523", "D602" }, //
				{ "1124", "D603" }, //
				{ "1555", "D604" }, //
				{ "1122", "D401" }, //
				{ "1123", "D301" }, // 
				{ "1493", "D302" } //
		*/
		// Note: WorkSequenceComparator from v14 sorts by sequence, then item, then orderID. Therefore, the quantity 1 one comes first, as its orderId is lower.
		LOGGER.info("2a :The first job needs a scan.");
		Assert.assertEquals("D601", picker.getLastCheDisplayString(1));
		Assert.assertEquals("1522", picker.getLastCheDisplayString(2));
		Assert.assertEquals("QTY 4", picker.getLastCheDisplayString(3)); // This line may change due to function changes. Just update it
		picker.scanSomething("1522");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("2b :The second job does not need a scan. The quantity counted down as the first is complete.");
		Assert.assertEquals("D601", picker.getLastCheDisplayString(1));
		Assert.assertEquals("1522", picker.getLastCheDisplayString(2));
		Assert.assertEquals("QTY 3", picker.getLastCheDisplayString(3)); // This line may change due to function changes. Just update it
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("2c :The third job needs a scan.");
		Assert.assertEquals("D602", picker.getLastCheDisplayString(1));
		Assert.assertEquals("1523", picker.getLastCheDisplayString(2));
		Assert.assertEquals("QTY 1", picker.getLastCheDisplayString(3)); // This line may change due to function changes. Just update it
		picker.scanSomething("1523");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		LOGGER.info("2d :The fourth job needs a scan.  Logout as the rest is not interesting.");

		picker.logout();
	}

}
