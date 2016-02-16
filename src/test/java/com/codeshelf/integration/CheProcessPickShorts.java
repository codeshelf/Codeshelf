/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014-2015, Codeshelf, All rights reserved
 *  file CheProcessTestPick.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.ws.server.CsServerEndPoint;

/**
 * Purpose of this test class is to examine shorts and short aheads. Only in the pick process using setupOrdersDeviceLogic
 * @author jon ranstrom
 */
public class CheProcessPickShorts extends ServerTest {

	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPickShorts.class);

	private int					WAIT_TIME	= 3000;

	@Override
	public void doAfter() {
		super.doAfter();
		LOGGER.info("Device pools: " + CsServerEndPoint.getDevicePools());
	}

	public CheProcessPickShorts() {

	}

	@Test
	public final void fullAndPartialShorts() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each will not come.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,gtin"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,2,each,gtin1493"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,2,each,gtin1522";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logCheDisplay();
		picker.scanLocation("D403");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		// Only two picks
		List<WorkInstruction> wiList = picker.getAllPicksList();
		this.logWiList(wiList);
		Assert.assertEquals(2, wiList.size());

		LOGGER.info("1: A full short. Pick 0 of 2");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);

		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		LOGGER.info("2: A partial short. Pick 1 of 2");

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		// That is all the jobs. That is why it is not at DO_PICK
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();

		// Give time for the short to update on server side
		this.waitForWorkInstructionStatus(facility, wi.getPersistentId(), WorkInstructionStatusEnum.SHORT, WAIT_TIME);

		beginTransaction();
		facility = facility.reload();
		List<WorkInstruction> serverWis = picker.getServerVersionAllPicksList();
		this.logWiList(serverWis);
		Assert.assertEquals(2, serverWis.size());
		// See the server has them both as short
		WorkInstruction wi1 = serverWis.get(0);
		WorkInstruction wi2 = serverWis.get(1);
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi2.getStatus());
		Assert.assertEquals((Integer) 0, wi1.getActualQuantity());
		Assert.assertEquals((Integer) 1, wi2.getActualQuantity());
		commitTransaction();

		LOGGER.info("3: START again will recompute and give two for the fully short and one for the partial");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		beginTransaction();
		facility = facility.reload();
		serverWis = picker.getServerVersionAllPicksList();
		this.logWiList(serverWis);
		commitTransaction();

		wi1 = serverWis.get(0);
		wi2 = serverWis.get(1);
		Assert.assertEquals(WorkInstructionStatusEnum.NEW, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.NEW, wi2.getStatus());
		Assert.assertEquals((Integer) 2, wi1.getPlanQuantity());
		Assert.assertEquals((Integer) 1, wi2.getPlanQuantity());
	}

	@Test
	public final void fullAndPartialShortAheads() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each will not come.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,gtin"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12555,12555,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12555,12555,1522,SJJ BPP,2,each,gtin1522";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.setupContainer("12555", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();
		picker.scanLocation("D403");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		// Four picks
		List<WorkInstruction> wiList = picker.getAllPicksList();
		this.logWiList(wiList);
		Assert.assertEquals(4, wiList.size());

		LOGGER.info("1: A full short. Pick 0 of 2");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);

		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		LOGGER.info("2: A partial short. Pick 1 of 2");

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		// That is all the jobs. That is why it is not at DO_PICK
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();

		// Give time for the short to update on server side
		this.waitForWorkInstructionStatus(facility, wi.getPersistentId(), WorkInstructionStatusEnum.SHORT, WAIT_TIME);

		beginTransaction();
		facility = facility.reload();
		List<WorkInstruction> serverWis = picker.getServerVersionAllPicksList();
		this.logWiList(serverWis);
		Assert.assertEquals(4, serverWis.size());
		// See the server has them all as short. Two are short aheads
		WorkInstruction wi1 = serverWis.get(0);
		WorkInstruction wi2 = serverWis.get(1);
		WorkInstruction wi3 = serverWis.get(2);
		WorkInstruction wi4 = serverWis.get(3);
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi3.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi4.getStatus());
		Assert.assertEquals((Integer) 0, wi1.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi2.getActualQuantity());
		Assert.assertEquals((Integer) 1, wi3.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi4.getActualQuantity());
		commitTransaction();

		LOGGER.info("3: START again will recompute and give four jobs again");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		List<WorkInstruction> siteconWis = picker.getAllPicksList();
		Assert.assertEquals(4, siteconWis.size());
	}

	/**
	 * Bug report from Loreal. Short aheads get prior completed work instructions?
	 */
	@Test
	public final void shortAheadsCompleteNoMultiPick() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "FALSE");
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each will not come.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,gtin"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12555,12555,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12555,12555,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12666,12666,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12666,12666,1522,SJJ BPP,2,each,gtin1522";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.setupContainer("12555", "2");
		picker.setupContainer("12666", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();
		picker.scanLocation("D403");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		// Six picks. 3 orders for the two items
		List<WorkInstruction> wiList = picker.getAllPicksList();
		this.logWiList(wiList);
		Assert.assertEquals(6, wiList.size());

		LOGGER.info("1:Pick the first");
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("1b: A full short on the next. Pick 0 of 2");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		LOGGER.info("2:Pick the second item's first job");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2b: A partial short. Pick 1 of 2");

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		// That is all the jobs. That is why it is not at DO_PICK
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();

		// Give time for the short to update on server side
		this.waitForWorkInstructionStatus(facility, wi.getPersistentId(), WorkInstructionStatusEnum.SHORT, WAIT_TIME);

		beginTransaction();
		facility = facility.reload();
		List<WorkInstruction> serverWis = picker.getServerVersionAllPicksList();
		this.logWiList(serverWis);
		Assert.assertEquals(6, serverWis.size());
		// See the server has them all as short. Two are short aheads
		WorkInstruction wi1 = serverWis.get(0);
		WorkInstruction wi2 = serverWis.get(1);
		WorkInstruction wi3 = serverWis.get(2);
		WorkInstruction wi4 = serverWis.get(3);
		WorkInstruction wi5 = serverWis.get(4);
		WorkInstruction wi6 = serverWis.get(5);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi3.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi4.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi5.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi6.getStatus());
		Assert.assertEquals((Integer) 2, wi1.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi2.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi3.getActualQuantity());
		Assert.assertEquals((Integer) 2, wi4.getActualQuantity());
		Assert.assertEquals((Integer) 1, wi5.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi6.getActualQuantity());
		commitTransaction();

		LOGGER.info("3: START again will recompute and give four jobs again");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		List<WorkInstruction> siteconWis = picker.getAllPicksList();
		Assert.assertEquals(4, siteconWis.size());
	}

	/**
	 * Bug report from Loreal. Short aheads get prior completed work instructions?
	 */
	@Test
	public final void shortAheadsCompleteWithMultiPick() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "TRUE");
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each will not come.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,gtin"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12555,12555,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12555,12555,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12666,12666,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12666,12666,1522,SJJ BPP,2,each,gtin1522";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.setupContainer("12555", "2");
		picker.setupContainer("12666", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();
		picker.scanLocation("D403");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		// Six picks. 3 orders for the two items
		List<WorkInstruction> wiList = picker.getAllPicksList();
		this.logWiList(wiList);
		Assert.assertEquals(6, wiList.size());

		LOGGER.info("1:Pick the first");
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("1b: A full short on the next. Pick 0 of 2");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		LOGGER.info("2:Pick the second item's first job");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2b: A partial short. Pick 1 of 2");

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.pick(button, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		// That is all the jobs. That is why it is not at DO_PICK
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();

		// Give time for the short to update on server side
		this.waitForWorkInstructionStatus(facility, wi.getPersistentId(), WorkInstructionStatusEnum.SHORT, WAIT_TIME);

		beginTransaction();
		facility = facility.reload();
		List<WorkInstruction> serverWis = picker.getServerVersionAllPicksList();
		this.logWiList(serverWis);
		Assert.assertEquals(6, serverWis.size());
		// See the server has them all as short. Two are short aheads
		WorkInstruction wi1 = serverWis.get(0);
		WorkInstruction wi2 = serverWis.get(1);
		WorkInstruction wi3 = serverWis.get(2);
		WorkInstruction wi4 = serverWis.get(3);
		WorkInstruction wi5 = serverWis.get(4);
		WorkInstruction wi6 = serverWis.get(5);
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi1.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi2.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi3.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, wi4.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi5.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, wi6.getStatus());
		Assert.assertEquals((Integer) 2, wi1.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi2.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi3.getActualQuantity());
		Assert.assertEquals((Integer) 2, wi4.getActualQuantity());
		Assert.assertEquals((Integer) 1, wi5.getActualQuantity());
		Assert.assertEquals((Integer) 0, wi6.getActualQuantity());
		commitTransaction();

		LOGGER.info("3: START again will recompute and give four jobs again");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		List<WorkInstruction> siteconWis = picker.getAllPicksList();
		Assert.assertEquals(4, siteconWis.size());

	}

	/**
	 * Bug report from Loreal. Short aheads get prior completed work instructions?
	 * Here we are multi-picking 6 jobs, completing (as activeJobList has ordered them), numbers 1,3,5, then shorting number 4. 2 and 6 should autoshort.
	 */
	@Test
	public final void outOfOrderShortAheads() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.PICKMULT, "TRUE");
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each will not come.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,gtin"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12555,12555,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12555,12555,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12666,12666,1123,12/16 oz Bowl Lids -PLA Compostable,2,each,gtin1123"
				+ "\r\n1,USF314,COSTCO,12666,12666,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12777,12777,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12888,12888,1522,SJJ BPP,2,each,gtin1522"
				+ "\r\n1,USF314,COSTCO,12999,12999,1522,SJJ BPP,2,each,gtin1522";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.setupContainer("12555", "2");
		picker.setupContainer("12666", "3");
		picker.setupContainer("12777", "4");
		picker.setupContainer("12888", "5");
		picker.setupContainer("12999", "6");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.logLastCheDisplay();
		picker.scanLocation("D403");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		// Look at the active picks. Should be 6 for item 1522. We need to see the order.
		List<WorkInstruction> wiList = picker.getActivePickList();
		this.logWiList(wiList);
		Assert.assertEquals(6, wiList.size());

		LOGGER.info("1:Pick the first, third, and fifth");
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi2 = wiList.get(1);
		WorkInstruction wi3 = wiList.get(2);
		WorkInstruction wi4 = wiList.get(3);
		WorkInstruction wi5 = wiList.get(4);
		WorkInstruction wi6 = wiList.get(5);

		LOGGER.info("1a: Pick the first as sitecontroller ordered them");
		int button = picker.buttonFor(wi1);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("1b: Pick the fifth");
		button = picker.buttonFor(wi5);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("1c: Pick the third");
		button = picker.buttonFor(wi3);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2: A full short of the fourth job. Pick 0 of 2");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		picker.logLastCheDisplay();
		button = picker.buttonFor(wi4);
		
		// Give time for and verify that wi5 completed
		this.waitForWorkInstructionStatus(facility, wi5.getPersistentId(), WorkInstructionStatusEnum.COMPLETE, WAIT_TIME);
		// Above does its own transaction. This next block is to make it absolutely clear that the bug indicated below is real.
		beginTransaction();
		facility = facility.reload();
		WorkInstruction serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi5.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, serverWi.getStatus());
		commitTransaction();

		// Now finish the short started above
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker.logLastCheDisplay();

		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.logLastCheDisplay();

		LOGGER.info("3:Pick the second item's first job");
		WorkInstruction secondItemWi = picker.nextActiveWi();
		button = picker.buttonFor(secondItemWi);
		picker.pick(button, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("4: verify work instructions");
		// Give time for the auto shorts to update on server side
		this.waitForWorkInstructionStatus(facility, wi6.getPersistentId(), WorkInstructionStatusEnum.SHORT, WAIT_TIME);

		beginTransaction();
		facility = facility.reload();
		// Check the work instructions we did. Specifically, wi1,wi3, an wi5 should not have changed to short.
		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi1.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, serverWi.getStatus());

		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi3.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, serverWi.getStatus());
		
		// BUG HERE! wi5 used to be complete, but turned into SHORT.
		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi5.getPersistentId());
		// Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, serverWi.getStatus());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, serverWi.getStatus()); // incorrect. Reproduces DEV-1491

		serverWi = WorkInstruction.staticGetDao().findByPersistentId(secondItemWi.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, serverWi.getStatus());

		// these should be short
		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi2.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, serverWi.getStatus());

		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi4.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, serverWi.getStatus());

		serverWi = WorkInstruction.staticGetDao().findByPersistentId(wi6.getPersistentId());
		Assert.assertEquals(WorkInstructionStatusEnum.SHORT, serverWi.getStatus());

		commitTransaction();

		LOGGER.info("5: START again will recompute and give three shorted 1522 jobs again, as well as the two uncompleted 1123 jobs");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> siteconWis = picker.getAllPicksList();
		// Assert.assertEquals(5, siteconWis.size());
		Assert.assertEquals(6, siteconWis.size()); // incorrect. Just a consequence of DEV-1491 bug above
		
		/*
		Note: this test is also generating/reproducing these errors that we have had a hard time tracking down.
		Do not fix in v25. Or at least check master first. Might be fixed already in master.
		[ERROR] bad calling context 1 for unCompletedUnneededHousekeep 
		[ERROR] decrementGoodCountAndIncrementShortCount() got the good count negative. How?
		*/

	}
}