package com.codeshelf.integration;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.IntegrationTest;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

public class CheProcessScanPickReversable extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessScanPickReversable.class);
	private static final int WAIT_TIME = 4000;
	
	private PickSimulator waitAndGetPickerForProcessType(IntegrationTest test, NetGuid cheGuid, String inProcessType) {
		// took over 250 ms on JR's fast macbook pro. Hence the initial wait, then checking more frequently in the loop
		ThreadUtils.sleep(250);
		long start = System.currentTimeMillis();
		final long maxTimeToWaitMillis = 5000;
		String existingType = "";
		int count = 0;
		while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
			count++;
			PickSimulator picker = new PickSimulator(test, cheGuid);
			existingType = picker.getProcessType();
			if (existingType.equals(inProcessType)) {
				LOGGER.info(count + " pickers made in waitAndGetPickerForProcessType before getting it right");
				return picker;
			}
			ThreadUtils.sleep(100); // retry every 100ms
		}
		Assert.fail("Process type " + inProcessType + " not encountered in " + maxTimeToWaitMillis + "ms. Process type is "
				+ existingType);
		return null;
	}

	private PickSimulator setupTestPicker() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpOneAisleFourBaysFlatFacilityWithOrders();

		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		
		LOGGER.info("1a: leave LOCAPICK off, set SCANPICK, set BayDistance");

		
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		Assert.assertNotNull(facility);
		propertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		propertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "Disabled");
		propertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());

		propertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();	
		
		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);
		
		// We would rather have the device manager know from parameter updates, but that does not happen yet in the integration test.
		manager.setSequenceKind(WorkInstructionSequencerType.BayDistance.toString());
		Assert.assertEquals(WorkInstructionSequencerType.BayDistance.toString(), manager.getSequenceKind());
		manager.setScanTypeValue("Disabled");
		Assert.assertEquals("Disabled", manager.getScanTypeValue());
		picker.forceDeviceToMatchManagerConfiguration();
		
		return picker;
	}
	
	private void compareList(List<WorkInstruction> instructions, String[] expectations) {
		Assert.assertEquals(expectations.length, instructions.size());
		for (int i = 0; i < expectations.length; i++) {
			WorkInstruction instruction = instructions.get(i);
			if (!expectations[i].equals(instruction.getItemId())){
				Assert.fail(String.format("Mismatch in item %d. Expected list %s, got [%s]", i, Arrays.toString(expectations), printInstructionsList(instructions)));
			}
		}
	}
	
	private String printInstructionsList(List<WorkInstruction> instructions) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < instructions.size(); i++) {
			result.append(instructions.get(i).getItemId());
			if (i < instructions.size() - 1) {
				result.append(",");
			}
		}
		return result.toString();
	}
	
	private void pickNextAndCompare(PickSimulator picker, String expectedItem){
		WorkInstruction wi = picker.nextActiveWi();
		Assert.assertEquals(expectedItem, wi.getItemId());
		picker.pick(picker.buttonFor(wi), wi.getPlanQuantity());
	}

	
	/**
	 * This is a simple setup -> start -> start test.
	 * It is here as a baseline and verification of the test setup process
	 */
	@Test
	public void testForwardForward() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations);
	}
	
	/**
	 * Setup -> reverse -> reverse.
	 * Should traverse the path from the end
	 */
	@Test
	public void testReverseReverse() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations);
	}
	
	/**
	 * Setup -> start -> reverse
	 * Should traverse the path from the end
	 */
	@Test
	public void testForwardReverse() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations);
	}

	/**
	 * Setup -> reverse -> start
	 * Should traverse the path from the beginning
	 */
	@Test
	public void testReverseForward() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations);
	}
	
	/**
	 * Keep switching between start/start and reverse/reverse scans
	 */
	@Test
	public void testMultipleDoubleStarts() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations1 = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations1);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		scWiList = picker.getRemainingPicksWiList();
		String[] expectations2 = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations2);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		scWiList = picker.getRemainingPicksWiList();
		String[] expectations3 = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations3);
	}
	
	/**
	 * Simple setup -> start -> location test
	 */
	@Test
	public void testForwardAndLocation() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item9","Item11","Item15","Item2","Item3","Item5","Item7"};
		compareList(scWiList, expectations);
	}

	/**
	 * Setup -> reverse -> location
	 * Should traverse path backwards, starting just before the scanned location
	 */
	@Test
	public void testReverseAndLocation() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations = {"Item7","Item5","Item3","Item2","Item15","Item11","Item9"};
		compareList(scWiList, expectations);
	}

	/**
	 * Setup -> start -> start -> location
	 * Tests jumping to a specified location on the path, going forward
	 */
	@Test
	public void testForwardForwardThenJump() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		//Double-forward, then jump
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations1 = {"Item9","Item11","Item15","Item2","Item3","Item5","Item7"};
		compareList(scWiList, expectations1);
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);

		//Reverse-forward, then jump
		//Same result as above, with with a slightly different start
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		scWiList = picker.getRemainingPicksWiList();
		String[] expectations2 = {"Item9","Item11","Item15","Item2","Item3","Item5","Item7"};
		compareList(scWiList, expectations2);
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);

	}
	
	/**
	 * Setup -> reverse -> reverse -> location
	 * Tests jumping to a specified location on the path, going backward
	 */
	@Test
	public void testReverseReverseThenJump() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		//Double-reverse, then jump
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations1 = {"Item7","Item5","Item3","Item2","Item15","Item11","Item9"};
		compareList(scWiList, expectations1);
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);

		//Same result as above, with with a slightly different start
		//Forward-reverse, then jump
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		scWiList = picker.getRemainingPicksWiList();
		String[] expectations2 = {"Item7","Item5","Item3","Item2","Item15","Item11","Item9"};
		compareList(scWiList, expectations2);
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
	}
	
	/**
	 * Start going forward, pick few items, go backward, pick an item, go forward and jump to a location
	 * Keep checking the remaining instructions and their order along the way
	 */
	@Test
	public void testForwardForwardPickReversePickForward() throws IOException{
		this.startSiteController();

		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		//Go forward
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getRemainingPicksWiList();
		String[] expectations1 = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations1);
		
		//Pick some items
		pickNextAndCompare(picker, "Item2");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		pickNextAndCompare(picker, "Item3");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		//Check the remaining list
		scWiList = picker.getRemainingPicksWiList();
		String[] expectations2 = {"Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations2);
		
		//Go reverse
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		scWiList = picker.getRemainingPicksWiList();
		String[] expectations3 = {"Item15","Item11","Item9","Item7","Item5"};
		compareList(scWiList, expectations3);
		
		//Pick item
		pickNextAndCompare(picker, "Item15");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
		
		//Go forward and jump
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		scWiList = picker.getRemainingPicksWiList();
		String[] expectations4 = {"Item9","Item11","Item5","Item7"};
		compareList(scWiList, expectations4);
	}
}
