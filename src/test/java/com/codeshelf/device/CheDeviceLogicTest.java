package com.codeshelf.device;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlClearPosController;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.CommandControlSetPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.ws.jetty.client.CsClientEndpoint;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Unit test with mocks for CheDeviceLogic
 *
 * @author pmonteiro
 *
 */
public class CheDeviceLogicTest extends MockDaoTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheDeviceLogicTest.class);

	@Test
	public void cheSetupAfterCompleteClearsPosCon() {
		int posconPosition = 1;
		String containerId = "CONTAINER9";
		int wiQuantity = 1;

		IRadioController radioController = mock(IRadioController.class);
		CsDeviceManager deviceManager = new CsDeviceManager(radioController, mock(CsClientEndpoint.class));
		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), deviceManager, radioController);
		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + containerId);

		cheDeviceLogic.scanCommandReceived("P%" + posconPosition);

		cheDeviceLogic.scanCommandReceived("X%START");
		WorkInstructionCount wiCount = new WorkInstructionCount(1, 0, 0, 0,0);
		cheDeviceLogic.processWorkInstructionCounts(1, ImmutableMap.<String, WorkInstructionCount>of(containerId, wiCount));
		cheDeviceLogic.scanCommandReceived("X%START");

		WorkInstruction wi = mock(WorkInstruction.class, Mockito.CALLS_REAL_METHODS);
		wi.setType(WorkInstructionTypeEnum.PLAN);
		wi.setStatus(WorkInstructionStatusEnum.NEW);
		wi.setPickInstruction("fakePickInstruction");
		wi.setLedCmdStream("[]");
		wi.setPlanQuantity(wiQuantity);
		wi.setPlanMinQuantity(wiQuantity);
		wi.setPlanMaxQuantity(wiQuantity);

		Mockito.when(wi.getContainerId()).thenReturn(containerId);
		Mockito.when(wi.getItemId()).thenReturn("fakeItemId");

		cheDeviceLogic.assignWork(ImmutableList.<WorkInstruction>of(wi), "ASSIGN WORK MESSAGE?");
		Assert.assertEquals(CheStateEnum.DO_PICK, cheDeviceLogic.waitForCheState(CheStateEnum.DO_PICK, 5000));
		pressButton(cheDeviceLogic, posconPosition, wiQuantity);
		Assert.assertEquals(CheStateEnum.PICK_COMPLETE, cheDeviceLogic.waitForCheState(CheStateEnum.PICK_COMPLETE, 5000));

		cheDeviceLogic.scanCommandReceived("X%SETUP");

		LinkedList<ICommand> commands =  posconCommands(radioController, 1);

		LOGGER.info(commands.toString());

		Collections.reverse(commands);
		for (ICommand command : commands) { //find last poscon related message
			if (command instanceof CommandControlClearPosController
				|| command instanceof CommandControlSetPosController) {
				Assert.assertTrue("Last command: " + command,  command instanceof CommandControlClearPosController);
				break;

			}
		}


	}

	private LinkedList<ICommand> posconCommands(IRadioController radioController, int posconPosition) {
		ArgumentCaptor<ICommand> commands = ArgumentCaptor.forClass(ICommand.class);
		verify(radioController, Mockito.atLeastOnce()).sendCommand(commands.capture(), any(NetAddress.class), any(Boolean.class));
		return new LinkedList<ICommand>(commands.getAllValues());
	}

	@Test
	public void showsCompleteWorkAfterPicks() {
		this.getTenantPersistenceService().beginTransaction();

		int chePosition = 1;

		Facility facility = new FacilityGenerator(TenantManagerService.getInstance().getDefaultTenant()).generateValid();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		WorkInstruction wi = new WorkInstructionGenerator().generateWithNewStatus(facility);
		List<WorkInstruction> wiToDo = ImmutableList.of(wi);
		this.getTenantPersistenceService().commitTransaction(); // end of transaction for this test

		IRadioController radioController = mock(IRadioController.class);

		// CheDeviceLogic is now an abstract base class
		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"),
			mock(CsDeviceManager.class), radioController);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + wi.getContainerId());

		cheDeviceLogic.scanCommandReceived("P%" + chePosition);

		cheDeviceLogic.scanCommandReceived("X%START");

		//This test only generates valid orders (no shorts etc). LOCATION_REVIEW_SELECT should never be entered.
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

	@SuppressWarnings("unused")
	@Test
	public void showsNoWorkIfNothingAheadOfLocation() {
		this.getTenantPersistenceService().beginTransaction();

		int chePosition = 1;

		Facility facility = new FacilityGenerator(TenantManagerService.getInstance().getDefaultTenant()).generateValid();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		WorkInstruction wi = new WorkInstructionGenerator().generateWithNewStatus(facility);
		this.getTenantPersistenceService().commitTransaction();

		List<WorkInstruction> wiToDo = ImmutableList.of(wi);

		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + wi.getContainerId());

		cheDeviceLogic.scanCommandReceived("P%" + chePosition);

		cheDeviceLogic.scanCommandReceived("X%START");

		//This test only generates valid orders (no shorts etc). LOCATION_REVIEW_SELECT should never be entered.
		//We will pass in a map containing good counts with no bad counts.
		Map<String, WorkInstructionCount> containerToWorkInstructionMap = new HashMap<String, WorkInstructionCount>();
		containerToWorkInstructionMap.put(wi.getContainerId(), new WorkInstructionCount(wi.getPlanQuantity(), 0, 0, 0, 0));

		cheDeviceLogic.processWorkInstructionCounts(0, containerToWorkInstructionMap);

		cheDeviceLogic.scanCommandReceived("L%ANYLOCATIONAFTERPICK");

		//Pretend no work ahead of this location
		cheDeviceLogic.assignWork(Collections.<WorkInstruction>emptyList(), null);


		pressButton(cheDeviceLogic, chePosition, wi.getPlanQuantity());

		verifyDisplay(radioController, "NO WORK TO DO");

	}

	@Test
	public void showsNoMoreWorkWhenNoWIs() {
		int chePosition = 1;

		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		cheDeviceLogic.scanCommandReceived("C%" + "ANY");

		cheDeviceLogic.scanCommandReceived("P%" + chePosition);

		cheDeviceLogic.scanCommandReceived("X%START");

		//Empty map for workinstructionMap. Since totalWiCount is 0. LOCATION_REVIEW should not happen
		Map<String, WorkInstructionCount> containerToWorkInstructionMap = new HashMap<String, WorkInstructionCount>();
		cheDeviceLogic.processWorkInstructionCounts(0, containerToWorkInstructionMap);

		verifyDisplay(radioController, "NO WORK TO DO");

	}


	@Test
	public void whenCheDisconnectedStateRemainsTheSameOnScan() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);

		cheDeviceLogic.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		cheDeviceLogic.startDevice();

		cheDeviceLogic.disconnectedFromServer();

		cheDeviceLogic.scanCommandReceived("U%PICKER1");

		ArgumentCaptor<CommandControlDisplayMessage> commandCaptor = forClass(CommandControlDisplayMessage.class);
		verify(radioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), eq(true));

		CommandControlDisplayMessage noConnectionMessage = commandCaptor.getValue();
		Assert.assertEquals("Last invocation was: " + noConnectionMessage, "Please Wait...", noConnectionMessage.getLine3MessageStr());

	}

	@Test
	public void whenCheReconnectsStateReturns() {
		IRadioController radioController = mock(IRadioController.class);

		SetupOrdersDeviceLogic cheDeviceLogic = new SetupOrdersDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);

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
		Assert.assertEquals("Last message was: " + scanContainer, "SCAN ORDER", scanContainer.getLine1MessageStr());

		//skip position controller clear
		reverseOrder.remove(0);

		//Should be a no network
		CommandControlDisplayMessage scanBadge = reverseOrder.remove(0);
		Assert.assertEquals("Second to last message was: " + scanBadge, "SCAN BADGE", scanBadge.getLine1MessageStr());

		//Should be a no network
		CommandControlDisplayMessage noNetwork = reverseOrder.remove(0);
		Assert.assertEquals("Second to last message was: " + noNetwork, "Unavailable", noNetwork.getLine2MessageStr());


	}


	/**
	 * Simulate button press for the given position and quantity value
	 */
	private void pressButton(CheDeviceLogic cheDeviceLogic, int pos, int value) {
		cheDeviceLogic.buttonCommandReceived(new CommandControlButton(mock(NetEndpoint.class), (byte)pos, (byte)value));
	}

	/**
	 * Determine if the radio controller displayed a message with the given message
	 */
	private void verifyDisplay(IRadioController radioController, String message) {
		verifyDisplayOfMockitoObj(verify(radioController, atLeast(1)), message);

	}

	/**
	 * Determine if the radio controller object (wrapped from Mockito.verify() has
	 * sent a display command with the given message
	 */
	private void verifyDisplayOfMockitoObj(IRadioController verifyRadioController, String message) {
		verifyRadioController.sendCommand(displayContains(message), any(NetAddress.class), eq(true));

	}

	/**
	 * Convenience method to return the matcher to long for the given message on any line in the display command
	 */
	private CommandControlDisplayMessage displayContains(String message) {
		return argThat(new DisplayCommandContains(message));
	}

	/**
	 * Custom matcher to see if the given message appears in any line of the display command
	 *
	 */
	private static class DisplayCommandContains extends ArgumentMatcher<CommandControlDisplayMessage> {
		private String message;

		public DisplayCommandContains(String message) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(message));
			this.message = message;
		}

		@Override
		public boolean matches(Object iCommand) {
			if (iCommand instanceof CommandControlDisplayMessage) {
				CommandControlDisplayMessage command = (CommandControlDisplayMessage) iCommand;
				return command.getLine1MessageStr().contains(this.message)
				|| command.getLine2MessageStr().contains(this.message)
				|| command.getLine3MessageStr().contains(this.message)
				|| command.getLine4MessageStr().contains(this.message);
			} else {
				return false;
			}
		}

		@Override
		public void describeTo(Description description) {
			super.describeTo(description);
			description.appendText(" \"" + this.message + "\"");
		}



	}
}
