/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdObjectGetterResp.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.node.ObjectNode;

/**
 * @author jeffw
 *
 */
public class WebSessionCmdObjectGetterResp extends WebSessionCmdABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionCmdObjectGetterResp(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionCmdEnum getCommandEnum() {
		return WebSessionCmdEnum.OBJECT_GETTER_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
