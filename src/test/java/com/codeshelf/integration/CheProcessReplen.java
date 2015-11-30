package com.codeshelf.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.resources.subresources.EventResource;
import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;
import com.google.inject.Provider;

public class CheProcessReplen extends ServerTest{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessReplen.class);

	@Before
	public void init() {
		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, WorkInstructionSequencerType.WorkSequence.toString());
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();
	}
	
	@Test
	public void replenishSimpleImport() throws IOException{
		beginTransaction();
		Facility facility = getFacility().reload();
		String orders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate,operationType\n" + 
				"Item7,,Item7,,1,each,LocX26,Item7,0,,,,,,replenish\n";
		importOrdersData(facility, orders);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(getFacility(), "Item7");
		Assert.assertNotNull(order);
		Assert.assertEquals(OrderTypeEnum.REPLENISH, order.getOrderType());
		List<OrderDetail> details = order.getOrderDetails();
		Assert.assertEquals(1, details.size());
		OrderDetail detail = details.get(0);
		Assert.assertTrue(detail.getDomainId().startsWith("Item7-each"));
		detail.reevaluateStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, detail.getStatus());
		commitTransaction();
	}
	
	@Test
	public void replenishPickAndReimport() throws IOException{
		beginTransaction();
		String orders1 = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate,operationType\n" + 
				"Item7,,Item7,,1,each,LocX26,Item7,0,,,,,,replenish\n" +
				"Item7,,Item7,,1,each,LocX27,Item7,1,,,,,,replenish\n";
		importOrdersData(getFacility().reload(), orders1);
		commitTransaction();
		
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		LOGGER.info("1: Peform the first replenish run");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("Item7", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 order\n2 jobs\n\nSTART (or SETUP)\n", picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals("LocX26\nItem7\nQTY 1\n\n", picker.getLastCheDisplay());
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals("LocX27\nItem7\nQTY 1\n\n", picker.getLastCheDisplay());
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 order\n0 jobs\n2 done\nSETUP\n", picker.getLastCheDisplay());
		
		LOGGER.info("2: Verify that no more work remains of that order");
		picker.scanCommand("START");
		ThreadUtils.sleep(500);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		beginTransaction();
		LOGGER.info("3: Verify that order was completed");
		Facility facility = getFacility().reload();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "Item7");
		Assert.assertNotNull(order);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order.getStatus());
		
		LOGGER.info("4: Re-import order, verify that it reopened");
		String orders2 = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate,operationType\n" + 
				"Item7,,Item7,,1,each,LocX27,Item7,1,,,,,,replenish\n";

		importOrdersData(getFacility().reload(), orders2);
		commitTransaction();
		
		beginTransaction();
		order = OrderHeader.staticGetDao().findByDomainId(facility, "Item7");
		Assert.assertNotNull(order);
		Assert.assertEquals(OrderStatusEnum.RELEASED, order.getStatus());
		commitTransaction();
		
		LOGGER.info("5: Perform another replenish run");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals("LocX27\nItem7\nQTY 1\n\n", picker.getLastCheDisplay());
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 order\n0 jobs\n3 done\nSETUP\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void replenishAPITrigger() throws IOException{
		LOGGER.info("1: Import outbound orders");
		beginTransaction();
		Facility facility = getFacility().reload();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.SCANPICK, "UPC");
		String orders1 = "orderId,orderDetailId,itemId,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin\n" + 
				"1111,1,Item1,3,each,LocX25,1111,1,gtinx1\n" + 
				"1111,2,Item2,4,each,LocX24,1111,2,\n" + 
				"2222,3,Item1,10,each,LocX27,2222,3,gtinx1\n";
		importOrdersData(facility, orders1);
		commitTransaction();
		
		LOGGER.info("2: Short all items during a normal outbound pick");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("2 orders\n3 jobs\n\nSTART (or SETUP)\n", picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX25\nItem1\nQTY 3\nSCAN UPC NEEDED\n", picker.getLastCheDisplay());
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX24\nItem2\nQTY 4\nSCAN UPC NEEDED\n", picker.getLastCheDisplay());
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("2 orders\n0 jobs\n0 done     3 short\nSETUP\n", picker.getLastCheDisplay());
		
		beginTransaction();
		LOGGER.info("3: Verify that 3 short events were created. Then run them through 'replenish' API");
		List<Criterion> eventParams = new ArrayList<Criterion>();
		eventParams.add(Restrictions.eq("eventType", EventType.SHORT));
		List<WorkerEvent> shortEvents = new ArrayList<>();
		//Give some time for the expected 3 short events to generate
		int attempt = 0;
		while (shortEvents.size() != 3){
			shortEvents = WorkerEvent.staticGetDao().findByFilter(eventParams);
			ThreadUtils.sleep(100);
			if (attempt++ > 10) {
				Assert.assertEquals(3, shortEvents.size());
			}
		}
		Provider<ICsvOrderImporter> orderImporterProvider = new Provider<ICsvOrderImporter>() {
			@Override
			public ICsvOrderImporter get() {
				return createOrderImporter();
			}
		};
		EventResource eventResource = new EventResource(orderImporterProvider);
		for (WorkerEvent event : shortEvents){
			eventResource.setEvent(event);
			eventResource.createReplenishOrderForEvent();
		}
		List<Criterion> orderParams = new ArrayList<Criterion>();
		orderParams.add(Restrictions.eq("orderType", OrderTypeEnum.REPLENISH));
		List<OrderHeader> replenishOrders = OrderHeader.staticGetDao().findByFilter(orderParams);
		Assert.assertEquals(2, replenishOrders.size());
		ArrayList<String> replenishIds = new ArrayList<>();
		for (OrderHeader order : replenishOrders) {
			replenishIds.add(order.getDomainId());
		}
		Assert.assertTrue(replenishIds.contains("gtinx1"));
		Assert.assertTrue(replenishIds.contains("Item2"));
		commitTransaction();
		
		LOGGER.info("4: Fulfill 2 replenish orders (containing 3 items total)");
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker.setupContainer("gtinx1", "1");
		picker.setupContainer("Item2", "2");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("2 orders\n3 jobs\n\nSTART (or SETUP)\n", picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("Item1", picker.getLastCheDisplayString(2));
		picker.scanSomething("gtinx1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("Item1", picker.getLastCheDisplayString(2));
		picker.scanSomething("gtinx1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("Item2", picker.getLastCheDisplayString(2));
		picker.scanSomething("Item2");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("2 orders\n0 jobs\n3 done\nSETUP\n", picker.getLastCheDisplay());
	}
}
