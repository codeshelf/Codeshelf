/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: RadioController.java,v 1.17 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.flyweight.bitfields.NBitInteger;
import com.gadgetworks.flyweight.command.AckStateEnum;
import com.gadgetworks.flyweight.command.CommandAssocABC;
import com.gadgetworks.flyweight.command.CommandAssocAck;
import com.gadgetworks.flyweight.command.CommandAssocCheck;
import com.gadgetworks.flyweight.command.CommandAssocReq;
import com.gadgetworks.flyweight.command.CommandAssocResp;
import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.CommandControlScan;
import com.gadgetworks.flyweight.command.CommandNetMgmtABC;
import com.gadgetworks.flyweight.command.CommandNetMgmtCheck;
import com.gadgetworks.flyweight.command.CommandNetMgmtIntfTest;
import com.gadgetworks.flyweight.command.CommandNetMgmtSetup;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetChannelValue;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;
import com.gadgetworks.flyweight.controller.FTDIInterface;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.IRadioControllerEventListener;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;
import com.google.inject.Inject;
import com.google.inject.name.Named;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public class RadioController implements IRadioController {

	// Right now, the devices are not sending back their network ID.
	public static final String									PRIVATE_GUID				= "00000000";
	public static final String									VIRTUAL_GUID				= "%%%%%%%%";

	// Artificially limit us to channels 0-3 to make testing faster.
	public static final byte									MAX_CHANNELS				= 16;
	public static final byte									NO_PREFERRED_CHANNEL		= (byte) 255;
	public static final String									NO_PREFERRED_CHANNEL_TEXT	= "None";

	private static final Logger									LOGGER						= LoggerFactory.getLogger(RadioController.class);

	private static final String									CONTROLLER_THREAD_NAME		= "Radio Controller";
	private static final String									BACKGROUND_THREAD_NAME		= "Radio Controller Background";
	private static final String									RECEIVER_THREAD_NAME		= "Packet Receiver";
	private static final String									STARTER_THREAD_NAME			= "Intferface Starter";

	private static final int									BACKGROUND_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;
	private static final int									RECEIVER_THREAD_PRIORITY	= Thread.MAX_PRIORITY;
	private static final int									STARTER_THREAD_PRIORITY		= Thread.NORM_PRIORITY;

	private static final long									CTRL_START_DELAY_MILLIS		= 5;
	private static final long									NETCHECK_DELAY_MILLIS		= 250;

	private static final long									ACK_TIMEOUT_MILLIS			= 20;
	private static final int									ACK_SEND_RETRY_COUNT		= 20;
	private static final long									MAX_PACKET_AGE_MILLIS		= 20000;
	private static final long									EVENT_SLEEP_MILLIS			= 50;
	private static final long									INTERFACE_CHECK_MILLIS		= 750;
	private static final long									CONTROLLER_SLEEP_MILLIS		= 10;
	private static final int									MAX_CHANNEL_VALUE			= 255;

	private static final long									PACKET_SPACING_MILLIS		= 20;
	private static final int									MAX_NETWORK_TEST_NUM		= 64;

	private static final int									ACK_QUEUE_SIZE				= 200;

	private Boolean												mShouldRun					= true;
	private Map<NetGuid, INetworkDevice>						mDeviceGuidMap;
	private Map<NetAddress, INetworkDevice>						mDeviceNetAddrMap;
	private IGatewayInterface									mGatewayInterface;
	private NetAddress											mServerAddress;
	private NetAddress											mBroadcastAddress;
	private NetworkId											mBroadcastNetworkId;
	private NetworkId											mNetworkId;
	private List<IRadioControllerEventListener>					mEventListeners;
	private long												mLastIntfCheckMillis;
	private long												mLastPacketSentMillis;
	private boolean												mIntfCheckPending;
	private byte												mAckId;
	private volatile Map<NetAddress, BlockingQueue<IPacket>>	mPendingAcksMap;

	private ChannelInfo[]										mChannelInfo;
	private boolean												mChannelSelected;
	private byte												mPreferredChannel;
	private byte												mRadioChannel;

	private Thread												mControllerThread;
	private Thread												mBackgroundThread;
	private Thread												mPacketProcessorThread;

	private byte												mNextAddress;
	
	private final Counter packetsSentCounter = MetricsService.addCounter(MetricsGroup.Radio,"packets.sent");

	// --------------------------------------------------------------------------
	/**
	 *  @param inSessionManager   The session manager for this controller.
	 */
	@Inject
	public RadioController(@Named(IPacket.NETWORK_NUM_PROPERTY) final byte inNetworkId, final IGatewayInterface inGatewayInterface) {

		mGatewayInterface = inGatewayInterface;
		mServerAddress = new NetAddress(IPacket.GATEWAY_ADDRESS);
		mBroadcastAddress = new NetAddress(IPacket.BROADCAST_ADDRESS);
		mBroadcastNetworkId = new NetworkId(IPacket.BROADCAST_NETWORK_ID);
		mEventListeners = new ArrayList<IRadioControllerEventListener>();

		mNetworkId = new NetworkId(inNetworkId);

		mChannelSelected = false;
		mChannelInfo = new ChannelInfo[MAX_CHANNELS];
		for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
			mChannelInfo[channel] = new ChannelInfo();
			mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
		}
		mRadioChannel = 0;

		mPendingAcksMap = new ConcurrentHashMap<NetAddress, BlockingQueue<IPacket>>();
		mDeviceGuidMap = new HashMap<NetGuid, INetworkDevice>();
		mDeviceNetAddrMap = new HashMap<NetAddress, INetworkDevice>();
		mNextAddress = 1;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#startController(byte)
	 */
	public final void startController(final byte inPreferredChannel) {

		mPreferredChannel = inPreferredChannel;

		LOGGER.info("--------------------------------------------");
		LOGGER.info("Starting radio controller on network: " + mNetworkId);
		LOGGER.info("--------------------------------------------");
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
		}, STARTER_THREAD_NAME);
		interfaceStarterThread.setPriority(STARTER_THREAD_PRIORITY);
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
				LOGGER.info("Waiting for interface to start");
			}
		} while (!started && mShouldRun);
		LOGGER.info("Interface started");

		selectChannel();

		mLastIntfCheckMillis = System.currentTimeMillis();
		while (mShouldRun) {
			try {
				if (/*(!mIntfCheckPending) && */(mLastIntfCheckMillis + INTERFACE_CHECK_MILLIS < System.currentTimeMillis())
						&& (mGatewayInterface.isStarted())) {

					if (testNum == MAX_NETWORK_TEST_NUM)
						testNum = 0;
					//					CommandNetMgmtIntfTest netIntTestCmd = new CommandNetMgmtIntfTest(testNum++);
					//					sendCommand(netIntTestCmd, mBroadcastAddress, false);
					CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_REQ,
						mBroadcastNetworkId,
						PRIVATE_GUID,
						mPreferredChannel,
						new NetChannelValue((byte) 0),
						new NetChannelValue((byte) 0));
					sendCommand(netCheck, mBroadcastAddress, false);
					mIntfCheckPending = true;

					mLastIntfCheckMillis = System.currentTimeMillis();
					// Wait for the next check.
					Thread.sleep(INTERFACE_CHECK_MILLIS);
				} else {
					Thread.sleep(CONTROLLER_SLEEP_MILLIS);
				}
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
			LOGGER.info("Radio channel " + inChannel);
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
							if (System.currentTimeMillis() > (packet.getSentTimeMillis() + (ACK_TIMEOUT_MILLIS * packet.getSendCount()))) {
								if ((packet.getSendCount() < ACK_SEND_RETRY_COUNT)
										&& ((System.currentTimeMillis() - packet.getCreateTimeMillis() < MAX_PACKET_AGE_MILLIS))) {
									//sendCommand(command, packet.getNetworkType(), packet.getDstAddr());
									sendPacket(packet);
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
	private void selectChannel() {

		if (mShouldRun) {

			// Pause to let the serial interface come up.
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
						mChannelInfo[channel].setControllerCount(0);
						mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
					}

					CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_REQ,
						mBroadcastNetworkId,
						PRIVATE_GUID,
						channel,
						new NetChannelValue((byte) 0),
						new NetChannelValue((byte) 0));
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
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IRadioController#sendCommand(com.gadgetworks.flyweight.command.ICommand, com.gadgetworks.flyweight.command.NetworkId, com.gadgetworks.flyweight.command.NetAddress, boolean)
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

		if ((inAckRequested) && (inNetworkId.getValue() != (IPacket.BROADCAST_NETWORK_ID))
				&& (inDstAddr.getValue() != (IPacket.BROADCAST_ADDRESS))) {

			// If we're pending an ACK then assign an ACK ID.
			mAckId++;
			if (mAckId == 0) {
				// To the network protocol a ACK ID of zero means we don't want a command ACK.
				mAckId = 1;
			}
			packet.setAckId(mAckId);
			packet.setAckState(AckStateEnum.PENDING);

			// Add the command to the pending ACKs map, and increment the command ID counter.
			BlockingQueue<IPacket> queue = mPendingAcksMap.get(inDstAddr);
			if (queue == null) {
				queue = new ArrayBlockingQueue<IPacket>(ACK_QUEUE_SIZE);
				mPendingAcksMap.put(inDstAddr, queue);
			}

			// If the ACK queue is too full then pause.
			// (Tho' in practice this starves the system - I raised ACK_QUEUE_SIZE to a crazy-high number until I can figure this out better.)
			while (queue.size() >= ACK_QUEUE_SIZE) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			}
			queue.add(packet);
			LOGGER.info("Queue packet:    " + packet.toString());
		} else {
			sendPacket(packet);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IController#addControllerListener(com.gadgetworks.controller.IControllerListener)
	 */
	public final void addControllerEventListener(final IRadioControllerEventListener inControllerEventListener) {
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
		//sendCommand(netSetupCmd, mBroadcastAddress, false);
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
					new NetChannelValue((byte) 0),
					new NetChannelValue((byte) 0));
				this.sendCommand(netCheck, mBroadcastAddress, false);
			}
		} else {
			// This is a net-check response.
			if (networkId.getValue() == IPacket.BROADCAST_NETWORK_ID) {

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
	 *  Periodically test the serial interface to see if it's still working.  
	 *  The controller sends a net interface test command to the gateway (dongle) and the
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
		for (IRadioControllerEventListener listener : mEventListeners) {
			if (listener.canNetworkDeviceAssociate(new NetGuid("0x" + uid))) {
				canAssociate = true;
			}
		}

		if (!canAssociate) {
			LOGGER.info("Device not allowed: " + uid);
		} else {
			INetworkDevice foundDevice = mDeviceGuidMap.get(new NetGuid("0x" + uid));

			if (foundDevice != null) {
				foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.SETUP);

				LOGGER.info("----------------------------------------------------");
				LOGGER.info("Device associated: " + foundDevice.getGuid().getHexStringNoPrefix());
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

				// Create and send an assign command to the remote that just woke up.
				CommandAssocResp assignCmd = new CommandAssocResp(uid, mNetworkId, foundDevice.getAddress(), foundDevice.getSleepSeconds());
				this.sendCommand(assignCmd, mBroadcastNetworkId, mBroadcastAddress, false);
				foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.ASSIGN_SENT);
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

		INetworkDevice foundDevice = mDeviceGuidMap.get(new NetGuid("0x" + uid));

		if (foundDevice != null) {
			CommandAssocAck ackCmd;
			LOGGER.info("Assoc check: " + foundDevice.toString());

			short level = inCommand.getBatteryLevel();
			if (foundDevice.getLastBatteryLevel() != level) {
				foundDevice.setLastBatteryLevel(level);
			}

			byte status = CommandAssocAck.IS_ASSOCIATED;

			// If the found device isn't in the STARTED state then it's not associated with us.
			if (foundDevice.getDeviceStateEnum() == null) {
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
				LOGGER.info("AssocCheck - NOT ASSOC: state was: " + foundDevice.getDeviceStateEnum());
			} else if (foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.ASSIGN_SENT)) {
				networkDeviceBecameActive(foundDevice);
			} else if (!foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.STARTED)) {
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
				LOGGER.info("AssocCheck - NOT ASSOC: state was: " + foundDevice.getDeviceStateEnum());
			}

			// If the found device has the wrong GUID then we have the wrong device.
			// (This could be two matching network IDs on the same channel.  
			// This could be a serious flaw in the network protocol.)
			if (!foundDevice.getGuid().toString().equalsIgnoreCase("0x" + uid)) {
				LOGGER.info("AssocCheck - NOT ASSOC: GUID mismatch: " + foundDevice.getGuid() + " and " + uid);
				status = CommandAssocAck.IS_NOT_ASSOCIATED;
			}

			// Create and send an ack command to the remote that we think is in the running state.
			ackCmd = new CommandAssocAck(uid, new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, status));

			// Send the command.
			sendCommand(ackCmd, inSrcAddr, false);
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

		// ~bhe: this should go into a separate class
		Thread gwThread = new Thread(new Runnable() {
			private final Counter packetsSentCounter = MetricsService.addCounter(MetricsGroup.Radio,"packets.sent");
			public void run() {
				while (mShouldRun) {
					try {
						if (mGatewayInterface.isStarted()) {
							IPacket packet = mGatewayInterface.receivePacket(mNetworkId);
							if (packet != null) {
								if (packet.getPacketType() == IPacket.ACK_PACKET) {
									LOGGER.info("Packet remote ACK req RECEIVED: " + packet.toString());
									processAckPacket(packet);
								} else {
									// If the inbound packet had an ACK ID then respond with an ACK ID.
									byte ackId = packet.getAckId();
									if (ackId != IPacket.EMPTY_ACK_ID) {
										respondToAck(ackId, packet.getNetworkId(), packet.getSrcAddr());
									}
									receiveCommand(packet.getCommand(), packet.getSrcAddr());
								}
								this.packetsSentCounter.inc();
							}
						} else {
							try {
								Thread.sleep(CTRL_START_DELAY_MILLIS);
							} catch (InterruptedException e) {
								LOGGER.error("", e);
							}
						}
					} catch (RuntimeException e) {
						// We catch EVERY exception, because we don't want the thread to abruptly exit on an unchecked exception.
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
	 * @param inAckId
	 * @param inNetId
	 * @param inSrcAddr
	 */
	private void respondToAck(final byte inAckId, final NetworkId inNetId, final NetAddress inSrcAddr) {
		INetworkDevice device = mDeviceNetAddrMap.get(inSrcAddr);
		if (device==null) {
			LOGGER.warn("Unable to respond to ack: Device with address "+inSrcAddr+" not found");
			return;
		}
		if (device.isAckIdNew(inAckId)) {

			LOGGER.info("Remote ack request RECEIVED: ack: " + inAckId + " net: " + inNetId + " src: " + inSrcAddr);

			device.setLastAckId(inAckId);
			CommandAssocAck ackCmd = new CommandAssocAck("00000000",
				new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, (byte) 0));

			sendCommand(ackCmd, inNetId, inSrcAddr, false);
			IPacket ackPacket = new Packet(ackCmd, inNetId, mServerAddress, inSrcAddr, false);
			ackCmd.setPacket(ackPacket);
			ackPacket.setAckId(inAckId);
			sendPacket(ackPacket);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPacket
	 */
	private void sendPacket(IPacket inPacket) {

		try {
			if (mGatewayInterface.isStarted()) {
				while ((System.currentTimeMillis() - mLastPacketSentMillis) < PACKET_SPACING_MILLIS) {
					Thread.sleep(Math.max(0, PACKET_SPACING_MILLIS - (System.currentTimeMillis() - mLastPacketSentMillis)));
				}
				inPacket.setSentTimeMillis(System.currentTimeMillis());
				inPacket.incrementSendCount();
				mLastPacketSentMillis = System.currentTimeMillis();
				mGatewayInterface.sendPacket(inPacket);
				this.packetsSentCounter.inc();
			} else {
				Thread.sleep(CTRL_START_DELAY_MILLIS);
			}
		} catch (InterruptedException e) {
			LOGGER.error("", e);
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
		inNetworkDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED);
		inNetworkDevice.startDevice();
	}

	// --------------------------------------------------------------------------
	/**
	 *  The control command gets processed by the subclass since this is where the application-specific knowledge resides.
	 *  @param inCommand
	 *  @param inSrcAddr
	 */
	private void processControlCmd(CommandControlABC inCommand, NetAddress inSrcAddr) {

		INetworkDevice device = mDeviceNetAddrMap.get(inSrcAddr);
		if (device != null) {

			switch (inCommand.getExtendedCommandID().getValue()) {
				case CommandControlABC.SCAN:
					CommandControlScan scanCommand = (CommandControlScan) inCommand;
					device.scanCommandReceived(scanCommand.getCommandString());
					break;

				case CommandControlABC.BUTTON:
					CommandControlButton buttonCommand = (CommandControlButton) inCommand;
					device.buttonCommandReceived(buttonCommand);
					break;

				default:
					break;
			}
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#addNetworkDevice(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public final void addNetworkDevice(final INetworkDevice inNetworkDevice) {

		// If the device has no address then assign one.
		if ((inNetworkDevice.getAddress() == null) || (inNetworkDevice.getAddress().equals(mServerAddress))) {
			inNetworkDevice.setAddress(new NetAddress(mNextAddress++));
		}

		mDeviceGuidMap.put(inNetworkDevice.getGuid(), inNetworkDevice);
		mDeviceNetAddrMap.put(inNetworkDevice.getAddress(), inNetworkDevice);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IController#removeNetworkDevice(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public final void removeNetworkDevice(INetworkDevice inNetworkDevice) {
		mDeviceGuidMap.remove(inNetworkDevice.getGuid());
		mDeviceNetAddrMap.remove(inNetworkDevice.getAddress());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IRadioController#getNetworkDevice(com.gadgetworks.flyweight.command.NetGuid)
	 */
	@Override
	public final INetworkDevice getNetworkDevice(NetGuid inGuid) {
		return mDeviceGuidMap.get(inGuid);
	}
}
