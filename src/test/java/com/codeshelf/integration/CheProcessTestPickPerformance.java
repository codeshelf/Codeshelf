/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014-2015, Codeshelf, All rights reserved
 *  file CheProcessTestPick.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTestPickPerformance extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestPickPerformance.class);

	public CheProcessTestPickPerformance() {

	}

	@SuppressWarnings(value = { "unused" })
	private void setUpSmallInventoryAndOrders(Facility inFacility) throws IOException {
		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		// One case item, just as part of our immediate short scenario
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1122,D302,8 oz Bowl Lids -PLA Compostable,,ea,6/25/14 12:00,150\r\n" //
				+ "1123,D301,12/16 oz Bowl Lids -PLA Compostable,,EA,6/25/14 12:00,95\r\n" //
				+ "1124,D303,8 oz Bowls -PLA Compostable,,ea,6/25/14 12:00,175\r\n" //
				+ "1493,D301,PARK RANGER Doll,,ea,6/25/14 12:00,164\r\n" //
				+ "1522,D302,Butterfly Yoyo,,ea,6/25/14 12:00,227\r\n" //
				+ "1523,D301,SJJ BPP, ,each,6/25/14 12:00,227\r\n"//
				+ "1555,D502,paper towel, ,cs,6/25/14 12:00,18\r\n";//
		beginTransaction();
		inFacility = Facility.staticGetDao().reload(inFacility);
		importInventoryData(inFacility, csvInventory);
		commitTransaction();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.
		// Item 1555 exists in case only, so will short on each

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1122,8 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1523,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1124,8 oz Bowls -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1555,paper towel,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		beginTransaction();
		inFacility = Facility.staticGetDao().reload(inFacility);
		importOrdersData(inFacility, csvOrders);
		commitTransaction();
	}

	private void setUpLargeEnoughOrders(Facility inFacility) throws IOException {

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,workSequence,locationId"
				+ "\r\n12311,12311,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,23,23"//
				+ "\r\n12311,12311,1493,PARK RANGER Doll,1,each,93,93" //
				+ "\r\n12311,12311,1522,Butterfly Yoyo,1,each,22,22"//
				+ "\r\n11111,11111,1122,8 oz Bowl Lids -PLA Compostable,1,each,22,22" //
				+ "\r\n11111,11111,1522,Butterfly Yoyo,1,each,22,22"//
				+ "\r\n11111,11111,1523,SJJ BPP,1,each,23,23" // 
				+ "\r\n11111,11111,1124,8 oz Bowls -PLA Compostable,1,each,24,24"//
				+ "\r\n11111,11111,1555,paper towel,2,each,55,55" // 
				+ "\r\n12312,12312,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,23,23" //
				+ "\r\n12312,12312,1493,PARK RANGER Doll,1,each,93,93" //
				+ "\r\n12312,12312,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11112,11112,1122,8 oz Bowl Lids -PLA Compostable,1,each,22,22" //
				+ "\r\n11112,11112,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11112,11112,1523,SJJ BPP,1,each,23,23" //
				+ "\r\n11112,11112,1124,8 oz Bowls -PLA Compostable,1,each,24,24" //
				+ "\r\n11112,11112,1555,paper towel,2,each,55,55"//
				+ "\r\n12313,12313,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,23,23" //
				+ "\r\n12313,12313,1493,PARK RANGER Doll,1,each,93,93" //
				+ "\r\n12313,12313,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11113,11113,1122,8 oz Bowl Lids -PLA Compostable,1,each,22,22" //
				+ "\r\n11113,11113,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11113,11113,1523,SJJ BPP,1,each,23,23" //
				+ "\r\n11113,11113,1124,8 oz Bowls -PLA Compostable,1,each,24,24" //
				+ "\r\n11113,11113,1555,paper towel,2,each,55,55"//
				+ "\r\n12314,12314,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,23,23" //
				+ "\r\n12314,12314,1493,PARK RANGER Doll,1,each,93,93" //
				+ "\r\n12314,12314,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11114,11114,1122,8 oz Bowl Lids -PLA Compostable,1,each,22,22" //
				+ "\r\n11114,11114,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11114,11114,1523,SJJ BPP,1,each,23,23" //
				+ "\r\n11114,11114,1124,8 oz Bowls -PLA Compostable,1,each,24,24" //
				+ "\r\n11114,11114,1555,paper towel,2,each,55,55"//
				+ "\r\n12315,12315,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,23,23" //
				+ "\r\n12315,12315,1493,PARK RANGER Doll,1,each,93,93" //
				+ "\r\n12315,12315,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11115,11115,1122,8 oz Bowl Lids -PLA Compostable,1,each,22,22" //
				+ "\r\n11115,11115,1522,Butterfly Yoyo,1,each,22,22" //
				+ "\r\n11115,11115,1523,SJJ BPP,1,each,23,23" //
				+ "\r\n11115,11115,1124,8 oz Bowls -PLA Compostable,1,each,24,24" //
				+ "\r\n11115,11115,1555,paper towel,2,each,55,55";

		beginTransaction();
		inFacility = Facility.staticGetDao().reload(inFacility);
		importOrdersData(inFacility, csvOrders);
		commitTransaction();
	}

	@Test
	public final void stub() throws IOException {
		Assert.assertTrue(true);
		// just to have a test when the following one is inactive and ignored.
	}

	/**
	 * A test that may be useful to profile and improve computeWork performance
	 * 
	 * Do not check in if you remove the ignore!
	 * See also ComputeWorkCommand.exec(). Uncomment some lines to add a loop that does 10 server-side calculations 
	 * instead of only one for each call from a CHE to compute work. Obviously, do not check that in!
	 */
	@Ignore
	@Test
	public final void cycleComputeWork() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		LOGGER.info("1: Upload orders");
		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, WorkInstructionSequencerType.WorkSequence.toString());
		commitTransaction();
		setUpLargeEnoughOrders(facility);

		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: Load 2 orders on cart");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("12311", "2");
		picker.setupOrderIdAsContainer("11112", "3");
		picker.setupOrderIdAsContainer("12312", "4");
		picker.setupOrderIdAsContainer("11113", "5");
		picker.setupOrderIdAsContainer("12313", "6");
		picker.setupOrderIdAsContainer("11114", "7");
		picker.setupOrderIdAsContainer("12314", "8");
		picker.setupOrderIdAsContainer("11115", "9");
		picker.setupOrderIdAsContainer("12315", "10");

		LOGGER.info("3: Start once, just to get it going");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(40, picker.getAllPicksList().size());

		LOGGER.info("4: Cycle begin");
		// profiling this shows next to nothing. For a 50 second process, perhaps 0.5 seconds spent in Codeshelf call chains.
		for (int n = 1; n <= 100; n++) {
			picker.scanCommand("START");
			picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
			picker.scanCommand("START");
			picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		}
		LOGGER.info("5: Cycle end");

	}
}