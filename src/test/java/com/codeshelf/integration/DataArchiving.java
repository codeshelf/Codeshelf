/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TestBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.google.common.collect.ImmutableMap;

/**
 * @author jon ranstrom
 *
 */
public class DataArchiving extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DataArchiving.class);
	private Facility			facility;

	public DataArchiving() {

	}

	@Before
	public void setupFacility() {
		facility = setUpSmallNoSlotFacility();
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
		beginTransaction();
		Facility facility = getFacility();
		importAislesData(facility, csvString);
		commitTransaction();

		// Get the aisle
		beginTransaction();
		facility = facility.reload();
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(facility);
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(facility, "A3");
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
		importLocationAliasesData(facility, csvLocationAliases);
		commitTransaction();

		beginTransaction();
		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController("LED3", new NetGuid("0x00000013"));

		Short channel1 = 1;
		Location tier = facility.findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		// Make sure we also got the alias
		String tierName = tier.getPrimaryAliasId();
		if (!tierName.equals("D301"))
			LOGGER.error("D301 vs. A1.B1.T1 alias not set up in setUpSimpleNoSlotFacility");

		tier = facility.findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A1.B3.T1");
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

		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.BayDistance.toString());

		commitTransaction();
		return facility;
	}

	private void setUpOrdersWithCntrGtinAndSequence(Facility inFacility) throws IOException {
		// Exactly the same as above, but with Gtin also

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,gtin,description,quantity,uom, locationId, workSequence"
				+ "\r\n,USF314,COSTCO,10001,10001,10001.1,1123,gtin1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301, 4000"
				+ "\r\n,USF314,COSTCO,10002,10002,10002.1,1493,gtin1493,PARK RANGER Doll,1,each, D302, 4001"
				+ "\r\n,USF314,COSTCO,10003,10003,10003.1,1522,gtin1522,Butterfly Yoyo,3,each, D601, 2000"
				+ "\r\n,USF314,COSTCO,10004,10004,10004.1,1122,gtin1122,8 oz Bowl Lids -PLA Compostable,2,each, D401, 3000"
				+ "\r\n,USF314,COSTCO,10004,10004,10004.2,1522,gtin1522,Butterfly Yoyo,1,each, D601, 2000"
				+ "\r\n,USF314,COSTCO,10004,10004,10004.3,1523,getin1523,SJJ BPP,1,each, D602, 2001"
				+ "\r\n,USF314,COSTCO,10005,10005,10005.1,1124,gtin1124,8 oz Bowls -PLA Compostable,1,each, D603, 2002"
				+ "\r\n,USF314,COSTCO,10005,10005,10005.2,1555,gtin1555,paper towel,2,each, D604, 2003";
		importOrdersData(inFacility, csvOrders);
	}

	/**
	 * This function assumes we are in a valid transaction, with facility properly loaded
	 * Sets the due date on the order back, and set the created date on the WI back
	 */
	private void makeDaysOldTestData(String orderId, int daysOld, Facility inFacility) {
		OrderHeader oh = OrderHeader.staticGetDao().findByDomainId(inFacility, orderId);
		if (oh == null) {
			LOGGER.error(" bad order ID in makeOrderDaysOld");
			return;
		}
			
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, (daysOld * -1));
		long desiredTimeLong = cal.getTimeInMillis();
		Timestamp desiredTime = new Timestamp(desiredTimeLong);
		oh.setDueDate(desiredTime);
		OrderHeader.staticGetDao().store(oh);
		
		List<OrderDetail> details = oh.getOrderDetails();
		for (OrderDetail detail : details) {
			for (WorkInstruction wi : detail.getWorkInstructions()) {
				wi.setCreated(desiredTime);
				WorkInstruction.staticGetDao().store(wi);
			}
		}		
	}

	/**
	 * LOCAPICK off. This is location based pick with no UPC scan, using WorkSequence
	 */
	@Test
	public final void testArchiveReportAndPurge() throws IOException {
		beginTransaction();

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, "WorkSequence");
		setUpOrdersWithCntrGtinAndSequence(facility);
		commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		Assert.assertEquals(CheStateEnum.IDLE, picker.getCurrentCheState());
		picker.loginAndSetup("Picker #1");

		LOGGER.info("1a: setup 5 orders on cart");
		picker.setupContainer("10001", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10002", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10003", "3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10004", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10005", "5");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1b: START. Now we get some work. The point was to make the work instructions");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		List<WorkInstruction> allWiList = picker.getAllPicksList();
		this.logWiList(allWiList);

		LOGGER.info("1c: Dump the cart. This keeps the old work instructions");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker.scanCommand("LOGOUT");
		picker.waitForCheState(CheStateEnum.IDLE, 4000);

		LOGGER.info("2a: Do a trivial count");
		beginTransaction();
		facility = facility.reload();

		List<OrderHeader> ordersList = OrderHeader.staticGetDao().getAll();
		List<WorkInstruction> wiList = WorkInstruction.staticGetDao().getAll();
		Assert.assertEquals(5, ordersList.size());
		Assert.assertEquals(8, wiList.size());
		commitTransaction();

		LOGGER.info("2b: Report, that what is archivable on new data");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		commitTransaction();

		
		LOGGER.info("3: Make some of the orders 'old' ");
		beginTransaction();
		facility = facility.reload();
		makeDaysOldTestData("10001", 1, facility);
		makeDaysOldTestData("10002", 2, facility);
		makeDaysOldTestData("10003", 3, facility);
		makeDaysOldTestData("10004", 4, facility);
		makeDaysOldTestData("10005", 5, facility);
		commitTransaction();
		
		LOGGER.info("4: Report, via the work service call");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		List<WorkInstruction> wiList2 = WorkInstruction.staticGetDao().getAll();
		Assert.assertEquals(8, wiList2.size());
		commitTransaction();

		LOGGER.info("5: Call the work instruction purge with no limit specified (will limit to 1000, way more than we have)");
		beginTransaction();
		facility = facility.reload();
		workService.purgeOldObjects(4, facility, WorkInstruction.class);
		commitTransaction();
		
		LOGGER.info("5b: Report, via the work service call");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		List<WorkInstruction> wiList3 = WorkInstruction.staticGetDao().getAll();
		Assert.assertEquals(3, wiList3.size());
		commitTransaction();

		LOGGER.info("6: Call the orders purge");
		beginTransaction();
		facility = facility.reload();
		workService.purgeOldObjects(2, facility, OrderHeader.class, 1000); // a typical value?
		commitTransaction();
		
		LOGGER.info("6b: Report, via the work service call");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		List<OrderHeader> orders2 = OrderHeader.staticGetDao().getAll();
		Assert.assertEquals(1, orders2.size());
		commitTransaction();

		LOGGER.info("6: Call the Containers purge");
		beginTransaction();
		facility = facility.reload();
		workService.purgeOldObjects(2, facility, Container.class, 3); 
		commitTransaction();
		
		LOGGER.info("6b: Report, via the work service call");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		List<Container> cntrs = Container.staticGetDao().getAll();
		Assert.assertEquals(2, cntrs.size());
		commitTransaction();

	}

	/**
	 * This should not happen in production. Test the system behavior when orders and work instructions are purged and then
	 * The cart keeps running to complete those jobs. (No delete message goes to site controller.)
	 */
	@Test
	public final void testPurgeActiveJobs() throws IOException {
		beginTransaction();

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, "WorkSequence");
		setUpOrdersWithCntrGtinAndSequence(facility);
		commitTransaction();

		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		Assert.assertEquals(CheStateEnum.IDLE, picker.getCurrentCheState());
		picker.loginAndSetup("Picker #1");

		LOGGER.info("1a: setup 5 orders on cart");
		picker.setupContainer("10001", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10002", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10003", "3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10004", "4");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("10005", "5");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("1b: START. Now we get some work. The point was to make the work instructions");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		List<WorkInstruction> allWiList = picker.getAllPicksList();
		this.logWiList(allWiList);

		LOGGER.info("2a: Do a trivial count");
		beginTransaction();
		facility = facility.reload();

		List<OrderHeader> ordersList = OrderHeader.staticGetDao().getAll();
		List<WorkInstruction> wiList = WorkInstruction.staticGetDao().getAll();
		Assert.assertEquals(5, ordersList.size());
		Assert.assertEquals(8, wiList.size());
		commitTransaction();

		LOGGER.info("2b: Report, that what is archivable on new data");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		commitTransaction();

		
		LOGGER.info("3: Make all of the orders  5 days 'old' ");
		beginTransaction();
		facility = facility.reload();
		makeDaysOldTestData("10001", 5, facility);
		makeDaysOldTestData("10002", 5, facility);
		makeDaysOldTestData("10003", 5, facility);
		makeDaysOldTestData("10004", 5, facility);
		makeDaysOldTestData("10005", 5, facility);
		commitTransaction();
		
		LOGGER.info("4: Call the work instruction purge with no limit specified (will limit to 1000, way more than we have)");
		beginTransaction();
		facility = facility.reload();
		workService.purgeOldObjects(3, facility, WorkInstruction.class);
		commitTransaction();
		
		LOGGER.info("5b: Report, via the work service call");
		beginTransaction();
		facility = facility.reload();
		workService.reportAchiveables(2, facility);
		List<WorkInstruction> wiList3 = WorkInstruction.staticGetDao().getAll();
		Assert.assertEquals(0, wiList3.size());
		commitTransaction();

		LOGGER.info("6a: See the site controller WI list again, and screen");
		LOGGER.info(picker.getLastCheDisplay());
		allWiList = picker.getAllPicksList();
		this.logWiList(allWiList);

		LOGGER.info("6b: Do the next pick");
		// this decomposes pickItemAuto
		WorkInstruction wi = picker.getActivePick();
		int button = picker.buttonFor(wi);
		int quantity = wi.getPlanQuantity();
		picker.pick(button, quantity);
		
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		LOGGER.info(picker.getLastCheDisplay());
		allWiList = picker.getAllPicksList();
		this.logWiList(allWiList);
				
		LOGGER.info("7: Log out. Just an indication that failures were handled cleanly above, if these states still work.");
		picker.logout(); // does a waitForCheState(CheStateEnum.IDLE
		
	}
	/**
	 * The tested function does not do anything except produce and log an output string that
	 * is convenience to transform into a test script
	 */
	@Test
	public final void testSetupManyCartsWithOrders() throws IOException {
		beginTransaction();

		propertyService.turnOffHK(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, "WorkSequence");
		setUpOrdersWithCntrGtinAndSequence(facility);
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();
		TestBehavior testBehavior = new TestBehavior();
		String outputString = testBehavior.setupManyCartsWithOrders(facility, 
			ImmutableMap.of("ordersPerChe", String.valueOf(2),
							"ches", "CHE1 CHE2 CHE3"));
		LOGGER.info(outputString);
		commitTransaction();

	}
}
