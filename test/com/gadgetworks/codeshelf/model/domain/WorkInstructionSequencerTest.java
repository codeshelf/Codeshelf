/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.eaio.uuid.UUID;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.LocationAliasCsvImporter;
import com.gadgetworks.codeshelf.edi.OutboundOrderCsvImporter;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerType;
import com.gadgetworks.flyweight.command.NetGuid;

public class WorkInstructionSequencerTest extends DomainTestABC {
	
	public WorkInstructionSequencerTest() {
	}

	private Facility setUpFacility(String inOrganizationName) {
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
		// There is a single CHE called CHE1

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
//				+ "Bay,B1,230,,12.85,43.45,,\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,0,,\r\n" //
				+ "Tier,T3,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
//				+ "Bay,B2,230,,18.85,43.45,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Tier,T2,,0,80,0,,\r\n" //
				+ "Tier,T3,,0,80,0,,\r\n"; //

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
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		
		// forward path b1 -> b2
		// PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 12.85, 48.45, 22.00, 48.45);
		
		// backward path b2 -> b1
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T2, D102\r\n" //
				+ "A1.B1.T3, D103\r\n" //
				+ "A1.B2, D200\r\n" //
				+ "A1.B2.T1, D201\r\n" //
				+ "A1.B2.T2, D202\r\n" //
				+ "A1.B2.T3, D203\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);
		
		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		Che che = network.createChe("CHE1", new NetGuid("0x00000001"));

		return facility;
	}	
	
	@Test
	public final void testBayDistanceSequencer()  throws IOException{
		
		Facility facility = setUpFacility("FAC-"+new UUID());

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D101,12 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1124,D102,18 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1125,D103,24 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1126,D201,PARK RANGER Doll,2,each,6/25/14 12:00,0\r\n" //
				+ "1127,D202,SJJ BPP,1,each,6/25/14 12:00,00\r\n" //
				+ "1128,D203,SJJ BPP,10,each,6/25/14 12:00,0\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		LocationABC locationD101 = (LocationABC) facility.findSubLocationById("D101");
		LocationABC locationD102 = (LocationABC) facility.findSubLocationById("D102");
		LocationABC locationD103 = (LocationABC) facility.findSubLocationById("D103");
		LocationABC locationD201 = (LocationABC) facility.findSubLocationById("D201");
		LocationABC locationD202 = (LocationABC) facility.findSubLocationById("D202");
		LocationABC locationD203 = (LocationABC) facility.findSubLocationById("D203");

		Item item1123LocD101 = locationD101.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123LocD101);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1125,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1126,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1128,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		// We should have one order with 4 details 
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount);

		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);
		
		// Set up a cart for order 12345, which will generate work instructions
		Facility.setSequencerType(WorkInstructionSequencerType.BayDistance);
		facility.setUpCheContainerFromString(theChe, "12345");
				
		List<WorkInstruction> aList = theChe.getCheWorkInstructions();
		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 4, wiCount);
		
		// paths is expected to be backwards from b2 to b1, see set-up method
		for (WorkInstruction wi : aList) {
			// System.out.println(wi.getLocationId()+": "+wi.getGroupAndSortCode());
			if (wi.getLocationId().endsWith("B2.T1")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0001"));
			}
			else if (wi.getLocationId().endsWith("B2.T3")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0002"));
			}
			else if (wi.getLocationId().endsWith("B1.T1")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0003"));
			}
			else if (wi.getLocationId().endsWith("B1.T3")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0004"));
			}
			else {
				Assert.fail("Invalid WI location: "+wi.getLocation().getLocationId());
			}
		}		
	}
	
	@Test
	public final void testBayDistanceTopLastSequencer()  throws IOException{
		
		Facility facility = setUpFacility("FAC-"+new UUID());

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D101,12 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1124,D102,18 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1125,D103,24 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,0\r\n" //
				+ "1126,D201,PARK RANGER Doll,2,each,6/25/14 12:00,0\r\n" //
				+ "1127,D202,SJJ BPP,1,each,6/25/14 12:00,00\r\n" //
				+ "1128,D203,SJJ BPP,10,each,6/25/14 12:00,0\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		LocationABC locationD101 = (LocationABC) facility.findSubLocationById("D101");
		LocationABC locationD102 = (LocationABC) facility.findSubLocationById("D102");
		LocationABC locationD103 = (LocationABC) facility.findSubLocationById("D103");
		LocationABC locationD201 = (LocationABC) facility.findSubLocationById("D201");
		LocationABC locationD202 = (LocationABC) facility.findSubLocationById("D202");
		LocationABC locationD203 = (LocationABC) facility.findSubLocationById("D203");

		Item item1123LocD101 = locationD101.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123LocD101);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1125,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1126,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1128,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		// We should have one order with 4 details 
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount);

		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);
		
		// Set up a cart for order 12345, which will generate work instructions
		Facility.setSequencerType(WorkInstructionSequencerType.BayDistanceTopLast);
		facility.setUpCheContainerFromString(theChe, "12345");
				
		List<WorkInstruction> aList = theChe.getCheWorkInstructions();
		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 4, wiCount);
		
		// paths is expected to be backwards from b2 to b1, see set-up method
		for (WorkInstruction wi : aList) {
			System.out.println(wi.getLocationId()+": "+wi.getGroupAndSortCode());
			if (wi.getLocationId().endsWith("B2.T3")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0001"));
			}
			else if (wi.getLocationId().endsWith("B1.T3")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0002"));
			}
			else if (wi.getLocationId().endsWith("B2.T1")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0003"));
			}
			else if (wi.getLocationId().endsWith("B1.T1")) {
				Assert.assertTrue(wi.getGroupAndSortCode().equals("0004"));
			}
			else {
				Assert.fail("Invalid WI location: "+wi.getLocation().getLocationId());
			}
		}		
	}
}
