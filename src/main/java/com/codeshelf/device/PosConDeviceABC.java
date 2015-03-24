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

public abstract class PosConDeviceABC extends DeviceLogicABC {
	private static final Logger				LOGGER	= LoggerFactory.getLogger(PosConDeviceABC.class);

	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	public PosConDeviceABC(UUID inPersistentId, NetGuid inGuid, CsDeviceManager inDeviceManager, IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}

	protected void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {
		LOGGER.info("Sending PosCon Instructions {}", inInstructions);
		if (inInstructions.isEmpty()) {return;}
		
		//Update the last sent posControllerInstr for the position 
		for (PosControllerInstr instr : inInstructions) {
			if (PosControllerInstr.POSITION_ALL.equals(instr.getPosition())) {
				//A POS_ALL instruction overrides all previous instructions
				mPosToLastSetIntrMap.clear();
			}
			mPosToLastSetIntrMap.put(instr.getPosition(), instr);
		}

		int batchStart = 0, size = inInstructions.size(), batchSize = 10;
		while (batchStart < size) {
			List<PosControllerInstr> batch = inInstructions.subList(batchStart, Math.min(batchStart + batchSize, size));
			ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, batch);
			mRadioController.sendCommand(command, getAddress(), true);
			batchStart += batchSize;
			try {Thread.sleep(5);} catch (InterruptedException e) {}
		}
	}

	protected void clearAllPositionControllers() {
		clearOnePositionController(PosControllerInstr.POSITION_ALL);
	}

	protected void clearOnePositionController(Byte inPosition) {
		if (inPosition == PosControllerInstr.POSITION_ALL)
			LOGGER.info("Sending Clear PosCon command for ALL");
		else
			LOGGER.info("Sending Clear PosCon command for {}", inPosition);

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
