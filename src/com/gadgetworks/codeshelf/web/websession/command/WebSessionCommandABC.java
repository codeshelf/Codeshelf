/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandABC.java,v 1.1 2012/02/12 02:08:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public abstract class WebSessionCommandABC implements IWebSessionCommand {

	private String	mCommandId;
	private JsonNode mDataJsonNode;

	public WebSessionCommandABC(final JsonNode inCommandAsJson) {
		JsonNode commandIdNode = inCommandAsJson.get(COMMAND_ID_ELEMENT);
		mCommandId = commandIdNode.asText();
		
		mDataJsonNode = inCommandAsJson.get(IWebSessionCommand.DATA_ELEMENT);
	}

	public final String getCommandId() {
		return mCommandId;
	}
	
	public final JsonNode getDataJsonNode() {
		return mDataJsonNode;
	}

}
