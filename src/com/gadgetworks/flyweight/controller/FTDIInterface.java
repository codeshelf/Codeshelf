/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: FTDIInterface.java,v 1.1 2013/02/20 08:28:25 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import java.io.IOException;

import jd2xx.JD2XX;
import jd2xx.JD2XX.DeviceInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.command.Packet;
import com.gadgetworks.flyweight.util.Util;

/**
 * --------------------------------------------------------------------------
 * 
 * This class encodes/decodes FlyWeightController Packets to/from the FlyWeightController network.
 * @author jeffw
 * 
 */

public final class FTDIInterface extends SerialInterfaceABC {

	private static final Log	LOGGER				= LogFactory.getLog(FTDIInterface.class);

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
	private static final int	BAUD_1250000		= 1250000;
	private static final int	READ_BUFFER_BYTES	= Packet.MAX_PACKET_BYTES * 2;
	private static final int	VID					= 0x0403;
	private static final int	GW_PID				= 0xada8;
	private static final long	GW_VID_PID			= 0x0403ada8;
	private static final int	FTDI_PID			= 0x6001;
	private static final long	FTDI_VID_PID		= 0x04036001;

	private JD2XX				mJD2XXInterface;
	private byte[]				mReadBuffer;
	private int					mReadBufferSize;
	private int					mReadBufferPos;
	private Boolean				mDeviceIsRunning	= false;

	// --------------------------------------------------------------------------
	/**
	 *  The constructor tries to setup the serial connection.
	 */
	public FTDIInterface() {
		mJD2XXInterface = new JD2XX();
		mReadBuffer = new byte[READ_BUFFER_BYTES];
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#clrRTS()
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
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#setRTS()
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
	/**
	 *  @return
	 */
	protected boolean doSetupConnection() {
		boolean result = false;

		// First try to use the GW gateway device
		if (!Util.isWindows()) {
			// On windows FTDI don't support PID/VID narrowing.
			mJD2XXInterface.setVIDPID(VID, GW_PID);
		}
		result = trySetupConnection(GW_VID_PID);

		// Next try to use a generic FTDI device.
		if (!result) {
			if (!Util.isWindows()) {
				// On windows FTDI don't support PID/VID narrowing.
				mJD2XXInterface.setVIDPID(VID, FTDI_PID);
			}
			result = trySetupConnection(FTDI_VID_PID);
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#doSetupConnection()
	 */
	private boolean trySetupConnection(long inVidPid) {

		boolean result = false;

		try {
			int deviceToOpen = -1;
			//long selectedHandle = -1;
			//mJD2XXInterface.reload(0x0403, 0x6001);
			int numDevices = mJD2XXInterface.createDeviceInfoList();
			for (int devNum = 0; devNum < numDevices; devNum++) {
				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(devNum);
				LOGGER.info("Gateway device: " + devInfo.toString());
				if (devInfo.id == inVidPid) {
					deviceToOpen = devNum;
					//selectedHandle = devInfo.handle;
				}
			}

			if (deviceToOpen == -1) {
				LOGGER.info("No Flyweight dongle found!");
			} else {
				result = true;
				mJD2XXInterface.open(deviceToOpen);
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
				
				DeviceInfo devInfo = mJD2XXInterface.getDeviceInfoDetail(deviceToOpen);
				LOGGER.info("Device started: " + devInfo.toString());
			}

		} catch (IOException e) {
			LOGGER.error("", e);
			//resetInterface();
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
				LOGGER.error("", e);
			}

//			try {
//				int status = mJD2XXInterface.getEventStatus();
//				LOGGER.error("Event status: " + status);
//				status = mJD2XXInterface.getModemStatus();
//				LOGGER.error("Modem status: " + status);
//				status = mJD2XXInterface.getQueueStatus();
//				LOGGER.error("Queue status: " + status);
//				int[] statuses = mJD2XXInterface.getStatus();
//				LOGGER.error("Data status: " + statuses[0] + " " + statuses[1] + " " + statuses[2]);
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			// mJD2XXInterface.purge(JD2XX.PURGE_RX | JD2XX.PURGE_TX);
//
//			try {
//				mJD2XXInterface.stopInTask();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
//			try {
//				mJD2XXInterface.restartInTask();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
//
//			try {
//				mJD2XXInterface.clrDtr();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
//
//			try {
//				mJD2XXInterface.clrRts();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
//
//			try {
//				mJD2XXInterface.setDtr();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
//
//			try {
//				mJD2XXInterface.setRts();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}

			if (Util.isWindows()) {
				try {
					mJD2XXInterface.cyclePort();
				} catch (IOException e) {
					LOGGER.error("", e);
				}

				try {
					mJD2XXInterface.reload(VID, GW_PID);
				} catch (IOException e) {
					// JD2XX needs to be fixed to show that it throws IOException here.
					LOGGER.error("", e);
				}

				try {
					mJD2XXInterface.reload(VID, FTDI_PID);
				} catch (IOException e) {
					// JD2XX needs to be fixed to show that it throws IOException here.
					LOGGER.error("", e);
				}

				try {
					mJD2XXInterface.resetPort();
				} catch (IOException e) {
					LOGGER.error("", e);
				}
			}

			try {
				mJD2XXInterface.resetDevice();
			} catch (IOException e) {
				LOGGER.error("", e);
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
		try {
			mJD2XXInterface.close();
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
		}
	};

	// --------------------------------------------------------------------------
	/**
	 */
	private void checkReadBuffer() {
		try {
			if (mReadBufferPos >= mReadBufferSize) {
				int[] status = mJD2XXInterface.getStatus();
				// The number of waiting bytes is in pos 0 of the status array.
				if (status[0] > 0) {
					int bytesToRead = Math.min(mReadBuffer.length, status[0]);
					mReadBufferSize = mJD2XXInterface.read(mReadBuffer, 0, bytesToRead);
					mReadBufferPos = 0;
				} else {
					// We didn't read a character, so sleep for a little.
					try {
						Thread.sleep(READ_SLEEP_MILLIS);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}

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

			checkReadBuffer();

			if (mReadBufferPos < mReadBufferSize) {
				byteRead = true;
				result = mReadBuffer[mReadBufferPos++];
			} else {
//				try {
//					Thread.sleep(READ_SLEEP_MILLIS);
//				} catch (InterruptedException e) {
//					LOGGER.error("", e);
//				}
			}

		}
		return result;
	};

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
		try {
			mJD2XXInterface.write(inByte);
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
		}
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.SerialInterfaceABC#writeBytes(byte[])
	 */
	protected void writeBytes(byte[] inBytes, int inLength) {
		long startTime = System.currentTimeMillis();
		try {
			//			long now = System.currentTimeMillis();
			mJD2XXInterface.write(inBytes, 0, inLength);
			//			System.out.println("Frame: " + inLength + " delay: " + (System.currentTimeMillis() - now));
		} catch (IOException e) {
			LOGGER.error("", e);
			resetInterface();
		}
		long endTime = System.currentTimeMillis();
		if ((endTime - startTime) > 20) {
			LOGGER.info("-------> Sendframe took: " + (endTime - startTime));
		}
	};

}
