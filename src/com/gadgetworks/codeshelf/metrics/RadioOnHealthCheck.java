package com.gadgetworks.codeshelf.metrics;

import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.flyweight.controller.IRadioController;

public class RadioOnHealthCheck extends CodeshelfHealthCheck {
	
	ICsDeviceManager theDeviceManager;
	
	public RadioOnHealthCheck(ICsDeviceManager deviceManager) {
		super("Radio On");
		this.theDeviceManager = deviceManager;
	}
	
	@Override
	protected Result check() throws Exception {
		IRadioController radioController = theDeviceManager.getRadioController();
		if(!radioController.isRunning()) {
			return Result.unhealthy("Radio is not enabled yet");
		}//else
		if(!radioController.getGatewayInterface().isStarted()) {
			return Result.healthy("Radio is not detected");
		}
		return Result.healthy("Radio working");
	}

}
