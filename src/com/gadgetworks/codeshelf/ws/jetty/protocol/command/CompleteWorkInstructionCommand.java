package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class CompleteWorkInstructionCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CompleteWorkInstructionCommand.class);

	CompleteWorkInstructionRequest request;
	
	public CompleteWorkInstructionCommand(CompleteWorkInstructionRequest request) {
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		CompleteWorkInstructionResponse response = new CompleteWorkInstructionResponse();

		UUID cheId = request.getCheId();
		Che che = Che.DAO.findByPersistentId(cheId);

		if (che != null) {
			WorkInstruction wiBean = request.getWorkInstruction();
			UUID wiId = request.getWorkInstruction().getPersistentId();
			WorkInstruction storedWi = WorkInstruction.DAO.findByPersistentId(wiId);
			
			if (storedWi != null) {
				storedWi.setPickerId(wiBean.getPickerId());
				storedWi.setActualQuantity(wiBean.getActualQuantity());
				storedWi.setStatusEnum(wiBean.getStatusEnum());
				storedWi.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
				storedWi.setStarted(wiBean.getStarted());
				storedWi.setCompleted(wiBean.getCompleted());
				try {
					WorkInstruction.DAO.store(storedWi);
				} catch (DaoException e) {
					LOGGER.error("Failed to update work instruction", e);
				}
				setOrderDetailStatus(storedWi);
				
				sendWorkInstructionToHost(storedWi);
				response.setWorkInstructionId(wiId);
				response.setStatus(ResponseStatus.Success);
				return response;
			} 
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
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
		if (qtyPicked >= detail.getMinQuantity()) {
			detail.setStatusEnum(OrderStatusEnum.COMPLETE);
		} else {
			detail.setStatusEnum(OrderStatusEnum.SHORT);
		}
		try {
			OrderDetail.DAO.store(detail);
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
			OrderHeader.DAO.store(order);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstruction
	 */
	private void sendWorkInstructionToHost(WorkInstruction inWorkInstruction) {
		List<WorkInstruction> wiList = new ArrayList<WorkInstruction>();
		wiList.add(inWorkInstruction);
		Facility facility = inWorkInstruction.getParent().getParent().getParent();
		if (facility != null) {
			facility.sendWorkInstructionsToHost(wiList);
		}
	}

}
