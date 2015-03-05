/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IRadioController.java,v 1.3 2013/04/15 04:01:37 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IRadioController extends Runnable {

	// --------------------------------------------------------------------------
	/**
	 *  Send a command to the controller.
	 *  @param inCommand		The command to send.
	 *  @param inDstAddr		The destination address for the command.
	 */
	void sendCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested);

	// --------------------------------------------------------------------------
	/**
	 *	Set the network ID (call before startController) 
	 */
	void setNetworkId(final NetworkId inNetworkId);

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
	void addControllerEventListener(IRadioControllerEventListener inControllerEventListener);

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

	// --------------------------------------------------------------------------
	/**
	 * @param inGuid
	 * @return
	 */
	INetworkDevice getNetworkDevice(NetGuid inGuid);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	boolean isRunning();

	IGatewayInterface getGatewayInterface();

	NetGuid getNetGuidFromNetAddress(byte networkAddr);

	NetGuid getNetGuidFromNetAddress(NetAddress netAddress);

}
