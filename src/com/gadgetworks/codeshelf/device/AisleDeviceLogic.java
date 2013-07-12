/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceLogic.java,v 1.5 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.EffectEnum;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

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
		private EffectEnum	mEffect;

		public LedCmd(final Short inChannel, final Short inPosition, final ColorEnum inColor, final EffectEnum inEffect) {
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
		updateLeds();
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified GUID.
	 * @param inNetGuid
	 */
	public final void clearLedCmdFor(final NetGuid inNetGuid) {
		// Only send the command if the device is known acrtive.
		if ((getDeviceStateEnum() != null) && (getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
			// First send a blanking command on each channel.
			List<LedSample> sampleList = new ArrayList<LedSample>();
			LedSample sample = new LedSample(CommandControlLight.POSITION_NONE, ColorEnum.BLACK);
			sampleList.add(sample);
			for (short channel = 1; channel <= 2; channel++) {
				ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, sampleList);
				mRadioController.sendCommand(command, getAddress(), true);
			}
		}
		mDeviceLedPosMap.remove(inNetGuid);
		//updateLeds();
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
		final EffectEnum inEffect) {
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

	/**
	 * Sort LedSamples by their position.
	 */
	private class LedPositionComparator implements Comparator<LedSample> {

		public int compare(LedSample inSample1, LedSample inSample2) {
			if (inSample1.getPosition() < inSample2.getPosition()) {
				return -1;
			} else if (inSample1.getPosition() > inSample2.getPosition()) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Light all of the LEDs required.
	 */
	public final void updateLeds() {

		Short channel = 1;
		EffectEnum effect = EffectEnum.FLASH;

		List<LedSample> samples = new ArrayList<LedSample>();

		// Add one sample to clear the flashers for the CHE.
		LedSample sample = new LedSample((short) -1, ColorEnum.BLACK);
		samples.add(sample);

		// Now send the commands needed for each CHE.
		for (Map.Entry<NetGuid, List<LedCmd>> entry : mDeviceLedPosMap.entrySet()) {
			for (LedCmd ledCmd : entry.getValue()) {
				channel = ledCmd.getChannel();
				effect = ledCmd.getEffect();
				sample = new LedSample(ledCmd.getPosition(), ledCmd.getColor());
				samples.add(sample);

				LOGGER.info("Light position: " + ledCmd.mPosition + " color: " + ledCmd.getColor() + " effect: "
						+ ledCmd.getEffect());
			}
		}

		// Now we have to sort the samples in position order.
		Collections.sort(samples, new LedPositionComparator());

		ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT, channel, effect, samples);
		mRadioController.sendCommand(command, getAddress(), true);
	}
}
