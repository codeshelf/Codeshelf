/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdObjectGetter.java,v 1.1 2012/03/19 04:05:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public class WebSessionRespCmdObjectGetter extends WebSessionRespCmdABC {

	private ObjectNode				mResponseNode;

	/**
	 * 
	 */
	public WebSessionRespCmdObjectGetter(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WebSessionRespCmdEnum getCommandEnum() {
		return WebSessionRespCmdEnum.OBJECT_GETTER_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
