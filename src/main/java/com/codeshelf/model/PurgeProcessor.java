package com.codeshelf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.BatchProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointEngine;

public class PurgeProcessor implements BatchProcessor {
	private static final Logger	LOGGER							= LoggerFactory.getLogger(PurgeProcessor.class);

	Facility					facility;
	DataPurgeParameters			purgeParams						= null;
	List<UUID>					orderUuidsToPurge				= null;
	List<UUID>					containerUuidsToPurge			= null;
	List<UUID>					wiUuidsToPurge					= null;
	List<UUID>					workerEventUuidsToPurge			= null;											// deletes any resolutions with them
	List<UUID>					workInstructionBeanUuidsToPurge	= null;
	List<UUID>					importReceiptUuidsToPurge		= null;
	List<UUID>					exportMessageUuidsToPurge		= null;

	enum PurgePhase {
		PurgePhaseInit("Init"),
		PurgePhaseSetup("Setup"),
		PurgePhaseOrders("Orders"),
		PurgePhaseContainers("Containers"),
		PurgePhaseWis("Work_Instructions"),
		PurgePhaseWorkerEvents("Worker_Events"),
		PurgePhaseWorkInstructionBeans("Work_Instruction_Beans"),
		PurgePhaseImportReceipts("EDI_Receipts"),
		PurgePhaseExportMessages("Export_Messages"),
		PurgePhaseDone("Done");

		@Getter
		@Accessors(prefix = "m")
		private String	mPhaseName;

		PurgePhase(String phaseName) {
			mPhaseName = phaseName;
		}

		@Override
		public String toString() {
			return getPhaseName();
		}
	}

	@Setter
	@Getter
	PurgePhase	purgePhase;

	@Setter
	@Getter
	int			ordersToPurge				= 0;
	@Setter
	@Getter
	int			workerEventsToPurge			= 0;
	@Setter
	@Getter
	int			workInstructionBeansToPurge	= 0;
	@Setter
	@Getter
	int			ediReceiptsToPurge			= 0;
	@Setter
	@Getter
	int			exportMessagesToPurge		= 0;
	// We do not pretend to track resolutions, work instructions, or containers

	// Although we do count work instructions deleted in the non-order phase, and containers deleted at the end.
	int			ordersPurged				= 0;
	int			workerEventsPurged			= 0;
	int			workInstructionBeansPurged	= 0;
	int			ediReceiptsPurged			= 0;
	int			exportMessagesPurged		= 0;
	int			wisPurged					= 0;
	int			cntrsPurged					= 0;

	public PurgeProcessor(Facility inFacility) {
		facility = inFacility;
		setPurgePhase(PurgePhase.PurgePhaseInit);
	}

	/**
	 * Only count progress against the things we knew the size of at the start. (Exclude containers, orderGroups. Although we knew WIs at the start we don't count the ones deleted via orders.).
	 */
	private int getProgressCount() {
		return ordersPurged + workerEventsPurged + workInstructionBeansPurged + ediReceiptsPurged + exportMessagesPurged;
	}

	/**
	 *  This needs to return a count of how many "things" it is going to do.
	 *  And assemble and remember what doBatch is going to do. This is called in an appropriate transaction
	 *  Unfortunately, we cannot really know how many containers or wis will be purged until after the order purge is done.
	 *  This returns the sum of orders to purge, and workerEvents to purge
	 */
	@Override
	public int doSetup() throws Exception {
		setPurgePhase(PurgePhase.PurgePhaseSetup);
		facility = facility.reload();
		// get our purge parameters
		ExtensionPointEngine ss = ExtensionPointEngine.getInstance(facility);
		purgeParams = ss.getDataPurgeParameters();

		LOGGER.info("Starting data purge with these parameters: {}", purgeParams);

		// preassemble some of the list, but not the ones that need to be computed in later stages.
		// and, don't fail the entire purge if only one part is bad.

		try {
			buildWorkerEventList();
		} catch (Exception e) {
			LOGGER.info("buildWorkerEventList failed", e);
		}
		try {
			buildOrdersList();
		} catch (Exception e) {
			LOGGER.info("buildOrdersList failed", e);
		}
		try {
			buildImportReceiptList();
		} catch (Exception e) {
			LOGGER.info("buildImportReceiptList failed", e);
		}
		try {
			buildWorkInstructionBeanList();
		} catch (Exception e) {
			LOGGER.info("buildWorkInstructionBeanList failed", e);
		}
		try {
			buildExportMessageList(); //must be called here instead of in doBatch() so we can return something about the size of the job
		} catch (Exception e) {
			LOGGER.info("buildExportMessageList failed", e);
		}

		// do not do work instructions or containers

		setPurgePhase(PurgePhase.PurgePhaseOrders);
		return (getOrdersToPurge() + getWorkerEventsToPurge());
	}

