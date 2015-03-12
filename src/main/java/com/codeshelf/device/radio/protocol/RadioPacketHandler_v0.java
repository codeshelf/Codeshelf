package com.codeshelf.device.radio.protocol;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.radio.ChannelInfo;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.device.radio.RadioControllerPacketIOService;
import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;
import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.CommandAssocABC;
import com.codeshelf.flyweight.command.CommandAssocAck;
import com.codeshelf.flyweight.command.CommandAssocCheck;
import com.codeshelf.flyweight.command.CommandAssocReq;
import com.codeshelf.flyweight.command.CommandAssocResp;
import com.codeshelf.flyweight.command.CommandControlABC;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlScan;
import com.codeshelf.flyweight.command.CommandNetMgmtABC;
import com.codeshelf.flyweight.command.CommandNetMgmtCheck;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetChannelValue;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;

public class RadioPacketHandler_v0 implements IRadioPacketHandler {
	private static final Logger										LOGGER			= LoggerFactory.getLogger(RadioPacketHandler_v0.class);
	// Ack Id must start from 1
	private final AtomicInteger										mAckId			= new AtomicInteger(1);
	private static final int										ACK_QUEUE_SIZE	= 200;

	private final NetAddress										mServerAddress;
	private final ConcurrentMap<NetAddress, BlockingQueue<IPacket>>	mPendingAcksMap;
	private final Map<NetAddress, INetworkDevice>					mDeviceNetAddrMap;
	private final NetworkId											broadcastNetworkId;
	private final NetAddress										broadcastAddress;
	private final AtomicBoolean										mChannelSelected;
	private final List<IRadioControllerEventListener>				mEventListeners;
	private final ChannelInfo[]										mChannelInfo;
	private final Map<String, INetworkDevice>						mDeviceGuidMap;
	private final RadioControllerPacketIOService					packetIOService;

	public RadioPacketHandler_v0(NetAddress mServerAddress,
		ConcurrentMap<NetAddress, BlockingQueue<IPacket>> mPendingAcksMap,
		Map<NetAddress, INetworkDevice> mDeviceNetAddrMap,
		NetworkId broadcastNetworkId,
		NetAddress broadcastAddress,
		AtomicBoolean mChannelSelected,
		List<IRadioControllerEventListener> mEventListeners,
		ChannelInfo[] mChannelInfo,
		Map<String, INetworkDevice> mDeviceGuidMap,
		RadioControllerPacketIOService packetIOService) {
		super();
		this.mServerAddress = mServerAddress;
		this.mPendingAcksMap = mPendingAcksMap;
		this.mDeviceNetAddrMap = mDeviceNetAddrMap;
		this.broadcastNetworkId = broadcastNetworkId;
		this.broadcastAddress = broadcastAddress;
		this.mChannelSelected = mChannelSelected;
		this.mEventListeners = mEventListeners;
		this.mChannelInfo = mChannelInfo;
		this.mDeviceGuidMap = mDeviceGuidMap;
		this.packetIOService = packetIOService;
	}

