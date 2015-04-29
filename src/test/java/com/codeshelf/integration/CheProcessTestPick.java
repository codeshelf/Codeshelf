/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;
import com.google.common.base.Strings;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTestPick extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestPick.class);

	public CheProcessTestPick() {

	}

	private Facility setUpZigzagSlottedFacility() {
		// This returns a facility with aisle A1 and A2, with path between, with two bays with several tiers each.
		// This is the zigzag/cross-batch portion of the MAT as of v10

		String csvAisles = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,116,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120\r\n" //
				+ "Bay,B1,116,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n";//
		importAislesData(getFacility(), csvAisles);

		// Get the aisle
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		String csvAliases = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T1.S1,D-96\r\n" + "A1.B1.T1.S2,D-97\r\n" + "A1.B1.T1.S3,D-98\r\n"
				+ "A1.B1.T1.S4,D-99\r\n"
				+ "A1.B1.T1.S5,D-100\r\n" + "A1.B1.T2.S1,D-91\r\n" + "A1.B1.T2.S2,D-92\r\n"
				+ "A1.B1.T2.S3,D-93\r\n"
				+ "A1.B1.T2.S4,D-94\r\n" + "A1.B1.T2.S5,D-95\r\n" + "A1.B1.T3.S1,D-86\r\n"
				+ "A1.B1.T3.S2,D-87\r\n"
				+ "A1.B1.T3.S3,D-88\r\n" + "A1.B1.T3.S4,D-89\r\n" + "A1.B1.T3.S5,D-90\r\n"
				+ "A1.B1.T4.S1,D-81\r\n"
				+ "A1.B1.T4.S2,D-82\r\n" + "A1.B1.T4.S3,D-83\r\n" + "A1.B1.T4.S4,D-84\r\n"
				+ "A1.B1.T4.S5,D-85\r\n"
				+ "A1.B1.T5.S1,D-76\r\n" + "A1.B1.T5.S2,D-77\r\n" + "A1.B1.T5.S3,D-78\r\n"
				+ "A1.B1.T5.S4,D-79\r\n"
				+ "A1.B1.T5.S5,D-80\r\n" + "A1.B2.T1.S1,D-46\r\n" + "A1.B2.T1.S2,D-47\r\n"
				+ "A1.B2.T1.S3,D-48\r\n"
				+ "A1.B2.T1.S4,D-49\r\n" + "A1.B2.T1.S5,D-50\r\n" + "A1.B2.T2.S1,D-41\r\n"
				+ "A1.B2.T2.S2,D-42\r\n"
				+ "A1.B2.T2.S3,D-43\r\n" + "A1.B2.T2.S4,D-44\r\n" + "A1.B2.T2.S5,D-45\r\n"
				+ "A1.B2.T3.S1,D-36\r\n"
				+ "A1.B2.T3.S2,D-37\r\n" + "A1.B2.T3.S3,D-38\r\n" + "A1.B2.T3.S4,D-39\r\n"
				+ "A1.B2.T3.S5,D-40\r\n"
				+ "A1.B2.T4.S1,D-31\r\n" + "A1.B2.T4.S2,D-32\r\n" + "A1.B2.T4.S3,D-33\r\n"
				+ "A1.B2.T4.S4,D-34\r\n"
				+ "A1.B2.T4.S5,D-35\r\n" + "A1.B2.T5.S1,D-26\r\n" + "A1.B2.T5.S2,D-27\r\n"
				+ "A1.B2.T5.S3,D-28\r\n"
				+ "A1.B2.T5.S4,D-29\r\n" + "A1.B2.T5.S5,D-30\r\n" + "A2.B1.T1.S1,D-75\r\n"
				+ "A2.B1.T1.S2,D-74\r\n"
				+ "A2.B1.T1.S3,D-73\r\n" + "A2.B1.T1.S4,D-72\r\n" + "A2.B1.T1.S5,D-71\r\n"
				+ "A2.B1.T2.S1,D-70\r\n"
				+ "A2.B1.T2.S2,D-69\r\n" + "A2.B1.T2.S3,D-68\r\n" + "A2.B1.T2.S4,D-67\r\n"
				+ "A2.B1.T2.S5,D-66\r\n"
				+ "A2.B1.T3.S1,D-65\r\n" + "A2.B1.T3.S2,D-64\r\n" + "A2.B1.T3.S3,D-63\r\n"
				+ "A2.B1.T3.S4,D-62\r\n"
				+ "A2.B1.T3.S5,D-61\r\n" + "A2.B1.T4.S1,D-60\r\n" + "A2.B1.T4.S2,D-59\r\n"
				+ "A2.B1.T4.S3,D-58\r\n"
				+ "A2.B1.T4.S4,D-57\r\n" + "A2.B1.T4.S5,D-56\r\n" + "A2.B1.T5.S1,D-55\r\n"
				+ "A2.B1.T5.S2,D-54\r\n"
				+ "A2.B1.T5.S3,D-53\r\n" + "A2.B1.T5.S4,D-52\r\n" + "A2.B1.T5.S5,D-51\r\n"
				+ "A2.B2.T1.S1,D-25\r\n"
				+ "A2.B2.T1.S2,D-24\r\n" + "A2.B2.T1.S3,D-23\r\n" + "A2.B2.T1.S4,D-22\r\n" + "A2.B2.T1.S5,D-21\r\n"
				/*
				// Could fix these. Not needed in current test.
				+ "A2.B2.T2.S1	D-20\r\n" + "A2.B2.T2.S2	D-19\r\n" + "A2.B2.T2.S3	D-18\r\n"
				+ "A2.B2.T2.S4	D-17\r\n"
				+ "A2.B2.T2.S5	D-16\r\n" + "A2.B2.T3.S1	D-15\r\n" + "A2.B2.T3.S2	D-14\r\n"
				+ "A2.B2.T3.S3	D-13\r\n"
				+ "A2.B2.T3.S4	D-12\r\n" + "A2.B2.T3.S5	D-11\r\n" + "A2.B2.T4.S1	D-10\r\n"
				+ "A2.B2.T4.S2	D-9\r\n"
				+ "A2.B2.T4.S3	D-8\r\n" + "A2.B2.T4.S4	D-7\r\n" + "A2.B2.T4.S5	D-6\r\n"
				+ "A2.B2.T5.S1	D-5\r\n"
				+ "A2.B2.T5.S2	D-4\r\n" + "A2.B2.T5.S3	D-3\r\n" + "A2.B2.T5.S4	D-2\r\n"
				*/
				+ "A2.B2.T5.S5	D-1\r\n";
		importLocationAliasesData(getFacility(), csvAliases);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("1", new NetGuid("0x00000011"));

		Short channel1 = 1;
		Location aisle1x = getFacility().findSubLocationById("A1");
		controller1.addLocation(aisle1x);
		aisle1x.setLedChannel(channel1);
		aisle1x.getDao().store(aisle1x);

		Location aisle2x = getFacility().findSubLocationById("A2");
		controller1.addLocation(aisle2x);
		aisle2x.setLedChannel(channel1);
		aisle2x.getDao().store(aisle2x);

		return getFacility();
	}

	private void setUpSmallInventoryAndOrders(Facility inFacility) throws IOException {
		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		// One case item, just as part of our immediate short scenario
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1122,D302,8 oz Bowl Lids -PLA Compostable,,ea,6/25/14 12:00,80\r\n" //
				+ "1123,D301,12/16 oz Bowl Lids -PLA Compostable,,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D303,8 oz Bowls -PLA Compostable,,ea,6/25/14 12:00,55\r\n" //
				+ "1493,D301,PARK RANGER Doll,,ea,6/25/14 12:00,66\r\n" //
				+ "1522,D302,Butterfly Yoyo,,ea,6/25/14 12:00,3\r\n" //
				+ "1523,D301,SJJ BPP, ,each,6/25/14 12:00,3\r\n"//
				+ "1555,D502,paper towel, ,cs,6/25/14 12:00,18\r\n";//
		importInventoryData(inFacility, csvInventory);

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
		importOrdersData(inFacility, csvOrders);
	}

	@SuppressWarnings("unused")
	private void setUpBatchOrdersForZigzag(Facility inFacility) throws IOException {
		// Setting up containers 2,3,7,11 to match the bug

		// Outbound orders

		String csvOrders = "orderGroupId,orderId,itemId,description,quantity,uom\r\n" //
				+ "5/26/14,1001dry,53a8a03ab38e3c0200000330,vitalvittles Organic Flax-Seed Oat Bread,2,loaf\r\n" //
				+ "5/26/14,1003dry,539f2da2622fcc0200001009,sayhayfarms Organic Sungold Cherry Tomatoes,4,pint\r\n" //
				+ "5/26/14,1006dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,7,bag\r\n" //
				+ "5/26/14,1007dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,8,bag\r\n" //
				+ "5/26/14,1016dry,50916c6dd136890200000311,blackjet Crack*a*Roons,17,cookies\r\n"; //
		importOrdersData(inFacility, csvOrders);

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(inFacility, "1001dry");
		// inFacility.getOrderHeader("1001dry");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detailCount);

		// Slotting
		String csvSlotting = "orderId,locationId\r\n" //
				+ "1001dry,D-26\r\n" + "1001dry,D-27\r\n" + "1001dry,D-28\r\n" + "1001dry,D-29\r\n"
				+ "1001dry,D-30\r\n"
				+ "1001dry,D-31\r\n" + "1001dry,D-32\r\n" + "1001dry,D-33\r\n" + "1001dry,D-34\r\n"
				+ "1001dry,D-35\r\n"
				+ "1003dry,D-22\r\n" + "1006dry,D-100\r\n" + "1016dry,D-76\r\n" + "1007dry,D-99\r\n";
		boolean result = importSlotting(inFacility, csvSlotting);

		// Batches file. Only containers 2,3,7,11
		String csvBatch = "itemId,orderGroupId,containerId,description,quantity,uom\r\n" //
				+ "539f2da2622fcc0200001009,5/26/14,2,sayhayfarms Organic Sungold Cherry Tomatoes,1,pint\r\n" //
				+ "53a8a03ab38e3c0200000330,5/26/14,3,vitalvittles Organic Flax-Seed Oat Bread,1,loaf\r\n" //
				+ "50916c6dd136890200000311,5/26/14,7,blackjet Crack*a*Roons,2,cookies\r\n" //
				+ "5266bd1e4d5eed0200000155,5/26/14,11,firebrand Pretzel Croutons,7,bag\r\n";//
		importBatchData(inFacility, csvBatch);
	}

	@Test
	public final void testStartWorkReverse() throws IOException {
		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();

		this.getTenantPersistenceService().commitTransaction();

		for(Entry<String, String> entry : ThreadContext.getContext().entrySet()) {
			LOGGER.info("ThreadContext: {} = {}",entry.getKey(),entry.getValue());
		}
		this.startSiteController();

		//For this data set
		//Forward ordering is 3,2,1
		// Reverse ordering is 1,2,3
		startReverseWork(facility);
	}

	//@Test
	public final void testStartWorkReverseSkipToLocation() throws IOException {
		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = startReverseWork(facility);

		//Then skip to location
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);

		//For this data set
		//Forward ordering is 3,2,1
		// Reverse ordering is 1,2,3
		List<WorkInstruction> wiList = picker.getAllPicksList();
		//Check Total WI size
		assertTrue(wiList.size() == 5);
		//Check each WI
		assertEquals(wiList.get(0).getItemId(), "2");
		assertEquals(wiList.get(1).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(2).getItemId(), "3");
		assertEquals(wiList.get(3).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(4).getItemId(), "1");

		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	//work items appear to be in the wrong order on normal forward
	public final void testStartWorkForwardSkipToLocation() throws IOException {
		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,2,2,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,3,3,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		// Start setting up cart etc
		this.getTenantPersistenceService().beginTransaction();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.setupOrderIdAsContainer("2", "2");
		picker.setupOrderIdAsContainer("3", "3");
		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);

		//Scan at item 1
		picker.scanLocation("");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		List<WorkInstruction> wiList = picker.getAllPicksList();

		//Check Total WI size
		assertTrue(wiList.size() == 5);

		//Check each WI

		assertEquals("3", wiList.get(0).getItemId());
		assertEquals(WorkInstructionTypeEnum.HK_BAYCOMPLETE, wiList.get(1).getType());
		assertEquals("2", wiList.get(2).getItemId());
		assertEquals(WorkInstructionTypeEnum.HK_BAYCOMPLETE, wiList.get(3).getType());
		assertEquals("1", wiList.get(4).getItemId());

		//Start at item 2
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		wiList = picker.getAllPicksList();

		//Check Total WI size
		assertTrue(wiList.size() == 5);
		//Check each WI
		assertEquals("2", wiList.get(0).getItemId());
		assertEquals(WorkInstructionTypeEnum.HK_BAYCOMPLETE, wiList.get(1).getType());
		assertEquals("1", wiList.get(2).getItemId());
		assertEquals(WorkInstructionTypeEnum.HK_BAYCOMPLETE, wiList.get(3).getType());
		assertEquals("3", wiList.get(4).getItemId());

		this.tenantPersistenceService.commitTransaction();
	}

	private PickSimulator startReverseWork(Facility facility) throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvString);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,2,2,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,3,3,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvString2);
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();

		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.setupOrderIdAsContainer("2", "2");
		picker.setupOrderIdAsContainer("3", "3");
		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);

		picker.scanCommand("REVERSE");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//AssertForwardOrdering
		List<WorkInstruction> wiList = picker.getAllPicksList();
		//Check Total WI size
		assertEquals(5, wiList.size());
		//Check each WI
		assertEquals(wiList.get(0).getItemId(), "1");
		assertEquals(wiList.get(1).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(2).getItemId(), "2");
		assertEquals(wiList.get(3).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(4).getItemId(), "3");
		this.tenantPersistenceService.commitTransaction();

		return picker;

	}

	@Test
	public final void testPick() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

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
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		//facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			}
		}
		Assert.assertEquals(2, itemLocations.size());
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// Turn off housekeeping work instructions so as to not confuse the counts
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Set up a cart for order 12345, which will generate work instructions
		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		workService.setUpCheContainerFromString(che1, "12345");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		List<WorkInstruction> aList = workService.getWorkInstructions(che1, "");
		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // 3, but one should be short. Only 1123 and 1522 find each inventory

		for (WorkInstruction workInstruction : aList) {
			Assert.assertEquals(OrderStatusEnum.INPROGRESS, workInstruction.getOrderDetail().getStatus());
		}
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.restoreHKDefaults(facility); // set it back
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		che1 = Che.staticGetDao().reload(che1);
		List<WorkInstruction> wiListAfterScan = workService.getWorkInstructions(che1, "D402");
		Integer wiCountAfterScan = wiListAfterScan.size();
		Double posOf402 = locationD402.getPosAlongPath();
		Double posOf403 = locationD403.getPosAlongPath();
		Assert.assertTrue(posOf402 > posOf403);

		// If DEV-477 route-wrap is in effect, both are there, but the 402 item is first. We still get the baychange between
		// If DEV-477 is not in effect, 402 item is still first, and 403 item is not in the list.
		Assert.assertEquals((Integer) 3, wiCountAfterScan);
		// See which work instruction is which
		WorkInstruction wi1 = wiListAfterScan.get(0);
		Assert.assertNotNull(wi1);
		String wiLoc = wi1.getPickInstruction(); // this is the denormalized position on the work instruction. Should have the alias, and not F1.A2.B2.T1
		Assert.assertEquals("D402", wiLoc);
		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertTrue(wi2.isHousekeeping());

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		che1 = Che.staticGetDao().reload(che1);
		// New from v4. Test our work instruction summarizer
		List<WiSetSummary> summaries = this.workService.workAssignedSummary(che1.getPersistentId(), facility.getPersistentId());

		// as this test, this facility only set up this one che, there should be only one wi set.
		Assert.assertEquals(1, summaries.size());

		// getAny should get the one. Call it somewhat as the UI would. Get a time, then query again with that time.
		WiSetSummary theSummary = summaries.get(0);
		// So, how many shorts, how many active? None complete yet.
		int actives = theSummary.getActiveCount();
		// int shorts = theSummary.getShortCount();
		int completes = theSummary.getCompleteCount();
		Assert.assertEquals(0, completes);
		Assert.assertEquals(3, actives);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testPickViaChe() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		//OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			} else {
				LOGGER.debug(detail.getItemMasterId() + " " + detail.getUomMasterId() + " has no location");
			}
		}
		Assert.assertEquals(2, itemLocations.size());
		// Turn off housekeeping work instructions for next test so as to not confuse the counts
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.restoreHKDefaults(facility);
		this.getTenantPersistenceService().commitTransaction();

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).byteValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());

		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());

		picker.waitForCheState(picker.getCompleteState(), 1000);
		picker.logout();
	}

	@Test
	@SuppressWarnings("unused")
	public final void testCheProcess1() throws IOException {
		// Test cases:
		// 1) If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)
		// 2) A happy-day pick startup. No housekeeping jobs.
		// Case 3: A happy-day short, with one short-ahead");
		// Case 4: Short and cancel leave you on the same job");
		// Case 5: Inappropriate location scan, then normal button press works");

		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		setUpSmallInventoryAndOrders(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		// perform pick operations
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		Assert.assertEquals(2, containers.size());
		this.getTenantPersistenceService().commitTransaction();

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		// This brief case covers and allows retirement of CheSimulationTest.java
		LOGGER.info("Case 1: If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)");
		picker.setupContainer("9x9x9", "1"); // unknown container
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK, 5000);

		Assert.assertEquals(0, picker.countActiveJobs());

		//Make sure position display controllers show proper feedback
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		// Back to our main test
		LOGGER.info("Case 2: A happy-day pick startup. No housekeeping jobs.");
		picker.setup();
		picker.setupContainer("12345", "1"); // This prepended to scan "C%12345" as per Codeshelf scan specification
		String firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("SCAN ORDER", firstLine); // see getContainerSetupMsg()

		//Check that container show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		picker.scanOrderId("11111");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);
		firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("SELECT POSITION", firstLine); // see getContainerSetupMsg()

		//Make sure we do not lose last container
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));

		picker.scanPosition("2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("SCAN ORDER", firstLine); // see getContainerSetupMsg()

		//Check that containers show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), Byte.valueOf("11"));

		//picker.startAndSkipReview("D303", 5000, 3000);
		//Check to make sure we can scan a good location after a bad location
		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(true), 4000);
		picker.scanLocation("BAD_LOCATION");
		picker.waitForCheState(CheStateEnum.NO_WORK, 4000);
		firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("NO WORK TO DO", firstLine);

		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("D303", firstLine);

		LOGGER.info("List the work instructions as the server sees them");

		this.getTenantPersistenceService().beginTransaction();
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(7, picker.countRemainingJobs());
		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();
		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);

		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		wi = WorkInstruction.staticGetDao().reload(wi);
		//Pos 1 should be the same
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 1));
		//After Scanning start location of D303 we should be right next to the
		//8oz bowls which is part of order 11111 in position 2 with a quantity of 1
		//That means the position controller for position 2 should have a quantity of 1:
		//Make sure I was right about position 2 (order 11111), quantity 1 of 8oz bowls which has an itemId 1123
		Assert.assertEquals(button, 2);
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), (byte) 1);
		Assert.assertEquals(wi.getItemId(), "1124");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) button),
			PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) button), PosControllerInstr.SOLID_FREQ);

		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(6, picker.countRemainingJobs());
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();

		LOGGER.info("Case 3: A happy-day short, with one short-ahead");
		wi = picker.nextActiveWi();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		wi = WorkInstruction.staticGetDao().reload(wi);
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();

		//Third job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), (byte) 1);
		// the third job is for 1522, which happens to be the one item going to both orders. So it should short-ahead
		Assert.assertEquals("1522", wi.getItemId());
		this.getTenantPersistenceService().commitTransaction();

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("YES");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Would be 5, but with one short ahead it is 4.

		LOGGER.info("Case 4: Short and cancel leave you on the same job");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("NO");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Still 4.
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2); // same work instruction still on

		LOGGER.info("Case 5: Inappropriate location scan, then normal button press works");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		Assert.assertNotEquals(0, button);
		quant = wi.getPlanQuantity();
		picker.scanLocation("D302");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000); // still on pick state, although with an error message
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Next job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		Byte ctrlDispValueObj = picker.getLastSentPositionControllerDisplayValue((byte) button);
		Assert.assertNotNull(ctrlDispValueObj);
		int ctrlDispValue = ctrlDispValueObj.byteValue();
		int planValue = wi.getPlanQuantity().byteValue();
		Assert.assertEquals(ctrlDispValue, planValue);
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);

		picker.pick(button, quant);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);

		//After the v15 release, jumping to a new position during a pick restores all previously shorted instructions
		Assert.assertEquals(5, picker.countRemainingJobs());
		//Skip a first instruction to mainain an older test that expected the shorted instructions to stay hidden
		picker.pickItemAuto();

		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		UUID changingWiPersist = wi.getPersistentId();

		//Last check:
		//Next job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), wi.getPlanQuantity()
			.byteValue());
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);

		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		LOGGER.info("List the work instructions as the server sees them");
		this.getTenantPersistenceService().beginTransaction();
		List<WorkInstruction> serverWiList2 = picker.getCurrentWorkInstructionsFromList(serverWiList);
		logWiList(serverWiList2);
		// In this, we see 2nd wi is user short, and third a short ahead. Item 1555 should have got an immediate short.
		WorkInstruction userShortWi = serverWiList2.get(1);
		WorkInstruction shortAheadWi = serverWiList2.get(2);
		WorkInstruction immediateShortWi = null;

		// wait here because doButton above takes some time to percolate over to server side.
		// Below, che1b.getCheWorkInstructions() should have the completed work instruction. Get a staleObjectState exception if it changes to slow.
		boolean done = false;
		int count = 0;
		while (!done && count < 200) { // 2 seconds bail. Test should fail below so don't worry.
			WorkInstruction wi3 = WorkInstruction.staticGetDao().findByPersistentId(changingWiPersist);
			if (wi3.getStatus() == WorkInstructionStatusEnum.COMPLETE) {
				done = true;
			}
			count++;
			ThreadUtils.sleep(10);
		}

		// If you ask che1 for getCheWorkInstructions(), the list will throw during lazy load because the che reference came from a different transaction.
		// But we had to change the transaction in order to see the completed work instructions.
		Che che1b = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		// for (WorkInstruction cheWi : che1.getCheWorkInstructions()) {
		List<WorkInstruction> cheWis2 = che1b.getCheWorkInstructions();
		Assert.assertNotNull(cheWis2);

		for (WorkInstruction cheWi : cheWis2) {
			if (cheWi.getItemMasterId().equals("1555"))
				immediateShortWi = cheWi;
		}
		Assert.assertNotNull(userShortWi);
		Assert.assertNotNull(shortAheadWi);
		//Auto-shorting functionality disabled 02/03/2015
		//Assert.assertNotNull(immediateShortWi);
		//logOneWi(immediateShortWi);
		logOneWi(userShortWi);
		logOneWi(shortAheadWi);
		// All should have the same assign time
		Assert.assertEquals(shortAheadWi.getAssigned(), userShortWi.getAssigned());
		//Assert.assertEquals(immediateShortWi.getAssigned(), shortAheadWi.getAssigned());

		propertyService.restoreHKDefaults(facility);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testRouteWrap() throws IOException {
		// create test data
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		setUpSmallInventoryAndOrders(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		// perform pick operation
		this.getTenantPersistenceService().beginTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we need to test removing the bay change just prior to the wrap point.

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Case 1: Scan on near the end of the route. Only 3 of 7 jobs left. (There are 3 housekeeping). So, with route-wrap, 10 jobs");

		picker.setupContainer("12345", "1");
		picker.setupContainer("11111", "2");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.startAndSkipReview("D301", 5000, 3000);
		propertyService.restoreHKDefaults(facility);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).byteValue(), (byte) 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 1));

		// WARNING: whenever getting work instructions via the picker, it is in the context that the site controller has. For example
		// the itemMaster field is null.
		Assert.assertEquals(10, picker.countRemainingJobs());
		LOGGER.info("List the work instructions as the site controller sees them");
		List<WorkInstruction> theWiList = picker.getAllPicksList();
		logWiList(theWiList);
		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();

		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D301", wi.getPickInstruction());

		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(9, picker.countRemainingJobs());

		LOGGER.info("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(8, picker.countRemainingJobs());
		// last job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals(7, picker.countRemainingJobs());
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		LOGGER.info("List the work instructions as the server now has them");
		List<WorkInstruction> serverWiList2 = picker.getCurrentWorkInstructionsFromList(serverWiList);
		logWiList(serverWiList2);

		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testRouteWrap2() throws IOException {
		// Reproduce bug seen during MAT for v10
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpZigzagSlottedFacility();
		setUpBatchOrdersForZigzag(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		// perform pick operation
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we found the bug with it on.

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Case 1: Scan ");
		// The case is to set up batch containers 2,3,7,11. Start location D-26 is ok (no wrap). Start location D-76 has a wrap.
		picker.setupContainer("2", "4");
		picker.setupContainer("3", "5");
		picker.setupContainer("7", "14");
		picker.setupContainer("11", "15");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.scanCommand("START");

		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker.scanLocation("D-76");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		this.getTenantPersistenceService().beginTransaction();
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();

		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D-76", wi.getPickInstruction());
		this.getTenantPersistenceService().commitTransaction();
		// D-76 is interesting. Actually last tier on the path in that tier, so our code normalizes back the the bay posAlongPath.
		// D-76 comes up first in the list compared to the other two in that bay only because it has the top tier location and we sort top down.

		// pick first item. 7 left (3 housekeeps)
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(7, picker.countRemainingJobs());

		LOGGER.info("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(6, picker.countRemainingJobs());
		Assert.assertEquals("D-100", wi.getPickInstruction());

		// fourth job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals(5, picker.countRemainingJobs());
		Assert.assertEquals("", wi.getPickInstruction()); // a housekeep

		// fifth job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals("D-99", wi.getPickInstruction());

	}

	@SuppressWarnings("unused")
	@Test
	public final void twoChesCrossBatch() throws IOException {
		// Reproduce DEV-592 seen during MAT for v10
		// This test case setup similarly to testRouteWrap2
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpZigzagSlottedFacility();
		setUpBatchOrdersForZigzag(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		// perform pick operation
		this.getTenantPersistenceService().beginTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we found the bug with it on.

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Set up first CHE ");
		// The case is to set up batch containers 2,3,7,11. Start location D-26 is ok (no wrap). Start location D-76 has a wrap.
		picker.setupContainer("2", "4");
		picker.setupContainer("3", "5");
		picker.setupContainer("7", "14");
		picker.setupContainer("11", "15");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.scanCommand("START");

		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker.scanLocation("D-76");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);
		Assert.assertEquals(8, serverWiList.size());

		LOGGER.info("First CHE walks away. Never doing anything. Set up same thing on second CHE ");
		// This is the DEV-592 bug. Our hibernate parent-childe patterns says we cannot add WI to one CHE without first removing from the other.

		PickSimulator picker2 = createPickSim(cheGuid2);
		picker2.loginAndSetup("Picker #2");

		picker2.setupContainer("2", "4");
		picker2.setupContainer("3", "5");
		picker2.setupContainer("7", "14");
		picker2.setupContainer("11", "15");
		picker2.scanCommand("START");

		picker2.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker2.scanLocation("D-76");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList2 = picker2.getServerVersionAllPicksList();
		logWiList(serverWiList2);
		Assert.assertEquals(8, serverWiList2.size());

		Assert.assertEquals(1, picker2.countActiveJobs());
		WorkInstruction wi = picker2.nextActiveWi();

		int button = picker2.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D-76", wi.getPickInstruction());

		//picker2.simulateCommitByChangingTransaction(this.persistenceService);

		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testCartSetupFeedback() throws IOException {
		// Test cases:
		// 1. Two good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. There is only a case pick order for position 5. Currently will give a work instruction if the case is in inventory.
		// set up data for pick scenario

		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 11111 has two items in stock (Item 1 and Item 2)
		// Order 22222 has 1 item in stock (Item 1) and 1 immediate short (Item 5 which is out of stock)
		// Order 44444 has an immediate short (Item 5 which is out of stock)
		// Order 55555 has a each pick for an item that only has a case (Item 6)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,9,5,6,Test Item 6,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a6,a6,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Start setting up cart etc
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		//Make sure we have 4 orders/containers
		Assert.assertEquals(5, containers.size());

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("a6", "6");

		//Check that container show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		this.getTenantPersistenceService().commitTransaction();

		// note no transaction active in test thread here - transactions will be opened by server during simulation

		picker.scanCommand("START");

		//Check State Make sure we do not hit REVIEW
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);

		LOGGER.info("Case 1: 1 good pick no flashing");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);
		//Make sure other position is null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		//Make this order complete
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pick(6, 1);
		picker.waitForCheState(picker.getCompleteState(), 3000);

		//Reset Picker
		picker.logout();
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Continue setting up containers with bad counts");
		picker.setupOrderIdAsContainer("a1111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("9", "5");
		picker.setupOrderIdAsContainer("a6", "6");

		//Quickly check assigment feedback
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), Byte.valueOf("22"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), Byte.valueOf("33"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), Byte.valueOf("44"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);

		//Pos 5 should have "09"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[9]);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[0]);

		LOGGER.info("Starting pick");

		picker.scanCommand("START");

		//Check State
		picker.waitForCheState(picker.getLocationStartReviewState(true), 3000);

		//Check Screens
		//Case 1: 2 good picks - solid , bright
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 2);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//Case 2: 1 good pick flashing, bright due to immediate short
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.BLINK_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 5: Each pick on a case pick which is an immediate short display solid, bright 1
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 5), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 5), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid, "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		//Scan location to make sure position controller does not show counts anymore
		picker.scanLocation("D301");

		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//Make sure all position controllers are cleared - except for case 3,4,6 since they are zero and 2 since that is the first task
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid oc
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		this.getTenantPersistenceService().beginTransaction();
		propertyService.restoreHKDefaults(facility);
		this.getTenantPersistenceService().commitTransaction();
	}

	//DEV-603 test case
	@Test
	public final void testCartSetupFeedbackWithPreviouslyShortedWI() throws IOException {

		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,1,1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		PickSimulator picker = createPickSim(cheGuid1);

		//SETUP
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SHORT FIRST ITEM
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(1, 1);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker.scanLocation("D301");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Make sure we have a bright 1 on the poscon
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//COMPLETE FIRST ITEM
		picker.pick(1, 1);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(picker.getLocationStartReviewState(), 3000);
		picker.scanLocation("D301");
		//picker.simulateCommitByChangingTransaction(this.persistenceService);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		propertyService.restoreHKDefaults(facility);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void noInventoryCartRunFeedback() throws IOException {
		// One good result for this, so the cart has something to run. And one no inventory.
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		propertyService.turnOffHK(facility);
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);

		this.getTenantPersistenceService().commitTransaction();
		this.startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "4");
		picker.startAndSkipReview("D301", 3000, 3000);

		// No item for 44444, therefore nothing good happened. If it autoshorts, it would show double dash. If no short made, then it shows
		// single dash. As of April 2015, doAutoShortInstructions() is hard coded to false in WorkService. It does not use AUTOSHRT parameter.
		// Therefore, we expect poscon 4 to get the single dash, instead of the double short dash.

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

	}

	@Test
	public final void testCartRunFeedback() throws IOException {
		// Test cases:
		// 1. One good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. One good plans for position 5.
		// set up data for pick scenario

		// set up data for pick scenario
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		// Start setting up cart etc
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		//Make sure we have 4 orders/containers
		Assert.assertEquals(4, containers.size());

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("55555", "5");
		picker.startAndSkipReview("D301", 3000, 3000);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Check Screens -- Everything should be clear except the one we are picked #1, #4 immediate short and #3 unknown order id
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Make sure position 1 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);

		// Look at the screen
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("D301", line1);
		Assert.assertEquals("1", line2);
		Assert.assertEquals("QTY 1", line3); // This may change soon. Just update this line.
		Assert.assertEquals("", line4);

		picker.pick(1, 1);
		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);
		//5 should stay null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		// Look at the screen
		line1 = picker.getLastCheDisplayString(1);
		line3 = picker.getLastCheDisplayString(3);
		Assert.assertEquals("D302", line1);
		// Important: this next line shows DEV-691 result. Two picks in a row from same spot, so total for that SKU is 2, not 1.
		Assert.assertEquals("QTY 2", line3); // This may change soon. Just update this line.

		/**
		 * Now we will do a short pick and cancel it and make sure we never lose feedback.
		 * Then will redo the short and confirm it and make sure we keep the feedback
		 */
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("NO");

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(picker.getCompleteState(), 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should now also be shorted ahead so display dim, solid, double dash
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//2 is now shorted so display dim, solid, double dash
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		// Case 4: Had no inventory. Does not autoshort, so single dash for detail-no-WI
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		// Look at the screen
		// TODO Check out the SUMMARY screen

		propertyService.restoreHKDefaults(facility);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testContainerReassignmentDuringCHESetup() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		//Check Screens
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == (byte) 11);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		picker.setupOrderIdAsContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		//Check Screens
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == (byte) 11);

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.restoreHKDefaults(facility);
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public void testCheSetupErrors() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();

		// Start setting up cart etc
		this.getTenantPersistenceService().beginTransaction();

		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");

		//CASE 1: Scan 2 containers in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);
		picker.scanOrderId("44444");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 1000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Reset
		picker.logout();
		picker.loginAndSetup("Picker #1");

		//CASE 2: Scan 2 positions in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.scanPosition("3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECTION_INVALID, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == Byte.valueOf("22"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 3: SCAN A TAKEN POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("1");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_IN_USE, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 4: SCAN START WORK AFTER CONTAINER W/ NO POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we got an error
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure scanning something random doesn;t change the state
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we still got an error codes
		assertPositionHasErrorCode(picker, (byte) 1);
		assertPositionHasErrorCode(picker, (byte) 2);
		assertPositionHasErrorCode(picker, (byte) 3);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Make sure CLEAR_ERROR again does nothing
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		propertyService.restoreHKDefaults(facility);

		this.getTenantPersistenceService().commitTransaction();
	}

	private void assertWIColor(WorkInstruction wi, Che che) {
		List<LedCmdGroup> cmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wi.getLedCmdStream());
		Assert.assertEquals(1, cmdGroups.size());
		ColorEnum wiColor = cmdGroups.get(0).getLedSampleList().get(0).getColor();
		ColorEnum cheColor = che.getColor();
		Assert.assertEquals(cheColor, wiColor);

	}

	private void assertPositionHasErrorCode(PickSimulator picker, byte position) {
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue(position) == PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayFreq(position) == PosControllerInstr.SOLID_FREQ);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayDutyCycle(position) == PosControllerInstr.MED_DUTYCYCLE);
		Assert.assertTrue(picker.getLastSentPositionControllerMaxQty(position) == PosControllerInstr.BITENCODED_LED_BLANK);
		Assert.assertTrue(picker.getLastSentPositionControllerMinQty(position) == PosControllerInstr.BITENCODED_LED_E);

	}

	@Test
	public void getDefaultProcessMode() {
		this.getTenantPersistenceService().beginTransaction();
		UiUpdateService service = new UiUpdateService();
		Facility facility = Facility.createFacility("F1", "facf1", Point.getZeroPoint());
		CodeshelfNetwork network = facility.createNetwork("WITEST");
		Che che = network.createChe("0x00000004", new NetGuid("0x00000004"));

		//Get default mode in a facility without aisles
		ProcessMode processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected Line_Scan as default process mode in a facility with no aisles",
			processMode,
			ProcessMode.LINE_SCAN);

		//Get default mode in a facility with aisles
		Aisle aisle = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint().add(5.0, 0.0));
		Aisle.staticGetDao().store(aisle);
		processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected Setup_Orers as default process mode in a facility with aisles",
			processMode,
			ProcessMode.SETUP_ORDERS);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void basicSimulPick() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku1,Test Item 1,1,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku3,Test Item 3,1,each,LocB,2"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku2,Test Item 2,4,each,LocC,4"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku1,Test Item 1,2,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku4,Test Item 4,1,each,LocD,3"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku2,Test Item 2,5,each,LocC,4";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set WORKSEQR, turn off housekeeping, set PICKMULT");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
		this.startSiteController(); // after all the parameter changes

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1b: setup two orders, that will have 3 work instructions. The first two are same SKU/Location so should be done as simultaneous WI ");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "2");
		picker.setupOrderIdAsContainer("22222", "3");

		/*
		 * 6 order lines above will yield 6 work instructions
		 * Simultaneous sequence 1, Sku1 in LocA. Count 1 on poscon 1. Count 2 on poscon 2.
		 * Sequence 2 is Sku3 in LocB. Count 1 on poscon 1.
		 * Sequence 3 is Sku4 in LocD. Count 1 on poscon 3.
		 * Simultaneous sequence 4, Sku2 in LocC. Count 4 on poscon 3. Count 5 on poscon 1.
		 */

		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("1c: Screen shows the location, SKU, and total to pick");
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 3", line3);
		Assert.assertEquals("", line4);

		LOGGER.info("1d: see that both poscons show their pick count: 1 and 2");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());

		// bug was here
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("2a: Complete the second poscon first");
		picker.pick(2, 2);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("2b: see that poscons 1 count remains, and 2 is now oc. 3 still null/blank");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("2c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("3a: Complete the first poscon");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("3b: Poscon 1 gets the next job, poscon 2 remains oc. 3 still null/blank");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		LOGGER.info("3c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocB", line1);
		Assert.assertEquals("Sku3", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("4a: Complete the job on first poscon");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("4b: 3 gets the next jobs, poscon 2 remains oc. 1 is blank");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);

		LOGGER.info("4c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocD", line1);
		Assert.assertEquals("Sku4", line2);
		Assert.assertEquals("QTY 1", line3);

		LOGGER.info("5a: Complete the job on first poscon. Now 1 is blank. Next job on 3");
		picker.pick(1, 5);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());

		LOGGER.info("5b: Complete the job on poscon3. Last jobs are simultaneous 1,3");
		picker.pick(3, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("6a: Poscon 1 and 3 gets the next jobs, poscon 2 remains oc.");
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		// same bug was here
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);

		LOGGER.info("6b: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocC", line1);
		Assert.assertEquals("Sku2", line2);
		Assert.assertEquals("QTY 9", line3);
	}

	@Test
	public final void simulPickShort() throws IOException {
		// Bug?  should be BLINK_FREQ
		final Byte kBLINK_FREQ = PosControllerInstr.BRIGHT_DUTYCYCLE;
		final Byte kSOLID_FREQ = PosControllerInstr.SOLID_FREQ;

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku1,Test Item 1,1,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,11111,11111,Sku2,Test Item 2,4,each,LocC,2"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku1,Test Item 1,2,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,44444,44444,Sku2,Test Item 2,3,each,LocC,2"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku1,Test Item 1,5,each,LocA,1"
				+ "\r\n1,USF314,COSTCO,22222,22222,Sku2,Test Item 2,6,each,LocC,2";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1a: leave LOCAPICK off, set WORKSEQR, turn off housekeeping, set PICKMULT");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.PICKMULT, Boolean.toString(true));
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());
		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
		this.startSiteController(); // after all the parameter changes

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("1b: setup two orders, that will have 3 work instructions. The first two are same SKU/Location so should be done as simultaneous WI ");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("44444", "2");
		picker.setupOrderIdAsContainer("22222", "3");

		/*
		 * 6 order lines above will yield 6 work instructions in 2 simulpick groups of 3
		 * Simultaneous sequence 1, Sku1 in LocA. Count 1 on poscon 1. Count 2 on poscon 2. Count 5 on poscon 3.
		 * Simultaneous sequence 2, Sku2 in LocC. Count 4 on poscon 1. Count 3 on poscon 2. Count 6 on poscon 3.
		 */

		picker.scanCommand("START");
		picker.waitForCheState(picker.getLocationStartReviewState(), 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("1c: Screen shows the location, SKU, and total to pick");
		String line1 = picker.getLastCheDisplayString(1);
		String line2 = picker.getLastCheDisplayString(2);
		String line3 = picker.getLastCheDisplayString(3);
		String line4 = picker.getLastCheDisplayString(4);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 8", line3);
		Assert.assertEquals("", line4);

		LOGGER.info("1d: see that both poscons show their pick count: 1 and 2");
		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());

		LOGGER.info("2a: SHORT now. This should make all 3 poscons flash on their number");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);

		Assert.assertEquals(1, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1));
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2));
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 1));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("2c: Screen shows the location, SKU, and total to pick");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 8", line3);

		LOGGER.info("3a: Complete the first poscon with the full count. State should still be short pick");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);

		LOGGER.info("3b: Poscon 1 goes out. 2 and 3 still flashing.");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kBLINK_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("3c: Screen shows same location, SKU. Total counts down.");
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);

		Assert.assertEquals("LocA", line1);
		Assert.assertEquals("Sku1", line2);
		Assert.assertEquals("QTY 7", line3);

		LOGGER.info("4a: Short poscon 3. What do poscons show then? Counts there? Still flashing?");
		picker.pick(3, 3);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		// 3 went out. 2 also went out. Otherwise misleading to user.
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		LOGGER.info("4b: Say no to confirm. Should be back at DO_PICK. Nothing flashing");
		line2 = picker.getLastCheDisplayString(2);
		Assert.assertEquals(line2, CheDeviceLogic.YES_NO_MSG);
		picker.scanCommand("NO");
		// Comes to DO_PICK. One could argue it should remain at SHORT_PICK. but behavior is consistent with single short case.
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals(2, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(5, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 2));
		Assert.assertEquals(kSOLID_FREQ, picker.getLastSentPositionControllerDisplayFreq((byte) 3));

		LOGGER.info("5a: This time short and confirm");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 4000);
		picker.pick(3, 3);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 4000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("5b: Poscon 3 job should have shorted. Poscon 2 should have shorted ahead. We should be on to the next simultaneous jobs");
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(3, picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue());
		Assert.assertEquals(6, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		line1 = picker.getLastCheDisplayString(1);
		line2 = picker.getLastCheDisplayString(2);
		line3 = picker.getLastCheDisplayString(3);
		Assert.assertEquals("LocC", line1);
		Assert.assertEquals("Sku2", line2);
		Assert.assertEquals("QTY 13", line3);

		LOGGER.info("6a: Just work these off");
		picker.pick(2, 3);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		// brief diversion to see poscon states. shorted 2 should be double dashed now. 1 and 3 with active picks
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(4, picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue());
		Assert.assertEquals(6, picker.getLastSentPositionControllerDisplayValue((byte) 3).intValue());
		picker.pick(1, 4);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pick(3, 6);
		picker.waitForCheState(picker.getCompleteState(), 4000);

		LOGGER.info("6b: See if the shorted and shorted-ahead poscons show double dash, and the one good one is oc");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_TOP_BOTTOM);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);

	}

	/**
	 * This test verifies that the "bay change" displays disappears after the button is pressed,
	 * and a different CHE poscon displays the quantity for the next pick
	 */
	@Test
	public final void testBayChangeDisappearTest() throws IOException {
		LOGGER.info("1: setup facility");
		this.getTenantPersistenceService().beginTransaction();
		setUpOneAisleFourBaysFlatFacilityWithOrders();
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();

		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: assign two identical two-item orders to containers on the CHE");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("7", "1");
		picker.setupOrderIdAsContainer("8", "2");

		LOGGER.info("3: verify 'Location Select' and work on containers");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), 4000);
		Byte posConValue1 = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("2"), posConValue1);
		Byte posConValue2 = picker.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("2"), posConValue2);

		LOGGER.info("4: verify generated instructions");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item2", "Item2", "Housekeeping", "Item6", "Item6" };
		compareInstructionsList(wiList, expectations);

		LOGGER.info("5: pick two items before bay change");
		picker.pick(2, 40);
		picker.pick(1, 40);

		LOGGER.info("6: verify 'bc' code on position 1; then - press button to advance");
		Assert.assertEquals(PosControllerInstr.BAY_COMPLETE_CODE, picker.getLastSentPositionControllerDisplayValue((byte) 1));
		picker.buttonPress(1, 0);

		// The main purpose of the test.
		LOGGER.info("7: verify the correct quantity on position 2, and nothing on position 1");
		Assert.assertEquals(new Byte("30"), picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));

		LOGGER.info("8: wrap up the test by finishing both orders");
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.waitForCheState(picker.getCompleteState(), 4000);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
	}

}