	private int getBatchSize(PurgePhase inWhatToPurge) {
		int value = 50;
		switch (inWhatToPurge) {
			case PurgePhaseWis:
				value = purgeParams.getWorkInstructionBatchValue();
				break;
			case PurgePhaseContainers:
				value = purgeParams.getContainerBatchValue();
				break;
			case PurgePhaseExportMessages:
				value = purgeParams.getEdiHistoryBatchValue();
				break;
			case PurgePhaseImportReceipts:
				value = purgeParams.getEdiHistoryBatchValue();
				break;
			case PurgePhaseOrders:
				value = purgeParams.getOrderBatchValue();
				break;
			case PurgePhaseWorkInstructionBeans:
				value = purgeParams.getWorkInstructionBatchValue();
				break;
			case PurgePhaseWorkerEvents:
				value = purgeParams.getWorkInstructionBatchValue();
				break;
			default:
				LOGGER.info("Unexpected value or missing implementation for phase {} in purgeBatch()", inWhatToPurge);
				break;
		}
		return value;
	}

	private int purgeBatch(List<UUID> inList, PurgePhase inWhatToPurge) {
		if (inList.isEmpty()) {
			LOGGER.error("unexpected state for purgeBatch() in phase {}", inWhatToPurge);
			return 0;
		}
		int batchSize = getBatchSize(inWhatToPurge);
		List<UUID> batch = subUuidListAndRemoveFromInList(inList, batchSize);
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		int value = 0;
		switch (inWhatToPurge) {
			case PurgePhaseWis:
				value = doMananager.purgeSomeWorkInstructions(batch);
				break;
			case PurgePhaseContainers:
				value = doMananager.purgeSomeCntrs(batch);
				break;
			case PurgePhaseExportMessages:
				value = doMananager.purgeSomeExportMessages(batch);
				break;
			case PurgePhaseImportReceipts:
				value = doMananager.purgeSomeImportReceipts(batch);
				break;
			case PurgePhaseOrders:
				value = doMananager.purgeSomeOrders(batch);
				break;
			case PurgePhaseWorkInstructionBeans:
				value = doMananager.purgeSomeWiCsvBeans(batch);
				break;
			case PurgePhaseWorkerEvents:
				value = doMananager.purgeSomeWorkerEvents(batch);
				break;
			default:
				LOGGER.info("Unexpected value or missing implementation for phase {} in purgeBatch()", inWhatToPurge);
				break;
		}
		return value;
	}

	/**
	 * via switch, gets the appropriate list with the side effect of setting the new list in the object
	 */
	private List<UUID> buildUuidList(PurgePhase inWhatToPurge) {
		int daysOld = purgeParams.getPurgeAfterDaysValue();
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		switch (inWhatToPurge) {
			case PurgePhaseWis:
				return doMananager.getWorkInstructionUuidsToPurge(daysOld);
			case PurgePhaseContainers:
				return doMananager.getCntrUuidsToPurge(daysOld);
			case PurgePhaseExportMessages:
				return doMananager.getExportMessageUuidsToPurge(daysOld);
			case PurgePhaseImportReceipts:
				return doMananager.getImportReceiptUuidsToPurge(daysOld);
			case PurgePhaseOrders:
				return doMananager.getOrderUuidsToPurge(daysOld);
			case PurgePhaseWorkInstructionBeans:
				return doMananager.getWorkInstructionCsvBeanUuidsToPurge(daysOld);
			case PurgePhaseWorkerEvents:
				return doMananager.getWorkerEventUuidsToPurge(daysOld);
			default:
				LOGGER.info("Unexpected value or missing implementation for phase {} in buildUuidList()", inWhatToPurge);
				break;
		}
		return new ArrayList<UUID>();
	}

