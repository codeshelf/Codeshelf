/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: RegisterEdiWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;

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
public class RegisterEdiWsRespCmd extends WsRespCmdABC {

	private String		mResponseValue;
	private Facility	mFacility;

	/**
	 * 
	 */
	public RegisterEdiWsRespCmd(final String inResponseValue, final Facility inFacility) {
		super();
		mResponseValue = inResponseValue;
		mFacility = inFacility;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.REGISTER_EDI_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {

		// Insert the response code.
		inOutDataNode.put(WsRespCmdEnum.REGISTER_EDI_RESP.getName(), mResponseValue);

		// For valid response codes, also return the organization object;
		if (mFacility != null) {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode searchListNode = mapper.valueToTree(mFacility);
			inOutDataNode.put("facility", searchListNode);
		}
	}
}
