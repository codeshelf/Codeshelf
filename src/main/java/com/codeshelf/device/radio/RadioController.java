/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: RadioController.java,v 1.17 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.device.radio;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.DeviceRestartCauseEnum;
import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;
import com.codeshelf.flyweight.command.CommandAssocABC;
import com.codeshelf.flyweight.command.CommandAssocAck;
import com.codeshelf.flyweight.command.CommandAssocCheck;
import com.codeshelf.flyweight.command.CommandAssocReq;
import com.codeshelf.flyweight.command.CommandAssocResp;
import com.codeshelf.flyweight.command.CommandControlABC;
import com.codeshelf.flyweight.command.CommandControlAck;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlScan;
import com.codeshelf.flyweight.command.CommandNetMgmtABC;
import com.codeshelf.flyweight.command.CommandNetMgmtIntfTest;
import com.codeshelf.flyweight.command.CommandNetMgmtSetup;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * IEEE_802_15_2_RadioController
 * 
 * @author jeffw, saba, huffa
 */

public class RadioController implements IRadioController {
	private static final Logger							LOGGER						= LoggerFactory.getLogger(RadioController.class);

	public static final String							PRIVATE_GUID				= "00000000";
	public static final String							VIRTUAL_GUID				= "%%%%%%%%";

	public static final byte							MAX_CHANNELS				= 16;
	public static final byte							NO_PREFERRED_CHANNEL		= (byte) 255;
	public static final String							NO_PREFERRED_CHANNEL_TEXT	= "None";
	public static final byte							DEFAULT_CHANNEL				= 1;

	private static final String							CONTROLLER_THREAD_NAME		= "Radio Controller";
	private static final String							STARTER_THREAD_NAME			= "Intferface Starter";

	private static final int							STARTER_THREAD_PRIORITY		= Thread.NORM_PRIORITY;

	private static final long							CTRL_START_DELAY_MILLIS		= 5;

	private static final long							NETCHECK_RATE_MILLIS		= 750;

	private static final int							MAX_CHANNEL_VALUE			= 255;

	@Getter
	private final IGatewayInterface						gatewayInterface;

	private volatile boolean							mShouldRun					= true;

	private final NetAddress							mServerAddress				= new NetAddress(IPacket.GATEWAY_ADDRESS);

	// We iterate over this list often, but write almost never. It needs to be thread-safe so we chose to make writes slow and reads lock-free.
	private final List<IRadioControllerEventListener>	mEventListeners				= new CopyOnWriteArrayList<>();

	// This does not need to be synchronized because it is only ever used by a single thread in the packet handler service
	// processNetworkCheckCommand only accesses this array for the broadcast network address.
	private final ChannelInfo[]							mChannelInfo				= new ChannelInfo[MAX_CHANNELS];

	// This 3 variables are only every modified in a synchronized method but we make volatile so it is visible to other threads.
	private volatile boolean							mChannelSelected			= false;
	private volatile byte								mPreferredChannel;
	private volatile byte								mRadioChannel				= 0;

	private Thread										mControllerThread;

	private final Map<NetGuid, INetworkDevice>			mDeviceGuidMap				= Maps.newConcurrentMap();
	private final Map<NetAddress, INetworkDevice>		mDeviceNetAddrMap			= Maps.newConcurrentMap();

	private volatile boolean							mRunning					= false;

	// Services
	private final RadioControllerInboundPacketService	packetHandlerService;
	private final RadioControllerPacketIOService		packetIOService;
	private final RadioControllerBroadcastService		broadcastService;
	private final RadioControllerPacketSchedulerService	packetSchedulerService;

	/**
	 * @param inSessionManager
	 *            The session manager for this controller.
	 */
	@Inject
	public RadioController(final IGatewayInterface inGatewayInterface) {
		this.gatewayInterface = inGatewayInterface;

		for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
			mChannelInfo[channel] = new ChannelInfo();
			mChannelInfo[channel].setChannelEnergy((short) MAX_CHANNEL_VALUE);
		}

