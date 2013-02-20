/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControllerABC.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.AckStateEnum;
import com.gadgetworks.codeshelf.command.CommandAssocABC;
import com.gadgetworks.codeshelf.command.CommandAssocAck;
import com.gadgetworks.codeshelf.command.CommandAssocCheck;
import com.gadgetworks.codeshelf.command.CommandAssocReq;
import com.gadgetworks.codeshelf.command.CommandAssocResp;
import com.gadgetworks.codeshelf.command.CommandControlABC;
import com.gadgetworks.codeshelf.command.CommandCsABC;
import com.gadgetworks.codeshelf.command.CommandIdEnum;
import com.gadgetworks.codeshelf.command.CommandInfoABC;
import com.gadgetworks.codeshelf.command.CommandInfoQuery;
import com.gadgetworks.codeshelf.command.CommandInfoResponse;
import com.gadgetworks.codeshelf.command.CommandNetMgmtABC;
import com.gadgetworks.codeshelf.command.CommandNetMgmtCheck;
import com.gadgetworks.codeshelf.command.CommandNetMgmtSetup;
import com.gadgetworks.codeshelf.command.ICommand;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.query.IQuery;
import com.gadgetworks.codeshelf.query.IResponse;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public abstract class ControllerABC implements IController {

	// Right now, the devices are not sending back their network ID.
	public static final String									PRIVATE_MacAddr						= "00000000";
	public static final String									VIRTUAL_MacAddr						= "%%%%%%%%";

	public static final String									CONTROLLER_THREAD_NAME				= "Controller";

	// Artificially limit us to channels 0-3 to make testing faster.
	public static final byte									MAX_CHANNELS						= 16;
	public static final byte									NO_PREFERRED_CHANNEL				= (byte) 255;
	public static final String									NO_PREFERRED_CHANNEL_TEXT			= "None";

	private static final Log									LOGGER								= LogFactory.getLog(ControllerABC.class);

	private static final String									BACKGROUND_THREAD_NAME				= "Controller Background";
	private static final String									RECEIVER_THREAD_NAME				= "Packet Receiver";
	private static final String									SENDER_THREAD_NAME					= "Packet Sender";
	private static final String									INTERFACESTARTER_THREAD_NAME		= "Intferface Starter";

	private static final int									BACKGROUND_THREAD_PRIORITY			= Thread.NORM_PRIORITY - 1;
	private static final int									RECEIVER_THREAD_PRIORITY			= Thread.MAX_PRIORITY;
	private static final int									SENDER_THREAD_PRIORITY				= Thread.MAX_PRIORITY;
	private static final int									INTERFACESTARTER_THREAD_PRIORITY	= Thread.NORM_PRIORITY;

	private static final long									CTRL_START_DELAY_MILLIS				= 200;
	private static final long									CTRL_NOCMD_DELAY_MILLIS				= 5;
	private static final long									NETCHECK_DELAY_MILLIS				= 250;

	private static final long									QUERY_TIMEOUT_MILLIS				= 500;
	private static final int									QUERY_SEND_RETRY_COUNT				= 3;
	private static final long									ACK_TIMEOUT_MILLIS					= 50;
	private static final int									ACK_SEND_RETRY_COUNT				= 5;
	private static final long									EVENT_SLEEP_MILLIS					= 50;
	private static final long									INTERFACE_CHECK_MILLIS				= 5 * 1000;
	private static final long									CONTROLLER_SLEEP_MILLIS				= 10;
	private static final int									MAX_PACKET_QUEUE_SIZE				= 100;
	private static final int									MAX_CHANNEL_VALUE					= 255;
	private static final short									HIGH_WATER							= 14;
	private static final int									MAX_NETWORK_TEST_NUM				= 64;

	private Facility											mFacility;
	protected IWirelessDeviceDao								mWirelessDeviceDao;
	private Boolean												mShouldRun							= true;
	private List<IWirelessInterface>							mInterfaceList;
	private NetAddress											mServerAddress;
	private NetAddress											mBroadcastAddress;
	private NetworkId											mBroadcastNetworkId;
	private Map<Long, CommandInfoQuery>							mQueryCmdMap;
	private NetworkId											mNetworkId;
	private ICommand[]											mCommandSendCircularBuffer;
	private int													mCommandSendProducerPos;
	private int													mCommandSendConsumerPos;
	private List<IControllerEventListener>						mEventListeners;
	private long												mLastIntfCheckMillis;
	private byte												mNextCommandID;
	private volatile Map<NetAddress, BlockingQueue<ICommand>>	mPendingAcksMap;

	private ChannelInfo[]										mChannelInfo;
	private boolean												mChannelSelected;
	private byte												mPreferredChannel;
	private byte												mRadioChannel;

	private Thread												mControllerThread;
	private Thread												mBackgroundThread;
	private Thread												mPacketProcessorThread;
	private Thread												mPacketSenderThread;

	private SourceDataLine										mSourceDataLine;
	private boolean												mPCSoundEnabled;

	// --------------------------------------------------------------------------
	/**
	 *  @param inSessionManager   The session manager for this controller.
	 */
	@Inject
	public ControllerABC(final List<IWirelessInterface> inInterfaceList, final Facility inFacility, final IWirelessDeviceDao inWirelessDeviceDao) {

		mInterfaceList = inInterfaceList;
		mFacility = inFacility;
		mWirelessDeviceDao = inWirelessDeviceDao;

		mServerAddress = IController.GATEWAY_ADDRESS;
		mBroadcastAddress = IController.BROADCAST_ADDRESS;
		mBroadcastNetworkId = IController.BROADCAST_NETWORK_ID;
		mQueryCmdMap = new HashMap<Long, CommandInfoQuery>();
		mCommandSendCircularBuffer = new ICommand[MAX_PACKET_QUEUE_SIZE];
		mCommandSendConsumerPos = 0;
		mCommandSendProducerPos = 0;
		mEventListeners = new ArrayList<IControllerEventListener>();

		mNetworkId = IController.DEFAULT_NETWORK_ID;

		mChannelSelected = false;
		mChannelInfo = new ChannelInfo[MAX_CHANNELS];
		mRadioChannel = 0;

		mPendingAcksMap = new HashMap<NetAddress, BlockingQueue<ICommand>>();
	}

	// --------------------------------------------------------------------------
	/**
	 *  Perform any startup operations for the sub-class of the controller.
	 */
	protected abstract void doStartController();

	//--------------------------------------------------------------------------
	/**
	 *  Process realtime/background events for the sub-class of the controller.
	 *  @return	returns true if the sub-class of the controller handled an event.
	 */
	protected abstract boolean doBackgroundProcessing();

	//--------------------------------------------------------------------------
	/**
	 *  The  sub-class of the controller handle the special case where a device just became active in the network.
	 *  @param inSession	The device that just became active.
	 */
	protected abstract void doNetworkDeviceBecameActive(INetworkDevice inNetworkDevice);

	// --------------------------------------------------------------------------
	/**
	 *  The sub-class of the controller will process queries.
	 *  @param inQuery
	 */
	protected abstract void doProcessQuery(IQuery inQuery);

	// --------------------------------------------------------------------------
	/**
	 *  The sub-class of the controller will process responses.
	 *  @param inResponseCmd	The response command just received.
	 *  @param inQueryCmd	The original query command the the response is for.
	 */
	protected abstract void doProcessResponse(IResponse inResponse, IQuery inQuery);

	// --------------------------------------------------------------------------
	/**
	 *  The control command gets processed by the subclass since this is where the application-specific knowledge resides.
	 *  @param inCommand
	 *  @param inSrcAddr
	 */
	protected abstract void doProcessControlCmd(CommandControlABC inCommand, INetworkDevice inNetworkDevice);

	// --------------------------------------------------------------------------
	/**
	 * The CodeShelf command gets processed by the subclass since this is where the application-specific knowledge resides.
	 * @param inCommand
	 * @param inNetworkDevice
	 */
	protected abstract void doProcessCodeShelfCmd(CommandCsABC inCommand);

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#startController(byte)
	 */
	public final void startController() {

		mPreferredChannel = getPreferredChannel();

		LOGGER.info("Starting controller");
		mControllerThread = new Thread(this, CONTROLLER_THREAD_NAME);
		mControllerThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	public final void stopController() {

		// Stop all of the interfaces.
		for (IWirelessInterface gwInterface : mInterfaceList) {
			gwInterface.stopInterface();
		}

		// Signal that we want to stop.
		mShouldRun = false;

		boolean aThreadIsRunning = false;
		while (aThreadIsRunning) {
			aThreadIsRunning = (mBackgroundThread.getState() != Thread.State.TERMINATED);
			//			aThreadIsRunning |= (mPacketReceiverThread.getState() != Thread.State.TERMINATED);
			aThreadIsRunning |= (mPacketProcessorThread.getState() != Thread.State.TERMINATED);
			aThreadIsRunning |= (mPacketSenderThread.getState() != Thread.State.TERMINATED);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}

	}

	/* --------------------------------------------------------------------------
	 * Get the preferred channel from the preferences store.
	 */
	public final byte getPreferredChannel() {
		byte result = 0;
		PersistentProperty preferredChannelProp = PersistentProperty.DAO.findByDomainId(mFacility.getParent(), PersistentProperty.FORCE_CHANNEL);
		if (preferredChannelProp != null) {
			if (ControllerABC.NO_PREFERRED_CHANNEL_TEXT.equals(preferredChannelProp.getCurrentValueAsStr())) {
				result = ControllerABC.NO_PREFERRED_CHANNEL;
			} else {
				result = (byte) preferredChannelProp.getCurrentValueAsInt();
			}
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getInterfaces()
	 */
	public final List<IWirelessInterface> getInterfaces() {
		return mInterfaceList;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final INetworkDevice getNetworkDevice(NetAddress inAddress) {
		return mWirelessDeviceDao.getNetworkDevice(inAddress);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final List<INetworkDevice> getNetworkDevices() {
		return mWirelessDeviceDao.getNetworkDevices();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public final void run() {

		byte testNum = 0;

		// Kick off the background event processing.
		mBackgroundThread = new Thread(new Runnable() {
			public void run() {
				processEvents();
			}
		}, BACKGROUND_THREAD_NAME);
		mBackgroundThread.setPriority(BACKGROUND_THREAD_PRIORITY);
		mBackgroundThread.setDaemon(true);
		mBackgroundThread.start();

		// Start all of the serial interfaces.
		// They start on a thread since this op won't complete if no dongle is attached.
		Thread interfaceStarterThread = new Thread(new Runnable() {
			public void run() {
				boolean allStarted;
				do {
					allStarted = true;
					for (IWirelessInterface gwInterface : mInterfaceList) {
						gwInterface.startInterface();
						if (!gwInterface.isStarted()) {
							allStarted = false;
						}
					}
				} while (!allStarted);
			}
		}, INTERFACESTARTER_THREAD_NAME);
		interfaceStarterThread.setPriority(INTERFACESTARTER_THREAD_PRIORITY);
		interfaceStarterThread.setDaemon(true);
		interfaceStarterThread.start();

		startCommandReceivers();

		// Kick off the packer reader
		//		mPacketProcessorThread = new Thread(new Runnable() {
		//			public void run() {
		//				processPackets();
		//			}
		//		}, PROCESSOR_THREAD_NAME);
		//		mPacketProcessorThread.setPriority(Thread.MAX_PRIORITY);
		//		mPacketProcessorThread.start();

		// Kick off the command sender thread.
		mPacketSenderThread = new Thread(new Runnable() {
			public void run() {
				mPCSoundEnabled = false;
				if (mPCSoundEnabled) {
					try {
						AudioFormat lineFormst = new AudioFormat(8000, 16, 1, true, false);

						DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, lineFormst);
						mSourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
						mSourceDataLine.open(lineFormst);
						mSourceDataLine.start();
					} catch (LineUnavailableException e) {
						LOGGER.error("", e);
					}
				}

				sendCommands();
			}
		}, SENDER_THREAD_NAME);
		mPacketSenderThread.setPriority(SENDER_THREAD_PRIORITY);
		mPacketSenderThread.start();

		// Wait until the interfaces start.
		boolean allStarted;
		do {
			allStarted = true;
			for (IWirelessInterface gwInterface : mInterfaceList) {
				if (!gwInterface.isStarted()) {
					allStarted = false;
				}
			}
			if (!allStarted) {
				try {
					Thread.sleep(INTERFACE_CHECK_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				LOGGER.info("Interfaces not started");
			}

		} while (!allStarted && mShouldRun);

		// Now let the sub-class setup for startup.
		doStartController();

		selectChannel();

		mLastIntfCheckMillis = System.currentTimeMillis();
		while (mShouldRun) {
			try {
				// Check to see if we should perform an interface check.
				// (Only perform this interface check on the radio interface.)
				if (mLastIntfCheckMillis + INTERFACE_CHECK_MILLIS < System.currentTimeMillis()) {
					for (IWirelessInterface gwInterface : mInterfaceList) {
						if (!gwInterface.checkInterfaceOk()) {
							gwInterface.resetInterface();
						}
					}

					mLastIntfCheckMillis = System.currentTimeMillis();
				}
				Thread.sleep(CONTROLLER_SLEEP_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getChannel()
	 */
	public final byte getRadioChannel() {
		return mRadioChannel;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#setChannel(byte)
	 */
	public final void setRadioChannel(byte inChannel) {
		if ((inChannel < 0) || (inChannel > MAX_CHANNELS)) {
			LOGGER.error("Could not set channel - out of range!");
		} else {
			mChannelSelected = true;
			mRadioChannel = inChannel;
			CommandNetMgmtSetup netSetupCmd = new CommandNetMgmtSetup(mNetworkId, mRadioChannel);
			sendCommandNow(netSetupCmd, mBroadcastAddress, false);
			LOGGER.info("Channel " + inChannel);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Process the events that occur in the controller.
	 */
	private void processEvents() {

		//	 The controller should process events continuously until the application wants to quit/exit.
		while (mShouldRun) {

			try {
				// Check to see if we have any timed-out queries pending.
				synchronized (mQueryCmdMap) {
					if (mQueryCmdMap.size() > 0) {
						// Figure out which queries to remove from the list.
						// (We can't remove while iterating.
						List<IQuery> removeList = new ArrayList<IQuery>();
						for (CommandInfoQuery queryCmd : mQueryCmdMap.values()) {
							IQuery query = queryCmd.getQuery();

							// The query timed-out, so resend it.
							if (System.currentTimeMillis() > (query.getQueryTimeMillis() + (query.getSendCount() * QUERY_TIMEOUT_MILLIS))) {

								removeList.add(query);

								INetworkDevice networkDevice = query.getQueryNetworkDevice();

								if ((networkDevice == null) || (networkDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.LOST)) || (query.getSendCount() > QUERY_SEND_RETRY_COUNT)) {
									// The session is invalid, so the query is invalid as well.

								} else {
									LOGGER.info("*** Resend query (" + queryCmd.getQuery().getSendCount() + ") " + Long.toHexString(queryCmd.getQuery().getQueryID()));
									sendCommandNow(queryCmd, networkDevice.getNetAddress(), false);
									query.incrementSendCount();
									//query.setQueryTimeMillis(System.currentTimeMillis());
								}
							}
						}
						// Now remove the packets that we resent.
						//						for (IQuery query : removeList) {
						//							CommandInfoQuery delQueryCmd = mQueryCmdMap.remove(query.getQueryID());
						//							if (delQueryCmd != null) {
						//								LOGGER.info("*** Deleted query id: " + Long.toHexString(delQueryCmd.getQuery().getQueryID()));
						//							}
						//						}
					}
				}

				// Check if there are any pending ACK packets that need resending.
				// Also consider the case where there is more than one packet destined for a remote.
				// (Only re-send one packet per network device, so that packets arrive in order.)
				//				synchronized (mPendingAcksMap) {
				if (mPendingAcksMap.size() > 0) {
					for (BlockingQueue<ICommand> queue : mPendingAcksMap.values()) {
						// Look at the next packet in the queue.
						ICommand command = queue.peek();
						if (command != null) {
							// If we've timed out waiting for an ACK then resend the command.
							if (System.currentTimeMillis() > (command.getSentTimeMillis() + ACK_TIMEOUT_MILLIS)) {
								if (command.getSendCount() < ACK_SEND_RETRY_COUNT) {
									//sendCommand(command, packet.getNetworkType(), packet.getDstAddr());
									putCommandInSendQueue(command);
									command.incrementSendCount();
								} else {
									// If we've exceeded the retry time then remove the packet.
									// We should probably mark the device lost and clear the queue.
									command.setAckState(AckStateEnum.NO_RESPONSE);
									LOGGER.info("Packet acked NO_RESPONSE: " + command.toString());
									queue.remove();
								}
							}
						}
					}
				}
				//				}

				// Give the sub-classes a chance to do some background processing.
				if (mShouldRun)
					mShouldRun = this.doBackgroundProcessing();

				// Sleep a little to save CPU
				try {
					Thread.sleep(EVENT_SLEEP_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			} catch (RuntimeException e) {
				LOGGER.error("", e);
			}

		}

		//this.stopController();

		//System.exit(0);
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private final void selectChannel() {

		//		if (mShouldRun) {
		//
		//			// Pause for a few seconds to let the serial interface come up.
		//			try {
		//				Thread.sleep(CTRL_START_DELAY_MILLIS);
		//			} catch (InterruptedException e) {
		//				LOGGER.error("", e);
		//			}
		//
		//			// If the user has a preferred channel then use it.
		//			if (mPreferredChannel != NO_PREFERRED_CHANNEL) {
		//				setRadioChannel(mPreferredChannel);
		//			} else {
		//
		//				/* The process for selecting a channel goes like this: 
		//				 * 
		//				 * 1. Choose a channel.
		//				 * 2. Send a net-check command on that channel.
		//				 * 3. Wait for responses.  (One response will come directly from the gateway (dongle) with a MacAddr = '00000000'
		//				 * 4. Repeat until all channels have been checked.
		//				 * 5. Choose the best channel.
		//				 * 6. Send a net-setup command directly to the dongle to tell it the channel ID.
		//				 */
		//
		//				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
		//
		//					// First setup the ChannelInfo structure for the channel.
		//					if (mChannelInfo[channel] == null) {
		//						mChannelInfo[channel] = new ChannelInfo();
		//						mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
		//					} else {
		//						mChannelInfo[channel].setControllerCount(0);
		//						mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
		//					}
		//
		//					CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_REQ,
		//						mBroadcastNetworkId,
		//						PRIVATE_MacAddr);
		//					sendCommandNow(netCheck, mBroadcastAddress, false);
		//
		//					// Wait NETCHECK delay millis before sending the next net-check.
		//					try {
		//						Thread.sleep(NETCHECK_DELAY_MILLIS);
		//					} catch (InterruptedException e) {
		//						LOGGER.error("", e);
		//					}
		//				}
		//
		//				// At this point the ChannelInfo structures will contain all of the information we need to make a channel choice.
		//				mPreferredChannel = 0;
		//				// First find the channel with the lowest number of controllers.
		//				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
		//					if (mPreferredChannel != channel) {
		//						// The most important test is the number of controllers already on a channel.
		//						if (mChannelInfo[channel].getControllerCount() < mChannelInfo[mPreferredChannel].getControllerCount()) {
		//							mPreferredChannel = channel;
		//						}
		//					}
		//				}
		//
		//				// By the above we will have picked the first channel with the lowest number of controllers.
		//				// There may be more than one channel with this same number of controllers.
		//				// So now search those equal channels for the one with the lowest energy.
		//				int lowestControllerCount = mChannelInfo[mPreferredChannel].getControllerCount();
		//				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
		//					if ((mPreferredChannel != channel) && (mChannelInfo[channel].getControllerCount() == lowestControllerCount)) {
		//						if (mChannelInfo[channel].getChannelEnergy() < mChannelInfo[mPreferredChannel].getChannelEnergy()) {
		//							mPreferredChannel = channel;
		//						}
		//					}
		//				}
		//				setRadioChannel(mPreferredChannel);
		//			}
		//		}
	}

	// --------------------------------------------------------------------------
	/**
	 * When the controller gets a new command it arrives here.
	 * 
	 *  @param inCommand   The command just received.
	 *  @param inSrcAddr   The address is was received from.
	 */
	public final void receiveCommand(final ICommand inCommand, final NetAddress inSrcAddr) {

		INetworkDevice foundDevice = getNetworkDevice(inSrcAddr);
		if (foundDevice != null) {
			foundDevice.setLastContactTime(System.currentTimeMillis());
		}

		if (inCommand != null) {

			switch (inCommand.getCommandGroupEnum()) {

				case NETMGMT:
					processNetworkMgmtCmd((CommandNetMgmtABC) inCommand, inSrcAddr);
					break;

				case ASSOC:
					if (mChannelSelected) {
						processAssocCmd((CommandAssocABC) inCommand, inSrcAddr);
						if (foundDevice != null) {
							mWirelessDeviceDao.deviceUpdated(foundDevice, false);
						}
					}
					break;

				case INFO:
					if (mChannelSelected) {
						if (foundDevice != null) {
							processInfoCmd((CommandInfoABC) inCommand, foundDevice);
							mWirelessDeviceDao.deviceUpdated(foundDevice, false);
						} else {
							LOGGER.error("Receive INFO command: device not found");
						}
					}
					break;

				case CONTROL:
					if (mChannelSelected) {
						if (foundDevice != null) {
							doProcessControlCmd((CommandControlABC) inCommand, foundDevice);
						} else {
							LOGGER.error("Receive CONTROL command: device not found");
						}
					}
					break;

				case CODESHELF:
					LOGGER.info("Rcvd cmd: " + inCommand.toString());
					doProcessCodeShelfCmd((CommandCsABC) inCommand);
					break;

				default:
					break;
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommand(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public final void sendCommandNow(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommandTimed(inCommand, mNetworkId, inDstAddr, 0, inAckRequested);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommand(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public final void sendCommandTimed(ICommand inCommand, NetAddress inDstAddr, long inSendTimeNanos, boolean inAckRequested) {
		sendCommandTimed(inCommand, mNetworkId, inDstAddr, inSendTimeNanos, inAckRequested);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommand(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.command.NetworkId, com.gadgetworks.codeshelf.command.NetAddress, long)
	 */
	public final void sendCommandTimed(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, long inSendTimeNanos, boolean inAckRequested) {

		inCommand.setNetworkId(inNetworkId);
		inCommand.setSrcAddr(mServerAddress);
		inCommand.setDstAddr(inDstAddr);
		inCommand.setScheduledTimeNanos(inSendTimeNanos);

		/*
		 * Certain commands can request the remote to ACK (to guarantee that the command arrived).
		 * Most commands will not request an ACK, so normally commands will just get sent right here.  
		 * 
		 * If a command requires an ACK then we perform the following steps:
		 * 
		 * - Check that the command address is something other than a broadcast network ID or network address.  
		 * 		(We don't support broadcast ACK.)
		 * - If a command queue does not exist for the destination then:
		 * 		1. Create a command queue for the destination.
		 * 		2. Put the command in the queue.
		 * 		3. Send the command.
		 * - If a command queue does exist for the destination then just put the command in it.
		 */

		if ((inAckRequested) && ((!inNetworkId.equals(IController.BROADCAST_NETWORK_ID)) && (!inDstAddr.equals(IController.BROADCAST_ADDRESS)))) {
			//			synchronized (mPendingAcksMap) {

			// Set the command ID.
			// To the network protocol a command ID of zero means we don't want a command ACK.
			mNextCommandID++;
			if (mNextCommandID == 0) {
				mNextCommandID = 1;
			}
			inCommand.setAckId(mNextCommandID);
			inCommand.setAckState(AckStateEnum.PENDING);

			// Add the command to the pending ACKs map, and increment the command ID counter.
			BlockingQueue<ICommand> queue = mPendingAcksMap.get(inDstAddr);
			if (queue == null) {
				queue = new ArrayBlockingQueue<ICommand>(10);
				mPendingAcksMap.put(inDstAddr, queue);
			}
			if (inSendTimeNanos == 0) {
				sendCommand(inCommand);
			} else {
				putCommandInSendQueue(inCommand);
				// Yield, so the sending thread will empty the queue.
				Thread.yield();
			}
			queue.add(inCommand);
			//			}
		} else {
			if (inSendTimeNanos == 0) {
				sendCommand(inCommand);
			} else {
				putCommandInSendQueue(inCommand);
				// Yield, so the sending thread will empty the queue.
				Thread.yield();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void putCommandInSendQueue(ICommand inCommand) {
		while (mShouldRun && (mCommandSendCircularBuffer[mCommandSendProducerPos] != null)) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		if (mCommandSendCircularBuffer[mCommandSendProducerPos] == null) {
			mCommandSendCircularBuffer[mCommandSendProducerPos++] = inCommand;
			if (mCommandSendProducerPos >= MAX_PACKET_QUEUE_SIZE)
				mCommandSendProducerPos = 0;
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IController#addControllerListener(com.gadgetworks.controller.IControllerListener)
	 */
	public final void addControllerEventListener(final IControllerEventListener inControllerEventListener) {
		mEventListeners.add(inControllerEventListener);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Return a list of the current IControllerEventListeners.
	 *  @return	The listeners
	 */
	protected final List<IControllerEventListener> getControllerEventListeners() {
		return mEventListeners;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand    The wake command the we want to process.  (The one just received.)
	 */
	private void processNetworkMgmtCmd(CommandNetMgmtABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of network management sub-command we have.

		switch (inCommand.getCommandIdEnum()) {
			case NET_SETUP:
				processNetworkSetupCommand((CommandNetMgmtSetup) inCommand, inSrcAddr);
				break;

			case NET_CHECK:
				processNetworkCheckCommand((CommandNetMgmtCheck) inCommand, inSrcAddr);
				break;

			default:
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *  When the gateway (dongle) starts/resets it will attempt to ask the controller
	 *  what channel it should be using.  This is done, because the gateway (dongle) is
	 *  not really capable of writing semi-permanent config parameters to NVRAM.
	 *  (It's a complication of the MCU's Flash RAM functionality and the limited 
	 *  number of times you can write to Flash over the life of the device.
	 *  @param inCommand
	 */
	private void processNetworkSetupCommand(CommandNetMgmtSetup inCommand, NetAddress inSrcAddr) {
		/* The only time that we will ever see a net-setup is when the gateway (dongle)
		 * sends it to the controller in order to learn the channel it should use.
		 * The gateway (dongle) may crash/reset, but it needs to be able to come back
		 * up fast without seriously disrupting the network.  The best way to do this
		 * is to allow the controller to maintain the state info about the network
		 * that is running.
		 */

		CommandNetMgmtSetup netSetupCmd = new CommandNetMgmtSetup(mNetworkId, mRadioChannel);
		sendCommandNow(netSetupCmd, mBroadcastAddress, false);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void processNetworkCheckCommand(CommandNetMgmtCheck inCommand, NetAddress inSrcAddr) {

		NetworkId networkId = inCommand.getNetworkId();
		if (inCommand.getNetCheckType() == CommandNetMgmtCheck.NETCHECK_REQ) {
			// This is a net-check request.

			// If it's an all-network broadcast, or a request to our network then respond.
			boolean shouldRespond = false;
			String responseMacAddr = "";
			if (inCommand.getNetCheckType() == CommandNetMgmtCheck.NETCHECK_RESP) {
				// For a broadcast request we respond with the private MacAddr.  This will cause the gateway (dongle)
				// to insert its own MacAddr before transmitting it to the air.
				shouldRespond = true;
				responseMacAddr = PRIVATE_MacAddr;
			} else if (networkId.equals(mNetworkId)) {
				// For a network-specific request we respond with the MacAddr of the requester.
				shouldRespond = true;
				responseMacAddr = inCommand.getMacAddr();
			}

			if (shouldRespond) {
				// If this is a network check for us then response back to the sender.
				// Send a network check response command back to the sender.
				CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_RESP, inCommand.getNetworkId(), responseMacAddr);
				this.sendCommandNow(netCheck, mBroadcastAddress, false);
			}
		} else {
			// This is a net-check response.
			//			if (networkId.equals(IController.BROADCAST_NETWORK_ID)) {
			//
			//				// If this is a all-network net-check broadcast response then book keep the values.
			//
			//				// Find the ChannelInfo instance for this channel.
			//				byte channel = inCommand.getChannel();
			//
			//				if (inCommand.getMacAddr().equals(PRIVATE_MacAddr)) {
			//					// This came from the gateway (dongle) directly.
			//					// The gateway (dongle) will have inserted an energy detect value for the channel.
			//					mChannelInfo[channel].setChannelEnergy(inCommand.getChannelEnergy().getValue());
			//				} else {
			//					// This came from another controller on the same channel, so increment the number of controllers on the channel.
			//					mChannelInfo[channel].incrementControllerCount();
			//				}
			//			} else {
			//				// The controller never receives network-specific net-check responses.
			//			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPacket
	 */
	private void processAckCommand(ICommand inCommand) {
		BlockingQueue<ICommand> queue = mPendingAcksMap.get(inCommand.getSrcAddr());
		if (queue != null) {
			for (ICommand command : queue) {
				if (command != null) {
					if (command.getAckId() == inCommand.getAckId()) {
						command.setAckData(inCommand.getAckData());
						command.setAckState(AckStateEnum.SUCCEEDED);
						queue.remove(command);
						LOGGER.info("Packet acked SUCCEEDED: " + command.toString());
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Handle the request of a remote device that wants to associate to our controller.
	 *  @param inCommand    The association command that we want to process.  (The one just received.)
	 */
	private void processAssocCmd(CommandAssocABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of associate sub-command we have.
		switch (inCommand.getCommandIdEnum()) {
			case ASSOC_REQ:
				processAssocReqCommand((CommandAssocReq) inCommand, inSrcAddr);
				break;

			case ASSOC_RESP:
				processAssocRespCommand((CommandAssocResp) inCommand, inSrcAddr);
				break;

			case ASSOC_CHECK:
				processAssocCheckCommand((CommandAssocCheck) inCommand, inSrcAddr);
				break;

			case ASSOC_ACK:
				processAssocAckCommand((CommandAssocAck) inCommand, inSrcAddr);
				break;

			default:
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void processAssocReqCommand(CommandAssocReq inCommand, NetAddress inSrcAddr) {

		// First get the unique ID from the command.
		NetMacAddress macAddr = inCommand.getMacAddr();

		LOGGER.info("AssocReq rcvd: " + inCommand.toString());

		// This is a device running on the radio network.

		// First let's make sure that this is a request from an actor that we are managing.
		// Indicate to listeners that there is a new actor.
		boolean canAssociate = false;
		for (IControllerEventListener listener : getControllerEventListeners()) {
			if (listener.canNetworkDeviceAssociate(macAddr)) {
				canAssociate = true;
			}
		}

		if (canAssociate) {

			INetworkDevice foundDevice = mWirelessDeviceDao.findNetworkDeviceByMacAddr(macAddr);

			if (foundDevice != null) {
				foundDevice.setNetworkDeviceState(NetworkDeviceStateEnum.SETUP);
				mWirelessDeviceDao.deviceUpdated(foundDevice, true);

				LOGGER.info("----------------------------------------------------");
				LOGGER.info("Device associated: " + foundDevice.toString());
				if ((inCommand.getSystemStatus() & 0x02) > 0) {
					LOGGER.info(" Status: LVD");
				}
				if ((inCommand.getSystemStatus() & 0x04) > 0) {
					LOGGER.info(" Status: ICG");
				}
				if ((inCommand.getSystemStatus() & 0x10) > 0) {
					LOGGER.info(" Status: ILOP");
				}
				if ((inCommand.getSystemStatus() & 0x20) > 0) {
					LOGGER.info(" Status: COP");
				}
				if ((inCommand.getSystemStatus() & 0x40) > 0) {
					LOGGER.info(" Status: PIN");
				}
				if ((inCommand.getSystemStatus() & 0x80) > 0) {
					LOGGER.info(" Status: POR");
				}
				LOGGER.info("----------------------------------------------------");

				// If the device has no address then assign one.
				if ((foundDevice.getNetAddress() == null) || (foundDevice.getNetAddress().equals(mServerAddress))) {
					//					// Put all of the candidate addresses into a map.
					//					int totalAddresses = (int) Math.pow(2, IPacket.ADDRESS_BITS) - 2;
					//					Map<Byte, NetAddress> addrMap = new HashMap<Byte, NetAddress>();
					//					for (short addr = 1; addr < totalAddresses; addr++) {
					//						addrMap.put((byte) addr, new NetAddress((byte) addr));
					//					}
					//					// Remove the candidates already in use.
					//					for (INetworkDevice networkDevice : this.getNetworkDevices()) {
					//						if (networkDevice.getNetAddress() != null) {
					//							addrMap.remove(networkDevice.getNetAddress().getValue());
					//						}
					//					}
					//					// Assign the first address left in the map.
					//					if (!addrMap.values().isEmpty()) {
					//						NetAddress newAddress = (NetAddress) addrMap.values().toArray()[0];
					//						foundDevice.setNetAddress(newAddress);
					//						mWirelessDeviceDao.deviceUpdated(foundDevice, true);
					//					}
				}

				// Create and send an assign command to the remote that just woke up.
				CommandAssocResp assignCmd = new CommandAssocResp(macAddr, mNetworkId, foundDevice.getNetAddress());
				this.sendCommandTimed(assignCmd, mBroadcastNetworkId, mBroadcastAddress, 0, false);
				foundDevice.setNetworkDeviceState(NetworkDeviceStateEnum.ASSIGN_SENT);
				mWirelessDeviceDao.deviceUpdated(foundDevice, false);

				// We should wait a bit for the remote to prepare to accept commands.
				try {
					Thread.sleep(CTRL_START_DELAY_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}

				this.doNetworkDeviceBecameActive(foundDevice);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void processAssocRespCommand(CommandAssocResp inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void processAssocCheckCommand(CommandAssocCheck inCommand, NetAddress inSrcAddr) {

		// First get the unique ID from the command.
		NetMacAddress macAddr = inCommand.getMacAddr();

		INetworkDevice foundDevice = mWirelessDeviceDao.findNetworkDeviceByMacAddr(macAddr);

		if (foundDevice != null) {
			CommandAssocAck ackCmd;
			LOGGER.info("Assoc check: " + foundDevice.toString());

			short level = inCommand.getBatteryLevel();
			if (foundDevice.getLastBatteryLevel() != level) {
				foundDevice.setLastBatteryLevel(level);
				mWirelessDeviceDao.deviceUpdated(foundDevice, true);
			}

			boolean status = CommandAssocAck.IS_ASSOCIATED;

			// If the found device isn't in the STARTED state then it's not associated with us.
			if (!(foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.STARTED))) {
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
				LOGGER.info("AssocCheck - NOT ASSOC: state was: " + foundDevice.getNetworkDeviceState());
			}

			// If the found device has the wrong MacAddr then we have the wrong device.
			// (This could be two matching network IDs on the same channel.  
			// This could be a serious flaw in the network protocol.)
			if (!foundDevice.getMacAddress().equals(macAddr)) {
				LOGGER.info("AssocCheck - NOT ASSOC: MacAddr mismatch: " + foundDevice.getMacAddress() + " and " + macAddr);
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
			}

			// Only send the response to devices whose state is INVALID, STARTED or LOST.
			//			if ((foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.INVALID))
			//					|| (foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.STARTED))
			//					|| (foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.LOST))) {
			// Create and send an ack command to the remote that we think is in the running state.
			ackCmd = new CommandAssocAck(macAddr, status);

			// Send the command.
			sendCommandNow(ackCmd, inSrcAddr, false);
			//			}

			// We should wait a bit for the remote to prepare to accept commands.
			try {
				Thread.sleep(CTRL_START_DELAY_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}

		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 */
	private void processAssocAckCommand(CommandAssocAck inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	// --------------------------------------------------------------------------
	/**
	 *	Send the first query command to the device.
	 *
	 * @param inQuery  The query to send.
	 * @param inDestAddr   The address to send the query to.
	 * @param inEndpoint   The endpoint to send the query to.
	 */
	protected final void sendQuery(IQuery inQuery, INetworkDevice inNetworkDevice) {

		// Set the session for this query.
		inQuery.setQueryNetworkDevice(inNetworkDevice);

		CommandInfoQuery queryCmd = doCreateQueryCommand(inQuery);
		sendCommandNow(queryCmd, inNetworkDevice.getNetAddress(), false);

		//  Add this query command to the map of outstanding queries.
		synchronized (mQueryCmdMap) {
			inQuery.incrementSendCount();
			mQueryCmdMap.put(inQuery.getQueryID(), queryCmd);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Send the first query command to the device.
	 *
	 * @param inQuery  The query to send.
	 * @param inDestAddr   The address to send the query to.
	 * @param inEndpoint   The endpoint to send the query to.
	 */
	protected final void clearQueriesForDevice(INetworkDevice inNetworkDevice) {

		//  Add this query command to the map of outstanding queries.
		synchronized (mQueryCmdMap) {
			List<CommandInfoQuery> deleteList = new ArrayList<CommandInfoQuery>();
			for (Map.Entry<Long, CommandInfoQuery> entry : mQueryCmdMap.entrySet()) {
				CommandInfoQuery query = entry.getValue();
				if (query.getQuery().getQueryNetworkDevice().equals(inNetworkDevice)) {
					deleteList.add(query);
				}
			}
			for (CommandInfoQuery query : deleteList) {
				mQueryCmdMap.remove(query.getQuery().getQueryID());
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 	Send any packets waiting in the packet queue.
	 */
	private void startCommandReceivers() {

		for (final IWirelessInterface gwInterface : mInterfaceList) {

			Thread gwThread = new Thread(new Runnable() {
				public void run() {
					while (mShouldRun) {
						try {
							if (gwInterface.isStarted()) {
								ICommand command = gwInterface.receiveCommand(mNetworkId);
								if (command != null) {
									//putPacketInRcvQueue(packet);
									if (command.getCommandIdEnum().equals(CommandIdEnum.ACK)) {
										LOGGER.info("Command ack RECEIVED: " + command.toString());
										processAckCommand(command);
									} else {
										receiveCommand(command, command.getSrcAddr());
									}
								} else {
									try {
										Thread.sleep(CTRL_NOCMD_DELAY_MILLIS);
									} catch (InterruptedException e) {
										LOGGER.error("", e);
									}
								}
							} else {
								try {
									Thread.sleep(CTRL_START_DELAY_MILLIS);
								} catch (InterruptedException e) {
									LOGGER.error("", e);
								}
							}
						} catch (RuntimeException e) {
							LOGGER.error("", e);
						}
					}
				}
			}, RECEIVER_THREAD_NAME + ": " + gwInterface.getClass().getSimpleName());
			gwThread.setPriority(RECEIVER_THREAD_PRIORITY);
			gwThread.start();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 	Send any commands waiting in the command queue.
	 */
	private void sendCommands() {

		long commandSchedTime;
		long lastCommandSchedTimeNanos = System.nanoTime();
		long lastCommandSentTimeNanos = System.nanoTime();
		short commandsSent;

		//	 The controller should process events continuously until the application wants to quit/exit.
		while (mShouldRun) {
			//notify();
			try {
				if (mCommandSendCircularBuffer[mCommandSendConsumerPos] == null) {
					Thread.sleep(0, 250);
				} else {
					// Get the next command waiting in the queue and send it.
					ICommand command = mCommandSendCircularBuffer[mCommandSendConsumerPos];
					mCommandSendCircularBuffer[mCommandSendConsumerPos++] = null;
					if (mCommandSendConsumerPos >= MAX_PACKET_QUEUE_SIZE)
						mCommandSendConsumerPos = 0;

					// Wait until it is the right time to send this packet.
					commandSchedTime = command.getScheduledTimeNanos();
					if (commandSchedTime > 0) {
						if (commandSchedTime > System.nanoTime()) {
							// It's not time to send the packet yet.
							while (commandSchedTime > System.nanoTime()) {
								//Thread.sleep(1);
							}
						} else {
						}
					} else {
						//	mLastSendDelayNanos = 0;
					}

					// Send this packet and HIGH_WATER more if we can.
					sendCommand(command);
					commandsSent = 1;

					for (int i = 1; i < HIGH_WATER; i++) {
						if (mCommandSendCircularBuffer[mCommandSendConsumerPos] != null) {
							command = mCommandSendCircularBuffer[mCommandSendConsumerPos];
							mCommandSendCircularBuffer[mCommandSendConsumerPos++] = null;
							sendCommand(command);
							commandsSent++;
							if (mCommandSendConsumerPos >= MAX_PACKET_QUEUE_SIZE)
								mCommandSendConsumerPos = 0;
						}
					}

					if (commandSchedTime > 0) {
						//Thread.sleep(packetsSents * 1);
						Thread.sleep(2);

						if (LOGGER.isInfoEnabled()) {
							LOGGER.info("packet sched delay: " + ((commandSchedTime - lastCommandSchedTimeNanos) / 100000) + " act delay: "
									+ ((System.nanoTime() - lastCommandSentTimeNanos) / 100000) + " behind: " + ((System.nanoTime() - commandSchedTime) / 100000));
						}
						lastCommandSchedTimeNanos = commandSchedTime;
					} else {
						//Thread.sleep(0, 250);
					}

					//LOGGER.debug("Sent: " + packetsSent + " delay: " + ((System.nanoTime() - lastPacketSentTime) / 100000));
					lastCommandSentTimeNanos = System.nanoTime();
				}

			} catch (InterruptedException e) {
				LOGGER.error("", e);
			} catch (RuntimeException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void sendCommand(ICommand inCommand) {
		for (IWirelessInterface gwInterface : mInterfaceList) {
			if (gwInterface.isStarted()) {
				inCommand.setSentTimeMillis(System.currentTimeMillis());
				gwInterface.sendCommand(inCommand);
			} else {
				try {
					Thread.sleep(CTRL_START_DELAY_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Handle the request of a remote device that wants to associate to our controller.
	 *  @param inCommand    The association command that we want to process.  (The one just received.)
	 */
	private void processInfoCmd(CommandInfoABC inCommand, INetworkDevice inNetworkDevice) {

		// Figure out what kind of associate sub-command we have.

		switch (inCommand.getCommandIdEnum()) {
			case QUERY:
				processQueryCmd((CommandInfoQuery) inCommand, inNetworkDevice);
				break;

			case RESPONSE:
				processResponseCmd((CommandInfoResponse) inCommand, inNetworkDevice);
				break;

			default:
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inQueryCmd	The query command that we just received.
	 */
	private void processQueryCmd(CommandInfoQuery inQueryCmd, INetworkDevice inNetworkDevice) {

		IQuery query = inQueryCmd.getQuery();
		this.doProcessQuery(query);

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inResponseCmd	The response command that we just received.
	 */
	private void processResponseCmd(CommandInfoResponse inResponseCmd, INetworkDevice inNetworkDevice) {

		IResponse response = inResponseCmd.getResponse();

		synchronized (mQueryCmdMap) {
			// Find the outstanding query command that belongs to this response.
			CommandInfoQuery queryCmd = mQueryCmdMap.get(response.getQueryID());

			if (queryCmd != null) {
				IQuery query = queryCmd.getQuery();

				LOGGER.info("Received response " + response.toString() + " to query " + query.toString());

				// Remove the query from the list of outstanding queries.
				mQueryCmdMap.remove(query.getQueryID());

				this.doProcessResponse(response, query);

			} else {
				LOGGER.info("Got a malformed or unexpected query command.");
			}
		}

	}

	//--------------------------------------------------------------------------
	/**
	 *  Create a query command relevant to the operation. 
	 *  @param inQueryType	The type of query (sub-class dependent)
	 *  @param inEndpoint	The endpoint that we're going to send the command to.
	 *  @return	The query command that the sub-class just created.
	 */
	protected final CommandInfoQuery doCreateQueryCommand(IQuery inQuery) {
		return new CommandInfoQuery(inQuery);
	}

	//--------------------------------------------------------------------------
	/**
	 *  Create a response command relevant to the operation.
	 *  @param inResponseType	The type of response (sub-class dependent)
	 *  @param inEndpoint	The endpoint that we're going to send the command to.
	 *  @return	The response command that the sub-class just created.
	 */
	protected final CommandInfoResponse doCreateResponseCommand(IResponse inResponse) {
		return new CommandInfoResponse(inResponse);
	}

	final static class ChannelInfo {

		private int	mChannelEnergy;
		private int	mControllerCount;

		ChannelInfo() {

		}

		// --------------------------------------------------------------------------
		/**
		 *  @return Returns the chennelEnergy.
		 */
		public int getChannelEnergy() {
			return mChannelEnergy;
		}

		// --------------------------------------------------------------------------
		/**
		 *  @param outChennelEnergy The chennelEnergy to set.
		 */
		public void setChannelEnergy(int inChannelEnergy) {
			mChannelEnergy = inChannelEnergy;
		}

		// --------------------------------------------------------------------------
		/**
		 *  @return Returns the controllerCount.
		 */
		public int getControllerCount() {
			return mControllerCount;
		}

		// --------------------------------------------------------------------------
		/**
		 *  @param outControllerCount The controllerCount to set.
		 */
		public void setControllerCount(int inControllerCount) {
			mControllerCount = inControllerCount;
		}

		// --------------------------------------------------------------------------
		/**
		 *  @param outControllerCount The controllerCount to set.
		 */
		public void incrementControllerCount() {
			mControllerCount++;
		}

	}

}
