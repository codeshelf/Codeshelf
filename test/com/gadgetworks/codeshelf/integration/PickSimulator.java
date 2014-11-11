package com.gadgetworks.codeshelf.integration;

import java.util.List;

import lombok.Getter;

import org.junit.Assert;

import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.NetGuid;

public class PickSimulator {

	EndToEndIntegrationTest	test;

	@Getter
	CheDeviceLogic			cheDeviceLogic;

	public PickSimulator(EndToEndIntegrationTest test, NetGuid cheGuid) {
		this.test = test;
		// verify that che is in site controller's device list
		cheDeviceLogic = (CheDeviceLogic) test.getSiteController().getDeviceManager().getDeviceByGuid(cheGuid);
		Assert.assertNotNull(cheDeviceLogic);
	}

	public void login(String pickerId) {
		// This is the "scan badge" scan
		cheDeviceLogic.scanCommandReceived("U%" + pickerId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}
	
	public void setup() {
		// The happy case. Scan setup needed after completing a cart run. Still logged in.
		scanCommand("SETUP");
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void setupContainer(String containerId, String positionId) {
		// used for normal success case of scan container, then position on cart.
		scanContainer(containerId);
		waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);

		scanPosition(positionId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
	}

	public void start(String location, int inComputeTimeOut, int inLocationTimeOut) {
		// This is both "Start pick" and scan of starting location.
		// Note: if no jobs at all, it will fail on waiting.
		scanCommand("START");
		if (location == null) {
			// perform start without location scan, if location is undefined
			return;
		}
		waitForCheState(CheStateEnum.LOCATION_SELECT, inComputeTimeOut);
		scanLocation(location);
		waitForCheState(CheStateEnum.DO_PICK, inLocationTimeOut);
	}

	public void pick(int position, int quantity) {
		buttonPress(position, quantity);
		// many state possibilities here. On to the next job, or finished all work, or need to confirm a short.
	}

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

	public void scanContainer(String inContainerId) {
		cheDeviceLogic.scanCommandReceived("C%" + inContainerId);
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
			WorkInstructionStatusEnum status = wi.getStatusEnum();
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
	
	public int buttonFor(WorkInstruction inWorkInstruction){
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
	 * Careful: returns the actual list from the cheDeviceLogic. This has all the wis, many completed.
	 * This is not usually very useful.
	 */
	public List<WorkInstruction> getAllPicksList() {
		List<WorkInstruction> activeList = cheDeviceLogic.getAllPicksWiList();
		return activeList;
	}

	public void waitForCheState(CheStateEnum state, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			if (cheDeviceLogic.getCheStateEnum() == state) {
				// expected state found - all good
				return;
			}
		}
		CheStateEnum existingState = cheDeviceLogic.getCheStateEnum();
		Assert.fail("Che state " + state + " not encountered in " + timeoutInMillis + "ms. State is " + existingState);
	}
}
