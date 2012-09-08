/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdLaunchCode.java,v 1.4 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

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
			ObjectNode searchListNode = mapper.valueToTree(mOrganization);
			inOutDataNode.put("organization", searchListNode);
		}
	}
}
