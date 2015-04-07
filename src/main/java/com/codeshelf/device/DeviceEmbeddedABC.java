/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: DeviceEmbeddedABC.java,v 1.6 2013/05/03 05:12:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandAssocABC;
import com.codeshelf.flyweight.command.CommandAssocAck;
import com.codeshelf.flyweight.command.CommandAssocReq;
import com.codeshelf.flyweight.command.CommandAssocResp;
import com.codeshelf.flyweight.command.CommandControlABC;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.flyweight.controller.TcpClientInterface;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
@Deprecated
@Accessors(prefix = "m")
public abstract class DeviceEmbeddedABC implements IEmbeddedDevice {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	private static final String	RECEIVER_THREAD_NAME	= "Packet Receiver";

	private static final long	CTRL_START_DELAY_MILLIS	= 200;

	private static final String	HARDWARE_VERSION		= "0.0";
	private static final String	FIRMWARE_VERSION		= "0.0";
	private static final byte	RADIOPROTOCOL_VERSION	= 0x01;

	private static final byte	RESET_REASON_POWERON	= 0x00;

	private IGatewayInterface	mGatewayInterface;
	@Getter(value = AccessLevel.PROTECTED)
	private NetworkId			mNetworkId;
	@Getter(value = AccessLevel.PROTECTED)
	private NetAddress			mNetAddress;
	@Getter(value = AccessLevel.PROTECTED)
	//	@Setter(value = AccessLevel.PROTECTED)
	private boolean				mShouldRun;

	private String				mGUID;
	private String				mServerName;

	public DeviceEmbeddedABC(final String inGUID, final String inServerName) {
		mGUID = inGUID;
		mServerName = inServerName;
		mNetworkId = new NetworkId(IPacket.DEFAULT_NETWORK_ID);
	}

	abstract void processControlCmd(CommandControlABC inCommand);

	abstract void doStart();

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.IEmbeddedDevice#start()
	 */
	@Override
	public final void start() {
		mGatewayInterface = new TcpClientInterface(mServerName);
		mShouldRun = true;

		doStart();

		startPacketReceivers();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.IEmbeddedDevice#stop()
	 */
	@Override
	public final void stop() {
		mShouldRun = false;
		mGatewayInterface.stopInterface();
	}

	// --------------------------------------------------------------------------
	/**
	 * 	Send any packets waiting in the packet queue.
	 */
	private void startPacketReceivers() {

		Thread gwThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (mShouldRun) {
					try {
						if (!mGatewayInterface.isStarted()) {
							mGatewayInterface.startInterface();
							if (mGatewayInterface.isStarted()) {
								ICommand command = new CommandAssocReq(HARDWARE_VERSION,
									FIRMWARE_VERSION,
									RADIOPROTOCOL_VERSION,
									RESET_REASON_POWERON,
									mGUID);
								IPacket packet = new Packet(command,
									new NetworkId(IPacket.BROADCAST_NETWORK_ID),
									new NetAddress(IPacket.BROADCAST_ADDRESS),
									new NetAddress(IPacket.GATEWAY_ADDRESS),
									false);
								command.setPacket(packet);
								sendPacket(packet);
							} else {
								try {
									Thread.sleep(CTRL_START_DELAY_MILLIS);
								} catch (InterruptedException e) {
									LOGGER.error("", e);
								}
							}
						} else {
							IPacket packet = mGatewayInterface.receivePacket(mNetworkId);
							if ((packet != null)
									&& ((packet.getDstAddr().equals(new NetAddress(IPacket.BROADCAST_ADDRESS)) || (packet.getDstAddr().equals(mNetAddress))))) {
								//putPacketInRcvQueue(packet);
								if (packet.getPacketType() == IPacket.ACK_PACKET) {
									LOGGER.info("Packet acked RECEIVED: " + packet.toString());
									//									processAckPacket(packet);
								} else {
									receiveCommand(packet.getCommand(), packet.getSrcAddr());
								}
							}
						}
					} catch (RuntimeException e) {
						LOGGER.error("", e);
					}
				}
			}
		},
			RECEIVER_THREAD_NAME + ": " + mGatewayInterface.getClass().getSimpleName());
		gwThread.setPriority(Thread.MIN_PRIORITY);
		gwThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 * When the controller gets a new command it arrives here.
	 * 
	 *  @param inCommand   The command just received.
	 *  @param inSrcAddr   The address is was received from.
	 */
	private void receiveCommand(final ICommand inCommand, final NetAddress inSrcAddr) {

		if (inCommand != null) {

			switch (inCommand.getCommandTypeEnum()) {

				case NETMGMT:
					//processNetworkMgmtCmd((CommandNetMgmtABC) inCommand, inSrcAddr);
					break;

				case ASSOC:
					processAssocCmd((CommandAssocABC) inCommand, inSrcAddr);
					break;

				case CONTROL:
					processControlCmd((CommandControlABC) inCommand);
					break;
				default:
					break;
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
			case CommandAssocABC.ASSOC_RESP_COMMAND:
				processAssocRespCommand((CommandAssocResp) inCommand, inSrcAddr);
				break;

			case CommandAssocABC.ASSOC_ACK_COMMAND:
				processAssocAckCommand((CommandAssocAck) inCommand, inSrcAddr);
				break;

			default:
		}
	}

	private void processAssocRespCommand(CommandAssocResp inCommand, NetAddress inSrcAddr) {
		// If this is our assoc request then set our assigned address and network.
		if (inCommand.getGUID().equals(mGUID)) {
			mNetAddress = inCommand.getNetAdress();
			mNetworkId = inCommand.getNetworkId();
		}
	}

	private void processAssocAckCommand(CommandAssocAck inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPacket
	 */
	protected void sendPacket(IPacket inPacket) {
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

}
