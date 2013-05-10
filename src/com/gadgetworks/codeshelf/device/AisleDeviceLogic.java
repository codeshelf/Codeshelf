/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceLogic.java,v 1.2 2013/05/10 16:55:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

public class AisleDeviceLogic extends DeviceLogicABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AisleDeviceLogic.class);

	@Accessors(prefix = "m")
	protected class LedCmd {
		@Getter
		private Short		mChannel;
		@Getter
		private Short		mPosition;
		@Getter
		private ColorEnum	mColor;
		@Getter
		private String		mEffect;

		public LedCmd(final Short inChannel, final Short inPosition, final ColorEnum inColor, final String inEffect) {
			mChannel = inChannel;
			mPosition = inPosition;
			mColor = inColor;
			mEffect = inEffect;
		}
	}

	private Map<NetGuid, List<LedCmd>>	mDeviceLedPosMap	= new HashMap<NetGuid, List<LedCmd>>();

	public AisleDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}

	@Override
	public void start() {
		//		short position = 1;
		//		sendLightCommand(CommandControlLight.CHANNEL1, position, ColorEnum.BLUE, CommandControlLight.EFFECT_SOLID);
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified GUID.
	 * @param inNetGuid
	 */
	public final void clearLedCmdFor(final NetGuid inNetGuid) {
		// First send a blanking command on each channel.
		for (short channel = 1; channel <= 2; channel++) {
			ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT,
				channel,
				CommandControlLight.POSITION_NONE,
				ColorEnum.BLACK,
				CommandControlLight.EFFECT_SOLID);
			mRadioController.sendCommand(command, getAddress(), false);
		}
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
	public final void addLedCmdFor(final NetGuid inNetGuid,
		final Short inChannel,
		final Short inPosition,
		final ColorEnum inColor,
		final String inEffect) {
		List<LedCmd> ledCmds = mDeviceLedPosMap.get(inNetGuid);
		if (ledCmds == null) {
			ledCmds = new ArrayList<LedCmd>();
			mDeviceLedPosMap.put(inNetGuid, ledCmds);
		}
		LedCmd ledCmd = new LedCmd(inChannel, inPosition, inColor, inEffect);
		ledCmds.add(ledCmd);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inNetGuid
	 * @param inChannel
	 * @param inPosition
	 * @return
	 */
	public final LedCmd getLedCmdFor(final NetGuid inNetGuid, final Short inChannel, final Short inPosition) {
		LedCmd result = null;

		List<LedCmd> ledCmds = mDeviceLedPosMap.get(inNetGuid);
		if (ledCmds != null) {
			for (LedCmd ledCmd : ledCmds) {
				if (ledCmd.getChannel().equals(inChannel) && ledCmd.getPosition().equals(inPosition)) {
					result = ledCmd;
					break;
				}
			}
		}

		return result;
	}

	@Override
	public void commandReceived(String inCommandStr) {
		// The aisle device never returns commands.
	}

	// --------------------------------------------------------------------------
	/**
	 * Light all of the LEDs required.
	 */
	public final void updateLeds() {

		LOGGER.info("CLear LEDs");

		// Now send the commands needed for each CHE.
		for (Map.Entry<NetGuid, List<LedCmd>> entry : mDeviceLedPosMap.entrySet()) {

			for (LedCmd ledCmd : entry.getValue()) {

				LOGGER.info("Light position: " + ledCmd.mPosition);
				ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT,
					ledCmd.getChannel(),
					ledCmd.getPosition(),
					ledCmd.getColor(),
					ledCmd.getEffect());
				mRadioController.sendCommand(command, getAddress(), false);
			}
		}
	}
}