		// Create Services
		this.packetHandlerService = new RadioControllerInboundPacketService(this);
		this.packetIOService = new RadioControllerPacketIOService(inGatewayInterface, packetHandlerService);
		this.broadcastService = new RadioControllerBroadcastService(this, NETCHECK_RATE_MILLIS);
		this.packetSchedulerService = new RadioControllerPacketSchedulerService(packetIOService);
	}

	@Override
	public final void setNetworkId(NetworkId inNetworkId) {
		if (mRunning) {
			if (!packetIOService.getNetworkId().equals(inNetworkId)) {
				LOGGER.error("Cannot change network ID, radio is already running");
			}
			return;
		}
		packetIOService.setNetworkId(inNetworkId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gadgetworks.flyweight.controller.IController#startController(byte)
	 */
	@Override
	public synchronized final void startController(final byte inPreferredChannel) {
		if (packetIOService.getNetworkId() == null) {
			LOGGER.error("Cannot start radio controller, must call setNetworkId() first");
			return;
		}
		if (mRunning) {
			if (inPreferredChannel != this.mPreferredChannel) {
				LOGGER.error("Cannot change channel, radio is already running");
			}
			return;
		}
		mRunning = true;
		mPreferredChannel = inPreferredChannel;

		LOGGER.info("--------------------------------------------");
		LOGGER.info("Starting radio controller on network {}", packetIOService.getNetworkId());
		LOGGER.info("--------------------------------------------");
		mControllerThread = new Thread(this, CONTROLLER_THREAD_NAME);
		mControllerThread.start();
	}

	@Override
	public final void stopController() {

		// Stop services
		packetIOService.stop();
		packetHandlerService.shutdown();
		broadcastService.stop();
		packetSchedulerService.stop();

		// Stop all of the interfaces.
		gatewayInterface.stopInterface();

		// Signal that we want to stop.
		mShouldRun = false;
		mRunning = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		// Start all of the serial interfaces.
		// They start on a thread since this op won't complete if no dongle is
		// attached.
		Thread interfaceStarterThread = new Thread(new Runnable() {
			@Override
			public void run() {
				gatewayInterface.startInterface();
			}
		}, STARTER_THREAD_NAME);
		interfaceStarterThread.setPriority(STARTER_THREAD_PRIORITY);
		interfaceStarterThread.setDaemon(true);
		interfaceStarterThread.start();

		// Wait until the interfaces start.
		boolean started;
		do {
			started = gatewayInterface.isStarted();
			if (!started) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				LOGGER.debug("Waiting for interface to start");
			}

		} while (!started && mShouldRun);

		if (mShouldRun) {
			LOGGER.info("Gateway radio interface started");

			selectChannel();

			// Start services
			packetSchedulerService.start();
			packetIOService.start();
			broadcastService.start();
			packetHandlerService.start();
			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.flyweight.controller.IController#getChannel()
	 */
	@Override
	public final byte getRadioChannel() {
		return mRadioChannel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.flyweight.controller.IController#setChannel(byte)
	 */
	@Override
	public final void setRadioChannel(byte inChannel) {
		if ((inChannel < 0) || (inChannel > MAX_CHANNELS)) {
			LOGGER.error("Could not set channel - out of range!");
		} else {
			LOGGER.info("Trying to set radio channel={}", inChannel);
			mChannelSelected = true;
			mRadioChannel = inChannel;
			CommandNetMgmtSetup netSetupCmd = new CommandNetMgmtSetup(packetIOService.getNetworkId(), mRadioChannel);
			sendCommand(netSetupCmd, broadcastService.getBroadcastAddress(), false);
			LOGGER.info("Radio channel={}", inChannel);
		}
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
				LOGGER.warn("No channel has been specified. Using default channel {}", DEFAULT_CHANNEL);
				setRadioChannel(DEFAULT_CHANNEL);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * When the controller gets a new command it arrives here.
	 * 
	 * @param inCommand
	 *            The command just received.
	 * @param inSrcAddr
	 *            The address is was received from.
	 */
	@Override
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

		} else {
			LOGGER.error("Command was null");
		}
	}

	// --------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 */
	@Override
	public final void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommand(inCommand, packetIOService.getNetworkId(), inDstAddr, inAckRequested);
	}
	
	public final void sendCommandFrontQueue(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommandFrontQueue(inCommand, packetIOService.getNetworkId(), inDstAddr, inAckRequested);
	}

	public final void sendAssociationCommand(ICommand inCommand,
		NetworkId inNetworkId,
		NetAddress inDstAddr,
		NetGuid deviceAddr,
		boolean inAckRequested) {

		INetworkDevice device = null;
		IPacket packet = null;

		device = this.mDeviceGuidMap.get(deviceAddr);

		if (device == null) {
			LOGGER.warn("Could not find device with net addres: {}", deviceAddr);
			return;
		}

		packet = new Packet(inCommand, inNetworkId, mServerAddress, inDstAddr, inAckRequested);
		packet.setDevice(device);

		if (inAckRequested) {
			packet.setAckId(device.getNextAckId());
		}

		packetSchedulerService.addCommandPacketToSchedule(packet, device);

	}

	// --------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gadgetworks.flyweight.controller.IRadioController#sendCommand(com
	 * .gadgetworks.flyweight.command.ICommand,
	 * com.gadgetworks.flyweight.command.NetworkId,
	 * com.gadgetworks.flyweight.command.NetAddress, boolean)
	 */
	@Override
	public final void sendCommand(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, boolean inAckRequested) {
		INetworkDevice device = null;
		IPacket packet = null;

		device = this.mDeviceNetAddrMap.get(inDstAddr);

		if (device == null) {
			LOGGER.warn("Could not find device with net addres: {}", inDstAddr);
			return;
		}

		packet = new Packet(inCommand, inNetworkId, mServerAddress, inDstAddr, inAckRequested);

		if (packet != null) {
			packet.setDevice(device);

			if (inAckRequested) {
				packet.setAckId(device.getNextAckId());
			}

			packetSchedulerService.addCommandPacketToSchedule(packet, device);
		}
	}
	
	public final void sendCommandFrontQueue(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, boolean inAckRequested) {
		INetworkDevice device = null;
		IPacket packet = null;

		device = this.mDeviceNetAddrMap.get(inDstAddr);

		if (device == null) {
			LOGGER.warn("Could not find device with net addres: {}", inDstAddr);
			return;
		}

		packet = new Packet(inCommand, inNetworkId, mServerAddress, inDstAddr, inAckRequested);

		if (packet != null) {
			packet.setDevice(device);

			if (inAckRequested) {
				packet.setAckId(device.getNextAckId());
			}

			packetSchedulerService.addAckPacketToSchedule(packet, device);
		}
	}

	@Override
	public final void sendNetMgmtCommand(ICommand inCommand, NetAddress inDstAddr) {
		IPacket packet = null;

		packet = new Packet(inCommand, packetIOService.getNetworkId(), mServerAddress, inDstAddr, false);

		if (packet != null) {
			packetSchedulerService.addNetMgmtPacketToSchedule(packet);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gadgetworks.controller.IController#addControllerListener(com.gadgetworks
	 * .controller.IControllerListener)
	 */
	@Override
	public final void addControllerEventListener(final IRadioControllerEventListener inControllerEventListener) {
		mEventListeners.add(inControllerEventListener);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 *            The wake command the we want to process. (The one just
	 *            received.)
	 */
	private void processNetworkMgmtCmd(CommandNetMgmtABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of network management sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandNetMgmtABC.NETSETUP_COMMAND:
				processNetworkSetupCommand((CommandNetMgmtSetup) inCommand, inSrcAddr);
				break;
				
			case CommandNetMgmtABC.NETCHECK_COMMAND:
				break;

			case CommandNetMgmtABC.NETINTFTEST_COMMAND:
				processNetworkIntfTestCommand((CommandNetMgmtIntfTest) inCommand, inSrcAddr);
				break;

			default:
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * When the gateway (dongle) starts/resets it will attempt to ask the
	 * controller what channel it should be using. This is done, because the
	 * gateway (dongle) is not really capable of writing semi-permanent config
	 * parameters to NVRAM. (It's a complication of the MCU's Flash RAM
	 * functionality and the limited number of times you can write to Flash over
	 * the life of the device.
	 * 
	 * @param inCommand
	 */
	private void processNetworkSetupCommand(CommandNetMgmtSetup inCommand, NetAddress inSrcAddr) {
		/*
		 * The only time that we will ever see a net-setup is when the gateway
		 * (dongle) sends it to the controller in order to learn the channel it
		 * should use. The gateway (dongle) may crash/reset, but it needs to be
		 * able to come back up fast without seriously disrupting the network.
		 * The best way to do this is to allow the controller to maintain the
		 * state info about the network that is running.
		 */
		new CommandNetMgmtSetup(packetIOService.getNetworkId(), mRadioChannel);
	}

	// --------------------------------------------------------------------------
	/**
	 * Periodically test the serial interface to see if it's still working. The
	 * controller sends a net interface test command to the gateway (dongle) and
	 * the gateway (dongle) sends an immediate reply if it's up-and-running.
	 * 
	 * Here we just note that we got a response by indicating that no check is
	 * pending.
	 * 
	 * @param inCommand
	 * @param inNetworkType
	 * @param inSrcAddr
	 */
	private void processNetworkIntfTestCommand(CommandNetMgmtIntfTest inCommand, NetAddress inSrcAddr) {
		// Do Nothing
	}

	// --------------------------------------------------------------------------
	/**
	 * Handle the request of a remote device that wants to associate to our
	 * controller.
	 * 
	 * @param inCommand
	 *            The association command that we want to process. (The one just
	 *            received.)
	 */
	private void processAssocCmd(CommandAssocABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of associate sub-command we have.
		ContextLogging.setNetGuid(inCommand.getGUID());
		try {
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
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processAssocReqCommand(CommandAssocReq inCommand, NetAddress inSrcAddr) {

		// First get the unique ID from the command.
		String uid = inCommand.getGUID();
		NetGuid theGuid = null;

		if (uid == null || uid.trim().isEmpty()) {
			LOGGER.warn("Blank or missing uid: " + uid + " for processAssocReqCommand");
			return;
		}
		// DEV-544 see garbage once in a while during MAT test. This may throw
		try {
			theGuid = new NetGuid("0x" + uid);
		} catch (OutOfRangeException e) {
			LOGGER.warn("Bad uid: " + uid + " for processAssocReqCommand ", e);
			return;
		}

		// Indicate to listeners that there is a new actor.
		boolean canAssociate = false;
		for (IRadioControllerEventListener listener : mEventListeners) {
			if (listener.canNetworkDeviceAssociate(theGuid)) {
				canAssociate = true;
			}
		}

		if (!canAssociate) {
			// LOGGER.info("Device not allowed: " + uid); No longer do this.
			// Happens all the time, especially in the office
			// Could keep a counter on these.
		} else {
			INetworkDevice foundDevice = mDeviceGuidMap.get(theGuid);

			if (foundDevice != null) {
				ContextLogging.setNetGuid(foundDevice.getGuid());
				try {

					foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.SETUP);
					foundDevice.setHardwareVersion(inCommand.getHardwareVersion());
					foundDevice.setFirmwareVersion(inCommand.getFirmwareVersion());

					//LOGGER.info("Device associated={}; Req={}", foundDevice.getGuid().getHexStringNoPrefix(), inCommand);
					if ((inCommand.getSystemStatus() & 0x02) > 0) {
						LOGGER.debug(" Status: LVD");
					}
					if ((inCommand.getSystemStatus() & 0x04) > 0) {
						LOGGER.debug(" Status: ICG");
					}
					if ((inCommand.getSystemStatus() & 0x10) > 0) {
						LOGGER.debug(" Status: ILOP");
					}
					if ((inCommand.getSystemStatus() & 0x20) > 0) {
						LOGGER.debug(" Status: COP");
					}
					if ((inCommand.getSystemStatus() & 0x40) > 0) {
						LOGGER.debug(" Status: PIN");
					}
					if ((inCommand.getSystemStatus() & 0x80) > 0) {
						LOGGER.debug(" Status: POR");
					}
					// LOGGER.info("----------------------------------------------------");

					// Create and send an assign command to the remote that just
					// woke up.
					// Clear packet queue
					packetSchedulerService.clearDevicePacketQueue(foundDevice);
					
					CommandAssocResp assignCmd = new CommandAssocResp(uid,
						packetIOService.getNetworkId(),
						foundDevice.getAddress(),
						foundDevice.getSleepSeconds());
					
					assignCmd.setScannerType(foundDevice.getScannerTypeCode());

					this.sendAssociationCommand(assignCmd,
						broadcastService.getBroadcastNetworkId(),
						broadcastService.getBroadcastAddress(),
						foundDevice.getGuid(),
						false);
					
					foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.ASSIGN_SENT);
				} finally {
					ContextLogging.clearNetGuid();
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processAssocRespCommand(CommandAssocResp inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processAssocCheckCommand(CommandAssocCheck inCommand, NetAddress inSrcAddr) {
		// FIXME - huffa this logic works but is based on legacy netcheck behavior
		boolean deviceBecameActive = false;

		// First get the unique ID from the command.
		String uid = inCommand.getGUID();

		INetworkDevice foundDevice = mDeviceGuidMap.get(new NetGuid("0x" + uid));

		if (foundDevice != null) {
			ContextLogging.setNetGuid(foundDevice.getGuid());
			try {
				CommandAssocAck ackCmd;
				LOGGER.info("Assoc check for {}", foundDevice);
				
				short level = inCommand.getBatteryLevel();
				if (foundDevice.getLastBatteryLevel() != level) {
					foundDevice.setLastBatteryLevel(level);
				}

				byte status = CommandAssocAck.IS_ASSOCIATED;

				// If the found device isn't in the STARTED state then it's not
				// associated with us.
				if (foundDevice.getDeviceStateEnum() == null) {
					status = CommandAssocAck.IS_NOT_ASSOCIATED;
					LOGGER.info("AssocCheck1 - NOT ASSOC: state was: {}", foundDevice.getDeviceStateEnum());
					return;
				} else if (foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.ASSIGN_SENT)) {
					// Device is requesting association ack message. Respond.
					packetSchedulerService.clearDevicePacketQueue(foundDevice);
					deviceBecameActive = true;
				} else if (foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.STARTED)) {
					// Device did not receive our association ack message. Resend.
					packetSchedulerService.clearDevicePacketQueue(foundDevice);
					deviceBecameActive = true;
				} else if (!foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.STARTED)) {
					status = CommandAssocAck.IS_NOT_ASSOCIATED;
					LOGGER.info("AssocCheck2 - NOT ASSOC: state was: {}", foundDevice.getDeviceStateEnum());
					return;
				}

				// If the found device has the wrong GUID then we have the wrong
				// device.
				// (This could be two matching network IDs on the same channel.
				// This could be a serious flaw in the network protocol.)
				if (!foundDevice.getGuid().toString().equalsIgnoreCase("0x" + uid)) {
					LOGGER.info("AssocCheck3 - NOT ASSOC: GUID mismatch: {} and {}", foundDevice.getGuid(), uid);
					status = CommandAssocAck.IS_NOT_ASSOCIATED;
					return;
				}
				
				DeviceRestartCauseEnum restartEnum = DeviceRestartCauseEnum.getRestartEnum(inCommand.getRestartCause());
				Integer restartPC = inCommand.getRestartPC();
				foundDevice.notifyAssociate("Restart cause: " + restartEnum.getName() + " PC: " + Integer.toHexString(restartPC));

				if (deviceBecameActive) {
					// Create and send an ack command to the remote that we think is in the running state.
					LOGGER.info("Device associated={}; Req={}", foundDevice.getGuid().getHexStringNoPrefix(), inCommand);
					ackCmd = new CommandAssocAck(uid, new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, status));
					sendCommandFrontQueue(ackCmd, inSrcAddr, false);
					networkDeviceBecameActive(foundDevice);
				}
			} finally {
				ContextLogging.clearNetGuid();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processAssocAckCommand(CommandAssocAck inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	@SuppressWarnings("unused")
	private void processAckPacket(IPacket ackPacket) {
		INetworkDevice device = null;
		
		device = mDeviceNetAddrMap.get(ackPacket.getSrcAddr());

		if (device != null) {
			packetSchedulerService.markPacketAsAcked(device, ackPacket.getAckId(), ackPacket);
		}
	}

	/**
	 * @param inAckId
	 * @param inNetId
	 * @param inSrcAddr
	 */
	private void sendPacketAck(INetworkDevice device, final byte inAckId, final NetworkId inNetId, final NetAddress inSrcAddr) {
		ContextLogging.setNetGuid(device.getGuid());
		try {
			LOGGER.info("ACKing packet: ackId={}; netId={}; srcAddr={}", inAckId, inNetId, inSrcAddr);
			device.setLastIncomingAckId(inAckId);
			
			CommandControlAck ackCmd = new CommandControlAck(NetEndpoint.PRIMARY_ENDPOINT, inAckId);
			IPacket ackPacket = new Packet(ackCmd, packetIOService.getNetworkId(), mServerAddress, device.getAddress(), false);
			
			//ackPacket.setPacketType(IPacket.ACK_PACKET);
			
			packetSchedulerService.addAckPacketToSchedule(ackPacket, device);
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	public void handleInboundPacket(IPacket packet) {
		NetAddress packetSourceAddress = packet.getSrcAddr();

		if (packetSourceAddress == mServerAddress) {
			// Ignore packet from ourselves or other servers
			LOGGER.debug("Ignoring packet from serverAddress={}", packetSourceAddress);
			return;
		}

		INetworkDevice device = this.mDeviceNetAddrMap.get(packetSourceAddress);
		if (device != null) {
			ContextLogging.setNetGuid(device.getGuid());
			device.setLastPacketReceivedTime(System.currentTimeMillis());
		}

		try {
//			if (packet.getPacketType() == IPacket.ACK_PACKET) {
//				LOGGER.debug("Packet remote ACK req RECEIVED: " + packet.toString());
//				processAckPacket(packet);
//			} else {
				// If the inbound packet had an ACK ID then respond with an ACK ID.
				boolean shouldActOnCommand = true;
				if (packet.getAckId() != IPacket.EMPTY_ACK_ID) {
					if (device == null) {
						//LOGGER.warn("Ignoring packet with device with unknown address={}", packetSourceAddress);
						return;
					} else {
						// Only act on the command if the ACK is new (i.e. > last ack id)
						shouldActOnCommand = device.isAckIdNew(packet.getAckId());

						// Always respond to an ACK
						sendPacketAck(device, packet.getAckId(), packet.getNetworkId(), packetSourceAddress);
					}
				}

				if (shouldActOnCommand) {
					receiveCommand(packet.getCommand(), packetSourceAddress);
				} else {
					LOGGER.warn("ACKed, but did not process a packet that we acked before; {}", packet);
				}
//			}
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Handle the special case where a device just became active in the network.
	 * 
	 * @param inSession
	 *            The device that just became active.
	 */
	private void networkDeviceBecameActive(INetworkDevice inNetworkDevice) {
		ContextLogging.setNetGuid(inNetworkDevice.getGuid());
		try {
			inNetworkDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED);
			inNetworkDevice.startDevice();
			for (IRadioControllerEventListener radioEventListener : mEventListeners) {
				radioEventListener.deviceActive(inNetworkDevice);
			}
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * The control command gets processed by the subclass since this is where
	 * the application-specific knowledge resides.
	 * 
	 * @param inCommand
	 * @param inSrcAddr
	 */
	private void processControlCmd(CommandControlABC inCommand, NetAddress inSrcAddr) {

		INetworkDevice device = mDeviceNetAddrMap.get(inSrcAddr);
		if (device != null) {
			ContextLogging.setNetGuid(device.getGuid());
			try {
				switch (inCommand.getExtendedCommandID().getValue()) {
					case CommandControlABC.SCAN:
						CommandControlScan scanCommand = (CommandControlScan) inCommand;
						// The scan command certainly should have a string, but if the scanner is wiggy, maybe not.
						String receivedScan = scanCommand.getCommandString();
						if (receivedScan == null)
							LOGGER.warn("Bad Scan! You may wish to check this scanner.");
						else
							device.scanCommandReceived(scanCommand.getCommandString());
						break;

					case CommandControlABC.BUTTON:
						CommandControlButton buttonCommand = (CommandControlButton) inCommand;
						device.buttonCommandReceived(buttonCommand);
						break;
						
					case CommandControlABC.ACK:
						CommandControlAck ackCommand = (CommandControlAck) inCommand;
						processAckPacket(ackCommand, inSrcAddr);
						break;

					default:
						break;
				}
			} finally {
				ContextLogging.clearNetGuid();
			}
		}

	}

	private void processAckPacket(CommandControlAck inCommand, NetAddress inSrcAddr) {
		INetworkDevice device = null;
		
		device = mDeviceNetAddrMap.get(inSrcAddr);

		if (device != null) {
			packetSchedulerService.markPacketAsAcked(device, inCommand.getAckNum());
		}
		
	}

	private NetAddress getBestNetAddressForDevice(final INetworkDevice inNetworkDevice) {
		/* DEV-639 old code was equivalent to
		mNextAddress++;
		return mNextAddress;
		*/
		
		NetGuid theGuid = inNetworkDevice.getGuid();
		// we want the last byte. Jeff says negative is ok as -110 is x97 and is interpreted in the air protocol as positive up to 255.
		byte[] theBytes = theGuid.getParamValueAsByteArray();
		int guidByteSize = NetGuid.NET_GUID_BYTES;
		NetAddress returnAddress = new NetAddress(theBytes[guidByteSize - 1]);
		// Now we must see if this is already in the map
		boolean done = false;
		boolean wentAround = false;
		while (!done) {
			// Do not allow a device to have the net address 255 which is used as the broadcast address!
			// Do not allow a device to have the net address 0 which is used as the controller address!
			if ((returnAddress.getValue() != 0xff) && (returnAddress.getValue() != 0x00) && (!mDeviceNetAddrMap.containsKey(returnAddress)))
				done = true;
			else {
				// we would like unsigned byte
				short unsignedValue = returnAddress.getValue();
				if (unsignedValue >= 255) {
					if (wentAround) { // some looping error, or we are full up. Bail
						LOGGER.error("mDeviceNetAddrMap is full! Or getBestNetAddressForDevice has loop error. Giving out duplicate of fe (254) net address");
						returnAddress.setValue((byte) 254);
						return returnAddress; // or throw?
					}
					unsignedValue = 1;
					wentAround = true;
				} else {
					unsignedValue++;
				}
				returnAddress.setValue((byte) unsignedValue);
			}
		}
		return returnAddress;
	}

	@Override
	public synchronized final void addNetworkDevice(final INetworkDevice inNetworkDevice) {
		ContextLogging.setNetGuid(inNetworkDevice.getGuid());
		try {
			// If the device has no address then assign one.
			if ((inNetworkDevice.getAddress() == null) || (inNetworkDevice.getAddress().equals(mServerAddress))) {
				NetAddress netAddressToUse = getBestNetAddressForDevice(inNetworkDevice);
				LOGGER.info("Adding device {} with network address: {} ", inNetworkDevice.getGuid(), netAddressToUse);
				inNetworkDevice.setAddress(netAddressToUse);
				// inNetworkDevice.setAddress(new NetAddress(mNextAddress++));
			}

			mDeviceGuidMap.put(inNetworkDevice.getGuid(), inNetworkDevice);
			mDeviceNetAddrMap.put(inNetworkDevice.getAddress(), inNetworkDevice);

		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	// --------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gadgetworks.flyweight.controller.IController#removeNetworkDevice(
	 * com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public synchronized final void removeNetworkDevice(INetworkDevice inNetworkDevice) {
		ContextLogging.setNetGuid(inNetworkDevice.getGuid());
		try {
			mDeviceGuidMap.remove(inNetworkDevice.getGuid());
			mDeviceNetAddrMap.remove(inNetworkDevice.getAddress());
			packetSchedulerService.removeDevice(inNetworkDevice);
		} finally {
			ContextLogging.clearNetGuid();
		}
	}

	// --------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gadgetworks.flyweight.controller.IRadioController#getNetworkDevice
	 * (com.gadgetworks.flyweight.command.NetGuid)
	 */
	@Override
	public final INetworkDevice getNetworkDevice(NetGuid inGuid) {
		return mDeviceGuidMap.get(inGuid);
	}

	@Override
	public boolean isRunning() {
		return this.mRunning;
	}

	@Override
	public NetGuid getNetGuidFromNetAddress(byte networkAddr) {
		return getNetGuidFromNetAddress(new NetAddress(networkAddr));
	}

	@Override
	public NetGuid getNetGuidFromNetAddress(NetAddress netAddress) {
		INetworkDevice device = this.mDeviceNetAddrMap.get(netAddress);
		if (device != null) {
			return device.getGuid();
		} // else
		return null;
	}
}
