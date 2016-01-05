package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Property;
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
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPalletizer extends ServerTest{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPalletizer.class);
	private static final int	WAIT_TIME	= 4000;
	
	private PickSimulator picker;
	
	@Before
	public void init() throws IOException{
		this.getTenantPersistenceService().beginTransaction();
		
		LOGGER.info("1: Set up facility");
		Facility facility = getFacility();
		//1 Aisle, 1 Bay, 2 Tiers
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n" + 
				"Aisle,A1,,,,,tierB1S1Side,2.85,5,X,20\n" + 
				"Bay,B1,100,,,,,,,,\n" + 
				"Tier,T1,100,4,32,0,,,,,\n" +  
				"Tier,T2,100,0,32,0,,,,,";
		importAislesData(facility, aislesCsvString);
		
		String locationsCsvString = "mappedLocationId,locationAlias\n" + 
				"A1.B1,Bay11\n" +
				"A1.B1.T1,Tier111\n" + 
				"A1.B1.T1.S1,Slot1111\n" + 
				"A1.B1.T1.S2,Slot1112\n" + 
				"A1.B1.T1.S3,Slot1113\n" + 
				"A1.B1.T1.S4,Slot1114\n" +
				"A1.B1.T2,Tier112";
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

		this.getTenantPersistenceService().commitTransaction();
		
		startSiteController();
		picker = createPickSim(cheGuid1);
		picker.login("Worker1");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testPut(){
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

		
		LOGGER.info("3b: Verify Order Details - three completed, another in progress");
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
		Assert.assertEquals(OrderStatusEnum.INPROGRESS, d10020002.getStatus());
		
		LOGGER.info("3c: Verify Work Instructions - three completed, another in progress");
		WorkInstruction wi11 = d10010001.getWorkInstructions().get(0);
		WorkInstruction wi12 = d10010002.getWorkInstructions().get(0);
		WorkInstruction wi21 = d10020001.getWorkInstructions().get(0);
		WorkInstruction wi22 = d10020002.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi11.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi12.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi21.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.INPROGRESS, wi22.getStatus());

		
		LOGGER.info("3d: Verify Work Events - three completed");
		Criteria criteria = WorkerEvent.staticGetDao()
		.createCriteria()
		.add(Property.forName("parent").eq(facility))
		.add(Property.forName("eventType").eq(WorkerEvent.EventType.COMPLETE));
		
		@SuppressWarnings("unchecked")
		List<WorkerEvent> workerEvents = criteria.list();
		Assert.assertEquals(3, workerEvents.size());
		for (WorkerEvent workerEvent : workerEvents) {
			Assert.assertEquals(WorkerEvent.EventType.COMPLETE, workerEvent.getEventType());
		}
		LOGGER.info(criteria.list().toString());

		commitTransaction();	
	}
	
	@Test
	public void testCloseByLicense(){
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
		commitTransaction();
	}
	
	@Test
	public void testCloseByLocation(){
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
	}
	
	@Test
	public void testCloseNothing(){
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
	public void testNewPalletLocationBusy(){
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
	public void testDamaged(){		
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
	
	private void openNewPallet(String item, String location, String locationName){
		String store = item.substring(0, 4);
		picker.scanSomething(item);
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store " + store + "\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
		picker.scanSomething(location);
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals(locationName + "\nItem: " + item + "\nStore: " + store + "\nScan Next Item\n", picker.getLastCheDisplay());
	}
	
	private OrderHeader findPalletizerOrderHeader(Facility facility, String orderId) {
		List<OrderHeader> orders = OrderHeader.staticGetDao().findByParent(facility);
		for (OrderHeader order : orders) {
			String domainId = order.getDomainId();
			if (domainId.startsWith(orderId) || domainId.startsWith("P_" + orderId)){
				return order;
			}
		}
		return null;
	}
}
