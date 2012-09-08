/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IControllerConnection.java,v 1.3 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

/**
 * @author jeffw
 *
 */
public interface IControllerConnection {

	// --------------------------------------------------------------------------
	/**
	 * Start the controller connection.
	 */
	void start();

	// --------------------------------------------------------------------------
	/**
	 * Stop the controller connection.
	 */
	void stop();

	// --------------------------------------------------------------------------
	/**
	 * @param inDataBytes
	 */
	void sendDataBytes(byte[] inDataBytes);

}
