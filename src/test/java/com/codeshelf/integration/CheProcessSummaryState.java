package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;

public class CheProcessSummaryState extends CheProcessPutWallSuper {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessSummaryState.class);
	private static final int	WAIT_TIME	= 4000;

	/*
	 * The purpose of these tests is to make sure all supported process variations work with the summary state.
	 * The super class provides setUpFacilityWithPutWall(). For our purposes, F11 to F18 on one path, and S11 to S18 on another path
	 * The super class provides setUpOrders1(). Orders except for 11115 and 11116 have workSequence. All other uses of this do not use the workSequence.
	 * This class will have a separate setupUnmodeledFacility() method. Same setUpOrders1() may be used.
	 */

	/**
	 * The goal is an unmodeled facility. Two CHEs. No aisle controllers, poscons. No paths.
	 * Naturally, must use WorkSequence
	 */
	protected Facility setupUnmodeledFacility() throws IOException {
		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		return getFacility();
	}

	/**
	 * The goal is the super class modeled facility. But with CHE's lastscanlocation set
	 */
	protected Facility getModeledFacility() throws IOException {

		Facility facility = setUpFacilityWithPutWall();
		// let's find our CHE and set its last logged in value before site controller initializes
		Che che1 = Che.staticGetDao().findByDomainId(getNetwork(), "CHE1");
		Assert.assertNotNull(che1);
		che1.setLastScannedLocation("F11");
		Che.staticGetDao().store(che1);
		return facility;
	}

	/**
	 * Assuming some structure of the SETUP_SUMMARY screen, find the location name we last scanned onto.
	 */
	private String getSummaryScreenLocation(PickSimulator picker) {
		String line1 = picker.getLastCheDisplayString(1);
		line1 = line1.trim();
		int lengthLine1 = line1.length();
		int lastSpaceAt = line1.lastIndexOf(" ");
		if (lastSpaceAt < 0)
			return "";
		return line1.substring(lastSpaceAt + 1, lengthLine1);
	}

	/**
	 * Assuming some structure of the SETUP_SUMMARY screen, find the count of orders shown as a string.
	 */
	private String getSummaryScreenOrderCount(PickSimulator picker) {
		String line1 = picker.getLastCheDisplayString(1);
		line1 = line1.trim(); // strip lead (and trail). At this point, the string is something like "2 orders          xxxxx"
		int firstSpaceAt = line1.indexOf(" ");
		if (firstSpaceAt < 0)
			return "";
		return line1.substring(0, firstSpaceAt);
	}

	/**
	 * Assuming some structure of the SETUP_SUMMARY screen, find the count of jobs shown as a string.
	 */
	private String getSummaryScreenJobCount(PickSimulator picker) {
		String line2 = picker.getLastCheDisplayString(2);
		line2 = line2.trim(); // strip lead (and trail). At this point, the string is something like "12 jobs          1 other"
		int firstSpaceAt = line2.indexOf(" ");
		if (firstSpaceAt < 0)
			return "";
		return line2.substring(0, firstSpaceAt);
	}

	/**
	 * Assuming some structure of the SETUP_SUMMARY screen, find the count done shown as a string.
	 * Return "" if done is not showing at all
	 */
	private String getSummaryScreenDoneCount(PickSimulator picker) {
		String line3 = picker.getLastCheDisplayString(3);
		line3 = line3.trim(); // strip lead (and trail). At this point, the string is something like "0 done          2 short"
		int firstSpaceAt = line3.indexOf(" ");
		if (firstSpaceAt < 0)
			return "";
		else
			return line3.substring(0, firstSpaceAt);
		// We are assuming if shorts > 0, then we also show done even if 0.
	}

	@Test
	public final void basicSummary() throws IOException {
		// This shows the simple success case of 4 picks from 2 orders on 2 paths. CHE's lastScanLocation at F11, so START would assume that.
		// A few extra starts and location scans thrown in before getting to the picks.

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = getModeledFacility();
		setUpOrders1(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		if (!picker1.usesSummaryState())
			return; // this test only applies to new CHE process, not old.

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1a: Check the initial summary screen. Right justified at line 1 should be F11"); // see getModeledFacility for why.
		Assert.assertEquals("F11", getSummaryScreenLocation(picker1));

		LOGGER.info("1b: Set up orders 11117 and 12345 for pick");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.scanSomething("11117");
		picker1.waitForCheState(CheStateEnum.CONTAINER_POSITION, WAIT_TIME);
		picker1.scanSomething("P%1");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("12345", "2"); // the easier way.
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have

		LOGGER.info("1c: The summary screen should show 2 orders and 3 jobs with 1 other");
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1)); // this way to test independent of labeling
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));

		String line2 = picker1.getLastCheDisplayString(2);
		int lengthLine2 = line2.length();
		String otherCountStr = line2.substring(lengthLine2 - 7, lengthLine2);
		Assert.assertEquals("1 other", otherCountStr); // not independent of labeling and screen format

		LOGGER.info("2a: Start, but don't pick");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> wis = picker1.getAllPicksList();
		this.logWiList(wis);

		LOGGER.info("2b: Scan start again. This comes back to summary screen");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("2c: From summary screen, scan location on same path. This will start the pick");
		picker1.scanLocation("F14");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2d: From pick, scan location on same path. This will start the pick from that point on the path.");
		picker1.scanLocation("F11");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("3a: From pick, scan location on different path. This will go the summary state.");
		picker1.scanLocation("S11");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have

		LOGGER.info("3b: Still 2 orders but now 1 job with 3 other");
		Assert.assertEquals("S11", getSummaryScreenLocation(picker1));
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("", getSummaryScreenDoneCount(picker1));

		LOGGER.info("3c: Start; ready to pick");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("3d: Pick the one job  on path. Comes to Summary screen as the pick is complete");
		WorkInstruction wi = picker1.nextActiveWi(); // spell out the steps of pickItemAuto() once
		int button = picker1.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker1.pick(button, quant);
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have

		LOGGER.info("3e: line 2 now has 0 picks, 3 other. Line 3 has 1 done. Line 4 suggests to scan another location");
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("0", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4a: From summary, scan location on different path. This will go the summary state but with different counts.");
		picker1.scanLocation("F12");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		// Now no other. Also, the complete count is lost. Fix someday?
		// This was a new path case. We might want to lose the complete on path change.
		Assert.assertEquals("F12", getSummaryScreenLocation(picker1));
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4b: Start; ready to pick");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("4c: Start; Pick 1. (2 remain)");
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("4d: Start; from pick screen, goes back to summary");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("2", getSummaryScreenJobCount(picker1));
		// the complete count is lost. Fix someday? Only not lost if you actually finish
		Assert.assertEquals("", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4e: Let's finish; goes to summary");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("0", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("2", getSummaryScreenDoneCount(picker1));
	}

	@Test
	public final void badLocationChange() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = getModeledFacility();
		setUpOrders1(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		if (!picker1.usesSummaryState())
			return; // this test only applies to new CHE process, not old.

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1: Set up orders 11117 and 12345 for pick");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1"); 
		picker1.setupOrderIdAsContainer("12345", "2"); // the easier way.
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
	
		LOGGER.info("2: Scan to a bad location. The back end gives us the work instructions for good location it had before.");
		picker1.scanLocation("XX12");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		// picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		List<WorkInstruction> wis = picker1.getAllPicksList();
		this.logWiList(wis);
		Assert.assertEquals(4, wis.size()); // one housekeeping. That is why not 3.

		LOGGER.info("2b: Start to go to summary screen");		
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// TODO call it a bug here. Backend kept assuming F11. Would be better if we knew and displayed that.
		Assert.assertEquals("XX12", getSummaryScreenLocation(picker1));
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));

		LOGGER.info("2c: Scan to our good S11 location that has one job on path.");
		picker1.scanLocation("S11");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));

		LOGGER.info("2d: Start the pick, but don't pick");		
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2e: Scan to a bad location. The back end gives us the work instructions for good location it had before.");
		picker1.scanLocation("XX12");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		// picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);

		LOGGER.info("2f: Start to go to summary screen");		
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("XX12", getSummaryScreenLocation(picker1));
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));
	}
}
