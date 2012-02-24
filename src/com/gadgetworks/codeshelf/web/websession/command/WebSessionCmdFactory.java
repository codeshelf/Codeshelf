/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdFactory.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public final class WebSessionCmdFactory {

	private static final Log	LOGGER					= LogFactory.getLog(WebSessionCmdFactory.class);

	private WebSessionCmdFactory() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommandAsJson
	 * @return
	 */
	public static IWebSessionCmd createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionCmd result = null;

		WebSessionCmdEnum commandEnum = getCommandTypeEnum(inCommandAsJson);
		
		String commandId = inCommandAsJson.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
		JsonNode dataNode = inCommandAsJson.get(IWebSessionCmd.DATA_ELEMENT);

		switch (commandEnum) {
			case LAUNCH_CODE_CHECK:
				result = new WebSessionCmdLaunchCodeCheck(commandId, dataNode);
				break;

			case OBJECT_GETTER_REQ:
				result = new WebSessionCmdObjectGetterReq(commandId, dataNode);
				break;

			case OBJECT_GETBYID_REQ:
				//result = new WebSessionCommandObjectGetById(commandId, dataNode);
				break;

			default:
				break;
		}

		if (result != null) {
			LOGGER.debug("Command ID: " + result.getCommandId() + " Type: " + commandEnum);
		}

		return result;
	}
	
	private static WebSessionCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionCmdEnum result = WebSessionCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			String commandType = commandTypeNode.getTextValue();
			result = WebSessionCmdEnum.valueOf(commandType);
		}

		return result;
	}


}
