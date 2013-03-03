/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IGatewayInterface.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetworkId;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IGatewayInterface {

	// From RFC 1055 SLIP framing protocol.
	byte	END				= (byte) 0xC0;						//0300; /* indicates end of packet */
	byte	ESC				= (byte) 0xDB;						//0333; /* indicates byte stuffing */
	byte	ESC_END			= (byte) 0xDC;						//0334; /* ESC ESC_END means END data byte */
	byte	ESC_ESC			= (byte) 0xDD;						//0335; /* ESC ESC_ESC means ESC data byte */

	// Due to SLIP protocol, the max frame size could be 2x the data, plus the END flag.
	int		MAX_FRAME_BYTES	= IPacket.MAX_PACKET_BYTES * 2 + 1;

	// --------------------------------------------------------------------------
	/**
	 *  Start the interface before use.
	 */
	void startInterface();
	
	// --------------------------------------------------------------------------
	/**
	 *  Reset the interface if there's a problem with it.
	 */
	void resetInterface();
	
	// --------------------------------------------------------------------------
	/**
	 *  Stop the interface when we're done with it.
	 */
	void stopInterface();
	
	// --------------------------------------------------------------------------
	/**
	 *  Indicates that the interface has started.
	 *  @return
	 */
	boolean isStarted();
	
	// --------------------------------------------------------------------------
	/**
	 *  Send a packet over the interface.
	 *  @param inPacket	The packet to send.
	 */
	void sendPacket(IPacket inPacket);

	// --------------------------------------------------------------------------
	/**
	 *  Read a packet from the interface.
	 *  @return	The packet read from the interface.
	 */
	IPacket receivePacket(NetworkId inMyNetworkId);

}
