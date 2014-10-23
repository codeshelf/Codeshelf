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
import java.util.Timer;
import java.util.TimerTask;
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

public class AisleDeviceLogic extends DeviceLogicABC {

	private static final Logger	LOGGER							= LoggerFactory.getLogger(AisleDeviceLogic.class);
	private static int			kNumChannelsOnAislController	= 2;												// We expect 4 ultimately. Just matching what was there.

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
		LOGGER.info("Start aisle controller (after association " + getMyGuidStr());
		//		short position = 1;
		//		sendLightCommand(CommandControlLed.CHANNEL1, position, ColorEnum.BLUE, CommandControlLed.EFFECT_SOLID);
		updateLeds();
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands on this aisle controller.
	 * This does the send directly. Does not leave a clearing command in mDeviceLedPosMap. No need to follow with updateLeds();
	 */
	public final void clearAllLedCmdsAndSend() {
		// CD_0041 note: one of two new functions. Clears two channels. why 2?
		// Note: as of V4, this is never called.
		// We really should have a means to call it. The important part is mDeviceLedPosMap.clear(); If some CHE's led samples gets in there, and then the CHE never goes away,
		// How can we get those lights off?

		LOGGER.info("Clear LEDs for all CHEs on " + getMyGuidStr());

		// Only send the command if the device is known active.
		if (isDeviceAssociated()) {
			// Send a blanking command on each channel.
			List<LedSample> sampleList = new ArrayList<LedSample>();
			LedSample sample = new LedSample(CommandControlLed.POSITION_NONE, ColorEnum.BLACK);
			sampleList.add(sample);
			for (short channel = 1; channel <= kNumChannelsOnAislController; channel++) {
				ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, sampleList);
				mRadioController.sendCommand(command, getAddress(), true);
			}
		}
		mDeviceLedPosMap.clear();
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear all of the active LED commands for the specified CHE GUID. on this aisle controller
	 * @param inNetGuid
	 */
	public final void removeLedCmdsForCheAndSend(final NetGuid inNetGuid) {
		// CD_0041 note: one of two new functions
		String cheGuidStr = inNetGuid.getHexStringNoPrefix();

		LOGGER.info("Clear LEDs for CHE:" + cheGuidStr + " on " + getMyGuidStr());

		mDeviceLedPosMap.remove(inNetGuid);
		// Only send the command if the device is known active.
		if (isDeviceAssociated()) {
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
		// exception: GUID of this aisle controller if this is a temporary light command from the UI.
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
	
	}

	/**
	 * Sort LedSamples by their position.
	 */
	private class LedPositionComparator implements Comparator<LedSample> {

		public int compare(LedSample inSample1, LedSample inSample2) {
			// throw proofing. See these errors sometimes.
			if (inSample1 == null || inSample2 == null ){
				LOGGER.error("null object in LedPositionComparator");
				return 0;
			}
			Short short1 = inSample1.getPosition();
			Short short2 = inSample2.getPosition();
			if (short1 == null || short2 == null ){
				LOGGER.error("uninitialized object in LedPositionComparator");
				return 0;
			}
		
			if (short1 < short2) {
				return -1;
			} else if (short1 > short2) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Utility function. Should be promoted, and get a cached value.
	 */
	private String getMyGuidStr() {
		String thisGuidStr = "";
		NetGuid thisGuid = this.getGuid();
		if (thisGuid != null)
			thisGuidStr = thisGuid.getHexStringNoPrefix();
		return thisGuidStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * This should fire once after the input duration seconds. On firing, clear out extra LED lights and refresh the aisle lights.
	 */
	private void setLightsExpireTimer(int inSeconds) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
				  clearExtraLedsFromMap();
				  updateLeds();
			  }
			}, inSeconds*1000);
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the data structure
	 */
	private void clearExtraLedsFromMap() {
		NetGuid thisAisleControllerGuid = getGuid();
		mDeviceLedPosMap.remove(thisAisleControllerGuid);
	}

	// --------------------------------------------------------------------------
	/**
	 * Light additional LEDs a user asked for. This is the function on AisleDeviceLogic that does most everything:
	 * 1) Clear out any previous user lights
	 * 2) Add the commands in, so that the call to updateLeds will result in these also showing.
	 * 3) Set a timer
	 * 4) Finally, call updateLeds() to make the lights go.
	 */
	public final void lightExtraLeds(int inSeconds, String inCommands) {
		clearExtraLedsFromMap();

		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inCommands);
		for (LedCmdGroup ledCmdGroup :ledCmdGroups){
			Short channnel = ledCmdGroup.getChannelNum();			
			for (LedSample ledSample : ledCmdGroup.getLedSampleList()) {
				// This is the clever part. Add for my own GUID, not a CHE guid.
				addLedCmdFor(getGuid(), channnel, ledSample, EffectEnum.FLASH);
			}
		}
		setLightsExpireTimer(inSeconds);
		updateLeds();
	}
	
// --------------------------------------------------------------------------
	/**
	 * Light all of the LEDs required.
	 * 
	 * Right now we just send the FLASH commands.  Those are the LEDs that get lit in the "ON" part of the cycle only.
	 * What we need to do is sort the samples by effect and send them in "effect groups."  There is no immediate need to 
	 * support any other effects, so we're skipping it for now.
	 */
	public final void updateLeds() {
		// See CD_0041 for initial scope. DEV-411 will have us send out separate CommandControlLed if the byte stream of samples > 125.
		// Looks like it does not really work yet for multiple channels. Does this need to figure out each channel, then send separate commands? Probably.
		final Integer kMaxLedCmdToLog = 25;
		final Integer kMaxLedCmdSendAtATime = 20;
		final Integer kDelayMillisBetweenPartialSends = 30;
		final Boolean splitLargeLedSendsIntoPartials = true; // became true at V7
		String myGuidStr = getMyGuidStr();

		Short channel = 1;

		List<LedSample> samples = new ArrayList<LedSample>();

		// Add one sample to clear the flashers for the CHE.
		LedSample sample = new LedSample(CommandControlLed.POSITION_NONE, ColorEnum.BLACK);
		samples.add(sample);

		String toLogString = "updateLeds on " + myGuidStr + ". " + EffectEnum.FLASH;
		Integer sentCount = 0;
		// Now send the commands needed for each CHE.
		for (Map.Entry<NetGuid, List<LedCmd>> entry : mDeviceLedPosMap.entrySet()) {
			for (LedCmd ledCmd : entry.getValue()) {
				channel = ledCmd.getChannel();
				samples.add(ledCmd.getLedSample());

				// Log concisely instead of each ledCmd individually
				sentCount++;
				if (sentCount <= kMaxLedCmdToLog)
					toLogString = toLogString + " " + ledCmd.getPosition() + ":" + ledCmd.getColor();
			}
		}
		if (sentCount > 0)
			LOGGER.info(toLogString);
		else { // A clearing sample was still sent
			LOGGER.info("updateLeds on " + myGuidStr + ". Cleared. None lit back."); // position 0 black is being sent
		}
		if (sentCount > kMaxLedCmdToLog)
			LOGGER.info("And more LED not logged. Total LED Cmds this update = " + sentCount);

		// Now we have to sort the samples in position order.
		Collections.sort(samples, new LedPositionComparator());

		// New to V5. We are seeing that the aisle controller can only handle 22 ledCmds at once, at least with our simple cases.
		if (!splitLargeLedSendsIntoPartials || sentCount <= kMaxLedCmdSendAtATime) {
			ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, samples);
			mRadioController.sendCommand(command, getAddress(), true);
		} else {
			int partialCount = 0;
			List<LedSample> partialSamples = new ArrayList<LedSample>();
			for (LedSample theSample : samples) {
				partialCount++;
				partialSamples.add(theSample);
				if (partialCount == kMaxLedCmdSendAtATime) {
					LOGGER.info("partial send to aisle controller");
					ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT,
						channel,
						EffectEnum.FLASH,
						partialSamples);
					mRadioController.sendCommand(command, getAddress(), true);
					
					partialCount = 0;
					// we have to leave the old reference to partialSamples as that is floating off in a command.
					// New list to accumulate more samples
					partialSamples = new ArrayList<LedSample>();

					// experiment. Set to 30 for v7
					if (kDelayMillisBetweenPartialSends > 0)
						try { // Not sure this helps. I hate to starve this thread at all. Definitely cuts down on useless sends to aisle controller that cannot receive yet.
							Thread.sleep(kDelayMillisBetweenPartialSends);
						} catch (InterruptedException e) {
							LOGGER.error("updateLeds delay exeption", e);
						}
				}
			}
			if (partialCount > 0) { // send the final leftovers
				LOGGER.info("last partial send to aisle controller");
				ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, partialSamples);
				mRadioController.sendCommand(command, getAddress(), true);
				
				// same as above. Wait after last partial
				if (kDelayMillisBetweenPartialSends > 0)
					try { // Not sure this helps. Might need to throw in another thread and modify the protocol a bit.
						Thread.sleep(kDelayMillisBetweenPartialSends);
					} catch (InterruptedException e) {
						LOGGER.error("updateLeds delay exeption", e);
					}

			}
		}
	}
}
