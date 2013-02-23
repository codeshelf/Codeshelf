/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: DeviceController.java,v 1.2 2013/02/23 05:42:09 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.bitfields.NBitInteger;
import com.gadgetworks.flyweight.command.AckStateEnum;
import com.gadgetworks.flyweight.command.CommandAssocABC;
import com.gadgetworks.flyweight.command.CommandAssocAck;
import com.gadgetworks.flyweight.command.CommandAssocCheck;
import com.gadgetworks.flyweight.command.CommandAssocReq;
import com.gadgetworks.flyweight.command.CommandAssocResp;
import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControl;
import com.gadgetworks.flyweight.command.CommandNetMgmtABC;
import com.gadgetworks.flyweight.command.CommandNetMgmtCheck;
import com.gadgetworks.flyweight.command.CommandNetMgmtIntfTest;
import com.gadgetworks.flyweight.command.CommandNetMgmtSetup;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetChannelValue;
import com.gadgetworks.flyweight.command.NetMacAddress;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public class DeviceController implements IController {

	// Right now, the devices are not sending back their network ID.
	public static final String									PRIVATE_GUID						= "00000000";
	public static final String									VIRTUAL_GUID						= "%%%%%%%%";

	public static final String									CONTROLLER_THREAD_NAME				= "Controller";

	// Artificially limit us to channels 0-3 to make testing faster.
	public static final byte									MAX_CHANNELS						= 16;
	public static final byte									NO_PREFERRED_CHANNEL				= (byte) 255;
	public static final String									NO_PREFERRED_CHANNEL_TEXT			= "None";

	private static final Log									LOGGER								= LogFactory.getLog(DeviceController.class);

	private static final String									BACKGROUND_THREAD_NAME				= "Controller Background";
	private static final String									RECEIVER_THREAD_NAME				= "Packet Receiver";
	private static final String									INTERFACESTARTER_THREAD_NAME		= "Intferface Starter";

	private static final int									BACKGROUND_THREAD_PRIORITY			= Thread.NORM_PRIORITY - 1;
	private static final int									RECEIVER_THREAD_PRIORITY			= Thread.MAX_PRIORITY;
	private static final int									INTERFACESTARTER_THREAD_PRIORITY	= Thread.NORM_PRIORITY;

	private static final long									CTRL_START_DELAY_MILLIS				= 200;
	private static final long									NETCHECK_DELAY_MILLIS				= 250;

	private static final long									ACK_TIMEOUT_MILLIS					= 50;
	private static final int									ACK_SEND_RETRY_COUNT				= 5;
	private static final long									EVENT_SLEEP_MILLIS					= 50;
	private static final long									INTERFACE_CHECK_MILLIS				= 5 * 1000;
	private static final long									CONTROLLER_SLEEP_MILLIS				= 10;
	private static final int									MAX_CHANNEL_VALUE					= 255;
	private static final int									MAX_NETWORK_TEST_NUM				= 64;

	private Boolean												mShouldRun							= true;
	private Map<NetMacAddress, INetworkDevice>					mDeviceMacAddrMap;
	private Map<NetAddress, INetworkDevice>						mDeviceNetAddrMap;
	private IGatewayInterface									mGatewayInterface;
	private NetAddress											mServerAddress;
	private NetAddress											mBroadcastAddress;
	private NetworkId											mBroadcastNetworkId;
	private NetworkId											mNetworkId;
	private List<IControllerEventListener>						mEventListeners;
	private long												mLastIntfCheckMillis;
	private boolean												mIntfCheckPending;
	private byte												mNextCommandID;
	private volatile Map<NetAddress, BlockingQueue<IPacket>>	mPendingAcksMap;

	private ChannelInfo[]										mChannelInfo;
	private boolean												mChannelSelected;
	private byte												mPreferredChannel;
	private byte												mRadioChannel;

	private Thread												mControllerThread;
	private Thread												mBackgroundThread;
	private Thread												mPacketProcessorThread;

	private byte												mNextAddress;

	// --------------------------------------------------------------------------
	/**
	 *  @param inSessionManager   The session manager for this controller.
	 */
	public DeviceController(final IGatewayInterface inGatewayInterface) {

		mGatewayInterface = inGatewayInterface;
		mServerAddress = IPacket.GATEWAY_ADDRESS;
		mBroadcastAddress = IPacket.BROADCAST_ADDRESS;
		mBroadcastNetworkId = IPacket.BROADCAST_NETWORK_ID;
		mEventListeners = new ArrayList<IControllerEventListener>();

		//Random random = new Random(System.currentTimeMillis());
		//		int randNetworkNum = random.nextInt(NetworkId.MAX_NETWORK_ID - 1) + 1;
		String networkIdStr = "0x01";// + Integer.toString(randNetworkNum, 10);
		mNetworkId = IPacket.DEFAULT_NETWORK_ID;

		mChannelSelected = false;
		mChannelInfo = new ChannelInfo[MAX_CHANNELS];
		mRadioChannel = 0;

		mPendingAcksMap = new HashMap<NetAddress, BlockingQueue<IPacket>>();
		mDeviceMacAddrMap = new HashMap<NetMacAddress, INetworkDevice>();
		mDeviceNetAddrMap = new HashMap<NetAddress, INetworkDevice>();
		mNextAddress = 1;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#startController(byte)
	 */
	public final void startController(final byte inPreferredChannel) {

		mPreferredChannel = inPreferredChannel;

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
		mGatewayInterface.stopInterface();

		// Signal that we want to stop.
		mShouldRun = false;

		boolean aThreadIsRunning = false;
		while (aThreadIsRunning) {
			aThreadIsRunning = (mBackgroundThread.getState() != Thread.State.TERMINATED);
			//			aThreadIsRunning |= (mPacketReceiverThread.getState() != Thread.State.TERMINATED);
			aThreadIsRunning |= (mPacketProcessorThread.getState() != Thread.State.TERMINATED);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}

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
				mGatewayInterface.startInterface();
			}
		}, INTERFACESTARTER_THREAD_NAME);
		interfaceStarterThread.setPriority(INTERFACESTARTER_THREAD_PRIORITY);
		interfaceStarterThread.setDaemon(true);
		interfaceStarterThread.start();

		startPacketReceivers();

		// Wait until the interfaces start.
		boolean started;
		do {
			started = mGatewayInterface.isStarted();
			if (!started) {
				try {
					Thread.sleep(INTERFACE_CHECK_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				LOGGER.info("Interfaces not started");
			}
		} while (!started && mShouldRun);

		selectChannel();

		mLastIntfCheckMillis = System.currentTimeMillis();
		while (mShouldRun) {
			try {
				// Check to see if we should perform an interface check.
				// (Only perform this interface check on the radio interface.)
				//				if (mLastIntfCheckMillis + INTERFACE_CHECK_MILLIS < System.currentTimeMillis()) {
				//					if (mIntfCheckPending) {
				//						if (mGatewayInterface.isStarted()) {
				//							//gwInterface.resetInterface();
				//						} else {
				//							try {
				//								Thread.sleep(CTRL_START_DELAY_MILLIS);
				//							} catch (Exception e) {
				//								LOGGER.error("", e);
				//							}
				//						}
				//
				//						// Now reselect the channel.
				//						//selectChannel();
				//
				//						// Wait for the next check.
				//						Thread.sleep(INTERFACE_CHECK_MILLIS);
				//					}
				//
				//					if (testNum == MAX_NETWORK_TEST_NUM)
				//						testNum = 0;
				//					CommandNetMgmtIntfTest netIntTestCmd = new CommandNetMgmtIntfTest(testNum++);
				//					sendCommand(netIntTestCmd, mBroadcastAddress, false);
				//					mIntfCheckPending = true;
				//
				//					mLastIntfCheckMillis = System.currentTimeMillis();
				//				}
				Thread.sleep(CONTROLLER_SLEEP_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#getChannel()
	 */
	public final byte getRadioChannel() {
		return mRadioChannel;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#setChannel(byte)
	 */
	public final void setRadioChannel(byte inChannel) {
		if ((inChannel < 0) || (inChannel > MAX_CHANNELS)) {
			LOGGER.error("Could not set channel - out of range!");
		} else {
			mChannelSelected = true;
			mRadioChannel = inChannel;
			CommandNetMgmtSetup netSetupCmd = new CommandNetMgmtSetup(mNetworkId, mRadioChannel);
			if (mGatewayInterface instanceof FTDIInterface) {
				// Net mgmt commands only get sent to the FTDI-controlled radio network.
				sendCommand(netSetupCmd, mBroadcastAddress, false);
			}
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
				// Check if there are any pending ACK packets that need resending.
				// Also consider the case where there is more than one packet destined for a remote.
				// (Only re-send one packet per network device, so that packets arrive in order.)
				if (mPendingAcksMap.size() > 0) {
					for (BlockingQueue<IPacket> queue : mPendingAcksMap.values()) {
						// Look at the next packet in the queue.
						IPacket packet = queue.peek();
						if (packet != null) {
							// If we've timed out waiting for an ACK then resend the command.
							if (System.currentTimeMillis() > (packet.getSentTimeMillis() + ACK_TIMEOUT_MILLIS)) {
								if (packet.getSendCount() < ACK_SEND_RETRY_COUNT) {
									//sendCommand(command, packet.getNetworkType(), packet.getDstAddr());
									sendPacket(packet);
									packet.incrementSendCount();
								} else {
									// If we've exceeded the retry time then remove the packet.
									// We should probably mark the device lost and clear the queue.
									packet.setAckState(AckStateEnum.NO_RESPONSE);
									LOGGER.info("Packet acked NO_RESPONSE: " + packet.toString());
									queue.remove();
								}
							}
						}
					}
				}
				//				}

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

		if (mShouldRun) {

			// Pause for a few seconds to let the serial interface come up.
			try {
				Thread.sleep(CTRL_START_DELAY_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}

			// If the user has a preferred channel then use it.
			if (mPreferredChannel != NO_PREFERRED_CHANNEL) {
				setRadioChannel(mPreferredChannel);
			} else {

				/* The process for selecting a channel goes like this: 
				 * 
				 * 1. Choose a channel.
				 * 2. Send a net-check command on that channel.
				 * 3. Wait for responses.  (One response will come directly from the gateway (dongle) with a GUID = '00000000'
				 * 4. Repeat until all channels have been checked.
				 * 5. Choose the best channel.
				 * 6. Send a net-setup command directly to the dongle to tell it the channel ID.
				 */

				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {

					// First setup the ChannelInfo structure for the channel.
					if (mChannelInfo[channel] == null) {
						mChannelInfo[channel] = new ChannelInfo();
						mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
					} else {
						mChannelInfo[channel].setControllerCount(NBitInteger.INIT_VALUE);
						mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
					}

					CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_REQ,
						mBroadcastNetworkId,
						PRIVATE_GUID,
						channel,
						new NetChannelValue(NBitInteger.INIT_VALUE),
						new NetChannelValue(NBitInteger.INIT_VALUE));
					sendCommand(netCheck, mBroadcastAddress, false);

					// Wait NETCHECK delay millis before sending the next net-check.
					try {
						Thread.sleep(NETCHECK_DELAY_MILLIS);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}
				}

				// At this point the ChannelInfo structures will contain all of the information we need to make a channel choice.
				mPreferredChannel = 0;
				// First find the channel with the lowest number of controllers.
				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
					if (mPreferredChannel != channel) {
						// The most important test is the number of controllers already on a channel.
						if (mChannelInfo[channel].getControllerCount() < mChannelInfo[mPreferredChannel].getControllerCount()) {
							mPreferredChannel = channel;
						}
					}
				}

				// By the above we will have picked the first channel with the lowest number of controllers.
				// There may be more than one channel with this same number of controllers.
				// So now search those equal channels for the one with the lowest energy.
				int lowestControllerCount = mChannelInfo[mPreferredChannel].getControllerCount();
				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
					if ((mPreferredChannel != channel) && (mChannelInfo[channel].getControllerCount() == lowestControllerCount)) {
						if (mChannelInfo[channel].getChannelEnergy() < mChannelInfo[mPreferredChannel].getChannelEnergy()) {
							mPreferredChannel = channel;
						}
					}
				}
				setRadioChannel(mPreferredChannel);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * When the controller gets a new command it arrives here.
	 * 
	 *  @param inCommand   The command just received.
	 *  @param inSrcAddr   The address is was received from.
	 */
	public final void receiveCommand(final ICommand inCommand, final NetAddress inSrcAddr) {

		if (inCommand != null) {

			switch (inCommand.getCommandTypeEnum()) {

				case NETMGMT:
					processNetworkMgmtCmd((CommandNetMgmtABC) inCommand, inSrcAddr);
					break;

				case ASSOC:
					if (mChannelSelected) {
						processAssocCmd((CommandAssocABC) inCommand, inSrcAddr);
					}
					break;

				case CONTROL:
					if (mChannelSelected) {
						processControlCmd((CommandControlABC) inCommand, inSrcAddr);
					}
					break;
				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * 
	 */
	public final void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommand(inCommand, mNetworkId, inDstAddr, inAckRequested);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * 
	 */
	public final void sendCommand(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, boolean inAckRequested) {

		IPacket packet = new Packet(inCommand, inNetworkId, mServerAddress, inDstAddr, inAckRequested);
		inCommand.setPacket(packet);

		/*
		 * Certain commands can request the remote to ACK (to guarantee that the command arrived).
		 * Most packets will not contain a command that requests and ACK, so normally packets will just
		 * get sent right here.  
		 * 
		 * If a packet's command requires an ACK then we perform the following steps:
		 * 
		 * - Check that the packet address is something other than a broadcast network ID or network address.  
		 * 		(We don't support broadcast ACK.)
		 * - If a packet queue does not exist for the destination then:
		 * 		1. Create a packet queue for the destination.
		 * 		2. Put the packet in the queue.
		 * 		3. Send the packet.
		 * - If a packet queue does exist for the destination then just put the packet in it.
		 */

		if ((inAckRequested) && (!inNetworkId.equals(IPacket.BROADCAST_NETWORK_ID)) && (!inDstAddr.equals(IPacket.BROADCAST_ADDRESS))) {

			// Set the command ID.
			// To the network protocol a command ID of zero means we don't want a command ACK.
			mNextCommandID++;
			if (mNextCommandID == 0) {
				mNextCommandID = 1;
			}
			packet.setAckId(mNextCommandID);
			packet.setAckState(AckStateEnum.PENDING);

			// Add the command to the pending ACKs map, and increment the command ID counter.
			BlockingQueue<IPacket> queue = mPendingAcksMap.get(inDstAddr);
			if (queue == null) {
				queue = new ArrayBlockingQueue<IPacket>(10);
				mPendingAcksMap.put(inDstAddr, queue);
			}
			sendPacket(packet);
			queue.add(packet);
		} else {
			sendPacket(packet);
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
	 *  @param inCommand    The wake command the we want to process.  (The one just received.)
	 */
	private void processNetworkMgmtCmd(CommandNetMgmtABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of network management sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandNetMgmtABC.NETSETUP_COMMAND:
				processNetworkSetupCommand((CommandNetMgmtSetup) inCommand, inSrcAddr);
				break;

			case CommandNetMgmtABC.NETCHECK_COMMAND:
				processNetworkCheckCommand((CommandNetMgmtCheck) inCommand, inSrcAddr);
				break;

			case CommandNetMgmtABC.NETINTFTEST_COMMAND:
				processNetworkIntfTestCommand((CommandNetMgmtIntfTest) inCommand, inSrcAddr);
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
		sendCommand(netSetupCmd, mBroadcastAddress, false);
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
			String responseGUID = "";
			if (inCommand.getNetCheckType() == CommandNetMgmtCheck.NETCHECK_RESP) {
				// For a broadcast request we respond with the private GUID.  This will cause the gateway (dongle)
				// to insert its own GUID before transmitting it to the air.
				shouldRespond = true;
				responseGUID = PRIVATE_GUID;
			} else if (networkId.equals(mNetworkId)) {
				// For a network-specific request we respond with the GUID of the requester.
				shouldRespond = true;
				responseGUID = inCommand.getGUID();
			}

			if (shouldRespond) {
				// If this is a network check for us then response back to the sender.
				// Send a network check response command back to the sender.
				CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_RESP,
					inCommand.getNetworkId(),
					responseGUID,
					inCommand.getChannel(),
					new NetChannelValue(NBitInteger.INIT_VALUE),
					new NetChannelValue(NBitInteger.INIT_VALUE));
				this.sendCommand(netCheck, mBroadcastAddress, false);
			}
		} else {
			// This is a net-check response.
			if (networkId.equals(IPacket.BROADCAST_NETWORK_ID)) {

				// If this is a all-network net-check broadcast response then book keep the values.

				// Find the ChannelInfo instance for this channel.
				byte channel = inCommand.getChannel();

				if (inCommand.getGUID().equals(PRIVATE_GUID)) {
					// This came from the gateway (dongle) directly.
					// The gateway (dongle) will have inserted an energy detect value for the channel.
					mChannelInfo[channel].setChannelEnergy(inCommand.getChannelEnergy().getValue());
				} else {
					// This came from another controller on the same channel, so increment the number of controllers on the channel.
					mChannelInfo[channel].incrementControllerCount();
				}
			} else {
				// The controller never receives network-specific net-check responses.
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Due to a bug in FTDI's driver software we need to periodically test the serial interface to see if
	 *  it's still working.  The controller sends a net interface test command to the gateway (dongle) and the
	 *  gateway (dongle) sends an immediate reply if it's up-and-running.
	 *  
	 *  Here we just note that we got a response by indicating that no check is pending.
	 *  
	 *  @param inCommand
	 *  @param inNetworkType
	 *  @param inSrcAddr
	 */
	private void processNetworkIntfTestCommand(CommandNetMgmtIntfTest inCommand, NetAddress inSrcAddr) {
		mIntfCheckPending = false;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPacket
	 */
	private void processAckPacket(IPacket inPacket) {
		BlockingQueue<IPacket> queue = mPendingAcksMap.get(inPacket.getSrcAddr());
		if (queue != null) {
			for (IPacket packet : queue) {
				//IPacket packet = queue.peek();
				if (packet != null) {
					if (packet.getAckId() == inPacket.getAckId()) {
						packet.setAckData(inPacket.getAckData());
						packet.setAckState(AckStateEnum.SUCCEEDED);
						queue.remove(packet);
						LOGGER.info("Packet acked SUCCEEDED: " + packet.toString());
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

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandAssocABC.ASSOC_REQ_COMMAND:
				processAssocReqCommand((CommandAssocReq) inCommand, inSrcAddr);
				break;

			case CommandAssocABC.ASSOC_RESP_COMMAND:
				processAssocRespCommand((CommandAssocResp) inCommand, inSrcAddr);
				break;

			case CommandAssocABC.ASSOC_CHECK_COMMAND:
				processAssocCheckCommand((CommandAssocCheck) inCommand, inSrcAddr);
				break;

			case CommandAssocABC.ASSOC_ACK_COMMAND:
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
		String uid = inCommand.getGUID();

		LOGGER.info("AssocReq rcvd: " + inCommand.toString());

		// This is a device running on the radio network.

		// First let's make sure that this is a request from an actor that we are managing.
		// Indicate to listeners that there is a new actor.
		boolean canAssociate = false;
		for (IControllerEventListener listener : mEventListeners) {
			if (listener.canNetworkDeviceAssociate(uid)) {
				canAssociate = true;
			}
		}

		if (canAssociate) {

			INetworkDevice foundDevice = mDeviceMacAddrMap.get(new NetMacAddress(uid.getBytes()));

			if (foundDevice != null) {
				foundDevice.setNetworkDeviceState(NetworkDeviceStateEnum.SETUP);

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
					foundDevice.setNetAddress(new NetAddress(mNextAddress++));
				}

				// Create and send an assign command to the remote that just woke up.
				CommandAssocResp assignCmd = new CommandAssocResp(uid, mNetworkId, foundDevice.getNetAddress());
				this.sendCommand(assignCmd, mBroadcastNetworkId, mBroadcastAddress, false);
				foundDevice.setNetworkDeviceState(NetworkDeviceStateEnum.ASSIGN_SENT);

				// We should wait a bit for the remote to prepare to accept commands.
				try {
					Thread.sleep(CTRL_START_DELAY_MILLIS);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}

				networkDeviceBecameActive(foundDevice);
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
		String uid = inCommand.getGUID();

		INetworkDevice foundDevice = mDeviceMacAddrMap.get(new NetMacAddress(uid.getBytes()));

		if (foundDevice != null) {
			CommandAssocAck ackCmd;
			LOGGER.info("Assoc check: " + foundDevice.toString());

			short level = inCommand.getBatteryLevel();
			if (foundDevice.getLastBatteryLevel() != level) {
				foundDevice.setLastBatteryLevel(level);
			}

			byte status = CommandAssocAck.IS_ASSOCIATED;

			// If the found device isn't in the STARTED state then it's not associated with us.
			if (!(foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.STARTED))) {
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
				LOGGER.info("AssocCheck - NOT ASSOC: state was: " + foundDevice.getNetworkDeviceState());
			}

			// If the found device has the wrong GUID then we have the wrong device.
			// (This could be two matching network IDs on the same channel.  
			// This could be a serious flaw in the network protocol.)
			if (!foundDevice.getMacAddress().toString().equals(uid)) {
				LOGGER.info("AssocCheck - NOT ASSOC: GUID mismatch: " + foundDevice.getMacAddress() + " and " + uid);
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
			}

			// Only send the response to devices whose state is INVALID, STARTED or LOST.
			//			if ((foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.INVALID))
			//					|| (foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.STARTED))
			//					|| (foundDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.LOST))) {
			// Create and send an ack command to the remote that we think is in the running state.
			ackCmd = new CommandAssocAck(uid, new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, status));

			// Send the command.
			sendCommand(ackCmd, inSrcAddr, false);
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
	 * 	Start the packet receivers.
	 */
	private void startPacketReceivers() {

		Thread gwThread = new Thread(new Runnable() {
			public void run() {
				while (mShouldRun) {
					try {
						if (mGatewayInterface.isStarted()) {
							IPacket packet = mGatewayInterface.receivePacket(mNetworkId);
							if (packet != null) {
								//putPacketInRcvQueue(packet);
								if (packet.getPacketType() == IPacket.ACK_PACKET) {
									LOGGER.info("Packet acked RECEIVED: " + packet.toString());
									processAckPacket(packet);
								} else {
									receiveCommand(packet.getCommand(), packet.getSrcAddr());
								}
							} else {
								//									try {
								//										Thread.sleep(0, 5000);
								//									} catch (InterruptedException e) {
								//										LOGGER.error("", e);
								//									}
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
		}, RECEIVER_THREAD_NAME + ": " + mGatewayInterface.getClass().getSimpleName());
		gwThread.setPriority(RECEIVER_THREAD_PRIORITY);
		gwThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPacket
	 */
	private void sendPacket(IPacket inPacket) {
		if (mGatewayInterface.isStarted()) {
			inPacket.setSentTimeMillis(System.currentTimeMillis());
			mGatewayInterface.sendPacket(inPacket);
		} else {
			try {
				Thread.sleep(CTRL_START_DELAY_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
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

	//--------------------------------------------------------------------------
	/**
	 *  Handle the special case where a device just became active in the network.
	 *  @param inSession	The device that just became active.
	 */
	private void networkDeviceBecameActive(INetworkDevice inNetworkDevice) {
		inNetworkDevice.setNetworkDeviceState(NetworkDeviceStateEnum.SETUP);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The control command gets processed by the subclass since this is where the application-specific knowledge resides.
	 *  @param inCommand
	 *  @param inSrcAddr
	 */
	private void processControlCmd(CommandControlABC inCommand, NetAddress inSrcAddr) {

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandControlABC.STANDARD:
				INetworkDevice device = mDeviceNetAddrMap.get(inSrcAddr);
				if (device != null) {
					CommandControl command = (CommandControl) inCommand;
					LOGGER.info("Remote command: " + command.getCommandString());
				}
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#addNetworkDevice(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public final void addNetworkDevice(final INetworkDevice inNetworkDevice) {
		mDeviceMacAddrMap.put(inNetworkDevice.getMacAddress(), inNetworkDevice);
		mDeviceNetAddrMap.put(inNetworkDevice.getNetAddress(), inNetworkDevice);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#removeNetworkDevice(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public final void removeNetworkDevice(INetworkDevice inNetworkDevice) {
		mDeviceMacAddrMap.remove(inNetworkDevice.getMacAddress());
		mDeviceNetAddrMap.remove(inNetworkDevice.getNetAddress());
	}
}
