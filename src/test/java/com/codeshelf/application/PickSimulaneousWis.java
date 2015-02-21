/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.EdiTestABC;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.multitenancy.TenantManagerService;

/**
 * @author jon ranstrom
 *
 */
public class PickSimulaneousWis extends EdiTestABC {

	static {
		JvmProperties.load("test");
	}

	@SuppressWarnings({ "unused" })
	private Facility setUpSimpleNoSlotFacility(String inOrganizationName) {
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

		String fName = "F-" + inOrganizationName;
		Facility facility = Facility.createFacility(TenantManagerService.getInstance().getDefaultTenant(), fName, "TEST", Point.getZeroPoint());

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,0,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();

		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = facility.getAisle("A1");// Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = facility.getAisle("A2");//Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(facility);
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

//		facility.getLocations().get("A2")l
		Aisle aisle3 = facility.getAisle("A3");//Aisle.DAO.findByDomainId(facility, "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T1.S1, D301\r\n" //
				+ "A1.B1.T1.S2, D302\r\n" //
				+ "A1.B1.T1.S3, D303\r\n" //
				+ "A1.B1.T1.S4, D304\r\n" //
				+ "A2.B1.T1, D402\r\n" //
				+ "A2.B2.T1, D403\r\n"//
				+ "A3.B1.T1, D502\r\n" //
				+ "A3.B2.T1, D503\r\n";//

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		CodeshelfNetwork network = facility.getNetworks().get(0);
		Che che1 = network.createChe("CHE3", new NetGuid("0x00000001"));
		Che che2 = network.createChe("CHE4", new NetGuid("0x00000002"));

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		Location tier = facility.findSubLocationById("A1.B1.T1");
		Short channel1 = 1;
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A2.B2.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A3.B1.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A3.B2.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);

