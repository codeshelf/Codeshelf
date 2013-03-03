/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebsocketClientMsgHandler.java,v 1.2 2013/03/03 02:52:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;



/**
 * @author jeffw
 *
 */
public interface ICsWebsocketClientMsgHandler {
	
	void handleWebSocketMessage(final String inMessage);
}
