/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceEmbedded.java,v 1.19 2013/05/03 18:27:35 jeffw Exp $
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
	private static final int	BAUD_625000					= 625000;
	private static final int	BAUD_1250000				= 1250000;
	private static final long	FTDI_VID_PID				= 0x04036001;
	private static final long	COLOR_INTERVAL_MILLIS		= 250;
	private static final long	BLANKING_INTERVAL_MILLIS	= 500;
	private static final int	MAX_POSITIONS				= 200;

	private static final String	LEDPROCESSOR_THREAD_NAME	= "LED_PROCESSOR";

	private static final Logger	LOGGER						= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	private JD2XX				mJD2XXInterface;
	private Integer				mTotalPositions				= 48;
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
		mProcessorThread.setPriority(Thread.MAX_PRIORITY);
		mProcessorThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void resetInterface() {
		try {
			mJD2XXInterface.close();
			mJD2XXInterface.resetDevice();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
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
		//		Short nextLedPosNum = 0;
		//		LedPos ledPos = null;
		//
		//		for (int pos = 1; pos < mTotalPositions; pos++) {
		//			if (mIsBlanking) {
		//				break;
		//			} else {
		//				if ((ledPos == null) && (nextLedPosNum < mStoredPositions.size())) {
		//					ledPos = mStoredPositions.get(nextLedPosNum++);
		//				}
		//
		//				if ((ledPos != null) && (ledPos.getPosition() == pos)) {
		//					sendLedValue(ledPos.getChannel(), ledPos.getPosition(), ledPos.getNextSample());
		//					ledPos = null;
		//				}
		//			}
		//		}
		if (!mIsBlanking) {
			for (LedPos ledPos : mStoredPositions) {
				if (ledPos.getPosition() > 0) {
					sendLedValue(ledPos.getChannel(), ledPos.getPosition(), ledPos.getNextSample());
				}
			}
		}
		mIsBlanking = !mIsBlanking;

		try {
			int bytesWritten = mJD2XXInterface.write(mAllChannelsOutput);
			//			LOGGER.debug("Bytes written: " + bytesWritten);
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
	private void sendLedValue(final Short inChannel, final Short inPosition, LedValue inLedValue) {

		Byte red = inLedValue.getRed();
		for (int bit = 0; bit < 8; bit++) {
			if ((red & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[(inPosition - 1) * 24 + bit] |= (1 << (inChannel - 1));
			}
		}

		Byte green = inLedValue.getGreen();
		for (int bit = 0; bit < 8; bit++) {
			if ((green & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[(inPosition - 1) * 24 + 8 + bit] |= (1 << (inChannel - 1));
			}
		}

		Byte blue = inLedValue.getBlue();
		for (int bit = 0; bit < 8; bit++) {
			if ((blue & ((byte) (1 << bit))) != 0) {
				mAllChannelsOutput[(inPosition - 1) * 24 + 16 + bit] |= (1 << (inChannel - 1));
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
				//if (devInfo.id == inVidPid) {
				if (devInfo != null) {
					deviceToOpen = devNum;
					LOGGER.info("Gateway device: " + devInfo.toString());
					//selectedHandle = devInfo.handle;
				}
			}

			if (deviceToOpen == -1) {
				LOGGER.info("No LED controller found!");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			} else {
				result = true;

				mJD2XXInterface.open(deviceToOpen);
				mJD2XXInterface.resetDevice();
				// Async bitbang mode with all pins as outputs.
				mJD2XXInterface.setBitMode(0xff, 0x1);
				mJD2XXInterface.setBaudRate(9600);

				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(deviceToOpen);
				LOGGER.info("Device started: " + devInfo.toString());
			}

		} catch (IOException e) {
			LOGGER.error("", e);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				LOGGER.error("", e1);
			}
			//resetInterface();
		}

		return result;
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
	protected final void processControlLightCommand(CommandControlLight inCommand) {
		LOGGER.info("Light message: " + inCommand.toString());

		if (inCommand.getPosition() == CommandControlLight.POSITION_NONE) {
			mStoredPositions.clear();
		} else {
			LedPos ledPos = new LedPos(inCommand.getChannel(), inCommand.getPosition());
			ledPos.addSample(mapColorEnumToLedValue(inCommand.getColor()));
			if (ledPos.getPosition() > mTotalPositions) {
				mTotalPositions = (int) ledPos.getPosition();
			}
			mStoredPositions.add(ledPos);
		}
	}
}
