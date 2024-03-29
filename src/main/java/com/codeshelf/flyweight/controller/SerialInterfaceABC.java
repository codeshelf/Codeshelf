/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: SerialInterfaceABC.java,v 1.5 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
//import java.text.DecimalFormat;
//import java.text.NumberFormat;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.command.CommandAssocABC;
import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.PacketFactory;

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

	public static final int			SERIAL_RESET_TIMEOUT_MS	= 500;
	public static final int			READ_SLEEP_MILLIS		= 10;
	public static final int			READ_RECOVER_MILLIS		= 5000;
	public static final int			WAIT_INTERFACE_MILLIS	= 5;
	public static final int			LQI_SIZE				= 1;

	private static final Logger		LOGGER					= LoggerFactory.getLogger(SerialInterfaceABC.class);

	private final Object			mLock					= new Object();

	@Setter
	private PacketCaptureListener	packetListener			= null;

	private boolean					mIsStarted;
	private boolean					mShouldRun				= true;
	private boolean					mIsStartingInterface;
	@SuppressWarnings("unused")
	private boolean					mPause					= false;
	private byte					mProtocolVersion		= IPacket.DEFAULT_PROTOCOL_VERSION;
	
	//private static final int							REPORT_INTERVAL_SECS	= 60 * 5;
	//private final ScheduledExecutorService				radioReportService = Executors.newScheduledThreadPool(1);
	//private radioTrafficCollector						mRadioStats;
	
	// Factories
	private PacketFactory			mPacketFactory			= new PacketFactory();

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
	 * @see com.codeshelf.flyweight.controller.IGatewayInterface#startInterface()
	 */
	public final void startInterface(byte inProtocolVersion) {
		mProtocolVersion = inProtocolVersion;
		
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
		
		//mRadioStats = new radioTrafficCollector(LOGGER);
		//mRadioStats.startCollecting();
		//this.radioReportService.scheduleAtFixedRate(mRadioStats, REPORT_INTERVAL_SECS, REPORT_INTERVAL_SECS, TimeUnit.SECONDS);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.IGatewayInterface#resetInterface()
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
				startInterface(mProtocolVersion);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.IGatewayInterface#stopInterface()
	 */
	public final void stopInterface() {
		mIsStarted = false;
		mShouldRun = false;
		doStopInterface();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.IGatewayInterface#isStarted()
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
	 *  radioController.getBroadcastAddress(), radioController.getZeroNetworkId()
	 */
	public final IPacket receivePacket(NetworkId inMyNetworkId, NetworkId inBroadcastNetworkId,
		NetworkId inZeroNetworkId) {
		
		boolean successfulRead = false;
		IPacket result = null;
		IPacket packet = null;

		// Get the next frame from the serial stream.
		//LOGGER.debug("Wait for frame.");
		byte[] nextFrameArray = this.receiveFrame();

		if (nextFrameArray.length == 0) {
			/*
			try {
				Thread.sleep(WAIT_INTERFACE_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
			*/
		} else {
			
			ByteArrayInputStream byteArray = new ByteArrayInputStream(nextFrameArray);
			BitFieldInputStream inputStream = new BitFieldInputStream(byteArray, true);

			// Receive the next packet.
			// TODO - needs to be based on the incoming packet version
			packet = mPacketFactory.getPacketForProtocol(mProtocolVersion);

			if (nextFrameArray.length > 0) {
				// Do not include LQI as packet data
				successfulRead = packet.fromStream(inputStream, nextFrameArray.length - LQI_SIZE);

				if(!successfulRead) {
					LOGGER.debug("Received packet of wrong version. Dropping.");
					return null;
				}
				
				//RadioStats.updateRcvdStats(nextFrameArray.length);
				// LQI of packet is in the last byte of the frame
				
				// TODO - Using this condition to test if we are using a KW2 radio
				// This is mostly incorrect, but works because KW2 radio only handles proto v1 atm.
				if (mProtocolVersion == IPacket.PACKET_VERSION_1) {
					byte lqi = nextFrameArray[nextFrameArray.length - LQI_SIZE];
					packet.setLQI(lqi);
				}
			}

			if ((packet.getNetworkId().equals(inMyNetworkId))
					|| (packet.getNetworkId().equals(inZeroNetworkId))
					|| (packet.getNetworkId().equals(inBroadcastNetworkId))) {
				result = packet;

				//				if (LOGGER.isInfoEnabled()) {
				//					LOGGER.info("Receive packet: " + result.toString());
				//				}
			}
			if ((LOGGER.isDebugEnabled() && (result != null))) {
				ICommand command = result.getCommand();
				// unknown guid is a possibility here. Therefore, somewhat unusual handling of rememberedGuid
				String rememberedGuid = ContextLogging.getNetGuid();	

				if (command instanceof CommandAssocABC) {
					CommandAssocABC assocCmd = (CommandAssocABC) command;
					rememberedGuid = ContextLogging.rememberThenSetNetGuid(assocCmd.getGUID());
				}
				try {
					boolean isMerelyNetManagementTraffic = false;
					ICommand aCommand = packet.getCommand();
					if (aCommand != null && aCommand.getCommandTypeEnum() == CommandGroupEnum.NETMGMT)
						isMerelyNetManagementTraffic = true;
					if (isMerelyNetManagementTraffic)
						LOGGER.trace("Receive packet: " + result.toString());
					else
						LOGGER.debug("Receive packet: " + result.toString());
					hexDumpArray(nextFrameArray);
				} catch (Exception e) {
					LOGGER.error("Failed to receive packet from network id {}", inMyNetworkId, e);
				} finally {
					ContextLogging.restoreNetGuid(rememberedGuid);
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
				if (!this.mShouldRun)
					return;

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
		//mRadioStats.updateSentStats(byteArrayStream.size());
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
							LOGGER.error("Serial framing error", e);
							bytesReceived = MAX_FRAME_BYTES;
							break loop;
						}
				}
			}
		}

		// Create a byte array that is exactly the right size.
		byte[] result = new byte[bytesReceived];
		System.arraycopy(frameBuffer, 0, result, 0, bytesReceived);

		if (this.packetListener != null) {
			this.packetListener.capture(result);
		}

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
		
		if (mProtocolVersion == IPacket.PACKET_VERSION_1) {
			// FIXME HUFFA - This conditional statement won't be accurate going forward.
			// I'm using it to test if we are using a KW2 gateway which currently only supports ver1
			buffer[bufPos++] = IGatewayInterface.END; // XXX HUFFA - used for KW2 Gateway buffer issues.
		}
		//		buffer[bufPos+1] = IGatewayInterface.END;

		//clrRTS();
		writeBytes(buffer, bufPos);
		//setRTS();

		boolean isMerelyNetManagementTraffic = false;
		ICommand aCommand = inPacket.getCommand();
		isMerelyNetManagementTraffic = aCommand != null && aCommand.getCommandTypeEnum() == CommandGroupEnum.NETMGMT;

		// The CommandAssocABC and CommandNetMagmtCheck commands have a mGuid field. Others do not; there is no convenient way to know the GUID.
		if (isMerelyNetManagementTraffic)
			LOGGER.trace("Send packet:    " + inPacket.toString());
		else
			LOGGER.debug("Send packet:    " + inPacket.toString());
		if (LOGGER.isDebugEnabled()) {
			try {
				hexDumpArray(packetBytes);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}

		if (this.packetListener != null) {
			this.packetListener.capture(packetBytes);
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
					LOGGER.info(text);
				}
				text = String.format("\t\t%02x: ", pos);
			} else {
				text += " ";
			}
			text += String.format("%02x", inByteArray[pos]);
		}
		if (pos != 0) {
			LOGGER.info(text);
		}
	}

	public void pause() {
		mPause = true;
	}

	public void resume() {
		mPause = false;
	}
	
