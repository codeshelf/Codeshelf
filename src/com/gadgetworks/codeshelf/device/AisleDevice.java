/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDevice.java,v 1.5 2013/04/15 21:27:05 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

public class AisleDevice extends DeviceABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AisleDevice.class);

	private class LedCmd {
		@Getter
		private Short		mPosition;
		@Getter
		private ColorEnum	mColor;
		@Getter
		private String		mEffect;

		public LedCmd(final Short inPosition, final ColorEnum inColor, final String inEffect) {
			mPosition = inPosition;
			mColor = inColor;
			mEffect = inEffect;
		}
	}

	private Map<NetGuid, List<LedCmd>>	mDeviceLedPosMap	= new HashMap<NetGuid, List<LedCmd>>();

	public AisleDevice(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}

	@Override
	public final void start() {
		//		short position = 1;
		//		sendLightCommand(CommandControlLight.CHANNEL1, position, ColorEnum.BLUE, CommandControlLight.EFFECT_SOLID);
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified GUID.
	 * @param inNetGuid
	 */
	public final void clearLedCmdFor(final NetGuid inNetGuid) {
		mDeviceLedPosMap.remove(inNetGuid);
		updateLeds();
	}

	// --------------------------------------------------------------------------
	/**
	 * Add an LED command for the specified GUID.
	 * @param inNetGuid
	 * @param inPosition
	 * @param inColor
	 * @param inEffect
	 */
	public final void addLedCmdFor(final NetGuid inNetGuid, final Short inPosition, final ColorEnum inColor, final String inEffect) {
		List<LedCmd> ledCmds = mDeviceLedPosMap.get(inNetGuid);
		if (ledCmds == null) {
			ledCmds = new ArrayList<LedCmd>();
			mDeviceLedPosMap.put(inNetGuid, ledCmds);
		}
		LedCmd ledCmd = new LedCmd(inPosition, inColor, inEffect);
		ledCmds.add(ledCmd);
		updateLeds();
	}

	@Override
	public final void commandReceived(String inCommandStr) {
		// The aisle device never returns commands.
	}

	// --------------------------------------------------------------------------
	/**
	 * Light all of the LEDs required.
	 */
	private void updateLeds() {
		
		LOGGER.info("CLear LEDs");

		// First send a blanking command.
		ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT,
			CommandControlLight.CHANNEL1,
			CommandControlLight.POSITION_NONE,
			ColorEnum.BLACK,
			CommandControlLight.EFFECT_SOLID);
		mRadioController.sendCommand(command, getAddress(), false);
		
		// Now send the commands needed for each CHE.
		for (Map.Entry<NetGuid, List<LedCmd>> entry : mDeviceLedPosMap.entrySet()) {

			for (LedCmd ledCmd : entry.getValue()) {

				LOGGER.info("Light position: " + ledCmd.mPosition);
				command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT,
					CommandControlLight.CHANNEL1,
					ledCmd.mPosition,
					ledCmd.mColor,
					ledCmd.mEffect);
				mRadioController.sendCommand(command, getAddress(), false);
			}
		}
	}
}
