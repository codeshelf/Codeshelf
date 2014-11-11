/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.gadgetworks.codeshelf.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.common.base.Strings;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTest extends EndToEndIntegrationTest {

	private static final Logger				LOGGER			= LoggerFactory.getLogger(CheProcessTest.class);

	static {
		Configuration.loadConfig("test");
	}

	public CheProcessTest() {
	}

	@SuppressWarnings({ "rawtypes" })
	private Facility setUpSimpleNoSlotFacility() {
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

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, getFacility(), ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", getFacility());
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest("F5X.3", getFacility());
		PathSegment segment02 = addPathSegmentForTest("F5X.3.0", path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.DAO.findByDomainId(getFacility(), "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
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

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(reader2, getFacility(), ediProcessTime2);

		/*
		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = getFacility().createNetwork(nName);
		Che che1 = network.createChe("CHE1", new NetGuid("0x00000001"));
		Che che2 = network.createChe("CHE2", new NetGuid("0x00000002"));
		*/

		CodeshelfNetwork network = getNetwork();
		Organization organization = getOrganization();
		String organizationId = organization.getDomainId();

		LedController controller1 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000013"));

		SubLocationABC tier = (SubLocationABC) getFacility().findSubLocationById("A1.B1.T1");
		tier.setLedController(controller1);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A1.B2.T1");
		tier.setLedController(controller1);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A1.B3.T1");
		tier.setLedController(controller1);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A2.B1.T1");
		tier.setLedController(controller2);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A2.B2.T1");
		tier.setLedController(controller2);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A3.B1.T1");
		tier.setLedController(controller3);
		tier.getDao().store(tier);
		tier = (SubLocationABC) getFacility().findSubLocationById("A3.B2.T1");
		tier.setLedController(controller3);
		tier.getDao().store(tier);

		return getFacility();
	}

	private void setUpSmallInventoryAndOrders(Facility inFacility) throws IOException {
		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1122,D302,8 oz Bowl Lids -PLA Compostable,,ea,6/25/14 12:00,80\r\n" //
				+ "1123,D301,12/16 oz Bowl Lids -PLA Compostable,,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D303,8 oz Bowls -PLA Compostable,,ea,6/25/14 12:00,55\r\n" //
				+ "1493,D301,PARK RANGER Doll,,ea,6/25/14 12:00,66\r\n" //
				+ "1522,D302,Butterfly Yoyo,,ea,6/25/14 12:00,3\r\n" //
				+ "1523,D301,SJJ BPP, ,each,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, inFacility, ediProcessTime);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1122,8 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1523,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1124,8 oz Bowls -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}

	@SuppressWarnings({ "unused", "rawtypes" })
	@Test
	public final void testPick() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		LocationABC locationD403 = (LocationABC) facility.findSubLocationById("D403");
		LocationABC locationD402 = (LocationABC) facility.findSubLocationById("D402");
		LocationABC locationD502 = (LocationABC) facility.findSubLocationById("D502");
		LocationABC locationD503 = (LocationABC) facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			}
		}
		Assert.assertEquals(2, itemLocations.size());

		// Turn off housekeeping work instructions so as to not confuse the counts
		HousekeepingInjector.turnOffHK();
		// Set up a cart for order 12345, which will generate work instructions
		facility.setUpCheContainerFromString(che1, "12345");
		HousekeepingInjector.restoreHKDefaults();

		List<WorkInstruction> aList = facility.getWorkInstructions(che1, "");
		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // 3, but one should be short. Only 1123 and 1522 find each inventory

		List<WorkInstruction> wiListAfterScan = facility.getWorkInstructions(che1, "D402");
		Integer wiCountAfterScan = wiListAfterScan.size();
		Double posOf402 = locationD402.getPosAlongPath();
		Double posOf403 = locationD403.getPosAlongPath();
		Assert.assertTrue(posOf402 > posOf403);

		// If DEV-477 route-wrap is in effect, both are there, but the 402 item is first. We still get the baychange between
		// If DEV-477 is not in effect, 402 item is still first, and 403 item is not in the list. 
		Assert.assertEquals((Integer) 3, wiCountAfterScan); 
		// See which work instruction is which
		WorkInstruction wi1 = wiListAfterScan.get(0);
		Assert.assertNotNull(wi1);
		String wiLoc = wi1.getPickInstruction(); // this is the denormalized position on the work instruction. Should have the alias, and not F1.A2.B2.T1
		Assert.assertEquals("D402", wiLoc);
		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertTrue(wi2.amIHouseKeepingWi());

		// New from v4. Test our work instruction summarizer
		List<WiSetSummary> summaries = new WorkService().workSummary(che1.getPersistentId().toString(), facility.getPersistentId()
			.toString());

		// as this test, this facility only set up this one che, there should be only one wi set. But we have 3. How?
		Assert.assertEquals(1, summaries.size());

		// getAny should get the one. Call it somewhat as the UI would. Get a time, then query again with that time.
		WiSetSummary theSummary = summaries.get(0);
		// So, how many shorts, how many active? None complete yet.
		int actives = theSummary.getActiveCount();
		int shorts = theSummary.getShortCount();
		int completes = theSummary.getCompleteCount();
		Assert.assertEquals(0, completes);
		Assert.assertEquals(3, actives);
		Assert.assertEquals(1, shorts);
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	@Test
	public final void testPickViaChe() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		LocationABC locationD403 = (LocationABC) facility.findSubLocationById("D403");
		LocationABC locationD402 = (LocationABC) facility.findSubLocationById("D402");
		LocationABC locationD502 = (LocationABC) facility.findSubLocationById("D502");
		LocationABC locationD503 = (LocationABC) facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			}
		}
		Assert.assertEquals(2, itemLocations.size());

		// Turn off housekeeping work instructions so as to not confuse the counts
		HousekeepingInjector.turnOffHK();
		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");
		picker.setupContainer("12345", "1");
		picker.start("D403", 5000, 1000);
		HousekeepingInjector.restoreHKDefaults();

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());

		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());

		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 1000);
		picker.logout();
	}
	
	@Test
	public final void testCheProcess1() throws IOException {
		// Test cases:
		// 1) If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)
		// 2) A happy-day pick startup. No housekeeping jobs.
		// Case 3: A happy-day short, with one short-ahead");
		// Case 4: Short and cancel leave you on the same job");
		// Case 5: Inappropriate location scan, then normal button press works");

		Facility facility = setUpSimpleNoSlotFacility();
		setUpSmallInventoryAndOrders(facility);
		
		HousekeepingInjector.turnOffHK();
		
		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");
		
		// This brief case covers and allows retirement of CheSimulationTest.java
		LOGGER.info ("Case 1: If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)");
		picker.setupContainer("9x9x9", "1"); // unknown container
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK,1000);
		Assert.assertEquals(0, picker.countActiveJobs());
		
		// Back to our main test
		LOGGER.info ("Case 2: A happy-day pick startup. No housekeeping jobs.");
		picker.setup();
		picker.setupContainer("12345", "1");
		picker.setupContainer("11111", "2");
		picker.start("D303", 5000, 3000);
		HousekeepingInjector.restoreHKDefaults();
		
		Assert.assertEquals(7, picker.countRemainingJobs());		
		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();
		assertWIColor(wi, che1);
		
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		
		
		
		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(6, picker.countRemainingJobs());

		LOGGER.info ("Case 3: A happy-day short, with one short-ahead");
		wi = picker.nextActiveWi();
		// the third job is for 1522, which happens to be the one item going to both orders. So it should short-ahead
		Assert.assertEquals("1522", wi.getItemId());
		button = picker.buttonFor(wi);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK,1000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM,1000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Would be 5, but with one short ahead it is 4.

		LOGGER.info ("Case 4: Short and cancel leave you on the same job");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK,1000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM,1000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Still 4.
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2); // same work instruction still on

		LOGGER.info ("Case 5: Inappropriate location scan, then normal button press works");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK,1000); // still on pick state, although with an error message
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(3, picker.countRemainingJobs()); 
	}
	
	@Test
	public final void testRouteWrap() throws IOException {
		// Test cases:

		Facility facility = setUpSimpleNoSlotFacility();
		setUpSmallInventoryAndOrders(facility);
		
		// HousekeepingInjector.turnOffHK(); // leave housekeeping on for this test, because we need to test removing the bay change just prior to the wrap point.
		
		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");
		
		LOGGER.info ("Case 1: Scan on near the end of the route. Only 3 of 7 jobs left. (There are 3 housekeeping). So, with route-wrap, 10 jobs");
		picker.setup();
		picker.setupContainer("12345", "1");
		picker.setupContainer("11111", "2");
		// Taking more than 3 seconds for the recompute and wrap. 
		picker.start("D301", 5000, 3000);
		HousekeepingInjector.restoreHKDefaults();
		
		// WARNING: whenever getting work instructions via the picker, it is in the context that the site controller has. For example
		// the itemMaster field is null.
		Assert.assertEquals(10, picker.countRemainingJobs());	
		List<WorkInstruction> theWiList = picker.getAllPicksList();
		logWiList(theWiList);
		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();
		assertWIColor(wi, che1);
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D301", wi.getPickInstruction());
		
		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(9, picker.countRemainingJobs());

		LOGGER.info ("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(8, picker.countRemainingJobs());
		// last job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		// Here is the end of it
		//picker.waitForCheState(CheStateEnum.DO_PICK,1000);
		Assert.assertEquals(7, picker.countRemainingJobs());
		
	}
	
	private void assertWIColor(WorkInstruction wi, Che che) {
		List<LedCmdGroup> cmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wi.getLedCmdStream());
		Assert.assertEquals(1, cmdGroups.size());
		ColorEnum wiColor = cmdGroups.get(0).getLedSampleList().get(0).getColor();
		ColorEnum cheColor = che.getColor();
		Assert.assertEquals(cheColor, wiColor);

	}

}
