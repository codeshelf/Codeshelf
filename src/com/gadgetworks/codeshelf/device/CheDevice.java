/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.command.CommandAssocReq;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;
import com.gadgetworks.flyweight.controller.DeviceController;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.TcpClientInterface;

/**
 * @author jeffw
 *
 */
public class CheDevice implements IDevice {

	private static final Log	LOGGER					= LogFactory.getLog(DeviceController.class);

	private static final String	RECEIVER_THREAD_NAME	= "Packet Receiver";

	private static final long	CTRL_START_DELAY_MILLIS	= 200;

	private IGatewayInterface	mGatewayInterface;
	private NetworkId			mNetworkId;
	private boolean				mShouldRun;

	public CheDevice() {
		mNetworkId = IPacket.DEFAULT_NETWORK_ID;
	}

	@Override
	public final void start() {
		mGatewayInterface = new TcpClientInterface();
		mShouldRun = true;
		mGatewayInterface.startInterface();
		startPacketReceivers();

		ICommand command = new CommandAssocReq((byte) 0x01, (byte) 0x00, "00000001");
		IPacket packet = new Packet(command, IPacket.BROADCAST_NETWORK_ID, IPacket.BROADCAST_ADDRESS, IPacket.GATEWAY_ADDRESS, false);
		command.setPacket(packet);
		sendPacket(packet);
	}

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
			public void run() {
				while (mShouldRun) {
					try {
						if (mGatewayInterface.isStarted()) {
							IPacket packet = mGatewayInterface.receivePacket(mNetworkId);
							if (packet != null) {
								//putPacketInRcvQueue(packet);
								if (packet.getPacketType() == IPacket.ACK_PACKET) {
									LOGGER.info("Packet acked RECEIVED: " + packet.toString());
									//									processAckPacket(packet);
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
					//					processNetworkMgmtCmd((CommandNetMgmtABC) inCommand, inNetworkType, inSrcAddr);
					break;

				case ASSOC:
					//					if (mChannelSelected) {
					//						processAssocCmd((CommandAssocABC) inCommand, inNetworkType, inSrcAddr);
					//					}
					break;

				case CONTROL:
					//					if (mChannelSelected) {
					//						processControlCmd((CommandControlABC) inCommand);
					//					}
					break;
				default:
					break;
			}
		}
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
