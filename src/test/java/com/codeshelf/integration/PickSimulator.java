package com.codeshelf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.IntegrationTest;

public class PickSimulator {

	IntegrationTest				test;

	@Getter
	CheDeviceLogic				cheDeviceLogic;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PickSimulator.class);

	public PickSimulator(IntegrationTest test, NetGuid cheGuid) {
		this.test = test;
		// verify that che is in site controller's device list
		cheDeviceLogic = (CheDeviceLogic) test.getDeviceManager().getDeviceByGuid(cheGuid);
		Assert.assertNotNull(cheDeviceLogic);
	}

	public void login(String pickerId) {
		// This is the original "scan badge" scan for setuporders process that assumes transition to Container_Select state.
		cheDeviceLogic.scanCommandReceived("U%" + pickerId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void loginAndCheckState(String pickerId, CheStateEnum inState) {
		// This is the "scan badge" scan
		cheDeviceLogic.scanCommandReceived("U%" + pickerId);
		waitForCheState(inState, 2000);
	}

	public String getProcessType() {
		// what process mode are we using?
		return cheDeviceLogic.getDeviceType();
	}

	public void setup() {
		// The happy case. Scan setup needed after completing a cart run. Still logged in.
		scanCommand("SETUP");
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void setupOrderIdAsContainer(String orderId, String positionId) {
		// DEV-518. Also accept the raw order ID as the containerId
		scanOrderId(orderId);
		waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);

		scanPosition(positionId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void setupContainer(String containerId, String positionId) {
		// used for normal success case of scan container, then position on cart.
		scanContainer(containerId);
		waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);

		scanPosition(positionId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void startAndSkipReview(String location, int inComputeTimeOut, int inLocationTimeOut) {
		// This is both "Start pick" and scan of starting location.
		// Note: if no jobs at all, it will fail on waiting.
		scanCommand("START");
		if (location == null) {
			// perform start without location scan, if location is undefined
			return;
		}
		waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, inComputeTimeOut);
		scanLocation(location);
		waitForCheState(CheStateEnum.DO_PICK, inLocationTimeOut);
	}

	public void pick(int position, int quantity) {
		buttonPress(position, quantity);
		// many state possibilities here. On to the next job, or finished all work, or need to confirm a short.
		// If the job finished, we would want to end the transaction as it does in production, but confirm short has nothing to commit yet.
	}

	/*	public void simulateCommitByChangingTransaction(PersistenceService inService) {
			// This would normally be done with the message boundaries. But as an example, see buttonPress(). In production the button message is formed and sent to server. But in this
			// pickSimulation, we form button command, and tell cheDeviceLogic to directly process it, as if it were just deserialized after receiving. No transaction boundary there.
			if (inService == null || !inService.hasActiveTransaction()) {
				LOGGER.error("bad call to simulateCommitByChangingTransaction");
			} else {
				inService.commitTransaction();
				inService.beginTransaction();
			}
		}
	*/
	public void logout() {
		scanCommand("LOGOUT");
		waitForCheState(CheStateEnum.IDLE, 1000);
	}

	/**
	 * This helps the test writer scanSomething may well include the % to simulate a command. But the test writer may have included extra in scanCommand, scanLocation, etc.
	 */
	private void checkExtraPercent(String inCommand){
		if (inCommand != null && inCommand.length() > 2) {
			if (inCommand.charAt(1) == '%'){
				LOGGER.error("Did you mean to use scanSomething() instead?");
			}
		}
	}

	// Extremely primitive commands here, useful for testing the state machine for error conditions, as well as using in high-level commands.
	public void scanCommand(String inCommand) {
		// Valid commands currently are only START, SETUP, LOGOUT, SHORT, YES, NO, See https://codeshelf.atlassian.net/wiki/display/TD/Bar+Codes+in+Codeshelf+Application
		checkExtraPercent(inCommand);
		cheDeviceLogic.scanCommandReceived("X%" + inCommand);
	}

	public void scanLocation(String inLocation) {
		checkExtraPercent(inLocation);
		cheDeviceLogic.scanCommandReceived("L%" + inLocation);
	}

	/**
	 * Adds the C% to conform with Codeshelf scan specification
	 */
	public void scanContainer(String inContainerId) {
		checkExtraPercent(inContainerId);
		cheDeviceLogic.scanCommandReceived("C%" + inContainerId);
	}

	/**
	 * Does not add C% or anything else to conform with Codeshelf scan specification. DEV-518. Also accept the raw order ID.
	 */
	public void scanOrderId(String inOrderId) {
		cheDeviceLogic.scanCommandReceived(inOrderId);
	}

	/**
	 * Same as scanOrderId. DEV-621. Just given this name for clarity of JUnit tests.
	 */
	public void scanOrderDetailId(String inOrderDetailId) {
		cheDeviceLogic.scanCommandReceived(inOrderDetailId);
	}

	/**
	 * Same as scanOrderId. DEV-653. Just given this name for clarity of JUnit tests. (Scan the SKU, or UPC, or license plate)
	 */
	public void scanSomething(String inSomething) {
		cheDeviceLogic.scanCommandReceived(inSomething);
	}

	public void scanPosition(String inPositionId) {
		cheDeviceLogic.scanCommandReceived("P%" + inPositionId);
	}

	public void buttonPress(int inPosition, int inQuantity) {
		cheDeviceLogic.simulateButtonPress(inPosition, inQuantity);
	}

	// Useful, primitive methods for checking the result after some actions
	public int countRemainingJobs() {
		int count = 0;
		for (WorkInstruction wi : getAllPicksList()) {
			WorkInstructionStatusEnum status = wi.getStatus();
			if (status == WorkInstructionStatusEnum.NEW)
				count++;
		}
		;
		return count;
	}

	public WorkInstruction nextActiveWi() {
		return getActivePickList().get(0); // This may return null
	}

	public int countActiveJobs() {
		return getActivePickList().size(); //  0 if out of work. Usually 1. Only higher for simultaneous pick work instructions.
	}

	public CheStateEnum currentCheState() {
		return cheDeviceLogic.getCheStateEnum();
	}

	public String getPickerTypeAndState(String inPrefix) {
		return inPrefix + " " + getProcessType() + ": State is " + currentCheState();
	}

	public int buttonFor(WorkInstruction inWorkInstruction) {
		// returns 0 if none
		if (!getActivePickList().contains(inWorkInstruction))
			return 0;
		byte button = cheDeviceLogic.getPosconIndexOfWi(inWorkInstruction);
		return button;
	}

	/**
	 * Careful: returns the actual list from the cheDeviceLogic. Also note this list only ever has one job, except for the
	 * simultaneous work instruction situation. This not usually very useful.
	 */
	public List<WorkInstruction> getActivePickList() {
		List<WorkInstruction> activeList = cheDeviceLogic.getActivePickWiList();
		return activeList;
	}

	/**
	 * Careful: simultaneous work instruction situation might have more than one active pick
	 * return null if none, or the WI if 1. Fails if more than one.
	 */
	public WorkInstruction getActivePick() {
		List<WorkInstruction> activeList = getActivePickList();
		int count = activeList.size();
		if (count == 0)
			return null;
		else if (count == 1)
			return activeList.get(0);
		else {
			Assert.fail("More than one active pick. Use getActivePickList() instead"); // and know what you are doing.
			return null;
		}
	}

	/**
	 * Careful: returns the actual list from the cheDeviceLogic. This has all the wis, many completed.
	 * This is not usually very useful.
	 */
	public List<WorkInstruction> getAllPicksList() {
		List<WorkInstruction> activeList = cheDeviceLogic.getAllPicksWiList();
		return activeList;
	}

	/**
	 * Careful: returns the actual list from the cheDeviceLogic.
	 * This is intended to return all NEW and INPROGRESS instructions that will appear on the che
	 */
	public List<WorkInstruction> getRemainingPicksWiList() {
		List<WorkInstruction> fullList = cheDeviceLogic.getAllPicksWiList();
		List<WorkInstruction> remainingInstructions = new ArrayList<>();
		for (WorkInstruction instruction : fullList) {
			WorkInstructionStatusEnum status = instruction.getStatus();
			if (status == WorkInstructionStatusEnum.NEW || status == WorkInstructionStatusEnum.INPROGRESS) {
				remainingInstructions.add(instruction);
			}
		}
		return remainingInstructions;
	}

	/**
	 * Returns the new list with each work instruction fetched from the DAO. Should represent how the server sees it.
	 */
	public List<WorkInstruction> getServerVersionAllPicksList() {
		List<WorkInstruction> activeList = cheDeviceLogic.getAllPicksWiList();
		List<WorkInstruction> serversList = new ArrayList<WorkInstruction>();
		for (WorkInstruction wi : activeList) {
			UUID theId = wi.getPersistentId();
			WorkInstruction fullWi = WorkInstruction.staticGetDao().findByPersistentId(theId);
			serversList.add(fullWi);
		}

		return serversList;
	}

	/**
	 * Returns a new list with each work instruction fetched from the DAO.
	 * Usually obtain the input list from getServerVersionAllPicksList. Don't get it from getAllPicksList
	 * as that returns the raw cheDeviceLogic list that usually has changed.
	 */
	public List<WorkInstruction> getCurrentWorkInstructionsFromList(List<WorkInstruction> inList) {
		List<WorkInstruction> currentList = new ArrayList<WorkInstruction>();
		for (WorkInstruction wi : inList) {
			UUID theId = wi.getPersistentId();
			WorkInstruction fullWi = WorkInstruction.staticGetDao().findByPersistentId(theId);
			if (fullWi != null)
				currentList.add(fullWi);
		}
		return currentList;
	}

	public void waitForCheState(CheStateEnum state, int timeoutInMillis) {
		CheStateEnum lastState = cheDeviceLogic.waitForCheState(state, timeoutInMillis);
		if (!state.equals(lastState)) {
			String theProblem = String.format("Che state %s not encountered in %dms. State is %s, inSetState: %s",
				state,
				timeoutInMillis,
				lastState,
				cheDeviceLogic.inSetState());
			Assert.fail(theProblem);
		}
	}

	public boolean hasLastSentInstruction(byte position) {
		return cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)
				|| cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL);
	}

	public Byte getLastSentPositionControllerDisplayValue(byte position) {
		return cheDeviceLogic.getLastSentPositionControllerDisplayValue(position);
	}

	public Byte getLastSentPositionControllerDisplayFreq(byte position) {
		return cheDeviceLogic.getLastSentPositionControllerDisplayFreq(position);
	}

	public Byte getLastSentPositionControllerDisplayDutyCycle(byte position) {
		return cheDeviceLogic.getLastSentPositionControllerDisplayDutyCycle(position);
	}

	public Byte getLastSentPositionControllerMinQty(byte position) {
		return cheDeviceLogic.getLastSentPositionControllerMinQty(position);
	}

	public Byte getLastSentPositionControllerMaxQty(byte position) {
		return cheDeviceLogic.getLastSentPositionControllerMaxQty(position);
	}

	public String getLastCheDisplayString(int lineIndex) {
		return cheDeviceLogic.getRecentCheDisplayString(lineIndex);
	}
	public void logCheDisplay() {
		LOGGER.info("Line1:{} Line2:{} Line3:{} Line4:{}",getLastCheDisplayString(1),getLastCheDisplayString(2),getLastCheDisplayString(3),getLastCheDisplayString(4));
	}

	public void forceDeviceToMatchManagerConfiguration() {
		cheDeviceLogic.updateConfigurationFromManager();
	}

}
