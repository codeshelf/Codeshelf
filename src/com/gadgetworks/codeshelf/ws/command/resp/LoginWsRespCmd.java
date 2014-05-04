/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LoginWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;

/**
 *  command {
 * 	id: <cmd_id>,
 * 	type: LOGIN_RS,
 * 	data {
 * 		classname: <class>,
 * 		persistentId: <persistentId>,
 * 		domainId: <domainId>,
 * 		description: <description>
 * 	}
 * }
 * @author jeffw
 *
 */
public class LoginWsRespCmd extends WsRespCmdABC {

	private String			mResponseValue;
	private Organization	mOrganization;

	/**
	 * 
	 */
	public LoginWsRespCmd(final String inResponseValue, final Organization inOrganization) {
		super();
		mResponseValue = inResponseValue;
		mOrganization = inOrganization;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.LOGIN_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(WsRespCmdEnum.LOGIN_RESP.toString(), mResponseValue);

		// For valid response codes, also return the organization object;
		if (mOrganization != null) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put(IWsReqCmd.CLASSNAME, mOrganization.getClassName());
			propertiesMap.put(IWsReqCmd.PERSISTENT_ID, mOrganization.getPersistentId());
			propertiesMap.put(IWsReqCmd.SHORT_DOMAIN_ID, mOrganization.getDomainId());
			propertiesMap.put(IWsReqCmd.DESC, mOrganization.getDescription());

			//			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			//			resultsList.add(propertiesMap);
			ObjectNode searchListNode = mapper.valueToTree(propertiesMap);
			inOutDataNode.put("organization", searchListNode);
		}
	}
}
