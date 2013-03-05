/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdCheWork.java,v 1.1 2013/03/05 07:47:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdCheWork extends WebSessionRespCmdABC {

	/**
	 * 
	 */
	public WebSessionRespCmdCheWork() {
		super();
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.CHE_WORK_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

//		// Insert the response code.
//		inOutDataNode.put(WebSessionRespCmdEnum.CHE_WORK_RESP.toString(), mResponseValue);
//
//		// For valid response codes, also return the facility object;
//		if (mCodeshelfNetwork != null) {
//			ObjectMapper mapper = new ObjectMapper();
//			Map<String, Object> propertiesMap = new HashMap<String, Object>();
//			propertiesMap.put(IWebSessionReqCmd.CLASSNAME, mCodeshelfNetwork.getClassName());
//			propertiesMap.put(IWebSessionReqCmd.PERSISTENT_ID, mCodeshelfNetwork.getPersistentId());
//			propertiesMap.put(IWebSessionReqCmd.SHORT_DOMAIN_ID, mCodeshelfNetwork.getDomainId());
//			propertiesMap.put(IWebSessionReqCmd.DESC, mCodeshelfNetwork.getDescription());
//
//			//			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
//			//			resultsList.add(propertiesMap);
//			ObjectNode searchListNode = mapper.valueToTree(propertiesMap);
//			inOutDataNode.put("codeshelfNetwork", searchListNode);
//		}
	}
}
