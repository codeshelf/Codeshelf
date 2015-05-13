package com.codeshelf.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.PosControllerInstr.PosConInstrGroupSerializer;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;

public class PosManagerDeviceLogic extends PosConDeviceABC {
	private static final Logger							LOGGER						= LoggerFactory.getLogger(PosManagerDeviceLogic.class);

	// Instructions can come from other site controller (getGuid()) or from other devices (CHEs)
	private Map<NetGuid, Map<Byte, PosControllerInstr>>	mPosInstructionBySource		= new HashMap<NetGuid, Map<Byte, PosControllerInstr>>();

	// Order feedback displays by slot. The source GUID is irrelevant, although it might be nice to know the orderID.
	private Map<String, PosControllerInstr>				mOrderFeedbackByLocation	= new HashMap<String, PosControllerInstr>();

	public PosManagerDeviceLogic(UUID inPersistentId,
		NetGuid inGuid,
		CsDeviceManager inDeviceManager,
		IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}

	public void connectedToServer() {
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, "PosCon Controller", "Connected", "", "");
		mRadioController.sendCommand(command, getAddress(), true);
	}

	public void disconnectedFromServer() {
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,
			"PosCon Controller",
			"Disconnected",
			"",
			"");
		mRadioController.sendCommand(command, getAddress(), true);
	}

	@Override
	public short getSleepSeconds() {
		return 0;
	}

	@Override
	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_POS_CON_CTRL;
	}

	@Override
	public void startDevice() {
		LOGGER.info("Start PosCon controller(after association " + getMyGuidStr());
		updatePosCons();
	}

	public final void removeAllPosConInstrsAndSend() {
		mPosInstructionBySource.clear();
		clearAllPosconsOnThisDevice();
	}

	/**
	 * Remove all instructions for a specific PosCon
	 */
	public final void removePosConInstrs(Byte position) {
		List<NetGuid> emptySources = new ArrayList<NetGuid>();
		Iterator<NetGuid> sources = mPosInstructionBySource.keySet().iterator();
		while (sources.hasNext()) {
			NetGuid source = sources.next();
			Map<Byte, PosControllerInstr> instructionsFromSource = mPosInstructionBySource.get(source);
			instructionsFromSource.remove(position);
			if (instructionsFromSource.isEmpty()) {
				emptySources.add(source);
			}
		}

		//Clean up instructions list by removing all sources with no instrcutions remaining
		for (NetGuid source : emptySources) {
			mPosInstructionBySource.remove(source);
		}
	}

	/**
	 * Remove all PosCon instructions from a single source device
	 * Update PosCon displays
	 */
	public final void removePosConInstrsForSource(final NetGuid inNetGuid) {
		String sourceStr = inNetGuid.getHexStringNoPrefix();
		String toLog = String.format("Clear PosCons for Source:%s on %s", sourceStr, getMyGuidStr());
		notifyPoscons(toLog);
		mPosInstructionBySource.remove(inNetGuid);
	}

	public final void removePosConInstrsForSourceAndPosition(NetGuid inNetGuid, Byte position) {
		List<Byte> positions = new ArrayList<>();
		positions.add(position);
		removePosConInstrsForSourceAndPositions(inNetGuid, positions);
	}

	/**
	 * Remove PosCon instruction for a specific source device / position combination
	 * Update PosCon displays
	 */
	public final void removePosConInstrsForSourceAndPositions(NetGuid inNetGuid, List<Byte> positions) {
		String sourceStr = inNetGuid.getHexStringNoPrefix();

		Map<Byte, PosControllerInstr> sourceInsts = mPosInstructionBySource.get(inNetGuid);
		//The above value might be null if  another process has already removed all instructions for the current controller from the provided source
		if (sourceInsts != null) {
			for (Byte position : positions) {
				String toLog = String.format("Clear PosCon %s for Source:%s on %s", position.toString(), sourceStr, getMyGuidStr());
				notifyPoscons(toLog);
				sourceInsts.remove(position);
			}
			if (sourceInsts.isEmpty()) {
				mPosInstructionBySource.remove(inNetGuid);
			}
		}
	}

	//LedSample isn't used yet.
	public final void addPosConInstrFor(NetGuid inNetGuid,
		LedSample inLedSample,
		byte position,
		byte quantity,
		byte min,
		byte max,
		byte blink,
		byte brightness) {
		PosControllerInstr cmd = new PosControllerInstr(position, quantity, min, max, blink, brightness);
		addPosConInstrFor(inNetGuid, cmd);
	}

	public final void addPosConInstrFor(NetGuid inNetGuid, PosControllerInstr instruction) {
		Map<Byte, PosControllerInstr> posConInstrs = mPosInstructionBySource.get(inNetGuid);
		if (posConInstrs == null) {
			posConInstrs = new HashMap<Byte, PosControllerInstr>();
			mPosInstructionBySource.put(inNetGuid, posConInstrs);
		}
		instruction.setPostedToPosConController(System.currentTimeMillis());
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
		}
		posConInstrs.put(instruction.getPosition(), instruction);
	}

	public final PosControllerInstr getPosConInstrFor(NetGuid inNetGuid, byte inPosition) {
		Map<Byte, PosControllerInstr> posConInstrsForSource = mPosInstructionBySource.get(inNetGuid);
		if (posConInstrsForSource != null) {
			return posConInstrsForSource.get(inPosition);
		}
		return null;
	}

	/**
	 * Clear the data structure
	 */
	private void removeExtraPosConsFromMap() {
		NetGuid thisPosConControllerGuid = getGuid();
		mPosInstructionBySource.remove(thisPosConControllerGuid);
	}

	public final void lightExtraPosCons(String inInstructions) {
		removeExtraPosConsFromMap();
		List<PosControllerInstr> posConInstrs = PosConInstrGroupSerializer.deserializePosConInstrString(inInstructions);
		for (PosControllerInstr cmd : posConInstrs) {
			addPosConInstrFor(getGuid(), cmd);
		}
	}

	@Override
	public void scanCommandReceived(String inCommandStr) {
		// Also empty in AisleDeviceLogic
		LOGGER.error("Inappropriate call to scanCommandReceived in PosManagerDeviceLogic");

	}

	private String getSourceControllerIdForPosconPosition(Byte whichPoscon) {
		// not so sure the data structures are ideal. For now, just iterate to find it.

		for (Map<Byte, PosControllerInstr> x : mPosInstructionBySource.values()) {
			PosControllerInstr y = x.get(whichPoscon);
			if (y != null)
				return y.getSourceId();
		}

		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 * We want to pass this button press back to the CHE Device logic that activated this poscon button 
	 */
	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {
		Byte whichPoscon = inButtonCommand.getPosNum();
		String controllerId = getSourceControllerIdForPosconPosition(whichPoscon);

		CheDeviceLogic displaySource = mDeviceManager.getCheDeviceByControllerId(controllerId);
		if (displaySource != null) {
			displaySource.processOffCheButtonPress(getMyGuidStr(), inButtonCommand);
		} else {
			LOGGER.error("Button from {}, but unknown CHE device logic", controllerId);
		}
	}

	public final void updatePosCons() {
		updatePosCons(false);
	}

	public final void updatePosCons(boolean updateUnassociated) {
		// This likely needs to be made much more efficient.
		clearAllPosconsOnThisDevice();

		// Add in all active work instructions. Modeled from mPosInstructionBySource
		Map<Byte, PosControllerInstr> latestInstructionsForPosition = new HashMap<Byte, PosControllerInstr>();
		List<Map<Byte, PosControllerInstr>> instructionsBySourceList = new ArrayList<Map<Byte, PosControllerInstr>>(mPosInstructionBySource.values());

		// Find the latest instruction posted to each PosCon
		for (Map<Byte, PosControllerInstr> instructionsFromSingleSource : instructionsBySourceList) {
			List<PosControllerInstr> instructionList = new ArrayList<PosControllerInstr>(instructionsFromSingleSource.values());
			for (PosControllerInstr instruction : instructionList) {
				Byte position = instruction.getPosition();
				PosControllerInstr latestInstructionForPosition = latestInstructionsForPosition.get(position);
				if (latestInstructionForPosition == null
						|| latestInstructionForPosition.getPostedToPosConController() < instruction.getPostedToPosConController()) {
					latestInstructionsForPosition.put(position, instruction);
				}
			}
		}
		// Add in feedback instructions on poscons that did not get an active instruction
		for (PosControllerInstr instruction : mOrderFeedbackByLocation.values()) {
			Byte posconIndex = instruction.getPosition();
			if (!latestInstructionsForPosition.containsKey(posconIndex)) {
				latestInstructionsForPosition.put(posconIndex, instruction);
			}

		}

		//Display latest instructions on PosCons
		List<PosControllerInstr> latestCommandsList = new ArrayList<PosControllerInstr>(latestInstructionsForPosition.values());

		// make sure we wait the 5 millis after the clear sent above.
		if (latestCommandsList.size() > 0) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
			}
			sendPositionControllerInstructions(latestCommandsList);
		}
	}

	private void addFeedbackInstruction(final String locationName, final PosControllerInstr feedbackInstruction) {
		removeFeedbackInstructionFromLocation(locationName); // silently ok if nothing there
		mOrderFeedbackByLocation.put(locationName, feedbackInstruction);
	}

	private void removeFeedbackInstructionFromLocation(final String locationName) {
		if (mOrderFeedbackByLocation.containsKey(locationName)) {
			mOrderFeedbackByLocation.remove(locationName);
		}
	}

	/**
	 * From the meaningful OrderLocationFeedbackMessage, compute and store PosControllerInstr
	 * Although returned null could be from an error, null also means no feedback for that location
	 */
	public void processFeedback(OrderLocationFeedbackMessage instruction) {
		boolean lastOfGroup = instruction.getLastMsgOfGroup();
		PosControllerInstr posInstruction = constructMsgFromFeedback(instruction);
		String locationName = instruction.getLocationName();
		if (posInstruction != null) {
			addFeedbackInstruction(locationName, posInstruction);
		} else {
			removeFeedbackInstructionFromLocation(locationName);
		}
		if (lastOfGroup)
			updatePosCons();
	}

	/**
	 * Return null if the location should show nothing.
	 * Otherwise, return the display message.
	 */
	private PosControllerInstr constructMsgFromFeedback(OrderLocationFeedbackMessage instruction) {
		Byte position = instruction.getPosition();
		if (!instruction.getHasAnyOrderAtAll())
			return null;
		// choices are oc and --
		Byte thirdByte = PosControllerInstr.BITENCODED_LED_DASH;
		Byte fourthByte = PosControllerInstr.BITENCODED_LED_DASH;
		if (instruction.getOrderFullyComplete()) {
			thirdByte = PosControllerInstr.BITENCODED_LED_C;
			fourthByte = PosControllerInstr.BITENCODED_LED_O;
		}

		return (new PosControllerInstr(position,
			PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			thirdByte,
			fourthByte,
			PosControllerInstr.SOLID_FREQ.byteValue(),
			PosControllerInstr.DIM_DUTYCYCLE.byteValue()));
	};

}
