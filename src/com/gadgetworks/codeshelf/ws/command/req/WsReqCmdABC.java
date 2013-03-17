/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WsReqCmdABC.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * The general form of a request command (in JSON) is:
 * 
 * 		{
 * 			id: <the id>
 * 			type: <the type>
 * 			data:	{
 * 			}
 * 		}
 * @author jeffw
 *
 */
public abstract class WsReqCmdABC implements IWsReqCmd {

	private String		mCommandId;
	private JsonNode	mDataJsonNode;

	public WsReqCmdABC() {

	}

	/**
	 * @param inCommandId
	 * @param inDataAsJson
	 */
	public WsReqCmdABC(final String inCommandId, final JsonNode inDataAsJson) {

		mCommandId = inCommandId;
		mDataJsonNode = inDataAsJson;
	}

	// The subclasses execute the command and return a command data reesponse.
	protected abstract IWsRespCmd doExec();

	public final String getCommandId() {
		return mCommandId;
	}

	public final void setCommandId(String inCommandId) {
		mCommandId = inCommandId;
	}

	public final JsonNode getDataJsonNode() {
		return mDataJsonNode;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.IWebSessionCommand#exec()
	 */
	public final IWsRespCmd exec() {
		IWsRespCmd result = null;

		result = doExec();

		if (result != null) {
			result.setCommandId(this.getCommandId());
		}

		return result;
	}
}
