/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebsocketClientMsgHandler.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

/**
 * @author jeffw
 *
 */
public interface ICsWebsocketClientMsgHandler {

	void handleWebSocketMessage(final String inMessage);

	void handleWebSocketClosed();
}
