/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectListenerWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public class ObjectListenerWsRespCmd extends WsRespCmdABC {

	private ObjectNode	mResponseNode;

	/**
	 * 
	 */
	public ObjectListenerWsRespCmd(final ObjectNode inResponseValue) {
		mResponseNode = inResponseValue;
	}

	public final WsRespCmdEnum getCommandEnum() {
		return WsRespCmdEnum.OBJECT_LISTENER_RESP;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		return result;
	}

	protected final void doPrepareDataNode(ObjectNode inOutDataNode) {
		inOutDataNode.putAll(mResponseNode);
	}
}
