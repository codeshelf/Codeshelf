package com.codeshelf.metrics;

import com.codeshelf.device.CsDeviceManager;

public class IsProductionSiteControllerHealthCheck extends CodeshelfHealthCheck {


	CsDeviceManager	mDeviceManager;

	public IsProductionSiteControllerHealthCheck(CsDeviceManager deviceManager) {
		super("IsProduction");
		this.mDeviceManager = deviceManager;
	}

	@Override
	protected Result check() throws Exception {


		if (mDeviceManager.getProductionValue()) {
			return Result.healthy("Connected to production facility.");
		} else {
			// Not unhealthy. Just the answer to IsProduction
			return Result.unhealthy("Not connected to production facility.");
		}
	}

}
