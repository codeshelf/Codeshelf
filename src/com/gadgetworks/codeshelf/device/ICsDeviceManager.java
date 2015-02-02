/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsDeviceManager.java,v 1.6 2013/04/15 21:27:05 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.List;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.PcapRingBuffer;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;

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
	 * @param inContainerIdList	The containers on the CHE when it requested the work.
	 */
	void computeCheWork(String inCheId, UUID inPersistentId, List<String> inContainerIdList);
	
	void computeCheWork(String inCheId, UUID inPersistentId, String orderDetailId);

	// --------------------------------------------------------------------------
	/**
	 * A CHE has asked for the work it has waiting at the server end.
	 * @param inCheId	The CHE's GUID
	 * @param inPersistentId	The CHE's persistent ID
	 * @param inStartLocationId	The location where the CHE is when it requested the work.
	 */
	void getCheWork(String inCheId, UUID inPersistentId, String inStartLocationId);

	// --------------------------------------------------------------------------
	/**
	 * @param inCheId
	 * @param inPersistentId
	 * @param inWorkInstruction
	 */
	void completeWi(String inCheId, UUID inPersistentId, WorkInstruction inWorkInstruction);

	List<AisleDeviceLogic> getAisleControllers();

	List<CheDeviceLogic> getCheControllers();

	IRadioController getRadioController();

	JettyWebSocketClient getWebSocketCient();

	PcapRingBuffer getPcapBuffer();
	
	boolean getAutoShortValue();

	String getPickInfoValue();

	String getContainerTypeValue();

	INetworkDevice updateOneDevice(UUID theUuid, NetGuid theGuid, String newProcessType);
}
