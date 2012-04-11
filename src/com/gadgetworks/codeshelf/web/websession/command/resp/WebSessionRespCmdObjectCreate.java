/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdObjectCreate.java,v 1.1 2012/04/11 05:13:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdObjectCreate extends WebSessionRespCmdABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionRespCmdObjectCreate(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.OBJECT_CREATE_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
