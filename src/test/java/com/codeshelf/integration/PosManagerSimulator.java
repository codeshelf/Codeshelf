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
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosManagerSimulator.class);

	public PosManagerSimulator(IntegrationTest test, NetGuid controllerGuid) {
		this.test = test;
		INetworkDevice deviceLogic = test.getDeviceManager().getDeviceByGuid(controllerGuid);
		if (deviceLogic instanceof PosManagerDeviceLogic) {
			controllerLogic = (PosManagerDeviceLogic)deviceLogic;
		}
	}
}
