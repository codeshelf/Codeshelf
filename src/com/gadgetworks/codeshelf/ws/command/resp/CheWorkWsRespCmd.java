/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheWorkWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_WORK_RS,
 * 	data {
 * 		cheId: <cheId>,
 * 		wis: [
 * 			{
 * 				acId: <aisleControllerId>,
 * 				acCmd: <cmd>,
 * 				cntrId: <containerId>,
 * 				qty: <quantity>,
 * 				sku: <skuId>,
 * 				loc: <locId>,
 * 				color: <colorName>
 * 			}
 * 		]
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CheWorkWsRespCmd extends WsRespCmdABC {

	private Che				mChe;
	private String			mCheLocation;
	private List<String>	mContainersIds;

	/**
	 * 
	 */
	public CheWorkWsRespCmd(final Che inChe, final String inCheLocation, final List<String> inContainerIds) {
		super();
		mChe = inChe;
		mCheLocation = inCheLocation;
		mContainersIds = inContainerIds;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.CHE_WORK_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put("cheId", mChe.getDeviceNetGuid().getHexStringNoPrefix());

		// Figure out the CHE's work area by its scanned location.
		Facility facility = mChe.getParent().getParent();
		List<WorkInstruction> wiList = facility.getWorkInstructions(mChe, mCheLocation, mContainersIds);

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode wiListNode = mapper.valueToTree(wiList);
		inOutDataNode.put(IWsReqCmd.RESULTS, wiListNode);
	}
}
