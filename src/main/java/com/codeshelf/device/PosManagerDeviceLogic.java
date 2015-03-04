package com.codeshelf.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.PosConInstrGroupSerializer.PosConCmdGroup;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;

public class PosManagerDeviceLogic extends PosConDeviceABC{
	private static final Logger	LOGGER							= LoggerFactory.getLogger(PosManagerDeviceLogic.class);
	
	private Map<NetGuid, Map<Byte, PosControllerInstr>>	mDevicePosConCmdMap	= new HashMap<NetGuid, Map<Byte, PosControllerInstr>>();
	
	public PosManagerDeviceLogic(UUID inPersistentId,
		NetGuid inGuid,
		CsDeviceManager inDeviceManager,
		IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}
	
	public void connectedToServer(){
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,"PosCon Controller","Connected","","");
		mRadioController.sendCommand(command, getAddress(), true);
	}
	
	public void disconnectedFromServer(){
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,"PosCon Controller","Disconnected","","");
		mRadioController.sendCommand(command, getAddress(), true);		
	}
	
	@Override
	public short getSleepSeconds() {
		return 0;
	}
	
	@Override
	public String getDeviceType(){
		return CsDeviceManager.DEVICETYPE_POS_CON_CTRL;
	}

	@Override
	public void startDevice() {
		LOGGER.info("Start PosCon controller(after association " + getMyGuidStr());
		updatePosCons();
	}

	public final void clearAllPosConCmdsAndSend() {
		mDevicePosConCmdMap.clear();
		clearAllPositionControllers();
	}

	public final void removePosConCmdsForCheAndSend(final NetGuid inNetGuid) {
		String cheGuidStr = inNetGuid.getHexStringNoPrefix();

		LOGGER.info("Clear LEDs for CHE:" + cheGuidStr + " on " + getMyGuidStr());

		mDevicePosConCmdMap.remove(inNetGuid);
		// Only send the command if the device is known active.
		if (isDeviceAssociated()) {
			updatePosCons();
		}
	}
	
	//LedSample isn't used yet.
	public final void addPosConCmdFor(NetGuid inNetGuid, LedSample inLedSample, byte position, byte quantity, byte min, byte max, byte blink, byte brightness) {
		PosControllerInstr cmd = new PosControllerInstr(position, quantity, min, max, blink, brightness);
		addPosConCmdFor(inNetGuid, cmd);
	}
	
	public final void addPosConCmdFor(NetGuid inNetGuid, PosControllerInstr cmd) {
		Map<Byte, PosControllerInstr> posConCmds = mDevicePosConCmdMap.get(inNetGuid);
		if (posConCmds == null) {
			posConCmds = new HashMap<Byte, PosControllerInstr>();
			mDevicePosConCmdMap.put(inNetGuid, posConCmds);
		}
		posConCmds.put(cmd.getPosition(), cmd);
	}
	
	
	public final PosControllerInstr getPosConCmdFor(NetGuid inNetGuid, byte inPosition) {
		Map<Byte, PosControllerInstr> posConCmds = mDevicePosConCmdMap.get(inNetGuid);
		if (posConCmds != null) {
			return posConCmds.get(inPosition);
		}
		return null;
	}
	
	/**
	 * Clear the data structure
	 */
	private void clearExtraPosConsFromMap() {
		NetGuid thisPosConControllerGuid = getGuid();
		mDevicePosConCmdMap.remove(thisPosConControllerGuid);
	}

	public final void lightExtraPosCons(int inSeconds, String inCommands) {
		clearExtraPosConsFromMap();

		List<PosConCmdGroup> posConCmdGroups = PosConInstrGroupSerializer.deserializePosConCmdString(inCommands);
		for (PosConCmdGroup cmd : posConCmdGroups) {
			NetGuid netGuid = new NetGuid(cmd.getControllerId());
			addPosConCmdFor(netGuid, null, cmd.getPosNum(), cmd.getQuantity(), cmd.getMin(), cmd.getMax(), cmd.getFrequency().toByte(), cmd.getBrightness().toByte());
		}
		//setLightsExpireTimer(inSeconds); 
		updatePosCons();
	}
	
	@Override
	public void scanCommandReceived(String inCommandStr) {
		// Also empty in AisleDeviceLogic
		
	}

	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {
		// Also empty in AisleDeviceLogic
		
	}
	
	public final void updatePosCons() {
		clearAllPositionControllers();
		for (Entry<NetGuid, Map<Byte, PosControllerInstr>> devices : mDevicePosConCmdMap.entrySet()) {
			Map<Byte, PosControllerInstr> devCommandsMap = devices.getValue();
			List<PosControllerInstr> devCommands = new ArrayList<PosControllerInstr>(devCommandsMap.values());
			sendPositionControllerInstructions(devCommands);
		}
	}
}
