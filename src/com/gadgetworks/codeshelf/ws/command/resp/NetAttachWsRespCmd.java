/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAttachWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;

/**
 * @author jeffw
 *
 */
public class NetAttachWsRespCmd extends WsRespCmdABC {

	private String				mResponseValue;
	private CodeshelfNetwork	mCodeshelfNetwork;

	/**
	 * 
	 */
	public NetAttachWsRespCmd(final String inResponseValue, final CodeshelfNetwork inCodeshelfNetwork) {
		super();
		mResponseValue = inResponseValue;
		mCodeshelfNetwork = inCodeshelfNetwork;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.NET_ATTACH_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(WsRespCmdEnum.NET_ATTACH_RESP.toString(), mResponseValue);

		// For valid response codes, also return the facility object;
		if (mCodeshelfNetwork != null) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put(IWsReqCmd.CLASSNAME, mCodeshelfNetwork.getClassName());
			propertiesMap.put(IWsReqCmd.PERSISTENT_ID, mCodeshelfNetwork.getPersistentId());
			propertiesMap.put(IWsReqCmd.SHORT_DOMAIN_ID, mCodeshelfNetwork.getDomainId());
			propertiesMap.put(IWsReqCmd.DESC, mCodeshelfNetwork.getDescription());

			//			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			//			resultsList.add(propertiesMap);
			ObjectNode searchListNode = mapper.valueToTree(propertiesMap);
			inOutDataNode.put("codeshelfNetwork", searchListNode);
		}
	}
}
