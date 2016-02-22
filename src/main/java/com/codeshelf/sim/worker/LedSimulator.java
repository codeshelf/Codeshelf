package com.codeshelf.sim.worker;

import lombok.Getter;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.AisleDeviceLogic;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;

public class LedSimulator {

	@Getter
	AisleDeviceLogic				aisleDeviceLogic;

	private static final Logger	LOGGER				= LoggerFactory.getLogger(LedSimulator.class);

	public LedSimulator(CsDeviceManager deviceManager, String cheGuid) {
		this(deviceManager, new NetGuid(cheGuid));
	}

	public LedSimulator(CsDeviceManager deviceManager, NetGuid cheGuid) {
		// verify that che is in site controller's device list
		aisleDeviceLogic = (AisleDeviceLogic) deviceManager.getDeviceByGuid(cheGuid);
		if (aisleDeviceLogic == null) {
			LOGGER.error("LedSimulator");
			throw new IllegalArgumentException("No LEDCON found with guid: " + cheGuid);
		}
	}

	public void assertNoLedColor(int inPosition) {
		Assert.assertFalse(aisleDeviceLogic.positionHasAnyColor(inPosition));
	}

	public void assertLedColor(int inPosition, ColorEnum inColor) {
		Assert.assertTrue(aisleDeviceLogic.positionHasColor(inPosition, inColor));
	}

}
