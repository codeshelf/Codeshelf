/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdFactory.java,v 1.19 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public final class WebSessionReqCmdFactory implements IWebSessionReqCmdFactory {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(WebSessionReqCmdFactory.class);

	private ITypedDao<Organization>	mOrganizationDao;
	private IDaoProvider			mDaoProvider;

	@Inject
	public WebSessionReqCmdFactory(final ITypedDao<Organization> inOrganizationDao, final IDaoProvider inDaoPovider) {
		mOrganizationDao = inOrganizationDao;
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
			case LOGIN_REQ:
				result = new WebSessionReqCmdLogin(commandId, dataNode, mOrganizationDao);
				break;

			case OBJECT_GETTER_REQ:
				result = new WebSessionReqCmdObjectGetter(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_LISTENER_REQ:
				result = new WebSessionReqCmdObjectListener(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_UPDATE_REQ:
				result = new WebSessionReqCmdObjectUpdate(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_FILTER_REQ:
				result = new WebSessionReqCmdObjectFilter(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_DELETE_REQ:
				result = new WebSessionReqCmdObjectDelete(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_METHOD_REQ:
				result = new WebSessionReqCmdObjectMethod(commandId, dataNode, mDaoProvider);
				break;

			case NET_ATTACH_REQ:
				result = new WebSessionReqCmdNetAttach(commandId, dataNode, mOrganizationDao);
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
			result = WebSessionReqCmdEnum.fromString(commandTypeNode.getTextValue());
		}

		return result;
	}

}
