package com.codeshelf.metrics;

import com.codeshelf.device.AisleDeviceLogic;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CsDeviceManager;

public class AssociatedRadioHealthCheck extends CodeshelfHealthCheck {

	CsDeviceManager	mDeviceManager;

	public AssociatedRadioHealthCheck(CsDeviceManager deviceManager) {
		super("Associated Radios");
		this.mDeviceManager = deviceManager;
	}

	@Override
	protected Result check() throws Exception {
		
		Integer cheAssoc = 0;
		Integer cheTotal = 0;
		for (CheDeviceLogic device : mDeviceManager.getCheControllers()){
			cheTotal++;
			if (device.isDeviceAssociated())
				cheAssoc++;
		}
		Integer aisleAssoc = 0;
		Integer aisleTotal = 0;
		for (AisleDeviceLogic device : mDeviceManager.getAisleControllers()){
			aisleTotal++;
			if (device.isDeviceAssociated())
				aisleAssoc++;
		}
		// There is no way to know what is really healthy. CHE may have batteries out. Aisle controllers may be unplugged.
		String resultString = cheAssoc + " of " + cheTotal + " CHE associated. "+ aisleAssoc + " of " + aisleTotal + " aisle controllers associated.";
		return Result.healthy(resultString);
	}
}