		return facility;

	}

	@SuppressWarnings("unused")
	@Test
	public final void testPick() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility("PK01");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);

		/*  From CD_0043  applied to each pick in aisle A1
		Order 1, with two order details: A and B.
		Odrer 2, with two order details: B and C.
		Order 3, with two order details B and D.
		Order 4, with order details for item E.
		Order 5 with order details for item E.
		The item order on the path is A, B, C, D, E.
		A = 1123. B=1144. C=1155. D=1493. D=1522.
		 */
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1144,D402,20 oz cups -PLA Compostable,10,EA,6/25/14 12:00,108\r\n" //
				+ "1155,D402,12 oz Bowl -PLA Compostable,10,EA,6/25/14 12:00,95\r\n" //
				+ "1493,D402,PARK RANGER Doll,20,EA,6/25/14 12:00,66\r\n" //
				+ "1522,D402,SJJ BPP,10,each,6/25/14 12:00,30\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12001,12001,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12001,12001,1144,20 oz cups -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12002,12002,1144,20 oz cups -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12002,12002,1155,12 oz Bowl -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12003,12003,1144,20 oz cups -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12003,12003,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12004,12004,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12005,12005,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);

		// Turn off housekeeping work instructions so as to not confuse the counts
		mPropertyService.turnOffHK(facility);
		// Set up a cart for the five orders, which will generate work instructions. (Tweak the order. 12001/1123 should be the first WI by the path.
		mWorkService.setUpCheContainerFromString(theChe, "12004,12005,12001,12002,12003");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		theChe = Che.DAO.reload(theChe);

		List<WorkInstruction> aList = mWorkService.getWorkInstructions(theChe, "");


		int wiCount = aList.size();
		Assert.assertEquals(8, wiCount); // 8 work instructions. But 2,3,4 in same group and 7,8 in same group.

		// All work instructions are for items in D402. So all 8 will have posAlongPath >= to the D402 value.
		// Therefore, all 8 will be in the result of starting from D402
		List<WorkInstruction> wiListAfterScan = mWorkService.getWorkInstructions(theChe, "D402");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		mPropertyService.restoreHKDefaults(facility);
		this.getTenantPersistenceService().commitTransaction();

		Integer wiCountAfterScan = wiListAfterScan.size();
		Assert.assertEquals((Integer) 8, wiCountAfterScan); // all 8 work instructions from D402 should be there.

		// Check the order of the work instructions. What we are really doing is seeing if the the 2nd, 3rd, and 4th WI have group component in the group and sort.
		// Answer: no. Not now anyway. So no simultaneous dispatch.
		this.getTenantPersistenceService().beginTransaction();
		WorkInstruction wi1 = WorkInstruction.DAO.reload(wiListAfterScan.get(0));
		Assert.assertNotNull(wi1);
		String wi1Order = wi1.getOrderId();
		String wi1Item = wi1.getItemId();
		String groupSortStr1 = wi1.getGroupAndSortCode();
		Assert.assertEquals("0001", groupSortStr1);
		Double wi1Pos = wi1.getPosAlongPath();

		WorkInstruction wi2 = WorkInstruction.DAO.reload(wiListAfterScan.get(1));
		Assert.assertNotNull(wi2);
		String groupSortStr2 = wi2.getGroupAndSortCode();
		Assert.assertEquals("0002", groupSortStr2);
		Double wi2Pos = wi2.getPosAlongPath();

		Assert.assertTrue(wi2Pos > wi1Pos);

		WorkInstruction wi3 = WorkInstruction.DAO.reload(wiListAfterScan.get(2));
		Assert.assertNotNull(wi3);
		String groupSortStr3 = wi3.getGroupAndSortCode();
		Assert.assertEquals("0003", groupSortStr3);
		Double wi3Pos = wi3.getPosAlongPath();

		WorkInstruction wi4 = WorkInstruction.DAO.reload(wiListAfterScan.get(3));
		Assert.assertNotNull(wi4);
		String groupSortStr4 = wi4.getGroupAndSortCode();
		Assert.assertEquals("0004", groupSortStr4);
		Double wi4Pos = wi4.getPosAlongPath();
		// 2, 3 and 4 for same item, so should be equal.
		Assert.assertEquals(wi2Pos, wi4Pos);

		WorkInstruction wi5 = WorkInstruction.DAO.reload(wiListAfterScan.get(4));
		Assert.assertNotNull(wi5);
		String groupSortStr5 = wi5.getGroupAndSortCode();
		Double wi5Pos = wi5.getPosAlongPath();

		WorkInstruction wi6 = WorkInstruction.DAO.reload(wiListAfterScan.get(5));
		Assert.assertNotNull(wi6);
		String groupSortStr6 = wi6.getGroupAndSortCode();
		Double wi6Pos = wi6.getPosAlongPath();

		WorkInstruction wi7 = WorkInstruction.DAO.reload(wiListAfterScan.get(6));
		Assert.assertNotNull(wi7);
		String groupSortStr7 = wi7.getGroupAndSortCode();
		Assert.assertEquals("0007", groupSortStr7);
		Double wi7Pos = wi7.getPosAlongPath();

		WorkInstruction wi8 = WorkInstruction.DAO.reload(wiListAfterScan.get(7));
		Assert.assertNotNull(wi8);
		String groupSortStr8 = wi8.getGroupAndSortCode();
		Assert.assertEquals("0008", groupSortStr8);
		Double wi8Pos = wi8.getPosAlongPath();

		Assert.assertEquals("1123", wi1Item);
		Assert.assertEquals("12001", wi1Order);

		// Last items picked should be 1522. Two orders, so that is arbitrary. (Until later, when we might care to alternate somewhat to reduce confusion about orders going to same cart slot.
		String wi7Item = wi7.getItemId();
		String wi8Item = wi8.getItemId();
		Assert.assertEquals("1522", wi7Item);
		Assert.assertEquals("1522", wi8Item);
		getTenantPersistenceService().commitTransaction();
	}

}