	private void buildOrdersList() {
		orderUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseOrders);
		setOrdersToPurge(orderUuidsToPurge.size()); // record the starting size of the job
	}

	private int purgeOrderBatch() {
		return purgeBatch(orderUuidsToPurge, PurgePhase.PurgePhaseOrders);
	}

	private void buildWorkerEventList() {
		workerEventUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWorkerEvents);
		setWorkerEventsToPurge(workerEventUuidsToPurge.size()); // record this component of the starting size of the job
	}

	private int purgeWorkerEventBatch() {
		return purgeBatch(workerEventUuidsToPurge, PurgePhase.PurgePhaseWorkerEvents);
	}

	private void buildImportReceiptList() {
		importReceiptUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseImportReceipts);
		setEdiReceiptsToPurge(importReceiptUuidsToPurge.size()); // record this component of the starting size of the job
	}

	@SuppressWarnings("unused")
	private int purgeImportReceiptBatch() {
		return purgeBatch(importReceiptUuidsToPurge, PurgePhase.PurgePhaseImportReceipts);
	}

	private void buildWorkInstructionBeanList() {
		workInstructionBeanUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWorkInstructionBeans);
		setWorkInstructionBeansToPurge(workInstructionBeanUuidsToPurge.size()); // record this component of the starting size of the job
	}

	private void buildExportMessageList() {
		exportMessageUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseExportMessages);
		setExportMessagesToPurge(exportMessageUuidsToPurge.size()); // record this component of the starting size of the job
	}

	// work instructions and containers do not have build functions because they are not built in advance
	private int purgeWiBatch() {
		return purgeBatch(wiUuidsToPurge, PurgePhase.PurgePhaseWis);
	}

	private int purgeCntrBatch() {
		return purgeBatch(containerUuidsToPurge, PurgePhase.PurgePhaseContainers);
	}

	/**
	 *  Pass in a list. We want to get back the sublist, and remove items from the passed in list.
	 *  (Would be ok also to replace the passed in list if we returned a two parameter class.)
	 */
	private List<UUID> subUuidListAndRemoveFromInList(List<UUID> inList, int subCount) {
		int countNeeded = Math.min(subCount, inList.size());

		List<UUID> returnList = new ArrayList<UUID>(inList.subList(0, countNeeded));
		inList.removeAll(returnList);
		return returnList;
	}

	@Override
	public int doBatch(int batchCount) throws Exception {
		PurgePhase currentPhase = getPurgePhase();
		// in almost every case, need to reload facility, so do it centrally
		facility = facility.reload();

		if (currentPhase == PurgePhase.PurgePhaseOrders) {
			// we want to know if we are done with orders. One quite reliable way to know is if the orders list size decreased.
			// If non-zero, but it did not decrease, lets log an error and call it done so we are not stuck forever.
			int startSize = orderUuidsToPurge.size();
			if (startSize > 0)
				ordersPurged += purgeOrderBatch();
			int endSize = orderUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				setPurgePhase(PurgePhase.PurgePhaseWis);
				LOGGER.info("Total: purged {} orders", ordersPurged);
				if (endSize > 0) {
					LOGGER.error("orders purge did not progress. Bailing out. Leaving {} orders that should have been purged",
						endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} orders", ordersPurged);
			}

		} else if (currentPhase == PurgePhase.PurgePhaseWis) {
			if (wiUuidsToPurge == null)
				wiUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWis);
			int startSize = wiUuidsToPurge.size();
			if (startSize > 0)
				wisPurged += purgeWiBatch();
			int endSize = wiUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				LOGGER.info("Total: purged {} work instructions", wisPurged);
				setPurgePhase(PurgePhase.PurgePhaseContainers);
				if (endSize > 0) {
					LOGGER.error("work instructions purge did not progress. Bailing out. Leaving {} work instructions that should have been purged",
						endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} work instructions", wisPurged);
			}

		} else if (currentPhase == PurgePhase.PurgePhaseContainers) {
			if (containerUuidsToPurge == null)
				containerUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseContainers);
			int startSize = containerUuidsToPurge.size();
			if (startSize > 0)
				cntrsPurged += purgeCntrBatch();
			int endSize = containerUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				LOGGER.info("Total: purged {} containers", cntrsPurged);
				setPurgePhase(PurgePhase.PurgePhaseWorkerEvents);
				if (endSize > 0) {
					LOGGER.error("container purge did not progress. Bailing out. Leaving {} containers that should have been purged",
						endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} containers", cntrsPurged);
			}

		} else if (currentPhase == PurgePhase.PurgePhaseWorkerEvents) {
			if (workerEventUuidsToPurge == null) {
				LOGGER.error("Expected worker event list in PurgePhaseWorkerEvents.Bailing out.");
				setPurgePhase(PurgePhase.PurgePhaseDone);
			}

			int startSize = workerEventUuidsToPurge.size();
			if (startSize > 0)
				workerEventsPurged += purgeWorkerEventBatch();
			int endSize = workerEventUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				setPurgePhase(PurgePhase.PurgePhaseDone);
				LOGGER.info("Total: purged {} worker events", workerEventsPurged);
				if (endSize > 0) {
					LOGGER.error("workerEvents purge did not progress. Bailing out. Leaving {} workerEvents that should have been purged",
						endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} workerEvents", workerEventsPurged);
			}

		} else {
			LOGGER.error("Unexpected phase in PurgeProcessor doBatch {}", currentPhase);
			setPurgePhase(PurgePhase.PurgePhaseDone);
		}

		return getProgressCount();
	}

	@Override
	public void doTeardown() {

	}

	@Override
	public boolean isDone() {
		return getPurgePhase() == PurgePhase.PurgePhaseDone;

	}
}