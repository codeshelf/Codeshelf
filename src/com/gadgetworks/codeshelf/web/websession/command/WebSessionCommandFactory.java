/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandFactory.java,v 1.1 2012/02/06 08:00:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public final class WebSessionCommandFactory {

	private static final Log	LOGGER					= LogFactory.getLog(WebSessionCommandFactory.class);

	private static final String	COMMAND_ID_ELEMENT		= "id";
	private static final String	COMMAND_TYPE_ELEMENT	= "type";
	private static final String	DATA_ELEMENT			= "data";

	private WebSessionCommandFactory() {

	}

	public static IWebSessionCommand createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionCommand result = null;

		JsonNode commandIdNode = inCommandAsJson.get(COMMAND_ID_ELEMENT);
		JsonNode commandTypeNode = inCommandAsJson.get(COMMAND_TYPE_ELEMENT);
		String commandType = commandTypeNode.getTextValue();
		JsonNode data = inCommandAsJson.get(DATA_ELEMENT);

		WebSessionCommandEnum commandEnum = WebSessionCommandEnum.valueOf(commandType);

		switch (commandEnum) {
			case LAUNCH_CODE:
				result = new WebSessionCommandLaunch(data);
				break;

			default:
				break;
		}

		LOGGER.debug("Command ID: " + commandIdNode.toString() + " Type: " + commandType);

		return result;
	}
}
