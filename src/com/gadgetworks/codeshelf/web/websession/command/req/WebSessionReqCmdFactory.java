/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdFactory.java,v 1.2 2012/03/22 06:21:47 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.User.IUserDao;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public final class WebSessionReqCmdFactory implements IWebSessionReqCmdFactory {

	private static final Log		LOGGER	= LogFactory.getLog(WebSessionReqCmdFactory.class);

	private IUserDao				mUserDao;
	private IDbFacade<PersistABC>	mDbFacade;
	private IDaoProvider			mDaoProvider;

	@Inject
	public WebSessionReqCmdFactory(final IUserDao inUserDao, final IDbFacade<PersistABC> inDbFacade, IDaoProvider inDaoPovider) {
		mUserDao = inUserDao;
		mDbFacade = inDbFacade;
		mDaoProvider = inDaoPovider;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommandAsJson
	 * @return
	 */
	public IWebSessionReqCmd createWebSessionCommand(JsonNode inCommandAsJson) {
		IWebSessionReqCmd result = null;

		WebSessionReqCmdEnum commandEnum = getCommandTypeEnum(inCommandAsJson);

		String commandId = inCommandAsJson.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
		JsonNode dataNode = inCommandAsJson.get(IWebSessionCmd.DATA_ELEMENT);

		switch (commandEnum) {
			case LAUNCH_CODE_CHECK:
				result = new WebSessionReqCmdLaunchCode(commandId, dataNode, mUserDao);
				break;

			case OBJECT_GETTER_REQ:
				result = new WebSessionReqCmdObjectGetter(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_LISTENER_REQ:
				result = new WebSessionReqCmdObjectListener(commandId, dataNode);
				break;

			case OBJECT_UPDATE_REQ:
				result = new WebSessionReqCmdObjectUpdate(commandId, dataNode);
				break;

			default:
				break;
		}

		if (result != null) {
			LOGGER.debug("Command ID: " + result.getCommandId() + " Type: " + commandEnum);
		}

		return result;
	}

	private WebSessionReqCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionReqCmdEnum result = WebSessionReqCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			String commandType = commandTypeNode.getTextValue();
			result = WebSessionReqCmdEnum.valueOf(commandType);
		}

		return result;
	}

}
