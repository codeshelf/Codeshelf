package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

public class CheProcessPickSubstitution extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessPickSubstitution.class);
	private static final String SUBSTITUTION_1 = "12345";
	private static final String SUBSTITUTION_2 = "67890";

	private PickSimulator		picker;
	
	@Before
	public void init() throws IOException{
		beginTransaction();
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.WORKSEQR, "WorkSequence");
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.SCANPICK, "SKU");
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.PICKMULT, "false");	//overridden in "multi" tests below
		PropertyBehavior.turnOffHK(getFacility());
		commitTransaction();
		
		startSiteController();
		picker = createPickSim(cheGuid1);
	}
	
	@Test
	public void testSubstitutionFlag() throws IOException {
		LOGGER.info("1: Import orders");
		beginTransaction();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description,3,each,LocX24,1111,1,true\n" + 
				"1111,2,ItemS2,ItemS2 Description,4,each,LocX25,1111,2,\n" + 
				"1111,3,ItemS3,ItemS3 Description,5,each,LocX26,1111,3,TRUE";
		importOrdersData(getFacility(), csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX24\nItemS1\nQTY 3\nSCAN SKU NEEDED\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Assert that first item has substituteAllowed = true");
		WorkInstruction wi = picker.getActivePick();
		Assert.assertTrue(wi.getSubstituteAllowed());
		
		LOGGER.info("4: Pick first item and advance to the second one");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX25\nItemS2\nQTY 4\nSCAN SKU NEEDED\n", picker.getLastCheDisplay());
		
		LOGGER.info("5: Assert that second item has substituteAllowed = false");
		wi = picker.getActivePick();
		Assert.assertFalse(wi.getSubstituteAllowed());
	}
	
	/**
	 * Single (not multi-pick) order for item. Substitution not allowed. Incorrect scan. Do not get the substitute confirm screen. Then correct scan. Pick.
	 */
	@Test
	public void testSubstitutionSingle1() throws IOException {
		LOGGER.info("1: Import orders, no substitutions allowed");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description,3,each,LocX24,1111,1,"; 
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("3: Scan substitution barcode, verify that nothing happens");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitInSameState(CheStateEnum.SCAN_SOMETHING, 1000);
		
		LOGGER.info("4: Scan correct barcode, pick item");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
	}
	
	/**
	 * Single (not multi-pick) order for item. Substitution allowed. Incorrect scan. YES. Pick.
	 */
	@Test
	public void testSubstitutionSingle2() throws IOException {
		LOGGER.info("1: Import orders, substitution allowed");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description,3,each,LocX24,1111,1,true"; 
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("3: Scan substitution barcode, confirm substitution");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		verifyCheDisplay(picker, "Substitute " + SUBSTITUTION_1, "For ItemS1?", "", "");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 3", "");
		picker.pickItemAuto();
		
		
		//Wait until pick propagates through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("4: Assert that WI, Detail and Order have correct statuses");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order.getStatus());
		OrderDetail detail = order.getOrderDetail("1");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail.getStatus());
		WorkInstruction wi = detail.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi.getStatus());
		Assert.assertEquals(SUBSTITUTION_1, wi.getSubstitution());
		commitTransaction();		
	}

	/**
	 * Single (not multi-pick) order for item. Substitution allowed. Incorrect scan. NO. Scan correct. Pick.
	 * Single (not multi-pick) order for item. Substitution allowed. Incorrect scan. CANCEL. Scan correct. Pick.
	 */
	@Test
	public void testSubstitutionSingle3() throws IOException {
		LOGGER.info("1: Import orders, substitution allowed");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,true\n" +
				"1111,2,ItemS2,ItemS2 Description 2,6,each,LocX25,1111,2,true";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("3: Scan substitution barcode, scan CANCEL");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("4: Scan correct barcode, pick, advance to the next item");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("5: Scan substitution barcode, scan NO");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);

		LOGGER.info("6: Scan correct barcode, pick, finish run");
		picker.scanSomething("ItemS2");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);

		//Wait until pick propagates through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("7: Assert that DB objects are NOT marked as SUBSTITUTION in any way");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order.getStatus());
		
		OrderDetail detail1 = order.getOrderDetail("1");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1.getStatus());
		WorkInstruction wi1 = detail1.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi1.getStatus());
		Assert.assertNull(wi1.getSubstitution());
		
		OrderDetail detail2 = order.getOrderDetail("2");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail2.getStatus());
		WorkInstruction wi2 = detail2.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi2.getStatus());
		Assert.assertNull(wi2.getSubstitution());
		commitTransaction();		
	}
	
	/**
	 * Single order, 1 item, 3 units. Pick 1 (short), restart run, Substitute 1 (short, wi still comes out as sunstitute)
	 */
	@Test
	public void testSubstitutionSingle4() throws IOException {
		LOGGER.info("1: Import orders, substitution allowed");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,true";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "1 order", "1 job", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 3", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Short-pick 1 unit");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "1 order", "0 jobs", "0 done     1 short", "SETUP");
		
		LOGGER.info("4: Restart pick and short-substitute 1 unit");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 2", "SCAN SKU NEEDED");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "1 order", "0 jobs", "0 done     2 short", "SETUP");
		
		//Wait until pick propagates through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("5: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order1.getStatus());
		
		OrderDetail detail1 = order1.getOrderDetail("1");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail1.getStatus());
		
		List<WorkInstruction> wis = detail1.getWorkInstructions();
		Assert.assertEquals(2, wis.size());
		for (WorkInstruction wi : wis) {
			if (wi.getPlanQuantity() == 3)  {
				Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi.getStatus());
				Assert.assertNull(wi.getSubstitution());
			} else if (wi.getPlanQuantity() == 2){
				Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi.getStatus());
				Assert.assertEquals(SUBSTITUTION_1, wi.getSubstitution());
			} else {
				Assert.fail("Unexpexted WI " + wi);
			}
		}
		commitTransaction();
	}
	
	/**
	 * Three (multi-pick) orders for item. Substitution not allowed on any. Incorrect scan. Then correct scan.
	 */
	@Test
	public void testSubstitutionMulti1() throws IOException {
		LOGGER.info("1: Import orders, no substitutions allowed");
		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "true");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,false\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,2,\n" +	// empty substituteAllowed = false
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,3,false";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "3 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Scan substitution barcode, verify that nothing happens");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitInSameState(CheStateEnum.SCAN_SOMETHING, 500);
		
		LOGGER.info("4: Scan correct barcode");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
	}
	
	/**
	 * Three (multi-pick) orders for item. Substitution allowed on any. Incorrect scan. YES. Pick each.
	 */
	@Test
	public void testSubstitutionMulti2() throws IOException {
		LOGGER.info("1: Import orders, all substitutions allowed");
		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "true");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,true\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,2,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,3,true";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "3 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Scan substitution barcode, scan YES, pick item for all 3 orders");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 18", "");
		picker.pick(1, 3);
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 15", "");
		picker.pick(2, 6);
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 9", "");
		picker.pick(3, 9);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		//Wait until picks propagate through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("4: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order3.getStatus());
		
		OrderDetail detail1 = order1.getOrderDetail("1111.1");
		OrderDetail detail2 = order2.getOrderDetail("2222.1");
		OrderDetail detail3 = order3.getOrderDetail("3333.1");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail3.getStatus());
		
		WorkInstruction wi1 = detail1.getWorkInstructions().get(0);
		WorkInstruction wi2 = detail2.getWorkInstructions().get(0);
		WorkInstruction wi3 = detail3.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi3.getStatus());
		Assert.assertEquals(SUBSTITUTION_1, wi1.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi2.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi3.getSubstitution());
		commitTransaction();
	}

	/**
	 * Three (multi-pick) orders for item. Substitution allowed on any. Correct scan. Pick one. Short. Restart pick. Incorrect scan. YES. Pick each.
	 */
	@Test
	public void testSubstitutionMulti3() throws IOException {
		LOGGER.info("1: Import orders, all substitutions allowed");
		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "true");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,true\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,2,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,3,true";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "3 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Correct scan, pick one");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pick(1, 3);
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 15", "");
		
		LOGGER.info("4: Short the rest, thus finishing run");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "0 jobs", "1 done     2 short", "SETUP");
		
		LOGGER.info("5: Start new run");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 15", "SCAN SKU NEEDED");
		
		LOGGER.info("6: Substitute item");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 15", "");
		picker.pick(2, 6);
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pick(3, 9);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "0 jobs", "3 done     2 short", "SETUP");
		
		//Wait until picks propagate through the server
		ThreadUtils.sleep(500);

		LOGGER.info("7: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order3.getStatus());
		
		OrderDetail detail1 = order1.getOrderDetail("1111.1");
		OrderDetail detail2 = order2.getOrderDetail("2222.1");
		OrderDetail detail3 = order3.getOrderDetail("3333.1");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail3.getStatus());		
		commitTransaction();
	}
	
	/**
	 * Three (multi-pick) orders for item. Substitution allowed on second in list only. Incorrect scan. YES. Pick the one. See that the other two auto-shorted.
	 */
	@Test
	public void testSubstitutionMulti4() throws IOException {
		LOGGER.info("1: Import orders, one substitution allowed");
		beginTransaction();
		Facility facility = getFacility();
		UiUpdateBehavior uiUpdateBehavior = new UiUpdateBehavior(webSocketManagerService);
		uiUpdateBehavior.updateFacilityProperty(facility.getPersistentId().toString(), FacilityPropertyType.PICKMULT.name(), "true");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,false\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,2,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,3,false";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "3 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Substitute item");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 6", "");
		picker.pick(2, 6);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		//Wait until picks propagate through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("4: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		Assert.assertEquals(OrderStatusEnum.SHORT, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, order3.getStatus());
		
		OrderDetail detail1 = order1.getOrderDetail("1111.1");
		OrderDetail detail2 = order2.getOrderDetail("2222.1");
		OrderDetail detail3 = order3.getOrderDetail("3333.1");
		Assert.assertEquals(OrderStatusEnum.SHORT, detail1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, detail3.getStatus());
		
		WorkInstruction wi1 = detail1.getWorkInstructions().get(0);
		WorkInstruction wi2 = detail2.getWorkInstructions().get(0);
		WorkInstruction wi3 = detail3.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi3.getStatus());
		Assert.assertNull(wi1.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi2.getSubstitution());
		Assert.assertNull(wi3.getSubstitution());
		commitTransaction();		
	}
	
	/**
	 * Three (multi-pick) orders for item. Substitution allowed on second in list only. Incorrect scan. NO. Correct scan. Pick each.
	 */
	@Test
	public void testSubstitutionMulti5() throws IOException {
		LOGGER.info("1: Import orders, one substitution allowed");
		beginTransaction();
		Facility facility = getFacility();
		UiUpdateBehavior uiUpdateBehavior = new UiUpdateBehavior(webSocketManagerService);
		uiUpdateBehavior.updateFacilityProperty(facility.getPersistentId().toString(), FacilityPropertyType.PICKMULT.name(), "true");
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,false\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,2,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,3,false";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "3 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Scan substitute code, then scan NO");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("4: Scan correct code, pick normally");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pick(1, 3);
		picker.pick(2, 6);
		picker.pick(3, 9);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		//Wait until picks propagate through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("5: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order3.getStatus());
		commitTransaction();
	}

	/**
	 * Three (sequential-pick) orders for 2 intermixed items. Substitution allowed on any. Substitute both items
	 */
	@Test
	public void testSubstitutionSequential1() throws IOException {
		LOGGER.info("1: Import orders");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,3,each,LocX24,1111,1,true\n" +
				"1111,1111.2,ItemS2,ItemS2 Description 2,4,each,LocX25,1111,2,true\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,6,each,LocX24,2222,3,true\n" +
				"2222,2222.2,ItemS2,ItemS2 Description 2,8,each,LocX25,2222,4,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,9,each,LocX24,3333,5,true";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "5 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 18", "SCAN SKU NEEDED");
		
		LOGGER.info("3: Substitute ItemS1, pick first item");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 18", "");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX25", "ItemS2", "QTY 12", "SCAN SKU NEEDED");
		
		LOGGER.info("4: Substitute ItemS2, pick first item");
		picker.scanSomething(SUBSTITUTION_2);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX25", SUBSTITUTION_2, "QTY 12", "");
		picker.pickItemAuto();
		
		LOGGER.info("5: Pick remaining items without confirmations");
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 15", "");
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX25", SUBSTITUTION_2, "QTY 8", "");
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 9", "");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "3 orders", "0 jobs", "5 done", "SETUP");
		
		LOGGER.info("6: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order3.getStatus());
		
		OrderDetail detail11 = order1.getOrderDetail("1111.1");
		OrderDetail detail12 = order1.getOrderDetail("1111.2");
		OrderDetail detail21 = order2.getOrderDetail("2222.1");
		OrderDetail detail22 = order2.getOrderDetail("2222.2");
		OrderDetail detail31 = order3.getOrderDetail("3333.1");
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail11.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail12.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail21.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail22.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail31.getStatus());
		
		WorkInstruction wi11 = detail11.getWorkInstructions().get(0);
		WorkInstruction wi12 = detail12.getWorkInstructions().get(0);
		WorkInstruction wi21 = detail21.getWorkInstructions().get(0);
		WorkInstruction wi22 = detail22.getWorkInstructions().get(0);
		WorkInstruction wi31 = detail31.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi11.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi12.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi21.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi22.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi31.getStatus());
		Assert.assertEquals(SUBSTITUTION_1, wi11.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_2, wi12.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi21.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_2, wi22.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi31.getSubstitution());
		commitTransaction();		
	}
	
	/**
	 * Five (sequential-pick) orders. 
	 * Pick first.
	 * Substitute second (without it asking for verification).
	 * Substitute third for something else and short by some amount. This will not auto-short fourth.
	 * Short fourth completely. This should auto-short fifth.
	 */
	@Test
	public void testSubstitutionSequential2() throws IOException {
		LOGGER.info("1: Import orders, all substitutions allowed");
		beginTransaction();
		Facility facility = getFacility();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1111.1,ItemS1,ItemS1 Description 1,1,each,LocX24,1111,1,true\n" +
				"2222,2222.1,ItemS1,ItemS1 Description 1,3,each,LocX24,2222,2,true\n" +
				"3333,3333.1,ItemS1,ItemS1 Description 1,5,each,LocX24,3333,3,true\n" +
				"4444,4444.1,ItemS1,ItemS1 Description 1,7,each,LocX24,4444,4,true\n" +
				"5555,5555.1,ItemS1,ItemS1 Description 1,9,each,LocX24,5555,5,true\n";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		LOGGER.info("2: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.setupContainer("2222", "2");
		picker.setupContainer("3333", "3");
		picker.setupContainer("4444", "4");
		picker.setupContainer("5555", "5");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "5 orders", "5 jobs", "", "START (or SETUP)");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 25", "SCAN SKU NEEDED");

		LOGGER.info("3: Verify and pick first item normally");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX24", "ItemS1", "QTY 24", "");
		
		LOGGER.info("4: Substitute and pick second item");
		picker.scanSomething(SUBSTITUTION_1);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 24", "");
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_1, "QTY 21", "");
		
		LOGGER.info("5: Substitute third item for something else, and short by some amount");
		picker.scanSomething(SUBSTITUTION_2);
		picker.waitForCheState(CheStateEnum.SUBSTITUTION_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_2, "QTY 21", "");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(3, 3);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitInSameState(CheStateEnum.DO_PICK, 400);
		verifyCheDisplay(picker, "LocX24", SUBSTITUTION_2, "QTY 16", "");
		
		LOGGER.info("6: Short the fourth item completely. Should also auto-short the fifth");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(4, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		//picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		//verifyCheDisplay(picker, "LocX24", SUBSTITUTION_2, "QTY 9", "");
		
		//LOGGER.info("7: Pick the fifth item");
		//picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		verifyCheDisplay(picker, "5 orders", "0 jobs", "2 done     3 short", "SETUP");
		
		//Wait until picks propagate through the server
		ThreadUtils.sleep(500);
		
		LOGGER.info("7: Verify DB state");
		beginTransaction();
		facility = facility.reload();
		OrderHeader order1 = OrderHeader.staticGetDao().findByDomainId(facility, "1111");
		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "2222");
		OrderHeader order3 = OrderHeader.staticGetDao().findByDomainId(facility, "3333");
		OrderHeader order4 = OrderHeader.staticGetDao().findByDomainId(facility, "4444");
		OrderHeader order5 = OrderHeader.staticGetDao().findByDomainId(facility, "5555");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, order1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, order3.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, order4.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, order5.getStatus());
		
		OrderDetail detail1 = order1.getOrderDetail("1111.1");
		OrderDetail detail2 = order2.getOrderDetail("2222.1");
		OrderDetail detail3 = order3.getOrderDetail("3333.1");
		OrderDetail detail4 = order4.getOrderDetail("4444.1");
		OrderDetail detail5 = order5.getOrderDetail("5555.1");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, detail1.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail2.getStatus());
		Assert.assertEquals(OrderStatusEnum.SUBSTITUTION, detail3.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, detail4.getStatus());
		Assert.assertEquals(OrderStatusEnum.SHORT, detail5.getStatus());
		
		WorkInstruction wi1 = detail1.getWorkInstructions().get(0);
		WorkInstruction wi2 = detail2.getWorkInstructions().get(0);
		WorkInstruction wi3 = detail3.getWorkInstructions().get(0);
		WorkInstruction wi4 = detail4.getWorkInstructions().get(0);
		WorkInstruction wi5 = detail5.getWorkInstructions().get(0);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SUBSTITUTION, wi3.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi4.getStatus());	//This WI is a shorted substitution. COmes out as substitution
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi5.getStatus());
		Assert.assertNull(wi1.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_1, wi2.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_2, wi3.getSubstitution());
		Assert.assertEquals(SUBSTITUTION_2, wi4.getSubstitution());
		Assert.assertNull(wi5.getSubstitution());
		commitTransaction();
	}
}
