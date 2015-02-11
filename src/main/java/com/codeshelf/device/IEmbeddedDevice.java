/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: IEmbeddedDevice.java,v 1.2 2013/04/17 17:02:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

/**
 * @author jeffw
 *
 */
public interface IEmbeddedDevice {

	String	CONTROLLER_IPADDR_PROPERTY	= "CONTROLLER_IPADDR";
	String	GUID_PROPERTY				= "GUID";

	void start();

	void stop();
}
