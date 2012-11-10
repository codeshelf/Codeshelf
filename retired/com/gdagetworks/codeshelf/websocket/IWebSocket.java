/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocket.java,v 1.1 2012/11/10 03:20:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

public interface IWebSocket {

	void send(String inSendString) throws InterruptedException;

}
