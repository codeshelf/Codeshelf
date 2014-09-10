/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceLogic.java,v 1.6 2013/09/05 03:26:03 jeffw Exp $
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.CommandControlLed;
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
		private LedSample	mLedSample;

		public LedCmd(final Short inChannel, final LedSample inLedSample, final EffectEnum inEffect) {
			mChannel = inChannel;
			mLedSample = inLedSample;
		}
		
		public Short getPosition() {
			return mLedSample.getPosition();
		}
		
		public ColorEnum getColor() {
			return mLedSample.getColor();
		}
	}

	private Map<NetGuid, List<LedCmd>>	mDeviceLedPosMap	= new HashMap<NetGuid, List<LedCmd>>();

	public AisleDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}

	public final short getSleepSeconds() {
		return 0;
	}

	public void startDevice() {
		//		short position = 1;
		//		sendLightCommand(CommandControlLed.CHANNEL1, position, ColorEnum.BLUE, CommandControlLed.EFFECT_SOLID);
		updateLeds();
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified GUID.
	 * @param inNetGuid
	 */
	public final void clearLedCmdFor(final NetGuid inNetGuid) {
		// CD_0041 note: this will be replaced by two specific functions
		
		// Only send the command if the device is known active.
		if ((getDeviceStateEnum() != null) && (getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
			// First send a blanking command on each channel.
			List<LedSample> sampleList = new ArrayList<LedSample>();
			LedSample sample = new LedSample(CommandControlLed.POSITION_NONE, ColorEnum.BLACK);
			sampleList.add(sample);
			for (short channel = 1; channel <= 2; channel++) {
				ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, sampleList);
				mRadioController.sendCommand(command, getAddress(), true);
			}
		}
		mDeviceLedPosMap.remove(inNetGuid);
		//updateLeds();
		
		// Note: simplify if we sort out the channels by calling only:
		// mDeviceLedPosMap.remove(inNetGuid);
		// updateLeds();
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands on this aisle controller.
	 * This does the send directly. Does not leave a clearing command in mDeviceLedPosMap. No need to follow with updateLeds();
	 */
	public final void clearAllLedCmdsAndSend() {
		// CD_0041 note: one of two new functions. Clears two channels. why 2?
		
		// Only send the command if the device is known active.
		if ((getDeviceStateEnum() != null) && (getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
			// Send a blanking command on each channel.
			List<LedSample> sampleList = new ArrayList<LedSample>();
			LedSample sample = new LedSample(CommandControlLed.POSITION_NONE, ColorEnum.BLACK);
			sampleList.add(sample);
			for (short channel = 1; channel <= 2; channel++) {
				ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, sampleList);
				mRadioController.sendCommand(command, getAddress(), true);
			}
		}
		mDeviceLedPosMap.clear();
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified GUID.
	 * @param inNetGuid
	 */
	public final void removeLedCmdsForCheAndSend(final NetGuid inNetGuid) {
		// CD_0041 note: one of two new functions
		
		mDeviceLedPosMap.remove(inNetGuid);
		// Only send the command if the device is known active.
		if ((getDeviceStateEnum() != null) && (getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
			updateLeds();
		}
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
		final LedSample inLedSample,
		final EffectEnum inEffect) {
		// CD_0041 note: perfect. Gets existing or makes command list. Adds new LedCmd to list. Note, does not check if same or similar command already in list.
		// inNetGuid is the Guid of the CHE, not of this aisle controller.
		// Called only in CheDeviceLogic ledControllerSetLed(), if getLedCmdFor returned null.  So, perhaps a getOrAdd would be better.
		
		List<LedCmd> ledCmds = mDeviceLedPosMap.get(inNetGuid);
		if (ledCmds == null) {
			ledCmds = new ArrayList<LedCmd>();
			mDeviceLedPosMap.put(inNetGuid, ledCmds);
		}
		LedCmd ledCmd = new LedCmd(inChannel, inLedSample, inEffect);
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
		// CD_0041 note: Called only in CheDeviceLogic ledControllerSetLed()
		
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#scanCommandReceived(java.lang.String)
	 */
	@Override
	public void scanCommandReceived(String inCommandStr) {
		// The aisle device never returns commands.
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#buttonCommandReceived(com.gadgetworks.flyweight.command.CommandControlButton)
	 */
	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {
		// TODO Auto-generated method stub
		
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
	 * 
	 * TODO: Right now we just send the FLASH commands.  Those are the LEDs that get lit in the "ON" part of the cycle only.
	 * What we need to do is sort the samples by effect and send them in "effect groups."  There is no immediate need to 
	 * support any other effects, so we're skipping it for now.
	 */
	public final void updateLeds() {
		// CD_0041 note: Perfect for initial scope. DEV-411 will have us send out separate CommandControlLed if the byte stream of samples > 125.
		// Does it need to clear channel 2 also?

		Short channel = 1;

		List<LedSample> samples = new ArrayList<LedSample>();

		// Add one sample to clear the flashers for the CHE.
		LedSample sample = new LedSample(CommandControlLed.POSITION_NONE, ColorEnum.BLACK);
		samples.add(sample);

		// Now send the commands needed for each CHE.
		for (Map.Entry<NetGuid, List<LedCmd>> entry : mDeviceLedPosMap.entrySet()) {
			for (LedCmd ledCmd : entry.getValue()) {
				channel = ledCmd.getChannel();
				samples.add(ledCmd.getLedSample());

				LOGGER.info("Light position: " + ledCmd.getPosition() + " color: " + ledCmd.getColor() + " effect: "
						+ EffectEnum.FLASH);
			}
		}

		// Now we have to sort the samples in position order.
		Collections.sort(samples, new LedPositionComparator());

		ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, samples);
		mRadioController.sendCommand(command, getAddress(), true);
	}
}
