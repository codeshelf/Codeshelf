/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdCheWork.java,v 1.3 2013/03/07 05:23:32 jeffw Exp $
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
		inOutDataNode.put("cheId", mChe.getDeviceGuid().getHexStringNoPrefix());

		// Figure out the CHE's work area by its scanned location.
		Facility facility = mChe.getParent().getParent();
		List<WorkInstruction> wiList = facility.getWorkInstructions(mChe, mCheLocation, mContainersIds);
		
		// Figure out from the location and containers what items we have to pick.
//		List<WorkInstruction> wiList = new ArrayList<WorkInstruction>();
//		for (String containerId : mContainersIds) {
//			wiList.add(createWi("A01.01", containerId, "ITEM1", 1, ColorEnum.BLUE));
//		}
//		for (String containerId : mContainersIds) {
//			wiList.add(createWi("A01.02", containerId, "ITEM2", 1, ColorEnum.BLUE));
//		}
//		for (String containerId : mContainersIds) {
//			wiList.add(createWi("A01.03", containerId, "ITEM3", 1, ColorEnum.BLUE));
//		}
//		for (String containerId : mContainersIds) {
//			wiList.add(createWi("A01.04", containerId, "ITEM4", 1, ColorEnum.BLUE));
//		}

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode wiListNode = mapper.valueToTree(wiList);
		inOutDataNode.put(IWebSessionReqCmd.RESULTS, wiListNode);
	}

//	private WorkInstruction createWi(final String inLocationId, final String inContainerId, final String inItemId, final Integer inQuantity, final ColorEnum inColor) {
//		WorkInstruction result = new WorkInstruction();
//		result.setAisleControllerId("0x00000003");
//		result.setAisleControllerCommand("cmd");
//		result.setContainerId(inContainerId);
//		result.setQuantity(1);
//		result.setItemId(inItemId);
//		result.setLocationId(inLocationId);
//		result.setColor(inColor);
//		return result;
//	}
}
