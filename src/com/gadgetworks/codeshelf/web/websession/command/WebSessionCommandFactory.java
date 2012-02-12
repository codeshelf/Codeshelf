/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandFactory.java,v 1.3 2012/02/12 02:08:26 jeffw Exp $
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

	private WebSessionCommandFactory() {

	}

	public static IWebSessionCommand createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionCommand result = null;

		WebSessionCommandEnum commandEnum = getCommandTypeEnum(inCommandAsJson);

		switch (commandEnum) {
			case LAUNCH_CODE:
				result = new WebSessionCommandLaunch(inCommandAsJson);
				break;

			default:
				break;
		}

		if (result != null) {
			LOGGER.debug("Command ID: " + result.getCommandId() + " Type: " + commandEnum);
		}

		return result;
	}
	
	private static WebSessionCommandEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionCommandEnum result = WebSessionCommandEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCommand.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			String mCommandType = commandTypeNode.getTextValue();
			result = WebSessionCommandEnum.valueOf(mCommandType);
		}

		return result;
	}


}
