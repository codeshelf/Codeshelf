package com.codeshelf.sim.worker.api;

import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.sim.worker.PickSimulator;

public class CheResource {

	private PickSimulator pickSim;

	public CheResource(CsDeviceManager deviceManager, NetGuid cheGuid) {
		this.pickSim = new PickSimulator(deviceManager, cheGuid);
	}
	
}
