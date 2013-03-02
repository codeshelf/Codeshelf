/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceEmbedded.java,v 1.5 2013/03/02 02:22:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandAssocABC;
import com.gadgetworks.flyweight.command.CommandAssocAck;
import com.gadgetworks.flyweight.command.CommandAssocReq;
import com.gadgetworks.flyweight.command.CommandAssocResp;
import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.CommandControlScan;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.TcpClientInterface;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
public class CheDeviceEmbedded implements IDevice {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(RadioController.class);

	private static final String	RECEIVER_THREAD_NAME	= "Packet Receiver";

	private static final long	CTRL_START_DELAY_MILLIS	= 200;
	private static final byte	DEVICE_VERSION			= 0x01;
	private static final byte	RESET_REASON_POWERON	= 0x00;

	private IGatewayInterface	mGatewayInterface;
	private NetworkId			mNetworkId;
	private boolean				mShouldRun;
	private NetAddress			mNetAddress;

	private String				mGUID					= "00000001";

	public CheDeviceEmbedded() {
		mNetworkId = new NetworkId(IPacket.DEFAULT_NETWORK_ID);
	}

	@Override
	public final void start() {
		mGatewayInterface = new TcpClientInterface("10.47.47.49");
		mShouldRun = true;
		startPacketReceivers();
		processScans();
	}

	@Override
	public final void stop() {
		mShouldRun = false;
		mGatewayInterface.stopInterface();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processScans() {
		Thread eventThread = new Thread(new Runnable() {
			public void run() {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				while (true) {
					try {
						if (reader.ready()) {
							String scanValue = reader.readLine();

							ICommand command = new CommandControlScan(NetEndpoint.PRIMARY_ENDPOINT, scanValue);
							IPacket packet = new Packet(command, mNetworkId, mNetAddress, new NetAddress(IPacket.GATEWAY_ADDRESS), false);
							command.setPacket(packet);
							sendPacket(packet);
						}
					} catch (IOException e) {
						LOGGER.error("", e);
					}
				}
			}
		});
		eventThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 * 	Send any packets waiting in the packet queue.
	 */
	private void startPacketReceivers() {

		Thread gwThread = new Thread(new Runnable() {
			public void run() {
				while (mShouldRun) {
					try {
						if (!mGatewayInterface.isStarted()) {
							mGatewayInterface.startInterface();
							if (mGatewayInterface.isStarted()) {
								ICommand command = new CommandAssocReq(DEVICE_VERSION, RESET_REASON_POWERON, mGUID);
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
							if (packet != null) {
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
		}, RECEIVER_THREAD_NAME + ": " + mGatewayInterface.getClass().getSimpleName());
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

		mNetAddress = inCommand.getNetAdress();
		mNetworkId = inCommand.getNetworkId();
	}

	private void processAssocAckCommand(CommandAssocAck inCommand, NetAddress inSrcAddr) {
		// The controller doesn't need to process these sub-commands.
	}

	// --------------------------------------------------------------------------
	/**
	 *  The radio controller sent this CHE a command.
	 *  @param inCommand    The control command that we want to process.  (The one just received.)
	 */
	private void processControlCmd(CommandControlABC inCommand) {

		// Figure out what kind of control sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandControlABC.MESSAGE:
				processControlMessageCommand((CommandControlMessage) inCommand);
				break;

			case CommandControlABC.SCAN:
				//processControlAckCommand((CommandControlScan) inCommand, inSrcAddr);
				break;

			default:
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processControlMessageCommand(CommandControlMessage inCommand) {
		LOGGER.info("Display message: " + inCommand.getLine1MessageStr() + " (line 1) " + inCommand.getLine2MessageStr() + " (line 2)");
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

}
