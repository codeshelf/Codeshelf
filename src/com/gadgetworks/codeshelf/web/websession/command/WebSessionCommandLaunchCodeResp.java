/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunchCodeResp.java,v 1.1 2012/02/21 08:36:00 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;


/**
 * @author jeffw
 *
 */
public class WebSessionCommandLaunchCodeResp extends WebSessionCommandABC {

	private static final String	LAUNCH_CODE_RESP	= "LAUNCH_CODE_RESP";

	private String				mResponseValue;

	/**
	 * 
	 */
	public WebSessionCommandLaunchCodeResp(final String inResponseValue) {
		super();
		mResponseValue = inResponseValue;
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE_RESP;
	}

	protected final IWebSessionCommand doExec() {
		IWebSessionCommand result = null;

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {	
		inOutDataNode.put(LAUNCH_CODE_RESP, mResponseValue);
	}
}
