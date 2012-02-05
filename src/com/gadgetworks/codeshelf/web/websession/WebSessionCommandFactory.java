/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandFactory.java,v 1.1 2012/02/05 08:41:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.web.websession.command.WebSessionCommandLaunch;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCommand;
import com.gadgetworks.codeshelf.web.websession.command.WebSessionCommandEnum;

/**
 * @author jeffw
 *
 */
public final class WebSessionCommandFactory {

	private static final Log	LOGGER			= LogFactory.getLog(WebSessionCommandFactory.class);

	private static final String	COMMAND_ELEMENT	= "command";
	private static final String	DETAILS_ELEMENT	= "details";

	private WebSessionCommandFactory() {

	}

	public static IWebSessionCommand createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionCommand result = null;

		JsonNode commandNode = inCommandAsJson.get(COMMAND_ELEMENT);
		JsonNode details = inCommandAsJson.get(DETAILS_ELEMENT);

		String commandName = commandNode.getTextValue();
		WebSessionCommandEnum commandEnum = WebSessionCommandEnum.valueOf(commandName);

		switch (commandEnum) {
			case LAUNCH_CODE:
				result = new WebSessionCommandLaunch(details);
				break;

			default:
				break;
		}

		LOGGER.debug(commandNode);

		return result;
	}
}
