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
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.ThreadUtils;

public class PickSimulator {

	EndToEndIntegrationTest		test;

	@Getter
	CheDeviceLogic				cheDeviceLogic;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PickSimulator.class);

	public PickSimulator(EndToEndIntegrationTest test, NetGuid cheGuid) {
		this.test = test;
		// verify that che is in site controller's device list
		cheDeviceLogic = (CheDeviceLogic) test.getSiteController().getDeviceManager().getDeviceByGuid(cheGuid);
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
		waitForCheState(inState, 1000);
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

	// Extremely primitive commands here, useful for testing the state machine for error conditions, as well as using in high-level commands.
	public void scanCommand(String inCommand) {
		// Valid commands currently are only START, SETUP, LOGOUT, SHORT, YES, NO, See https://codeshelf.atlassian.net/wiki/display/TD/Bar+Codes+in+Codeshelf+Application
		cheDeviceLogic.scanCommandReceived("X%" + inCommand);
	}

	public void scanLocation(String inLocation) {
		cheDeviceLogic.scanCommandReceived("L%" + inLocation);
	}

	/**
	 * Adds the C% to conform with Codeshelf scan specification
	 */
	public void scanContainer(String inContainerId) {
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

	public void scanPosition(String inPositionId) {
		cheDeviceLogic.scanCommandReceived("P%" + inPositionId);
	}

	public void buttonPress(int inPosition, int inQuantity) {
		// Caller's responsibility to get the quantity correct. Normally match the planQuantity. Normally only lower after SHORT command.
		CommandControlButton buttonCommand = new CommandControlButton();
		buttonCommand.setPosNum((byte) inPosition);
		buttonCommand.setValue((byte) inQuantity);
		cheDeviceLogic.buttonCommandReceived(buttonCommand);
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
		byte button = cheDeviceLogic.buttonFromContainer(inWorkInstruction.getContainerId());
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
	 * Returns the new list with each work instruction fetched from the DAO. Should represent how the server sees it.
	 */
	public List<WorkInstruction> getServerVersionAllPicksList() {
		List<WorkInstruction> activeList = cheDeviceLogic.getAllPicksWiList();
		List<WorkInstruction> serversList = new ArrayList<WorkInstruction>();
		for (WorkInstruction wi : activeList) {
			UUID theId = wi.getPersistentId();
			WorkInstruction fullWi = WorkInstruction.DAO.findByPersistentId(theId);
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
			WorkInstruction fullWi = WorkInstruction.DAO.findByPersistentId(theId);
			currentList.add(fullWi);
		}
		return currentList;
	}

	public void waitForCheState(CheStateEnum state, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			CheStateEnum currentState = cheDeviceLogic.getCheStateEnum();
			// we are waiting for the expected CheStateEnum, AND the indicator that we are out of the setState() routine.
			// Typically, the state is set first, then some side effects are called that depend on the state.  The picker is usually checking on
			// some of the side effects after this call.
			if (currentState.equals(state) && !cheDeviceLogic.inSetState()) {
				// expected state found - all good
				return;
			}
		}
		CheStateEnum existingState = cheDeviceLogic.getCheStateEnum();
		String theProblem = "Che state " + state + " not encountered in " + timeoutInMillis + "ms. State is " + existingState;
		LOGGER.error(theProblem);
		Assert.fail(theProblem);
	}

	public boolean hasLastSentInstruction(byte position) {
		return cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)
				|| cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL);
	}

	public Byte getLastSentPositionControllerDisplayValue(byte position) {
		if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(position).getReqQty();
		} else if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getReqQty();
		} else {
			return null;
		}

	}

	public Byte getLastSentPositionControllerDisplayFreq(byte position) {
		if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(position).getFreq();
		} else if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getFreq();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerDisplayDutyCycle(byte position) {
		if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(position).getDutyCycle();
		} else if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getDutyCycle();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerMinQty(byte position) {
		if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(position).getMinQty();
		} else if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMinQty();
		} else {
			return null;
		}

	}

	public Byte getLastSentPositionControllerMaxQty(byte position) {
		if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(position).getMaxQty();
		} else if (cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return cheDeviceLogic.getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMaxQty();
		} else {
			return null;
		}

	}

	/**
	 * Intentionally incomplete. Could parameterize for each line, but initially only remember the first line.
	 */
	public String getLastCheDisplayString() {
		return cheDeviceLogic.getRecentCheDisplayString();
	}

}