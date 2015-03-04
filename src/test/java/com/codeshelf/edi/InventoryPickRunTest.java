/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
// domain objects needed
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.service.PropertyService;

/**
 *
 *
 */
public class InventoryPickRunTest extends EdiTestABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(InventoryPickRunTest.class);

	@SuppressWarnings({ "unused" })
	private Facility setUpSimpleNonSlottedFacility(String inOrganizationName) {
		// Besides basic crossbatch functionality, with this facility we want to test housekeeping WIs for
		// 1) same position on cart
		// 2) Bay done/change bay
		// 3) aisle done/change aisle

		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,120,,\r\n" //
				+ "Tier,T3,,0,80,240,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,120,,\r\n" //
				+ "Tier,T3,,0,80,240,,\r\n" //
				+ "Aisle,A2,,,,,tierB1S1Side,12.85,55.45,X,120\r\n" //
				+ "Bay,B1,244,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,120,,\r\n" //
				+ "Tier,T3,,0,80,240,,\r\n" //
				+ "Bay,B2,244,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,120,,\r\n" //
				+ "Tier,T3,,0,80,240,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		String fName = "F-" + inOrganizationName;
		Facility facility = Facility.createFacility(getDefaultTenant(),fName, "TEST", Point.getZeroPoint());

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisles
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 10.85, 48.45, 22.0, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T2, D-26\r\n" // D-26 mainly because that is the scan location we use for good eggs tests.
				+ "A1.B1.T1, D-27\r\n" //
				+ "A1.B2.T2, D-28\r\n" //
				+ "A1.B2.T1, D-29\r\n" //
				+ "A2.B1.T2, D-30\r\n" //
				+ "A2.B1.T1, D-31\r\n" //
				+ "A2.B2.T2, D-32\r\n" //
				+ "A2.B2.T1, D-33\r\n" //
				+ "A1.B1.T3, D-71\r\n" //
				+ "A1.B2.T3, D-72\r\n" //
				+ "A2.B1.T3, D-73\r\n" //
				+ "A2.B2.T3, D-74\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		CodeshelfNetwork network = facility.getNetworks().get(0);

		Che che1 = network.getChe("CHE1");
		che1.setColor(ColorEnum.GREEN);
		Che che2 = network.getChe("CHE2");
		che1.setColor(ColorEnum.MAGENTA);

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		LedController controller4 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000014"));
		Tier tierA1B1T2 = (Tier) facility.findSubLocationById("D-26");
		Tier tierA1B1T1 = (Tier) facility.findSubLocationById("D-27");
		Tier tierA2B1T2 = (Tier) facility.findSubLocationById("D-30");
		Tier tierA2B1T1 = (Tier) facility.findSubLocationById("D-31");
		String channel1Str = "1";

		tierA1B1T2.setControllerChannel(controller1.getPersistentId().toString(), channel1Str, "aisle");// ALL_TIERS_IN_AISLE is private to tier
		tierA1B1T1.setControllerChannel(controller2.getPersistentId().toString(), channel1Str, "aisle");
		tierA2B1T2.setControllerChannel(controller3.getPersistentId().toString(), channel1Str, "aisle");
		tierA2B1T1.setControllerChannel(controller4.getPersistentId().toString(), channel1Str, "aisle");

		return facility;
	}

	private void readOrdersForA1(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// SKU 1123 needed for 12000
		// SKU 1123 needed for 12010
		// Each product needed for 12345

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n,USF314,TARGET,12000,12000,1831,Shorts-xl,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1830,Shorts-large,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1123,8 oz Bowl Lids,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1124,12 oz Bowl Lids,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1125,16 oz Bowl Lids,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1126,24 oz Bowl Lids,1,each"
				+ "\r\n,USF314,TARGET,12000,12000,1522,Tshirt-small,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03"
				+ "\r\n,USF314,TARGET,12000,12000,1523,Tshirt-med,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03"
				+ "\r\n,USF314,TARGET,12000,12000,1524,Tshirt-large,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03"
				+ "\r\n,USF314,TARGET,12000,12000,1525,Tshirt-xl,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03" + "\n";

		byte[] csvArray2 = csvString2.getBytes();
		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}
	
	private void readOrdersForBayDistance(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// SKU 1123 needed for 12000
		// SKU 1123 needed for 12010
		// Each product needed for 12345

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDetailId,locationId"
				+ "\r\n,USF314,TARGET,12000,12000,1831,Shorts-xl,1,each,101,D-27"		// Should get work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1830,Shorts-large,1,each,102"			// Should not get work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1123,8 oz Bowl Lids,1,each,103, F-67" // Should not get a work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1124,12 oz Bowl Lids,1,each,104"		// Should get work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1125,16 oz Bowl Lids,1,each,105"
				+ "\r\n,USF314,TARGET,12000,12000,1126,24 oz Bowl Lids,1,each,106" + "\n";

		byte[] csvArray2 = csvString2.getBytes();
		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}
	
	private void readOrdersForWorkSequence(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// SKU 1123 needed for 12000
		// SKU 1123 needed for 12010
		// Each product needed for 12345

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDetailId,locationId,workSequence"
				+ "\r\n,USF314,TARGET,12000,12000,1831,Shorts-xl,1,each,101,D-27,1" // no item location, has preferred, has sequence, should get work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1830,Shorts-large,1,each,102,D-27" // no item location, no workSequence, should not get work instruction
				+ "\r\n,USF314,TARGET,12000,12000,1123,8 oz Bowl Lids,1,each,103, F-67, 2" // has item location, has bad preferred, has sequence, should get wi
				+ "\r\n,USF314,TARGET,12000,12000,1124,12 oz Bowl Lids,1,each,104"	// item has location, no workSequence, no preferred, should not get wi
				+ "\r\n,USF314,TARGET,12000,12000,1125,16 oz Bowl Lids,1,each,105"
				+ "\r\n,USF314,TARGET,12000,12000,1126,24 oz Bowl Lids,1,each,106" + "\n";

		byte[] csvArray2 = csvString2.getBytes();
		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}

	private void readInventoryWithoutTop(Facility inFacility) throws IOException {
		// A1.B1.T2 is D-26.
		// In D-26, left to right are SKUs 1124,1126,1123,1125

		// A1.B2.T2 is D-28
		// In D-28, left to right are SKUs 1522,1525,1523,1524

		// A1.B1.T1 is D-27.
		// In D-27, left to right are SKUs  1831, 1830

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D-26,8 oz Bowl Lids,6,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D-26,12 oz Bowl Lids,0,ea,6/25/14 12:00,8\r\n" //
				+ "1125,D-26,16 oz Bowl Lids,,each,6/25/14 12:00,185\r\n" //
				+ "1126,D-26,24 oz Bowl Lids,80,each,6/25/14 12:00,55\r\n" //
				+ "1522,D-28,Tshirt-small,,ea,,3\r\n" //
				+ "1523,D-28,Tshirt-med,,ea,,190\r\n" //
				+ "1524,D-28,Tshirt-large,0,ea,6/25/14 12:00,214\r\n" //
				+ "1525,D-28,Tshirt-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1831,D-27,Shorts-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1830,D-27,Shorts-large,1,each,6/25/14 12:00,182\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, inFacility, ediProcessTime);
	}

	private void readInventoryWithTop(Facility inFacility) throws IOException {
		// A1.B1.T2 is D-26.
		// In D-26, left to right are SKUs 1124,1126,1123,1125

		// A1.B2.T2 is D-28
		// In D-28, left to right are SKUs 1522,1525

		// A1.B1.T3 is D-71
		// In D-71, left to right are SKUs 1523

		// A1.B2.T3 is D-72
		// In D-72, left to right are SKUs 1524

		// A1.B1.T1 is D-27.
		// In D-27, left to right are SKUs  1831, 1830

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D-26,8 oz Bowl Lids,6,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D-26,12 oz Bowl Lids,0,ea,6/25/14 12:00,8\r\n" //
				+ "1125,D-26,16 oz Bowl Lids,,each,6/25/14 12:00,185\r\n" //
				+ "1126,D-26,24 oz Bowl Lids,80,each,6/25/14 12:00,55\r\n" //
				+ "1522,D-28,Tshirt-small,,ea,,3\r\n" //
				+ "1523,D-71,Tshirt-med,,ea,,190\r\n" //
				+ "1524,D-72,Tshirt-large,0,ea,6/25/14 12:00,214\r\n" //
				+ "1525,D-28,Tshirt-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1831,D-27,Shorts-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1830,D-27,Shorts-large,1,each,6/25/14 12:00,182\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, inFacility, ediProcessTime);
	}
	
	private void readInventoryBayDistance(Facility inFacility) throws IOException {
		// A1.B1.T2 is D-26.
		// In D-26, left to right are SKUs 1124,1126,1123,1125

		// A1.B2.T2 is D-28
		// In D-28, left to right are SKUs 1522,1525

		// A1.B1.T3 is D-71
		// In D-71, left to right are SKUs 1523

		// A1.B2.T3 is D-72
		// In D-72, left to right are SKUs 1524

		// A1.B1.T1 is D-27.
		// In D-27, left to right are SKUs  1831, 1830

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,,8 oz Bowl Lids,6,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D-26,12 oz Bowl Lids,0,ea,6/25/14 12:00,8\r\n" //
				+ "1125,D-26,16 oz Bowl Lids,,each,6/25/14 12:00,185\r\n" //
				+ "1126,D-26,24 oz Bowl Lids,80,each,6/25/14 12:00,55\r\n" //
				+ "1522,D-28,Tshirt-small,,ea,,3\r\n" //
				+ "1523,D-71,Tshirt-med,,ea,,190\r\n" //
				+ "1524,D-72,Tshirt-large,0,ea,6/25/14 12:00,214\r\n" //
				+ "1525,D-28,Tshirt-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1831,,Shorts-xl,1,each,6/25/14 12:00,82\r\n"//
				+ "1830,,Shorts-large,1,each,6/25/14 12:00,182\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, inFacility, ediProcessTime);
	}

	@SuppressWarnings("unused")
	@Test
	public final void testSequenceAlongTierWithoutTop() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNonSlottedFacility("InvP_01");
		Assert.assertNotNull(facility);

		Tier tierA1B1T2 = (Tier) facility.findSubLocationById("D-26"); // just using alias a little.
		Tier tierA1B1T1 = (Tier) facility.findSubLocationById("D-27"); // just using alias a little.
		Assert.assertNotNull(tierA1B1T1.getLedController());
		Tier tierA1B2T1 = (Tier) facility.findSubLocationById("A1.B2.T1");
		Assert.assertNotNull(tierA1B1T1.getLedController());
		// Check the path direction
		String posA1B1 = tierA1B1T1.getPosAlongPathui();
		String posA2B1 = tierA1B2T1.getPosAlongPathui();
		Assert.assertTrue(tierA1B2T1.getPosAlongPath() > tierA1B1T1.getPosAlongPath());

		// Inventory
		readInventoryWithoutTop(facility);
		List<Item> aList = tierA1B1T2.getInventoryInWorkingOrder();
		Assert.assertTrue(aList.size() == 4);
		logItemList(aList);
		Item firstOnPath = aList.get(0);
		String firstSku = firstOnPath.getItemMasterId();
		Assert.assertEquals("1124", firstSku);

		// Orders
		readOrdersForA1(facility);

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		LOGGER.info("Set up CHE for order 12000. Should get 4 jobs on B1T2, the two on B1T1, and four on B2T2");
		List<WorkInstruction> wiList = startWorkFromBeginning(facility, "CHE1", "12000");

		Integer theSize = wiList.size();
		Assert.assertEquals((Integer) 10, theSize);
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi5 = wiList.get(4);
		WorkInstruction wi10 = wiList.get(9);
		Assert.assertEquals("1124", wi1.getItemId());
		Assert.assertEquals("1831", wi5.getItemId());
		Assert.assertEquals("1524", wi10.getItemId());

		propertyService.restoreHKDefaults(facility);

		// Need more cases for BayDistanceTopLast.

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void testSequenceAlongTierWithTop() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNonSlottedFacility("InvP_02");
		Assert.assertNotNull(facility);
		Tier tierA1B1T2 = (Tier) facility.findSubLocationById("D-26"); // just using alias a little.
		Tier tierA1B1T1 = (Tier) facility.findSubLocationById("D-27"); // just using alias a little.
		Assert.assertNotNull(tierA1B1T1.getLedController());
		Tier tierA1B2T1 = (Tier) facility.findSubLocationById("A1.B2.T1");
		Assert.assertNotNull(tierA1B1T1.getLedController());

		// Check the path direction
		String posA1B1 = tierA1B1T1.getPosAlongPathui();
		String posA2B1 = tierA1B2T1.getPosAlongPathui();
		Assert.assertTrue(tierA1B2T1.getPosAlongPath() > tierA1B1T1.getPosAlongPath());

		// Inventory
		readInventoryWithTop(facility);
		List<Item> aList = tierA1B1T2.getInventoryInWorkingOrder();
		Assert.assertTrue(aList.size() == 4);
		logItemList(aList);
		Item firstOnPath = aList.get(0);
		String firstSku = firstOnPath.getItemMasterId();
		Assert.assertEquals("1124", firstSku);

		// Orders
		readOrdersForA1(facility);

		// Just check a UI field. Basically looking for NPE
		OrderHeader order = facility.getOrderHeader("12000");
		Assert.assertNotNull(order);
		for (OrderDetail detail : order.getOrderDetails()) {
			String theUiField = detail.getWillProduceWiUi();
		}

		// Now ready to run the cart
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
LOGGER.info("Set up CHE for order 12000. Should get 4 jobs on B1T2, the two on B1T1, and four on B2T2");
		List<WorkInstruction> wiList = startWorkFromBeginning(facility, "CHE1", "12000");
		Integer theSize = wiList.size();
		Assert.assertEquals((Integer) 10, theSize);
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi5 = wiList.get(4);
		WorkInstruction wi10 = wiList.get(9);
		Assert.assertEquals("1523", wi1.getItemId());
		Assert.assertEquals("1125", wi5.getItemId());
		Assert.assertEquals("1525", wi10.getItemId());

		propertyService.restoreHKDefaults(facility);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testimmediateShorts() throws IOException {
		// generation of immediateShort
		// cleanup of immediate short
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNonSlottedFacility("InvP_03");
		Assert.assertNotNull(facility);

		// Inventory
		readInventoryWithTop(facility);

		// Orders
		readOrdersForA1(facility);

		// Delete two of the items, which will cause immediate short upon cart setup
		ItemMaster master1123 = facility.getItemMaster("1123");
		ItemMaster master1124 = facility.getItemMaster("1124");
		Item item1123 = master1123.getItemsOfUom("EA").get(0);
		Item item1124 = master1124.getItemsOfUom("EA").get(0);
		Assert.assertNotNull(item1123);
		Assert.assertNotNull(item1124);
		Item.DAO.delete(item1123);
		Item.DAO.delete(item1124);

		// Interesting and important. If this commit is not done here, the cart setup will still find undeleted items 1123 and 1124.
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		//

		// Now ready to run the cart
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		LOGGER.info("Set up CHE for order 12000.");
		WorkList workList = workService.setUpCheContainerFromString(theChe, "12000");
		Integer theSize = workList.getInstructions().size();
		Assert.assertEquals((Integer) 8, theSize); // Would be 10 with 1123 and 1124
		// Let's find and count the immediate shorts
		theSize = workList.getDetails().size();
		Assert.assertEquals((Integer) 2, theSize); // Infer 2 shorts in there

		// Set up the CHE again. DEV-609. This should delete the previous 2 immediate shorts, then make 2 new ones
		workList = workService.setUpCheContainerFromString(theChe, "12000");
		theSize = workList.getInstructions().size();
		Assert.assertEquals((Integer) 8, theSize); // Would be 10 with 1123 and 1124
		// Let's find and count the immediate shorts
		theSize = workList.getDetails().size();
		Assert.assertEquals((Integer) 2, theSize); // Before DEV-609, this had 12

		propertyService.restoreHKDefaults(facility);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testLocationBasedPick() throws IOException {
		// This is a very complete test of location-based pick. Do not need the integration test with site controller because
		// Location-based pick is built via:
		// - Creation of inventory during orders file read, then
		// - Selection of the inventory items during computeWorkInstructions
		// Once that is done, site controller just implements the work instructions that were made.

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNonSlottedFacility("InvLocP_01");
		Assert.assertNotNull(facility);

		LOGGER.info("1: Set LOCAPICK = true.  Leave EACHMULT = false");
		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.LOCAPICK);
		if (theProperty != null) {
			theProperty.setValue(true);
			PropertyDao.getInstance().store(theProperty);
		}
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("2: Read the orders file, which has some preferred locations");
		// This facility has aliases D26 ->D33 and D71->D74
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D-27,61"
				+ "\r\n10,10,10.2,SKU0002,16 oz Clear Cup,2,CS,,pick,D-28,43"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D-21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshell,2,EA,,pick,D-71,";

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = createOrderImporter();
		importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

		this.getTenantPersistenceService().commitTransaction();
		// This should give us inventory at D-27, D-28, and D-71, but not at D-21

		LOGGER.info("3: Set up CHE for orders 10 and 11. Should get 3 jobs");
		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.DAO.reload(facility);
		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
List<WorkInstruction> wiList = startWorkFromBeginning(facility, "CHE1", "10,11");
		logWiList(wiList);
		Integer theSize = wiList.size();
		Assert.assertEquals((Integer) 3, theSize);
		LOGGER.info("4: Success! Three jobs from location based pick. SKU0003 did not get one made.");

		LOGGER.info("5: Let's read an inventory file creating SKU0003 in a different place.");

		String csvString2 = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "SKU0003,D-33,Spoon 6in.,80,Cs,6/25/14 12:00,\r\n"; //

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer2 = createInventoryImporter();
		importer2.importSlottedInventoryFromCsvStream(new StringReader(csvString2), facility, ediProcessTime2);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("6: Set up CHE again for orders 10 and 11. Now should get 4 jobs");
		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.DAO.reload(facility);
		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		wiList = startWorkFromBeginning(facility, "CHE1", "10,11");
		logWiList(wiList);
		theSize = wiList.size();
		Assert.assertEquals((Integer) 4, theSize);

		LOGGER.info("7: Let's move SKU0004 to D-74. The order preferredLocation is still D-71");
		String csvString3 = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "SKU0004,D-74,9 Three Compartment Unbleached Clamshell,,Ea,6/25/14 12:00,\r\n"; //

		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer3 = createInventoryImporter();
		importer3.importSlottedInventoryFromCsvStream(new StringReader(csvString3), facility, ediProcessTime3);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("8: Set up CHE again for orders 10 and 11. Should still get 4 jobs");
		LOGGER.info("And a logger.warn saying: Item not found at D-71. Substituted item at D-74");
		this.getTenantPersistenceService().beginTransaction();

		facility = Facility.DAO.reload(facility);
		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		wiList = startWorkFromBeginning(facility, "CHE1", "10,11");
		logWiList(wiList);
		theSize = wiList.size();
		Assert.assertEquals((Integer) 4, theSize);
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void testBayDistance() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		OrderDetail.workService = workService;

		
		Facility facility = setUpSimpleNonSlottedFacility("InvP_01");
		Assert.assertNotNull(facility);
		
		LOGGER.info("0. Set WORKSEQR = BayDistance.");
		PropertyService.getInstance().changePropertyValue(facility, DomainObjectProperty.WORKSEQR, "BayDistance");
		
		// Inventory
		readInventoryBayDistance(facility);

		// Orders
		readOrdersForBayDistance(facility);
		
		OrderHeader orderHeader = facility.getOrderHeader("12000");
		Assert.assertNotNull(orderHeader);
		
		LOGGER.info("1. OrderDetail 101 does not have an inventory location, does have a good preferred location");
		OrderDetail orderDetail = orderHeader.getOrderDetail("101");
		Assert.assertNotNull(orderDetail);
		Assert.assertTrue(orderDetail.willProduceWi());
		
		LOGGER.info("2. OrderDetail 102 does not have an inventory location, does not have preferred location");
		OrderDetail orderDetail2 = orderHeader.getOrderDetail("102");
		Assert.assertNotNull(orderDetail2);
	
		Assert.assertFalse(orderDetail2.willProduceWi());
		
		LOGGER.info("3. OrderDetail 103 does not have an inventory location, has bad preferred location");
		OrderDetail orderDetail3 = orderHeader.getOrderDetail("103");
		Assert.assertNotNull(orderDetail3);
		Assert.assertFalse(orderDetail3.willProduceWi());
		
		LOGGER.info("4. OrderDetail 104 does have an inventory location, has bad preferred location");
		OrderDetail orderDetail4 = orderHeader.getOrderDetail("104");
		Assert.assertNotNull(orderDetail4);
		Assert.assertTrue(orderDetail4.willProduceWi());
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void testWorkSequence() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNonSlottedFacility("InvP_01");
		Assert.assertNotNull(facility);
		
		LOGGER.info("1: Set WORKSEQR = WorkSequence.");
		PropertyService.getInstance().changePropertyValue(facility, DomainObjectProperty.WORKSEQR, "WorkSequence");
		
		// Inventory
		readInventoryBayDistance(facility);

		// Orders
		readOrdersForWorkSequence(facility);
		
		OrderHeader orderHeader = facility.getOrderHeader("12000");
		Assert.assertNotNull(orderHeader);
		
		LOGGER.info("1. OrderDetail 101 does not have an inventory location, does have a good preferred location, has sequence");
		OrderDetail orderDetail = orderHeader.getOrderDetail("101");
		Assert.assertNotNull(orderDetail);
		Assert.assertTrue(orderDetail.willProduceWi());
		
		LOGGER.info("2. OrderDetail 102 does not have an inventory location, does not have preferred location, no sequence");
		OrderDetail orderDetail2 = orderHeader.getOrderDetail("102");
		Assert.assertNotNull(orderDetail2);
	
		Assert.assertFalse(orderDetail2.willProduceWi());
		
		LOGGER.info("3. OrderDetail 103 does not have an inventory location, has bad preferred location, has sequence");
		OrderDetail orderDetail3 = orderHeader.getOrderDetail("103");
		Assert.assertNotNull(orderDetail3);
		Assert.assertTrue(orderDetail3.willProduceWi());
		
		LOGGER.info("4. OrderDetail 104 does have an inventory location, has bad preferred location, no sequence");
		OrderDetail orderDetail4 = orderHeader.getOrderDetail("104");
		Assert.assertNotNull(orderDetail4);
		Assert.assertTrue(orderDetail4.willProduceWi());
		
		
		this.getTenantPersistenceService().commitTransaction();
	}

}
