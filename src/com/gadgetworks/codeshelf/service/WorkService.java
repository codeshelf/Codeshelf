package com.gadgetworks.codeshelf.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
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
import com.gadgetworks.codeshelf.model.domain.ProductivitySummary;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.collect.ImmutableList;

public class WorkService implements IApiService {

	public static final long				DEFAULT_RETRY_DELAY			= 10000L;
	public static final int					DEFAULT_CAPACITY			= Integer.MAX_VALUE;

	private static final Logger				LOGGER						= LoggerFactory.getLogger(WorkService.class);
	private BlockingQueue<WorkInstruction>	completedWorkInstructions;

	@Getter
	@Setter
	long									retryDelay;

	@Getter
	@Setter
	int										capacity;

	IEdiExportServiceProvider				exportServiceProvider;

	@Getter
	PersistenceService						persistenceService;

	private WorkServiceThread				wsThread					= null;
	private static boolean					aWorkServiceThreadExists	= false;

	public WorkService() {
		init(new IEdiExportServiceProvider() {
			@Override
			public IEdiService getWorkInstructionExporter(Facility facility) {
				return facility.getEdiExportService();
			}
		});
	}

	public WorkService(PersistenceService persistenceService, IEdiExportServiceProvider exportServiceProvider) {
		init(exportServiceProvider);
	}

	private void init(IEdiExportServiceProvider exportServiceProvider) {
		this.persistenceService = PersistenceService.getInstance();
		this.exportServiceProvider = exportServiceProvider;

		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.capacity = DEFAULT_CAPACITY;
	}

	private class WorkServiceThread extends Thread {
		public WorkServiceThread() {
			super("WorkService Thread");
		}
		
		@Override
		public void run() {
			WorkService.aWorkServiceThreadExists = true;
			try {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						persistenceService.beginTenantTransaction();
						sendWorkInstructions();
						persistenceService.commitTenantTransaction();
					} catch (Exception e) {
						persistenceService.rollbackTenantTransaction();
						LOGGER.error("Unable to send work instructions", e);
					}


				}
			} catch (Exception e) {
				LOGGER.error("Work instruction exporter interrupted waiting for completed work instructions. Shutting down.", e);
			}
			WorkService.aWorkServiceThreadExists = false;
		}
	}

	public WorkService start() {
		if (WorkService.aWorkServiceThreadExists) {
			LOGGER.error("Only one WorkService thread is allowed to run at once");
		}
		this.completedWorkInstructions = new LinkedBlockingQueue<WorkInstruction>(this.capacity);
		this.wsThread = new WorkServiceThread();
		this.wsThread.start();
		return this;
	}

	public void stop() {
		this.wsThread.interrupt();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		if (WorkService.aWorkServiceThreadExists) {
			LOGGER.error("Failed to stop WorkServiceThread by interruption");
		} else {
			this.completedWorkInstructions = null;
		}
	}

	private void sendWorkInstructions() throws InterruptedException {

		while (!Thread.currentThread().isInterrupted()) {
			WorkInstruction wi = completedWorkInstructions.take();
			try {
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
			} catch (Exception e) {
				LOGGER.warn("Unexpected exception sending work instruction, skipping: " + wi, e);
			}
		}
	}

	public List<WiSetSummary> workSummary(UUID cheId, UUID facilityId) {
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
		// jr/hibernate  tracking down an error
		if (completedWorkInstructions == null)
			LOGGER.error("null completedWorkInstructions in WorkService.exportWorkInstruction", new Exception());
		else if (inWorkInstruction == null)
			LOGGER.error("null input to WorkService.exportWorkInstruction", new Exception());
		else {
			LOGGER.debug("Queueing work instruction: " + inWorkInstruction);
			completedWorkInstructions.add(inWorkInstruction);
		}
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
	
	public ProductivitySummary getProductivitySummary(UUID facilityId){
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		ProductivitySummary productivitySummary = new ProductivitySummary(facility);
		return productivitySummary;
	}
}
