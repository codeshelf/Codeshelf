/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsDeviceManager.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

/**
 * @author jeffw
 *
 */
public interface ICsDeviceManager {
	
	void start();
	
	void stop();
	
	void requestCheWork(String inCheId, String inContainerId, String inLocationId);

}
