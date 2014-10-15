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

import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;

/**
 * 
 * 
 */
public class LocationDeleteTest extends EdiTestABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationDeleteTest.class);
	
	private final boolean LARGER_FACILITY = true;
	private final boolean SMALLER_FACILITY = true;

	@SuppressWarnings({ "unused" })
	private Facility setUpSimpleSlottedFacility(String inOrganizationName, boolean inWhichFacility) {
		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		if (inWhichFacility == LARGER_FACILITY)
			readStandardAisleFile(facility);
		else 
			readSmallerAisleFile(facility);
	
		// Get the aisles
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 10.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T2.S5,D-1\r\n" //
				+ "A1.B1.T2.S4,D-2\r\n" //
				+ "A1.B1.T2.S3, D-3\r\n" //
				+ "A1.B1.T2.S2, D-4\r\n" //
				+ "A1.B1.T2.S1, D-5\r\n" //
				+ "A1.B1.T1.S5, D-6\r\n" //
				+ "A1.B1.T1.S4, D-7\r\n" //
				+ "A1.B1.T1.S3, D-8\r\n" //
				+ "A1.B1.T1.S2, D-9\r\n" //
				+ "A1.B2.T1.S1, D-10\r\n" //
				+ "A1.B2.T2.S5, D-21\r\n" //
				+ "A1.B2.T2.S4, D-22\r\n" //
				+ "A1.B2.T2.S3, D-23\r\n" //
				+ "A1.B2.T2.S2, D-24\r\n" //
				+ "A1.B2.T2.S1, D-25\r\n" //
				+ "A1.B2.T1.S5, D-26\r\n" //
				+ "A1.B2.T1.S4, D-27\r\n" //
				+ "A1.B2.T1.S3, D-28\r\n" //
				+ "A1.B2.T1.S2, D-29\r\n" //
				+ "A1.B2.T1.S1, D-30\r\n" //
				+ "A2.B1.T2.S5, D-11\r\n" //
				+ "A2.B1.T2.S4, D-12\r\n" //
				+ "A2.B1.T2.S3, D-13\r\n" //
				+ "A2.B1.T2.S2, D-14\r\n" //
				+ "A2.B1.T2.S1, D-15\r\n" //
				+ "A2.B1.T1.S5, D-16\r\n" //
				+ "A2.B1.T1.S4, D-17\r\n" //
				+ "A2.B1.T1.S3, D-18\r\n" //
				+ "A2.B1.T1.S2, D-19\r\n" //
				+ "A2.B2.T1.S1, D-20\r\n" //
				+ "A2.B2.T2.S5, D-31\r\n" //
				+ "A2.B2.T2.S4, D-32\r\n" //
				+ "A2.B2.T2.S3, D-33\r\n" //
				+ "A2.B2.T2.S2, D-34\r\n" //
				+ "A2.B2.T2.S1, D-35\r\n" //
				+ "A2.B2.T1.S5, D-36\r\n" //
				+ "A2.B2.T1.S4, D-37\r\n" //
				+ "A2.B2.T1.S3, D-38\r\n" //
				+ "A2.B2.T1.S2, D-39\r\n" //
				+ "A2.B2.T1.S1, D-40\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		//Che che = 
		network.createChe("CHE1", new NetGuid("0x00000001"));
		network.createChe("CHE2", new NetGuid("0x00000002"));

		Che che1 = network.getChe("CHE1");
		che1.setColor(ColorEnum.GREEN);
		Che che2 = network.getChe("CHE2");
		che1.setColor(ColorEnum.MAGENTA);

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		aisle1.setLedController(controller1);
		aisle2.setLedController(controller2);

		return facility;

	}
	
	private void readStandardAisleFile(Facility inFacility){
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, inFacility, ediProcessTime);
	}

	private void readSmallerAisleFile(Facility inFacility){
		// Compared to the standard, this is missing:
		// A1.B1.T2  (one tier, five slots missing)
		// A1.B2.T1.S5 (one more slot missing) = 7 fewer locations in A1
		// A2.B2 (one bay, two tiers, 10 slots missing) = 13 fewer locations in A2
		
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,4,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, inFacility, ediProcessTime);
	}

	@SuppressWarnings("unused")
	private void setUpGroup1OrdersAndSlotting(Facility inFacility) throws IOException {
		// These are group = "1". Orders "123", "456", and "789"
		// 5 products batched into containers 11 through 15

		String orderCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,,123,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,456,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10100250,Organic Fire-Roasted Red Bell Peppers,3,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte orderCsvArray[] = orderCsvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(orderCsvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ordersEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter orderImporter = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		orderImporter.importOrdersFromCsvStream(reader, inFacility, ordersEdiProcessTime);

		// Slotting file

		String csvString2 = "orderId,locationId\r\n" //
				+ "123,D-2\r\n" // in A1.B1
				+ "456,D-25\r\n" // in A1.B2
				+ "789,D-35\r\n"; // in A2.B2

		byte[] csvArray2 = csvString2.getBytes();

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mOrderLocationDao);
		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		boolean result = importer.importOrderLocationsFromCsvStream(reader2, inFacility, ediProcessTime);

		// Batches file
		String thirdCsvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "1,11,10700589,5,ea\r\n" //
				+ "1,12,10722222,10,ea\r\n" //
				+ "1,13,10706962,3,ea\r\n" //
				+ "1,14,10100250,4,ea\r\n" //
				+ "1,15,10706961,2,ea\r\n"; //

		byte[] thirdCsvArray = thirdCsvString.getBytes();

		ByteArrayInputStream stream3 = new ByteArrayInputStream(thirdCsvArray);
		InputStreamReader reader3 = new InputStreamReader(stream3);

		Timestamp thirdEdiProcessTime = new Timestamp(System.currentTimeMillis());
		CrossBatchCsvImporter importer3 = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer3.importCrossBatchesFromCsvStream(reader3, inFacility, thirdEdiProcessTime);

	}

	@Test
	public final void locationDelete1() throws IOException {
		// The idea is to setup, then delete an aisle that has order locations, complete and active work instruction, associated path and controller.
		// Make no throws as those things are accessed.
		// Bring it back

		Facility facility = setUpSimpleSlottedFacility("LD01", LARGER_FACILITY);
		setUpGroup1OrdersAndSlotting(facility);

		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);

		// Before this test, let's check our setup. We should have
		// Inventory master for 10700589 and others, Container use for 11 and others, Outbound order headers for 123, 456, and 789
		// Order 123 should have 4 details.
		OrderHeader order = facility.getOrderHeader("123");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount);
		// Make sure our order locations ( from slotting file)  are valid. Make sure D-36 has early location on path
		LocationABC<?> locationD2 = (LocationABC<?>) facility.findSubLocationById("A1.B1.T2.S4");
		Assert.assertNotNull(locationD2);
		LocationABC<?> locationD2a = (LocationABC<?>) facility.findSubLocationById("D-2");
		Assert.assertNotNull(locationD2a);
		LocationABC<?> locationD36 = (LocationABC<?>) facility.findSubLocationById("D-36");
		Aisle aisle2 = (Aisle) facility.findSubLocationById("A2");
		Double a2Pos = aisle2.getPosAlongPath();
		Double d36Pos = locationD36.getPosAlongPath();
		Double d2Pos = locationD2.getPosAlongPath();
		Assert.assertEquals(d36Pos, a2Pos); // first slot along path in A2.
		Assert.assertTrue(d36Pos < d2Pos);

		// Verify that the crossBatch orders were well made. 5 products setup in containers
		HeaderCounts theCounts = facility.countCrossOrders();
		Assert.assertTrue(theCounts.mTotalHeaders == 5);
		Assert.assertTrue(theCounts.mActiveHeaders == 5);
		Assert.assertTrue(theCounts.mActiveDetails == 5);
		Assert.assertTrue(theCounts.mActiveCntrUses == 5);
		// Assume all is good.  Other tests in this class will not need to check these things.

		// Turn off housekeeping work instructions so as to not confuse the counts
		HousekeepingInjector.turnOffHK();
		// Set up a cart for container 11, which should generate work instructions for orders 123 and 456.
		facility.setUpCheContainerFromString(theChe, "11");
		HousekeepingInjector.restoreHKDefaults();
		
		List<WorkInstruction> aList = theChe.getCheWorkInstructions();
		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 2, wiCount); // one product going to 2 orders

		List<WorkInstruction> wiListAfterScan = facility.getWorkInstructions(theChe, "D-36"); // this is earliest on path
		Integer wiCountAfterScan = wiListAfterScan.size();
		Assert.assertEquals((Integer) 2, wiCountAfterScan);

		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertNotNull(wi2);
		String groupSortStr2 = wi2.getGroupAndSortCode();
		Assert.assertEquals("0002", groupSortStr2);
		
		// Now the test starts. Delete aisle A1
		Aisle aisle1 = (Aisle) facility.findSubLocationById("A1");
		Assert.assertTrue(aisle1.getActive());
		
		LOGGER.info("Making ailse A1 inactive, with its children");
		aisle1.makeInactiveAndAllChildren();
		// fetch again as the earlier reference is probably stale
		aisle1 = (Aisle) facility.findSubLocationById("A1");
		Assert.assertFalse(aisle1.getActive());
		// This should have also inactivated all children,  recursively.
		LocationABC<?> locationA1B1 = (LocationABC<?>) facility.findSubLocationById("A1.B1");
		Assert.assertFalse(locationA1B1.getActive());
		LocationABC<?> locationA1B1T1 = (LocationABC<?>) facility.findSubLocationById("A1.B1.T1");
		Assert.assertFalse(locationA1B1T1.getActive());
		LocationABC<?> locationA1B1T1S1 = (LocationABC<?>) facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertFalse(locationA1B1T1S1.getActive());

		LOGGER.info("Read back the aisles file. Should make A1 and its children active again");
		readStandardAisleFile(facility);
		aisle1 = (Aisle) facility.findSubLocationById("A1");
		Assert.assertTrue(aisle1.getActive());
		locationA1B1 = (LocationABC<?>) facility.findSubLocationById("A1.B1");
		Assert.assertTrue(locationA1B1.getActive());
		locationA1B1T1 = (LocationABC<?>) facility.findSubLocationById("A1.B1.T1");
		Assert.assertTrue(locationA1B1T1.getActive());
		locationA1B1T1S1 = (LocationABC<?>) facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertTrue(locationA1B1T1S1.getActive());

	}
	
	@Test
	public final void locationDelete2() throws IOException {
		// The idea is to setup, then redo "smaller" aisle file that results in deleted bays, tiers, and slots.
		// Bring it back with original
		
		LOGGER.info("DeleteLocation Test 2. Start by setting up standard aisles A1 and A2");
		Facility facility = setUpSimpleSlottedFacility("LD02", LARGER_FACILITY);
		setUpGroup1OrdersAndSlotting(facility);
		
		LOGGER.info("Reread same aisles file again. Just to see that there is no throw.");
		readStandardAisleFile(facility);

		LOGGER.info("And another reread."); // Reread case is covered in AisleImporterTest
		readStandardAisleFile(facility);

		// Note: this does not make locations inactive yet.
		LOGGER.info("reading aisles file that should remove bay, tier, and slot");
		readSmallerAisleFile(facility);
		// Look at the normal and deleted locations.
		
		// Why does this next one throw on obscure method not found?
		LOGGER.info("reading original aisles file that should restore the bay, tier, and slot");
		readStandardAisleFile(facility);
		// Look at the normal and deleted locations.

	}

	@Test
	public final void locationDelete3() throws IOException {
		// The purpose of this is to investigate "java.lang.NoSuchMethodException: com.gadgetworks.codeshelf.model.domain.Aisle.setPickFaceEndPosX(java.lang.Double)"
		// That we see in locationDelete2. This just flips the larger and smaller aisle file reads to see if that matters.
		
		// Also note, if you have this exception as a breakpoint, you will catch it during all facility setup during IronMQ setup. That should be cleaned up.
		
		LOGGER.info("DeleteLocation Test . Start by setting up smaller aisle A1 and A2");
		Facility facility = setUpSimpleSlottedFacility("LD02", SMALLER_FACILITY);
		setUpGroup1OrdersAndSlotting(facility);
		
		LOGGER.info("Reread same aisles file again. Just to see that there is no throw.");
		readSmallerAisleFile(facility);

		LOGGER.info("And another reread."); 
		readSmallerAisleFile(facility);

		LOGGER.info("reading larger aisles file that should add bay, tier, and slot");
		readStandardAisleFile(facility);

		// BIZARRE! comment these two lines. Then there will be a throw in readSmallerAisleFile()
		// LOGGER.info("Reread same aisles file again. Just to see that there is no throw.");
		// readStandardAisleFile(facility);

		// Why does this next one throw on obscure method not found?
		// Note: this does not make locations inactive yet.
		LOGGER.info("reading original aisles file that should make the added bay, tier, and slot inactive");
		readSmallerAisleFile(facility);

	}

}
