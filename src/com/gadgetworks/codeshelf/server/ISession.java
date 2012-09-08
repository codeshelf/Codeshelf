/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISession.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;


// --------------------------------------------------------------------------
/**
 *  IChatSession is the generic interface for a chat session from any chat network type.
 *  e.g. XMPP, Yahoo, MSN, etc.
 *  
 *  @author jeffw
 */
public interface ISession {

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	IServerConnection getServerConnection();

	// --------------------------------------------------------------------------
	/**
	 *  Send a message on this session.
	 *  @param inMessage
	 */
	void sendMessage(String inMessage);

}
