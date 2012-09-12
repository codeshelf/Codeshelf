/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdRegisterEdi.java,v 1.1 2012/09/12 23:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: REGISTER_EDI_RESP,
 * 	data {
 * 		authorizationUrl: <url>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class WebSessionRespCmdRegisterEdi extends WebSessionRespCmdABC {

	private String				mResponseValue;
	private Facility			mFacility;

	/**
	 * 
	 */
	public WebSessionRespCmdRegisterEdi(final String inResponseValue, final Facility inFacility) {
		super();
		mResponseValue = inResponseValue;
		mFacility = inFacility;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.REGISTER_EDI_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(WebSessionRespCmdEnum.REGISTER_EDI_RESP.getName(), mResponseValue);

		// For valid response codes, also return the organization object;
		if (mFacility != null) {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode searchListNode = mapper.valueToTree(mFacility);
			inOutDataNode.put("facility", searchListNode);
		}
	}
}
