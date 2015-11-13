/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014-2015, Codeshelf, All rights reserved
 *  file CheProcessTestPick.java
 *******************************************************************************/
package com.codeshelf.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.ThreadContext;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityMetric;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.validation.InputValidationException;
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
		LOGGER.info("START zigzag aisles");

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
		beginTransaction();
		importAislesData(getFacility(), csvAisles);
		commitTransaction();

		LOGGER.info("END zigzag aisles");

		// Get the aisle
		beginTransaction();
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);
		commitTransaction();

		LOGGER.info("START location aliases");

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
				+ "A2.B2.T5.S5,D-1\r\n";	

		beginTransaction();
		Facility fac = getFacility().reload();
		importLocationAliasesData(fac, csvAliases);
		commitTransaction();

		LOGGER.info("END location aliases");

		beginTransaction();
		fac = fac.reload();

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("1", new NetGuid("0x00000011"));

		Short channel1 = 1;
		Location aisle1x = fac.findSubLocationById("A1");
		controller1.addLocation(aisle1x);
		aisle1x.setLedChannel(channel1);
		aisle1x.getDao().store(aisle1x);

		Location aisle2x = fac.findSubLocationById("A2");
		controller1.addLocation(aisle2x);
		aisle2x.setLedChannel(channel1);
		aisle2x.getDao().store(aisle2x);
		commitTransaction();

		return fac;
	}

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

	private void setUpBatchOrdersForZigzag(Facility inFacility) throws IOException {
		// Setting up containers 2,3,7,11 to match the bug

		// Outbound orders
		beginTransaction();
		String csvOrders = "orderGroupId,orderId,itemId,description,quantity,uom\r\n" //
				+ "5/26/14,1001dry,53a8a03ab38e3c0200000330,vitalvittles Organic Flax-Seed Oat Bread,2,loaf\r\n" //
				+ "5/26/14,1003dry,539f2da2622fcc0200001009,sayhayfarms Organic Sungold Cherry Tomatoes,4,pint\r\n" //
				+ "5/26/14,1006dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,7,bag\r\n" //
				+ "5/26/14,1007dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,8,bag\r\n" //
				+ "5/26/14,1016dry,50916c6dd136890200000311,blackjet Crack*a*Roons,17,cookies\r\n"; //
		importOrdersData(inFacility, csvOrders);
		inFacility = Facility.staticGetDao().reload(inFacility);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(inFacility, "1001dry");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detailCount);
		commitTransaction();

		// Slotting
		beginTransaction();
		String csvSlotting = "orderId,locationId\r\n" //
				+ "1001dry,D-26\r\n" + "1001dry,D-27\r\n" + "1001dry,D-28\r\n" + "1001dry,D-29\r\n"
				+ "1001dry,D-30\r\n"
				+ "1001dry,D-31\r\n" + "1001dry,D-32\r\n" + "1001dry,D-33\r\n" + "1001dry,D-34\r\n"
				+ "1001dry,D-35\r\n"
				+ "1003dry,D-22\r\n" + "1006dry,D-100\r\n" + "1016dry,D-76\r\n" + "1007dry,D-99\r\n";
		inFacility = Facility.staticGetDao().reload(inFacility);
		importSlotting(inFacility, csvSlotting);
		commitTransaction();

		// Batches file. Only containers 2,3,7,11
		beginTransaction();
		inFacility = Facility.staticGetDao().reload(inFacility);
		String csvBatch = "itemId,orderGroupId,containerId,description,quantity,uom\r\n" //
				+ "539f2da2622fcc0200001009,5/26/14,2,sayhayfarms Organic Sungold Cherry Tomatoes,1,pint\r\n" //
				+ "53a8a03ab38e3c0200000330,5/26/14,3,vitalvittles Organic Flax-Seed Oat Bread,1,loaf\r\n" //
				+ "50916c6dd136890200000311,5/26/14,7,blackjet Crack*a*Roons,2,cookies\r\n" //
				+ "5266bd1e4d5eed0200000155,5/26/14,11,firebrand Pretzel Croutons,7,bag\r\n";//
		inFacility = Facility.staticGetDao().reload(inFacility);
		importBatchData(inFacility, csvBatch);
		commitTransaction();
	}

	@Test
	public final void testStartWorkReverse() throws IOException {
		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		for (Entry<String, String> entry : ThreadContext.getContext().entrySet()) {
			LOGGER.info("ThreadContext: {} = {}", entry.getKey(), entry.getValue());
		}
		this.startSiteController();

		//For this data set
		//Forward ordering is 3,2,1
		// Reverse ordering is 1,2,3
		startReverseWork(facility);
	}

	//@Test
	public final void testStartWorkReverseSkipToLocation() throws IOException {
		// JR: not sure what this test used to do. Does not work

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		PickSimulator picker = startReverseWork(facility);

		//Then skip to location
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

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
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,2,2,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,3,3,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// Start setting up cart etc
		beginTransaction();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.setupOrderIdAsContainer("2", "2");
		picker.setupOrderIdAsContainer("3", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

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
		beginTransaction();
		facility = facility.reload();
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,2,2,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,3,3,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvString2);

		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.setupOrderIdAsContainer("2", "2");
		picker.setupOrderIdAsContainer("3", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

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
		Facility facility = setUpSimpleNoSlotFacility();

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

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

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
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		// Turn off housekeeping work instructions so as to not confuse the counts
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();
		beginTransaction();
		facility = facility.reload();
		// Set up a cart for order 12345, which will generate work instructions
		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		workService.setUpCheContainerFromString(che1, "12345");
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		che1 = Che.staticGetDao().reload(che1);
		List<WorkInstruction> aList = workService.getWorkInstructions(che1, "");
		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // 3, but one should be short. Only 1123 and 1522 find each inventory

		for (WorkInstruction workInstruction : aList) {
			Assert.assertEquals(OrderStatusEnum.INPROGRESS, workInstruction.getOrderDetail().getStatus());
		}
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.restoreHKDefaults(facility); // set it back
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
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

		commitTransaction();

		beginTransaction();
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

		commitTransaction();
	}

	@Test
	public final void testPickViaChe() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

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
				+ "1522,D503,SJJ BPP,1,case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

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
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.restoreHKDefaults(facility);
		commitTransaction();

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).byteValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.BLINK_FREQ);

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

		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);
		picker.logout();
	}

	@Test
	public final void testCheProcess1() throws IOException {
		// Test cases:
		// 1) If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)
		// 2) A happy-day pick startup. No housekeeping jobs.
		// Case 3: A happy-day short, with one short-ahead");
		// Case 4: Short and cancel leave you on the same job");
		// Case 5: Inappropriate location scan, then normal button press works");

		// set up data for pick scenario
		Facility facility = setUpSimpleNoSlotFacility();

		setUpSmallInventoryAndOrders(facility);

		// Verify only two containers made
		beginTransaction();
		facility = facility.reload();
		List<Container> containers = Container.staticGetDao().findByParent(facility);
		Assert.assertEquals(2, containers.size());
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		this.startSiteController();
		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		// This brief case covers and allows retirement of CheSimulationTest.java
		LOGGER.info("Case 1: If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)");
		picker.setupContainer("9x9x9", "1"); // unknown container
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 5000);

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
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanLocation("BAD_LOCATION");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);

		// Change from bad location to a resolved location on a path is interpretted as a path change. Therefore
		// to the summary state and not directly to pick.
		// Note: this fails if not in usesSummaryState()
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		firstLine = picker.getLastCheDisplayString(1);
		Assert.assertEquals("D303", firstLine);

		LOGGER.info("List the work instructions as the server sees them");

		beginTransaction();
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(7, picker.countRemainingJobs());
		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();
		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);

		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		LOGGER.info("first wi button: {} quant:{}", button, quant);
		commitTransaction();

		beginTransaction();
		wi = WorkInstruction.staticGetDao().reload(wi);

		// Does pos 1 show some sort of order feedback? No.
		Byte valuePos1 = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		LOGGER.info("pos1 display value: {}", valuePos1);
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
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) button), PosControllerInstr.BLINK_FREQ);

		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(6, picker.countRemainingJobs());
		commitTransaction();

		beginTransaction();

		LOGGER.info("Case 3: A happy-day short, with one short-ahead");
		wi = picker.nextActiveWi();
		commitTransaction();

		beginTransaction();
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
		commitTransaction();

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
		beginTransaction();
		List<WorkInstruction> serverWiList2 = picker.getCurrentWorkInstructionsFromList(serverWiList);
		logWiList(serverWiList2);
		// In this, we see 2nd wi is user short, and third a short ahead. Item 1555 should have got an immediate short.
		WorkInstruction userShortWi = serverWiList2.get(1);
		WorkInstruction shortAheadWi = serverWiList2.get(2);

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

		Assert.assertNotNull(userShortWi);
		Assert.assertNotNull(shortAheadWi);

		logOneWi(userShortWi);
		logOneWi(shortAheadWi);
		// All should have the same assign time
		Assert.assertEquals(shortAheadWi.getAssigned(), userShortWi.getAssigned());

		PropertyBehavior.restoreHKDefaults(facility);

		commitTransaction();
	}

	@Test
	public final void testRouteWrap() throws IOException {
		// create test data
		Facility facility = setUpSimpleNoSlotFacility();
		setUpSmallInventoryAndOrders(facility);

		this.startSiteController();

		// perform pick operation
		beginTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we need to test removing the bay change just prior to the wrap point.

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Case 1: Scan on near the end of the route. Only 3 of 7 jobs left. (There are 3 housekeeping). So, with route-wrap, 10 jobs");

		picker.setupContainer("12345", "1");
		picker.setupContainer("11111", "2");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.startAndSkipReview("D301", 5000, 3000);
		PropertyBehavior.restoreHKDefaults(facility);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).byteValue(), (byte) 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.BLINK_FREQ);
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
	public final void twoChesCrossBatch() throws IOException {
		// Reproduce DEV-592 seen during MAT for v10
		// And incorporate testRouteWrap2 as the second part of this test, that checks basic wrapping
		// This test runs a little long, but  remember is is really two tests. Slow part is setting up the aisles.
		Facility facility = setUpZigzagSlottedFacility();

		setUpBatchOrdersForZigzag(facility);

		this.startSiteController();

		// perform pick operation
		beginTransaction();
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

		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
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

		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
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
		commitTransaction();
		
		// This carries on as the old routewrap2 test did

		// D-76 is interesting. Actually last tier on the path in that tier, so our code normalizes back the the bay posAlongPath.
		// D-76 comes up first in the list compared to the other two in that bay only because it has the top tier location and we sort top down.
		picker = null; // to force NPE if we call it below

		// pick first item. 7 left (3 housekeeps)
		picker2.pick(button, quant);
		picker2.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(7, picker2.countRemainingJobs());

		LOGGER.info("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker2.nextActiveWi();
		button = picker2.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker2.pick(button, quant);
		picker2.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(6, picker2.countRemainingJobs());
		Assert.assertEquals("D-100", wi.getPickInstruction());

		// fourth job
		wi = picker2.nextActiveWi();
		button = picker2.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker2.pick(button, quant);
		Assert.assertEquals(5, picker2.countRemainingJobs());
		Assert.assertEquals("", wi.getPickInstruction()); // a housekeep

		// fifth job
		wi = picker2.nextActiveWi();
		button = picker2.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker2.pick(button, quant);
		Assert.assertEquals("D-99", wi.getPickInstruction());

	}





	@Test
	public void testContainerReassignmentDuringCHESetup() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		this.startSiteController();

		beginTransaction();
		facility = facility.reload();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

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

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.restoreHKDefaults(facility);
		commitTransaction();

	}

	private void assertWIColor(WorkInstruction wi, Che che) {
		List<LedCmdGroup> cmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wi.getLedCmdStream());
		Assert.assertEquals(1, cmdGroups.size());
		ColorEnum wiColor = cmdGroups.get(0).getLedSampleList().get(0).getColor();
		ColorEnum cheColor = che.getColor();
		Assert.assertEquals(cheColor, wiColor);

	}

	@Test
	public void testDefaultProcessMode() {
		beginTransaction();
		UiUpdateBehavior service = new UiUpdateBehavior();
		Facility facility = getFacility();
		CodeshelfNetwork network = this.getNetwork();
		Che che = network.createChe("0x00000004", new NetGuid("0x00000004"));

		// Get default mode in a facility without aisles
		// Change v24. no-aisle facility used to yield LINE_SCAN. Now setup orders.
		ProcessMode processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected SETUP_ORDERS as default process mode in a facility with no aisles",
			processMode,
			ProcessMode.SETUP_ORDERS);
		// Instead of this mechanism, might want new CHE to be the modal value among existing CHE.

		//Get default mode in a facility with aisles
		Aisle aisle = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint().add(5.0, 0.0));
		Aisle.staticGetDao().store(aisle);
		processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected Setup_Orers as default process mode in a facility with aisles",
			processMode,
			ProcessMode.SETUP_ORDERS);
		commitTransaction();
	}

	/**
	 * In case of re-uploading an order with changed details, forget prok previously done on those details
	 */
	@Test
	public final void testDetailModificationAfterSomeWorkDone() throws IOException {
		setUpOneAisleFourBaysFlatFacilityWithOrders();

		beginTransaction();
		PropertyBehavior.turnOffHK(getFacility());
		commitTransaction();
		
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		LOGGER.info("1: Load order onto CHE. Verify that there are 7 items on the path");
		picker.loginAndSetup("Picker1");
		picker.setupContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 400000);
		Assert.assertEquals("7 jobs", picker.getLastCheDisplayString(2).trim());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		LOGGER.info("2: Pick first 2 items, that will later be modified");
		WorkInstruction wi = picker.getActivePick();
		Assert.assertEquals("Item2", wi.getItemId());
		Assert.assertEquals(22, (int) wi.getPlanQuantity());
		picker.pick(1, 22);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		wi = picker.getActivePick();
		Assert.assertEquals("Item3", wi.getItemId());
		Assert.assertEquals(21, (int) wi.getPlanQuantity());
		picker.pick(1, 22);
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		LOGGER.info("3: Pick the 3rd item. It will not be changed, so don't worry about what it is.");
		picker.pickItemAuto();
		picker.logout();

		LOGGER.info("4: Set up the order again and re-calc work. Make sure only 4 items remain");
		picker.loginAndSetup("Picker1");
		picker.setupContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("4 jobs", picker.getLastCheDisplayString(2).trim());
		picker.logout();

		beginTransaction();
		LOGGER.info("5: Upload the order file again, modifying 2 already picked items");
		//Upload order file, modifying 2 already picked order details: changing uom in the first (349) and the item master in the second (351)
		//351 already had a preferred location, so CHE will know where to look for the new item.
		//Still need to add location to 349, so that the changed item will still be on the path
		String updatedOrder = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,workSequence,locationId\r\n"
				+ "1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1,,\r\n"
				+ "1,1,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1,,\r\n"
				+ "1,1,347,12/03/14 12:00,12/31/14 12:00,Item11,,120,a,Group1,,\r\n"
				+ "1,1,348,12/03/14 12:00,12/31/14 12:00,Item9,,11,a,Group1,,\r\n"
				+ "1,1,349,12/03/14 12:00,12/31/14 12:00,Item2,,22,cs,Group1,,LocX24\r\n"
				+ "1,1,350,12/03/14 12:00,12/31/14 12:00,Item5,,33,a,Group1,,\r\n"
				+ "1,1,351,12/03/14 12:00,12/31/14 12:00,Item1,,22,a,Group1,5,LocX24\r\n";
		importOrdersData(getFacility(), updatedOrder);

		LOGGER.info("6: Ensure that the currect Detail-WI-Mismatch events were created due to some work aldeady been done to the changed details");
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", getFacility()));
		filterParams.add(Restrictions.eq("eventType", WorkerEvent.EventType.DETAIL_WI_MISMATCHED));
		List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
		Assert.assertEquals(2, events.size());
		ArrayList<String> expectedEvents = new ArrayList<>();
		expectedEvents.add("OrderDetail 349 changed from Item2-a to Item2-cs. Already picked 22 items.");
		expectedEvents.add("OrderDetail 351 changed from Item3-a to Item1-a. Already picked 22 items.");
		for (WorkerEvent event : events) {
			Assert.assertTrue(expectedEvents.contains(event.getDescription()));
			expectedEvents.remove(event.getDescription());
		}
		commitTransaction();

		LOGGER.info("7: Set up cart and verify that there are now 6 items on the path");
		picker.loginAndSetup("Picker1");
		picker.setupContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("6 jobs", picker.getLastCheDisplayString(2).trim());
		picker.logout();
	}

	/**
	 * The intent is to setup the cart before orders are loaded
	 * PFSWeb asked/complained about this feature 
	 */
	@Test
	public void testCartSetupBeforeOrder() throws Exception {

		LOGGER.info("1: Set up facility. Add the export extensions");
		// somewhat cloned from FacilityAccumulatingExportTest
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");

		commitTransaction();

		LOGGER.info("2: Load only one relevant order. No inventory, so uses locationA, etc. as the location-based pick");
		beginTransaction();
		facility = facility.reload();

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n55555,55555,2,Test Item 2,1,each,locationA,20";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("3a: Set up order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");

		LOGGER.info("3b: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");

		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");

		LOGGER.info("4: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		List<WorkInstruction> wiList = picker.getAllPicksList();
		Assert.assertEquals(1, wiList.size());

		LOGGER.info("5: Now load orders 22222 and 44444");
		beginTransaction();
		facility = facility.reload();

		String csvOrders2 = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n22222,22222,2,Test Item 2,1,each,locationB,20" //
				+ "\r\n22222,22222,3,Test Item 3,1,each,locationC,30" //				
				+ "\r\n44444,44444,5,Test Item 5,1,each,locationD,500";
		importOrdersData(facility, csvOrders2);
		commitTransaction();

		LOGGER.info("6: Pick the one and only job");
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		
		LOGGER.info("7: Start, which will find the other jobs");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		
		wiList = picker.getAllPicksList();
		Assert.assertEquals(3, wiList.size());
	}
	
	@Test
	public final void testFacilityMetric() throws Exception {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.SCANPICK, "UPC");
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");

		LOGGER.info("1: Import orders");
		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1111,1111,Item 1,Item Descr 1,1,each,locationA,1" //
				+ "\r\n2222,2222,Item 2,Item Descr 2,2,ea,locationB,2" //
				+ "\r\n2222,2222,Item 3,Item Descr 3,3,PK,locationC,3" //
				+ "\r\n3333,3333,Item 4,Item Descr 4,4,cs,locationD,4" //
				+ "\r\n3333,3333,Item 5,Item Descr 5,5,case,locationE,5" //
				+ "\r\n3333,3333,Item 6,Item Descr 6,6,uom1,locationF,6" //
				+ "\r\n4444,4444,Item 7,Item Descr 7,7,uom2,locationA,7";
		importOrdersData(facility, csvOrders);
		commitTransaction();
		beginTransaction();
		commitTransaction();
		
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("2: Load orders on cart");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("1111", "1");
		picker.setupOrderIdAsContainer("2222", "2");
		picker.setupOrderIdAsContainer("3333", "3");
		picker.setupOrderIdAsContainer("4444", "4");
		
		LOGGER.info("3: Compute work");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("4 orders\n7 jobs\n\nSTART (or SETUP)\n", picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("4: Pick orders, with 1 short and 2 skip-scans");
		//Item 1
		picker.scanSomething("Item 1");
		picker.pickItemAuto();
		//Item 2
		picker.logCheDisplay();
		picker.scanSomething("SKIPSCAN");
		picker.logCheDisplay();
		picker.pickItemAuto();
		//Housekeeping
		picker.pickItemAuto();
		//Item 3
		picker.scanSomething("SKIPSCAN");
		picker.pickItemAuto();
		//Item 4
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000);
		picker.scanCommand("YES");
		//Housekeeping
		picker.pickItemAuto();
		//Item 5
		picker.scanSomething("Item 5");
		picker.pickItemAuto();
		//Housekeeping
		picker.pickItemAuto();
		//Item 6
		picker.scanSomething("Item 6");
		picker.pickItemAuto();
		//Item 7
		picker.scanSomething("Item 7");
		picker.pickItemAuto();
		
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		ThreadUtils.sleep(1500);
		
		beginTransaction();
		LOGGER.info("5: Generate metrics for the day");
		FacilityResource facilityResourse = new FacilityResource(workService, null, null, webSocketManagerService, null, null, null, null, null, null);
		facilityResourse.setFacility(facility);
		Response response = facilityResourse.computeMetrics(null, true);
		Assert.assertEquals(200, response.getStatus());
		
		LOGGER.info("6: Retrieve and verify metrics");
		@SuppressWarnings("unchecked")
		List<FacilityMetric> metricList = (List<FacilityMetric>)facilityResourse.getMetrics().getEntity();
		Assert.assertEquals(1, metricList.size());
		FacilityMetric metric = metricList.get(0);
		Assert.assertEquals(4, (int)metric.getOrdersPicked());
		Assert.assertEquals(24, (int)metric.getCountPicked());
		//Each = 'each' + 'ea' + 'pk'
		Assert.assertEquals(6, (int)metric.getCountPickedEach());
		Assert.assertEquals(5, (int)metric.getCountPickedCase());
		Assert.assertEquals(13, (int)metric.getCountPickedOther());
		Assert.assertEquals(7, (int)metric.getLinesPicked());
		Assert.assertEquals(3, (int)metric.getLinesPickedEach());
		Assert.assertEquals(2, (int)metric.getLinesPickedCase());
		Assert.assertEquals(2, (int)metric.getLinesPickedOther());
		Assert.assertEquals(3, (int)metric.getHouseKeeping());
		Assert.assertEquals(1, (int)metric.getShortEvents());
		Assert.assertEquals(2, (int)metric.getSkipScanEvents());

		commitTransaction();
	}

	@Test
	public final void testOrdersub() throws Exception {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");
		
		LOGGER.info("1: Verify that invalid ORDERSUB values are not accepted");
		Assert.assertEquals("Disabled", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		assertInvalidPropertyValue(facility, FacilityPropertyType.ORDERSUB, "xxxx");
		Assert.assertEquals("Disabled", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		assertInvalidPropertyValue(facility, FacilityPropertyType.ORDERSUB, "4 5");
		Assert.assertEquals("Disabled", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		assertInvalidPropertyValue(facility, FacilityPropertyType.ORDERSUB, "-4 - 5");
		Assert.assertEquals("Disabled", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		assertInvalidPropertyValue(facility, FacilityPropertyType.ORDERSUB, "6 - 5");
		Assert.assertEquals("Disabled", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		
		LOGGER.info("2: Set valid ORDERSUB");
		PropertyBehavior.setProperty(facility, FacilityPropertyType.ORDERSUB, "5 - 8");
		Assert.assertEquals("5-8", PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB));
		

		LOGGER.info("3: Import order");
		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n1111,1111,Item 1,Item Descr 1,1,each,locationA,1" //
				+ "\r\n1111,1111,Item 2,Item Descr 2,2,ea,locationB,2" //
				+ "\r\n1111,1111,Item 3,Item Descr 3,3,PK,locationC,3";
		importOrdersData(facility, csvOrders);
		commitTransaction();
		
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		LOGGER.info("4: Scan barcode conbtaining orderId");
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("xxxx1111xx", "1");
		
		LOGGER.info("5: Verify that the orderId was successfully extracted using the ORDERSUB property");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 order\n3 jobs\n\nSTART (or SETUP)\n", picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);		
	}
	
	private void assertInvalidPropertyValue(Facility facility, FacilityPropertyType type, String propertyValue) {
		try {
			PropertyBehavior.setProperty(facility, type, propertyValue);
			Assert.fail("Test did not throw exception when setting invalid value " + propertyValue + " for property " + type.name());
		} catch (InputValidationException e) {}
	}
}