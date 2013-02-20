/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IControlMessageTransport.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

/**
 * @author jeffw
 *
 */
public interface IControlMessageTransport {
	
	void startTransport();
	
	void pauseTransport();
	
	void stopTransport();

	void inputControlMessage(ControlMessage inControlMessage);
	
	void outputControlMessage(ControlMessage inControlMessage);
	
	ControlMessageHandler getControlMessageHandler();
	
	void setControlMessageHandler(ControlMessageHandler inMessageHandler);
}
