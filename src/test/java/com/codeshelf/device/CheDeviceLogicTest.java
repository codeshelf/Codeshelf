package com.codeshelf.device;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.MockDaoTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Unit test with mocks for CheDeviceLogic
 *
 * @author pmonteiro
 *
 */
public class CheDeviceLogicTest extends MockDaoTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheDeviceLogicTest.class);

	// Removed cheSetupAfterCompleteClearsPosCon() because CHE setup persistence changes this, and we have very good realistic tests for this.
	// The rest are passing but do not assume they are working correctly. If there is a new failure here, evaluate carefully. These unit tests
	// are much faster to run than the integration tests, but these are only better if they are absolutely clear about what the Che process is.

	@Test
	public void showsCompleteWorkAfterPicks() {
		this.getTenantPersistenceService().beginTransaction();

		int chePosition = 1;

		Facility facility = new FacilityGenerator().generateValid();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		WorkInstruction wi = new WorkInstructionGenerator().generateWithNewStatus(facility);
		List<WorkInstruction> wiToDo = ImmutableList.of(wi);
		this.getTenantPersistenceService().commitTransaction(); // end of transaction for this test

		IRadioController radioController = mock(IRadioController.class);

		// CheDeviceLogic is now an abstract base class
		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + wi.getContainerId());

		cheDeviceLogic.scanCommandReceived("P%" + chePosition);

		cheDeviceLogic.scanCommandReceived("X%START");

		LOGGER.info("This test only generates valid orders (no shorts etc). LOCATION_REVIEW_SELECT should never be entered.");
		//We will pass in a map containing good counts with no bad counts.
		Map<String, WorkInstructionCount> containerToWorkInstructionMap = new HashMap<String, WorkInstructionCount>();
		containerToWorkInstructionMap.put(wi.getContainerId(), new WorkInstructionCount(wi.getPlanQuantity(), 0, 0, 0, 0));

		cheDeviceLogic.processWorkInstructionCounts(wiToDo.size(), containerToWorkInstructionMap);

		cheDeviceLogic.scanCommandReceived("L%ANYLOCATIONBEFOREPICK");

		/*
		 * Remove in favor of our better integration tests
		cheDeviceLogic.assignWork(wiToDo);


		pressButton(cheDeviceLogic, chePosition, wi.getPlanQuantity());

		//MAKE SURE COMPLETE IS HAPPENING AT LEAST AFTER DISPLAY OF PICK INSTRUCTION
		InOrder ordered = inOrder(radioController);
		verifyDisplayOfMockitoObj(ordered.verify(radioController), wi.getPickInstruction());
		verifyDisplayOfMockitoObj(ordered.verify(radioController), "COMPLETE");
		 */
	}

	@Test
	public void showsNoMoreWorkWhenNoWIs() {
		int chePosition = 1;

		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + "ANY");

		cheDeviceLogic.scanCommandReceived("P%" + chePosition);

		cheDeviceLogic.scanCommandReceived("X%START");

		//Empty map for workinstructionMap. Since totalWiCount is 0. LOCATION_REVIEW should not happen
		Map<String, WorkInstructionCount> containerToWorkInstructionMap = new HashMap<String, WorkInstructionCount>();
		cheDeviceLogic.processWorkInstructionCounts(0, containerToWorkInstructionMap);

	}

	@Test
	public void whenCheDisconnectedStateRemainsTheSameOnScan() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason

		cheDeviceLogic.disconnectedFromServer();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		ArgumentCaptor<CommandControlDisplayMessage> commandCaptor = forClass(CommandControlDisplayMessage.class);
		verify(radioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), eq(true));

		CommandControlDisplayMessage noConnectionMessage = commandCaptor.getValue();
		Assert.assertEquals("Last invocation was: " + noConnectionMessage,
			"Please Wait...",
			noConnectionMessage.getLine3MessageStr());

	}

	@Test
	public void whenCheReconnectsStateReturns() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason

		cheDeviceLogic.disconnectedFromServer();

		//even though badge is scanned, should not change to show SCAN CONTAINER
		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.connectedToServer();

		//now that badge is scanned should be directed to scan container
		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		ArgumentCaptor<CommandControlDisplayMessage> commandCaptor = forClass(CommandControlDisplayMessage.class);
		verify(radioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), eq(true));
		List<CommandControlDisplayMessage> reverseOrder = Lists.reverse(commandCaptor.getAllValues());

		//In reverse order
		CommandControlDisplayMessage scanContainer = reverseOrder.remove(0);
		Assert.assertEquals("Last message was: " + scanContainer, "VERIFYING BADGE", scanContainer.getLine1MessageStr());

		//skip position controller clear
		reverseOrder.remove(0);

		//Should be a no network
		CommandControlDisplayMessage scanBadge = reverseOrder.remove(0);
		Assert.assertEquals("Second to last message was: " + scanBadge, "SCAN BADGE", scanBadge.getLine1MessageStr());

		//Should be a no network
		// ClassCastException here, probably because Setup_summary has different kind of command
		/*
		CommandControlDisplayMessage noNetwork = reverseOrder.remove(0);
		Assert.assertEquals("Second to last message was: " + noNetwork, "Unavailable", noNetwork.getLine2MessageStr());
		*/

	}

	/**
	 * Purpose of test is to see what happens during reassociate after CHE reset if CHE was in an odd state.
	 */
	@Test
	public void reconnectFromOddStates() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("1a: from idle, disconnect  from server and reconnect back to idle");
		// "Disconnect from server, reconnect, means CHE and site controller are fine, but site controller lost server connection.
		// Not so relevant to this test
		cheDeviceLogic.disconnectedFromServer();
		cheDeviceLogic.connectedToServer();
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("1b: from idle, reconnect to site controller: back to idle");
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.USER_RESTART);
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("2: from container select, reconnect back to same. Just a generic case of going back to existing state");
		cheDeviceLogic.testOnlySetState(CheStateEnum.CONTAINER_SELECT);
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.WATCHDOG_TIMEOUT);
		Assert.assertEquals(CheStateEnum.CONTAINER_SELECT, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("2b: from verify badge, manual reset does change state");
		cheDeviceLogic.testOnlySetState(CheStateEnum.VERIFYING_BADGE);
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.USER_RESTART);
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("3: from compute work or get work, reconnect back to setup_summary on manual reset.");
		cheDeviceLogic.testOnlySetState(CheStateEnum.COMPUTE_WORK);
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.USER_RESTART);
		Assert.assertEquals(CheStateEnum.SETUP_SUMMARY, cheDeviceLogic.getCheStateEnum());
		cheDeviceLogic.testOnlySetState(CheStateEnum.GET_WORK);
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.USER_RESTART);
		Assert.assertEquals(CheStateEnum.SETUP_SUMMARY, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("3b: non-manual reset does not");
		cheDeviceLogic.testOnlySetState(CheStateEnum.COMPUTE_WORK);
		cheDeviceLogic.startDevice(DeviceRestartCauseEnum.SMAC_ERROR); // saw a few of these at PFSWeb
		Assert.assertEquals(CheStateEnum.SETUP_SUMMARY, cheDeviceLogic.getCheStateEnum());
	}

	/**
	 * A very weak test, but did replicate bug DEV-1257
	 */
	@Test
	public void lateServerResponseTest() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xABC"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice(null); // not specifying restart reason
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("1a: from idle, see that compute work response does not go directly to setup_summary. (Expect late... error)");
		cheDeviceLogic.testOnlySetState(CheStateEnum.IDLE);
		cheDeviceLogic.processWorkInstructionCounts(0, null);
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("1b: Same, but with count = 1, but still null map. (Expect late... error, but no error on the null map.)"); // should this be silent?
		cheDeviceLogic.processWorkInstructionCounts(1, null);
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("1c: compute work response from verifying badge");
		cheDeviceLogic.testOnlySetState(CheStateEnum.VERIFYING_BADGE);
		cheDeviceLogic.processWorkInstructionCounts(0, null);
		Assert.assertEquals(CheStateEnum.VERIFYING_BADGE, cheDeviceLogic.getCheStateEnum()); // CHE reset will force to idle

		LOGGER.info("1d: compute work response from do pick");
		cheDeviceLogic.testOnlySetState(CheStateEnum.DO_PICK);
		cheDeviceLogic.processWorkInstructionCounts(0, null);
		Assert.assertEquals(CheStateEnum.DO_PICK, cheDeviceLogic.getCheStateEnum()); // CHE reset will force to idle

		LOGGER.info("2a: from idle, see that getWorkInstructions response does not go directly to setup_summary. (Expect late... error)");
		cheDeviceLogic.testOnlySetState(CheStateEnum.IDLE);
		cheDeviceLogic.assignWork(null, null);
		Assert.assertEquals(CheStateEnum.IDLE, cheDeviceLogic.getCheStateEnum());
		
		LOGGER.info("2b: getWorkInstructions response from verifying badge");
		cheDeviceLogic.testOnlySetState(CheStateEnum.VERIFYING_BADGE);
		cheDeviceLogic.assignWork(null, null);
		Assert.assertEquals(CheStateEnum.VERIFYING_BADGE, cheDeviceLogic.getCheStateEnum());

		LOGGER.info("2c: cgetWorkInstructions response from do pick");
		cheDeviceLogic.testOnlySetState(CheStateEnum.DO_PICK);
		// DEV-1331 late compute work response throws back to setup summary
		cheDeviceLogic.assignWork(null, null);
		Assert.assertEquals(CheStateEnum.SETUP_SUMMARY, cheDeviceLogic.getCheStateEnum());


	}
}
