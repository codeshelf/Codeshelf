/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISessionManager.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;


// --------------------------------------------------------------------------
/**
 *  IChatSessionManager is the generic interface for chat network session management.
 *  e.g. XMPP, Yahoo, MSN, etc.
 *   
 *  @author jeffw
 */
public interface ISessionManager {

	// --------------------------------------------------------------------------
	/**
	 *  Remove the session from the active set of sessions.
	 *  @param inAccountPair The AccountPair that defines this session.
	 */
	void terminateChatSessionsForConnection(IServerConnection inConnection);

}
