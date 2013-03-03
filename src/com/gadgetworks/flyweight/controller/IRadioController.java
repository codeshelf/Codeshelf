/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IRadioController.java,v 1.1 2013/03/03 02:52:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetworkId;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IRadioController extends Runnable {

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
	void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested);
	
	// --------------------------------------------------------------------------
	/**
	 *  Send a command to the controller.
	 *  @param inCommand		The command to send.
	 *  @param inNetworkId	The network type (real/virtual).
	 *  @param inDstAddr		The destination address for the command.
	 */
	void sendCommand(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, boolean inAckRequested);
	
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
	 * @param inNetworkDevice
	 */
	void addNetworkDevice(INetworkDevice inNetworkDevice);
	
	// --------------------------------------------------------------------------
	/**
	 * @param inNetworkDevice
	 */
	void removeNetworkDevice(INetworkDevice inNetworkDevice);
	
}
