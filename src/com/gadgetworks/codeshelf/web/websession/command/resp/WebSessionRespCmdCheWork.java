/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdCheWork.java,v 1.5 2013/03/13 03:52:50 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.flyweight.command.ColorEnum;

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

	private Che				mChe;
	private String			mCheLocation;
	private List<String>	mContainersIds;

	/**
	 * 
	 */
	public WebSessionRespCmdCheWork(final Che inChe, final String inCheLocation, final List<String> inContainerIds) {
		super();
		mChe = inChe;
		mCheLocation = inCheLocation;
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
		inOutDataNode.put("cheId", mChe.getDeviceNetGuid().getHexStringNoPrefix());

		// Figure out the CHE's work area by its scanned location.
		Facility facility = mChe.getParent().getParent();
		List<WorkInstruction> wiList = facility.getWorkInstructions(mChe, mCheLocation, mContainersIds);

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode wiListNode = mapper.valueToTree(wiList);
		inOutDataNode.put(IWebSessionReqCmd.RESULTS, wiListNode);
	}
}
