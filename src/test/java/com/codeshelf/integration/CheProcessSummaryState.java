package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
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
	protected Facility getUnmodeledFacility() throws IOException {
		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		// Look's like CHE1 is reused as is test to test.
		Che che1 = Che.staticGetDao().findByDomainId(getNetwork(), "CHE1");
		Assert.assertNotNull(che1);
		che1.setLastScannedLocation("");
		Che.staticGetDao().store(che1);

		return getFacility();
	}

	/**
	 * The goal is the super class modeled facility. But with CHE1's lastscanlocation set to F11
	 */
	protected Facility getModeledFacility() throws IOException {

		Facility facility = setUpFacilityWithPutWall();
		// let's find our CHE and set its last logged in value before site controller initializes
		beginTransaction();
		Che che1 = Che.staticGetDao().findByDomainId(getNetwork(), "CHE1");
		Assert.assertNotNull(che1);
		che1.setLastScannedLocation("F11");
		Che.staticGetDao().store(che1);
		commitTransaction();
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
	
	// need getSummaryScreenOtherCount(). Need to parse it out of the line.


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
	
	// need getSummaryScreenShortsCount(). Need to parse it out of the line.

	@Test
	public final void basicSummary() throws IOException {
		// This shows the simple success case of 4 picks from 2 orders on 2 paths. CHE's lastScanLocation at F11, so START would assume that.
		// A few extra starts and location scans thrown in before getting to the picks.

		Facility facility = getModeledFacility();
		
		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

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
		// Notice we keep the complete count, even though this was a path change
		Assert.assertEquals("F12", getSummaryScreenLocation(picker1));
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4b: Start; ready to pick");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("4c: Start; Pick 1. (2 remain)");
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		LOGGER.info("4d: REVERSE; from pick screen, goes back to summary, just as start would");
		picker1.scanCommand("REVERSE");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("2", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("2", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4e: Let's finish in reverse; goes to summary");
		picker1.scanCommand("REVERSE");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		wis = picker1.getAllPicksList();
		this.logWiList(wis);
		// The F14 job was going to be fourth above. Can see in the log/console at the logWiList() call. Now it is done next since we reversed.
		Assert.assertEquals("F14", picker1.getLastCheDisplayString(1));
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("0", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("4", getSummaryScreenDoneCount(picker1));
	}

	@Test
	public final void badLocationChange() throws IOException {

		Facility facility = getModeledFacility();

		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

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

		LOGGER.info("2: Scan to a bad location. The back end interprets this as a path change, so come to summary screen instead of DO_PICK.");
		picker1.scanLocation("XX12");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		picker1.logCheDisplay(); // Look in log to see what we have
		List<WorkInstruction> wis = picker1.getAllPicksList();
		this.logWiList(wis);
		Assert.assertEquals(6, wis.size()); // Found all 4 jobs, plus 2 housekeeping.

		// See that the backend persisted the scan even though it is not resolvable
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Che che = Che.staticGetDao().findByDomainId(getNetwork(), "CHE1");
		Assert.assertEquals("XX12", che.getLastScannedLocation());
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("2b: Start to go to summary screen");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("XX12", getSummaryScreenLocation(picker1));
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));

		LOGGER.info("2c: Scan to our good S11 location that has one job on path.");
		picker1.scanLocation("S11");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));

		LOGGER.info("2d: Start the pick, but don't pick");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2e: Scan to a bad location. Interpretted as path change so back to summary screen.");
		picker1.scanLocation("XX12");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2f: Start to go to summary screen. Back to our 4 jobs");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("XX12", getSummaryScreenLocation(picker1));
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));
		
		// Change of test mission here. Above was changes between good and bad paths.
		// Now make sure setup resets the remembered completes and shorts since that was not checked in basicSummary().
		LOGGER.info("3a: Complete one job and short one, then go to setup summary screen");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		// next in is housekeeping, so do that also
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		WorkInstruction wi = picker1.nextActiveWi();
		picker1.scanCommand("SHORT");
		picker1.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
		int button = picker1.buttonFor(wi);
		picker1.pick(button, 0);
		picker1.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
		picker1.scanCommand("YES");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// Three left. We only completed one, and we shorted one. (And the one housekeeping.)
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));
		// we do not have getSummaryScreenShortsCount(picker1));
		
		LOGGER.info("3b: Setup again, with same orders");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.setupOrderIdAsContainer("12345", "2"); // the easier way.
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("", getSummaryScreenDoneCount(picker1));
		
		/*
		 * add this part when we pick up unmodeled and unpathed locations in bayDistance sort
		 * 
		LOGGER.info("4a: Setup for an order that has only unmodeled locations. Therefore, no path");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11120", "1");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// Assert.assertEquals("2", getSummaryScreenJobCount(picker1));

		LOGGER.info("4b: The preferred location is X11. Scan there, although any other unmodeled location name would do.");
		picker1.scanLocation("X11");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> wis4 = picker1.getAllPicksList();
		this.logWiList(wis4);

		LOGGER.info("4b: We are seeing what bay sequencer does with no path. We sort by location first, then sku");
		// This is a very important (and only) test of PosAlongPathComparator for zero and null posAlongPath values
		// The two X11s, then the X12. And for the X11s, secondary sort by SKU.
		// Note the WARN in the console "3 work instructions made not on path. Add them last."
		Assert.assertEquals("X11", picker1.getLastCheDisplayString(1));
		Assert.assertEquals("1602", picker1.getLastCheDisplayString(2));
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		// a housekeep
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		Assert.assertEquals("X11", picker1.getLastCheDisplayString(1));
		Assert.assertEquals("1702", picker1.getLastCheDisplayString(2));
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		// a housekeep				
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		Assert.assertEquals("X12", picker1.getLastCheDisplayString(1));
		Assert.assertEquals("1701", picker1.getLastCheDisplayString(2));
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4c: Log out/Log in again. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));
		*/

	}

	@Test
	public final void workSequenceAndSummary() throws IOException {

		beginTransaction();
		Facility facility = getUnmodeledFacility();
		commitTransaction();
		
		beginTransaction();
		setUpOrders1(facility);
		commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1: Set up orders 11117 and 12345 for pick. No known paths, so should get all 4 jobs");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.setupOrderIdAsContainer("12345", "2");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("2", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));

		LOGGER.info("2: Scan Start. The back end gives us the work instructions for good location it had before.");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> wis = picker1.getAllPicksList();
		this.logWiList(wis);
		Assert.assertEquals(5, wis.size()); // one housekeeping. That is why not 4.

		LOGGER.info("2b: Scan Start. Back to summary.");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));

		LOGGER.info("2c: Scan to what was a good S11 location  in other test with one job on path. But this test should not have it");
		picker1.scanLocation("S11");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2d: Scan Start. Back to summary.");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));

		LOGGER.info("3a: Scan a location from DO_PICK. Should do little in workSequence mode. Stays picking. (No path change)");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.scanLocation("F11");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("3b: Start. Back to summary. See if we picked up the F11");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		// TODO This is questionable. The location probably has no value
		Assert.assertEquals("F11", getSummaryScreenLocation(picker1));
		Assert.assertEquals("4", getSummaryScreenJobCount(picker1));

		LOGGER.info("4: pick to completion"); // 5 because one housekeeping
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("0", getSummaryScreenJobCount(picker1));
		Assert.assertEquals("4", getSummaryScreenDoneCount(picker1));
	}

	@Test
	public final void badOrdersAndSummary() throws IOException {

		beginTransaction();
		Facility facility = getUnmodeledFacility();
		commitTransaction();

		beginTransaction();
		setUpOrders1(facility);
		commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1: Set up orders 11117 and bad order 99999 for pick. No known paths, so should get all 1 good job for 11117");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.setupOrderIdAsContainer("99999", "2");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals("1", getSummaryScreenOrderCount(picker1));
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));
		// Summary screen does not show any error.
		// poscon 2 shows "--" which may be ambiguous. Same a no orders on this path, but are on other paths. Good to show something anyway.
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 2),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);

		LOGGER.info("2: Scan Start. Ready to do our one job");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> wis = picker1.getAllPicksList();
		this.logWiList(wis);
		Assert.assertEquals(1, wis.size());
		Assert.assertEquals(toByte(5), picker1.getLastSentPositionControllerDisplayValue((byte) 1));
		// continue showing feedback on position 2, even as we are doing the one
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 2),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);

		LOGGER.info("2b: Scan Start. Back to summary.");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 2),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
	}

	@Test
	public final void cheSetupPersistence() throws IOException {
		// This somewhat replicates a small section in CheProcessScanPick

		beginTransaction();
		Facility facility = getUnmodeledFacility();
		commitTransaction();

		beginTransaction();
		setUpOrders1(facility);
		commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1: Set up orders 11117 and bad order 99999 for picker1. No known paths, so should get 1 good job for 11117");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.setupOrderIdAsContainer("99999", "2");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2: Set up orders 11117 and bad order 12345 for picker2. No known paths, so should get all 4 jobs");
		picker2.loginAndCheckState("Picker #2", CheStateEnum.SETUP_SUMMARY);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("11117", "3");
		picker2.setupOrderIdAsContainer("12345", "2");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("3: Set up picker2 again, with only one different order");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.scanCommand("SETUP");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("11111", "3");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);


		// Let's see what the backend has
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Container c12345 = Container.staticGetDao().findByDomainId(facility, "12345");
		ContainerUse cu12345 = ContainerUse.staticGetDao().findByDomainId(c12345, "12345");
		Assert.assertTrue(cu12345.getPosconIndex() == 2);
		Container c11117 = Container.staticGetDao().findByDomainId(facility, "11117");
		ContainerUse cu11117 = ContainerUse.staticGetDao().findByDomainId(c11117, "11117");
		Assert.assertTrue(cu11117.getPosconIndex() == 3);
		Container c99999 = Container.staticGetDao().findByDomainId(facility, "99999");
		Assert.assertNull(c99999);
		
		Container c11111 = Container.staticGetDao().findByDomainId(facility, "11111");
		ContainerUse cu11111 = ContainerUse.staticGetDao().findByDomainId(c11111, "11111");
		Assert.assertTrue(cu11111.getPosconIndex() == 3);
		
		// And check the CHEs. See CheStatusMessage and how it populates. We can check the same.
		Che che1 = Che.staticGetDao().findByDomainId(getNetwork(), "CHE1");
		Che che2 = Che.staticGetDao().findByDomainId(getNetwork(), "CHE2");
		Assert.assertNotNull(che1);
		Assert.assertNotNull(che2);
		// It is not clear if che1 should have 11117 use anymore, as che2 took it away. But ok if both had it.
		// This seems to show that che1 lost it. Usually good, but there are bad possibilities
		List<ContainerUse> uses1 = che1.getUses();
		Assert.assertEquals(0, uses1.size());

		// It is clear that che2 should only have 11111 now. It used to have 12345 and 111117 but no longer should.
		// If uses2 size is 3, we have a bug.
		List<ContainerUse> uses2 = che2.getUses();
		Assert.assertEquals(1, uses2.size());
		Assert.assertEquals(cu11111, uses2.get(0)); // Should be the same container use we saw before

	
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void doneCounts() throws IOException {
		// A trick part of this feature is to remember the done and short counts done on this cart run between restarts,
		// logout, login.  If site controller has to reinitialize, the setup is good but the done and short counts are lost.
		// That is ok.

		Facility facility = getModeledFacility();
		
		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);

		LOGGER.info("1a: Check the initial summary screen. Right justified at line 1 should be F11"); // see getModeledFacility for why.
		Assert.assertEquals("F11", getSummaryScreenLocation(picker1));

		LOGGER.info("1b: Set up orders 11117 and 12345 for pick");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.setupOrderIdAsContainer("12345", "2");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have

		LOGGER.info("1c: The summary screen we have 3 jobs with 1 other");
		Assert.assertEquals("3", getSummaryScreenJobCount(picker1));


		LOGGER.info("2a: Pick one");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		LOGGER.info("2b: See the summary screen show one done");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));


		LOGGER.info("3a: Logout. Log in. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));

		LOGGER.info("3b: Logout/Log in again. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));


		LOGGER.info("4a: Start; pick this path to completion");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));
		
		LOGGER.info("4b: Logout. Log in. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4c: Logout/Log in again. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));

		LOGGER.info("4d: Logout/Log in again. See that done count is the same");
		picker1.logout();
		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));

		LOGGER.info("5a: Scan start, which only comes to same screen. Done count should not increment");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("3", getSummaryScreenDoneCount(picker1));

	}

	@Test
	public final void doneCountStartIncrement() throws IOException {
		// Explore a bug.

		Facility facility = getModeledFacility();

		setUpOrders1(facility);

		this.startSiteController();
		PickSimulator picker1 = createPickSim(cheGuid1);

		picker1.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);


		LOGGER.info("1a: Set up order 11117");
		picker1.scanCommand("SETUP");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker1.setupOrderIdAsContainer("11117", "1");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		picker1.logCheDisplay(); // Look in log to see what we have

		LOGGER.info("1b: The summary screen we have 1 job only");
		Assert.assertEquals("1", getSummaryScreenJobCount(picker1));

		LOGGER.info("2: Pick it. Summary will show 1 done");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker1.pickItemAuto();
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));

		LOGGER.info("2b: position 1 will show oc");
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 1),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);

		LOGGER.info("3: Immediately scan start again. Done count should not increment. Poscon still show oc");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 1),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);

		LOGGER.info("3: Immediately scan start again. Done count should not increment");
		picker1.scanCommand("START");
		picker1.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals("1", getSummaryScreenDoneCount(picker1));
		Assert.assertEquals(picker1.getLastSentPositionControllerDisplayValue((byte) 1),
			PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker1.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker1.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);

	}

}
