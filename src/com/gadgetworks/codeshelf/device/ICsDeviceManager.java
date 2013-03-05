/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsDeviceManager.java,v 1.3 2013/03/05 00:05:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.List;

/**
 * @author jeffw
 *
 */
public interface ICsDeviceManager {
	
	void start();
	
	void stop();
	
	// --------------------------------------------------------------------------
	/**
	 * A CHE has asked for the work it has waiting at the server end.
	 * @param inCheId	The CHE's GUID
	 * @param inStartLocationId	The location where the CHE is when it requested the work.
	 * @param inContainerIdList	The containers on the CHE when it requested the work.
	 */
	void requestCheWork(String inCheId, String inStartLocationId, List<String> inContainerIdList);

}
