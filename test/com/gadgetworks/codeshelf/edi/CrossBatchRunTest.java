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
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.exception.DataException;
import org.junit.Assert;
import org.junit.Test;
// domain objects needed
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.gadgetworks.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
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

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 10.85, 48.45);

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
		organization.createDefaultSiteControllerUser(network); 
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
		controller1.addLocation(aisle1);
		aisle1.setLedChannel(channel1);
		aisle1.getDao().store(aisle1);
		controller2.addLocation(aisle2);
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

	@SuppressWarnings("unused")
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
		Location locationD2 = facility.findSubLocationById("A1.B1.T2.S4");
		Assert.assertNotNull(locationD2);
		Location locationD2a = facility.findSubLocationById("D-2");
		Assert.assertNotNull(locationD2a);
		Location locationD36 = facility.findSubLocationById("D-36");
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
		mPropertyService.turnOffHK(facility);
		// Set up a cart for container 11, which should generate work instructions for orders 123 and 456.
		facility.setUpCheContainerFromString(theChe, "11");

		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, "");

		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // one product going to 2 orders

		List<WorkInstruction> wiListAfterScan = facility.getWorkInstructions(theChe, "D-36"); // this is earliest on path

		mPropertyService.restoreHKDefaults(facility);

		// Just some quick log output to see it
		logWiList(wiListAfterScan);
		Integer wiCountAfterScan = wiListAfterScan.size();
		Assert.assertEquals((Integer) 2, wiCountAfterScan);

		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertNotNull(wi2);
		String groupSortStr2 = wi2.getGroupAndSortCode();
		Assert.assertEquals("0002", groupSortStr2);

		this.getPersistenceService().commitTenantTransaction();
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
		mPropertyService.restoreHKDefaults(facility);
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

		this.getPersistenceService().commitTenantTransaction();
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
		mPropertyService.turnOffHK(facility);
		mPropertyService.setBayChangeChoice(facility, BayChangeChoice.BayChangePathSegmentChange);
		mPropertyService.setRepeatPosChoice(facility, RepeatPosChoice.RepeatPosContainerAndCount);
		facility.setUpCheContainerFromString(theChe, "15,14");

		// Important to realize. theChe.getWorkInstruction() just gives all work instructions in an arbitrary order.
		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.
		mPropertyService.restoreHKDefaults(facility); // set it back

		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 4, wiCount); // one product going to 1 order, and 1 product going to the same order and 2 more.
		// Just some quick log output to see it
		logWiList(aList);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void housekeepingContainerAndCount() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleSlottedFacility("XB05");
		setUpGroup1OrdersAndSlotting(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 11,12,13, which should generate 6 normal work instructions.
		LOGGER.info("housekeepingContainerAndCount.  Set up CHE for 11,12,13");

		// Future proof for other kinds of housekeeps
		mPropertyService.turnOffHK(facility);
		mPropertyService.setBayChangeChoice(facility, BayChangeChoice.BayChangeNone);
		mPropertyService.setRepeatPosChoice(facility, RepeatPosChoice.RepeatPosContainerAndCount);
		facility.setUpCheContainerFromString(theChe, "11,12,13");
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		theChe = Che.DAO.reload(theChe);
		// Important to realize. theChe.getWorkInstruction() just gives all work instructions in an arbitrary order.
		List<WorkInstruction> aList = facility.getWorkInstructions(theChe, ""); // This returns them in working order.

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		mPropertyService.restoreHKDefaults(facility); // set it back
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		Integer wiCount = aList.size();
		for(int i=0;i<wiCount; i++) {
			aList.set(i, WorkInstruction.DAO.reload(aList.get(i)));
		}
		Assert.assertEquals((Integer) 8, wiCount); // one product going to 1 order, and 1 product going to the same order and 2 more.
		// Just some quick log output to see it
		logWiList(aList);

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		WorkInstruction wi4 = WorkInstruction.DAO.reload(aList.get(3));

		String wi4Desc = wi4.getDescription();

		Assert.assertEquals("Repeat Container", wi4Desc);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void containerAssignmentTest() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		// This uses the cross batch setup because the container are convenient.
		Facility facility = setUpSimpleSlottedFacility("XB06");
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		setUpGroup1OrdersAndSlotting(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe("CHE1");

		// Set up a cart for containers 11,12,13, which should generate 6 normal work instructions.
		LOGGER.info("containerAssignmentTest.  Set up CHE for 11,12,13");
		mPropertyService.turnOffHK(facility);
		facility.setUpCheContainerFromString(theChe, "11,12,13");
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		// Important: need to get theChe again from scratch. Not from theNetwork.getChe

		int usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		// Just exploring: see what happens if we add the same use several times.
		// Alternating this a bit for hibernate branch. The new pattern is:
		// - From the parent, add the child
		// - Look at the parent add method. It needs to call the child's set method for the parent relationship.
		// - Then code needs to remember to do the DAO.store(child)
		ContainerUse aUse = theChe.getUses().get(0);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		// Adding same item. Not storing
		theChe.addContainerUse(ContainerUse.DAO.reload(aUse));
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		theChe.addContainerUse(ContainerUse.DAO.reload(aUse));
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		theChe = Che.DAO.reload(theChe);
		aUse=aUse.getDao().reload(aUse);
		theChe.addContainerUse(aUse);
		aUse.getDao().store(aUse);
		usesCount = theChe.getUses().size();
		Assert.assertTrue(usesCount == 3);
		// We just proved that adding the same object extra times to CHE uses does not
		// not result in duplicates in the list. Probably true for most hibernate relationships. But don't try this at home.

		// Now the new part for DEV-492. Show that we remove prior run uses
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		theChe = Che.DAO.reload(theChe);
		facility.setUpCheContainerFromString(theChe, "14");
		Assert.assertTrue(theChe.getUses().size() == 1);
		// BUG! at least with ebeans. If we used 12 above instead of 14, it throws on an ebeans
		// lazy load exception on work instruction

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		mPropertyService.restoreHKDefaults(facility); // set it back
		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void parentChildTransactions() throws IOException {
		// This uses the cross batch setup because the containers are convenient.
		// Shows three non-obvious behaviors
		// 1) findByPersistentId() not in a transactions throws on the get, but catch in the function and returns null.
		// 2) Possible (but difficult) to get old reference and new reference to same object out of synch on the child list contents.
		// 3) Whether you get NonUniqueObjectException is tricky.

		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleSlottedFacility("XB06");
		setUpGroup1OrdersAndSlotting(facility);
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che che1 = theNetwork.getChe("CHE1");
		Che che2 = theNetwork.getChe("CHE2");

		int che1UsesCount = che1.getUses().size();
		Assert.assertEquals(che1UsesCount, 0);
		Assert.assertNotNull(che2);
		UUID che1Uuid = che1.getPersistentId();
		UUID che2Uuid = che2.getPersistentId();
		// get additional references to the same CHE
		Che che1b = theNetwork.getChe("CHE1");
		Che che1c = Che.DAO.findByPersistentId(che1Uuid);

		List<ContainerUse> aList = ContainerUse.DAO.getAll();
		int useCount = aList.size();
		ContainerUse use0 = aList.get(0);
		ContainerUse use1 = aList.get(1);
		ContainerUse use2 = aList.get(2);

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		// - Parent add method needs to call the child's set method for the parent relationship.
		// - Then code needs to remember to do the DAO.store(child)
		che1.addContainerUse(use0);
		ContainerUse.DAO.store(use0);
		che1UsesCount = che1.getUses().size();
		Assert.assertEquals(che1UsesCount, 1);

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 1: findByPersistentId() not within a transaction. Will throw, and is caught.");
		Che che1d = null;
		// get only works within a transaction, and findByPersistentId does a get. But it catches the exception and returns null
		boolean expectedCatch = false;
		try {
			che1d = Che.DAO.findByPersistentId(che1Uuid);
			Assert.assertNull("findByPersistentId returned an object without a transaction?", che1d);
		} catch (HibernateException e) {
			expectedCatch = true;
		}
		Assert.assertTrue(expectedCatch);

		LOGGER.info("Case 2: findByPersistentId() works within a transaction. And has the expected container use");
		this.getPersistenceService().beginTenantTransaction();
		Che che1e = Che.DAO.findByPersistentId(che1Uuid);
		che1UsesCount = che1e.getUses().size();
		Assert.assertEquals(che1UsesCount, 1);

		LOGGER.info("Case 3: check earlier references obtained in prior transaction before use was added");
		che1UsesCount = che1b.getUses().size();
		Assert.assertEquals(che1UsesCount, 1);
		che1UsesCount = che1c.getUses().size();
		Assert.assertEquals(che1UsesCount, 1);

		LOGGER.info("Case 4: add to an earlier obtained reference. See if all our references have the new use");
		che1.addContainerUse(use1);
		ContainerUse.DAO.store(use1);

		this.getPersistenceService().commitTenantTransaction();

		che1UsesCount = che1b.getUses().size(); // che1b reference from same time/transaction as che1
		Assert.assertEquals(2, che1UsesCount);
		che1UsesCount = che1c.getUses().size(); // che1b reference from same time/transaction as che1. but came from DAO find
		Assert.assertEquals(2, che1UsesCount);
		// See inconsistency here!
		che1UsesCount = che1e.getUses().size(); // hibernate usage bug. Che1e came from DAO find in later transaction. We did the add to che1 (older reference)
		Assert.assertEquals(1, che1UsesCount);

		LOGGER.info("Case 4b: getUses within a new transaction");
		this.getPersistenceService().beginTenantTransaction();
		che1UsesCount = che1e.getUses().size();
		// Still inconsistent
		Assert.assertEquals(1, che1UsesCount);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 4c: get the reference from the DAO again. Now the uses count is ok.");
		this.getPersistenceService().beginTenantTransaction();
		che1e = Che.DAO.findByPersistentId(che1Uuid);
		che1UsesCount = che1e.getUses().size();
		// Now ok
		Assert.assertEquals(2, che1UsesCount);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 5: try to add the same use again. Ok. See error in log.");
		this.getPersistenceService().beginTenantTransaction();
		che1.addContainerUse(use0);
		ContainerUse.DAO.store(use0);
		che1UsesCount = che1.getUses().size();
		Assert.assertEquals(2, che1UsesCount);

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 6: add a use to che2. Works fine, but this sets up for case 7.");
		this.getPersistenceService().beginTenantTransaction();
		Assert.assertNotNull(use2);
		che2.addContainerUse(use2);
		ContainerUse.DAO.store(use2);
		// extra store call, just to see no trouble
		ContainerUse.DAO.store(use2);
		int che2UsesCount = che2.getUses().size();
		Assert.assertEquals(1, che2UsesCount);
		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 6b: after the transaction closes. Old che reference.");
		che2UsesCount = che2.getUses().size();
		Assert.assertEquals(1, che2UsesCount);

		LOGGER.info("Case 6c: get the CHE reference again.");
		this.getPersistenceService().beginTenantTransaction();
		Che che2b = Che.DAO.findByPersistentId(che2Uuid);
		che2UsesCount = che2b.getUses().size();
		Assert.assertEquals(1, che2UsesCount);
		Che che2c = use2.getCurrentChe();
		Assert.assertEquals(che2c, che2b);

		LOGGER.info("Case 7: get the NonUniqueObjectException if we store an object that was not changed?.");
		// Javadoc: This exception is thrown when an operation would break session-scoped identity. 
		// This occurs if the user tries to associate two different instances of the same Java class with a particular identifier, in the scope of a single Session.
		boolean expectedCaught = false;
		try {
			// store call on old reference. Nothing changed. Should this throw?
			ContainerUse.DAO.store(use2);
		} catch (NonUniqueObjectException e) {
			expectedCaught = true;
		}
		if (!expectedCaught)
			Assert.fail("did not get the NonUniqueObjectException");

		this.getPersistenceService().commitTenantTransaction();

		LOGGER.info("Case 7b: do not get the NonUniqueObjectException for store of changed object.");
		this.getPersistenceService().beginTenantTransaction();
	
		// Uncomment these two lines uncommented will cause the store(use2); line to throw because
		// This pulls new reference for the use into memory for that persistentId, and then we try to store the old reference.
		// Che che2d = Che.DAO.findByPersistentId(che2Uuid);
		// int use2dCount = che2d.getUses().size();
		
		boolean unExpectedCaught = false;
		try {
			// Same as above, but a real change. Should this throw?
			use2.setActive(false);
			ContainerUse.DAO.store(use2);
		} catch (NonUniqueObjectException e) {
			unExpectedCaught = true;
		}
		if (unExpectedCaught)
			Assert.fail("got a NonUniqueObjectException when not expected");

		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void intentionalPSQLError()  throws IOException {
		// We found out the hard way that a longer string in a VAR(255) column blows up inelegantly.
	
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleSlottedFacility("XB06");
		setUpGroup1OrdersAndSlotting(facility);
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che che1 = theNetwork.getChe("CHE1");
		Che che2 = theNetwork.getChe("CHE2");
		String che1DefaultDescription = che1.getDescription();
		int che1UsesCount = che1.getUses().size();
		Assert.assertEquals(che1UsesCount, 0);
		Assert.assertNotNull(che2);
		UUID che1Uuid = che1.getPersistentId();
		UUID che2Uuid = che2.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		LOGGER.info("Case 1: set up a too-long field. Using CHE description field. Commit the transaction. Should throw the error.");
		String desc = "";
		for (int count = 0; count < 500; count++ ) {
			desc += "X";
		}
		
		try {
			this.getPersistenceService().beginTenantTransaction();
			che1.setDescription(desc);
			Che.DAO.store(che1);
			Assert.assertEquals(desc, che1.getDescription());
			this.getPersistenceService().commitTenantTransaction();
			Assert.fail("Should have thrown exception related to column width");  
		} catch (DataException e) {
			this.getPersistenceService().rollbackTenantTransaction();
			LOGGER.debug("Exception OK  during test");
		}

		final String descript2 = "Description2";
		LOGGER.info("Case 2: modify the description field on the other CHE in separate transaction.");
		try {
			this.getPersistenceService().beginTenantTransaction();
			che2.setDescription(descript2);
			Assert.assertEquals(descript2, che2.getDescription());
			Che.DAO.store(che2);
			this.getPersistenceService().commitTenantTransaction();
		} catch(DataException e) {
			this.getPersistenceService().rollbackTenantTransaction();
			throw e;
		}
		
		LOGGER.info("Case 3: get che2 in yet another transaction and check the description.");
		try {
			this.getPersistenceService().beginTenantTransaction();
			Che che2b = Che.DAO.findByPersistentId(che2Uuid);
			
			Assert.assertEquals(descript2, che2b.getDescription());
				
			this.getPersistenceService().commitTenantTransaction();
		} catch(DataException e) {
			this.getPersistenceService().rollbackTenantTransaction();
			throw e;
		}
		
		LOGGER.info("Case 4: get che1 in yet another transaction and check the description.");
		try {
			this.getPersistenceService().beginTenantTransaction();
			Che che1b = Che.DAO.findByPersistentId(che1Uuid);
			
			Assert.assertEquals(che1DefaultDescription, che1b.getDescription());
			this.getPersistenceService().commitTenantTransaction();
		} catch(DataException e) {
			this.getPersistenceService().rollbackTenantTransaction();
			throw e;
		}
	}
}
