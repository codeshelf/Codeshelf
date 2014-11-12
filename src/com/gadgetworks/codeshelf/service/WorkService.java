package com.gadgetworks.codeshelf.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.edi.IEdiExportServiceProvider;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WiSummarizer;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.collect.ImmutableList;

public class WorkService implements IApiService {

	private static final Logger						LOGGER	= LoggerFactory.getLogger(WorkService.class);
	private final BlockingQueue<WorkInstruction>	completedWorkInstructions;

	public WorkService() {
		this(Integer.MAX_VALUE, new IEdiExportServiceProvider() {
			@Override
			public IEdiService getWorkInstructionExporter(Facility facility) {
				return facility.getEdiExportService();
			}
		}, 10000L);
	}

	public WorkService(int capacity, final IEdiExportServiceProvider exportServiceProvider, final long retryDelay) {
		completedWorkInstructions = new LinkedBlockingQueue<WorkInstruction>(capacity);
		Executor executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
			public void run() {
				try {
					while (!Thread.currentThread().isInterrupted()) {
						WorkInstruction wi = completedWorkInstructions.take();
						boolean sent = false;
						while (!sent) {
							List<WorkInstruction> wiList = ImmutableList.of(wi);
							try {
								Facility facility = wi.getParent();
								IEdiService ediExportService = exportServiceProvider.getWorkInstructionExporter(facility);
								ediExportService.sendWorkInstructionsToHost(wiList);
								sent = true;
							} catch (IOException e) {
								LOGGER.warn("failure to send work instructions, retrying after: " + retryDelay, e);
								Thread.sleep(retryDelay);
							}
						}
					}
					LOGGER.info("WorkService exporting thread ending, interrupted");
				} catch (Exception e) {
					LOGGER.error("Work instruction exporter interrupted by exception while waiting for completed work instructions. Shutting down.", e);
				}
			}
		});
	}

	public List<WiSetSummary> workSummary(String cheId, String facilityId) {
		WiSummarizer summarizer = new WiSummarizer();
		summarizer.computeWiSummariesForChe(cheId, facilityId);
		return summarizer.getSummaries();
	}

	public void completeWorkInstruction(UUID cheId, WorkInstruction incomingWI) {
		Che che = Che.DAO.findByPersistentId(cheId);
		if (che != null) {
			try {
				final WorkInstruction storedWi = persistWorkInstruction(incomingWI);
				exportWorkInstruction(storedWi);
			} catch (DaoException e) {
				LOGGER.error("Unable to record work instruction: " + incomingWI, e);
			}
		} else {
			throw new IllegalArgumentException("Could not find che for id: " + cheId);
		}
	}

	private WorkInstruction persistWorkInstruction(WorkInstruction updatedWi) throws DaoException {
		UUID wiId = updatedWi.getPersistentId();
		WorkInstruction storedWi = WorkInstruction.DAO.findByPersistentId(wiId);
		if (storedWi == null) {
			throw new InputValidationException(updatedWi, "persistentId", wiId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
		}
		storedWi.setPickerId(updatedWi.getPickerId());
		storedWi.setActualQuantity(updatedWi.getActualQuantity());
		storedWi.setStatusEnum(updatedWi.getStatusEnum());
		storedWi.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
		storedWi.setStarted(updatedWi.getStarted());
		storedWi.setCompleted(updatedWi.getCompleted());
		WorkInstruction.DAO.store(storedWi);

		OrderDetail orderDetail = setOrderDetailStatus(storedWi);
		// v5 orderDetail can be null for housekeeping wis
		if (orderDetail != null)
			setOrderStatus(orderDetail);
		return storedWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstruction
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void exportWorkInstruction(WorkInstruction inWorkInstruction) {
		LOGGER.debug("Queueing work instruction: " + inWorkInstruction);
		completedWorkInstructions.add(inWorkInstruction);
	}

	// --------------------------------------------------------------------------
	/**
	 * Set order detail status for the WI.
	 * @param inWorkInstruction
	 */
	private OrderDetail setOrderDetailStatus(final WorkInstruction inWorkInstruction) {
		// Find the order item for this WI and mark it.
		OrderDetail detail = inWorkInstruction.getOrderDetail();
		// from v5 housekeeping WI may have null orderDetail
		if (detail == null) {
			// No need to log anything. This is commonly called by persistWorkInstruction
			return null;
		}
		Double qtyPicked = 0.0;
		for (WorkInstruction sumWi : detail.getWorkInstructions()) {
			qtyPicked += sumWi.getActualQuantity();
		}
		if (qtyPicked >= detail.getMinQuantity()) {
			detail.setStatusEnum(OrderStatusEnum.COMPLETE);
		} else {
			detail.setStatusEnum(OrderStatusEnum.SHORT);
		}
		OrderDetail.DAO.store(detail);
		return detail;
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
			LOGGER.error("Failed to update order status", e);
		}
	}

}
