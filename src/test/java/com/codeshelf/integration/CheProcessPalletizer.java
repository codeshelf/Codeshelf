package com.codeshelf.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

public class CheProcessPalletizer extends ServerTest {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPalletizer.class);
	private static final int	WAIT_TIME	= 4000;

	private PickSimulator		picker;

	/**
	 * This init() sets up the facility with palletizer slots, and che1 and "picker" for the che that is set to palletizer process
	 */
	@Before
	public void init() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		LOGGER.info("1: Set up facility");
		Facility facility = getFacility();
		//1 Aisle, 1 Bay, 2 Tiers
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n"
				+ "Aisle,A1,,,,,tierB1S1Side,2.85,5,X,20\n"
				+ "Bay,B1,100,,,,,,,,\n"
				+ "Tier,T1,100,4,32,0,,,,,\n"
				+ "Tier,T2,100,0,32,0,,,,,";
		importAislesData(facility, aislesCsvString);

		String locationsCsvString = "mappedLocationId,locationAlias\n" + "A1.B1,Bay11\n" + "A1.B1.T1,Tier111\n"
				+ "A1.B1.T1.S1,Slot1111\n" + "A1.B1.T1.S2,Slot1112\n" + "A1.B1.T1.S3,Slot1113\n" + "A1.B1.T1.S4,Slot1114\n"
				+ "A1.B1.T2,Tier112";
		importLocationAliasesData(facility, locationsCsvString);

		Aisle a1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Bay b1 = Bay.staticGetDao().findByDomainId(a1, "B1");
		Tier t111 = Tier.staticGetDao().findByDomainId(b1, "T1");
		Tier t112 = Tier.staticGetDao().findByDomainId(b1, "T2");
		t111.setTapeIdUi("0001");
		t112.setTapeIdUi("0002");

		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe(cheId1);
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.PALLETIZER);
		Che.staticGetDao().store(che1);
		
		LedController ledcon = network.findLedController(ledconId1);
		a1.setLedController(ledcon);
		a1.setLedChannel((short)1);
		Aisle.staticGetDao().store(a1);

		this.getTenantPersistenceService().commitTransaction();

		startSiteController();
		picker = createPickSim(cheGuid1);
		picker.login("Worker1");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
	}

	@Test
	public void testPut() {
		LOGGER.info("1: Open two pallets");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		openNewPallet("10020001", "%000000020010", "Tier112");

		LOGGER.info("1a: Verify that the WIs have the correct PickerId");
		beginTransaction();
		List<WorkInstruction> wis = WorkInstruction.staticGetDao().getAll();
		for (WorkInstruction wi : wis) {
			Assert.assertEquals("Worker1", wi.getPickerId());
		}
		commitTransaction();

		LOGGER.info("2: Put item in pallet 1001, scan item for pallet 1002, but don't complete it");
		picker.scanSomething("10010002");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Slot1111\nItem: 10010002\nStore: 1001\nScan Next Item\n", picker.getLastCheDisplay());
		picker.scanSomething("10020002");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Tier112\nItem: 10020002\nStore: 1002\nScan Next Item\n", picker.getLastCheDisplay());
		picker.buttonPress(1);

		ThreadUtils.sleep(700);

		LOGGER.info("3: Verify DB objects");
		beginTransaction();
		Facility facility = getFacility();
		LOGGER.info("3a: Verify Order Headers - both orders ongoing");
		OrderHeader h1001 = findPalletizerOrderHeader(facility, "1001");
		OrderHeader h1002 = findPalletizerOrderHeader(facility, "1002");
		Assert.assertNotNull("Didn't find order 1001", h1001);
		Assert.assertNotNull("Didn't find order 1002", h1002);
		Assert.assertEquals(OrderStatusEnum.RELEASED, h1001.getStatus());
		Assert.assertEquals(OrderStatusEnum.RELEASED, h1002.getStatus());
		Interval today = new Interval(DateTime.now().withTimeAtStartOfDay(), Days.ONE);
		Assert.assertTrue(today.contains(h1001.getDueDate().getTime()));
		Assert.assertTrue(today.contains(h1002.getDueDate().getTime()));

		LOGGER.info("3b: Verify Order Details - four completed");
		OrderDetail d10010001 = h1001.getOrderDetail("10010001");
		OrderDetail d10010002 = h1001.getOrderDetail("10010002");
		OrderDetail d10020001 = h1002.getOrderDetail("10020001");
		OrderDetail d10020002 = h1002.getOrderDetail("10020002");
		Assert.assertNotNull("Didn't find detail 10010001", d10010001);
		Assert.assertNotNull("Didn't find detail 10010002", d10010002);
		Assert.assertNotNull("Didn't find detail 10020001", d10020001);
		Assert.assertNotNull("Didn't find detail 10020002", d10020002);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, d10010001.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, d10010002.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, d10020001.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, d10020002.getStatus());

		LOGGER.info("3c: Verify Work Instructions - four completed");
		WorkInstruction wi11 = d10010001.getWorkInstructions().get(0);
		WorkInstruction wi12 = d10010002.getWorkInstructions().get(0);
		WorkInstruction wi21 = d10020001.getWorkInstructions().get(0);
		WorkInstruction wi22 = d10020002.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi11.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi12.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi21.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi22.getStatus());

		LOGGER.info("3d: Verify Work Events - four completed");
		Criteria criteria = WorkerEvent.staticGetDao()
			.createCriteria()
			.add(Property.forName("parent").eq(facility))
			.add(Property.forName("eventType").eq(WorkerEvent.EventType.COMPLETE));

		@SuppressWarnings("unchecked")
		List<WorkerEvent> workerEvents = criteria.list();
		Assert.assertEquals(4, workerEvents.size());

		commitTransaction();
	}

	@Test
	public void testCloseByLicense() {
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");

		LOGGER.info("2: Scan REMOVE, and a license coressponding to the pallet");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());

		picker.scanSomething("10019991");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());

		LOGGER.info("3: Try to put another item from that pallet. Verify that the pallet was not found");
		picker.scanSomething("10010003");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1001\n\nOr Scan Another Item\n", picker.getLastCheDisplay());

		LOGGER.info("4: Verify DB objects");
		beginTransaction();
		Facility facility = getFacility();
		LOGGER.info("4a: Verify Order Header - completed and inactive");
		OrderHeader h1001 = findPalletizerOrderHeader(facility, "1001");
		Assert.assertNotNull("Didn't find order 1001", h1001);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, h1001.getStatus());
		Assert.assertTrue(h1001.getActive());

		LOGGER.info("4b: Verify Order Detail - completed and inactive");
		OrderDetail d10010001 = h1001.getOrderDetail("10010001");
		Assert.assertNotNull("Didn't find detail 10010001", d10010001);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, d10010001.getStatus());
		Assert.assertTrue(d10010001.getActive());

		LOGGER.info("4c: Verify WorkInstructions - completed and having correct container id");
		List<WorkInstruction> wis = d10010001.getWorkInstructions();
		Assert.assertFalse(wis.isEmpty());
		WorkInstruction wi = wis.get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi.getStatus());
		Assert.assertEquals("10019991", wi.getContainerId());

		LOGGER.info("4d: Verify WorkerEvent - created, completed, and referencing the correct Detail");
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("purpose", WiPurpose.WiPurposePalletizerPut.name()));
		filterParams.add(Restrictions.eq("parent", facility));
		List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
		Assert.assertEquals(1, events.size());
		WorkerEvent event = events.get(0);
		Assert.assertEquals(EventType.COMPLETE, event.getEventType());
		OrderDetail eventDetail = OrderDetail.staticGetDao().findByPersistentId(event.getOrderDetailId());
		Assert.assertEquals(d10010001, eventDetail);
		commitTransaction();

		LOGGER.info("5: Place item at the same location");
		picker.scanLocation("Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Slot1111\nItem: 10010003\nStore: 1001\nScan Next Item\n", picker.getLastCheDisplay());
	}

	@Test
	public void testCloseByLocation() {
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");

		LOGGER.info("2: Scan REMOVE, and a location with an existing pallet");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());

		picker.scanLocation("Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());

		LOGGER.info("3: Try to put another item from that pallet. Verify that the pallet was not found");
		picker.scanSomething("10010003");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1001\n\nOr Scan Another Item\n", picker.getLastCheDisplay());

		LOGGER.info("4: Place item at the same location");
		picker.scanLocation("Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
	}

	@Test
	public void testCloseNothing() {
		LOGGER.info("1: Try to close a non-open pallet with a license");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());
		picker.scanSomething("10010001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Pallet 1001 Not Found\nCANCEL TO CONTINUE\n\n\n", picker.getLastCheDisplay());

		LOGGER.info("2: Try to close a pallet in an empty location");
		picker.scanLocation("Tier111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("No Pallets In Tier111\nCANCEL TO CONTINUE\n\n\n", picker.getLastCheDisplay());
	}

	@Test
	public void testNewPalletLocationBusy() {
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");

		LOGGER.info("2: Scan item from another pallet");
		picker.scanSomething("10020001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1002\n\nOr Scan Another Item\n", picker.getLastCheDisplay());

		LOGGER.info("3: Try to put it in the already occupied location");
		picker.scanSomething("%000000010010");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Busy: Slot1111\nRemove 1001 First\n\nCANCEL TO CONTINUE\n", picker.getLastCheDisplay());
	}

	@Test
	public void testReusePalletLocation() {
		// For DEV-1429
		LOGGER.info("1a: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("1b: Add two more cartons");
		picker.scanSomething("10010002");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		picker.scanSomething("10010003");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("2: Close the pallet, adding a license plate");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		picker.scanSomething("1001aaaa");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("2: Let's see if all completed, even though we did not push the poscon button on last one.");

		beginTransaction();
		Facility facility = getFacility();
		OrderHeader h1001 = findPalletizerOrderHeader(facility, "1001");
		Assert.assertNotNull("Didn't find order 1001", h1001);
		OrderDetail d10010001 = h1001.getOrderDetail("10010001");
		OrderDetail d10010002 = h1001.getOrderDetail("10010002");
		OrderDetail d10010003 = h1001.getOrderDetail("10010003");
		Assert.assertNotNull("Didn't find detail 10010001", d10010001);
		Assert.assertNotNull("Didn't find detail 10010002", d10010002);
		Assert.assertNotNull("Didn't find detail 10010003", d10010003);

		WorkInstruction wi11 = d10010001.getWorkInstructions().get(0);
		WorkInstruction wi12 = d10010002.getWorkInstructions().get(0);
		WorkInstruction wi13 = d10010002.getWorkInstructions().get(0);
		Assert.assertNotNull(wi11);
		Assert.assertNotNull(wi12);
		Assert.assertNotNull(wi13);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi11.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi12.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi13.getStatus());

		commitTransaction();

		LOGGER.info("3: Now open a new pallet for the same store at the same location.");
		picker.scanSomething("10010004");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		picker.scanSomething("L%Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("3b: Add one more that should complete the first.");
		picker.scanSomething("10010005");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("4: Close the pallet, via position scan");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanSomething("L%Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		LOGGER.info(picker.getLastCheDisplay());

		beginTransaction();
		LOGGER.info("5: Retrieve old and new orders");
		h1001 = OrderHeader.staticGetDao().reload(h1001);
		facility = facility.reload();
		OrderHeader h1001b = findPalletizerOrderHeader(facility, "1001");
		Assert.assertNotNull(h1001b);
		Assert.assertNotEquals(h1001, h1001b);

		OrderDetail d10010004 = h1001b.getOrderDetail("10010004");
		Assert.assertNotNull(d10010004);
		WorkInstruction wi14 = d10010004.getWorkInstructions().get(0);
		Assert.assertNotNull(wi14);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi14.getStatus());
		OrderDetail d10010005 = h1001b.getOrderDetail("10010005");
		Assert.assertNotNull(d10010005);
		WorkInstruction wi15 = d10010005.getWorkInstructions().get(0);
		Assert.assertNotNull(wi15);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi15.getStatus());

		commitTransaction();
	}

	@Test
	public void testDamaged() {
		LOGGER.info("1: Scan item, open pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");

		LOGGER.info("2: Scan SHORT, but scan NO at confirmation");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.PALLETIZER_DAMAGED, WAIT_TIME);
		Assert.assertEquals("CONFIRM DAMAGED\nSCAN YES OR NO\n\n\n", picker.getLastCheDisplay());
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Slot1111\nItem: 10010001\nStore: 1001\nScan Next Item\n", picker.getLastCheDisplay());

		LOGGER.info("3: Scan SHORT, and confrm");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.PALLETIZER_DAMAGED, WAIT_TIME);
		Assert.assertEquals("CONFIRM DAMAGED\nSCAN YES OR NO\n\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
	}

	@Test
	public void testMultiCheOperations() {
		LOGGER.info("1. Create and login to second che");
		beginTransaction();
		Che che2 = getNetwork().getChe(cheId2);
		che2.setProcessMode(ProcessMode.PALLETIZER);
		Che.staticGetDao().store(che2);
		commitTransaction();
		ThreadUtils.sleep(500);

		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.login("Worker2");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("2. Open pallet on first che");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");

		LOGGER.info("3. Place 2 items into the same pallet on second che");
		picker2.scanSomething("10010002");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		picker2.scanSomething("10010003");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("4. Close pallet from the first che");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		picker.scanSomething("10019991");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("5. Complete the last item of the second che");
		picker2.buttonPress(1);
		picker2.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		ThreadUtils.sleep(500);

		LOGGER.info("6. Verify that the order and details were completed");
		beginTransaction();
		Facility facility = getFacility().reload();
		OrderHeader order = findPalletizerOrderHeader(facility, "10019991");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order.getStatus());
		OrderDetail detail1 = order.getOrderDetail("10010001");
		OrderDetail detail2 = order.getOrderDetail("10010002");
		OrderDetail detail3 = order.getOrderDetail("10010003");
		Assert.assertNotNull("Detail 10010001 not found", detail1);
		Assert.assertNotNull("Detail 10010002 not found", detail2);
		Assert.assertNotNull("Detail 10010003 not found", detail3);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail2.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail3.getStatus());
		commitTransaction();
	}

	/**
	 * Not so much of a unit test enforced with asserts. Rather, an exercise to follow how it is logged. In logs see
	 * "No cached location for 1001. Query to server." and 
	 * "Using cached location Slot1111 for 1001"
	 */
	@Test
	public void testCacheMultiChe() {
		
		// This somewhat kludgy block expands test coverage significantly.
		// See lots of WARN about "resend number" in the logs.
		setResendQueueing(true);
		
		LOGGER.info("1. Create and login to second che");
		beginTransaction();
		Che che2 = getNetwork().getChe(cheId2);
		che2.setProcessMode(ProcessMode.PALLETIZER);
		Che.staticGetDao().store(che2);
		commitTransaction();
		ThreadUtils.sleep(500);

		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.login("Worker2");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("2a. Open pallet on first che. Scan the item");
		picker.scanSomething("10010001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		LOGGER.info("2b. Scan the pallet location to complete the open as well as carton plan");
		picker.scanSomething("L%Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("2c. Open another pallet on first che. Scan the item");
		picker.scanSomething("10020001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		LOGGER.info("2b. Scan the pallet location to complete the open as well as carton plan");
		picker.scanSomething("L%Slot1114");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("3a. Place an item on the same pallet with second che.");
		picker2.scanSomething("10010002");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		LOGGER.info("3b.  Place another item on the same pallet with second che.");
		picker2.scanSomething("10010003");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("4a. Place an item on the same pallet with first che. .");
		picker.scanSomething("10010004");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		LOGGER.info("4b.  Place another item on the same pallet with first che.");
		picker.scanSomething("10010005");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("5. Complete the last item of the second che");
		picker2.buttonPress(1);
		picker2.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("6. Close  the 1001 pallet from the first che");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		picker.scanSomething("10019991");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("7. Verify that the order and details were completed");
		Facility facility = this.getFacility();
		beginTransaction();
		facility = facility.reload();
		OrderHeader order = findPalletizerOrderHeader(facility, "10019991");
		commitTransaction();
		this.waitForOrderStatus(facility, order, OrderStatusEnum.COMPLETE, true, WAIT_TIME);

		LOGGER.info("8. Place a 1002 item with second che.");
		picker2.scanSomething("10020012");
		picker2.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("9 : Open 1001 store again at another location");
		// see  "No cached location for 1001. Query to server." as the close had cleared the cache value.
		picker.scanSomething("10010011");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		LOGGER.info("9b. Scan the pallet location to complete the open as well as carton plan");
		picker.scanSomething("L%Slot1112");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("9c. Place 1002 item on the same pallet with first che.");
		picker.scanSomething("10020013");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);

		LOGGER.info("10a. Close 1001 pallet from the first che");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		picker.scanSomething("10019992");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("10b. Close 1002 pallet from the first che");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		picker.scanSomething("10029992");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);

		LOGGER.info("11. Mostly giving time for transactions to complete.");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order3 = findPalletizerOrderHeader(facility, "10019992");
		OrderHeader order4 = findPalletizerOrderHeader(facility, "10029992");
		commitTransaction();
		this.waitForOrderStatus(facility, order3, OrderStatusEnum.COMPLETE, true, WAIT_TIME);
		this.waitForOrderStatus(facility, order4, OrderStatusEnum.COMPLETE, true, WAIT_TIME);
		
		// not that it matters, but restore the radioController state
		setResendQueueing(false);

	}

	private void openNewPallet(String item, String location, String locationName) {
		String store = item.substring(0, 4);
		picker.scanSomething(item);
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store " + store + "\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
		picker.scanSomething(location);
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals(locationName + "\nItem: " + item + "\nStore: " + store + "\nScan Next Item\n",
			picker.getLastCheDisplay());
	}

	/**
	 * Not all that clever to distinguish different orders for the same store. Returns the first uncompleted, or the most recent completed.
	 */
	private OrderHeader findPalletizerOrderHeader(Facility facility, String orderId) {
		List<OrderHeader> orders = OrderHeader.staticGetDao().findByParent(facility);
		OrderHeader latestCompleteOrder = null, latestIncompleteOrder = null;
		for (OrderHeader order : orders) {
			String domainId = order.getDomainId();
			if (domainId.startsWith(orderId) || domainId.startsWith("P_" + orderId)) {
				if (order.getStatus() == OrderStatusEnum.COMPLETE) {
					if (latestCompleteOrder == null || order.getUpdated().after(latestCompleteOrder.getUpdated())) {
						latestCompleteOrder = order;
					}
				} else {
					if (latestIncompleteOrder == null || order.getUpdated().after(latestIncompleteOrder.getUpdated())) {
						latestIncompleteOrder = order;
					}
				}
			}
		}
		return latestIncompleteOrder != null ? latestIncompleteOrder : latestCompleteOrder;
	}
}
