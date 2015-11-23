package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

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
		picker.waitForCheState(CheStateEnum.GET_WORK, 4000);
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

}
