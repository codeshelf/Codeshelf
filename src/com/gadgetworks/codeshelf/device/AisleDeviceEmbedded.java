/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceEmbedded.java,v 1.10 2013/04/17 17:02:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd2xx.JD2XX;
import jd2xx.JD2XX.DeviceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
public class AisleDeviceEmbedded extends DeviceEmbeddedABC {

	//	private static final int	BAUD_312500		= 312500;
	//	private static final int	BAUD_416667		= 416667;
	//	private static final int	BAUD_625000		= 625000;
	private static final int	BAUD_1250000				= 1250000;
	private static final long	FTDI_VID_PID				= 0x04036001;
	private static final long	COLOR_INTERVAL_MILLIS		= 250;
	private static final long	BLANKING_INTERVAL_MILLIS	= 500;
	private static final int	MAX_POSITIONS				= 200;

	private static final String	LEDPROCESSOR_THREAD_NAME	= "LED_PROCESSOR";

	private static final Logger	LOGGER						= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	private JD2XX				mJD2XXInterface;
	private Integer				mTotalPositions				= 32;
	private List<LedPos>		mStoredPositions;
	private Boolean				mIsBlanking					= false;
	private byte[]				mAllChannelsOutput;

	private long				mLastProcessMillis;
	private Thread				mProcessorThread;

	@Inject
	public AisleDeviceEmbedded(@Named(IEmbeddedDevice.GUID_PROPERTY) final String inGuidStr,
		@Named(IEmbeddedDevice.CONTROLLER_IPADDR_PROPERTY) final String inIpAddrStr) {
		super(inGuidStr, inIpAddrStr);

		mJD2XXInterface = new JD2XX();
		mStoredPositions = new ArrayList<LedPos>();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#start()
	 */
	@Override
	public void doStart() {

		// Setup the FTDI D2XX interface.
		setupConnection(FTDI_VID_PID);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		mProcessorThread = new Thread(new Runnable() {
			public void run() {
				process();
			}
		}, LEDPROCESSOR_THREAD_NAME);
		mProcessorThread.setDaemon(true);
		mProcessorThread.setPriority(Thread.MIN_PRIORITY);
		mProcessorThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void process() {
		while (isShouldRun()) {
			try {
				if (mIsBlanking) {
					if (System.currentTimeMillis() > (mLastProcessMillis + COLOR_INTERVAL_MILLIS)) {

						// Time to harvest events.
						refreshLedChannels();
						mLastProcessMillis = System.currentTimeMillis();
					}
				} else {
					if (System.currentTimeMillis() > (mLastProcessMillis + BLANKING_INTERVAL_MILLIS)) {

						// Time to harvest events.
						refreshLedChannels();
						mLastProcessMillis = System.currentTimeMillis();
					}
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			} catch (Exception e) {
				// We don't want the thread to exit on some weird, uncaught errors in the processor.
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  The radio controller sent this CHE a command.
	 *  @param inCommand    The control command that we want to process.  (The one just received.)
	 */
	protected void processControlCmd(CommandControlABC inCommand) {

		// Figure out what kind of control sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandControlABC.MESSAGE:
				break;

			case CommandControlABC.SCAN:
				break;

			case CommandControlABC.LIGHT:
				processControlLightCommand((CommandControlLight) inCommand);
				break;

			default:
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void refreshLedChannels() {
		mAllChannelsOutput = new byte[mTotalPositions * 24];
		Integer nextLedPosNum = 0;
		LedPos ledPos = null;
		//LOGGER.debug("\nFlash");
		for (int pos = 0; pos < mTotalPositions; pos++) {
			if (mIsBlanking) {
				break;
			} else {
				if ((ledPos == null) && (nextLedPosNum < mStoredPositions.size())) {
					ledPos = mStoredPositions.get(nextLedPosNum++);
				}

				if ((ledPos != null) && (ledPos.getPosition() == pos)) {
					sendLedValue(pos, ledPos.getNextSample());
					ledPos = null;
				} else {
					//sendLedOff(pos);
				}
			}
		}
		mIsBlanking = !mIsBlanking;
		//		LOGGER.debug(Arrays.toString(mAllChannelsOutput));
		try {
			mJD2XXInterface.write(mAllChannelsOutput);
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
			setupConnection(FTDI_VID_PID);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLedValues
	 */
	private void sendLedValue(final Integer inPos, LedValue inLedValue) {

		Byte blue = inLedValue.getBlue();
		for (int bit = 0; bit < 8; bit++) {
			if ((blue & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[inPos * 24 + bit] |= 1;
			}
		}

		Byte green = inLedValue.getGreen();
		for (int bit = 0; bit < 8; bit++) {
			if ((green & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[inPos * 24 + 8 + bit] |= 1;
			}
		}

		Byte red = inLedValue.getRed();
		for (int bit = 0; bit < 8; bit++) {
			if ((red & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[inPos * 24 + 16 + bit] |= 1;
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#doSetupConnection()
	 */
	private boolean setupConnection(long inVidPid) {

		boolean result = false;

		try {
			int deviceToOpen = -1;
			int numDevices = mJD2XXInterface.createDeviceInfoList();
			for (int devNum = 0; devNum < numDevices; devNum++) {
				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(devNum);
				LOGGER.info("Gateway device: " + devInfo.toString());
				//if (devInfo.id == inVidPid) {
				if (devInfo != null) {
					deviceToOpen = devNum;
					//selectedHandle = devInfo.handle;
				}
			}

			if (deviceToOpen == -1) {
				LOGGER.info("No gateway found!");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			} else {
				result = true;

				mJD2XXInterface.open(deviceToOpen);
				mJD2XXInterface.resetDevice();
				// Async bitbang mode with all pins as outputs.
				mJD2XXInterface.setBitMode(0xf, 0x1);
				mJD2XXInterface.setBaudRate(BAUD_1250000);

				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(deviceToOpen);
				LOGGER.info("Device started: " + devInfo.toString());
			}

		} catch (IOException e) {
			LOGGER.error("", e);
			//resetInterface();
		}

		return result;
	}

	private void resetInterface() {
		try {
			mJD2XXInterface.close();
			mJD2XXInterface.resetDevice();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	private LedValue mapColorEnumToLedValue(final ColorEnum inColorEnum) {
		LedValue result = null;

		switch (inColorEnum) {
			case RED:
				result = new LedValue(LedValue.LED_RED);
				break;

			case GREEN:
				result = new LedValue(LedValue.LED_GREEN);
				break;

			case BLUE:
				result = new LedValue(LedValue.LED_BLUE);
				break;

			case CYAN:
				result = new LedValue(LedValue.LED_CYAN);
				break;

			case MAGENTA:
				result = new LedValue(LedValue.LED_MAGENTA);
				break;

			case ORANGE:
				result = new LedValue(LedValue.LED_ORANGE);
				break;

			case BLACK:
				result = new LedValue(LedValue.LED_BLACK);
				break;

			case WHITE:
				result = new LedValue(LedValue.LED_WHITE);
				break;

			default:
				result = new LedValue(LedValue.LED_RED);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	protected void processControlLightCommand(CommandControlLight inCommand) {
		LOGGER.info("Light message: " + inCommand.toString());

		if (inCommand.getPosition() == CommandControlLight.POSITION_NONE) {
			mStoredPositions.clear();
		} else {
			LedPos ledPos = new LedPos(inCommand.getPosition());
			ledPos.addSample(mapColorEnumToLedValue(inCommand.getColor()));
			mStoredPositions.add(ledPos);
		}
	}
}
