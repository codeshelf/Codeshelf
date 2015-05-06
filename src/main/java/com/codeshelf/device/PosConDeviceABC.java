package com.codeshelf.device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlClearPosController;
import com.codeshelf.flyweight.command.CommandControlSetPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.util.ThreadUtils;

public abstract class PosConDeviceABC extends DeviceLogicABC {
	private static final Logger				LOGGER	= LoggerFactory.getLogger(PosConDeviceABC.class);

	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	public PosConDeviceABC(UUID inPersistentId, NetGuid inGuid, CsDeviceManager inDeviceManager, IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}

	protected String getConciseInstructionsSummary() {
		return null;
	}
	
	private void logOnePosconBatch(final List<PosControllerInstr> inInstructions){
		// Much better than LOGGER.info("{}: Sending PosCon Instructions {}", this.getMyGuidStr(), inInstructions);
	
		String header = String.format("%s: Sending PosCon Instructions", this.getMyGuidStr());
		final int logGroupSize = 3;
		int intructionCount = 0;
		int totalCount = inInstructions.size();
		String toLogStr = "";
		for (PosControllerInstr instr : inInstructions) {
			intructionCount++;
			if (intructionCount == 1) {
				toLogStr = String.format("%s %s", header, instr.conciseDescription());
			} else {
				toLogStr = String.format("%s %s", toLogStr, instr.conciseDescription());
			}
			if ((intructionCount == totalCount) || (intructionCount % logGroupSize == 0)) {
				LOGGER.info(toLogStr);
				toLogStr = "";
			}
		}		
	}

	protected void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {

		if (inInstructions.isEmpty()) {
			LOGGER.error("sendPositionControllerInstructions called for empty instructions");
			return;
		}

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
			// log these as we are really sending them out
			logOnePosconBatch(batch);
			ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, batch);
			mRadioController.sendCommand(command, getAddress(), true);
			batchStart += batchSize;
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
			}
		}
	}

	protected void clearAllPosconsOnThisDevice() {
		clearOnePosconOnThisDevice(PosControllerInstr.POSITION_ALL);
	}

	protected void clearOnePosconOnThisDevice(Byte inPosition) {
		if (inPosition == PosControllerInstr.POSITION_ALL)
			LOGGER.info("{}: Sending Clear PosCon command for ALL", this.getMyGuidStr());
		else
			LOGGER.info("{}: Sending Clear PosCon command for {}", this.getMyGuidStr(), inPosition);

		//Remove lastSent Set Instr from map to indicate the clear
		if (PosControllerInstr.POSITION_ALL.equals(inPosition)) {
			mPosToLastSetIntrMap.clear();
		} else {
			mPosToLastSetIntrMap.remove(inPosition);
		}

		ICommand command = new CommandControlClearPosController(NetEndpoint.PRIMARY_ENDPOINT, inPosition);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	public void simulateButtonPress(int inPosition, int inQuantity) {
		// Caller's responsibility to get the quantity correct. Normally match the planQuantity. Normally only lower after SHORT command.
		CommandControlButton buttonCommand = new CommandControlButton();
		buttonCommand.setPosNum((byte) inPosition);
		buttonCommand.setValue((byte) inQuantity);
		this.buttonCommandReceived(buttonCommand);
	}

	public Byte getLastSentPositionControllerDisplayValue(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getReqQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getReqQty();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerDisplayFreq(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getFreq();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getFreq();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerDisplayDutyCycle(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getDutyCycle();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getDutyCycle();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerMinQty(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getMinQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMinQty();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerMaxQty(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getMaxQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMaxQty();
		} else {
			return null;
		}
	}
	
	public Byte waitForControllerDisplayValue(byte position, Byte value, int timeoutInMillis){
	long start = System.currentTimeMillis();
	Byte currentValue = null;
	while (System.currentTimeMillis() - start < timeoutInMillis) {
		// retry every 100ms
		ThreadUtils.sleep(100);
		currentValue = getLastSentPositionControllerDisplayValue(position);
		// either can be null
		if (currentValue == value) {
			// expected state found - all good
			break;
		}
	}
	return currentValue;
	}


}
