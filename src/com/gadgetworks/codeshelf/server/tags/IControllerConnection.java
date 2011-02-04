/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IControllerConnection.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
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

}
