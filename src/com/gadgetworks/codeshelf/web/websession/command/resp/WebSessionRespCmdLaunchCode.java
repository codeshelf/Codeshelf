/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdLaunchCode.java,v 1.6 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdLaunchCode extends WebSessionRespCmdABC {

	private static final String	LAUNCH_CODE_RESP	= "LAUNCH_CODE_RS";

	private String				mResponseValue;
	private Organization		mOrganization;

	/**
	 * 
	 */
	public WebSessionRespCmdLaunchCode(final String inResponseValue, final Organization inOrganization) {
		super();
		mResponseValue = inResponseValue;
		mOrganization = inOrganization;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.LAUNCH_CODE_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(LAUNCH_CODE_RESP, mResponseValue);

		// For valid response codes, also return the organization object;
		if (mOrganization != null) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put(IWebSessionReqCmd.CLASSNAME, mOrganization.getClassName());
			propertiesMap.put(IWebSessionReqCmd.PERSISTENT_ID, mOrganization.getPersistentId());
			propertiesMap.put(IWebSessionReqCmd.SHORT_DOMAIN_ID, mOrganization.getDomainId());
			propertiesMap.put(IWebSessionReqCmd.DESC, mOrganization.getDescription());
			
//			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
//			resultsList.add(propertiesMap);
			ObjectNode searchListNode = mapper.valueToTree(propertiesMap);
			inOutDataNode.put("organization", searchListNode);
		}
	}
}
