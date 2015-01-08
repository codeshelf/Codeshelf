package com.gadgetworks.codeshelf.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javassist.NotFoundException;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.edi.IEdiExportServiceProvider;
import com.gadgetworks.codeshelf.edi.WorkInstructionCSVExporter;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WiSummarizer;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.CriteriaRegistry;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class WorkService implements IApiService {

	public static final long			DEFAULT_RETRY_DELAY			= 10000L;
	public static final int				DEFAULT_CAPACITY			= Integer.MAX_VALUE;

	private static final Logger			LOGGER						= LoggerFactory.getLogger(WorkService.class);
	private BlockingQueue<WIMessage>	completedWorkInstructions;

	@Getter
	@Setter
	private long						retryDelay;

	@Getter
	@Setter
	private int							capacity;

	private IEdiExportServiceProvider	exportServiceProvider;

	@Transient
	private WorkInstructionCSVExporter	wiCSVExporter;

	@Getter
	private PersistenceService			persistenceService;

	private WorkServiceThread			wsThread					= null;
	private static boolean				aWorkServiceThreadExists	= false;

	public WorkService() {
		init(new IEdiExportServiceProvider() {
			@Override
			public IEdiService getWorkInstructionExporter(Facility facility) {
				return facility.getEdiExportService();
			}
		});
	}

	public WorkService(IEdiExportServiceProvider exportServiceProvider) {
		init(exportServiceProvider);
	}

	private void init(IEdiExportServiceProvider exportServiceProvider) {
		this.persistenceService = PersistenceService.getInstance();
		this.exportServiceProvider = exportServiceProvider;
		this.wiCSVExporter = new WorkInstructionCSVExporter();
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
				sendWorkInstructions();
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
		this.completedWorkInstructions = new LinkedBlockingQueue<WIMessage>(this.capacity);
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

			WIMessage exportMessage = completedWorkInstructions.take(); //blocking
			try {
				//transaction begun and closed after blocking call so that it is not held open
				persistenceService.beginTenantTransaction();
				boolean sent = false;
				while (!sent) {
					try {
						IEdiService ediExportService = exportMessage.exportService;
						ediExportService.sendWorkInstructionsToHost(exportMessage.messageBody);
						sent = true;
					} catch (IOException e) {
						LOGGER.warn("failure to send work instructions, retrying after: " + retryDelay, e);
						Thread.sleep(retryDelay);
					}
				}
				persistenceService.commitTenantTransaction();
			} catch (Exception e) {
				persistenceService.rollbackTenantTransaction();
				LOGGER.error("Unexpected exception sending work instruction, skipping: " + exportMessage, e);
			}
		}
	}

	public List<WiSetSummary> workSummary(UUID cheId, UUID facilityId) {
		WiSummarizer summarizer = new WiSummarizer();
		summarizer.computeWiSummariesForChe(cheId, facilityId);
		return summarizer.getSummaries();
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI simulation
	 * @return
	 */
	public final void fakeCompleteWi(String wiPersistentId, String inCompleteStr) {
		WorkInstruction wi = WorkInstruction.DAO.findByPersistentId(wiPersistentId);
		boolean doComplete = inCompleteStr.equalsIgnoreCase("COMPLETE");
		boolean doShort = inCompleteStr.equalsIgnoreCase("SHORT");

		// default to complete values
		Integer actualQuant = wi.getPlanQuantity();
		WorkInstructionStatusEnum newStatus = WorkInstructionStatusEnum.COMPLETE;

		if (doComplete) {
		} else if (doShort) {
			actualQuant--;
			newStatus = WorkInstructionStatusEnum.SHORT;
		}
		Timestamp completeTime = new Timestamp(System.currentTimeMillis());
		Timestamp startTime = new Timestamp(System.currentTimeMillis() - (10 * 1000)); // assume 10 seconds earlier

		wi.setActualQuantity(actualQuant);
		wi.setCompleted(completeTime);
		wi.setStarted(startTime);
		wi.setStatus(newStatus);
		wi.setType(WorkInstructionTypeEnum.ACTUAL);

		//send in in like in came from SiteController
		completeWorkInstruction(wi.getAssignedChe().getPersistentId(), wi);
	}

	public void completeWorkInstruction(UUID cheId, WorkInstruction incomingWI) {
		Che che = Che.DAO.findByPersistentId(cheId);
		if (che != null) {
			try {
				final WorkInstruction storedWi = persistWorkInstruction(incomingWI);
				exportWorkInstruction(storedWi);
			} catch (DaoException e) {
				LOGGER.error("Unable to record work instruction: " + incomingWI, e);
			} catch (IOException e) {
				LOGGER.error("Unable to export work instruction: " + incomingWI, e);
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
	public void exportWorkInstruction(WorkInstruction inWorkInstruction) throws IOException {
		// jr/hibernate  tracking down an error
		if (completedWorkInstructions == null)
			LOGGER.error("null completedWorkInstructions in WorkService.exportWorkInstruction", new Exception());
		else if (inWorkInstruction == null)
			LOGGER.error("null input to WorkService.exportWorkInstruction", new Exception());
		else {
			LOGGER.debug("Queueing work instruction: " + inWorkInstruction);
			String messageBody = wiCSVExporter.exportWorkInstructions(ImmutableList.of(inWorkInstruction));
			Facility facility = inWorkInstruction.getParent();
			IEdiService ediExportService = exportServiceProvider.getWorkInstructionExporter(facility);
			WIMessage wiMessage = new WIMessage(ediExportService, messageBody);
			completedWorkInstructions.add(wiMessage);
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

	public static ProductivitySummaryList getProductivitySummary(UUID facilityId, boolean skipSQL) throws Exception {
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new NotFoundException("Facility " + facilityId + " does not exist");
		}
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		List<Object[]> picksPerHour = null;
		if (!skipSQL) {
			String schema = System.getProperty("db.schemaname", "codeshelf");
			String queryStr = String.format("" 
					+ "SELECT dur.order_group AS group,\n" 
					+ "		trim(to_char(\n"
					+ "		 3600 / (EXTRACT('epoch' FROM avg(dur.duration)) + 1) ,\n"
					+ "		'9999999999999999999D9')) AS picksPerHour\n" 
					+ "FROM \n" + "	(\n" + "		SELECT group_and_sort_code,\n"
					+ "			COALESCE(g.domainid, 'undefined') AS order_group,\n"
					+ "			i.completed - lag(i.completed) over (ORDER BY i.completed) as duration\n"
					+ "		FROM %s.work_instruction i\n"
					+ "			INNER JOIN %s.order_detail d ON i.order_detail_persistentid = d.persistentid\n"
					+ "			INNER JOIN %s.order_header h ON d.parent_persistentid = h.persistentid\n"
					+ "			LEFT JOIN %s.order_group g ON h.order_group_persistentid = g.persistentid\n"
					+ "		WHERE  i.item_id != 'Housekeeping'\n" + "	) dur\n" + "WHERE dur.group_and_sort_code != '0001'\n"
					+ "GROUP BY dur.order_group\n" + "ORDER BY dur.order_group", schema, schema, schema, schema);
			SQLQuery getPicksPerHourQuery = session.createSQLQuery(queryStr)
				.addScalar("group", StandardBasicTypes.STRING)
				.addScalar("picksPerHour", StandardBasicTypes.DOUBLE);
			picksPerHour = getPicksPerHourQuery.list();
		}
		ProductivitySummaryList productivitySummary = new ProductivitySummaryList(facility, picksPerHour);
		return productivitySummary;
	}

	public static ProductivityCheSummaryList getCheByGroupSummary(UUID facilityId) throws Exception {
		List<WorkInstruction> instructions = WorkInstruction.DAO.findByFilterAndClass(CriteriaRegistry.ALL_BY_PARENT, ImmutableMap.<String, Object>of("parentId", facilityId), WorkInstruction.class);
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(facilityId, instructions);
		return summary;
	}
	
	public static List<WorkInstruction> getGroupShortInstructions(UUID facilityId, String groupNameIn) throws NotFoundException{
		//Get Facility
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new NotFoundException("Facility " + facilityId + " does not exist");
		}
		//If group name provided, confirm that such group exists
		boolean allGroups = groupNameIn == null, undefined = OrderGroup.UNDEFINED.equalsIgnoreCase(groupNameIn);
		if (!(allGroups || undefined)) {
			OrderGroup group = OrderGroup.DAO.findByDomainId(facility, groupNameIn);
			if (group == null){
				throw new NotFoundException("Group " + groupNameIn + " had not been created");
			}
		}
		//Get all instructions and filter those matching the requirements
		List<WorkInstruction> instructions = WorkInstruction.DAO.findByFilterAndClass(CriteriaRegistry.ALL_BY_PARENT, ImmutableMap.<String, Object>of("parentId", facilityId), WorkInstruction.class);
		List<WorkInstruction> filtered = new ArrayList<>();
		for (WorkInstruction instruction : instructions){
	 		if (instruction.isHousekeeping() || instruction.getStatus() != WorkInstructionStatusEnum.SHORT) {
	 			continue;
 			}
			OrderDetail detail = instruction.getOrderDetail();
			if (detail == null) {
				continue;
			}
			OrderHeader header = detail.getParent();
			String groupName = header.getOrderGroup() == null? OrderGroup.UNDEFINED : header.getOrderGroup().getDomainId();
			if (allGroups || groupName.equals(groupNameIn)) {
				filtered.add(instruction);
			}			
		}
		return filtered;
	}

	/**
	 * Simple struct to keep associate export settings
	 * @author pmonteiro
	 *
	 */
	private static class WIMessage {
		private IEdiService	exportService;
		private String		messageBody;

		public WIMessage(IEdiService exportService, String messageBody) {
			super();
			this.exportService = exportService;
			this.messageBody = messageBody;
		}
	}
}
