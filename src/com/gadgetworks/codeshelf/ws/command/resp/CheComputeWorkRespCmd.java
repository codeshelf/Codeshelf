/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheWorkWsRespCmd.java,v 1.2 2013/04/04 19:05:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_COMPUTEWORK_RS,
 * 	data {
 * 		wiCount: <wiCount>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CheComputeWorkRespCmd extends WsRespCmdABC {

	public static final String	WI_COUNT	= "wiCount";

	private Che					mChe;
	private List<String>		mContainersIds;

	/**
	 * 
	 */
	public CheComputeWorkRespCmd(final Che inChe, final List<String> inContainerIds) {
		super();
		mChe = inChe;
		mContainersIds = inContainerIds;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.CHE_COMPUTEWORK_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	/*
	 * Serialize the work instructions for this CHE and put them on the WebSocket.
	 */
	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put("cheId", mChe.getDeviceNetGuid().getHexStringNoPrefix());

		// Figure out the CHE's work area by its scanned location.
		Facility facility = mChe.getParent().getParent();

		// Get the work instructions for this CHE at this location for the given containers.
		Integer wiCount = facility.computeWorkInstructions(mChe, mContainersIds);

		// Serialize the work instructions into the websocket command data.
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put(WI_COUNT, wiCount);
		inOutDataNode.put(IWsReqCmd.RESULTS, objectNode);
	}
}
