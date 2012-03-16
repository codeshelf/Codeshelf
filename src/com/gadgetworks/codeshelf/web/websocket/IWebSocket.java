/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocket.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

public interface IWebSocket {

	void send(String inSendString) throws InterruptedException;

}
