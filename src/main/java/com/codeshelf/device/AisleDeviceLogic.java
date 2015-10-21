/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceLogic.java,v 1.6 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

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

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.CommandControlLed;
import com.codeshelf.flyweight.command.EffectEnum;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.domain.ScannerTypeEnum;
import com.codeshelf.util.CompareNullChecker;

public class AisleDeviceLogic extends DeviceLogicABC {

	private static final Logger	LOGGER							= LoggerFactory.getLogger(AisleDeviceLogic.class);
	private static int			kNumChannelsOnAislController	= 2;												// We expect 4 ultimately. Just matching what was there.

	private static final String				THREAD_CONTEXT_TAGS_KEY				= "tags";
	private static final String				THREAD_CONTEXT_NETGUID_KEY			= "netguid";						// clone from private ContextLogging variable

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

	private long						mExpectedExpireMilliseconds	= 0;

	private Map<NetGuid, List<LedCmd>>	mDeviceLedPosMap			= new HashMap<NetGuid, List<LedCmd>>();

	public AisleDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}

	/**
	 * The following message is meant to appear on a CHE that was set up as an LED controller.
	 * This can happen after an incorrect setup as a PosCon controller
	 */
	public void connectedToServer() {
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, "LED Controller", "Connected", "Incorrect Configuration!", "This is not LED controller");
		sendRadioControllerCommand(command, true);
	}

	/**
	 * The following message is meant to appear on a CHE that was set up as an LED controller.
	 * This can happen after an incorrect setup as a PosCon controller
	 */
	public void disconnectedFromServer() {
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,
			"LED Controller",
			"Disconnected",
			"Incorrect Configuration!",
			"This is not LED controller");
		sendRadioControllerCommand(command, true);
	}

	public final short getSleepSeconds() {
		return 0;
	}

	@Override
	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_LED;
	}

	// --------------------------------------------------------------------------
	/* 
	 * This happens after reassociate. Warning: restartEnum may be null!
	 */
	@Override
	public void startDevice(DeviceRestartCauseEnum restartEnum) {
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
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#scanCommandReceived(java.lang.String)
	 */
	@Override
	public void scanCommandReceived(String inCommandStr) {
		// The aisle device never returns commands.
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#buttonCommandReceived(com.codeshelf.flyweight.command.CommandControlButton)
	 */
	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {

	}

	/**
	 * Sort LedSamples by their color, and then position.  Goofy: black = 6. That has to be first. Otherwise, just need the colors grouped.
	 */
	private class LedColorPositionComparator implements Comparator<LedSample> {

		private int compareColor(ColorEnum color1, ColorEnum color2) {
			int value = CompareNullChecker.compareNulls(color1, color2);
			if (value != 0)
				return value;
			int color1Value = color1.getValue();
			int color2Value = color2.getValue();

			if (color1Value == 6 && color2Value != 6)
				return -1;
			else if (color1Value != 6 && color2Value == 6)
				return 1;
			else

				return Integer.compare(color1Value, color2Value);

		}

		public int compare(LedSample inSample1, LedSample inSample2) {
			int value = CompareNullChecker.compareNulls(inSample1, inSample2);
			if (value != 0)
				return value;

			value = compareColor(inSample1.getColor(), inSample2.getColor());
			if (value != 0)
				return value;

			Short short1 = inSample1.getPosition();
			Short short2 = inSample2.getPosition();
			value = CompareNullChecker.compareNulls(short1, short2);
			if (value != 0)
				return value;

			return Short.compare(short1, short2);
		}
	};

	private void setExpectedLastExpiration(long inMilliseconds) {
		// Don't let a shorter timer set back after a longer one.
		if (inMilliseconds > mExpectedExpireMilliseconds)
			mExpectedExpireMilliseconds = inMilliseconds;
	}
/*
	private void clearLightsIfThisLooksLikeLastExpiry() {
		// This is flawed. Therefore not referenced.
		long currentMillis = System.currentTimeMillis();
		if (currentMillis > mExpectedExpireMilliseconds) {
			LOGGER.info("clearLightsIfThisLooksLikeLastExpiry fired.");
			clearExtraLedsFromMap();
			updateLeds();
		}
		else { // temporary
			LOGGER.info("clearLightsIfThisLooksLikeLastExpiry  does not look like last.");	
		}
	}
*/
	// --------------------------------------------------------------------------
	/**
	 * This should fire once after the input duration seconds. On firing, clear out extra LED lights and refresh the aisle lights.
	 */
	private void setLightsExpireTimer(int inSeconds) {
		long currentMillis = System.currentTimeMillis();
		long inputMillis = inSeconds * 1000;
		setExpectedLastExpiration(currentMillis + inputMillis);
		// Probably change to ScheduledExecutorService
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("AisleDeviceLogic expire timer fired.");
				// clearLightsIfThisLooksLikeLastExpiry();
				clearExtraLedsFromMap();
				updateLeds();
			}
		}, inputMillis);
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
		for (LedCmdGroup ledCmdGroup : ledCmdGroups) {
			Short channnel = ledCmdGroup.getChannelNum();
			for (LedSample ledSample : ledCmdGroup.getLedSampleList()) {
				// This is the clever part. Add for my own GUID, not a CHE guid.
				addLedCmdFor(getGuid(), channnel, ledSample, EffectEnum.FLASH);
			}
		}
		setLightsExpireTimer(inSeconds);
		updateLeds();
	}

	//--------------------------
	/**
	 * CHE_DISPLAY notifies. Similar to posconDeviceAbc one, but not the same as aisle controller does not have a worker context.
	 */
	protected void notifyDisplayTag(String logStr, String tagName) {
		boolean guidChange = false;
		String loggerNetGuid = org.apache.logging.log4j.ThreadContext.get(THREAD_CONTEXT_NETGUID_KEY);

		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, tagName);

			// A kludge to cover up some sloppiness of lack of logging context. And also, even without sloppiness, some cases happen
			// somewhat independent of a transaction context

			String myGuid = this.getMyGuidStr();
			if (!myGuid.equals(loggerNetGuid)) {
				org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_NETGUID_KEY, myGuid);
				guidChange = true;
			}
			LOGGER.info(logStr);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
			if (guidChange)
				org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_NETGUID_KEY, loggerNetGuid);
		}
	}

	protected void notifyLeds(String ledSummary) {
		notifyDisplayTag(ledSummary, "CHE_DISPLAY Lights");
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
		final Integer kMaxLedCmdSendAtATime = 12;
		final Integer kDelayMillisBetweenPartialSends = 40;
		//final Boolean splitLargeLedSendsIntoPartials = true; // became true at V7
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
			notifyLeds(toLogString);
		else { // A clearing sample was still sent
			notifyLeds("Cleared LEDs. None lit back."); // position 0 black is being sent
		}
		if (sentCount > kMaxLedCmdToLog)
			notifyLeds("And more LED not logged. Total LED Cmds this update = " + sentCount);

		// New understanding of the protocol.
		// Sort by color, then position.
		// Send partials always, changing at each color.

		// Now we have to sort the samples in position order.
		Collections.sort(samples, new LedColorPositionComparator());

		// New to V5. We are seeing that the aisle controller can only handle 22 ledCmds at once, at least with our simple cases.
		// New to V9. split commands up by color.
		//		if (false && (!splitLargeLedSendsIntoPartials || sentCount <= kMaxLedCmdSendAtATime)) {
		//			ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, samples);
		//			mRadioController.sendCommand(command, getAddress(), true);
		//		} else {
		int partialCount = 0;
		List<LedSample> partialSamples = new ArrayList<LedSample>();
		final int blackColorValue = 6; // ColorNum.BLACK;// BLACK	= 6;
		int previousColorValue = blackColorValue;
		for (LedSample theSample : samples) {
			int thisColorValue = theSample.getColor().getValue();
			// Are we changing color on this sample  in this set of samples. Do not count the first change from black.
			boolean colorChanged = thisColorValue != blackColorValue && thisColorValue != previousColorValue;

			if (colorChanged || partialCount == kMaxLedCmdSendAtATime) {
				logSampleSend(partialSamples);
				ICommand command = new CommandControlLed(NetEndpoint.PRIMARY_ENDPOINT, channel, EffectEnum.FLASH, partialSamples);
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
			previousColorValue = thisColorValue;
			partialCount++;
			partialSamples.add(theSample);

		}
		if (partialCount > 0) { // send the final leftovers
			logSampleSend(partialSamples);
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
		//
		//		}
		//
	}

	void logSampleSend(List<LedSample> inSamples) {
		String sampleSummary = "";
		for (LedSample sample : inSamples) {
			sampleSummary += sample.getPosition().toString();
			sampleSummary += ":";
			sampleSummary += ColorEnum.getLogCodeOf(sample.getColor());
			sampleSummary += ";";
		}
		LOGGER.info("sent to aisles controller:" + sampleSummary);
		;
	}

	@Override
	public byte getScannerTypeCode() {		
		return ScannerTypeEnum.scannerTypeToByte(ScannerTypeEnum.INVALID);
	}
}
