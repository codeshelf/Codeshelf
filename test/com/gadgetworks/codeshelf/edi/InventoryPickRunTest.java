/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
// domain objects needed
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerType;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;

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

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisles
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 10.85, 48.45, 22.0, 48.45);

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

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(organization,nName);
		//Che che = 
		network.createChe("CHE1", new NetGuid("0x00000001"));
		network.createChe("CHE2", new NetGuid("0x00000002"));

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
				+ "\r\n,USF314,TARGET,12000,12000,1522,Tshirt-small,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,TARGET,12000,12000,1523,Tshirt-med,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,TARGET,12000,12000,1524,Tshirt-large,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,TARGET,12000,12000,1525,Tshirt-xl,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\n";

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

	@Test
	public final void testSequenceAlongTierWithoutTop() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

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

		// Now ready to run the cart
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		LOGGER.info("Set up CHE for order 12000. Should get 4 jobs on B1T2, the two on B1T1, and four on B2T2");
		HousekeepingInjector.turnOffHK();
		Facility.setSequencerType(WorkInstructionSequencerType.BayDistance);
		facility.setUpCheContainerFromString(theChe, "12000");

		List<WorkInstruction> wiList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.
		logWiList(wiList);
		Integer theSize = wiList.size();
		Assert.assertEquals((Integer) 10, theSize);
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi5 = wiList.get(4);
		WorkInstruction wi10 = wiList.get(9);
		Assert.assertEquals("1124", wi1.getItemId());
		Assert.assertEquals("1831", wi5.getItemId());
		Assert.assertEquals("1524", wi10.getItemId());

		// Check again with the Accu sequencer
		// ebeans bug? Cannot immediately setup the CHE again. Try different CHE
		Che theChe2 = theNetwork.getChe("CHE2");

		Facility.setSequencerType(WorkInstructionSequencerType.BayDistanceTopLast);
		facility.setUpCheContainerFromString(theChe2, "12000");
		List<WorkInstruction> wiList2 = facility.getWorkInstructions(theChe2, ""); // This returns them in working order.
		logWiList(wiList2);
		wi1 = wiList2.get(0);
		wi5 = wiList2.get(4);
		wi10 = wiList2.get(9);
		// All wrong!
		Assert.assertEquals("1124", wi1.getItemId());
		Assert.assertEquals("1831", wi5.getItemId());
		Assert.assertEquals("1524", wi10.getItemId());

		HousekeepingInjector.restoreHKDefaults();

		// Need more cases for BayDistanceTopLast.

		this.getPersistenceService().endTenantTransaction();
	}
	
	@Test
	public final void testSequenceAlongTierWithTop() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

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

		LOGGER.info("Set up CHE for order 12000. Should get 4 jobs on B1T2, the two on B1T1, and four on B2T2");
		HousekeepingInjector.turnOffHK();
		Facility.setSequencerType(WorkInstructionSequencerType.BayDistance);
		facility.setUpCheContainerFromString(theChe, "12000");

		List<WorkInstruction> wiList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.
		logWiList(wiList);
		Integer theSize = wiList.size();
		Assert.assertEquals((Integer) 10, theSize);
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi5 = wiList.get(4);
		WorkInstruction wi10 = wiList.get(9);
		Assert.assertEquals("1523", wi1.getItemId());
		Assert.assertEquals("1125", wi5.getItemId());
		Assert.assertEquals("1525", wi10.getItemId());

		// Check again with the Accu sequencer
		// ebeans bug? Cannot immediately setup the CHE again. Try different CHE
		Che theChe2 = theNetwork.getChe("CHE2");

		Facility.setSequencerType(WorkInstructionSequencerType.BayDistanceTopLast);
		facility.setUpCheContainerFromString(theChe2, "12000");
		List<WorkInstruction> wiList2 = facility.getWorkInstructions(theChe2, ""); // This returns them in working order.
		logWiList(wiList2);
		wi1 = wiList2.get(0);
		wi5 = wiList2.get(4);
		wi10 = wiList2.get(9);
		// All wrong!
		Assert.assertEquals("1124", wi1.getItemId());
		Assert.assertEquals("1831", wi5.getItemId());
		Assert.assertEquals("1524", wi10.getItemId());

		HousekeepingInjector.restoreHKDefaults();

		this.getPersistenceService().endTenantTransaction();
	}	

}
