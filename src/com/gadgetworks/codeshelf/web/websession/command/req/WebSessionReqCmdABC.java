/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdABC.java,v 1.5 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;

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
public abstract class WebSessionReqCmdABC implements IWebSessionReqCmd {

	private String		mCommandId;
	private JsonNode	mDataJsonNode;

	public WebSessionReqCmdABC() {

	}

	/**
	 * @param inCommandId
	 * @param inDataAsJson
	 */
	public WebSessionReqCmdABC(final String inCommandId, final JsonNode inDataAsJson) {

		mCommandId = inCommandId;
		mDataJsonNode = inDataAsJson;
	}

	// The subclasses execute the command and return a command data reesponse.
	protected abstract IWebSessionRespCmd doExec();

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
	public final IWebSessionRespCmd exec() {
		IWebSessionRespCmd result = null;

		result = doExec();

		if (result != null) {
			result.setCommandId(this.getCommandId());
		}

		return result;
	}
}
