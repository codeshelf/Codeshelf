/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTestCrossBatch extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestCrossBatch.class);

	public CheProcessTestCrossBatch() {

	}

	// @SuppressWarnings("rawtypes")
	@SuppressWarnings("unused")
	private Facility setUpSimpleSlottedFacility() {
		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n"; //
		Facility facility = getFacility();
		importAislesData(facility, csvString);

		// Get the aisles
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 10.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T2.S5,D-1\r\n" //
				+ "A1.B1.T2.S4,D-2\r\n" //
				+ "A1.B1.T2.S3, D-3\r\n" //
				+ "A1.B1.T2.S2, D-4\r\n" //
				+ "A1.B1.T2.S1, D-5\r\n" //
				+ "A1.B1.T1.S5, D-6\r\n" //
				+ "A1.B1.T1.S4, D-7\r\n" //
				+ "A1.B1.T1.S3, D-8\r\n" //
				+ "A1.B1.T1.S2, D-9\r\n" //
				+ "A1.B2.T1.S1, D-10\r\n" //
				+ "A1.B2.T2.S5, D-21\r\n" //
				+ "A1.B2.T2.S4, D-22\r\n" //
				+ "A1.B2.T2.S3, D-23\r\n" //
				+ "A1.B2.T2.S2, D-24\r\n" //
				+ "A1.B2.T2.S1, D-25\r\n" //
				+ "A1.B2.T1.S5, D-26\r\n" //
				+ "A1.B2.T1.S4, D-27\r\n" //
				+ "A1.B2.T1.S3, D-28\r\n" //
				+ "A1.B2.T1.S2, D-29\r\n" //
				+ "A1.B2.T1.S1, D-30\r\n" //
				+ "A2.B1.T2.S5, D-11\r\n" //
				+ "A2.B1.T2.S4, D-12\r\n" //
				+ "A2.B1.T2.S3, D-13\r\n" //
				+ "A2.B1.T2.S2, D-14\r\n" //
				+ "A2.B1.T2.S1, D-15\r\n" //
				+ "A2.B1.T1.S5, D-16\r\n" //
				+ "A2.B1.T1.S4, D-17\r\n" //
				+ "A2.B1.T1.S3, D-18\r\n" //
				+ "A2.B1.T1.S2, D-19\r\n" //
				+ "A2.B2.T1.S1, D-20\r\n" //
				+ "A2.B2.T2.S5, D-31\r\n" //
				+ "A2.B2.T2.S4, D-32\r\n" //
				+ "A2.B2.T2.S3, D-33\r\n" //
				+ "A2.B2.T2.S2, D-34\r\n" //
				+ "A2.B2.T2.S1, D-35\r\n" //
				+ "A2.B2.T1.S5, D-36\r\n" //
				+ "A2.B2.T1.S4, D-37\r\n" //
				+ "A2.B2.T1.S3, D-38\r\n" //
				+ "A2.B2.T1.S2, D-39\r\n" //
				+ "A2.B2.T1.S1, D-40\r\n"; //
		importLocationAliasesData(facility, csvString2);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid("0x00000012"));

		Che che1 = network.getChe("CHE1");
		che1.setColor(ColorEnum.GREEN);
		Che che2 = network.getChe("CHE2");
		che1.setColor(ColorEnum.MAGENTA);

		Short channel1 = 1;
		controller1.addLocation(aisle1);
		aisle1.setLedChannel(channel1);
		aisle1.getDao().store(aisle1);
		controller2.addLocation(aisle2);
		aisle2.setLedChannel(channel1);
		aisle2.getDao().store(aisle2);

		return facility;

	}

	private void setUpGroup1OrdersAndSlotting(Facility facility) throws IOException {
		// These are group = "1". Orders "123", "456", and "789"
		// 5 products batched into containers 11 through 15
		// and 99999999,Unknown Item
		// order 888 bad slotting. Order 999 no slotting

		String orderCsvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,,123,99999999,Unknown Item,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,88888888,Unknown Item,1,XXX,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,66666666,Unknown Item,0,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,,456,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10706962,Authentic Pizza Sauces,2,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10706962,Authentic Pizza Sauces,2,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10100250,Organic Fire-Roasted Red Bell Peppers,3,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,888,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,,999,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		beginTransaction();
		facility = facility.reload();
		importOrdersData(facility, orderCsvString);
		commitTransaction();
		
		// Slotting file

		String csvString2 = "orderId,locationId\r\n" //
				+ "123,D-2\r\n" // in A1.B1
				+ "456,D-25\r\n" // in A1.B2
				+ "888,D-XX\r\n" // bad location
				+ "789,D-35\r\n"; // in A2.B2
		beginTransaction();
		facility = facility.reload();
		importSlotting(facility, csvString2);
		commitTransaction();

		// Batches file. 11-15 are valid. 16-18 are looking for problems.
		String thirdCsvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "1,11,10700589,5,ea\r\n" //
				+ "1,12,10722222,10,ea\r\n" //
				+ "1,13,10706962,3,ea\r\n" //
				+ "1,14,10100250,4,ea\r\n" //
				+ "1,16,99999999,0,ea\r\n" // Order for item exists, but we say we have 0 in container 16
				+ "1,17,88888888,2,ea\r\n" // Order for item exists, but with different UOM
				+ "1,18,77777777,2,ea\r\n" // Order for item does not exist.
				+ "1,19,66666666,2,ea\r\n" // Order for item came with 0 count in outbound order.
				+ "1,15,10706961,2,ea\r\n"; // a good one last, to prove we elegantly skipped one line each only above.
		beginTransaction();
		facility = facility.reload();
		importBatchData(facility, thirdCsvString);
		commitTransaction();
	}

	@Test
	public final void testDataSetup() throws IOException {

		beginTransaction();
		Facility facility = setUpSimpleSlottedFacility();
		UUID facId = facility.getPersistentId();
		commitTransaction();
		
		setUpGroup1OrdersAndSlotting(facility);

		beginTransaction();
		facility = Facility.staticGetDao().findByPersistentId(facId);
		Assert.assertNotNull(facility);

		List<Container> containers = Container.staticGetDao().findByParent(facility);
		int containerCount = containers.size(); // This can throw if  we did not re-get the facility in the new transaction boundary. Just testing that.
		Assert.assertTrue(containerCount == 7);
		commitTransaction();
	}

	@Test
	public final void basicCrossBatchRun() throws IOException {
		beginTransaction();
		Facility facility = setUpSimpleSlottedFacility();
		PropertyBehavior.turnOffHK(facility);
		UUID facId = facility.getPersistentId();
		commitTransaction();

		setUpGroup1OrdersAndSlotting(facility);

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().findByPersistentId(facId);
		Assert.assertNotNull(facility);

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Case 1: A happy-day startup. No housekeeping jobs. Two from one container.");

		picker.setupContainer("11", "1"); // This prepended to scan "C%11" as per Codeshelf scan specification
		picker.setupContainer("15", "3");

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 5000);
		picker.scanLocation("D-36");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		PropertyBehavior.restoreHKDefaults(facility);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(3, picker.countRemainingJobs());
		Assert.assertEquals(1, picker.countActiveJobs());

		// put away first item
		WorkInstruction wi = picker.nextActiveWi();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(2, picker.countRemainingJobs());

		// put away second item
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);

		// put away last item
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 5000);

		commitTransaction();
	}

	@Test
	public final void crossBatchShorts() throws IOException {
		beginTransaction();
		Facility facility = setUpSimpleSlottedFacility();
		UUID facId = facility.getPersistentId();
		PropertyBehavior.turnOffHK(facility);
		commitTransaction();

		setUpGroup1OrdersAndSlotting(facility);

		this.startSiteController();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().findByPersistentId(facId);
		Assert.assertNotNull(facility);

		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("Case 1: Startup. Container 11 will have 2 jobs. We can short one, and see the other short ahead. Container 15 has one job.");
		LOGGER.info("                  Containers 16-19 do 4 situations that might cause immediate short. They do not. ");

		picker.setupContainer("15", "1"); // Good one gives one work instruction
		picker.setupContainer("16", "2");
		picker.setupContainer("17", "3");
		picker.setupContainer("18", "4");
		picker.setupContainer("19", "5");
		picker.setupContainer("11", "6"); // Good one gives two work instruction

		picker.startAndSkipReview("D-36", 5000, 3000);
		PropertyBehavior.restoreHKDefaults(facility);

		Assert.assertEquals(3, picker.countRemainingJobs());
		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);


		//picker.simulateCommitByChangingTransaction(this.persistenceService);

		Che che1 = Che.staticGetDao().findByPersistentId(this.che1PersistentId);
		List<WorkInstruction> cheWis = che1.getCheWorkInstructions();
		Assert.assertNotNull(cheWis);
		int cheWiTotal = cheWis.size();
		Assert.assertEquals(3, cheWiTotal);
		LOGGER.info("List the CHE work instructions which might have new shorts. Order is random for this. Only 3 confirms no immediate shorts.");
		// CheProcessTestPick.java shows immediate shorts for picks.
		logWiList(cheWis);

		LOGGER.info("Case 2: Short the first job. This should short ahead the third.");
		WorkInstruction wi = picker.nextActiveWi();
		// cannot get there except by scanning short
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		int button = picker.buttonFor(wi);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(1, picker.countRemainingJobs());

		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();

		// pick last item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 5000);

		this.getTenantPersistenceService().commitTransaction();
	}
}
