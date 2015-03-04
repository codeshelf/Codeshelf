package com.codeshelf.metrics;

import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.flyweight.controller.IRadioController;

public class RadioOnHealthCheck extends CodeshelfHealthCheck {
	
	CsDeviceManager theDeviceManager;
	
	public RadioOnHealthCheck(CsDeviceManager deviceManager) {
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
