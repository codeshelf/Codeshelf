/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WsReqCmdFactory.java,v 1.2 2013/03/17 23:10:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public final class WsReqCmdFactory implements IWsReqCmdFactory {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(WsReqCmdFactory.class);

	private ITypedDao<Organization>		mOrganizationDao;
	private ITypedDao<Che>				mCheDao;
	private ITypedDao<WorkInstruction>	mWorkInstructionDao;
	private ITypedDao<OrderHeader>		mOrderHeaderDao;
	private ITypedDao<OrderDetail>		mOrderDetailDao;
	private IDaoProvider				mDaoProvider;

	@Inject
	public WsReqCmdFactory(final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Che> inCheDao,
		final ITypedDao<WorkInstruction> inWorkInstructionDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final IDaoProvider inDaoPovider) {
		mOrganizationDao = inOrganizationDao;
		mCheDao = inCheDao;
		mWorkInstructionDao = inWorkInstructionDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mDaoProvider = inDaoPovider;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommandAsJson
	 * @return
	 */
	public IWsReqCmd createWebSessionCommand(JsonNode inCommandAsJson) {
		IWsReqCmd result = null;

		WsReqCmdEnum commandEnum = getCommandTypeEnum(inCommandAsJson);

		String commandId = inCommandAsJson.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
		JsonNode dataNode = inCommandAsJson.get(IWebSessionCmd.DATA_ELEMENT);

		switch (commandEnum) {
			case LOGIN_REQ:
				result = new LoginWsReqCmd(commandId, dataNode, mOrganizationDao);
				break;

			case OBJECT_GETTER_REQ:
				result = new ObjectGetterWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_LISTENER_REQ:
				result = new ObjectListenerWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_UPDATE_REQ:
				result = new ObjectUpdateWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_FILTER_REQ:
				result = new ObjectFilterWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_DELETE_REQ:
				result = new ObjectDeleteWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case OBJECT_METHOD_REQ:
				result = new ObjectMethodWsReqCmd(commandId, dataNode, mDaoProvider);
				break;

			case NET_ATTACH_REQ:
				result = new NetAttachWsReqCmd(commandId, dataNode, mOrganizationDao);
				break;

			case CHE_COMPUTEWORK_REQ:
				result = new CheComputeWorkReqCmd(commandId, dataNode, mCheDao);
				break;

			case CHE_GETWORK_REQ:
				result = new CheGetWorkReqCmd(commandId, dataNode, mCheDao);
				break;

			case CHE_WICOMPLETE_REQ:
				result = new CompleteWorkInstructionWsReqCmd(commandId, dataNode, mCheDao, mWorkInstructionDao, mOrderHeaderDao, mOrderDetailDao);
				break;

			default:
				break;
		}

		if (result != null) {
			LOGGER.debug("Command ID: " + result.getCommandId() + " Type: " + commandEnum);
		}

		return result;
	}

	private WsReqCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WsReqCmdEnum result = WsReqCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			result = WsReqCmdEnum.fromString(commandTypeNode.getTextValue());
		}

		return result;
	}

}
