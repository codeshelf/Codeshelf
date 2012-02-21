/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandObjectQueryResp.java,v 1.2 2012/02/21 23:32:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.node.ObjectNode;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandObjectQueryResp extends WebSessionCommandABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionCommandObjectQueryResp(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.OBJECT_QUERY_RESP;
	}

	protected final IWebSessionCommand doExec() {
		IWebSessionCommand result = null;

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
