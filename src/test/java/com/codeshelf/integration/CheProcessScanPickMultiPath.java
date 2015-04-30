package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessScanPickMultiPath extends ServerTest {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessScanPickMultiPath.class);
	private static final int	WAIT_TIME	= 4000;

	private PickSimulator setupCheOnPathAndReturnPicker(String cheLastScannedLocation) throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpMultiPathFacilityWithOrders(cheLastScannedLocation);

		LOGGER.info("1a: leave LOCAPICK off, SCANPICK off, set BayDistance, no housekeeping");
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "Disabled");
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.BayDistance.toString());
		propertyService.turnOffHK(facility);

		this.getTenantPersistenceService().commitTransaction();
		this.startSiteController();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");
		return picker;
	}

	private Facility setUpMultiPathFacilityWithOrders(String cheLastScannedLocation) throws IOException {
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n"
				+ "Aisle,A1,,,,,zigzagB1S1Side,3,4,X,20\n"
				+ "Bay,B1,100,,,,,,,,\n"
				+ "Tier,T1,100,0,32,0,,,,,\n"
				+ "Bay,B2,100,,,,,,,,\n"
				+ "Tier,T1,100,0,32,0,,,,,\n"
				+ "Aisle,A2,,,,,zigzagB1S1Side,3,6,X,20\n"
				+ "Bay,B1,100,,,,,,,,\n" + "Tier,T1,100,0,32,0,,,,,\n" + "Bay,B2,100,,,,,,,,\n" + "Tier,T1,100,0,32,0,,,,,\n";
		importAislesData(getFacility(), aislesCsvString);

		// Get the aisles
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle1);
		Assert.assertNotNull(aisle2);

		Path path1 = createPathForTest(getFacility());
		PathSegment segment1_1 = addPathSegmentForTest(path1, 0, 3d, 4.5, 5d, 4.5);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment2_1 = addPathSegmentForTest(path2, 0, 3d, 6.5, 5d, 6.5);

		String persistStr1 = segment1_1.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr1);

		String persistStr2 = segment2_1.getPersistentId().toString();
		aisle2.associatePathSegment(persistStr2);

		String csvLocationAliases = "mappedLocationId,locationAlias\r\n" + "A1.B1.T1,Loc1A\r\n" + "A1.B2.T1,Loc1B\r\n"
				+ "A2.B1.T1,Loc2A\r\n" + "A2.B2.T1,Loc2B\r\n";
		importLocationAliasesData(getFacility(), csvLocationAliases);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid("0x00000012"));

		Short channel1 = 1;
		Location tier1 = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier1);
		tier1.setLedChannel(channel1);
		tier1.getDao().store(tier1);

		Location tier2 = getFacility().findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier2);
		tier2.setLedChannel(channel1);
		tier2.getDao().store(tier1);

		if (cheLastScannedLocation != null) {
			Che che = getNetwork().getChe("CHE1");
			che.setLastScannedLocation(cheLastScannedLocation);
			Che.staticGetDao().store(che);
		}

		String inventory = "itemId,locationId,description,quantity,uom,inventoryDate,lotId,cmFromLeft\r\n"
				+ "Item1,Loc1A,Item Desc 1,1000,a,12/03/14 12:00,,0\r\n" + "Item2,Loc1A,Item Desc 2,1000,a,12/03/14 12:00,,12\r\n"
				+ "Item3,Loc1A,Item Desc 3,1000,a,12/03/14 12:00,,24\r\n" + "Item4,Loc1A,Item Desc 4,1000,a,12/03/14 12:00,,36\r\n"
				+ "Item5,Loc1B,Item Desc 5,1000,a,12/03/14 12:00,,0\r\n" + "Item6,Loc1B,Item Desc 6,1000,a,12/03/14 12:00,,12\r\n"
				+ "Item7,Loc1B,Item Desc 7,1000,a,12/03/14 12:00,,24\r\n" + "Item8,Loc1B,Item Desc 8,1000,a,12/03/14 12:00,,36\r\n"
				+ "Item9,Loc2A,Item Desc 9,1000,a,12/03/14 12:00,,0\r\n"
				+ "Item10,Loc2A,Item Desc 10,1000,a,12/03/14 12:00,,12\r\n"
				+ "Item11,Loc2A,Item Desc 11,1000,a,12/03/14 12:00,,24\r\n"
				+ "Item12,Loc2A,Item Desc 12,1000,a,12/03/14 12:00,,36\r\n"
				+ "Item13,Loc2B,Item Desc 13,1000,a,12/03/14 12:00,,0\r\n"
				+ "Item14,Loc2B,Item Desc 14,1000,a,12/03/14 12:00,,12\r\n"
				+ "Item15,Loc2B,Item Desc 15,1000,a,12/03/14 12:00,,24\r\n"
				+ "Item16,Loc2B,Item Desc 16,1000,a,12/03/14 12:00,,36\r\n";
		importInventoryData(getFacility(), inventory);

		String orders = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,workSequence,locationId\r\n"
				+ "1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1,,\n"
				+ "1,1,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1,,\n"
				+ "1,1,347,12/03/14 12:00,12/31/14 12:00,Item14,,120,a,Group1,,\n"
				+ "1,1,348,12/03/14 12:00,12/31/14 12:00,Item1,,11,a,Group1,,\n"
				+ "1,1,349,12/03/14 12:00,12/31/14 12:00,Item10,,22,a,Group1,,\n"
				+ "1,1,350,12/03/14 12:00,12/31/14 12:00,Item5,,33,a,Group1,,\n"
				+ "1,1,351,12/03/14 12:00,12/31/14 12:00,Item3,,22,a,Group1,,\n"
				+ "1,1,352,12/03/14 12:00,12/31/14 12:00,Item6,,33,a,Group1,,\n";
		importOrdersData(getFacility(), orders);
		return getFacility();
	}

	private void containerSetup(PickSimulator picker) {
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("1", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
	}

	/**
	 * In this test, the CHE has no last_scanned_location.
	 * Instead, it retrieves the list of all available items, then attaches itself to the path of the first item in the list
	 */
	@Test
	public void testStartStartNoPath() throws IOException {
		//Do not set last_scanned_location on the CHE
		PickSimulator picker = setupCheOnPathAndReturnPicker(null);

		containerSetup(picker);

		//Start work without specifying path
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		//If CHE doesn't have a saved path, it will pick one with a required item closest to the beginning of the path (i.e. - pretty arbitrary)
		//Verify the number of items on the auto-chosen path
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item10", "Item14", "Item15" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * In this test, CHE receives work from the path matching its last_scanned_location
	 */
	@Test
	public void testStartStartPath1() throws IOException {
		//Set Che on the first path 
		//In this case, the path is "1"; we scan "B" (the middle of the path "1"), to verify that work is still calculated from the beginning of the path 
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1B");

		containerSetup(picker);

		//Scan START, verify the number of items on the CHE's path
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan START to begin pick, review the first and the remaining items on the path
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc1A", "Item1", "QTY 11", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item1", "Item3", "Item5", "Item6", "Item7" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * Same as testStartStartPath1, but with CHE starting off and remaining on a different path
	 */
	@Test
	public void testStartStartPath2() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc2A");

		containerSetup(picker);

		//Scan START, verify the number of items on the CHE's path
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("3"), posConValue);

		//Scan START to begin pick, review the first and the remaining items on the path
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc2A", "Item10", "QTY 22", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item10", "Item14", "Item15" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * From LOCATION_SELECT state, select a location on the same path.
	 * Verify that the Pick starts immediately, just line during the single-path operations
	 */
	@Test
	public void testSelectLocationSamePath() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START, review displays
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan location on the same path, but not in the beginning. Make sure work starts from the middle of the path
		picker.scanLocation("Loc1B");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc1B", "Item5", "QTY 33", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item5", "Item6", "Item7", "Item1", "Item3" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * From LOCATION_SELECT state, select a location on a different path.
	 * Verify that the CHE remains in LOCATION_SELECT, but showing work for the new path 
	 */
	@Test
	public void testSelectLocationChangePath() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START, review displays
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan location on the same path, but not in the beginning
		//The che will remain the in the LOCATION_SELECT state, but will now show work for the new path
		picker.scanLocation("Loc2B");
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("3"), posConValue);

		//Note that despite new location being in the middle of the second path, the pick begins at its start
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc2A", "Item10", "QTY 22", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item10", "Item14", "Item15" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * From LOCATION_SELECT, scan a location on a different path. Make sure CHE switches to it.
	 * Then, scan a location in the middle of the new path. Make sure that the work is ordered and wrapped accordingly
	 */
	@Test
	public void testSelectLocationChangePathJumpForward() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START, review displays
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan location on a different path
		//The che will remain the in the LOCATION_SELECT state, but will now show work for the new path
		picker.scanLocation("Loc2B");
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("3"), posConValue);

		//Note that despite new location being in the meddle of the second path, the pick begins at its start
		picker.scanLocation("Loc2B");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc2B", "Item14", "QTY 120", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item14", "Item15", "Item10" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * From LOCATION_SELECT, scan a location on a different path. Make sure CHE switches to it.
	 * Then, scan a location in the middle of the new path. Make sure that the work is ordered and wrapped accordingly
	 */
	@Test
	public void testChangePathsTwice() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START, review displays
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan location on a different path
		//The che will remain the in the LOCATION_SELECT state, but will now show work for the new path
		picker.scanLocation("Loc2B");
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("3"), posConValue);

		//Once again, scan location on a different path (returning to the original one)
		//The che will remain the in the LOCATION_SELECT state, but will now show work for the new path
		picker.scanLocation("Loc1B");
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Star pick
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc1A", "Item1", "QTY 11", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations = { "Item1", "Item3", "Item5", "Item6", "Item7" };
		compareInstructionsList(wiList, expectations);
	}

	/**
	 * This test verifies the "NO WORK ON CURRENT PATH" message after setting up containers
	 */
	@Test
	public void testFinishWorkOnPathAndNoWorkOnCurrentPath() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START, review displays
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		Byte posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("5"), posConValue);

		//Scan START to begin pick, review results
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc1A", "Item1", "QTY 11", "");
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		String[] expectations1 = { "Item1", "Item3", "Item5", "Item6", "Item7" };
		compareInstructionsList(wiList, expectations1);

		//Pick all items on this path
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();

		//Very "All Work Complete" message and "--" on poscon
		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);
		// verifyCheDisplay(picker, "PATH COMPLETE", "SCAN NEW LOCATION", "OR SETUP NEW CART", "");
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, picker.getLastSentPositionControllerMinQty((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, picker.getLastSentPositionControllerMaxQty((byte) 1));

		//Setup containers again
		picker.scanCommand(CheDeviceLogic.LOGOUT_COMMAND);
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		containerSetup(picker);

		//Verify "No Work On Current Path" message
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getNoWorkReviewState(), WAIT_TIME);

		//Scan location on a different path. Verify remaining work count
		picker.scanLocation("Loc2A");
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		posConValue = picker.getLastSentPositionControllerDisplayValue((byte) 1);
		Assert.assertEquals(new Byte("3"), posConValue);

		//Start pick on the new path. Verify work list
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		verifyCheDisplay(picker, "Loc2A", "Item10", "QTY 22", "");
		wiList = picker.getRemainingPicksWiList();
		String[] expectations2 = { "Item10", "Item14", "Item15" };
		compareInstructionsList(wiList, expectations2);

		//Finish the other path
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();

		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);
		// verifyCheDisplay(picker, "ALL WORK COMPLETE", "", "", "");
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_C, picker.getLastSentPositionControllerMinQty((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_O, picker.getLastSentPositionControllerMaxQty((byte) 1));
	}

	@Test
	//restore this test, maybe.
	// Ilya, I suspect handling of unexpected scans is incomplete.
	public void testIncompleteWorkSinglePath() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		//Scan START to begin pick
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		//Verify that there are 5 items on the first path
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		Assert.assertEquals(5, wiList.size());

		//Pick all 5 items on this path
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();

		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);

		// Can you directly scan location onto a different path. No. Still at complete state.
		picker.scanLocation("Loc2A");
		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);
		// So, start again, then get the new location.

		//TODO fix
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		if (!picker.usesSummaryState())
			picker.waitForCheState(picker.getNoWorkReviewState(), WAIT_TIME);
		else
			picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		picker.scanLocation("Loc2A");
		// TODO CLUMSY, fix.
		if (!picker.usesSummaryState())
			picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		else
			picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		picker.scanLocation("Loc2A");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		//Verify that there are 3 items on the second path
		wiList = picker.getRemainingPicksWiList();
		Assert.assertEquals(3, wiList.size());

		//Pick 2 items on the other path
		picker.pickItemAuto();
		picker.pickItemAuto();
		//Short the last item
		picker.scanCommand("SHORT");
		picker.pick(1, 0);
		picker.scanCommand("YES");

		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);
		// verifyCheDisplay(picker, "ALL WORK COMPLETE", "", "", "");
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TOP_BOTTOM, picker.getLastSentPositionControllerMinQty((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TOP_BOTTOM, picker.getLastSentPositionControllerMaxQty((byte) 1));
	}

	@Test
	public void testIncompleteWorkMultiPath() throws IOException {
		//Set Che on the second path
		PickSimulator picker = setupCheOnPathAndReturnPicker("Loc1A");

		containerSetup(picker);

		//Scan START
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(picker.getLocationStartReviewState(), WAIT_TIME);
		//Scan START to begin pick
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		// how did this work? 5 picks shown next. Looks like bad test.
		List<WorkInstruction> wiList = picker.getRemainingPicksWiList();
		Assert.assertEquals(5, wiList.size());

		//Pick 4 items on this path and short the last one
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.scanCommand("SHORT");
		picker.pick(1, 0); //Short the last item
		picker.scanCommand("YES");

		//At this point, there are unfinished items on both paths
		//(Shorted items on this and all items on another)
		picker.waitForCheState(picker.getCompleteState(), WAIT_TIME);
		// verifyCheDisplay(picker, "PATH COMPLETE", "SCAN NEW LOCATION", "OR SETUP NEW CART", "");
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TRIPLE_DASH, picker.getLastSentPositionControllerMinQty((byte) 1));
		Assert.assertEquals(PosControllerInstr.BITENCODED_TRIPLE_DASH, picker.getLastSentPositionControllerMaxQty((byte) 1));
	}
}