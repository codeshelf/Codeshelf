/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWirelessInterface.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import com.gadgetworks.codeshelf.command.ICommand;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IWirelessInterface {

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
	 * @return
	 */
	boolean checkInterfaceOk();
	
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
