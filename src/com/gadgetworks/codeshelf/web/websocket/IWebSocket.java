/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocket.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

public interface IWebSocket {

	void send(String inSendString) throws InterruptedException;

}
