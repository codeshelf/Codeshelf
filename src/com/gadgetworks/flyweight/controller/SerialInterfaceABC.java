/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: SerialInterfaceABC.java,v 1.5 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;

/**
 * --------------------------------------------------------------------------
 * 
 * This is the abstract class for encoding/decoding serial frames that contain
 * packets/commands for the FlyWeightController network.
 * 
 * @author jeffw
 * 
 */

public abstract class SerialInterfaceABC implements IGatewayInterface {

	public static final int		SERIAL_RESET_TIMEOUT_MS	= 500;
	public static final int		READ_SLEEP_MILLIS		= 1;
	public static final int		READ_RECOVER_MILLIS		= 5000;
	public static final int		WAIT_INTERFACE_MILLIS	= 10;

	private static final Logger	LOGGER					= LoggerFactory.getLogger(SerialInterfaceABC.class);

	private final Object		mLock					= new Object();

	private boolean				mIsStarted;
	private boolean				mShouldRun				= true;
	private boolean				mIsStartingInterface;

	// --------------------------------------------------------------------------
	/**
	 *  The constructor tries to setup the serial connection.
	 */
	SerialInterfaceABC() {

		mIsStarted = false;
		//		mHexDumpEncoder = new HexDumpEncoder();

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IGatewayInterface#startInterface()
	 */
	public final void startInterface() {
		mShouldRun = true;

		boolean isSetup = false;

		try {
			if (!mIsStartingInterface) {
				mIsStartingInterface = true;

				// Try to setup the serial I/O.
				int retryCount = 0;
				while ((mShouldRun) && !isSetup) {
					retryCount++;
					if (retryCount == 1) {
						LOGGER.info("Attempting to init serial interface.");
						isSetup = setupConnection();
					} else {
						try {
							Thread.sleep(SERIAL_RESET_TIMEOUT_MS);
						} catch (InterruptedException e) {
							LOGGER.error("", e);
						}
						if (retryCount > 20) {
							retryCount = 0;
						}
					}
				}

				// If we're still running then finish setting up the interface at the sub-classes.
				if (mShouldRun) {
					doStartInterface();

					// Send the END character, so that the receiver has a fresh start on the first frame sent.
					if (isSetup) {
						writeByte(IGatewayInterface.END);
						writeByte(IGatewayInterface.END);
						writeByte(IGatewayInterface.END);
					}
				}
				mIsStartingInterface = false;
			}
		} catch (Exception e) {
			LOGGER.error("", e);
			mIsStartingInterface = false;
		}
		if ((isSetup) && (mShouldRun)) {
			mIsStarted = true;
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IGatewayInterface#resetInterface()
	 */
	public final void resetInterface() {
		// We can only reset an already running interface.
		synchronized (mLock) {
			if (mShouldRun) {
				stopInterface();
				// First let's pause 5 seconds to make sure everything settles.
				try {
					mLock.wait(READ_RECOVER_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				doResetInterface();
				startInterface();
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IGatewayInterface#stopInterface()
	 */
	public final void stopInterface() {
		mIsStarted = false;
		mShouldRun = false;
		doStopInterface();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IGatewayInterface#isStarted()
	 */
	public final boolean isStarted() {
		return mIsStarted;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Determine if the serial interface should keep runnning.
	 *  @return
	 */
	protected final boolean shouldRun() {
		return (mShouldRun);
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	public final IPacket receivePacket(NetworkId inMyNetworkId) {

		IPacket result = null;
		IPacket packet = null;

		// Get the next frame from the serial stream.
		//LOGGER.debug("Wait for frame.");
		byte[] nextFrameArray = this.receiveFrame();

		if (nextFrameArray.length == 0) {
			try {
				Thread.sleep(WAIT_INTERFACE_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		} else {

			ByteArrayInputStream byteArray = new ByteArrayInputStream(nextFrameArray);
			BitFieldInputStream inputStream = new BitFieldInputStream(byteArray, true);

			// Receive the next packet.
			packet = new Packet();
			packet.fromStream(inputStream, nextFrameArray.length);

			if ((packet.getNetworkId().equals(inMyNetworkId))
					|| (packet.getNetworkId().equals(new NetworkId(IPacket.ZERO_NETWORK_ID)))
					|| (packet.getNetworkId().equals(new NetworkId(IPacket.BROADCAST_NETWORK_ID)))) {
				result = packet;

				//				if (LOGGER.isInfoEnabled()) {
				//					LOGGER.info("Receive packet: " + result.toString());
				//				}
			}
			if ((LOGGER.isDebugEnabled() && (result != null))) {
				try {
					LOGGER.info("Receive packet: " + result.toString());
					hexDumpArray(nextFrameArray);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inPacket	The packet that we want to send across the serial interface.
	 */
	public final void sendPacket(IPacket inPacket) {

		while (!mIsStarted) {
			try {
				Thread.sleep(SERIAL_RESET_TIMEOUT_MS);
			} catch (InterruptedException e) {
				LOGGER.warn("", e);
			}
		}

		// Reset the byte array in preparation for the generation of a new frame.
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		BitFieldOutputStream bitFieldOutStream = new BitFieldOutputStream(byteArrayStream);
		byteArrayStream.reset();

		// Spool the packet to the formatted bit stream.
		inPacket.toStream(bitFieldOutStream);
		//mBitFieldOutStream.writeEND();

		// Write the bytes to the serial interface.
		sendFrame(inPacket, byteArrayStream);

	}

	// --------------------------------------------------------------------------
	/**
	 *  Allow the subclass to setup the connection.
	 *  @return	The connection is setup or not
	 */
	protected abstract boolean doSetupConnection();

	// --------------------------------------------------------------------------
	/**
	 *  Perform any necessary startup in the subclass.
	 */
	protected abstract void doStartInterface();

	// --------------------------------------------------------------------------
	/**
	 *  Perform any necessary reset in the subclass.
	 */
	protected abstract void doResetInterface();

	// --------------------------------------------------------------------------
	/**
	 *  Perform any necessary reset in the subclass.
	 */
	protected abstract void setRTS();

	// --------------------------------------------------------------------------
	/**
	 *  Perform any necessary reset in the subclass.
	 */
	protected abstract void clrRTS();

	// --------------------------------------------------------------------------
	/**
	 *  Perform any necessary shutdown in the subclass.
	 */
	protected abstract void doStopInterface();

	// --------------------------------------------------------------------------
	/**
	 *  General method for reading in the subclass.
	 *  @return	The next byte from the stream.
	 */
	protected abstract byte readByte();

	// --------------------------------------------------------------------------
	/**
	 *  General method for reading in the subclass.
	 *  @return	All of the bytes that we can read from the stream at the moment.
	 */
	protected abstract int readBytes(byte[] inBuffer);

	// --------------------------------------------------------------------------
	/**
	 *  General method for writing to the stream.
	 *  @param inByte	The byte that we want to write to the stream.
	 */
	protected abstract void writeByte(byte inByte);

	// --------------------------------------------------------------------------
	/**
	 *  General method for writing to the stream.
	 *  @param inBytes	The bytes that we want to write to the stream;
	 */
	protected abstract void writeBytes(byte[] inBytes, int inLength);

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	private boolean setupConnection() {

		boolean result = doSetupConnection();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The next SLIP frame received from the serial interface.
	 */
	private byte[] receiveFrame() {
		byte[] frameBuffer = new byte[MAX_FRAME_BYTES];
		byte nextByte;
		int bytesReceived = 0;

		// Loop reading bytes until we put together  a whole packet.
		loop: while (shouldRun()) {
			nextByte = readByte();

			if (shouldRun()) {
				switch (nextByte) {

				// if it's an END character then we're done with the packet.
					case END:
						if (bytesReceived > 0) {
							break loop;
							//return result;
						} else {
							break;
						}

						/* 
						 * If it's the same code as an ESC character, wait
						 * and get another character and then figure out
						 * what to store in the packet based on that.
						 */
					case ESC:
						nextByte = readByte();

						/* If "c" is not one of these two, then we
						 * have a protocol violation.  The best bet
						 * seems to be to leave the byte alone and
						 * just stuff it into the packet
						 */
						switch (nextByte) {
							case ESC_END:
								nextByte = END;
								break;
							case ESC_ESC:
								nextByte = ESC;
								break;
							default:
								break;
						}

					default:
						// here we fall into the default handler and let it store the character for us.
						try {
							frameBuffer[bytesReceived++] = (byte) nextByte;
						} catch (ArrayIndexOutOfBoundsException e) {
							// Note the error and send the full frame up for handling.
							LOGGER.error("", e);
							bytesReceived = MAX_FRAME_BYTES;
							break loop;
						}
				}
			}
		}

		// Create a byte array that is exactly the right size.
		byte[] result = new byte[bytesReceived];
		System.arraycopy(frameBuffer, 0, result, 0, bytesReceived);
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Frame the packet bytes before putting them onto the serial link.
	 */

	private void sendFrame(IPacket inPacket, ByteArrayOutputStream inByteArrayStream) {

		byte[] packetBytes = inByteArrayStream.toByteArray();
		byte[] buffer = new byte[MAX_FRAME_BYTES];
		int bufPos = 0;
		int bytesToSend = Math.min(IPacket.MAX_PACKET_BYTES - 1, packetBytes.length);

		if (packetBytes.length > bytesToSend) {
			LOGGER.error("Packet contains more bytes than can fit on radio!");
		}

		for (int i = 0; i < bytesToSend; i++) {
			byte nextByte = packetBytes[i];

			if (nextByte == IGatewayInterface.ESC) {
				//					mSerialOutputStream.write(IGatewayInterface.ESC);
				//					mSerialOutputStream.write(IGatewayInterface.ESC_ESC);
				buffer[bufPos++] = IGatewayInterface.ESC;
				buffer[bufPos++] = IGatewayInterface.ESC_ESC;
			} else if (nextByte == IGatewayInterface.END) {
				//					mSerialOutputStream.write(IGatewayInterface.ESC);
				//					mSerialOutputStream.write(IGatewayInterface.ESC_END);
				buffer[bufPos++] = IGatewayInterface.ESC;
				buffer[bufPos++] = IGatewayInterface.ESC_END;
			} else {
				//					mSerialOutputStream.write(nextByte);
				buffer[bufPos++] = nextByte;
			}
		}
		//			mSerialOutputStream.write(IGatewayInterface.END);
		//			mSerialOutputStream.flush();
		buffer[bufPos++] = IGatewayInterface.END;
		buffer[bufPos + 1] = IGatewayInterface.END;

		clrRTS();
		writeBytes(buffer, bufPos);
		setRTS();

		LOGGER.info("Send packet:    " + inPacket.toString());
		if (LOGGER.isDebugEnabled()) {
			try {
				hexDumpArray(packetBytes);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inByteArray0
	 */
	protected void hexDumpArray(byte[] inByteArray) {
		String text = "";
		int pos = 0;
		for (pos = 0; pos < inByteArray.length; pos++) {
			if (pos % 16 == 0) {
				if (text.length() > 0) {
					LOGGER.debug(text);
				}
				text = String.format("\t%02x: ", pos);
			} else {
				text += " ";
			}
			text += String.format("%02x", inByteArray[pos]);
		}
		if (pos != 0) {
			LOGGER.debug(text);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inByteArray0
	 */
	protected void hexDumpArray(byte[] inByteArray, int inBytesToWrite) {
		String text = "";
		int pos = 0;
		for (pos = 0; pos < inBytesToWrite; pos++) {
			if (pos % 16 == 0) {
				if (text.length() > 0) {
					LOGGER.debug(text);
				}
				text = String.format("\t\t%02x: ", pos);
			} else {
				text += " ";
			}
			text += String.format("%02x", inByteArray[pos]);
		}
		if (pos != 0) {
			LOGGER.debug(text);
		}
	}
}
