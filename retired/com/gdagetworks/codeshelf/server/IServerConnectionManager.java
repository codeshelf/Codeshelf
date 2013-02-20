/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IServerConnectionManager.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;


// --------------------------------------------------------------------------
/**
 *  IServerConnectionManager is the generic interface for chat network client-server connection management.
 *  e.g. XMPP, Yahoo, MSN, etc.
 *  
 *  In general, the owner Person has only one account for a particular chat network.  (Though the onwer person
 *  is welcome to have as many as they want.)  Each of these accounts will only ever have one connection into
 *  that network.
 *  
 *  @author jeffw
 */
public interface IServerConnectionManager {

	// --------------------------------------------------------------------------
	/**
	 *  The connection failed for some reason, so we remove it from management.
	 *  @param inServerConnection
	 */
	void serverConnectionFailed(IServerConnection inServerConnection);

	// --------------------------------------------------------------------------
	/**
	 *  Remove the connection from the active set of connections.
	 *  @param inServerConnection the server connection to remove.
	 */
	void removeServerConnection(IServerConnection inServerConnection);
}
