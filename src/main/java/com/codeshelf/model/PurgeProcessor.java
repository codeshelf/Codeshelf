package com.codeshelf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointEngine;
import com.google.common.collect.ImmutableMap;

public class PurgeProcessor implements BatchProcessor {
	private static final Logger	LOGGER							= LoggerFactory.getLogger(PurgeProcessor.class);

	private Facility			facility;
	private DataPurgeParameters	purgeParams						= null;
	private List<UUID>			orderUuidsToPurge				= null;
	private List<UUID>			replenishDetailUuidsToPurge	= null;
	private List<UUID>			containerUuidsToPurge			= null;
	private List<UUID>			wiUuidsToPurge					= null;
	private List<UUID>			workerEventUuidsToPurge			= null;											// deletes any resolutions with them
	private List<UUID>			workInstructionBeanUuidsToPurge	= null;
	private List<UUID>			importReceiptUuidsToPurge		= null;
	private List<UUID>			exportMessageUuidsToPurge		= null;

	enum PurgePhase {
		PurgePhaseInit("Init"),
		PurgePhaseSetup("Setup"),
		PurgePhaseReplenishDetails("Replenish_Details"),
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

	private static final Map<PurgePhase, PurgePhase> NEXT_PHASE = new ImmutableMap.Builder<PurgePhase, PurgePhase>()
	           .put(PurgePhase.PurgePhaseSetup, PurgePhase.PurgePhaseWorkerEvents)
	           .put(PurgePhase.PurgePhaseWorkerEvents, PurgePhase.PurgePhaseOrders)
	           .put(PurgePhase.PurgePhaseOrders, PurgePhase.PurgePhaseReplenishDetails)
	           .put(PurgePhase.PurgePhaseReplenishDetails, PurgePhase.PurgePhaseWis)
	           .put(PurgePhase.PurgePhaseWis, PurgePhase.PurgePhaseContainers)
	           .put(PurgePhase.PurgePhaseContainers, PurgePhase.PurgePhaseWorkInstructionBeans)
	           .put(PurgePhase.PurgePhaseWorkInstructionBeans, PurgePhase.PurgePhaseExportMessages)
	           .put(PurgePhase.PurgePhaseExportMessages, PurgePhase.PurgePhaseImportReceipts)
	           .put(PurgePhase.PurgePhaseImportReceipts, PurgePhase.PurgePhaseDone)
	           .build();
	
	@Setter
	@Getter
	private PurgePhase	purgePhase;

	// We do not pretend to track resolutions, work instructions, or containers

	// Although we do count work instructions deleted in the non-order phase, and containers deleted at the end.
	int			ordersPurged				= 0;
	int			replenishDetailPurged		= 0;
	int			workerEventsPurged			= 0;
	int			workInstructionBeansPurged	= 0;
	int			importReceiptsPurged		= 0;
	int			exportMessagesPurged		= 0;
	int			wisPurged					= 0;
	int			cntrsPurged					= 0;

	public PurgeProcessor(Facility inFacility) {
		facility = inFacility;
		setPurgePhase(PurgePhase.PurgePhaseInit);
	}

	/**
	 * This function gives the order of phases worked in the purge.
	 * Obviously, this starts from PurgePhaseSetup
	 */
	private void setNextPurgePhase(PurgePhase currentPhase) {
		PurgePhase nextPhase = NEXT_PHASE.get(currentPhase);
		if (nextPhase == null){
			LOGGER.error("unexpected or unimplemented phase in setNextPurgePhase(). Setting to DONE");
			nextPhase = PurgePhase.PurgePhaseDone;
		}
		setPurgePhase(nextPhase);
	}

	/**
	 * Only count progress against the things we knew the size of at the start. (Exclude containers, orderGroups. Although we knew WIs at the start we don't count the ones deleted via orders.).
	 */
	private int getProgressCount() {
		return ordersPurged + workerEventsPurged + workInstructionBeansPurged + importReceiptsPurged + exportMessagesPurged;
	}
	/**
	 * Should match getProgressCount(). Assumes the counts are all set from the pre-built lists.
	 */
	private int getSizeOfJob() {
		return orderUuidsToPurge.size() + workerEventUuidsToPurge.size() + workInstructionBeanUuidsToPurge.size() + importReceiptUuidsToPurge.size() + exportMessageUuidsToPurge.size();
	}

