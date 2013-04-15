/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsDeviceManager.java,v 1.6 2013/04/15 21:27:05 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.List;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;

/**
 * @author jeffw
 *
 */
public interface ICsDeviceManager {

	void start();

	void stop();

	INetworkDevice getDeviceByGuid(NetGuid inGuid);

	// --------------------------------------------------------------------------
	/**
	 * A CHE has asked for the work it has waiting at the server end.
	 * @param inCheId	The CHE's GUID
	 * @param inPersistentId	The CHE's persistent ID
	 * @param inStartLocationId	The location where the CHE is when it requested the work.
	 * @param inContainerIdList	The containers on the CHE when it requested the work.
	 */
	void requestCheWork(String inCheId, UUID inPersistentId, String inStartLocationId, List<String> inContainerIdList);

	// --------------------------------------------------------------------------
	/**
	 * @param inCheId
	 * @param inPersistentId
	 * @param inWorkInstruction
	 */
	void completeWi(String inCheId, UUID inPersistentId, WorkInstruction inWorkInstruction);

}