//	private class radioTrafficCollector implements Runnable {
//		private long st;
//		private AtomicInteger totalPacketsSent, totalPacketsRcvd;
//		private AtomicInteger totalBytesSent, totalBytesRcvd;
//		
//		NumberFormat formatter = new DecimalFormat("#0.00");
//		
//		private Logger logger;
//		
//		public radioTrafficCollector(Logger inLogger) {
//			this.logger = inLogger;
//			
//			totalBytesSent = new AtomicInteger();
//			totalBytesRcvd  = new AtomicInteger();
//			totalPacketsSent = new AtomicInteger();
//			totalPacketsRcvd  = new AtomicInteger();
//		}
//		
//		@Override
//		public void run() {
//			logger.info(getSentReport());
//			logger.info(getReceivedReport());
//			resetCollection();
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *  Start collection
//		 */
//		public void startCollecting() {
//			resetCollection();
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 * Reset all collection counters
//		 */
//		public void resetCollection() {
//			totalPacketsSent.set(0);
//			totalPacketsRcvd.set(0);
//			totalBytesSent.set(0);
//			totalBytesRcvd.set(0);
//			st = System.currentTimeMillis();
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *  Update sent byte count
//		 */
//		public void updateSentStats(int inByteCount) {
//			totalPacketsSent.incrementAndGet();
//			totalBytesSent.addAndGet(inByteCount);
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *  Update received byte count
//		 */
//		public void updateRcvdStats(int inByteCount) {;
//			totalPacketsRcvd.incrementAndGet();
//			totalBytesRcvd.addAndGet(inByteCount);
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *	Get send data report
//		 *@return String
//		 */
//		private String getSentReport() {
//			double time = getMeasuredTimeSec();
//			double data_throughput = (totalBytesSent.get() * 8) / time;
//			double packet_throughput = totalPacketsSent.get() / time;
//			
//			return new String("Radio Sent Report - Total bytes sent: " + totalBytesSent.get() + " Data Througput: " + formatter.format(data_throughput) + " bps" +
//				" Total Packets sent: " + totalPacketsSent + " Packet Throughput: " + formatter.format(packet_throughput) + "/sec" + " Elapsed time: " + formatter.format(time) + " (secs)");
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *  Get received data report
//		 *  @return String
//		 */
//		private String getReceivedReport() {
//			double time = getMeasuredTimeSec();
//			double data_throughput = (totalBytesRcvd.get() * 8) / time;
//			double packet_throughput = totalPacketsRcvd.get() / time;
//			
//			return new String("Radio Rcvd Report - Total bytes recv: " + totalBytesRcvd.get() + " Data Througput: " + formatter.format(data_throughput) + " bps" +
//				" Total Packets recv: " + totalPacketsRcvd + " Packet Throughput: " + formatter.format(packet_throughput) + "/sec" + " Elapsed time: " + formatter.format(time) + " (secs)");
//		}
//		
//		// --------------------------------------------------------------------------
//		/**
//		 *  Get elapsed time in seconds
//		 *  @return double - elapsed time
//		 */
//		private double getMeasuredTimeSec() {
//			double time_seconds = (System.currentTimeMillis() - st) / 1000;
//			return time_seconds;
//		}
//	}

}
