/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdLaunchCodeResp.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.persist.Organization;

/**
 * @author jeffw
 *
 */
public class WebSessionCmdLaunchCodeResp extends WebSessionCmdABC {

	private static final String	LAUNCH_CODE_RESP	= "LAUNCH_CODE_RESP";

	private String				mResponseValue;
	private Organization		mOrganization;

	/**
	 * 
	 */
	public WebSessionCmdLaunchCodeResp(final String inResponseValue, final Organization inOrganization) {
		super();
		mResponseValue = inResponseValue;
		mOrganization = inOrganization;
	}

	public final WebSessionCmdEnum getCommandEnum() {
		return WebSessionCmdEnum.LAUNCH_CODE_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {

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
