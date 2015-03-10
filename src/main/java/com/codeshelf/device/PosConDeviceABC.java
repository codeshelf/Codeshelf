package com.codeshelf.device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlClearPosController;
import com.codeshelf.flyweight.command.CommandControlSetPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;

public abstract class PosConDeviceABC extends DeviceLogicABC{
	private static final Logger				LOGGER									= LoggerFactory.getLogger(PosConDeviceABC.class);

	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	public PosConDeviceABC(UUID inPersistentId, NetGuid inGuid, CsDeviceManager inDeviceManager, IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);		
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}
	
	protected void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {
		LOGGER.info("Sending PosCon Instructions {}", inInstructions);
		//Update the last sent posControllerInstr for the position 
		for (PosControllerInstr instr : inInstructions) {
			if (PosControllerInstr.POSITION_ALL.equals(instr.getPosition())) {
				//A POS_ALL instruction overrides all previous instructions
				mPosToLastSetIntrMap.clear();
			}
			mPosToLastSetIntrMap.put(instr.getPosition(), instr);
		}

		ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, inInstructions);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	protected void clearAllPositionControllers() {
		clearOnePositionController(PosControllerInstr.POSITION_ALL);
	}
	
	protected void clearOnePositionController(Byte inPosition) {
		LOGGER.info("Sending Clear PosCon Instruction {}", inPosition);

		//Remove lastSent Set Instr from map to indicate the clear
		if (PosControllerInstr.POSITION_ALL.equals(inPosition)) {
			mPosToLastSetIntrMap.clear();
		} else {
			mPosToLastSetIntrMap.remove(inPosition);
		}
		
		ICommand command = new CommandControlClearPosController(NetEndpoint.PRIMARY_ENDPOINT, inPosition);
		mRadioController.sendCommand(command, getAddress(), true);
	}
}
