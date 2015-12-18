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
			// Not unhealthy. Just the answer to IsProduction.
			// But send our flag that means it is ok. This prevents the logs spamming test and stage every two minutes.
			return unhealthy(CodeshelfHealthCheck.OK);
		}
	}

}
