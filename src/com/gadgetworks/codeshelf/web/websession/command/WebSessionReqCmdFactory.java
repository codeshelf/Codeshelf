/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdFactory.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public final class WebSessionReqCmdFactory {

	private static final Log	LOGGER					= LogFactory.getLog(WebSessionReqCmdFactory.class);

	private WebSessionReqCmdFactory() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommandAsJson
	 * @return
	 */
	public static IWebSessionReqCmd createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionReqCmd result = null;

		WebSessionReqCmdEnum commandEnum = getCommandTypeEnum(inCommandAsJson);
		
		String commandId = inCommandAsJson.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
		JsonNode dataNode = inCommandAsJson.get(IWebSessionCmd.DATA_ELEMENT);

		switch (commandEnum) {
			case LAUNCH_CODE_CHECK:
				result = new WebSessionReqCmdLaunchCode(commandId, dataNode);
				break;

			case OBJECT_GETTER_REQ:
				result = new WebSessionReqCmdObjectGetter(commandId, dataNode);
				break;

			case OBJECT_LISTENER_REQ:
				result = new WebSessionReqCmdObjectListener(commandId, dataNode);
				break;

			default:
				break;
		}

		if (result != null) {
			LOGGER.debug("Command ID: " + result.getCommandId() + " Type: " + commandEnum);
		}

		return result;
	}
	
	private static WebSessionReqCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionReqCmdEnum result = WebSessionReqCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			String commandType = commandTypeNode.getTextValue();
			result = WebSessionReqCmdEnum.valueOf(commandType);
		}

		return result;
	}


}
