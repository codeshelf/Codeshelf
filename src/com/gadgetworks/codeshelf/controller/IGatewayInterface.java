/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IGatewayInterface.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import com.gadgetworks.codeshelf.command.ICommand;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IGatewayInterface {

	String	BEAN_ID			= "SerialInterface";

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
	 *  Send a command over the interface.
	 *  @param inCommand	The command to send.
	 */
	void sendCommand(ICommand inCommand);

	// --------------------------------------------------------------------------
	/**
	 *  Read a command from the interface.
	 *  @return	The command read from the interface.
	 */
	ICommand receiveCommand(NetworkId inMyNetworkId);

}
