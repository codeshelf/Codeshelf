/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: FTDIInterface.java,v 1.5 2013/08/23 21:28:08 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd2xx.JD2XX;
import jd2xx.JD2XX.DeviceInfo;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.Packet;

/**
 * --------------------------------------------------------------------------
 * 
 * This class encodes/decodes FlyWeightController Packets to/from the FlyWeightController network.
 * @author jeffw
 * 
 */

public final class FTDIInterface extends SerialInterfaceABC {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(FTDIInterface.class);
	
	private static final int		NUM_PKTS_DROP			= 10;
	private static final int		MAX_PACKET_BYTES		= IPacket.MAX_PACKET_BYTES;

	//	private static final int	TX_TIMEOUT_MILLIS	= 100;
	//	private static final int	RX_TIMEOUT_MILLIS	= 100;
	//	private static final int	LATENCY_MILLIS		= 10;

	/*
	 * Baud rates:
	 * 
	 * The HCS08 MCUs generate their baud rates by using the bus rate / (prescaler * 16).
	 * The cbus rate in the current design is 1.25Mhz, so the achievable baud rates are
	 * 1,250,000 / prescaler, so rates can range from 153 - 1,250,000 baud.
	 * 
	 * The FTDI generates baud by taking 3,000,000 / desired baud rate.  This results in an
	 * integer and a remainder.  The valid values for the remainder are 1/8ths (0, .125, .250, etc.)
	 * As long as you can get within +/-3% of the desired baud rate then this will work.
	 * 
	 * The HC08 is much more limited in it's valid baud rates, so the list of rate reflects that.
	 * The rates listed here are the ones know to work for both sides.
	 */

	//	private static final int BAUD_156250 = 156250;
	//	private static final int BAUD_178571 = 178571;
	//	private static final int BAUD_208333 = 208333;
	//	private static final int BAUD_250000 = 250000;
	//	private static final int	BAUD_312500			= 312500;
	//	private static final int BAUD_416667 = 416667;
	//	private static final int BAUD_625000 = 625000;
	private static final int	BAUD_1250000			= 1250000;
	private static final int	DEFAULT_BUFFER_SIZE		= Packet.MAX_PACKET_BYTES * 200;
	private static final int	DEFAULT_SLEEP_MILLIS	= 1;//25;
	//private static final int	VID					= 0x0403;
	//private static final int	GW_PID				= 0xada8;
	private static final long	GW_VID_PID				= 0x0403ada8;
	//private static final int	FTDI_PID			= 0x6001;
	private static final long	FTDI_VID_PID			= 0x04036001;

	private CircularFifoBuffer	buffer;
	private JD2XX				mJD2XXInterface;
	private byte[]				mReadBuffer;
	private int					mReadBufferSize;
	private int					mReadBufferPos;
	private Boolean				mDeviceIsRunning		= false;
	private Boolean				mPause					= false;
	@SuppressWarnings("unused")
	private int[] status = new int[3];			

