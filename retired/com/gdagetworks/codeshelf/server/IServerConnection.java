/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IServerConnection.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import com.gadgetworks.codeshelf.model.UserStatusEnum;

// --------------------------------------------------------------------------
/**
 *  IServerConnection is the generic interface for a client-server connection in any chat network type.
 *  e.g. XMPP, Yahoo, MSN, etc.
 *  
 *  @author jeffw
 */
public interface IServerConnection {

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	ISessionManager getSessionManager();

	// --------------------------------------------------------------------------
	/**
	 *  Indicate if the connection is connected to the server.
	 *  @return The state of the connection.
	 */
	boolean isConnected();

	// --------------------------------------------------------------------------
	/**
	 *  @param inPassword
	 */
	void connectUsingPassword(String inPassword, UserStatusEnum inUserStatusEnum);

	// --------------------------------------------------------------------------
	/**
	 *  @param inDefaultPassword
	 */
	void connectUsingPasswordDlog(String inDefaultPassword, UserStatusEnum inUserStatusEnum);

	// --------------------------------------------------------------------------
	/**
	 */
	void disconnect();

	// --------------------------------------------------------------------------
	/**
	 *  Map the network protocol's native user status to our unified user status enum.
	 *  @param inNetworkUserStatus
	 *  @return
	 */
	UserStatusEnum mapNativeUserStatusToUserStatusEnum(Object inNetworkUserStatus);
	
	// --------------------------------------------------------------------------
	/**
	 *  Map our unified user status enum to the network protocol's native user status.
	 *  @param inUserStatusEnum
	 *  @return
	 */
	Object mapUserStatusEnumToNativeUserStatus(UserStatusEnum inUserStatusEnum);
	
	// --------------------------------------------------------------------------
	/**
	 *  Set the user status for the connection.
	 *  @param inUserStatusEnum
	 */
	void setConnectionUserStatus(UserStatusEnum inUserStatusEnum);
	
}
