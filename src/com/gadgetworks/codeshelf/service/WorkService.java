package com.gadgetworks.codeshelf.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Getter;
import lombok.Setter;

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
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class WorkService {

	public static final long DEFAULT_RETRY_DELAY 	= 10000L;
	public static final int DEFAULT_CAPACITY		= Integer.MAX_VALUE;
	
	private static final Logger						LOGGER	= LoggerFactory.getLogger(WorkService.class);
	private BlockingQueue<WorkInstruction>	completedWorkInstructions;

	@Getter
	@Setter
	long retryDelay;

	@Getter
	@Setter
	int capacity;
	
	IEdiExportServiceProvider exportServiceProvider;
	
	@Getter
	PersistenceService persistenceService;
	
	@Inject
	public WorkService(PersistenceService persistenceService) {
		this.persistenceService = persistenceService;

		this.exportServiceProvider = new IEdiExportServiceProvider() {
				@Override
				public IEdiService getWorkInstructionExporter(Facility facility) {
					return facility.getEdiExportService();
				}
			};

		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.capacity = DEFAULT_CAPACITY;
	}

	public WorkService start() {
		this.completedWorkInstructions = new LinkedBlockingQueue<WorkInstruction>(this.capacity);

		Executor executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
			public void run() {
				try {
					while (!Thread.interrupted()) {
						
						sendWorkInstructions();

					}
				} catch (InterruptedException e) {
					LOGGER.error("Work instruction exporter interrupted waiting for completed work instructions. Shutting down.", e);
				}
			}
		});
		
		return this;
	}
	
	private void sendWorkInstructions() throws InterruptedException {
		persistenceService.beginTenantTransaction();

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
				Thread.sleep(retryDelay);
				LOGGER.warn("failure to send work instructions, retrying: ", e);
			}
		}

		persistenceService.endTenantTransaction();
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
			throw new InputValidationException(updatedWi, "persistentId", ErrorCode.FIELD_NOT_FOUND);
		}
		storedWi.setPickerId(updatedWi.getPickerId());
		storedWi.setActualQuantity(updatedWi.getActualQuantity());
		storedWi.setStatus(updatedWi.getStatus());
		storedWi.setType(WorkInstructionTypeEnum.ACTUAL);
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
			detail.setStatus(OrderStatusEnum.COMPLETE);
		} else {
			detail.setStatus(OrderStatusEnum.SHORT);
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
		order.setStatus(OrderStatusEnum.COMPLETE);
		for (OrderDetail detail : order.getOrderDetails()) {
			if (detail.getStatus().equals(OrderStatusEnum.SHORT)) {
				order.setStatus(OrderStatusEnum.SHORT);
				break;
			} else if (!detail.getStatus().equals(OrderStatusEnum.COMPLETE)) {
				order.setStatus(OrderStatusEnum.INPROGRESS);
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
