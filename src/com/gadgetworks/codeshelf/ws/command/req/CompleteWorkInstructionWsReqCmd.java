/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CompleteWorkInstructionWsReqCmd.java,v 1.2 2013/03/17 23:10:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_WICOMP_REQ,
 * 	data {
 * 		persistentId: 	<persistentId>,
 * 		wi: 			<workInstruction>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CompleteWorkInstructionWsReqCmd extends WsReqCmdABC {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(CompleteWorkInstructionWsReqCmd.class);

	private ITypedDao<Che>				mCheDao;
	private ITypedDao<WorkInstruction>	mWorkInstructionDao;
	private ITypedDao<OrderHeader>		mOrderDao;
	private ITypedDao<OrderDetail>		mOrderDetailDao;

	public CompleteWorkInstructionWsReqCmd(final String inCommandId,
		final JsonNode inDataNodeAsJson,
		final ITypedDao<Che> inCheDao,
		final ITypedDao<WorkInstruction> inWorkInstructionDao,
		final ITypedDao<OrderHeader> inOrderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao) {
		super(inCommandId, inDataNodeAsJson);
		mCheDao = inCheDao;
		mWorkInstructionDao = inWorkInstructionDao;
		mOrderDao = inOrderDao;
		mOrderDetailDao = inOrderDetailDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.CHE_WICOMPLETE_REQ;
	}

	// --------------------------------------------------------------------------
	/*
	 * Deserialize a completed work instruction from the remote gateway controller (from the WebSocket).
	 */
	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		JsonNode persistentIdNode = getDataJsonNode().get("persistentId");
		String persistentId = persistentIdNode.getTextValue();
		Che che = mCheDao.findByPersistentId(UUID.fromString(persistentId));

		if (che != null) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode wiNode = getDataJsonNode().get("wi");
			try {
				WorkInstruction wi = mapper.readValue(wiNode, WorkInstruction.class);

				WorkInstruction storedWi = mWorkInstructionDao.findByPersistentId(wi.getPersistentId());
				if (storedWi != null) {
					storedWi.setPickerId(wi.getPickerId());
					storedWi.setActualQuantity(wi.getActualQuantity());
					storedWi.setContainerId(wi.getContainerId());
					storedWi.setStatusEnum(wi.getStatusEnum());
					storedWi.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
					storedWi.setStarted(wi.getStarted());
					storedWi.setCompleted(wi.getCompleted());
					try {
						mWorkInstructionDao.store(storedWi);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}

					setOrderDetailStatus(storedWi);
				}
			} catch (IOException e) {
				LOGGER.error("", e);
			}

			//result = new CheWorkWsRespCmd(che, locationId, containerIdList);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Set order detail status for the WI.
	 * @param inWorkInstruction
	 */
	private void setOrderDetailStatus(final WorkInstruction inWorkInstruction) {
		// Find the order item for this WI and mark it.
		OrderDetail detail = inWorkInstruction.getParent();
		Double qtyPicked = 0.0;
		for (WorkInstruction sumWi : detail.getWorkInstructions()) {
			qtyPicked += sumWi.getActualQuantity();
		}
		if (qtyPicked >= detail.getQuantity()) {
			detail.setStatusEnum(OrderStatusEnum.COMPLETE);
		} else {
			detail.setStatusEnum(OrderStatusEnum.SHORT);
		}
		try {
			mOrderDetailDao.store(detail);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		setOrderStatus(detail);
	}

	// --------------------------------------------------------------------------
	/**
	 * Set the order status.
	 * @param inOrderDetail
	 */
	private void setOrderStatus(final OrderDetail inOrderDetail) {
		OrderHeader order = inOrderDetail.getParent();
		order.setStatusEnum(OrderStatusEnum.COMPLETE);
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getStatusEnum().equals(OrderStatusEnum.SHORT)) {
				order.setStatusEnum(OrderStatusEnum.SHORT);
				break;
			} else if (!detail.getStatusEnum().equals(OrderStatusEnum.COMPLETE)) {
				order.setStatusEnum(OrderStatusEnum.INPROGRESS);
				break;
			}
		}
		try {
			mOrderDao.store(order);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}
}
