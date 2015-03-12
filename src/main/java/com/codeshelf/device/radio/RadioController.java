/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: RadioController.java,v 1.17 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.device.radio;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.radio.protocol.IRadioPacketHandler;
import com.codeshelf.device.radio.protocol.RadioPacketHandler_v0;
import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.CommandNetMgmtCheck;
import com.codeshelf.flyweight.command.CommandNetMgmtSetup;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetChannelValue;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.FTDIInterface;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

/**
 * IEEE_802_15_4_RadioController
 * 
 * @author jeffw, saba
 */

public class RadioController implements IRadioController {
	private static final Logger										LOGGER								= LoggerFactory.getLogger(RadioController.class);

	public static final String										PRIVATE_GUID						= "00000000";
	public static final String										VIRTUAL_GUID						= "%%%%%%%%";

	public static final byte										MAX_CHANNELS						= 16;
	public static final byte										NO_PREFERRED_CHANNEL				= (byte) 255;
	public static final String										NO_PREFERRED_CHANNEL_TEXT			= "None";

	private static final String										CONTROLLER_THREAD_NAME				= "Radio Controller";
	private static final String										STARTER_THREAD_NAME					= "Intferface Starter";

	private static final int										STARTER_THREAD_PRIORITY				= Thread.NORM_PRIORITY;

	private static final long										CTRL_START_DELAY_MILLIS				= 5;
	private static final long										NETCHECK_DELAY_MILLIS				= 250;

	private static final long										ACK_TIMEOUT_MILLIS					= 20;
	private static final int										ACK_SEND_RETRY_COUNT				= 20;
	private static final long										MAX_PACKET_AGE_MILLIS				= 2000;

	private static final long										BACKGROUND_SERVICE_DELAY_MS			= 20;

	private static final long										BROADCAST_RATE_MILLIS				= 750;

	private static final int										MAX_CHANNEL_VALUE					= 255;
	private static final long										PACKET_SPACING_MILLIS				= 20;

	@Getter
	private final IGatewayInterface									gatewayInterface;

	private volatile boolean										mShouldRun							= true;

	private final NetAddress										mServerAddress						= new NetAddress(IPacket.GATEWAY_ADDRESS);

	// We iterate over this list often, but write almost never. It needs to be thread-safe so we chose to make writes slow and reads lock-free.
	private final List<IRadioControllerEventListener>				mEventListeners						= new CopyOnWriteArrayList<>();

	// This does not need to be synchronized because it is only ever used by a single thread in the packet handler service
	// processNetworkCheckCommand only accesses this array for the broadcast network address.
	private final ChannelInfo[]										mChannelInfo						= new ChannelInfo[MAX_CHANNELS];

	private final AtomicBoolean										mChannelSelected					= new AtomicBoolean(false);

	// These 2 variables are only every modified in a synchronized method but we make volatile so it is visable to other threads.
	private volatile byte											mPreferredChannel;
	private volatile byte											mRadioChannel						= 0;

	private Thread													mControllerThread;

