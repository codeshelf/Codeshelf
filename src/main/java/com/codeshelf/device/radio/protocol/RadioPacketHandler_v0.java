package com.codeshelf.device.radio.protocol;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.DeviceRestartCauseEnum;
import com.codeshelf.device.radio.RadioControllerBroadcastService;
import com.codeshelf.device.radio.RadioControllerPacketIOService;
import com.codeshelf.device.radio.RadioControllerPacketSchedulerService;
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
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;

public class RadioPacketHandler_v0 implements IRadioPacketHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RadioPacketHandler_v0.class);

	private RadioControllerPacketSchedulerService	packetScheduler;
	private final RadioControllerPacketIOService	packetIOService;
	private final RadioControllerBroadcastService	broadcastService;

	private boolean										mChannelSelected;
	private final Map<NetGuid, INetworkDevice>			mDeviceGuidMap;
	private final Map<NetAddress, INetworkDevice>		mDeviceNetAddrMap;
	private final List<IRadioControllerEventListener>	mEventListeners;
	
	private final NetAddress							mServerAddress;

	public RadioPacketHandler_v0(RadioControllerPacketSchedulerService inPacketScheduler,
		RadioControllerPacketIOService packetIOService,
		RadioControllerBroadcastService broadcastService,
		boolean channelSelected,
		Map<NetGuid, INetworkDevice> DeviceGuidMap,
		Map<NetAddress, INetworkDevice> DeviceNetAddrMap,
		List<IRadioControllerEventListener> eventListeners,
		NetAddress serverAddress) {

		packetScheduler = inPacketScheduler;
		this.packetIOService = packetIOService;
		this.broadcastService = broadcastService;
		
		mChannelSelected = channelSelected;
		mDeviceGuidMap = DeviceGuidMap;
		mDeviceNetAddrMap = DeviceNetAddrMap;
		mEventListeners = eventListeners;
		mServerAddress = serverAddress;
	}

	@Override
	public void handleInboundPacket(IPacket packet) {
		// TODO Auto-generated method stub
		receiveCommand(packet.getCommand(), packet.getSrcAddr());
	}

	@Override
	public void handleOutboundPacket(IPacket packet) {
		// TODO Auto-generated method stub

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
			LOGGER.error("RadioController receiveCommand: Command was null", new Exception()); // give the stack trace
		}
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
				//processNetworkSetupCommand((CommandNetMgmtSetup) inCommand, inSrcAddr); //TODO fix
				break;

			case CommandNetMgmtABC.NETCHECK_COMMAND:
				break;

			case CommandNetMgmtABC.NETINTFTEST_COMMAND:
				//processNetworkIntfTestCommand((CommandNetMgmtIntfTest) inCommand, inSrcAddr); //TODO fix
				break;

			default:
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
		// Already in guid context from handleInbountPacket, so do not need rememberThenSetNetGuid
		// String rememberedGuid = ContextLogging.rememberThenSetNetGuid(inCommand.getGUID());
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
			// ContextLogging.restoreNetGuid(rememberedGuid);
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
			// Already in guid context from handleInbountPacket, so do not need rememberThenSetNetGuid
			//	String rememberedGuid = ContextLogging.rememberThenSetNetGuid(device.getGuid());
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
				// ContextLogging.restoreNetGuid(rememberedGuid);
			}
		}

	}

	private void processAckPacket(CommandControlAck inCommand, NetAddress inSrcAddr) {
		INetworkDevice device = null;

		device = mDeviceNetAddrMap.get(inSrcAddr);

		if (device != null) {
			packetScheduler.markPacketAsAcked(device, inCommand.getAckNum());
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
				String rememberedGuid = ContextLogging.rememberThenSetNetGuid(foundDevice.getGuid());
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
					packetScheduler.clearDevicePacketQueue(foundDevice);

					CommandAssocResp assignCmd = new CommandAssocResp(uid,
						packetIOService.getNetworkId(), //TODO seems pointless to pass object for just this
						foundDevice.getAddress(),
						foundDevice.getSleepSeconds());

					assignCmd.setScannerType(foundDevice.getScannerTypeCode());

					sendAssociationCommand(assignCmd,
						broadcastService.getBroadcastNetworkId(), //TODO seems pointless to pass object for just this
						broadcastService.getBroadcastAddress(), //TODO seems pointless to pass object for just this
						foundDevice.getGuid(),
						false);

					foundDevice.setDeviceStateEnum(NetworkDeviceStateEnum.ASSIGN_SENT);
				} finally {
					ContextLogging.restoreNetGuid(rememberedGuid);
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

		// Saw this OutOfRangeException several times at PfSWeb
		INetworkDevice foundDevice = null;
		try {
			foundDevice = mDeviceGuidMap.get(new NetGuid("0x" + uid));
		} catch (OutOfRangeException e) {
			LOGGER.error("Bad guid: {} in associate command", uid);
			return;
		}

		if (foundDevice != null) {
			// Already in guid context from handleInbountPacket, so do not need rememberThenSetNetGuid
			// String rememberedGuid = ContextLogging.rememberThenSetNetGuid(foundDevice.getGuid());
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
					packetScheduler.clearDevicePacketQueue(foundDevice);
					deviceBecameActive = true;
				} else if (foundDevice.getDeviceStateEnum().equals(NetworkDeviceStateEnum.STARTED)) {
					// Device did not receive our association ack message. Resend.
					packetScheduler.clearDevicePacketQueue(foundDevice);
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
					networkDeviceBecameActive(foundDevice, restartEnum);
				}
			} finally {
				// ContextLogging.restoreNetGuid(rememberedGuid);
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

		packetScheduler.addCommandPacketToSchedule(packet, device);

	}
	
	public final void sendCommandFrontQueue(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		sendCommandFrontQueue(inCommand, packetIOService.getNetworkId(), inDstAddr, inAckRequested);
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

			packetScheduler.addAckPacketToSchedule(packet, device);
		}
	}

	private void networkDeviceBecameActive(INetworkDevice inNetworkDevice, DeviceRestartCauseEnum restartEnum) {
		// Already in guid context from handleInbountPacket, so do not need rememberThenSetNetGuid
		// String rememberedGuid = ContextLogging.rememberThenSetNetGuid(inNetworkDevice.getGuid());
		LOGGER.info("networkDeviceBecameActive called");
		try {
			inNetworkDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED);
			inNetworkDevice.startDevice(restartEnum);
			for (IRadioControllerEventListener radioEventListener : mEventListeners) {
				radioEventListener.deviceActive(inNetworkDevice);
			}
		} finally {
			// ContextLogging.restoreNetGuid(rememberedGuid);
		}

	}
	
	// --------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 */
	public final void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
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

			packetScheduler.addCommandPacketToSchedule(packet, device);
		}
	}
}