	// --------------------------------------------------------------------------
	/**
	 *  The constructor tries to setup the serial connection.
	 */
	public FTDIInterface() {
		mReadBuffer = new byte[DEFAULT_BUFFER_SIZE];
		buffer = new CircularFifoBuffer(DEFAULT_BUFFER_SIZE);
		try {
			mJD2XXInterface = new JD2XX();
		} catch (UnsatisfiedLinkError e) {
			LOGGER.error("JD2XX is not available");
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#clrRTS()
	 */
	protected void clrRTS() {
		try {
			mJD2XXInterface.clrRts();
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#setRTS()
	 */
	protected void setRTS() {
		try {
			mJD2XXInterface.setRts();
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#doSetupConnection()
	 */
	protected boolean doSetupConnection() {
		boolean result = false;

		List<Long> vidPidList = new ArrayList<Long>();
		vidPidList.add(GW_VID_PID);
		vidPidList.add(FTDI_VID_PID);

		int deviceToOpen = selectDeviceToOpen(vidPidList);

		result = trySetupConnection(deviceToOpen);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inVidPidList
	 * @return
	 */
	private int selectDeviceToOpen(List<Long> inVidPidList) {
		int result = -1;
		try {
			//long selectedHandle = -1;
			//mJD2XXInterface.reload(0x0403, 0x6001);
			int numDevices = mJD2XXInterface.createDeviceInfoList();
			for (int devNum = 0; devNum < numDevices; devNum++) {
				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(devNum);
				LOGGER.info("Gateway device: " + devInfo.toString());
				for (Long vidPid : inVidPidList) {
					if (devInfo.id == vidPid) {
						result = devNum;
						//selectedHandle = devInfo.handle;
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		if (result == -1) {
			LOGGER.info("No Flyweight dongle found!");
		}

		return result;
	}

	private boolean trySetupConnection(int inDeviceToOpen) {

		boolean result = false;

		try {
			result = true;
			//mJD2XXInterface.close();
			mJD2XXInterface.open(inDeviceToOpen);
			mJD2XXInterface.purge(JD2XX.PURGE_RX);
			mJD2XXInterface.purge(JD2XX.PURGE_TX);

			mJD2XXInterface.resetDevice();
			//mJD2XXInterface.setLatencyTimer(LATENCY_MILLIS);

			mJD2XXInterface.setBaudRate(BAUD_1250000);
			mJD2XXInterface.setDataCharacteristics(JD2XX.BITS_8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE);
			mJD2XXInterface.setFlowControl(JD2XX.FLOW_RTS_CTS, 0, 0);
			//mJD2XXInterface.setTimeouts(RX_TIMEOUT_MILLIS, TX_TIMEOUT_MILLIS);

			//				synchronized (mDeviceIsRunning) {
			mDeviceIsRunning = true;
			//				}

			//				try {
			//					mJD2XXInterface.addEventListener(
			//						new JD2XXEventListener() {
			//							public void jd2xxEvent(JD2XXEvent inEvent) {
			//								JD2XX jd2xx = (JD2XX) inEvent.getSource();
			//								int event = inEvent.getEventType();
			//								try {
			//									if ((event & mJD2XXInterface.EVENT_RXCHAR) != 0) {
			//										int r = jd2xx.getQueueStatus();
			//										LOGGER.info("RX event: " + new String(jd2xx.read(r)));
			//									} else if ((event & mJD2XXInterface.EVENT_MODEM_STATUS) != 0) {
			//										LOGGER.info("Modem status event type: " + event);
			//									}
			//								} catch (IOException e) {
			//									LOGGER.error("", e);
			//								}
			//							}
			//						}
			//					);
			//				} catch (TooManyListenersException e) { 
			//					LOGGER.error("", e);
			//				}

			//				try {
			//					mJD2XXInterface.notifyOnEvent(JD2XX.EVENT_RXCHAR | JD2XX.EVENT_MODEM_STATUS, true);
			//					mJD2XXInterface.setEventNotification(JD2XX.EVENT_RXCHAR | JD2XX.EVENT_MODEM_STATUS, selectedHandle);
			//				} catch (IOException e) {
			//					LOGGER.error("", e);
			//				}

			DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(inDeviceToOpen);
			LOGGER.info("Device started: " + devInfo.toString());

		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			//resetInterface();
			mJD2XXInterface = new JD2XX();
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#doStartInterface()
	 */
	protected void doStartInterface() {
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#doResetInterface()
	 */
	protected void doResetInterface() {
		//		synchronized (mDeviceIsRunning) {
		if (mDeviceIsRunning) {
			mDeviceIsRunning = false;
			LOGGER.error("USB Interface reset");

			// We want to try all of these reset strategies, all of which can have weird exception
			// side-effects, so we catch each one and move to the next.

			try {
				mJD2XXInterface.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close interface", e);
			}

			try {
				mJD2XXInterface.resetDevice();
			} catch (IOException e) {
				LOGGER.error("Failed to reset device", e);
			}
		}
		//		}

		//doSetupConnection();
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#doStopInterface()
	 */
	protected void doStopInterface() {
		if (mJD2XXInterface == null) {
			LOGGER.warn("doStopInterface called, but mJD2XXInterface was null");
		} else {
			try {
				mJD2XXInterface.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close interface", e);
				resetInterface();
			}
		}
	};

	// --------------------------------------------------------------------------
	/**
	 */
	private void checkReadBuffer() {
		try {

			//int[] status = mJD2XXInterface.getStatus();
			int bytesReady = mJD2XXInterface.getQueueStatus();
			int mask = 0;
			mask = mask | JD2XX.EVENT_RXCHAR;

			if (bytesReady > (NUM_PKTS_DROP * MAX_PACKET_BYTES)) {
				LOGGER.warn("Purgin the JD2XX RX buffer! Packets backlogged {} bytes", bytesReady);
				mJD2XXInterface.purge(JD2XX.PURGE_RX);
			} else if (!buffer.isFull() && bytesReady > 0){ 
				int bytesToRead = Math.min(DEFAULT_BUFFER_SIZE - buffer.size(),  bytesReady);
				//LOGGER.info("---------- Start read from FTDI ---------------------------------------------- ");
				//LOGGER.info("------------- Radio has {} bytes", status[0]);
				mReadBufferSize = mJD2XXInterface.read(mReadBuffer, 0, bytesToRead);
				//hexDumpArray(mReadBuffer, mReadBufferSize);
				//LOGGER.info("------------------------------------------------------------------------ ");
				mReadBufferPos = 0;

				for (int i = 0; i < mReadBufferSize; i++) {
					buffer.add(mReadBuffer[i]);
				}
			}
		} catch (IOException e) {
			resetInterface();
		} catch (IndexOutOfBoundsException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#readByte()
	 */
	protected byte readByte() {
		byte result = 0;
		boolean byteRead = false;

		while ((shouldRun()) && (!byteRead)) {

			while (mPause) {
				try {
					Thread.sleep(DEFAULT_SLEEP_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			}

			checkReadBuffer();

			if (!buffer.isEmpty()) {
				byteRead = true;
				result = (byte) buffer.remove();
			}
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#readBytes()
	 */
	protected int readBytes(byte[] inBuffer) {
		int result = 0;

		checkReadBuffer();

		if (mReadBufferPos < mReadBufferSize) {
			result = Math.min(mReadBufferSize - mReadBufferPos, inBuffer.length);
			System.arraycopy(mReadBuffer, mReadBufferPos, inBuffer, 0, result);
		}

		return result;
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#writeByte(byte)
	 */
	protected void writeByte(byte inByte) {
		int bytesWritten = 0;
		
		try {
			bytesWritten = mJD2XXInterface.write(inByte);
		} catch (IOException e) {
			LOGGER.error("Failed to send data", e.getMessage());
			resetInterface();
		}
		
		if (bytesWritten != 1) {
			LOGGER.error("Radio did not write byte!");
		}
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#writeBytes(byte[])
	 */
	protected void writeBytes(byte[] inBytes, int inLength) {
		long startTime = System.currentTimeMillis();
		int bytesWritten = 0;
		try {
			//			long now = System.currentTimeMillis();
			bytesWritten = mJD2XXInterface.write(inBytes, 0, inLength);
			//			System.out.println("Frame: " + inLength + " delay: " + (System.currentTimeMillis() - now));
		
		} catch (IOException e) {
			LOGGER.error("Failed to send data", e);
			resetInterface();
		}
		
		long endTime = System.currentTimeMillis();
		if ((endTime - startTime) > 20) {
			LOGGER.info("-------> Sendframe took: " + (endTime - startTime));
		}
		
		if (bytesWritten != inLength) {
			LOGGER.error("Radio did not write all the bytes! Wrote {} of {}", bytesWritten, inLength);
		}
	}
	
	public void pause() {
		mPause = true;
	}
	
	public void resume() {
		mPause = false;
	}

}
