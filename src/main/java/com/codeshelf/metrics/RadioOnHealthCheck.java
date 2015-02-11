package com.codeshelf.metrics;

import com.codeshelf.device.ICsDeviceManager;
import com.codeshelf.flyweight.controller.IRadioController;

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
