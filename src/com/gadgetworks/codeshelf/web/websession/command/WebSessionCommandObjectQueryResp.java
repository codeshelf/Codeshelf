/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandObjectQueryResp.java,v 1.1 2012/02/21 08:36:00 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.node.ObjectNode;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandObjectQueryResp extends WebSessionCommandABC {

	private static final String	OBJECT_QUERY_RESP	= "OBJECT_QUERY_RESP";

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionCommandObjectQueryResp(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE_RESP;
	}

	protected final IWebSessionCommand doExec() {
		IWebSessionCommand result = null;

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