	// Background service executor
	private final ScheduledExecutorService							backgroundService					= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("radio-bckgrnd-thread")
																											.build());
	private final Map<String, INetworkDevice>						mDeviceGuidMap						= Maps.newConcurrentMap();
	private final Map<NetAddress, INetworkDevice>					mDeviceNetAddrMap					= Maps.newConcurrentMap();

	private volatile boolean										mRunning							= false;

	// Services
	private final RadioControllerPacketHandlerService				packetHandlerService;
	private final RadioControllerPacketIOService					packetIOService;
	private final RadioControllerBroadcastService					broadcastService;

	private final ConcurrentMap<NetAddress, BlockingQueue<IPacket>>	mPendingAcksMap						= Maps.newConcurrentMap();

	//This map is initialized in the constructor
	private final Map<Byte, IRadioPacketHandler>					mProtocolVersionToPacketHandlerMap	= Maps.newHashMap();

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

		NetworkId broadcastNetworkId = new NetworkId(IPacket.BROADCAST_NETWORK_ID);
		NetAddress broadcastAddress = new NetAddress(IPacket.BROADCAST_ADDRESS);

		// Create Services
		this.packetHandlerService = new RadioControllerPacketHandlerService(mProtocolVersionToPacketHandlerMap,
			mDeviceNetAddrMap,
			mDeviceGuidMap);
		this.packetIOService = new RadioControllerPacketIOService(inGatewayInterface,
			packetHandlerService,
			broadcastAddress,
			PACKET_SPACING_MILLIS);
		this.broadcastService = new RadioControllerBroadcastService(broadcastNetworkId,
			broadcastAddress,
			this,
			BROADCAST_RATE_MILLIS);

		//Protocol to handler map

		//Version 0
		mProtocolVersionToPacketHandlerMap.put((byte) 0, new RadioPacketHandler_v0(broadcastAddress,
			mPendingAcksMap,
			mDeviceNetAddrMap,
			broadcastNetworkId,
			broadcastAddress,
			mChannelSelected,
			mEventListeners,
			mChannelInfo,
			mDeviceGuidMap,
			packetIOService));
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
		backgroundService.shutdown();
		packetIOService.stop();
		packetHandlerService.shutdown();

		// Stop all of the interfaces.
		gatewayInterface.stopInterface();

		broadcastService.stop();

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

			// Start IO Service
			packetIOService.start();

			// Kick off the background event processing
			backgroundService.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					processEvents();
				}
			}, 0, BACKGROUND_SERVICE_DELAY_MS, TimeUnit.MILLISECONDS);

			broadcastService.start();
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
			mChannelSelected.set(true);
			mRadioChannel = inChannel;
			CommandNetMgmtSetup netSetupCmd = new CommandNetMgmtSetup(packetIOService.getNetworkId(), mRadioChannel);
			if (gatewayInterface instanceof FTDIInterface) {
				// Net mgmt commands only get sent to the FTDI-controlled radio network.
				sendCommand(netSetupCmd, broadcastService.getBroadcastAddress(), false);
			}
			LOGGER.info("Radio channel={}", inChannel);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Process the events that occur in the controller.
	 */
	private void processEvents() {

		// The controller should process events continuously until the application wants to quit/exit.
		if (mShouldRun) {
			try {
				// Check if there are any pending ACK packets that need resending. Also consider the case where there is more than one packet
				// destined for a remote. (Only re-send one packet per network device, so that packets arrive in order.)
				if (mPendingAcksMap.size() > 0) {
					for (BlockingQueue<IPacket> queue : mPendingAcksMap.values()) {
						// Look at the next packet in the queue.
						IPacket packet = queue.peek();
						if (packet != null) {
							INetworkDevice device = this.mDeviceNetAddrMap.get(packet.getDstAddr());
							if (device != null) {
								ContextLogging.setNetGuid(device.getGuid());
							}
							try {
								// If we've timed out waiting for an ACK then resend the command.
								if (System.currentTimeMillis() - packet.getSentTimeMillis() > ACK_TIMEOUT_MILLIS) {
									if ((packet.getSendCount() < ACK_SEND_RETRY_COUNT)
											&& ((System.currentTimeMillis() - packet.getCreateTimeMillis() < MAX_PACKET_AGE_MILLIS))) {
										//Resend packet
										packetIOService.handleOutboundPacket(packet);
									} else {
										// If we've exceeded the retry time then remove the packet.
										// We should probably mark the device lost and clear the queue.
										packet.setAckState(AckStateEnum.NO_RESPONSE);
										queue.remove();
										LOGGER.info("Packet acked NO_RESPONSE {}. QueueSize={}", packet, queue.size());
									}
								}

							} finally {
								ContextLogging.clearNetGuid();
							}
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error("ProcessEvents Error ", e);
			}

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

				/*
				 * The process for selecting a channel goes like this:
				 * 
				 * 1. Choose a channel. 2. Send a net-check command on that
				 * channel. 3. Wait for responses. (One response will come
				 * directly from the gateway (dongle) with a GUID = '00000000'
				 * 4. Repeat until all channels have been checked. 5. Choose the
				 * best channel. 6. Send a net-setup command directly to the
				 * dongle to tell it the channel ID.
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
						broadcastService.getBroadcastNetworkId(),
						PRIVATE_GUID,
						channel,
						new NetChannelValue((byte) 0),
						new NetChannelValue((byte) 0));
					sendCommand(netCheck, broadcastService.getBroadcastAddress(), false);

					// Wait NETCHECK delay millis before sending the next
					// net-check.
					try {
						Thread.sleep(NETCHECK_DELAY_MILLIS);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}
				}

				// At this point the ChannelInfo structures will contain all of
				// the information we need to make a channel choice.
				mPreferredChannel = 0;
				// First find the channel with the lowest number of controllers.
				for (byte channel = 0; channel < MAX_CHANNELS; channel++) {
					if (mPreferredChannel != channel) {
						// The most important test is the number of controllers
						// already on a channel.
						if (mChannelInfo[channel].getControllerCount() < mChannelInfo[mPreferredChannel].getControllerCount()) {
							mPreferredChannel = channel;
						}
					}
				}

				// By the above we will have picked the first channel with the
				// lowest number of controllers.
				// There may be more than one channel with this same number of
				// controllers.
				// So now search those equal channels for the one with the
				// lowest energy.
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
	/*
	 * (non-Javadoc)
	 */
	@Override
	public void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommand(inCommand, packetIOService.getNetworkId(), inDstAddr, inAckRequested);
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
	private void sendCommand(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, boolean inAckRequested) {
		INetworkDevice device = this.mDeviceNetAddrMap.get(inDstAddr);
		if (device != null) {
			ContextLogging.setNetGuid(device.getGuid());
		}
		try {
			IPacket packet = new Packet(inCommand, inNetworkId, mServerAddress, inDstAddr, inAckRequested);
			inCommand.setPacket(packet);

			if (inDstAddr.getValue() == (IPacket.BROADCAST_ADDRESS)) {
				//If we're broadcasting just send it out
				packetIOService.handleOutboundPacket(packet);
			} else {
				//Now we can lookup the INetworkDevice from the NetAddr
				//Make sure device is registered
				if (device == null) {
					LOGGER.warn("Cannot send packet to unregistered (null) packet={}", packet);
					return;
				}

				//Check version
				Byte radioProtocolVersion = device.getRadioProtocolVersion();

				if (radioProtocolVersion == null) {
					LOGGER.warn("Cannot send packet to unregistered (with no radio protocol version) packet={}", packet);
					return;
				}

				IRadioPacketHandler handler = mProtocolVersionToPacketHandlerMap.get(radioProtocolVersion);

				if (handler == null) {
					LOGGER.warn("Cannot send packet to device with an unsupported radio protocol verions. packet={}", packet);
					return;
				}

				//Handle Packet
				handler.handleInboundPacket(packet);
			}

		} finally {
			ContextLogging.clearNetGuid();
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

	private byte getBestNetAddressForDevice(final INetworkDevice inNetworkDevice) {
		/* DEV-639 old code was equivalent to
		mNextAddress++;
		return mNextAddress;
		*/

		NetGuid theGuid = inNetworkDevice.getGuid();
		// we want the last byte. Jeff says negative is ok as -110 is x97 and is interpreted in the air protocol as positive up to 255.
		byte[] theBytes = theGuid.getParamValueAsByteArray();
		int guidByteSize = NetGuid.NET_GUID_BYTES;
		byte returnByte = theBytes[guidByteSize - 1];
		// Now we must see if this is already in the map
		boolean done = false;
		boolean wentAround = false;
		while (!done) {
			if (!mDeviceNetAddrMap.containsKey(returnByte))
				done = true;
			else {
				// we would like unsigned byte
				int unsignedValue = (returnByte & 0xff);
				if (unsignedValue >= 255) {
					if (wentAround) { // some looping error. Bail
						LOGGER.error("getBestNetAddressForDevice has loop error");
						return 127; // or throw?
					}
					unsignedValue = 1;
					wentAround = true;
				} else {
					unsignedValue++;
				}
				returnByte = (byte) unsignedValue;
			}
		}
		return returnByte;
	}

	@Override
	public synchronized final void addNetworkDevice(final INetworkDevice inNetworkDevice) {
		ContextLogging.setNetGuid(inNetworkDevice.getGuid());
		try {
			// If the device has no address then assign one.
			if ((inNetworkDevice.getAddress() == null) || (inNetworkDevice.getAddress().equals(mServerAddress))) {
				byte netAddressToUse = getBestNetAddressForDevice(inNetworkDevice);
				LOGGER.info("adding network address " + netAddressToUse);
				inNetworkDevice.setAddress(new NetAddress(netAddressToUse));
				// inNetworkDevice.setAddress(new NetAddress(mNextAddress++));
			}

			mDeviceGuidMap.put(inNetworkDevice.getGuid().getHexStringNoPrefix(), inNetworkDevice);
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
			mDeviceGuidMap.remove(inNetworkDevice.getGuid().getHexStringNoPrefix());
			mDeviceNetAddrMap.remove(inNetworkDevice.getAddress());
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
		return mDeviceGuidMap.get(inGuid.getHexStringNoPrefix());
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
