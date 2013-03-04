/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebsocketClientMsgHandler.java,v 1.4 2013/03/04 05:13:48 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;



/**
 * @author jeffw
 *
 */
public interface ICsWebsocketClientMsgHandler {
	
	void handleWebSocketMessage(final String inMessage);
	
	void handleWebSocketClosed();
}
