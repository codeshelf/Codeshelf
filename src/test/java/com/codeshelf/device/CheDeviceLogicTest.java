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
		cheDeviceLogic.startDevice();

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
		cheDeviceLogic.startDevice();

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
		cheDeviceLogic.startDevice();

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
		cheDeviceLogic.startDevice();

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
		CommandControlDisplayMessage noNetwork = reverseOrder.remove(0);
		Assert.assertEquals("Second to last message was: " + noNetwork, "Unavailable", noNetwork.getLine2MessageStr());

	}

}
