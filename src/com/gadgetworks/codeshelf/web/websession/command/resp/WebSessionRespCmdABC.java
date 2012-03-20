/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdABC.java,v 1.2 2012/03/20 06:28:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author jeffw
 *
 */
public abstract class WebSessionRespCmdABC implements IWebSessionRespCmd {

	private String		mCommandId;
	private JsonNode	mDataJsonNode;
	private ObjectNode				mResponseNode;

	public WebSessionRespCmdABC() {

	}

	/**
	 * @param inCommandId
	 * @param inDataAsJson
	 */
	public WebSessionRespCmdABC(final String inCommandId, final JsonNode inDataAsJson) {

		mCommandId = inCommandId;
		mDataJsonNode = inDataAsJson;
	}

	protected abstract void doPrepareDataNode(ObjectNode inOutDataNode);

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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String result = "Command Id: " + mCommandId + " Data: " + mDataJsonNode;
		return result;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.IWebSessionCommand#serialize()
	 */
	public final String getResponseMsg() {

		// Construct the response node from the data node generated by the subclass.
		ObjectMapper treeMapper = new ObjectMapper();
		ObjectNode objectNode = treeMapper.createObjectNode();
		objectNode.put(COMMAND_ID_ELEMENT, getCommandId());
		objectNode.put(COMMAND_TYPE_ELEMENT, getCommandEnum().getName());

		// Create and populate the data node.
		ObjectNode dataNode = objectNode.putObject(DATA_ELEMENT);
		doPrepareDataNode(dataNode);

		return objectNode.toString();
	}
}