	/**
	 *  This needs to return a count of how many "things" it is going to do.
	 *  And assemble and remember what doBatch is going to do. This is called in an appropriate transaction
	 *  Unfortunately, we cannot really know how many containers or wis will be purged until after the order purge is done.
	 *  This returns the sum of orders to purge, and workerEvents to purge
	 */
	@Override
	public int doSetup() throws Exception {
		// This kicks off the the purge phase state machine. No more calls to setPurgePhase. 
		// Only call setNextPurgePhase(currentPhase) as this does at the end of the function.
		setPurgePhase(PurgePhase.PurgePhaseSetup);
		PurgePhase currentPhase = this.getPurgePhase();

		facility = facility.reload();
		// get our purge parameters
		ExtensionPointEngine ss = ExtensionPointEngine.getInstance(facility);
		purgeParams = ss.getDataPurgeParameters();

		LOGGER.info("Starting data purge with these parameters: {}", purgeParams);

		// preassemble some of the list, but not the ones that need to be computed in later stages.
		// and, don't fail the entire purge if only one part is bad.

		try {
			workerEventUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWorkerEvents);
		} catch (Exception e) {
			LOGGER.info("buildWorkerEventList failed", e);
		}
		try {
			orderUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseOrders);
		} catch (Exception e) {
			LOGGER.info("buildOrdersList failed", e);
		}
		try {
			replenishDetailUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseReplenishDetails);
		} catch (Exception e) {
			LOGGER.info("buildOrdersList failed", e);
		}
		try {
			importReceiptUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseImportReceipts);
		} catch (Exception e) {
			LOGGER.info("buildImportReceiptList failed", e);
		}
		try {
			workInstructionBeanUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWorkInstructionBeans);
		} catch (Exception e) {
			LOGGER.info("buildWorkInstructionBeanList failed", e);
		}
		try {
			exportMessageUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseExportMessages); //must be called here instead of in doBatch() so we can return something about the size of the job
		} catch (Exception e) {
			LOGGER.info("buildExportMessageList failed", e);
		}

		// do not do work instructions or containers

		setNextPurgePhase(currentPhase);
		return getSizeOfJob();
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
			case PurgePhaseReplenishDetails:
				value = doMananager.purgeSomeReplenishDetails(batch);
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
			case PurgePhaseReplenishDetails:
				return doMananager.getReplenishDetailsUuidsToPurge(daysOld);
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

	/**
	 * Performs one batch of the purge process. What it does depends on the purge phase.
	 * In each if block, just make sure you have the correct list, purgeXxxBatch(), and xxxPurged counter.
	 * A couple of the phases do not have pre-built lists, so build their list as the phase is entered the first time.
	 */
	@Override
	public int doBatch(int batchCount) throws Exception {
		PurgePhase currentPhase = getPurgePhase();
		// in almost every case, need to reload facility, so do it centrally
		facility = facility.reload();

		if (currentPhase == PurgePhase.PurgePhaseOrders) {
			if (complainNoList(currentPhase, orderUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = orderUuidsToPurge.size();
			if (startSize > 0)
				ordersPurged += purgeBatch(orderUuidsToPurge, PurgePhase.PurgePhaseOrders);
			int endSize = orderUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, ordersPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseReplenishDetails) {
			if (complainNoList(currentPhase, replenishDetailUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = replenishDetailUuidsToPurge.size();
			if (startSize > 0)
				replenishDetailPurged += purgeBatch(replenishDetailUuidsToPurge, PurgePhase.PurgePhaseReplenishDetails);
			int endSize = replenishDetailUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, replenishDetailPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseWis) {
			// Work instructions does not have a pre-built list
			if (wiUuidsToPurge == null)
				wiUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseWis);
			int startSize = wiUuidsToPurge.size();
			if (startSize > 0)
				wisPurged += purgeBatch(wiUuidsToPurge, PurgePhase.PurgePhaseWis);
			int endSize = wiUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, wisPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseContainers) {
			// Containers does not have a pre-built list
			if (containerUuidsToPurge == null)
				containerUuidsToPurge = buildUuidList(PurgePhase.PurgePhaseContainers);
			int startSize = containerUuidsToPurge.size();
			if (startSize > 0)
				cntrsPurged += purgeBatch(containerUuidsToPurge, PurgePhase.PurgePhaseContainers);
			int endSize = containerUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, cntrsPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseWorkerEvents) {
			if (complainNoList(currentPhase, workerEventUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = workerEventUuidsToPurge.size();
			if (startSize > 0)
				workerEventsPurged += purgeBatch(workerEventUuidsToPurge, PurgePhase.PurgePhaseWorkerEvents);
			int endSize = workerEventUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, workerEventsPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseExportMessages) {
			if (complainNoList(currentPhase, exportMessageUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = exportMessageUuidsToPurge.size();
			if (startSize > 0)
				exportMessagesPurged += purgeBatch(exportMessageUuidsToPurge, PurgePhase.PurgePhaseExportMessages);
			int endSize = exportMessageUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, exportMessagesPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseImportReceipts) {
			if (complainNoList(currentPhase, importReceiptUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = importReceiptUuidsToPurge.size();
			if (startSize > 0)
				importReceiptsPurged += purgeBatch(importReceiptUuidsToPurge, PurgePhase.PurgePhaseImportReceipts);
			int endSize = importReceiptUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, importReceiptsPurged, batchCount);

		} else if (currentPhase == PurgePhase.PurgePhaseWorkInstructionBeans) {
			if (complainNoList(currentPhase, workInstructionBeanUuidsToPurge)) {
				return getProgressCount();
			}
			int startSize = workInstructionBeanUuidsToPurge.size();
			if (startSize > 0)
				workInstructionBeansPurged += purgeBatch(workInstructionBeanUuidsToPurge, PurgePhase.PurgePhaseWorkInstructionBeans);
			int endSize = workInstructionBeanUuidsToPurge.size();
			advanceAndReport(currentPhase, startSize, endSize, workInstructionBeansPurged, batchCount);

		} else {
			LOGGER.error("Unexpected phase in PurgeProcessor doBatch {}", currentPhase);
			setPurgePhase(PurgePhase.PurgePhaseDone);
		}

		return getProgressCount();
	}

	/**
	 * Declone and enforce this common error check pattern. 
	 * Large side effect: advances to the next phase if this return true.
	 */
	private boolean complainNoList(PurgePhase currentPhase, List<UUID> inList) {
		if (inList == null) {
			LOGGER.error("Expected list for phase {}. Bailing out.", currentPhase);
			setNextPurgePhase(currentPhase);
			return true;
		} else {
			return false;
		}
	}

	/**
	* Declone and enforce this common pattern. Advances and/or reports only if appropriate
	* Large side effect: may advance to the next phase
	*/
	private void advanceAndReport(PurgePhase currentPhase, int startSize, int endSize, int totalThisObjectPurged, int batchCount) {
		// we want to know if we are done with the phase. One quite reliable way to know is if the object list size decreased.
		// If non-zero, but it did not decrease, lets log an error and call it done so we are not stuck forever.
		if (endSize == startSize || endSize == 0) {
			LOGGER.info("Total: purged {} {}", totalThisObjectPurged, currentPhase);
			if (endSize > 0) {
				LOGGER.error("{} purge did not progress. Bailing out. Leaving {} that should have been purged",
					currentPhase,
					endSize);
			}
			setNextPurgePhase(currentPhase);
		} else if (batchCount % 10 == 0) {
			LOGGER.info("incremental total: purged {} {}", totalThisObjectPurged, currentPhase);
		}
	}

	@Override
	public void doTeardown() {

	}

	@Override
	public boolean isDone() {
		return getPurgePhase() == PurgePhase.PurgePhaseDone;
	}
}