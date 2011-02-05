/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IController.java,v 1.5 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import java.util.List;

import com.gadgetworks.codeshelf.command.ICommand;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IController extends Runnable {

	String		BEAN_ID					= "IController";

	NetAddress	GATEWAY_ADDRESS			= new NetAddress("0x000000");
	NetAddress	BROADCAST_ADDRESS		= new NetAddress("0x000000");

	NetworkId	BROADCAST_NETWORK_ID	= new NetworkId("0x0000");
	NetworkId	DEFAULT_NETWORK_ID		= new NetworkId("0x0000");

	// --------------------------------------------------------------------------
	/**
	 *  Receive the next command from the controller.
	 *  @param inCommand	The command sent.
	 *  @param inSrcAddr	The source address that sent the command.
	 */
	void receiveCommand(ICommand inCommand, NetAddress inSrcAddr);

	// --------------------------------------------------------------------------
	/**
	 *  Send a command to the controller.
	 *  @param inCommand		The command to send.
	 *  @param inDstAddr		The destination address for the command.
	 */
	void sendCommandNow(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested);

	// --------------------------------------------------------------------------
	/**
	 *  Send a command to the controller, but not before the send time..
	 *  @param inCommand		The command to send.
	 *  @param inDstAddr		The destination address for the command.
	 *  @param inSendTime		The send time before which we should not send the packet.
	 */
	void sendCommandTimed(ICommand inCommand, NetAddress inDstAddr, long InSendTimeNanos, boolean inAckRequested);

	// --------------------------------------------------------------------------
	/**
	 *  Send a command to the controller, but not before the send time.
	 *  (If the send time is zero then send ASAP.)
	 *  @param inCommand		The command to send.
	 *  @param inNetworkId		The network ID for this packet.
	 *  @param inDstAddr		The destination address for the command.
	 *  @param inSendTime		The send time before which we should not send the packet.
	 */
	void sendCommandTimed(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, long inSendTime, boolean inAckRequested);

	// --------------------------------------------------------------------------
	/**
	 *	Perform a controlled start of the controller.
	 */
	void startController(byte inPreferredChannel);

	// --------------------------------------------------------------------------
	/**
	 *	Perform a controlled shutdown of the controller.
	 */
	void stopController();

	// --------------------------------------------------------------------------
	/**
	 * @param inChannel
	 */
	void setRadioChannel(byte inChannel);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	byte getRadioChannel();

	// --------------------------------------------------------------------------
	/**
	 *	Add an event listener to the controller.
	 */
	void addControllerEventListener(IControllerEventListener inControllerEventListener);

	// --------------------------------------------------------------------------
	/**
	 *  Return all of the interfaces for this controller.
	 *  @return
	 */
	List<IWirelessInterface> getInterfaces();

	// --------------------------------------------------------------------------
	/**
	 *  Return the network devices managed by this controller for this address.
	 *  @return
	 */
	INetworkDevice getNetworkDevice(NetAddress inAddress);

	// --------------------------------------------------------------------------
	/**
	 *  Return a list of the network devices managed by this controller.
	 *  @return
	 */
	List<INetworkDevice> getNetworkDevices();

}
