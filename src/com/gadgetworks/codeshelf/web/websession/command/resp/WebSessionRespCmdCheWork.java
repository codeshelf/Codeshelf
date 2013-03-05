/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdCheWork.java,v 1.2 2013/03/05 20:45:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;

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
public class WebSessionRespCmdCheWork extends WebSessionRespCmdABC {

	private String			mCheId;
	private List<String>	mContainersIds;

	/**
	 * 
	 */
	public WebSessionRespCmdCheWork(final String inCheId, final List<String> inContainerIds) {
		super();
		mCheId = inCheId;
		mContainersIds = inContainerIds;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.CHE_WORK_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put("cheId", mCheId);

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode wiArray = mapper.createArrayNode();
		for (String containerId : mContainersIds) {
			createWi(wiArray, "A01.01", containerId, "ITEM1", 1, "BLUE");
		}
		for (String containerId : mContainersIds) {
			createWi(wiArray, "A01.02", containerId, "ITEM2", 1, "BLUE");
		}
		for (String containerId : mContainersIds) {
			createWi(wiArray, "A01.03", containerId, "ITEM3", 1, "BLUE");
		}
		for (String containerId : mContainersIds) {
			createWi(wiArray, "A01.04", containerId, "ITEM4", 1, "BLUE");
		}
		inOutDataNode.put(IWebSessionReqCmd.RESULTS, wiArray);
	}

	private void createWi(final ArrayNode inWiArrayNode, final String inLocation, final String inContainerId, final String inSku, final Integer inQuantity, final String inColorName) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode wiNode = mapper.createObjectNode();
		wiNode.put("acId", "0x00000003");
		wiNode.put("acCmd", "cmd");
		wiNode.put("cntrId", inContainerId);
		wiNode.put("qty", 1);
		wiNode.put("sku", inSku);
		wiNode.put("loc", inLocation);
		wiNode.put("color", inColorName);
		inWiArrayNode.add(wiNode);

	}
}
