/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdObjectDelete.java,v 1.1 2012/04/21 08:23:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdObjectDelete extends WebSessionRespCmdABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionRespCmdObjectDelete(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.OBJECT_DELETE_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
