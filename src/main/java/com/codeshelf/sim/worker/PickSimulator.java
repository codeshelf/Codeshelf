package com.codeshelf.sim.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.CommandControlCreateButton;
import com.codeshelf.flyweight.command.CommandControlCreateScan;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.ThreadUtils;
import com.google.common.collect.Lists;

public class PickSimulator {

	@Getter
	CheDeviceLogic				cheDeviceLogic;

	@Getter
	@Setter
	private boolean				useRadio			= false;

	private static final Logger	LOGGER				= LoggerFactory.getLogger(PickSimulator.class);

	private final int			RADIO_SEND_DELAY	= 500;

	public PickSimulator(CsDeviceManager deviceManager, String cheGuid) {
		this(deviceManager, new NetGuid(cheGuid));
	}

	public PickSimulator(CsDeviceManager deviceManager, NetGuid cheGuid) {
		// verify that che is in site controller's device list
		cheDeviceLogic = (CheDeviceLogic) deviceManager.getDeviceByGuid(cheGuid);
		if (cheDeviceLogic == null) {
			throw new IllegalArgumentException("No che found with guid: " + cheGuid);
		}
	}

	private int getWaitTime() {
		return useRadio ? 6000 : 4000;
	}

	public void login(String pickerId) {
		waitForCheState(CheStateEnum.IDLE, getWaitTime());
		scanUser(pickerId);
		ArrayList<CheStateEnum> loginStates = Lists.newArrayList();
		loginStates.add(CheStateEnum.SETUP_SUMMARY);
		loginStates.add(CheStateEnum.READY);
		loginStates.add(CheStateEnum.REMOTE);
		loginStates.add(CheStateEnum.PALLETIZER_SCAN_ITEM);
		waitForCheStates(loginStates, getWaitTime());
	}

	public void loginAndSetup(String pickerId) {
		waitForCheState(CheStateEnum.IDLE, getWaitTime());
		scanUser(pickerId);
		waitForCheStates(states(CheStateEnum.SETUP_SUMMARY, CheStateEnum.REMOTE), getWaitTime());
		if (getCurrentCheState() == CheStateEnum.REMOTE) {
			scanCommand("CANCEL");
			//Since we are going from state REMOTE to REMOTE, give CHE a bit of time to enter a transactional state
			ThreadUtils.sleep(500);
			waitForCheState(CheStateEnum.REMOTE, getWaitTime());
			scanCommand("CANCEL");
			waitForCheState(CheStateEnum.SETUP_SUMMARY, getWaitTime());
		}
		scanCommand("SETUP");
		waitForCheState(CheStateEnum.CONTAINER_SELECT, getWaitTime());
	}

	public void loginAndRemoteLink(String pickerId, String connectTo) {
		waitForCheState(CheStateEnum.IDLE, getWaitTime());
		scanUser(pickerId);
		//If this CHE was previously used as a remote controller, it will go into REMOTE state after the badge scan
		waitForCheStates(states(CheStateEnum.SETUP_SUMMARY, CheStateEnum.REMOTE), getWaitTime());
		//If CHE goes into SETUP_SUMMARY state, scan the REMOTE command to go to REMOTE state
		if (getCurrentCheState() == CheStateEnum.SETUP_SUMMARY) {
			scanCommand("REMOTE");
			waitForCheState(CheStateEnum.REMOTE, getWaitTime());
		}
		scanSomething("H%" + connectTo);
		waitForCheState(CheStateEnum.REMOTE_LINKED, getWaitTime());
	}

	public void loginAndCheckState(String pickerId, CheStateEnum inState) {
		waitForCheState(CheStateEnum.IDLE, getWaitTime());
		// This only does the login ("scan badge" scan). Especially in Line_Scan process, this is used in tests rather than loginAndSetup.
		scanUser(pickerId);
		// badge authorization now takes longer. Trip to server and back
		waitForCheState(inState, getWaitTime());
	}

	public String getProcessType() {
		// what process mode are we using?
		return cheDeviceLogic.getDeviceType();
	}

	public void setup() {
		// The happy case. Scan setup needed after completing a cart run. Still logged in.
		scanCommand("SETUP");
		waitForCheState(CheStateEnum.CONTAINER_SELECT, getWaitTime());
	}

	/**
	 * Typical use is picker.inventoryViaTape("gtin1123", "%004290590250");
	 * where you pass the actual scan from the tape. 
	 * However, it will also work as picker.inventoryViaTape("gtin1123", "L%D23");
	 */
	public void inventoryViaTape(String gtin, String tapeScan) {
		scanSomething(gtin);
		// not really right. Stays in SCAN_GTIN state. Perhaps it should transition to other state and wait for server response to get back to state.
		waitForCheState(CheStateEnum.SCAN_GTIN, getWaitTime());
		scanSomething(tapeScan);
		waitForCheState(CheStateEnum.SCAN_GTIN, getWaitTime());
	}

