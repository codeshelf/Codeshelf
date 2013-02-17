/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdNetAttach.java,v 1.1 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdNetAttach extends WebSessionRespCmdABC {

	private String		mResponseValue;
	private Facility	mFacility;

	/**
	 * 
	 */
	public WebSessionRespCmdNetAttach(final String inResponseValue, final Facility inFacility) {
		super();
		mResponseValue = inResponseValue;
		mFacility = inFacility;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.NET_ATTACH_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(WebSessionRespCmdEnum.NET_ATTACH_RESP.toString(), mResponseValue);

		// For valid response codes, also return the facility object;
		if (mFacility != null) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put(IWebSessionReqCmd.CLASSNAME, mFacility.getClassName());
			propertiesMap.put(IWebSessionReqCmd.PERSISTENT_ID, mFacility.getPersistentId());
			propertiesMap.put(IWebSessionReqCmd.SHORT_DOMAIN_ID, mFacility.getDomainId());
			propertiesMap.put(IWebSessionReqCmd.DESC, mFacility.getDescription());

			//			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			//			resultsList.add(propertiesMap);
			ObjectNode searchListNode = mapper.valueToTree(propertiesMap);
			inOutDataNode.put("facility", searchListNode);
		}
	}
}
