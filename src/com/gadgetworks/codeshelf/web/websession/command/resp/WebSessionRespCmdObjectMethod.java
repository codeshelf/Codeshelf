/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdObjectMethod.java,v 1.3 2012/09/23 03:05:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdObjectMethod extends WebSessionRespCmdABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionRespCmdObjectMethod(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.OBJECT_METHOD_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
