package com.codeshelf.integration;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.PosManagerDeviceLogic;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.testframework.IntegrationTest;

public class PosManagerSimulator {
	@SuppressWarnings("unused")
	private IntegrationTest			test;

	@Getter
	private PosManagerDeviceLogic	controllerLogic;
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosManagerSimulator.class);

	public PosManagerSimulator(IntegrationTest test, NetGuid controllerGuid) {
		this.test = test;
		INetworkDevice deviceLogic = test.getDeviceManager().getDeviceByGuid(controllerGuid);
		if (deviceLogic instanceof PosManagerDeviceLogic) {
			controllerLogic = (PosManagerDeviceLogic)deviceLogic;
		}
		else
			LOGGER.error("Trying to get PosManagerSimulator for wrong kind of deviceLogic");
	}
	
	public void buttonPress(int inPosition, int inQuantity) {
		controllerLogic.simulateButtonPress(inPosition, inQuantity);
	}

	public Byte getLastSentPositionControllerDisplayValue(byte position) {
		return controllerLogic.getLastSentPositionControllerDisplayValue(position);
	}

	public Byte getLastSentPositionControllerDisplayFreq(byte position) {
		return controllerLogic.getLastSentPositionControllerDisplayFreq(position);
	}

	public Byte getLastSentPositionControllerDisplayDutyCycle(byte position) {
		return controllerLogic.getLastSentPositionControllerDisplayDutyCycle(position);
	}

	public Byte getLastSentPositionControllerMinQty(byte position) {
		return controllerLogic.getLastSentPositionControllerMinQty(position);
	}

	public Byte getLastSentPositionControllerMaxQty(byte position) {
		return controllerLogic.getLastSentPositionControllerMaxQty(position);
	}

}