	public void setupOrderIdAsContainer(String orderId, String positionId) {
		// DEV-518. Also accept the raw order ID as the containerId
		scanOrderId(orderId);
		waitForCheState(CheStateEnum.CONTAINER_POSITION, getWaitTime());

		scanPosition(positionId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT, getWaitTime());
	}

	/**
	 * Should be in state PUT_WALL_SCAN_ORDER when called. 
	 * Usually scan ORDER_WALL, then wait for PUT_WALL_SCAN_ORDER, then start calling this.
	 * Do not include any prefix in the parameters.
	 */
	public void setOrderToPutWall(String orderId, String locationName) {
		if ((orderId.length() > 1) && (orderId.charAt(1) == '%'))
			LOGGER.error("orderId in setOrderToPutWall should not take percent");

		if ((locationName.length() > 1) && (locationName.charAt(1) == '%'))
			LOGGER.error("location in setOrderToPutWall should not take percent. Just the alias name.");

		scanSomething(orderId);
		waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, getWaitTime());
		scanLocation(locationName);
		waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, getWaitTime());
	}

	public void setupContainer(String containerId, String positionId) {
		// used for normal success case of scan container, then position on cart.
		scanContainer(containerId);
		waitForThisOrLinkedCheState(CheStateEnum.CONTAINER_POSITION, getWaitTime());

		scanPosition(positionId);
		waitForThisOrLinkedCheState(CheStateEnum.CONTAINER_SELECT, getWaitTime());
	}

	public void startAndSkipReview(String location, int inComputeTimeOut, int inLocationTimeOut) {
		// This is both "Start pick" and scan of starting location.
		// Note: if no jobs at all, it will fail on waiting.
		scanCommand("START");
		if (location == null) {
			// perform start without location scan, if location is undefined
			return;
		}
		waitForCheState(CheStateEnum.SETUP_SUMMARY, inComputeTimeOut);
		scanLocation(location);
		waitForCheState(CheStateEnum.DO_PICK, inLocationTimeOut);
	}

	public void pick(int position, int quantity) {
		buttonPress(position, quantity);
		// many state possibilities here. On to the next job, or finished all work, or need to confirm a short.
		// If the job finished, we would want to end the transaction as it does in production, but confirm short has nothing to commit yet.
	}

	public void pickItemAuto() {
		waitForCheState(CheStateEnum.DO_PICK, getWaitTime());
		WorkInstruction wi = getFirstActivePick();
		int button = buttonFor(wi);
		int quantity = wi.getPlanQuantity();
		pick(button, quantity);
	}

	public void logout() {
		scanCommand("LOGOUT");
		waitForCheState(CheStateEnum.IDLE, getWaitTime());
	}

	/**
	 * This helps the test writer scanSomething may well include the % to simulate a command. But the test writer may have included extra in scanCommand, scanLocation, etc.
	 */
	private void checkExtraPercent(String inCommand) {
		if (inCommand != null && inCommand.length() > 2) {
			if (inCommand.charAt(1) == '%') {
				LOGGER.error("Did you mean to use scanSomething() instead?");
			}
		}
	}

	// Extremely primitive commands here, useful for testing the state machine for error conditions, as well as using in high-level commands.
	public void scanCommand(String inCommand) {
		// Valid commands currently are only START, SETUP, LOGOUT, SHORT, YES, NO, See https://codeshelf.atlassian.net/wiki/display/TD/Bar+Codes+in+Codeshelf+Application
		checkExtraPercent(inCommand);
		scan("X%" + inCommand);
	}

	public void scanLocation(String inLocation) {
		checkExtraPercent(inLocation);
		scan("L%" + inLocation);
	}

	/**
	 * Adds the C% to conform with Codeshelf scan specification
	 */
	public void scanContainer(String inContainerId) {
		checkExtraPercent(inContainerId);
		scan("C%" + inContainerId);
	}

	/**
	 * Does not add C% or anything else to conform with Codeshelf scan specification. DEV-518. Also accept the raw order ID.
	 */
	public void scanOrderId(String inOrderId) {
		scan(inOrderId);
	}

	/**
	 * Same as scanOrderId. DEV-621. Just given this name for clarity of JUnit tests.
	 */
	public void scanOrderDetailId(String inOrderDetailId) {
		scan(inOrderDetailId);
	}

	/**
	 * Same as scanOrderId. DEV-653. Just given this name for clarity of JUnit tests. (Scan the SKU, or UPC, or license plate)
	 */
	public void scanSomething(String inSomething) {
		scan(inSomething);
	}

	public void scanPosition(String inPositionId) {
		scan("P%" + inPositionId);
	}

	private void scanUser(String pickerId) {
		scan("U%" + pickerId);
	}

	private void scan(String scan) {
		String rememberedGuid = ContextLogging.rememberThenSetNetGuid(cheDeviceLogic.getGuid());
		try {
			if (useRadio) {
				CommandControlCreateScan command = new CommandControlCreateScan(NetEndpoint.PRIMARY_ENDPOINT, scan);
				cheDeviceLogic.sendRadioControllerCommand(command, true);
				ThreadUtils.sleep(RADIO_SEND_DELAY);
			} else {
				cheDeviceLogic.scanCommandReceived(scan);
			}
		} finally {
			ContextLogging.restoreNetGuid(rememberedGuid);
		}
	}

	public void buttonPress(int inPosition, int inQuantity) {
		String rememberedGuid = ContextLogging.rememberThenSetNetGuid(cheDeviceLogic.getGuid());
		try {
			if (useRadio) {
				CommandControlCreateButton command = new CommandControlCreateButton(NetEndpoint.PRIMARY_ENDPOINT,
					(byte) inPosition,
					(byte) inQuantity);
				cheDeviceLogic.sendRadioControllerCommand(command, true);
				ThreadUtils.sleep(RADIO_SEND_DELAY);
			} else {
				cheDeviceLogic.simulateButtonPress(inPosition, inQuantity);
			}
		} finally {
			ContextLogging.restoreNetGuid(rememberedGuid);
		}
	}

	public void buttonPress(int inPosition) {
		buttonPress(inPosition, getLastSentPositionControllerDisplayValue(inPosition));
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

	public CheStateEnum getCurrentCheState() {
		return cheDeviceLogic.getCheStateEnum();
	}

	public CheStateEnum getThisOrLinkedCurrentCheState() {
		return getDeviceToAsk().getCheStateEnum();
	}

	public String getPickerTypeAndState(String inPrefix) {
		return inPrefix + " " + getProcessType() + ": State is " + getCurrentCheState();
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
			throw new IllegalStateException("More than one active pick. Use getActivePickList() instead"); // and know what you are doing.
		}
	}

	public WorkInstruction getFirstActivePick() {
		List<WorkInstruction> instructions = getActivePickList();
		if (instructions == null || instructions.isEmpty()) {
			return null;
		} else {
			return instructions.get(0);
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

	/**
	 * This waits for state for the picker's linked device only. If not linked, it throws causing the test to fail
	 */
	public void waitForLinkedCheState(CheStateEnum state, int timeoutInMillis) {
		CheDeviceLogic deviceToAsk = getDeviceToAsk();
		if (deviceToAsk.equals(cheDeviceLogic)) {
			String theProblem = "picker's CHE not linked";
			throw new IllegalStateException(theProblem);
		}
		waitForDeviceState(deviceToAsk, state, timeoutInMillis);
	}

	/**
	 * waitForCheState() is the primary API
	 * This waits for state for the picker's device only. If in remoteLinked state, it returns REMOTE_LINKED
	 */
	public void waitForCheState(CheStateEnum state, int timeoutInMillis) {
		waitForDeviceState(cheDeviceLogic, state, timeoutInMillis);
	}

	/**
	 * If this CHE is linked, wait for state on another che. If now, wait for state on this CHE
	 */
	public void waitForThisOrLinkedCheState(CheStateEnum state, int timeoutInMillis) {
		waitForDeviceState(getDeviceToAsk(), state, timeoutInMillis);
	}

	/**
	 * If this CHE is linked, wait for state on another che. If now, wait for state on this CHE
	 */
	public void waitForThisOrLinkedCheStates(ArrayList<CheStateEnum> states, int timeoutInMillis) {
		waitForDeviceStates(getDeviceToAsk(), states, timeoutInMillis);
	}

	public void waitForCheStates(ArrayList<CheStateEnum> states, int timeoutInMillis) {
		waitForDeviceStates(cheDeviceLogic, states, timeoutInMillis);
	}

	/**
	 * This is called in test when we expect nothing much to happen.
	 * Use this sparingly! The test slows by this much each call.
	 */
	public void waitInSameState(CheStateEnum state, int millisToWait) {
		CheStateEnum startingState = cheDeviceLogic.getCheStateEnum();
		if (!startingState.equals(state)) {
			throw new IllegalStateException(getCheDeviceLogic().getGuid() + ": did not start in expected state");
		}
		ThreadUtils.sleep(millisToWait);
		if (!startingState.equals(cheDeviceLogic.getCheStateEnum())) {
			throw new IllegalStateException(getCheDeviceLogic().getGuid() + ": did not remain in same state");
		}
	}

	/**
	 * Wait for specified state. Throw if state does not come in time, causing the test to fail.
	 */
	private void waitForDeviceState(CheDeviceLogic device, CheStateEnum state, int timeoutInMillis) {
		ArrayList<CheStateEnum> states = Lists.newArrayList();
		states.add(state);
		waitForDeviceStates(device, states, timeoutInMillis);
	}

	/**
	 * Method used for script testing, where a Che may transition to one of several states, and we'd like to wait for transition to finish before proceeding
	 */
	private CheStateEnum waitForDeviceStates(CheDeviceLogic device, ArrayList<CheStateEnum> states, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		CheStateEnum currentState = null;
		while (System.currentTimeMillis() - start < timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			currentState = device.getCheStateEnum();
			// we are waiting for the expected CheStateEnum, AND the indicator that we are out of the setState() routine.
			// Typically, the state is set first, then some side effects are called that depend on the state.  The picker is usually checking on
			// some of the side effects after this call.
			if (states.contains(currentState) && !device.inSetState()) {
				// expected state found - all good
				return currentState;
			}
		}
		//Exception code below
		StringBuilder statesStr = new StringBuilder();
		for (CheStateEnum state : states) {
			statesStr.append(state).append(" ");
		}
		String theProblem = String.format("Che states %snot encountered in %dms. State is %s, inSetState: %s",
			statesStr.toString(),
			timeoutInMillis,
			currentState,
			device.inSetState());
		throw new IllegalStateException(getCheDeviceLogic().getGuid() + ": " + theProblem);
	}

	public boolean hasLastSentInstruction(byte position) {
		return cheDeviceLogic.getPosToLastSetIntrMap().containsKey(position)
				|| cheDeviceLogic.getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL);
	}

	public Byte getLastSentPositionControllerDisplayValue(int position) {
		return getLastSentPositionControllerDisplayValue((byte) position);
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

	public void assertPosconDisplayOc(int inPosition) {
		byte position = (byte) inPosition;
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, getLastSentPositionControllerDisplayValue(position));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_C, getLastSentPositionControllerMinQty(position));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_O, getLastSentPositionControllerMaxQty(position));
	}

	public void assertPosconDisplayDash(int inPosition) {
		byte position = (byte) inPosition;
		Assert.assertEquals(PosControllerInstr.BITENCODED_SEGMENTS_CODE, getLastSentPositionControllerDisplayValue(position));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, getLastSentPositionControllerMinQty(position));
		Assert.assertEquals(PosControllerInstr.BITENCODED_LED_DASH, getLastSentPositionControllerMaxQty(position));
	}

	/**
	 * Returns the picker's own device, unless it is in REMOTE_LINKED state. In which case it returns the link-to device
	 */
	private CheDeviceLogic getDeviceToAsk() {
		CheDeviceLogic deviceToAsk = cheDeviceLogic;
		if (CheStateEnum.REMOTE_LINKED.equals(cheDeviceLogic.getCheStateEnum())) {
			deviceToAsk = cheDeviceLogic.getLinkedCheDevice();
			if (deviceToAsk == null)
				deviceToAsk = cheDeviceLogic;
		}
		return deviceToAsk;
	}

	public String getLastCheDisplayString(int lineIndex) {
		// If a cloned screen we need the other cheDeviceLog.
		CheDeviceLogic deviceToAsk = getDeviceToAsk();
		return deviceToAsk.getRecentCheDisplayString(lineIndex);
	}

	public void logCheDisplay() {
		// each line below may get the linked screen line. So the effect is logging the linked screen. 
		// No need for another getDeviceToAsk() call.
		LOGGER.info("{} SCREEN Line1:{} Line2:{} Line3:{} Line4:{}",
			cheDeviceLogic.getGuidNoPrefix(),
			getLastCheDisplayString(1),
			getLastCheDisplayString(2),
			getLastCheDisplayString(3),
			getLastCheDisplayString(4));
	}

	public String getLastCheDisplay() {
		StringBuffer s = new StringBuffer();
		for (int i = 1; i <= 4; i++) {
			s.append(getLastCheDisplayString(i).trim()).append("\n");
		}
		return s.toString();
	}

	public void forceDeviceToMatchManagerConfiguration() {
		cheDeviceLogic.updateConfigurationFromManager();
	}

	public static ArrayList<CheStateEnum> states(CheStateEnum state1, CheStateEnum state2) {
		ArrayList<CheStateEnum> states = Lists.newArrayListWithCapacity(2);
		states.add(state1);
		states.add(state2);
		return states;
	}

	public static ArrayList<CheStateEnum> states(CheStateEnum state1, CheStateEnum state2, CheStateEnum state3) {
		ArrayList<CheStateEnum> states = Lists.newArrayListWithCapacity(2);
		states.add(state1);
		states.add(state2);
		states.add(state3);
		return states;
	}
}