	@Override
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
		}

		try {
			if (packet.getPacketType() == IPacket.ACK_PACKET) {
				LOGGER.debug("Packet remote ACK req RECEIVED: " + packet.toString());
				processAckPacket(packet);
			} else {
				// If the inbound packet had an ACK ID then respond with an ACK
				// ID.
				boolean shouldActOnCommand = true;
				if (packet.getAckId() != IPacket.EMPTY_ACK_ID) {
					if (device == null) {
						LOGGER.warn("Ignoring packet with device with unknown address={}", packetSourceAddress);
						return;
					} else {
						// Only act on the command if the ACK is new (i.e. >
						// last ack id)
						shouldActOnCommand = device.isAckIdNew(packet.getAckId());

						// Always respond to an ACK
						respondToAck(device, packet.getAckId(), packet.getNetworkId(), packetSourceAddress);
					}
				}

				if (shouldActOnCommand) {
					receiveCommand(packet.getCommand(), packetSourceAddress);
				} else {
					LOGGER.warn("ACKed, but did not process a packet that we acked before; {}", packet);
				}
			}
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

	private void processAckPacket(IPacket ackPacket) {
		BlockingQueue<IPacket> queue = mPendingAcksMap.get(ackPacket.getSrcAddr());
		if (queue != null) {
			for (IPacket packet : queue) {
				// IPacket packet = queue.peek();
				if (packet != null) {
					if (packet.getAckId() == ackPacket.getAckId()) {
						queue.remove(packet);
						packet.setAckData(ackPacket.getAckData());
						packet.setAckState(AckStateEnum.SUCCEEDED);
						LOGGER.info("Packet acked SUCCEEDED={}", packet);
					}
				}
			}
		}
	}

	/**
	 * @param inAckId
	 * @param inNetId
	 * @param inSrcAddr
	 */
	protected void respondToAck(INetworkDevice device, final byte inAckId, final NetworkId inNetId, final NetAddress inSrcAddr) {
		ContextLogging.setNetGuid(device.getGuid());
		try {

			LOGGER.info("ACKing packet: ackId={}; netId={}; srcAddr={}", inAckId, inNetId, inSrcAddr);

			device.setLastAckId(inAckId);
			//device.getGuid().getHexStringNoPrefix()
			CommandAssocAck ackCmd = new CommandAssocAck("00000000",
				new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, (byte) 0));

			IPacket ackPacket = new Packet(ackCmd, inNetId, mServerAddress, inSrcAddr, false);
			ackCmd.setPacket(ackPacket);
			ackPacket.setAckId(inAckId);
			handleOutboundPacket(ackPacket);

		} finally {
			ContextLogging.clearNetGuid();
		}

	}

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
						device.scanCommandReceived(scanCommand.getCommandString());
						break;

					case CommandControlABC.BUTTON:
						CommandControlButton buttonCommand = (CommandControlButton) inCommand;
						device.buttonCommandReceived(buttonCommand);
						break;

					default:
						break;
				}
			} finally {
				ContextLogging.clearNetGuid();
			}
		}

	}

	public final void receiveCommand(final ICommand inCommand, final NetAddress inSrcAddr) {

		if (inCommand != null) {
			switch (inCommand.getCommandTypeEnum()) {

				case NETMGMT:
					processNetworkMgmtCmd((CommandNetMgmtABC) inCommand, inSrcAddr);
					break;

				case ASSOC:
					if (mChannelSelected.get()) {
						CommandAssocABC assocCmd = (CommandAssocABC) inCommand;
						processAssocCmd(assocCmd, inSrcAddr);
					}
					break;

				case CONTROL:
					if (mChannelSelected.get()) {
						processControlCmd((CommandControlABC) inCommand, inSrcAddr);
					}
					break;
				default:
					break;
			}

		}
	}

	/**
	 * @param inCommand
	 *            The wake command the we want to process. (The one just
	 *            received.)
	 */
	private void processNetworkMgmtCmd(CommandNetMgmtABC inCommand, NetAddress inSrcAddr) {

		// Figure out what kind of network management sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandNetMgmtABC.NETSETUP_COMMAND:
				//Do Nothing
				break;

			case CommandNetMgmtABC.NETCHECK_COMMAND:
				processNetworkCheckCommand((CommandNetMgmtCheck) inCommand, inSrcAddr);
				break;

			case CommandNetMgmtABC.NETINTFTEST_COMMAND:
				//Do Nothing
				break;

			default:
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processNetworkCheckCommand(CommandNetMgmtCheck inCommand, NetAddress inSrcAddr) {

		NetworkId networkId = inCommand.getNetworkId();
		if (inCommand.getNetCheckType() == CommandNetMgmtCheck.NETCHECK_REQ) {
			// This is a net-check request.

			// If it's an all-network broadcast, or a request to our network
			// then respond.
			boolean shouldRespond = false;
			String responseGUID = "";
			if (inCommand.getNetCheckType() == CommandNetMgmtCheck.NETCHECK_RESP) {
				// For a broadcast request we respond with the private GUID.
				// This will cause the gateway (dongle)
				// to insert its own GUID before transmitting it to the air.
				shouldRespond = true;
				responseGUID = RadioController.PRIVATE_GUID;
			} else if (networkId.equals(packetIOService.getNetworkId())) {
				// For a network-specific request we respond with the GUID of
				// the requester.
				shouldRespond = true;
				responseGUID = inCommand.getGUID();
			}

			if (shouldRespond) {
				// If this is a network check for us then response back to the
				// sender.
				// Send a network check response command back to the sender.
				CommandNetMgmtCheck netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_RESP,
					inCommand.getNetworkId(),
					responseGUID,
					inCommand.getChannel(),
					new NetChannelValue((byte) 0),
					new NetChannelValue((byte) 0));

				IPacket packet = new Packet(netCheck, packetIOService.getNetworkId(), mServerAddress, broadcastAddress, false);
				inCommand.setPacket(packet);
				handleOutboundPacket(packet);
			}
		} else {
			// This is a net-check response.
			if (networkId.getValue() == IPacket.BROADCAST_NETWORK_ID) {

				// If this is a all-network net-check broadcast response then
				// book keep the values.

				// Find the ChannelInfo instance for this channel.
				byte channel = inCommand.getChannel();

				if (inCommand.getGUID().equals(RadioController.PRIVATE_GUID)) {
					// This came from the gateway (dongle) directly.
					// The gateway (dongle) will have inserted an energy detect
					// value for the channel.
					mChannelInfo[channel].setChannelEnergy(inCommand.getChannelEnergy().getValue());
				} else {
					// This came from another controller on the same channel, so
					// increment the number of controllers on the channel.
					mChannelInfo[channel].incrementControllerCount();
				}
			} else {
				// The controller never receives network-specific net-check
				// responses.
			}
		}
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
					//Do nothing
					break;

				case CommandAssocABC.ASSOC_CHECK_COMMAND:
					processAssocCheckCommand((CommandAssocCheck) inCommand, inSrcAddr);
					break;

				case CommandAssocABC.ASSOC_ACK_COMMAND:
					//Do nothing
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
			INetworkDevice foundDevice = mDeviceGuidMap.get(inCommand.getGUID());

			if (foundDevice != null) {
				ContextLogging.setNetGuid(foundDevice.getGuid());
				try {

					foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.SETUP);
					foundDevice.setHardwareVersion(inCommand.getHardwareVersion());
					foundDevice.setFirmwareVersion(inCommand.getFirmwareVersion());

					LOGGER.info("Device associated={}; Req={}", foundDevice.getGuid().getHexStringNoPrefix(), inCommand);
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
					// LOGGER.info("----------------------------------------------------");

					// Create and send an assign command to the remote that just
					// woke up.
					CommandAssocResp assignCmd = new CommandAssocResp(uid,
						packetIOService.getNetworkId(),
						foundDevice.getAddress(),
						foundDevice.getSleepSeconds());

					IPacket packet = new Packet(assignCmd, broadcastNetworkId, mServerAddress, broadcastAddress, false);
					inCommand.setPacket(packet);
					handleOutboundPacket(packet);

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
	private void processAssocCheckCommand(CommandAssocCheck inCommand, NetAddress inSrcAddr) {
		INetworkDevice foundDevice = mDeviceGuidMap.get(inCommand.getGUID());

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
					LOGGER.info("AssocCheck - NOT ASSOC: state was: {}", foundDevice.getDeviceStateEnum());
				} else if (foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.ASSIGN_SENT)) {

					Queue<IPacket> pendingAcks = mPendingAcksMap.get(inSrcAddr);
					if (pendingAcks != null && !pendingAcks.isEmpty()) {
						LOGGER.info("Clearing pending acks queue for newly associated device={} size={}",
							foundDevice,
							pendingAcks.size());
						pendingAcks.clear();
					}

					networkDeviceBecameActive(foundDevice);
				} else if (!foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.STARTED)) {
					status = CommandAssocAck.IS_NOT_ASSOCIATED;
					LOGGER.info("AssocCheck - NOT ASSOC: state was: {}", foundDevice.getDeviceStateEnum());
				}

				// If the found device has the wrong GUID then we have the wrong
				// device.
				// (This could be two matching network IDs on the same channel.
				// This could be a serious flaw in the network protocol.)
				if (!foundDevice.getGuid().toString().equalsIgnoreCase("0x" + inCommand.getGUID())) {
					LOGGER.info("AssocCheck - NOT ASSOC: GUID mismatch: {} and {}", foundDevice.getGuid(), inCommand.getGUID());
					status = CommandAssocAck.IS_NOT_ASSOCIATED;
				}

				// Create and send an ack command to the remote that we think is
				// in the running state.
				ackCmd = new CommandAssocAck(inCommand.getGUID(), new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, status));

				// Send the command.
				IPacket packet = new Packet(ackCmd, packetIOService.getNetworkId(), mServerAddress, inSrcAddr, false);
				inCommand.setPacket(packet);
				handleOutboundPacket(packet);
			} finally {
				ContextLogging.clearNetGuid();
			}
		}
	}

	@Override
	public void handleOutboundPacket(IPacket packet) {
		/*
		 * Certain commands can request the remote to ACK (to guarantee that
		 * the command arrived). Most packets will not contain a command
		 * that requests and ACK, so normally packets will just get sent
		 * right here.
		 * 
		 * If a packet's command requires an ACK then we perform the
		 * following steps:
		 * 
		 * - Check that the packet address is something other than a
		 * broadcast network ID or network address. (We don't support
		 * broadcast ACK.) - If a packet queue does not exist for the
		 * destination then: 1. Create a packet queue for the destination.
		 * 2. Put the packet in the queue. 3. Send the packet. - If a packet
		 * queue does exist for the destination then just put the packet in
		 * it.
		 */
		if ((packet.isAckRequested()) && (packet.getNetworkId().getValue() != (IPacket.BROADCAST_NETWORK_ID))
				&& (packet.getDstAddr().getValue() != (IPacket.BROADCAST_ADDRESS))) {

			// If we're pending an ACK then assign an ACK ID.
			int nextAckId = mAckId.getAndIncrement();
			while (nextAckId > Byte.MAX_VALUE) {
				mAckId.compareAndSet(nextAckId, 1);
				nextAckId = mAckId.get();
			}

			packet.setAckId((byte) nextAckId);
			packet.setAckState(AckStateEnum.PENDING);

			// Add the command to the pending ACKs map, and increment the command ID counter.
			BlockingQueue<IPacket> queue = mPendingAcksMap.get(packet.getDstAddr());
			if (queue == null) {
				queue = new ArrayBlockingQueue<IPacket>(ACK_QUEUE_SIZE);
				BlockingQueue<IPacket> existingQueue = mPendingAcksMap.putIfAbsent(packet.getDstAddr(), queue);
				if (existingQueue != null) {
					queue = existingQueue;
				}
			}

			// If the ACK queue is too full then pause.
			boolean success = queue.offer(packet);
			while (!success) {
				// Given an ACK timeout of 20ms and a read frequency of 20ms. If the max queue size is over 20 (and it should be)
				// then we can drop the earlier packets since they should be timed out anyway.
				IPacket packetToDrop = queue.poll();
				LOGGER.warn("Dropping packet because pendingAcksMap is full. Size={}; DroppedPacket={}", queue.size(), packetToDrop);
				success = queue.offer(packet);
			}
			LOGGER.debug("Packet is now pending ACK: {}", packet);
		} else {
			packetIOService.handleOutboundPacket(packet);
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
			//Add device to 

			inNetworkDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED);
			inNetworkDevice.startDevice();
			for (IRadioControllerEventListener radioEventListener : mEventListeners) {
				radioEventListener.deviceActive(inNetworkDevice);
			}
		} finally {
			ContextLogging.clearNetGuid();
		}

	}

}
