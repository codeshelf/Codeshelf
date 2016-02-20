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
	 *	Send a network management command
	*/
	void sendNetMgmtCommand(ICommand inCommand, NetAddress inDstAddr);
	
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

	// --------------------------------------------------------------------------
	/**
	 * Get broadcast network Id for radio controller
	 * @return
	 */
	NetworkId getBroadcastNetworkId();
	
	// --------------------------------------------------------------------------
	/**
	 * Get zero network Id for radio controller
	 * @return
	 */
	NetworkId getZeroNetworkId();

	// --------------------------------------------------------------------------
	/**
	 * Get broadcast address for radio controller
	 * @return
	 */
	NetAddress getBroadcastAddress();
	
	// --------------------------------------------------------------------------
	/**
	 * Get broadcast address for radio controller
	 * @return
	 */
	NetAddress getServerAddress();

}
