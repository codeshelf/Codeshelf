/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IControlMessageTransport.java,v 1.1 2011/01/21 02:22:35 jeffw Exp $
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
