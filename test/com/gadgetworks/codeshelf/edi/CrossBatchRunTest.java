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
import com.gadgetworks.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
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
public class CrossBatchRunTest extends EdiTestABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CrossBatchRunTest.class);

	@SuppressWarnings({ "unused" })
	private Facility setUpSimpleSlottedFacility(String inOrganizationName) {
		// Besides basic crossbatch functionality, with this facility we want to test housekeeping WIs for
		// 1) same position on cart
		// 2) Bay done/change bay
		// 3) aisle done/change aisle

		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta

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
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
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
		Short channel1 = 1;
		aisle1.setLedController(controller1);
		aisle1.setLedChannel(channel1);
		aisle1.getDao().store(aisle1);
		aisle2.setLedController(controller2);
		aisle2.setLedChannel(channel1);
		aisle2.getDao().store(aisle2);

		return facility;
	}

	@SuppressWarnings("unused")
	private void setUpGroup1OrdersAndSlotting(Facility inFacility) throws IOException {
		// These are group = "1". Orders "123", "456", and "789"
		// 5 products batched into containers 11 through 15

		String orderCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,,123,99999,Unknown Item,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,456,10700589,Napa Valley Bistro - Jalape��o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10706962,Authentic Pizza Sauces,2,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10706962,Authentic Pizza Sauces,2,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10100250,Organic Fire-Roasted Red Bell Peppers,3,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte orderCsvArray[] = orderCsvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(orderCsvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ordersEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter orderImporter = createOrderImporter();
		orderImporter.importOrdersFromCsvStream(reader, inFacility, ordersEdiProcessTime);

		// Slotting file

		String csvString2 = "orderId,locationId\r\n" //
				+ "123,D-2\r\n" // in A1.B1
				+ "456,D-25\r\n" // in A1.B2
				+ "789,D-35\r\n"; // in A2.B2

		byte[] csvArray2 = csvString2.getBytes();

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = createOrderLocationImporter();
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
		ICsvCrossBatchImporter importer3 = createCrossBatchImporter();
		importer3.importCrossBatchesFromCsvStream(reader3, inFacility, thirdEdiProcessTime);

	}

	@Test
	public final void basicCrossBatchRun() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleSlottedFacility("XB01");
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
		Assert.assertEquals((Integer) 5, detailCount); // 4 good ones, and the "unknown item"
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
		
		// Just check a UI field. Basically looking for NPE
		for (OrderDetail detail : order.getOrderDetails()) {
			String theUiField = detail.getWillProduceWiUi();
		}

		// Turn off housekeeping work instructions so as to not confuse the counts
		HousekeepingInjector.turnOffHK();
		// Set up a cart for container 11, which should generate work instructions for orders 123 and 456.
		facility.setUpCheContainerFromString(theChe, "11");
		HousekeepingInjector.restoreHKDefaults();

		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, "");
		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // one product going to 2 orders

		List<WorkInstruction> wiListAfterScan = facility.getWorkInstructions(theChe, "D-36"); // this is earliest on path
		// Just some quick log output to see it
		logWiList(wiListAfterScan);
		Integer wiCountAfterScan = wiListAfterScan.size();
		Assert.assertEquals((Integer) 2, wiCountAfterScan);

		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertNotNull(wi2);
		String groupSortStr2 = wi2.getGroupAndSortCode();
		Assert.assertEquals("0002", groupSortStr2);

		this.getPersistenceService().endTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void basicHousekeeping() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleSlottedFacility("XB03");

		setUpGroup1OrdersAndSlotting(facility);

		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 15 and 14, which should generate 4 work normal instructions.
		// However, as we are coming from the same container for subsequent ones, there will be housekeeping WIs inserted.

		LOGGER.info("basicHousekeeping.  Set up CHE for 15,14");
		// Make sure housekeeping is on
		HousekeepingInjector.restoreHKDefaults();
		facility.setUpCheContainerFromString(theChe, "15,14");

		// Important to realize. theChe.getWorkInstruction() just gives all work instructions in an arbitrary order.
		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.
		// Just some quick log output to see it
		logWiList(aList);

		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 6, wiCount); // one product going to 1 order, and 1 product going to the same order and 2 more.

		WorkInstruction wi1 = aList.get(0);
		WorkInstruction wi2 = aList.get(1);
		WorkInstruction wi3 = aList.get(2);
		WorkInstruction wi4 = aList.get(3);
		WorkInstruction wi5 = aList.get(4);
		WorkInstruction wi6 = aList.get(5);

		String wi2Desc = wi2.getDescription();
		String wi5Desc = wi5.getDescription();

		Assert.assertEquals("Bay Change", wi2Desc);
		// We qualify for both bayChange and repeat container before wi6. But only get a baychange from version v8 and DEV-478
		Assert.assertEquals("Bay Change", wi5Desc);

		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void housekeepingNegativeTest() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		// Same as basic housekeeping, but showing that no housekeeps if set to pathSegmentChange and containerAndCount
		Facility facility = setUpSimpleSlottedFacility("XB04");
		setUpGroup1OrdersAndSlotting(facility);

		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 15 and 14, which should generate 4 work normal instructions.
		// However, as we are coming from the same container for subsequent ones, there will be housekeeping WIs inserted.

		LOGGER.info("housekeepingNegativeTest.  Set up CHE for 15,14");
		// Job 3 and 4 are same container, different count. No repeate container by the parameter
		// Job 3 and 4 are different bays on the same path. No bay change by the parameter
		// Future proof for other kinds of housekeeps
		HousekeepingInjector.turnOffHK();
		HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangePathSegmentChange);
		HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		facility.setUpCheContainerFromString(theChe, "15,14");
		HousekeepingInjector.restoreHKDefaults();

		// Important to realize. theChe.getWorkInstruction() just gives all work instructions in an arbitrary order.
		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.

		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 4, wiCount); // one product going to 1 order, and 1 product going to the same order and 2 more.
		// Just some quick log output to see it
		logWiList(aList);

		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void housekeepingContainerAndCount() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleSlottedFacility("XB05");
		setUpGroup1OrdersAndSlotting(facility);

		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 11,12,13, which should generate 6 normal work instructions.
		LOGGER.info("housekeepingContainerAndCount.  Set up CHE for 11,12,13");

		// Future proof for other kinds of housekeeps
		HousekeepingInjector.turnOffHK();
		HousekeepingInjector.setBayChangeChoice(BayChangeChoice.BayChangeNone);
		HousekeepingInjector.setRepeatPosChoice(RepeatPosChoice.RepeatPosContainerAndCount);
		facility.setUpCheContainerFromString(theChe, "11,12,13");
		HousekeepingInjector.restoreHKDefaults(); // set it back

		// Important to realize. theChe.getWorkInstruction() just gives all work instructions in an arbitrary order.
		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.

		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 8, wiCount); // one product going to 1 order, and 1 product going to the same order and 2 more.
		// Just some quick log output to see it
		logWiList(aList);

		WorkInstruction wi4 = aList.get(3);

		String wi4Desc = wi4.getDescription();

		Assert.assertEquals("Repeat Container", wi4Desc);

		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void containerAssignmentTest() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		// This uses the cross batch setup because the container are convenient.
		Facility facility = setUpSimpleSlottedFacility("XB06");
		setUpGroup1OrdersAndSlotting(facility);

		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 11,12,13, which should generate 6 normal work instructions.
		LOGGER.info("containerAssignmentTest.  Set up CHE for 11,12,13");
		HousekeepingInjector.turnOffHK();
		facility.setUpCheContainerFromString(theChe, "11,12,13");
		
		// Important: need to get theChe again from scratch. Not from theNetwork.getChe
		theChe = Che.DAO.findByDomainId(theNetwork, "CHE1");
		int usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		// Just exploring: see what happens if we add the same use several times.
		// Alternating this a bit for hibernate branch. The new pattern is:
		// - From the parent, add the child
		// - Look at the parent add method. It needs to call the child's set method for the parent relationship.
		// - Then code needs to remember to do the DAO.store(child)
		ContainerUse aUse = theChe.getUses().get(0);
		// Adding same item. Not storing
		theChe.addContainerUse(aUse);
		theChe.addContainerUse(aUse);
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		
		theChe = Che.DAO.findByDomainId(theNetwork, "CHE1");
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);

		theChe.addContainerUse(aUse);
		aUse.getDao().store(aUse);
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		// We just proved that adding the same object extra times to CHE uses does not
		// not result in duplicates in the list. Probably true for most hibernate relationships. But don't try this at home.
		
		// Now the new part for DEV-492. Show that we remove prior run uses
		facility.setUpCheContainerFromString(theChe, "14");
		theChe = Che.DAO.findByDomainId(theNetwork, "CHE1");
		Assert.assertTrue(theChe.getUses().size() == 1);		
		// BUG! at least with ebeans. If we used 12 above instead of 14, it throws on an ebeans
		// lazy load exception on work instruction

		HousekeepingInjector.restoreHKDefaults(); // set it back

		this.getPersistenceService().endTenantTransaction();
	}

}
